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
 * 🎨 Elegant Selection View - Seleção retangular elegante e redimensionável
 * 
 * Características:
 * - 🔲 Retângulo com bordas elegantes arredondadas
 * - 🎯 Redimensionamento por arrastar cantos/bordas
 * - ✨ Efeitos visuais modernos
 * - 🎨 Gradientes sutis
 * - 📱 Responsivo e intuitivo
 */
class ElegantSelectionView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "ElegantSelection"
        private const val CORNER_RADIUS = 24f // 🎨 Bordas otimizadas
        private const val STROKE_WIDTH = 1.5f // 🔥 Ultra fino
        private const val GLOW_WIDTH = 3f // 🔥 Glow mínimo
        private const val HANDLE_SIZE = 18f // 🔥 Menor
        private const val TOUCH_TOLERANCE = 30f
        private const val MIN_SIZE = 80f
    }

    // 🔲 Retângulo de seleção
    private var selectionRect: RectF? = null
    private var isVisible = false
    
    // 🎯 Sistema de redimensionamento
    private enum class ResizeMode {
        NONE, MOVE, 
        RESIZE_TOP_LEFT, RESIZE_TOP_RIGHT, 
        RESIZE_BOTTOM_LEFT, RESIZE_BOTTOM_RIGHT
        // 🎯 Removidos laterais para simplicidade
    }
    
    private var currentResizeMode = ResizeMode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    
    // 🎨 Paints elegantes
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.WHITE
    }
    
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = GLOW_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = Color.parseColor("#40FFFFFF")
        maskFilter = BlurMaskFilter(4f, BlurMaskFilter.Blur.OUTER)
    }
    
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.WHITE
    }
    
    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#4285F4")
    }

    // 🎭 Animação
    private var animator: ValueAnimator? = null
    private var animationProgress = 0f
    
    // 📞 Callbacks
    var onSelectionChanged: ((RectF) -> Unit)? = null
    var onSelectionCompleted: ((RectF) -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Para BlurMaskFilter
    }

    /**
     * 🎨 Desenha a seleção elegante
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isVisible || selectionRect == null) return
        
        val rect = selectionRect!!
        
        // ✨ Glow removido para melhor performance
        
        // 🔲 Desenha retângulo principal
        drawMainRectangle(canvas, rect)
        
        // 🎯 Handles removidos - cantos brancos são suficientes
    }

    /**
     * ✨ Desenha brilho ao redor do retângulo
     */
    private fun drawGlow(canvas: Canvas, rect: RectF) {
        val cornerRadius = CORNER_RADIUS + GLOW_WIDTH / 2
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, glowPaint)
    }

    /**
     * 🔲 Desenha retângulo principal com cantos arredondados
     */
    private fun drawMainRectangle(canvas: Canvas, rect: RectF) {
        // 🌈 Nova paleta de cores conectada
        // 🎨 Borda principal mais visível
        val primaryPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f // 🎯 Ainda mais visível
            color = Color.parseColor("#FF069E6E") // Verde água sólido
        }
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, primaryPaint)
        
        // ✨ Cantos brancos elegantes
        drawCornerAccents(canvas, rect)
    }

    /**
     * 🎯 Desenha handles de redimensionamento
     */
    private fun drawResizeHandles(canvas: Canvas, rect: RectF) {
        val handles = listOf(
            // Cantos
            PointF(rect.left, rect.top),           // Top-left
            PointF(rect.right, rect.top),          // Top-right
            PointF(rect.left, rect.bottom),        // Bottom-left
            PointF(rect.right, rect.bottom),       // Bottom-right
            // Lados
            PointF(rect.centerX(), rect.top),      // Top
            PointF(rect.centerX(), rect.bottom),   // Bottom
            PointF(rect.left, rect.centerY()),     // Left
            PointF(rect.right, rect.centerY())     // Right
        )
        
        handles.forEach { handle ->
            // 🎯 Handle minimalista (sem sombra para performance)
            val simplePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.FILL
            }
            canvas.drawCircle(handle.x, handle.y, HANDLE_SIZE / 2, simplePaint)
            
            // 🔥 Borda colorida simples
            val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FF069E6E") // Verde água
                style = Paint.Style.STROKE
                strokeWidth = 2f // 🎯 Mais fino
            }
            canvas.drawCircle(handle.x, handle.y, HANDLE_SIZE / 2, strokePaint)
        }
    }

    /**
     * 🖱️ Gerencia eventos de toque para redimensionamento
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isVisible || selectionRect == null) return false
        
        val x = event.x
        val y = event.y
        
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentResizeMode = detectResizeMode(x, y)
                lastTouchX = x
                lastTouchY = y
                return currentResizeMode != ResizeMode.NONE
            }
            
            MotionEvent.ACTION_MOVE -> {
                if (currentResizeMode != ResizeMode.NONE) {
                    handleResize(x, y)
                    return true
                }
            }
            
            MotionEvent.ACTION_UP -> {
                if (currentResizeMode != ResizeMode.NONE) {
                    currentResizeMode = ResizeMode.NONE
                    onSelectionCompleted?.invoke(selectionRect!!)
                    return true
                }
            }
        }
        
        return false
    }

    /**
     * 🎯 Detecta modo de redimensionamento baseado no toque
     */
    private fun detectResizeMode(x: Float, y: Float): ResizeMode {
        val rect = selectionRect ?: return ResizeMode.NONE
        
        // Verifica handles dos cantos
        if (isNearPoint(x, y, rect.left, rect.top)) return ResizeMode.RESIZE_TOP_LEFT
        if (isNearPoint(x, y, rect.right, rect.top)) return ResizeMode.RESIZE_TOP_RIGHT
        if (isNearPoint(x, y, rect.left, rect.bottom)) return ResizeMode.RESIZE_BOTTOM_LEFT
        if (isNearPoint(x, y, rect.right, rect.bottom)) return ResizeMode.RESIZE_BOTTOM_RIGHT
        
        // 🎯 Handles das bordas removidos para simplicidade
        
        // Verifica se está dentro do retângulo (mover)
        if (rect.contains(x, y)) return ResizeMode.MOVE
        
        return ResizeMode.NONE
    }

    /**
     * 🎯 Verifica se o toque está próximo de um ponto
     */
    private fun isNearPoint(x: Float, y: Float, pointX: Float, pointY: Float): Boolean {
        val distance = sqrt((x - pointX).pow(2) + (y - pointY).pow(2))
        return distance <= TOUCH_TOLERANCE
    }

    /**
     * 🔄 Processa redimensionamento
     */
    private fun handleResize(x: Float, y: Float) {
        val rect = selectionRect ?: return
        val deltaX = x - lastTouchX
        val deltaY = y - lastTouchY
        
        val newRect = RectF(rect)
        
        when (currentResizeMode) {
            ResizeMode.MOVE -> {
                newRect.offset(deltaX, deltaY)
            }
            ResizeMode.RESIZE_TOP_LEFT -> {
                newRect.left += deltaX
                newRect.top += deltaY
            }
            ResizeMode.RESIZE_TOP_RIGHT -> {
                newRect.right += deltaX
                newRect.top += deltaY
            }
            ResizeMode.RESIZE_BOTTOM_LEFT -> {
                newRect.left += deltaX
                newRect.bottom += deltaY
            }
            ResizeMode.RESIZE_BOTTOM_RIGHT -> {
                newRect.right += deltaX
                newRect.bottom += deltaY
            }
            // 🎯 Handles das laterais removidos para simplicidade
            else -> return
        }
        
        // 📏 Valida tamanho mínimo
        if (newRect.width() >= MIN_SIZE && newRect.height() >= MIN_SIZE) {
            // 📱 Garante que fica dentro da tela
            newRect.left = newRect.left.coerceAtLeast(0f)
            newRect.top = newRect.top.coerceAtLeast(0f)
            newRect.right = newRect.right.coerceAtMost(width.toFloat())
            newRect.bottom = newRect.bottom.coerceAtMost(height.toFloat())
            
            selectionRect = newRect
            onSelectionChanged?.invoke(newRect)
            invalidate()
        }
        
        lastTouchX = x
        lastTouchY = y
    }

    /**
     * 🎨 Define retângulo de seleção
     */
    fun setSelection(rect: RectF) {
        selectionRect = RectF(rect)
        isVisible = true
        visibility = View.VISIBLE // 🔥 Força visibilidade
        
        // Seleção definida silenciosamente
        
        // ✨ Sem animação para melhor performance
        
        invalidate()
        requestLayout() // 🔥 Força redesenho
    }
    
    /**
     * ✨ Desenha acentos arredondados nos cantos (design elegante)
     */
    private fun drawCornerAccents(canvas: Canvas, rect: RectF) {
        val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 4f
            color = Color.WHITE
            strokeCap = Paint.Cap.ROUND
        }
        
        val accentLength = 30f
        val cornerRadius = CORNER_RADIUS
        
        // 🎨 Canto superior esquerdo (arredondado)
        val topLeftPath = android.graphics.Path().apply {
            moveTo(rect.left, rect.top + accentLength)
            lineTo(rect.left, rect.top + cornerRadius)
            quadTo(rect.left, rect.top, rect.left + cornerRadius, rect.top)
            lineTo(rect.left + accentLength, rect.top)
        }
        canvas.drawPath(topLeftPath, accentPaint)
        
        // 🎨 Canto superior direito (arredondado)
        val topRightPath = android.graphics.Path().apply {
            moveTo(rect.right - accentLength, rect.top)
            lineTo(rect.right - cornerRadius, rect.top)
            quadTo(rect.right, rect.top, rect.right, rect.top + cornerRadius)
            lineTo(rect.right, rect.top + accentLength)
        }
        canvas.drawPath(topRightPath, accentPaint)
        
        // 🎨 Canto inferior esquerdo (arredondado)
        val bottomLeftPath = android.graphics.Path().apply {
            moveTo(rect.left, rect.bottom - accentLength)
            lineTo(rect.left, rect.bottom - cornerRadius)
            quadTo(rect.left, rect.bottom, rect.left + cornerRadius, rect.bottom)
            lineTo(rect.left + accentLength, rect.bottom)
        }
        canvas.drawPath(bottomLeftPath, accentPaint)
        
        // 🎨 Canto inferior direito (arredondado)
        val bottomRightPath = android.graphics.Path().apply {
            moveTo(rect.right - accentLength, rect.bottom)
            lineTo(rect.right - cornerRadius, rect.bottom)
            quadTo(rect.right, rect.bottom, rect.right, rect.bottom - cornerRadius)
            lineTo(rect.right, rect.bottom - accentLength)
        }
        canvas.drawPath(bottomRightPath, accentPaint)
    }

    /**
     * 📍 Retorna retângulo de seleção atual
     */
    fun getSelectionRect(): RectF? = selectionRect

    /**
     * 🧹 Limpa seleção
     */
    fun clearSelection() {
        isVisible = false
        selectionRect = null
        animator?.cancel()
        invalidate()
    }

    /**
     * ✨ Animação de entrada
     */
    private fun animateIn() {
        animator?.cancel()
        
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 300L
            interpolator = DecelerateInterpolator()
            
            addUpdateListener {
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            
            start()
        }
    }

    /**
     * 📐 Obtém retângulo atual
     */
    fun getCurrentSelection(): RectF? = selectionRect

    /**
     * 🎯 Verifica se há seleção ativa
     */
    fun hasSelection(): Boolean = isVisible && selectionRect != null
}
