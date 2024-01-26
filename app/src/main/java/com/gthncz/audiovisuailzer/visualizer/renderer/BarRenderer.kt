package com.gthncz.audiovisuailzer.visualizer.renderer

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.Log
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.math.log10

class BarRenderer: IRenderer {

    companion object {
        private const val TAG = "BarRenderer"
    }

    private val mTargetEndPoints = listOf(
        0f, 63f, 100f, 160f, 200f, 250f, 315f, 400f, 500f, 630f, 800f, 1000f, 1250f, 1600f, 2000f, 2500f,
        3150f, 4000f, 5000f, 6250f, 8000f, 10000f, 12500f, 16000f, 20000f
    )

    private var mFreqencyOrdinalRanges: List<IntRange> = emptyList()

    private val mPaint: Paint
    private var mGapWidth: Float = 4.dp.value
    private var mBarWidth: Float = 4.dp.value

    // number of bar.
    private var mBarNum: Int = 100

    init {
        mPaint = Paint()
        mPaint.color = Color.MAGENTA
        mPaint.strokeWidth = mBarWidth
        mPaint.maskFilter = BlurMaskFilter(1.dp.value, BlurMaskFilter.Blur.SOLID)
        // mPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
    }

    override fun initRender(extra: IRenderer.RenderExtra) {
        Log.v(TAG, "[initRender] extra: $extra")
        val captureSize = extra.captureSize.takeIf { it > 0 } ?: 1024
        val samplingRate = extra.samplingRate.takeIf { it > 0 } ?: 44100
        mFreqencyOrdinalRanges = mTargetEndPoints.zipWithNext { a, b ->
            val startOrdinal = (1 + (captureSize * a / samplingRate)).toInt()
            // The + 1 omits the DC offset in the first range, and the overlap for remaining ranges
            val endOrdinal = (captureSize * b / samplingRate).toInt()
            startOrdinal..endOrdinal
        }.also {
            it.forEach {range->
                Log.v(TAG, "visualizer range: ${range.joinToString()}")
            }
        }
    }

    override fun render(canvas: Canvas, rect: Rect, fft: ByteArray) {
        val dbData = processFftData(fft)

        // 1. calculate bar width and bar gap.
        mBarNum = dbData.size
        val barGapWidth = rect.width() / (mBarNum * 2 - 1) * 1.0f
        mGapWidth = barGapWidth
        mBarWidth = barGapWidth
        mPaint.strokeWidth = mBarWidth

        val magDbScaled = dbData.map {
            it / 90.0f
        }

        Log.v(TAG, "db scaled: ${magDbScaled.size} ${magDbScaled.joinToString()}")

        val height = rect.height() * 1.0f
        magDbScaled.forEachIndexed { index, data ->
            val lineHeight = height * data
            val x = index * mGapWidth + index * mBarWidth
            // canvas.drawLine(x, height, x + mBarWidth, height - lineHeight, mPaint)
            canvas.drawRect(x, height, x + mBarWidth, height - lineHeight, mPaint)
        }
    }

    private fun processFftData(fft: ByteArray): FloatArray {
        val output = FloatArray(mFreqencyOrdinalRanges.size)
        mFreqencyOrdinalRanges.forEachIndexed { index, frequencyOrdinalRange ->
            var dbSum = 0f
            for (ordinal in frequencyOrdinalRange) {
                val fftIndex = ordinal * 2
                val magnitude = hypot(fft[fftIndex].toFloat(), fft[fftIndex + 1].toFloat())
                val db = magnitude.takeIf { it > 0 }?.let { 20 * log10(it) } ?: 0f
                dbSum += db
            }
            output[index] = dbSum / (frequencyOrdinalRange.last - frequencyOrdinalRange.first + 1)
        }
        return output
    }

}