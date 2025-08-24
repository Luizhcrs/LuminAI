package com.example.floatingbutton

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class CircularCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CircularCropView"
        private const val MIN_CIRCLE_RADIUS = 50f
        private const val STROKE_WIDTH = 4f
        private const val HANDLE_RADIUS = 20f
    }

    private var centerX = 0f
    private var centerY = 0f
    private var radius = 100f
    private var isDragging = false
    private var isResizing = false
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var originalRadius = 0f
    private var originalCenterX = 0f
    private var originalCenterY = 0f

    private val circlePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        isAntiAlias = true
    }

    private val handlePaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val cropPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var onCropChangedListener: ((Float, Float, Float) -> Unit)? = null

    fun setOnCropChangedListener(listener: (Float, Float, Float) -> Unit) {
        onCropChangedListener = listener
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        // Centraliza o círculo na tela
        centerX = w / 2f
        centerY = h / 2f
        radius = min(w, h) / 4f
        notifyCropChanged()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        // Salva o estado do canvas
        val save = canvas.save()
        
        // Desenha a área escura ao redor do círculo
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), cropPaint)
        
        // Cria um path circular para recortar
        val path = Path()
        path.addCircle(centerX, centerY, radius, Path.Direction.CW)
        
        // Aplica o clipping invertido (área fora do círculo)
        canvas.clipOutPath(path)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), cropPaint)
        
        // Restaura o canvas
        canvas.restoreToCount(save)
        
        // Desenha o círculo de recorte
        canvas.drawCircle(centerX, centerY, radius, circlePaint)
        
        // Desenha as alças de redimensionamento
        val handleDistance = radius + HANDLE_RADIUS
        canvas.drawCircle(centerX + handleDistance, centerY, HANDLE_RADIUS, handlePaint)
        canvas.drawCircle(centerX - handleDistance, centerY, HANDLE_RADIUS, handlePaint)
        canvas.drawCircle(centerX, centerY + handleDistance, HANDLE_RADIUS, handlePaint)
        canvas.drawCircle(centerX, centerY - handleDistance, HANDLE_RADIUS, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleTouchDown(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                handleTouchMove(x, y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                handleTouchUp()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun handleTouchDown(x: Float, y: Float) {
        val distanceFromCenter = sqrt((x - centerX).pow(2) + (y - centerY).pow(2))
        val distanceFromHandle = abs(distanceFromCenter - radius)

        if (distanceFromHandle <= HANDLE_RADIUS * 2) {
            // Toca em uma alça de redimensionamento
            isResizing = true
            originalRadius = radius
            dragStartX = x
            dragStartY = y
            Log.d(TAG, "handleTouchDown: Iniciando redimensionamento")
        } else if (distanceFromCenter <= radius) {
            // Toca dentro do círculo
            isDragging = true
            originalCenterX = centerX
            originalCenterY = centerY
            dragStartX = x
            dragStartY = y
            Log.d(TAG, "handleTouchDown: Iniciando arraste")
        }
    }

    private fun handleTouchMove(x: Float, y: Float) {
        if (isDragging) {
            // Move o círculo
            val deltaX = x - dragStartX
            val deltaY = y - dragStartY
            
            centerX = (originalCenterX + deltaX).coerceIn(radius, width - radius)
            centerY = (originalCenterY + deltaY).coerceIn(radius, height - radius)
            
            Log.d(TAG, "handleTouchMove: Movendo círculo para ($centerX, $centerY)")
            invalidate()
            notifyCropChanged()
        } else if (isResizing) {
            // Redimensiona o círculo
            val deltaX = x - dragStartX
            val deltaY = y - dragStartY
            val delta = sqrt(deltaX.pow(2) + deltaY.pow(2))
            
            // Determina se está aumentando ou diminuindo
            val direction = if (deltaX + deltaY > 0) 1 else -1
            
            radius = (originalRadius + delta * direction).coerceIn(MIN_CIRCLE_RADIUS, min(width, height) / 2f)
            
            Log.d(TAG, "handleTouchMove: Redimensionando para raio $radius")
            invalidate()
            notifyCropChanged()
        }
    }

    private fun handleTouchUp() {
        isDragging = false
        isResizing = false
        Log.d(TAG, "handleTouchUp: Finalizando interação")
    }

    private fun notifyCropChanged() {
        onCropChangedListener?.invoke(centerX, centerY, radius)
    }

    // Retorna as coordenadas do retângulo de recorte
    fun getCropRect(): RectF {
        val left = centerX - radius
        val top = centerY - radius
        val right = centerX + radius
        val bottom = centerY + radius
        return RectF(left, top, right, bottom)
    }

    // Retorna o centro e raio do círculo
    fun getCropCircle(): Triple<Float, Float, Float> {
        return Triple(centerX, centerY, radius)
    }
}
