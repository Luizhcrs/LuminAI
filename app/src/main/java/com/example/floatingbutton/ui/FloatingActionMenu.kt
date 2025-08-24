package com.example.floatingbutton.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.example.floatingbutton.R

/**
 * Floating Action Menu - Menu flutuante elegante com animações
 * 
 * Botões disponíveis:
 * - OCR (Extrair texto)
 * - Salvar área selecionada
 * - Recortar
 * - Pesquisar
 * - Fechar
 */
class FloatingActionMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val ANIMATION_DURATION = 80L // Ultra rápido
        private const val STAGGER_DELAY = 8L // Delay mínimo entre botões
    }

    // Ações disponíveis
    enum class Action {
        OCR, SAVE_AREA, SEARCH, AI_SCAN, CLOSE
        // CROP removido
    }

    // Callback para ações
    private var onActionClickListener: ((Action) -> Unit)? = null
    
    // Botões
    private val actionButtons = mutableListOf<View>()
    private var isExpanded = false

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        createActionButtons()
    }

    /**
     * Cria os botões de ação com design moderno
     */
    private fun createActionButtons() {
        // IA - Primeiro lugar
        addActionButton(
            action = Action.AI_SCAN,
            iconRes = R.drawable.ic_ai_brain,
            title = "IA",
            subtitle = "",
            color = "#069E6E" // Cor única
        )

        // OCR - Texto
        addActionButton(
            action = Action.OCR,
            iconRes = R.drawable.ic_text_recognition,
            title = "OCR",
            subtitle = "",
            color = "#069E6E" // Cor única
        )

        // Buscar
        addActionButton(
            action = Action.SEARCH,
            iconRes = R.drawable.ic_search,
            title = "Buscar",
            subtitle = "",
            color = "#069E6E" // Cor única
        )

        // Salvar
        addActionButton(
            action = Action.SAVE_AREA,
            iconRes = R.drawable.ic_save,
            title = "Salvar",
            subtitle = "",
            color = "#069E6E" // Cor única
        )

        // Fechar
        addActionButton(
            action = Action.CLOSE,
            iconRes = R.drawable.ic_close,
            title = "Fechar",
            subtitle = "",
            color = "#069E6E" // Cor única
        )
    }

    /**
     * Adiciona um botão de ação elegante
     */
    private fun addActionButton(
        action: Action,
        iconRes: Int,
        title: String,
        subtitle: String,
        color: String
    ) {
        val buttonContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(24, 16, 24, 16) // Padding elegante
            
            // Background com sombra e cantos arredondados
            background = createButtonBackground(color)
            elevation = 8f // Sombra elegante
            
            // Efeito de clique
            isClickable = true
            isFocusable = true
            
            setOnClickListener {
                onActionClickListener?.invoke(action)
                animateClick(this)
            }
        }

        // Ícone Material Design
        val iconView = ImageView(context).apply {
            setImageResource(iconRes)
            layoutParams = LinearLayout.LayoutParams(56, 56).apply { // Mais espaçoso
                marginEnd = 16 // Margem elegante
            }
            setColorFilter(Color.WHITE)
        }

        // Container de texto
        val textContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Título (minimalista)
        val titleView = TextView(context).apply {
            text = title
            textSize = 14f // Menor e minimalista
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        textContainer.addView(titleView)
        
        // Só adiciona subtítulo se não estiver vazio
        if (subtitle.isNotEmpty()) {
            val subtitleView = TextView(context).apply {
                text = subtitle
                textSize = 11f // Menor
                setTextColor(Color.parseColor("#B3FFFFFF"))
            }
            textContainer.addView(subtitleView)
        }
        
        buttonContainer.addView(iconView)
        buttonContainer.addView(textContainer)

        // Adiciona margem entre botões
        val layoutParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 0, 0, 16)
        }
        buttonContainer.layoutParams = layoutParams

        actionButtons.add(buttonContainer)
        addView(buttonContainer)

        // Inicialmente invisível
        buttonContainer.alpha = 0f
        buttonContainer.scaleX = 0.8f
        buttonContainer.scaleY = 0.8f
        buttonContainer.visibility = View.GONE
    }

    /**
     * Cria background moderno com gradiente e sombra
     */
    private fun createButtonBackground(color: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 16f
            
            // Cor sólida minimalista (sem gradiente)
            setColor(Color.parseColor(color))
            
            // Borda elegante
            setStroke(2, Color.parseColor("#60FFFFFF"))
        }
    }
    
    /**
     * Ajusta brilho da cor
     */
    private fun adjustColorBrightness(color: Int, factor: Float): Int {
        val hsv = FloatArray(3)
        Color.colorToHSV(color, hsv)
        hsv[2] = (hsv[2] * factor).coerceIn(0f, 1f)
        return Color.HSVToColor(Color.alpha(color), hsv)
    }

    /**
     * Animação de clique no botão
     */
    private fun animateClick(view: View) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.95f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.95f)
            )
            duration = 100
        }
        
        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f)
            )
            duration = 100
        }
        
        AnimatorSet().apply {
            playSequentially(scaleDown, scaleUp)
            start()
        }
    }

    /**
     * Mostra o menu com animação elegante
     */
    fun showMenu() {
        if (isExpanded) return
        
        isExpanded = true
        visibility = View.VISIBLE
        
        actionButtons.forEachIndexed { index, button ->
            button.visibility = View.VISIBLE
            
            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(button, "alpha", 0f, 1f),
                    ObjectAnimator.ofFloat(button, "scaleX", 0.8f, 1f),
                    ObjectAnimator.ofFloat(button, "scaleY", 0.8f, 1f),
                    ObjectAnimator.ofFloat(button, "translationY", 50f, 0f)
                )
                duration = ANIMATION_DURATION
                interpolator = DecelerateInterpolator()
                startDelay = index * STAGGER_DELAY
            }
            
            animator.start()
        }
    }

    /**
     * Esconde o menu com animação elegante
     */
    fun hideMenu() {
        if (!isExpanded) return
        
        isExpanded = false
        
        actionButtons.forEachIndexed { index, button ->
            val animator = AnimatorSet().apply {
                playTogether(
                    ObjectAnimator.ofFloat(button, "alpha", 1f, 0f),
                    ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.8f),
                    ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.8f),
                    ObjectAnimator.ofFloat(button, "translationY", 0f, -50f)
                )
                duration = ANIMATION_DURATION / 2
                interpolator = DecelerateInterpolator()
                startDelay = (actionButtons.size - index - 1) * (STAGGER_DELAY / 2)
            }
            
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    button.visibility = View.GONE
                    if (index == actionButtons.size - 1) {
                        visibility = View.GONE
                    }
                }
            })
            
            animator.start()
        }
    }

    /**
     * Setter para callback de ações
     */
    fun setOnActionClickListener(listener: (Action) -> Unit) {
        onActionClickListener = listener
    }

    /**
     * Verifica se o menu está expandido
     */
    fun isMenuExpanded(): Boolean = isExpanded
}
