package com.example.floatingbutton.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.*

/**
 * 🔲 Smart Rectangle Drawing View - Desenho livre que se completa como retângulo
 * 
 * Características:
 * - Desenho livre inicial
 * - Detecção inteligente de intenção retangular
 * - Completamento automático para retângulo perfeito
 * - Animações suaves de transição
 * - Visual moderno e elegante
 */
class SmartRectangleDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "SmartRectangleDrawing"
        private const val STROKE_WIDTH = 6f
        private const val GLOW_RADIUS = 16f
        private const val TOUCH_TOLERANCE = 8f
        private const val ANIMATION_DURATION = 400L
        private const val MIN_RECTANGLE_SIZE = 100f
    }

    // 🎨 Pontos do desenho livre
    private val freeDrawPoints = mutableListOf<PointF>()
    private val freeDrawPath = Path()
    
    // 🔲 Retângulo inteligente
    private var smartRectangle: RectF? = null
    private var isShowingRectangle = false
    private var rectangleAnimationProgress = 0f
    
    // 🎨 Estados
    private var isDrawing = false
    private var isAnimatingToRectangle = false
    
    // 🎨 Paints elegantes
    private val freeDrawPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#4285F4")
        alpha = 180 // Mais transparente durante desenho livre
    }
    
    private val rectanglePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH + 2f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#4285F4")
    }
    
    private val rectangleGlowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH + 8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#804285F4")
        maskFilter = BlurMaskFilter(GLOW_RADIUS, BlurMaskFilter.Blur.NORMAL)
    }
    
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000")
        isAntiAlias = true
    }
    
    private val selectionFillPaint = Paint().apply {
        color = Color.parseColor("#204285F4")
        isAntiAlias = true
    }
    
    // 🎯 Animadores
    private var rectangleAnimator: ValueAnimator? = null
    
    // 📱 Callbacks
    private var onDrawingStartListener: (() -> Unit)? = null
    private var onRectangleDetectedListener: ((RectF) -> Unit)? = null
    private var onSelectionCompleteListener: ((RectF) -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        when {
            isDrawing -> drawFreeDrawing(canvas)
            isAnimatingToRectangle -> drawRectangleTransition(canvas)
            isShowingRectangle -> drawFinalRectangle(canvas)
        }
    }

    /**
     * 🎨 Desenha o traçado livre
     */
    private fun drawFreeDrawing(canvas: Canvas) {
        if (freeDrawPoints.isNotEmpty()) {
            updateFreeDrawPath()
            canvas.drawPath(freeDrawPath, freeDrawPaint)
            
            // Mostra preview do retângulo potencial (muito sutil)
            val previewRect = calculatePotentialRectangle()
            if (previewRect != null && isRectangleViable(previewRect)) {
                val previewPaint = Paint(rectanglePaint).apply {
                    alpha = 60 // Muito transparente
                    pathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
                }
                canvas.drawRect(previewRect, previewPaint)
            }
        }
    }

    /**
     * ✨ Desenha a transição animada para retângulo
     */
    private fun drawRectangleTransition(canvas: Canvas) {
        val progress = rectangleAnimationProgress
        val rect = smartRectangle ?: return
        
        // Desenha overlay com fade-in
        val overlayAlpha = (255 * progress * 0.8f).toInt()
        val animatedOverlay = Paint(overlayPaint).apply { alpha = overlayAlpha }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), animatedOverlay)
        
        // Área selecionada transparente
        val save = canvas.save()
        canvas.clipRect(rect)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.restoreToCount(save)
        
        // Preenchimento sutil animado
        val fillAlpha = (128 * progress).toInt()
        val animatedFill = Paint(selectionFillPaint).apply { alpha = fillAlpha }
        canvas.drawRect(rect, animatedFill)
        
        // Transição do desenho livre para retângulo
        if (progress < 0.7f) {
            // Ainda mostra o desenho livre, mas fade out
            val freeAlpha = (255 * (1f - progress / 0.7f)).toInt()
            val fadingFreeDraw = Paint(freeDrawPaint).apply { alpha = freeAlpha }
            updateFreeDrawPath()
            canvas.drawPath(freeDrawPath, fadingFreeDraw)
        }
        
        // Retângulo com fade in
        val rectAlpha = (255 * max(0f, (progress - 0.3f) / 0.7f)).toInt()
        val animatedRect = Paint(rectanglePaint).apply { alpha = rectAlpha }
        val animatedGlow = Paint(rectangleGlowPaint).apply { alpha = (rectAlpha * 0.6f).toInt() }
        
        canvas.drawRect(rect, animatedGlow)
        canvas.drawRect(rect, animatedRect)
    }

    /**
     * 🔲 Desenha o retângulo final
     */
    private fun drawFinalRectangle(canvas: Canvas) {
        val rect = smartRectangle ?: return
        
        // Overlay escuro
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        
        // Área selecionada transparente
        val save = canvas.save()
        canvas.clipRect(rect)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        canvas.restoreToCount(save)
        
        // Preenchimento sutil
        canvas.drawRect(rect, selectionFillPaint)
        
        // Borda do retângulo com glow
        canvas.drawRect(rect, rectangleGlowPaint)
        canvas.drawRect(rect, rectanglePaint)
        
        // Cantos arredondados sutis (opcional)
        drawRectangleCorners(canvas, rect)
    }

    /**
     * ✨ Desenha cantos arredondados no retângulo
     */
    private fun drawRectangleCorners(canvas: Canvas, rect: RectF) {
        val cornerSize = 12f
        val cornerPaint = Paint(rectanglePaint).apply {
            strokeWidth = STROKE_WIDTH + 4f
        }
        
        // Cantos superiores
        canvas.drawLine(rect.left, rect.top + cornerSize, rect.left, rect.top, cornerPaint)
        canvas.drawLine(rect.left, rect.top, rect.left + cornerSize, rect.top, cornerPaint)
        
        canvas.drawLine(rect.right - cornerSize, rect.top, rect.right, rect.top, cornerPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerSize, cornerPaint)
        
        // Cantos inferiores
        canvas.drawLine(rect.left, rect.bottom - cornerSize, rect.left, rect.bottom, cornerPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left + cornerSize, rect.bottom, cornerPaint)
        
        canvas.drawLine(rect.right - cornerSize, rect.bottom, rect.right, rect.bottom, cornerPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerSize, cornerPaint)
    }

    /**
     * 🎨 Atualiza o path do desenho livre
     */
    private fun updateFreeDrawPath() {
        if (freeDrawPoints.size < 2) return
        
        freeDrawPath.reset()
        freeDrawPath.moveTo(freeDrawPoints[0].x, freeDrawPoints[0].y)
        
        for (i in 1 until freeDrawPoints.size) {
            val point = freeDrawPoints[i]
            freeDrawPath.lineTo(point.x, point.y)
        }
    }

    /**
     * 🧠 Calcula o retângulo potencial baseado nos pontos
     */
    private fun calculatePotentialRectangle(): RectF? {
        if (freeDrawPoints.size < 3) return null
        
        var minX = freeDrawPoints[0].x
        var maxX = freeDrawPoints[0].x
        var minY = freeDrawPoints[0].y
        var maxY = freeDrawPoints[0].y
        
        for (point in freeDrawPoints) {
            minX = min(minX, point.x)
            maxX = max(maxX, point.x)
            minY = min(minY, point.y)
            maxY = max(maxY, point.y)
        }
        
        // Adiciona uma margem pequena
        val margin = 20f
        return RectF(
            max(0f, minX - margin),
            max(0f, minY - margin),
            min(width.toFloat(), maxX + margin),
            min(height.toFloat(), maxY + margin)
        )
    }

    /**
     * ✅ Verifica se o retângulo é viável
     */
    private fun isRectangleViable(rect: RectF): Boolean {
        val width = rect.width()
        val height = rect.height()
        
        return width >= MIN_RECTANGLE_SIZE && 
               height >= MIN_RECTANGLE_SIZE &&
               width <= this.width * 0.9f &&
               height <= this.height * 0.9f
    }

    /**
     * 🧠 Detecta se o usuário tem intenção retangular
     */
    private fun detectRectangularIntent(): Boolean {
        if (freeDrawPoints.size < 5) return false
        
        val rect = calculatePotentialRectangle() ?: return false
        if (!isRectangleViable(rect)) return false
        
        // Verifica se os pontos cobrem as bordas do retângulo
        val tolerance = 50f
        var coverageScore = 0f
        
        // Verifica cobertura das bordas
        val edges = listOf(
            Pair(PointF(rect.left, rect.top), PointF(rect.right, rect.top)), // Top
            Pair(PointF(rect.right, rect.top), PointF(rect.right, rect.bottom)), // Right
            Pair(PointF(rect.right, rect.bottom), PointF(rect.left, rect.bottom)), // Bottom
            Pair(PointF(rect.left, rect.bottom), PointF(rect.left, rect.top)) // Left
        )
        
        for (edge in edges) {
            var edgeCovered = false
            for (point in freeDrawPoints) {
                val distToEdge = distancePointToLineSegment(point, edge.first, edge.second)
                if (distToEdge <= tolerance) {
                    edgeCovered = true
                    break
                }
            }
            if (edgeCovered) coverageScore += 0.25f
        }
        
        return coverageScore >= 0.5f // Pelo menos 2 bordas cobertas
    }

    /**
     * 📐 Calcula distância de ponto para segmento de linha
     */
    private fun distancePointToLineSegment(point: PointF, lineStart: PointF, lineEnd: PointF): Float {
        val A = point.x - lineStart.x
        val B = point.y - lineStart.y
        val C = lineEnd.x - lineStart.x
        val D = lineEnd.y - lineStart.y
        
        val dot = A * C + B * D
        val lenSq = C * C + D * D
        
        if (lenSq == 0f) return sqrt(A * A + B * B)
        
        val param = dot / lenSq
        
        val (xx, yy) = when {
            param < 0 -> lineStart.x to lineStart.y
            param > 1 -> lineEnd.x to lineEnd.y
            else -> lineStart.x + param * C to lineStart.y + param * D
        }
        
        val dx = point.x - xx
        val dy = point.y - yy
        return sqrt(dx * dx + dy * dy)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startDrawing(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                continueDrawing(x, y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                finishDrawing()
                return true
            }
        }
        return false
    }

    /**
     * 🎨 Inicia o desenho
     */
    private fun startDrawing(x: Float, y: Float) {
        clearSelection()
        isDrawing = true
        freeDrawPoints.add(PointF(x, y))
        onDrawingStartListener?.invoke()
        invalidate()
    }

    /**
     * 🎨 Continua o desenho
     */
    private fun continueDrawing(x: Float, y: Float) {
        if (!isDrawing) return
        
        val lastPoint = freeDrawPoints.lastOrNull() ?: return
        val dx = abs(x - lastPoint.x)
        val dy = abs(y - lastPoint.y)
        
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            freeDrawPoints.add(PointF(x, y))
            invalidate()
        }
    }

    /**
     * ✨ Finaliza o desenho e detecta intenção
     */
    private fun finishDrawing() {
        if (!isDrawing || freeDrawPoints.size < 3) {
            clearSelection()
            return
        }
        
        isDrawing = false
        
        // Detecta se tem intenção retangular
        if (detectRectangularIntent()) {
            val rect = calculatePotentialRectangle()
            if (rect != null && isRectangleViable(rect)) {
                smartRectangle = rect
                onRectangleDetectedListener?.invoke(rect)
                animateToRectangle()
                return
            }
        }
        
        // Se não detectou intenção retangular, limpa
        clearSelection()
    }

    /**
     * ✨ Anima transição para retângulo
     */
    private fun animateToRectangle() {
        isAnimatingToRectangle = true
        
        rectangleAnimator?.cancel()
        rectangleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                rectangleAnimationProgress = animator.animatedValue as Float
                invalidate()
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimatingToRectangle = false
                    isShowingRectangle = true
                    
                    smartRectangle?.let { rect ->
                        onSelectionCompleteListener?.invoke(rect)
                    }
                    invalidate()
                }
            })
            
            start()
        }
    }

    /**
     * 🧹 Limpa a seleção
     */
    fun clearSelection() {
        freeDrawPoints.clear()
        freeDrawPath.reset()
        smartRectangle = null
        isDrawing = false
        isAnimatingToRectangle = false
        isShowingRectangle = false
        rectangleAnimationProgress = 0f
        rectangleAnimator?.cancel()
        invalidate()
    }

    /**
     * 📱 Callbacks
     */
    fun setOnDrawingStartListener(listener: () -> Unit) {
        onDrawingStartListener = listener
    }

    fun setOnRectangleDetectedListener(listener: (RectF) -> Unit) {
        onRectangleDetectedListener = listener
    }

    fun setOnSelectionCompleteListener(listener: (RectF) -> Unit) {
        onSelectionCompleteListener = listener
    }

    /**
     * 🎯 Getters
     */
    fun getSelectedRectangle(): RectF? = smartRectangle
    fun hasSelection(): Boolean = isShowingRectangle
}
