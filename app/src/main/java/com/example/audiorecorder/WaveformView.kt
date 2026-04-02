package com.example.audiorecorder

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class WaveformView(context: Context, attrs: AttributeSet?) : View(context, attrs) {

    private val paint = Paint()
    private var amplitudes = ArrayList<Float>()
    private var spikes = ArrayList<RectF>()

    private val radius = 6f
    private val w = 9f
    private val d = 6f

    init {
        paint.color = Color.rgb(244, 81, 30)
        paint.style = Paint.Style.FILL
        paint.isAntiAlias = true
    }

    fun addAmplitude(amp: Float) {
        // High sensitivity scaling
        val normAmp = (amp / 32767f) * height.toFloat() * 5.0f
        val finalAmp = normAmp.coerceIn(10f, height.toFloat())
        
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

        invalidate()
    }
    
    fun clear() {
        amplitudes.clear()
        spikes.clear()
        invalidate()
    }

    fun clearAndGetAmplitudes() : ArrayList<Float>{
        val amps = ArrayList(amplitudes)
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
