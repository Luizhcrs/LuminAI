package com.example.floatingbutton.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.floatingbutton.ai.AIDetectionService

/**
 * 🤖 AI Results Dialog - Mostra resultados da detecção de IA de forma elegante
 * 
 * Características:
 * - Design moderno e intuitivo
 * - Animações suaves
 * - Indicador visual de confiança
 * - Recomendações baseadas no resultado
 * - Interface responsiva
 */
class AIResultsDialog @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "AIResultsDialog"
        private const val ANIMATION_DURATION = 200L // 🚀 Ultra rápido
        private const val BOUNCE_SCALE = 1.1f // 🎯 Efeito bounce
    }

    // 🎨 Views principais
    private lateinit var containerCard: LinearLayout
    private lateinit var emojiView: TextView
    private lateinit var titleView: TextView
    private lateinit var confidenceView: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var analysisView: TextView
    private lateinit var recommendationView: TextView
    private lateinit var closeButton: TextView

    // 📊 Estado atual
    private var currentResult: AIDetectionService.AIDetectionResult? = null
    private var onCloseListener: (() -> Unit)? = null

    init {
        setupViews()
    }

    /**
     * 🎨 Configura as views com design Lumin moderno
     */
    private fun setupViews() {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        setPadding(24, 24, 24, 24)
        
        // Container principal com design Lumin
        containerCard = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 48, 48, 48)
            background = createLuminCardBackground()
            elevation = 24f
        }

        // Ícone Lumin moderno
        emojiView = TextView(context).apply {
            textSize = 72f // 🎯 Muito maior para impacto
            gravity = Gravity.CENTER
            text = "✨" // Ícone padrão Lumin
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 32 // 🎨 Ainda mais espaço
            }
        }

        // Título principal
        titleView = TextView(context).apply {
            textSize = 28f // 🎯 Maior
            setTextColor(Color.parseColor("#FF069E6E")) // 🌟 Cor Lumin
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16 // 🎨 Mais espaço
            }
        }

        // Percentual de confiança
        confidenceView = TextView(context).apply {
            textSize = 18f
            setTextColor(Color.parseColor("#E0E0E0"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Barra de progresso visual
        progressBar = ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal).apply {
            layoutParams = LinearLayout.LayoutParams(
                300,
                24
            ).apply {
                bottomMargin = 24
            }
            progressDrawable = createProgressDrawable()
        }

        // Análise detalhada
        analysisView = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#CCCCCC"))
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 16
            }
        }

        // Recomendação
        recommendationView = TextView(context).apply {
            textSize = 14f
            setTextColor(Color.parseColor("#4285F4"))
            gravity = Gravity.CENTER
            typeface = Typeface.DEFAULT_BOLD
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = 24
            }
        }

        // Botão de fechar moderno
        closeButton = TextView(context).apply {
            text = "Entendi"
            textSize = 18f
            setTextColor(Color.WHITE)
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(48, 20, 48, 20)
            background = createLuminButtonBackground()
            isClickable = true
            isFocusable = true
            
            // 🎯 Efeito de toque
            foreground = createRippleEffect()
            
            setOnClickListener {
                hideWithAnimation()
            }
        }

        // Monta hierarquia
        containerCard.addView(emojiView)
        containerCard.addView(titleView)
        containerCard.addView(confidenceView)
        containerCard.addView(progressBar)
        containerCard.addView(analysisView)
        containerCard.addView(recommendationView)
        containerCard.addView(closeButton)
        
        addView(containerCard)

        // Inicialmente invisível
        visibility = View.GONE
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f
    }

    /**
     * 🎨 Cria background moderno do Lumin
     */
    private fun createLuminCardBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 28f // 🎯 Mais arredondado
            
            // 🌟 Gradiente elegante Lumin
            colors = intArrayOf(
                Color.parseColor("#FF1A2F2F"), // Verde-azulado escuro
                Color.parseColor("#FF0F1A1A"), // Centro mais escuro
                Color.parseColor("#FF1A2A2A")  // Base escura
            )
            orientation = GradientDrawable.Orientation.TL_BR
            
            // ✨ Borda luminosa
            setStroke(3, Color.parseColor("#80069E6E"))
        }
    }

    /**
     * 🎨 Cria background do botão
     */
    private fun createLuminButtonBackground(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 24f
            setColor(Color.parseColor("#FF069E6E"))
            setStroke(2, Color.parseColor("#80FFFFFF"))
        }
    }
    
    /**
     * 🌊 Cria efeito ripple
     */
    private fun createRippleEffect(): android.graphics.drawable.RippleDrawable {
        return android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(Color.parseColor("#40FFFFFF")),
            null,
            null
        )
    }

    /**
     * 🎨 Cria drawable da barra de progresso
     */
    private fun createProgressDrawable(): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f
            setColor(Color.parseColor("#4285F4"))
        }
    }

    /**
     * 📊 Mostra resultados da detecção de IA
     */
    fun showResults(result: AIDetectionService.AIDetectionResult) {
        currentResult = result
        
        // Atualiza conteúdo
        emojiView.text = result.emoji
        
        titleView.text = if (result.isAIGenerated) {
            "Gerada por IA"
        } else {
            "Imagem Real"
        }
        
        confidenceView.text = "Confiança: ${result.confidencePercentage}%"
        
        // Anima barra de progresso
        progressBar.max = 100
        animateProgressBar(result.confidencePercentage)
        
        analysisView.text = result.analysisDetails
        recommendationView.text = result.recommendationText
        
        // Cores baseadas no resultado
        updateColorsBasedOnResult(result)
        
        // Mostra com animação
        showWithAnimation()
    }

    /**
     * 🎨 Atualiza cores baseadas no resultado
     */
    private fun updateColorsBasedOnResult(result: AIDetectionService.AIDetectionResult) {
        val primaryColor = if (result.isAIGenerated) {
            when {
                result.confidence >= 0.8f -> "#FF5722" // Vermelho - Definitivamente IA
                result.confidence >= 0.6f -> "#FF9800" // Laranja - Provável IA
                else -> "#FFC107" // Amarelo - Possível IA
            }
        } else {
            when {
                result.confidence <= 0.2f -> "#4CAF50" // Verde - Definitivamente real
                result.confidence <= 0.4f -> "#8BC34A" // Verde claro - Provável real
                else -> "#CDDC39" // Verde amarelado - Incerto
            }
        }

        // Atualiza cor da barra de progresso
        progressBar.progressDrawable = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12f
            setColor(Color.parseColor(primaryColor))
        }

        // Atualiza cor do título
        titleView.setTextColor(Color.parseColor(primaryColor))
    }

    /**
     * ✨ Anima barra de progresso
     */
    private fun animateProgressBar(targetProgress: Int) {
        val animator = ObjectAnimator.ofInt(progressBar, "progress", 0, targetProgress)
        animator.duration = ANIMATION_DURATION
        animator.interpolator = DecelerateInterpolator()
        animator.start()
    }

    /**
     * ✨ Mostra dialog com animação bounce elegante
     */
    private fun showWithAnimation() {
        visibility = View.VISIBLE
        
        // 🎯 Animação em duas fases: bounce + settle
        val bounceAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(this@AIResultsDialog, "scaleX", 0.3f, BOUNCE_SCALE),
                ObjectAnimator.ofFloat(this@AIResultsDialog, "scaleY", 0.3f, BOUNCE_SCALE),
                ObjectAnimator.ofFloat(this@AIResultsDialog, "alpha", 0f, 1f),
                ObjectAnimator.ofFloat(this@AIResultsDialog, "rotation", -5f, 2f)
            )
            duration = ANIMATION_DURATION / 2
            interpolator = android.view.animation.OvershootInterpolator(1.2f)
        }
        
        val settleAnimator = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(this@AIResultsDialog, "scaleX", BOUNCE_SCALE, 1f),
                ObjectAnimator.ofFloat(this@AIResultsDialog, "scaleY", BOUNCE_SCALE, 1f),
                ObjectAnimator.ofFloat(this@AIResultsDialog, "rotation", 2f, 0f)
            )
            duration = ANIMATION_DURATION / 2
            interpolator = DecelerateInterpolator()
        }
        
        AnimatorSet().apply {
            playSequentially(bounceAnimator, settleAnimator)
            start()
        }
    }

    /**
     * ✨ Esconde dialog com animação
     */
    private fun hideWithAnimation() {
        val scaleXAnimator = ObjectAnimator.ofFloat(this, "scaleX", 1f, 0.8f)
        val scaleYAnimator = ObjectAnimator.ofFloat(this, "scaleY", 1f, 0.8f)
        val alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)
        
        AnimatorSet().apply {
            playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
            duration = ANIMATION_DURATION / 2
            interpolator = DecelerateInterpolator()
            
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    visibility = View.GONE
                    onCloseListener?.invoke()
                }
            })
            
            start()
        }
    }

    /**
     * 📱 Setter para callback de fechamento
     */
    fun setOnCloseListener(listener: () -> Unit) {
        onCloseListener = listener
    }

    /**
     * 🔄 Força fechamento
     */
    fun forceClose() {
        visibility = View.GONE
        alpha = 0f
        scaleX = 0.8f
        scaleY = 0.8f
        onCloseListener?.invoke()
    }

    /**
     * 🎯 Verifica se está visível
     */
    fun isShowing(): Boolean = visibility == View.VISIBLE
}
