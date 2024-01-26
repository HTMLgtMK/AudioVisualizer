package com.gthncz.audiovisuailzer.visualizer

import android.media.audiofx.Visualizer
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.annotation.WorkerThread
import com.gthncz.audiovisuailzer.visualizer.renderer.IRenderer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.absoluteValue
import kotlin.math.hypot
import kotlin.math.log10

object VisualizerController {

    private const val TAG = "VisualizerController"

    private var mVisualizer: Visualizer? = null

    private val mHandlerThread = HandlerThread("Visualizer-Handler").also {
        it.start()
    }
    private val mHandler = Handler(mHandlerThread.looper) {
        return@Handler false
    }

    private val DESIRED_CAPTURE_SIZE = 1024 // capture times every second.

    private val mProcessAudioFeatureCallbacks = CopyOnWriteArrayList<ProcessAudioFeatureCallback>()

    fun addProcessAudioFeatureCallback(callback: ProcessAudioFeatureCallback) {
        if (callback !in mProcessAudioFeatureCallbacks) {
            mProcessAudioFeatureCallbacks.add(callback)
        }
    }

    fun delProcessAudioFeatureCallback(callback: ProcessAudioFeatureCallback) {
        mProcessAudioFeatureCallbacks.remove(callback)
    }

    fun onPlayerPrepared(audioSession: Int = 0) {
        val captureSize = getCaptureSize()
        mVisualizer = Visualizer(audioSession)
        mVisualizer?.setCaptureSize(captureSize)
        mVisualizer?.setDataCaptureListener(object: Visualizer.OnDataCaptureListener {
            override fun onWaveFormDataCapture(visualizer: Visualizer?, waveform: ByteArray?, samplingRate: Int) {

            }

            override fun onFftDataCapture(vializer: Visualizer?, fft: ByteArray?, samplingRate: Int) {
                fft ?: return
                mHandler.post {
                    processAudioFeature(fft)
                }
            }
        }, Visualizer.getMaxCaptureRate() / 2, false, true)
    }

    fun onPlayerReleased() {
        try {
            mVisualizer?.release()
            mVisualizer = null
        } catch (e: Exception) {
            Log.i(TAG, "[onPlayerReleased] exception.", e)
        }
    }

    fun start() {
        mVisualizer?.enabled = true
    }

    fun stop() {
        mVisualizer?.enabled = false
    }

    @WorkerThread
    fun processAudioFeature(model: ByteArray) {
        mProcessAudioFeatureCallbacks.forEach { callback->
            callback.invoke(model)
        }
    }

    fun getCaptureSize(): Int {
        val captureSizeRange = Visualizer.getCaptureSizeRange().let { it[0]..it[1] }
        val captureSize = DESIRED_CAPTURE_SIZE.coerceIn(captureSizeRange)
        return captureSize
    }

    // return samplingRate in hz.
    fun getSamplingRate(): Int? = mVisualizer?.samplingRate?.div(1000)
}

typealias ProcessAudioFeatureCallback = (ByteArray)->Unit