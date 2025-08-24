package com.example.floatingbutton.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

/**
 * 🖌️ Magical Brush View - Pincel com efeitos visuais impressionantes
 *
 * Versão Refatorada para Performance e Fluidez Máxima:
 * - ✅ Lógica de desenho unificada e eficiente.
 * - 🚀 Algoritmo de suavização superior para curvas perfeitamente fluidas.
 * - ✨ Efeito de brilho (glow) aprimorado com desfoque realista.
 * - 🎨 Código mais limpo e fácil de manter.
 */
class MagicalBrushView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "MagicalBrushView"
        private const val STROKE_WIDTH = 10f // 🔥 Linha principal mais nítida
        private const val GLOW_WIDTH = 35f   // 🌟 Brilho expandido para um efeito suave
        private const val TOUCH_TOLERANCE = 4f // ⚡ Tolerância de toque mais sensível
        private const val PARTICLE_COUNT = 5 // ✨ Partículas para um efeito mais rico
    }

    // --- Dados do Desenho ---
    private val drawPoints = mutableListOf<PointF>()
    private val smoothedPath = Path()
    private var isDrawing = false

    // --- Sistema de Partículas ---
    private val particles = mutableListOf<Particle>()

    // --- Pincéis (Paints) Otimizados ---
    // Inicializados apenas uma vez para máxima performance.
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
        color = Color.parseColor("#40069E6E") // Cor base para o brilho
        // Efeito de desfoque para um brilho realista e suave
        maskFilter = BlurMaskFilter(16f, BlurMaskFilter.Blur.NORMAL)
    }

    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var particleAnimator: ValueAnimator? = null
    var onDrawingCompleted: ((List<PointF>) -> Unit)? = null

    init {
        // Habilita a renderização por software, necessária para o BlurMaskFilter funcionar.
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    // --- LÓGICA PRINCIPAL DE DESENHO ---

    /**
     * ✅ O Coração do Desenho
     * Este método agora orquestra o desenho de forma limpa e em camadas.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (drawPoints.isEmpty()) return

        // 1. Desenha a trilha e o brilho com o caminho suave
        drawTrailAndEffects(canvas)

        // 2. Desenha as partículas por cima de tudo
        drawParticles(canvas)
    }

    /**
     * ✅ Unifica o desenho da linha e do brilho.
     * Calcula o caminho suave e o desenha em camadas para um efeito visual coeso.
     */
    private fun drawTrailAndEffects(canvas: Canvas) {
        // Se houver poucos pontos, desenha uma linha simples para resposta imediata.
        if (drawPoints.size < 3) {
            drawFallbackLine(canvas)
            return
        }

        // 1. Gera o caminho perfeitamente suave a partir dos pontos.
        updateSmoothedPath()

        // 2. Desenha os efeitos em camadas (de trás para frente).

        // Camada 1: O Brilho (desenhado primeiro, por baixo de tudo).
        canvas.drawPath(smoothedPath, glowPaint)

        // Camada 2: A Linha Principal (desenhada por cima do brilho).
        val gradient = LinearGradient(
            drawPoints.first().x, drawPoints.first().y,
            drawPoints.last().x, drawPoints.last().y,
            intArrayOf(
                Color.parseColor("#FF069E6E"),
                Color.parseColor("#FF3E7996"),
                Color.parseColor("#FF00BAB4")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        strokePaint.shader = gradient
        canvas.drawPath(smoothedPath, strokePaint)
    }

    /**
     * 🚀 ALGORITMO DE SUAVIZAÇÃO APRIMORADO
     * Atualiza o `smoothedPath` usando curvas que conectam os pontos médios dos segmentos.
     * Isso garante que não haja "quinas" e a curva seja perfeitamente fluida.
     */
    private fun updateSmoothedPath() {
        smoothedPath.reset()
        smoothedPath.moveTo(drawPoints[0].x, drawPoints[0].y)

        // Conecta o primeiro ponto ao ponto médio entre o primeiro e o segundo.
        val firstMidX = (drawPoints[0].x + drawPoints[1].x) / 2
        val firstMidY = (drawPoints[0].y + drawPoints[1].y) / 2
        smoothedPath.lineTo(firstMidX, firstMidY)

        // Itera pelos pontos para criar as curvas.
        for (i in 1 until drawPoints.size - 1) {
            val p1 = drawPoints[i]
            val p2 = drawPoints[i + 1]
            val midX = (p1.x + p2.x) / 2
            val midY = (p1.y + p2.y) / 2

            // O ponto atual (p1) age como o "ímã" (ponto de controle) que puxa a curva,
            // conectando os pontos médios de forma suave.
            smoothedPath.quadTo(p1.x, p1.y, midX, midY)
        }

        // Conecta a última curva ao ponto final.
        val lastPoint = drawPoints.last()
        smoothedPath.lineTo(lastPoint.x, lastPoint.y)
    }

    /**
     * Linha reta simples usada como fallback quando há poucos pontos para uma curva.
     */
    private fun drawFallbackLine(canvas: Canvas) {
        val path = Path()
        path.moveTo(drawPoints[0].x, drawPoints[0].y)
        for (i in 1 until drawPoints.size) {
            path.lineTo(drawPoints[i].x, drawPoints[i].y)
        }
        val gradient = LinearGradient(
            drawPoints.first().x, drawPoints.first().y,
            drawPoints.last().x, drawPoints.last().y,
            intArrayOf(
                Color.parseColor("#FF069E6E"),
                Color.parseColor("#FF3E7996"),
                Color.parseColor("#FF00BAB4")
            ),
            null,
            Shader.TileMode.CLAMP
        )
        strokePaint.shader = gradient
        canvas.drawPath(path, strokePaint)
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
        smoothedPath.reset()
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
     * 💫 Cria partículas ao redor do pincel
     */
    private fun createParticles(x: Float, y: Float) {
        repeat(PARTICLE_COUNT) {
            val angle = (it * 45f) * (Math.PI / 180f)
            val distance = 20f + (Math.random() * 30f).toFloat()

            particles.add(
                Particle(
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
                )
            )
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