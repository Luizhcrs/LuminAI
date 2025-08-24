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
 * 🎨 Beautiful Drawing View - Interface de desenho suave e elegante
 * 
 * Características:
 * - Desenho suave como caneta do Paint
 * - Efeitos visuais modernos (glow, sombra, gradiente)
 * - Animações fluidas
 * - Detecção inteligente de área
 */
class BeautifulDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "BeautifulDrawingView"
        private const val STROKE_WIDTH = 8f
        private const val GLOW_RADIUS = 20f
        private const val TOUCH_TOLERANCE = 4f
        private const val ANIMATION_DURATION = 300L
    }

    // 🎨 Pontos do desenho
    private val drawPoints = mutableListOf<PointF>()
    private val smoothPath = Path()
    private var currentX = 0f
    private var currentY = 0f
    
    // 🎨 Estados de desenho
    private var isDrawing = false
    private var isAnimatingComplete = false
    
    // 🎨 Paints para efeitos visuais
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#4285F4") // Azul Google
    }
    
    private val glowPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH + 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#804285F4") // Azul com transparência
        maskFilter = BlurMaskFilter(GLOW_RADIUS, BlurMaskFilter.Blur.NORMAL)
    }
    
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000") // Preto semi-transparente
        isAntiAlias = true
    }
    
    private val selectionPaint = Paint().apply {
        color = Color.parseColor("#204285F4") // Azul muito transparente
        isAntiAlias = true
    }
    
    // 🎨 Animação de conclusão
    private var completionAnimator: ValueAnimator? = null
    private var animationProgress = 0f
    
    // 📱 Callbacks
    private var onDrawingStartListener: (() -> Unit)? = null
    private var onDrawingProgressListener: ((List<PointF>) -> Unit)? = null
    private var onDrawingCompleteListener: ((Path, List<PointF>, RectF) -> Unit)? = null

    init {
        // Habilita desenho de sombras/blur
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (drawPoints.isNotEmpty()) {
            updateSmoothPath()
            
            if (isDrawing) {
                // 🎨 Enquanto desenha: apenas a linha suave com glow
                drawStrokeWithGlow(canvas)
            } else if (isAnimatingComplete) {
                // 🎨 Animação de conclusão
                drawCompletionAnimation(canvas)
            } else {
                // 🎨 Desenho finalizado: overlay + seleção + linha
                drawFinalSelection(canvas)
            }
        }
    }

    /**
     * 🎨 Desenha apenas o traço com efeito glow (durante o desenho)
     */
    private fun drawStrokeWithGlow(canvas: Canvas) {
        // Glow effect
        canvas.drawPath(smoothPath, glowPaint)
        // Main stroke
        canvas.drawPath(smoothPath, strokePaint)
    }

    /**
     * 🎨 Desenha a animação de conclusão
     */
    private fun drawCompletionAnimation(canvas: Canvas) {
        val animatedAlpha = (255 * animationProgress).toInt()
        
        // Overlay animado
        val animatedOverlay = Paint(overlayPaint).apply {
            alpha = animatedAlpha
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), animatedOverlay)
        
        // Área selecionada (transparente) com animação
        if (drawPoints.size > 2) {
            val closedPath = Path(smoothPath)
            closedPath.close()
            
            val save = canvas.save()
            canvas.clipPath(closedPath)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.restoreToCount(save)
            
            // Preenchimento sutil animado
            val animatedSelection = Paint(selectionPaint).apply {
                alpha = (128 * animationProgress).toInt()
            }
            canvas.drawPath(closedPath, animatedSelection)
        }
        
        // Linha principal sempre visível
        canvas.drawPath(smoothPath, glowPaint)
        canvas.drawPath(smoothPath, strokePaint)
    }

    /**
     * 🎨 Desenha a seleção final
     */
    private fun drawFinalSelection(canvas: Canvas) {
        // Overlay escuro
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
        
        if (drawPoints.size > 2) {
            val closedPath = Path(smoothPath)
            closedPath.close()
            
            // Remove overlay da área selecionada
            val save = canvas.save()
            canvas.clipPath(closedPath)
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            canvas.restoreToCount(save)
            
            // Preenchimento sutil na área selecionada
            canvas.drawPath(closedPath, selectionPaint)
        }
        
        // Linha principal com glow
        canvas.drawPath(smoothPath, glowPaint)
        canvas.drawPath(smoothPath, strokePaint)
    }

    /**
     * 🎨 Atualiza o path suavizado usando curvas Bézier
     */
    private fun updateSmoothPath() {
        if (drawPoints.size < 2) return
        
        smoothPath.reset()
        smoothPath.moveTo(drawPoints[0].x, drawPoints[0].y)
        
        for (i in 1 until drawPoints.size) {
            val point = drawPoints[i]
            
            if (i == 1) {
                // Primeira curva
                val midX = (drawPoints[0].x + point.x) / 2f
                val midY = (drawPoints[0].y + point.y) / 2f
                smoothPath.quadTo(drawPoints[0].x, drawPoints[0].y, midX, midY)
            } else {
                // Curvas suaves entre pontos
                val prevPoint = drawPoints[i - 1]
                val midX = (prevPoint.x + point.x) / 2f
                val midY = (prevPoint.y + point.y) / 2f
                smoothPath.quadTo(prevPoint.x, prevPoint.y, midX, midY)
            }
        }
        
        // Linha até o último ponto
        if (drawPoints.size > 1) {
            val lastPoint = drawPoints.last()
            smoothPath.lineTo(lastPoint.x, lastPoint.y)
        }
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
        clearDrawing()
        isDrawing = true
        currentX = x
        currentY = y
        
        drawPoints.add(PointF(x, y))
        onDrawingStartListener?.invoke()
        invalidate()
    }

    /**
     * 🎨 Continua o desenho
     */
    private fun continueDrawing(x: Float, y: Float) {
        if (!isDrawing) return
        
        val dx = abs(x - currentX)
        val dy = abs(y - currentY)
        
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            drawPoints.add(PointF(x, y))
            currentX = x
            currentY = y
            
            onDrawingProgressListener?.invoke(drawPoints.toList())
            invalidate()
        }
    }

    /**
     * 🎨 Finaliza o desenho com animação suave
     */
    private fun finishDrawing() {
        if (!isDrawing || drawPoints.size < 3) {
            clearDrawing()
            return
        }
        
        isDrawing = false
        
        // Inicia animação de conclusão
        startCompletionAnimation()
    }

    /**
     * ✨ Inicia animação suave de conclusão
     */
    private fun startCompletionAnimation() {
        isAnimatingComplete = true
        
        completionAnimator?.cancel()
        completionAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animator ->
                animationProgress = animator.animatedValue as Float
                invalidate()
            }
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    isAnimatingComplete = false
                    
                    // Calcula bounds da área selecionada
                    val bounds = calculateDrawingBounds()
                    
                    // Notifica conclusão
                    onDrawingCompleteListener?.invoke(smoothPath, drawPoints.toList(), bounds)
                    invalidate()
                }
            })
            
            start()
        }
    }

    /**
     * 📐 Calcula os bounds da área desenhada
     */
    private fun calculateDrawingBounds(): RectF {
        if (drawPoints.isEmpty()) return RectF()
        
        var minX = drawPoints[0].x
        var maxX = drawPoints[0].x
        var minY = drawPoints[0].y
        var maxY = drawPoints[0].y
        
        for (point in drawPoints) {
            minX = min(minX, point.x)
            maxX = max(maxX, point.x)
            minY = min(minY, point.y)
            maxY = max(maxY, point.y)
        }
        
        return RectF(minX, minY, maxX, maxY)
    }

    /**
     * 🧹 Limpa o desenho
     */
    fun clearDrawing() {
        drawPoints.clear()
        smoothPath.reset()
        isDrawing = false
        isAnimatingComplete = false
        completionAnimator?.cancel()
        animationProgress = 0f
        invalidate()
    }

    /**
     * 📱 Setters para callbacks
     */
    fun setOnDrawingStartListener(listener: () -> Unit) {
        onDrawingStartListener = listener
    }

    fun setOnDrawingProgressListener(listener: (List<PointF>) -> Unit) {
        onDrawingProgressListener = listener
    }

    fun setOnDrawingCompleteListener(listener: (Path, List<PointF>, RectF) -> Unit) {
        onDrawingCompleteListener = listener
    }

    /**
     * 🎯 Obtém bounds da área desenhada
     */
    fun getDrawingBounds(): RectF = calculateDrawingBounds()

    /**
     * 🎨 Verifica se tem desenho ativo
     */
    fun hasDrawing(): Boolean = drawPoints.isNotEmpty()
}
