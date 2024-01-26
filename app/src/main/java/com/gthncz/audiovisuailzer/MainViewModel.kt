package com.gthncz.audiovisuailzer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gthncz.audiovisuailzer.player.LocalMediaPlayer
import com.gthncz.audiovisuailzer.player.PlayerState
import com.gthncz.audiovisuailzer.visualizer.VisualizerController
import com.gthncz.audiovisuailzer.visualizer.renderer.IRenderer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel(){

    private val mPlayerState: MutableStateFlow<Int> = MutableStateFlow(PlayerState.STATE_IDLE)
    val playerState: StateFlow<Int> = mPlayerState.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        mPlayerState.value
    )

    private val mSongPathStateFlow = MutableStateFlow<String?>(null)
    val songPathState: StateFlow<String?> = mSongPathStateFlow.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        mSongPathStateFlow.value
    )

    private val mVisualizerState = MutableStateFlow(IRenderer.RenderExtra(0, 0))
    val visualizerState: StateFlow<IRenderer.RenderExtra> = mVisualizerState.stateIn(
        viewModelScope,
        SharingStarted.Lazily,
        mVisualizerState.value
    )

    private var mPlayer: LocalMediaPlayer? = null
    val player: LocalMediaPlayer?
        get() = mPlayer

    fun updateSongPath(songPath: String?) {
        mSongPathStateFlow.update { songPath }
    }

    fun playOrPause() {
        when (playerState.value) {
            PlayerState.STATE_PAUSED -> resume()
            PlayerState.STATE_STARTED -> pause()
            else -> play()
        }
    }

    private fun pause() {
        mPlayer?.pause()
    }

    private fun resume() {
        mPlayer?.resume()
    }

    private fun play() {
        mPlayer?.release()
        val context = getAppContext() ?: return
        var songFilePath = songPathState.value?.takeIf { it.isNotEmpty() } ?: return
        songFilePath = if (songFilePath.startsWith("file://")) songFilePath else "file://$songFilePath"
        mPlayer = createPlayer(context, songFilePath)
        mPlayer?.start()
    }

    private fun createPlayer(context: Context, filePath: String): LocalMediaPlayer {
        return LocalMediaPlayer(context, filePath) {state->
            mPlayerState.update { state }
            when (state) {
                PlayerState.STATE_PREPARED -> {
                    VisualizerController.onPlayerPrepared(mPlayer?.getAudioSession() ?: 0)
                    mVisualizerState.update {
                        IRenderer.RenderExtra(
                            VisualizerController.getCaptureSize(),
                            VisualizerController.getSamplingRate() ?: 44100
                        )
                    }
                }
                PlayerState.STATE_COMPLETED,
                PlayerState.STATE_ERROR -> {
                    VisualizerController.onPlayerReleased()
                }
                PlayerState.STATE_STARTED -> {
                    VisualizerController.start()
                }
                PlayerState.STATE_PAUSED -> {
                    VisualizerController.stop()
                }
            }
        }
    }

    private fun getAppContext(): Context? = VisualizerApplication.get()
}