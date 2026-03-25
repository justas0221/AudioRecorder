package com.example.audiorecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.sqrt

class WaveformView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private var paint = Paint()
    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()

    private var radius = 6f
    private var w = 9f
    private var d = 6f

    init {
        paint.color = Color.rgb(244, 81, 30) // Match the record button color (Orange-Red)
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
    }

    fun addAmplitude(amp: Float) {
        val sensitiveAmp = (amp * 1.5f).coerceAtMost(32767f)
        val normAmp = sqrt(sensitiveAmp.coerceAtLeast(0f) / 32767f) * (height.toFloat() * 1.0f)
        
        val finalAmp = normAmp.coerceAtLeast(4f)
        
        amplitudes.add(finalAmp)

        spikes.clear()
        val maxSpikes = (width / (w + d)).toInt()
        
        val amps = amplitudes.takeLast(maxSpikes)

        for (i in amps.indices) {
            val left = width.toFloat() - (amps.size - i) * (w + d)
            val top = (height / 2f) - (amps[i] / 2f)
            val right = left + w
            val bottom = (height / 2f) + (amps[i] / 2f)
            spikes.add(RectF(left, top, right, bottom))
        }

        invalidate() // This triggers onDraw
    }
    
    fun clear() {
        amplitudes.clear()
        spikes.clear()
        invalidate()
    }

    fun clearAndGetAmplitudes() : ArrayList<Float>{
        val amps = amplitudes.clone() as ArrayList<Float>
        amplitudes.clear()
        spikes.clear()
        invalidate()
        return amps
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        spikes.forEach {
            canvas.drawRoundRect(it, radius, radius, paint)
        }
    }
}
