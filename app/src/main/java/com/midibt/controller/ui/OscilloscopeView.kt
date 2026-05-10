package com.midibt.controller.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.ScaleGestureDetector
import android.view.MotionEvent
import android.view.View

class OscilloscopeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var zoomX = 1.0f // Controla la cantidad de ciclos visibles (frecuencia visual)
    private var zoomY = 1.0f // Controla la altura de la onda (amplitud visual)

    private val linePaint = Paint().apply {
        color = Color.parseColor("#00FF41")
        strokeWidth = 5f
        style = Paint.Style.STROKE
        isAntiAlias = true
        setShadowLayer(15f, 0f, 0f, Color.parseColor("#00FF41"))
    }

    private val gridPaint = Paint().apply {
        color = Color.parseColor("#1A331A")
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }

    private val path = Path()
    private var audioData: ShortArray? = null

    // Detector de gestos para Zoom
    private val scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            // Zoom horizontal (frecuencia)
            zoomX *= detector.scaleFactor
            zoomX = zoomX.coerceIn(0.5f, 10.0f)
            
            // Zoom vertical (amplitud) - Usamos una lógica similar
            // Si el pellizco es más vertical, podríamos separar los ejes, 
            // pero por simplicidad escalamos ambos proporcionalmente.
            zoomY *= detector.scaleFactor
            zoomY = zoomY.coerceIn(0.5f, 5.0f)
            
            invalidate()
            return true
        }
    })

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        return true
    }

    fun updateData(data: ShortArray) {
        this.audioData = data.copyOf()
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val midY = h / 2

        canvas.drawColor(Color.BLACK)
        drawGrid(canvas, w, h)

        val data = audioData ?: return
        if (data.isEmpty()) return

        path.reset()
        
        // Canal L únicamente
        val totalPoints = data.size / 2
        
        // Aplicamos Zoom X: Cuanto más alto zoomX, más "estirada" la onda (menos ciclos)
        // Calculamos cuántos puntos del buffer entran en el ancho de la pantalla
        val pointsToDisplay = (totalPoints / zoomX).toInt().coerceIn(10, totalPoints)
        val stepX = w / pointsToDisplay
        
        for (i in 0 until pointsToDisplay) {
            val x = i * stepX
            val rawValue = data[i * 2] / 32768f
            
            // Aplicamos Zoom Y (Amplitud)
            val y = midY + (rawValue * midY * zoomY)
            
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        canvas.drawPath(path, linePaint)
    }

    private fun drawGrid(canvas: Canvas, w: Float, h: Float) {
        val numCols = 10
        val numRows = 6
        for (i in 0..numCols) {
            val x = (w / numCols) * i
            canvas.drawLine(x, 0f, x, h, gridPaint)
        }
        for (i in 0..numRows) {
            val y = (h / numRows) * i
            canvas.drawLine(0f, y, w, y, gridPaint)
        }
        canvas.drawLine(0f, h/2, w, h/2, Paint().apply { 
            color = Color.parseColor("#336633")
            strokeWidth = 3f 
        })
    }
}
