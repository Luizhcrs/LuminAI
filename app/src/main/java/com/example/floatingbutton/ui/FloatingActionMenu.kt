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
 * 🎯 Floating Action Menu - Menu flutuante elegante com animações
 * 
 * Botões disponíveis:
 * - 📝 OCR (Extrair texto)
 * - 🖼️ Salvar área selecionada
 * - ✂️ Recortar
 * - 🔍 Pesquisar
 * - ❌ Fechar
 */
class FloatingActionMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val ANIMATION_DURATION = 300L
        private const val STAGGER_DELAY = 50L
    }

    // 🎯 Ações disponíveis
    enum class Action {
        OCR, SAVE_AREA, CROP, SEARCH, AI_SCAN, CLOSE
    }

    // 🎯 Callback para ações
    private var onActionClickListener: ((Action) -> Unit)? = null
    
    // 🎯 Botões
    private val actionButtons = mutableListOf<View>()
    private var isExpanded = false

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        createActionButtons()
    }

    /**
     * 🎨 Cria os botões de ação com design moderno
     */
    private fun createActionButtons() {
        // 📝 OCR - Extrair Texto
        addActionButton(
            action = Action.OCR,
            icon = "📝",
            title = "Extrair Texto",
            subtitle = "OCR da área selecionada",
            color = "#4285F4"
        )

        // 🖼️ Salvar Área
        addActionButton(
            action = Action.SAVE_AREA,
            icon = "🖼️",
            title = "Salvar Área",
            subtitle = "Salvar região selecionada",
            color = "#34A853"
        )

        // ✂️ Recortar
        addActionButton(
            action = Action.CROP,
            icon = "✂️",
            title = "Recortar",
            subtitle = "Cortar imagem",
            color = "#FBBC04"
        )

        // 🔍 Pesquisar
        addActionButton(
            action = Action.SEARCH,
            icon = "🔍",
            title = "Pesquisar",
            subtitle = "Pesquisar conteúdo",
            color = "#EA4335"
        )

        // 🤖 Scan de IA
        addActionButton(
            action = Action.AI_SCAN,
            icon = "🤖",
            title = "Detectar IA",
            subtitle = "Verificar se é gerada por IA",
            color = "#9C27B0"
        )

        // ❌ Fechar
        addActionButton(
            action = Action.CLOSE,
            icon = "❌",
            title = "Fechar",
            subtitle = "Cancelar seleção",
            color = "#9AA0A6"
        )
    }

    /**
     * 🎨 Adiciona um botão de ação elegante
     */
    private fun addActionButton(
        action: Action,
        icon: String,
        title: String,
        subtitle: String,
        color: String
    ) {
        val buttonContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(32, 16, 32, 16)
            
            // Background com sombra e cantos arredondados
            background = createButtonBackground(color)
            
            // Efeito de clique
            isClickable = true
            isFocusable = true
            
            setOnClickListener {
                onActionClickListener?.invoke(action)
                animateClick(this)
            }
        }

        // 🎨 Ícone
        val iconView = TextView(context).apply {
            text = icon
            textSize = 24f
            gravity = Gravity.CENTER
            layoutParams = LinearLayout.LayoutParams(80, 80).apply {
                marginEnd = 24
            }
        }

        // 🎨 Container de texto
        val textContainer = LinearLayout(context).apply {
            orientation = VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        }

        // 🎨 Título
        val titleView = TextView(context).apply {
            text = title
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        // 🎨 Subtítulo
        val subtitleView = TextView(context).apply {
            text = subtitle
            textSize = 12f
            setTextColor(Color.parseColor("#B3FFFFFF"))
        }

        textContainer.addView(titleView)
        textContainer.addView(subtitleView)
        
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
     * 🎨 Cria background elegante para os botões
     */
    private fun createButtonBackground(color: String): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 28f
            setColor(Color.parseColor(color))
            
            // Sombra suave
            setStroke(2, Color.parseColor("#20000000"))
        }
    }

    /**
     * ✨ Animação de clique no botão
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
     * ✨ Mostra o menu com animação elegante
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
     * ✨ Esconde o menu com animação elegante
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
     * 📱 Setter para callback de ações
     */
    fun setOnActionClickListener(listener: (Action) -> Unit) {
        onActionClickListener = listener
    }

    /**
     * 🎯 Verifica se o menu está expandido
     */
    fun isMenuExpanded(): Boolean = isExpanded
}
