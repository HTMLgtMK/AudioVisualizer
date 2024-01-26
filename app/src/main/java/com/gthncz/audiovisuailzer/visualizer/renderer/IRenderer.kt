package com.gthncz.audiovisuailzer.visualizer.renderer

import android.graphics.Canvas
import android.graphics.Rect

interface IRenderer {

    data class RenderExtra(
        var captureSize: Int,
        var samplingRate: Int
    )

    fun initRender(extra: RenderExtra)

    fun render(canvas: Canvas, rect: Rect, fft: ByteArray)

}