package com.example.floatingbutton.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.appcompat.widget.AppCompatImageView
import kotlin.math.sin
import kotlin.math.PI

/**
 * 🎨 Minimal Image View - ImageView minimalista com efeitos sutis nos cantos
 * 
 * Características:
 * - 🌟 Gradientes sutis apenas nos 4 cantos
 * - 💫 Animação suave de pulsação durante processamento
 * - ⚡ Otimizado para performance
 * - 🎯 Efeitos visuais mínimos mas elegantes
 */
class MinimalImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        private const val CORNER_SIZE = 350f // 🔥 MUITO maior e visível
        private const val GLOW_ALPHA = 1.0f // 🔥 Alpha máximo
        private const val INNER_GLOW_SIZE = 180f // 🌟 Gradiente interno bem maior
        private const val ANIMATION_DURATION = 2500L
    }

    // 🎯 Estados visuais simples
    enum class State {
        NORMAL,      // Estado padrão com gradientes sutis
        PROCESSING,  // Processando com pulsação
        SUCCESS,     // Sucesso com brilho verde
        ERROR        // Erro com brilho vermelho
    }

    // 🎨 Estado atual
    private var currentState = State.NORMAL
    private var animationProgress = 0f
    private var animator: ValueAnimator? = null
    
    // 🌑 Overlay de escurecimento para destaque da seleção
    private var selectionRect: RectF? = null
    private var shouldDimImage = false

    // 🎨 Paints otimizados
    private val cornerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    
    // 🌈 Nova paleta de cores baseada na imagem do usuário
    private val stateColors = mapOf(
        State.NORMAL to Color.parseColor("#FF069E6E"),    // Verde água vibrante
        State.PROCESSING to Color.parseColor("#FF3E7996"), // Azul acinzentado
        State.SUCCESS to Color.parseColor("#FF00BAB4"),   // Verde turquesa
        State.ERROR to Color.parseColor("#FF2D2E47")      // Roxo escuro
    )

    init {
        // 🌟 Software layer necessário para BlurMaskFilter
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    /**
     * 🎯 Muda estado visual
     */
    fun setState(state: State) {
        android.util.Log.d("MinimalImageView", "🎯 setState: $currentState → $state")
        
        currentState = state
        
        when (state) {
            State.PROCESSING -> startProcessingAnimation()
            State.NORMAL -> startSubtleAnimation() // 🌊 Animação sutil sempre
            else -> stopAnimation()
        }
        
        // 🔥 Força redesenho múltiplo
        invalidate()
        post { invalidate() }
        postDelayed({ invalidate() }, 100)
        
        android.util.Log.d("MinimalImageView", "✅ Estado alterado para: $state")
    }

    /**
     * 💫 Animação otimizada para processamento
     */
    private fun startProcessingAnimation() {
        stopAnimation()
        
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ANIMATION_DURATION
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            
            addUpdateListener { 
                animationProgress = it.animatedValue as Float
                invalidate()
            }
            
            start()
        }
    }

    /**
     * 🌊 Animação sutil para estado normal
     */
    private fun startSubtleAnimation() {
        stopAnimation()
        
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000L // 🌊 Mais lenta e sutil
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE // 🔄 Vai e volta
            interpolator = DecelerateInterpolator()
            
            addUpdateListener { animation ->
                animationProgress = animation.animatedValue as Float
                invalidate()
            }
            
            start()
        }
        
        android.util.Log.d("MinimalImageView", "🌊 Animação sutil iniciada")
    }

    /**
     * ⏹️ Para todas as animações
     */
    private fun stopAnimation() {
        animator?.cancel()
        animator = null
        animationProgress = 0f
    }

    /**
     * 🎨 Desenho minimalista otimizado
     */
    override fun onDraw(canvas: Canvas) {
        // 🖼️ Desenha a imagem primeiro
        super.onDraw(canvas)
        
        // 🔥 Debug: Verifica se está sendo chamado
        android.util.Log.d("MinimalImageView", "🎨 onDraw chamado - width: $width, height: $height, estado: $currentState")
        
        // 🌟 Desenha vinheta dos cantos
        if (width > 0 && height > 0) {
            drawCornerEffects(canvas)
        }
    }
    


    /**
     * 🌟 Desenha vinheta com gradiente blur dos cantos
     */
    private fun drawCornerEffects(canvas: Canvas) {
        val baseColor = stateColors[currentState] ?: stateColors[State.NORMAL]!!
        
        // 💫 Intensidade baseada no estado
        val intensity = when (currentState) {
            State.PROCESSING -> {
                val pulse = (sin(animationProgress * PI * 2) * 0.5 + 0.5).toFloat()
                0.4f + (pulse * 0.2f) // 🌊 Pulsação sutil
            }
            State.SUCCESS, State.ERROR -> 0.7f
            else -> 0.5f // 🌟 Sutil mas visível
        }
        
        android.util.Log.d("MinimalImageView", "🎨 Desenhando vinheta - estado: $currentState, intensidade: $intensity")
        
        // 🌟 Desenha vinheta dos 4 cantos
        drawVignetteFromCorners(canvas, baseColor, intensity)
        
        // 🌑 Desenha overlay de escurecimento com destaque
        if (shouldDimImage) {
            drawSelectionOverlay(canvas)
        }
    }
    
    /**
     * 📐 Calcula bounds reais da imagem dentro do ImageView
     */
    private fun getImageBounds(): RectF {
        val drawable = drawable ?: return RectF()
        
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val drawableWidth = drawable.intrinsicWidth.toFloat()
        val drawableHeight = drawable.intrinsicHeight.toFloat()
        
        if (drawableWidth <= 0 || drawableHeight <= 0) return RectF()
        
        // 📏 Calcula escala para FIT_CENTER
        val scale = minOf(viewWidth / drawableWidth, viewHeight / drawableHeight)
        val scaledWidth = drawableWidth * scale
        val scaledHeight = drawableHeight * scale
        
        // 📍 Centraliza
        val left = (viewWidth - scaledWidth) / 2f
        val top = (viewHeight - scaledHeight) / 2f
        
        return RectF(left, top, left + scaledWidth, top + scaledHeight)
    }
    
    /**
     * 🌟 Desenha vinheta contínua e animada das bordas
     */
    private fun drawVignetteFromCorners(canvas: Canvas, baseColor: Int, intensity: Float) {
        android.util.Log.d("MinimalImageView", "🌊 Desenhando vinheta contínua")
        
        // 🌊 Animação suave
        val animatedIntensity = intensity + (sin(animationProgress * PI) * 0.1).toFloat()
        
        // 🎨 Desenha vinheta contínua das 4 bordas
        drawContinuousVignette(canvas, baseColor, animatedIntensity)
    }
    
    /**
     * 🌊 Desenha vinheta contínua e animada (efeito profissional)
     */
    private fun drawContinuousVignette(canvas: Canvas, baseColor: Int, intensity: Float) {
        val centerX = width.toFloat() / 2f
        val centerY = height.toFloat() / 2f
        val maxRadius = maxOf(width.toFloat(), height.toFloat()) * 0.8f // 80% da tela
        
        // 🌊 Animação suave de pulsação
        val pulseEffect = (sin(animationProgress * PI * 2) * 0.1).toFloat()
        val animatedIntensity = intensity + pulseEffect
        val maxAlpha = (255 * animatedIntensity).toInt().coerceIn(0, 255)
        
        android.util.Log.d("MinimalImageView", "🌊 Vinheta radial - centro: ($centerX, $centerY), radius: $maxRadius, alpha: $maxAlpha")
        
        // 🎨 Vinheta radial invertida (escuro nas bordas, claro no centro)
        val vignetteGradient = RadialGradient(
            centerX, centerY,
            maxRadius,
            intArrayOf(
                Color.TRANSPARENT, // Centro transparente
                Color.argb((maxAlpha * 0.1f).toInt(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)), // 10%
                Color.argb((maxAlpha * 0.3f).toInt(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)), // 30%
                Color.argb((maxAlpha * 0.6f).toInt(), Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)), // 60%
                Color.argb(maxAlpha, Color.red(baseColor), Color.green(baseColor), Color.blue(baseColor)) // 100% nas bordas
            ),
            floatArrayOf(0f, 0.4f, 0.7f, 0.9f, 1f),
            Shader.TileMode.CLAMP
        )
        
        val vignettePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = vignetteGradient
            // 🌀 Blur reduzido para melhor performance
            maskFilter = BlurMaskFilter(12f, BlurMaskFilter.Blur.NORMAL)
        }
        
        // 🌊 Desenha vinheta cobrindo toda a tela
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), vignettePaint)
        
        android.util.Log.d("MinimalImageView", "✅ Vinheta radial animada desenhada")
    }
    




    /**
     * 🎯 Métodos públicos simplificados
     */
    fun showNormal() = setState(State.NORMAL)
    fun showProcessing() = setState(State.PROCESSING)
    fun showSuccess() = setState(State.SUCCESS)
    fun showError() = setState(State.ERROR)
    
    /**
     * 🔥 Força exibição da moldura (para debug)
     */
    fun forceBorderRedraw() {
        android.util.Log.d("MinimalImageView", "🔥 Forçando redesenho da moldura")
        currentState = State.NORMAL
        post { 
            invalidate()
            postDelayed({ invalidate() }, 100)
            postDelayed({ invalidate() }, 300)
        }
    }
    
    /**
     * 🌑 Define área selecionada para destaque
     */
    fun setSelectionHighlight(rect: RectF?) {
        selectionRect = rect
        shouldDimImage = rect != null
        invalidate()
        // Destaque atualizado
    }
    
    /**
     * 🌑 Remove destaque da seleção
     */
    fun clearSelectionHighlight() {
        selectionRect = null
        shouldDimImage = false
        invalidate()
        android.util.Log.d("MinimalImageView", "🌑 Destaque removido")
    }

    /**
     * 🌑 Desenha overlay de escurecimento com destaque da seleção
     */
    private fun drawSelectionOverlay(canvas: Canvas) {
        val rect = selectionRect ?: return
        
        // 🌑 Escurece toda a tela EXCETO a área selecionada
        val dimPaint = Paint().apply {
            color = Color.parseColor("#60000000") // Preto 40% transparente
        }
        
        // 🎯 Desenha 4 retângulos ao redor da seleção (não sobre ela)
        // Topo
        canvas.drawRect(0f, 0f, width.toFloat(), rect.top, dimPaint)
        // Esquerda
        canvas.drawRect(0f, rect.top, rect.left, rect.bottom, dimPaint)
        // Direita  
        canvas.drawRect(rect.right, rect.top, width.toFloat(), rect.bottom, dimPaint)
        // Baixo
        canvas.drawRect(0f, rect.bottom, width.toFloat(), height.toFloat(), dimPaint)
        
        // Overlay desenhado
    }

    /**
     * 🧹 Limpeza otimizada
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
