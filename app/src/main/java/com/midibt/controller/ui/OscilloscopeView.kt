package com.midibt.controller.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class OscilloscopeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 4f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val path = Path()
    private var audioData: ShortArray? = null

    fun updateData(data: ShortArray) {
        // Tomamos una muestra (el canal izquierdo solamente para simplificar el gráfico)
        this.audioData = data
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val data = audioData ?: return
        val w = width.toFloat()
        val h = height.toFloat()
        val midY = h / 2

        path.reset()
        
        // Dibujamos la línea base
        canvas.drawLine(0f, midY, w, midY, Paint().apply { color = Color.DKGRAY })

        if (data.isEmpty()) return

        // Dibujamos la onda
        // Saltamos de 2 en 2 porque el buffer es estéreo (L, R, L, R...)
        val step = w / (data.size / 2)
        path.moveTo(0f, midY)

        for (i in 0 until data.size / 2) {
            val x = i * step
            // Normalizamos el valor short (-32768 a 32767) al alto de la vista
            val y = midY + (data[i * 2] / 32768f) * midY
            path.lineTo(x, y)
        }

        canvas.drawPath(path, paint)
    }
}
