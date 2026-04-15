package com.kuroda33.acapnys

import android.content.Context
import android.graphics.Canvas
import android.view.View
import com.daasuu.mp4compose.filter.GlOverlayFilter
import java.util.concurrent.atomic.AtomicInteger

internal class GyroOverlayFilter(
    context: Context,
    private val data: List<DataPoint>,
    private val durationMs: Long,
    private val syncOffsetMs: Long,
    overlaySizePx: Int,
    private val onProgress: (Float) -> Unit = {}
) : GlOverlayFilter() {

    private val currentFrame = AtomicInteger(0)
    private var exportSizePx = 0
    private val overlayView = MyView(context, null).apply {
        playMode = true
        setRpkPpk()
        measureExact(overlaySizePx.coerceAtLeast(1))
        layout(0, 0, measuredWidth, measuredHeight)
    }

    override fun setFrameSize(width: Int, height: Int) {
        super.setFrameSize(width, height)
        exportSizePx = ((minOf(width, height) * 0.25f)).toInt().coerceAtLeast(1)
        overlayView.measureExact(exportSizePx)
        overlayView.layout(0, 0, exportSizePx, exportSizePx)
        if (data.isNotEmpty()) {
            val first = data.first()
            overlayView.setReferenceQuat(first.q0, first.q1, first.q2, first.q3)
        }
    }

    override fun drawCanvas(canvas: Canvas) {
        val frameIndex = currentFrame.getAndIncrement()
        val timeMs = (((frameIndex * 1000L) / 30L) + syncOffsetMs).coerceAtMost(durationMs)
        val point = sampleAtTime(data, timeMs)
        overlayView.setQuats(point.q0, point.q1, point.q2, point.q3, invalidateView = false)
        val dx = 0f
        val dy = 0f
        canvas.save()
        canvas.translate(dx, dy)
        overlayView.draw(canvas)
        canvas.restore()

        val totalFrames = ((durationMs * 30L) / 1000L).coerceAtLeast(1L)
        val progress = ((frameIndex + 1).toFloat() / totalFrames.toFloat()).coerceIn(0f, 1f)
        onProgress(progress)
    }

    private fun sampleAtTime(data: List<DataPoint>, timeMs: Long): DataPoint {
        if (data.isEmpty()) {
            return DataPoint(timeMs, 0f, 0f, 0f, 1f)
        }
        if (data.size == 1) return data.first()

        var index = 0
        while (index + 1 < data.size && data[index + 1].time <= timeMs) {
            index++
        }

        val current = data[index]
        val next = if (index + 1 < data.size) data[index + 1] else null

        if (next == null || next.time <= current.time) {
            return current
        }

        val alpha = ((timeMs - current.time).toFloat() / (next.time - current.time).toFloat())
            .coerceIn(0f, 1f)

        return DataPoint(
            timeMs,
            lerp(current.q0, next.q0, alpha),
            lerp(current.q1, next.q1, alpha),
            lerp(current.q2, next.q2, alpha),
            lerp(current.q3, next.q3, alpha)
        )
    }

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    private fun android.view.View.measureExact(sizePx: Int) {
        measure(
            View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(sizePx, View.MeasureSpec.EXACTLY)
        )
    }
}
