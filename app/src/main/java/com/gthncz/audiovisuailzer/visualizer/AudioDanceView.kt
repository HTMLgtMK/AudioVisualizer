package com.gthncz.audiovisuailzer.visualizer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.media.audiofx.Visualizer
import android.util.AttributeSet
import android.view.View
import androidx.compose.ui.unit.dp
import com.gthncz.audiovisuailzer.visualizer.renderer.BarRenderer
import com.gthncz.audiovisuailzer.visualizer.renderer.IRenderer
import kotlin.math.log10
import kotlin.math.sign

/**
 * Audio Visualizer View
 * usage: todo
 * addon-list: custom render styles
 */
class AudioDanceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
): View(context, attrs, defStyle) {

    companion object {
        // fft size / 2: Visualizer.getCaptureSize
        private const val DEFAULT_CAPTURE_SIZE = 1024
    }

    // magnitude data, transform from fft data.
    private var mFeatureData: ByteArray = ByteArray(DEFAULT_CAPTURE_SIZE)

    // canvas rect.
    private var mRect = Rect()
    // audio renderer list.
    private val mRenderers = mutableListOf<IRenderer>(
        BarRenderer()
    )

    fun initRenderer(extra: IRenderer.RenderExtra) {
        mRenderers.forEach {
            it.initRender(extra)
        }
    }

    fun updateFeatureData(data: ByteArray) {
        if (data.size != mFeatureData.size) {
            mFeatureData = ByteArray(data.size)
        }
        System.arraycopy(data, 0, mFeatureData, 0, mFeatureData.size)
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return
        mRect.set(0, 0, width, height)
        for (renderer in mRenderers) {
            renderer.render(canvas, mRect, mFeatureData)
        }
    }
}