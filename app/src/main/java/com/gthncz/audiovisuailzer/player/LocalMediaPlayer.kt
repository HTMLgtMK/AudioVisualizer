package com.gthncz.audiovisuailzer.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.PowerManager
import android.util.Log
import kotlin.Exception

class LocalMediaPlayer(
    private val context: Context,
    private val filePath: String,
    private val stateChanged: (state: Int)->Unit
) {

    companion object {
        private const val TAG = "LocalMediaPlayer"
    }

    private var mPlayer: MediaPlayer? = null

    @PlayerState
    private var mPlayerState: Int = PlayerState.STATE_IDLE

    private val mStateChangeInner = {state: Int->
        Log.i(TAG, "[stateChange] oldState: $mPlayerState, newState: $state")
        mPlayerState = state
        stateChanged.invoke(state)
    }

    init {
        mPlayer = MediaPlayer().apply {
            val attrs =  AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        setAllowedCapturePolicy(AudioAttributes.ALLOW_CAPTURE_BY_ALL)
                    }
                }
                .build()
            setAudioAttributes(attrs)
            setDataSource(filePath)
        }

        mPlayer?.setOnPreparedListener {
            Log.i(TAG, "onPrepared.")

            mStateChangeInner.invoke(PlayerState.STATE_PREPARED)

            it.start()

            mStateChangeInner.invoke(PlayerState.STATE_STARTED)
        }

        mPlayer?.setOnBufferingUpdateListener { mp, percent->
            Log.i(TAG, "[onBufferingUpdate] percent: $percent")
        }

        mPlayer?.setOnCompletionListener {
            Log.i(TAG, "[onComplete] .")
            mStateChangeInner.invoke(PlayerState.STATE_COMPLETED)
        }

        mPlayer?.setOnInfoListener { mp, what, extra ->
            Log.i(TAG, "[onInfoChange] what: $what, extra: $extra")
            when (what) {
                MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                    mStateChangeInner.invoke(PlayerState.STATE_BUFFERING)
                }
            }
            return@setOnInfoListener false
        }

        mPlayer?.setOnErrorListener { mp, what, extra ->
            Log.i(TAG, "[onError] what: $what, extra: $extra")
            stateChanged.invoke(PlayerState.STATE_ERROR)
            return@setOnErrorListener false
        }

        mPlayer?.setOnSeekCompleteListener {
            Log.i(TAG, "seek completed.")
            mStateChangeInner.invoke(PlayerState.STATE_PREPARED)
            // todo: resume or start?
        }

        mStateChangeInner.invoke(PlayerState.STATE_INITIALIZED)
    }

    fun start() {
        mPlayer?.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
        mPlayer?.prepareAsync()
        mStateChangeInner.invoke(PlayerState.STATE_PREPARING)
    }

    fun release() {
        try {
            mPlayer?.release()
        } catch (e: Exception) {
            Log.i(TAG, "[release] exception.", e)
        } finally {
            mStateChangeInner.invoke(PlayerState.STATE_IDLE)
        }
    }

    fun pause() {
        try {
            mPlayer?.pause()
        } catch (e: Exception) {
            Log.i(TAG, "[pause] exception.", e)
        } finally {
            mStateChangeInner.invoke(PlayerState.STATE_PAUSED)
        }
    }

    fun resume() {
        try {
            mPlayer?.start()
        } catch (e: Exception) {
            Log.i(TAG, "[resume] exception.", e)
        } finally {
            mStateChangeInner.invoke(PlayerState.STATE_STARTED)
        }
    }

    fun getDuration() = mPlayer?.duration

    fun getCurrentPosition() = mPlayer?.currentPosition

    fun getPlayerState() = mPlayerState

    fun getAudioSession(): Int = mPlayer?.audioSessionId ?: 0

}