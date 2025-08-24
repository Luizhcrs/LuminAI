package com.example.floatingbutton.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import kotlin.math.*

/**
 * 🖌️ Magical Brush View - Pincel com efeitos visuais impressionantes
 * 
 * Características:
 * - ✨ Gradiente dinâmico no traço
 * - 💫 Rastro luminoso que segue o pincel
 * - 🌟 Efeito de partículas ao desenhar
 * - 🎨 Cores vibrantes e modernas
 * - 🚫 SEM tela azul de seleção
 */
class MagicalBrushView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "MagicalBrushView"
        private const val STROKE_WIDTH = 12f // 🔥 Mais grosso
        private const val GLOW_WIDTH = 16f // 🌟 Glow restaurado
        private const val TRAIL_WIDTH = 8f // 🎯 Trail otimizado
        private const val TOUCH_TOLERANCE = 8f
        private const val PARTICLE_COUNT = 2 // ⚡ Ainda menos partículas
    }

    // 🎨 Dados do desenho
    private val drawPoints = mutableListOf<PointF>()
    private val smoothPath = Path()
    private var isDrawing = false
    
    // 💫 Sistema de partículas
    private val particles = mutableListOf<Particle>()
    
    // 🎨 Paints otimizados
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = GLOW_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        maskFilter = BlurMaskFilter(8f, BlurMaskFilter.Blur.OUTER)
    }
    
    private val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = TRAIL_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    // 🎭 Animação de partículas
    private var particleAnimator: ValueAnimator? = null
    
    // 📞 Callback para quando o desenho é concluído
    var onDrawingCompleted: ((List<PointF>) -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null) // Para BlurMaskFilter
    }

    /**
     * 🎨 Desenha todos os efeitos visuais
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (drawPoints.isEmpty()) return
        
        // 🌟 Rastro otimizado (essencial para visibilidade)
        drawOptimizedTrail(canvas)
        
        // ✨ Glow otimizado para visibilidade
        drawOptimizedGlow(canvas)
        
        // 🎨 Linha principal com gradiente
        drawMainStroke(canvas)
        
        // 💫 Partículas reduzidas
        drawParticles(canvas)
    }

    /**
     * ⚡ Rastro otimizado para performance
     */
    private fun drawOptimizedTrail(canvas: Canvas) {
        if (drawPoints.size < 2) return
        
        // 🎨 Paint mais visível
        val trailPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f // 🔥 Mais grosso
            color = Color.parseColor("#B0069E6E") // Verde água mais opaco
            strokeCap = Paint.Cap.ROUND
        }
        
        // 🌟 Desenha linha simples conectando pontos
        for (i in 1 until drawPoints.size) {
            val start = drawPoints[i - 1]
            val end = drawPoints[i]
            canvas.drawLine(start.x, start.y, end.x, end.y, trailPaint)
        }
    }

    /**
     * ✨ Glow otimizado para visibilidade
     */
    private fun drawOptimizedGlow(canvas: Canvas) {
        if (drawPoints.isEmpty()) return
        
        // 🌟 Paint de glow simples
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = GLOW_WIDTH
            color = Color.parseColor("#40069E6E") // Verde água suave
            strokeCap = Paint.Cap.ROUND
        }
        
        // ✨ Desenha glow nos últimos pontos
        val startIndex = (drawPoints.size - 10).coerceAtLeast(0)
        for (i in startIndex + 1 until drawPoints.size) {
            val start = drawPoints[i - 1]
            val end = drawPoints[i]
            canvas.drawLine(start.x, start.y, end.x, end.y, glowPaint)
        }
    }

    /**
     * 🌟 Desenha rastro luminoso atrás do pincel (método original)
     */
    private fun drawTrail(canvas: Canvas) {
        if (drawPoints.size < 2) return
        
        val trailPath = Path()
        trailPath.moveTo(drawPoints[0].x, drawPoints[0].y)
        
        for (i in 1 until drawPoints.size) {
            val point = drawPoints[i]
            trailPath.lineTo(point.x, point.y)
        }
        
        // 💫 Gradiente para o rastro
        val gradient = LinearGradient(
            drawPoints.first().x, drawPoints.first().y,
            drawPoints.last().x, drawPoints.last().y,
            intArrayOf(
                Color.parseColor("#00069E6E"), // Verde água transparente
                Color.parseColor("#40069E6E"), // Verde água 25%
                Color.parseColor("#80069E6E")  // Verde água 50%
            ),
            null,
            Shader.TileMode.CLAMP
        )
        
        trailPaint.shader = gradient
        canvas.drawPath(trailPath, trailPaint)
    }

    /**
     * ✨ Desenha brilho ao redor da linha
     */
    private fun drawGlow(canvas: Canvas) {
        if (drawPoints.size < 2) return
        
        updateSmoothPath()
        canvas.drawPath(smoothPath, glowPaint)
    }

    /**
     * 🎨 Desenha linha principal com gradiente dinâmico
     */
    private fun drawMainStroke(canvas: Canvas) {
        if (drawPoints.size < 2) return
        
        // 🌈 Gradiente que muda baseado na posição
        val gradient = LinearGradient(
            drawPoints.first().x, drawPoints.first().y,
            drawPoints.last().x, drawPoints.last().y,
            intArrayOf(
                Color.parseColor("#FF069E6E"), // Verde água vibrante
                Color.parseColor("#FF3E7996"), // Azul acinzentado
                Color.parseColor("#FF00BAB4")  // Verde turquesa
            ),
            null,
            Shader.TileMode.CLAMP
        )
        
        strokePaint.shader = gradient
        canvas.drawPath(smoothPath, strokePaint)
    }

    /**
     * 💫 Desenha partículas ao redor do pincel
     */
    private fun drawParticles(canvas: Canvas) {
        particles.forEach { particle ->
            particlePaint.color = particle.color
            particlePaint.alpha = (particle.alpha * 255).toInt()
            canvas.drawCircle(particle.x, particle.y, particle.size, particlePaint)
        }
    }

    /**
     * 🔄 Atualiza path suave
     */
    private fun updateSmoothPath() {
        smoothPath.reset()
        
        if (drawPoints.isEmpty()) return
        
        smoothPath.moveTo(drawPoints[0].x, drawPoints[0].y)
        
        for (i in 1 until drawPoints.size) {
            val point = drawPoints[i]
            if (i == 1) {
                val midX = (drawPoints[0].x + point.x) / 2f
                val midY = (drawPoints[0].y + point.y) / 2f
                smoothPath.quadTo(drawPoints[0].x, drawPoints[0].y, midX, midY)
            } else {
                val prevPoint = drawPoints[i - 1]
                val midX = (prevPoint.x + point.x) / 2f
                val midY = (prevPoint.y + point.y) / 2f
                smoothPath.quadTo(prevPoint.x, prevPoint.y, midX, midY)
            }
        }
        
        if (drawPoints.size > 1) {
            val lastPoint = drawPoints.last()
            smoothPath.lineTo(lastPoint.x, lastPoint.y)
        }
    }

    /**
     * 💫 Cria partículas ao redor do pincel
     */
    private fun createParticles(x: Float, y: Float) {
        repeat(PARTICLE_COUNT) {
            val angle = (it * 45f) * (Math.PI / 180f)
            val distance = 20f + (Math.random() * 30f).toFloat()
            
            particles.add(Particle(
                x = x + (cos(angle) * distance).toFloat(),
                y = y + (sin(angle) * distance).toFloat(),
                size = 2f + (Math.random() * 3f).toFloat(),
                color = when ((Math.random() * 3).toInt()) {
                    0 -> Color.parseColor("#FF069E6E") // Verde água
                    1 -> Color.parseColor("#FF3E7996") // Azul acinzentado
                    else -> Color.parseColor("#FF00BAB4") // Verde turquesa
                },
                alpha = 0.8f,
                life = 1f
            ))
        }
        
        // 🧹 Remove partículas antigas
        particles.removeAll { it.life <= 0f }
    }

    /**
     * 🎭 Anima partículas
     */
    private fun startParticleAnimation() {
        particleAnimator?.cancel()
        
        particleAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 60L // 60ms para 16fps suave
            repeatCount = ValueAnimator.INFINITE
            
            addUpdateListener {
                // 💫 Atualiza partículas
                particles.forEach { particle ->
                    particle.life -= 0.05f
                    particle.alpha = particle.life
                    particle.y -= 2f // Movimento para cima
                }
                
                invalidate()
            }
            
            start()
        }
    }

    /**
     * ⏹️ Para animação de partículas
     */
    private fun stopParticleAnimation() {
        particleAnimator?.cancel()
        particleAnimator = null
        particles.clear()
    }

    /**
     * 🖱️ Gerencia eventos de toque
     */
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
     * 🎨 Inicia desenho
     */
    private fun startDrawing(x: Float, y: Float) {
        clearDrawing()
        isDrawing = true
        drawPoints.add(PointF(x, y))
        createParticles(x, y)
        startParticleAnimation()
        invalidate()
    }

    /**
     * ✏️ Continua desenho
     */
    private fun continueDrawing(x: Float, y: Float) {
        if (!isDrawing) return
        
        val dx = abs(x - drawPoints.last().x)
        val dy = abs(y - drawPoints.last().y)
        
        if (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE) {
            drawPoints.add(PointF(x, y))
            createParticles(x, y)
            invalidate()
        }
    }

    /**
     * ✅ Finaliza desenho
     */
    private fun finishDrawing() {
        if (!isDrawing) return
        
        isDrawing = false
        stopParticleAnimation()
        
        // 📞 Notifica conclusão
        onDrawingCompleted?.invoke(drawPoints.toList())
    }

    /**
     * 🧹 Limpa desenho
     */
    fun clearDrawing() {
        drawPoints.clear()
        smoothPath.reset()
        particles.clear()
        stopParticleAnimation()
        invalidate()
    }

    /**
     * 🧹 Limpeza de recursos
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopParticleAnimation()
    }

    /**
     * 💫 Classe para partículas
     */
    private data class Particle(
        var x: Float,
        var y: Float,
        val size: Float,
        val color: Int,
        var alpha: Float,
        var life: Float
    )
}
