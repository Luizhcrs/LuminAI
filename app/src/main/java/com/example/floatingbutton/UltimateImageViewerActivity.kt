package com.example.floatingbutton

import android.app.Activity
import android.content.Intent
import android.animation.ObjectAnimator
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
// SmartRectangleDrawingView removido - substituído por ElegantSelectionView
import com.example.floatingbutton.ui.MagicalBrushView
import com.example.floatingbutton.ui.ElegantSelectionView
import com.example.floatingbutton.ui.LiveOCROverlay
import com.example.floatingbutton.ui.FloatingActionMenu
import com.example.floatingbutton.ui.AIResultsDialog
import com.example.floatingbutton.ui.MinimalImageView
import com.example.floatingbutton.ai.SmartSelectionEngine
import com.example.floatingbutton.ai.AIDetectionService
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * Ultimate Image Viewer - A experiência definitiva de seleção inteligente
 * 
 * Características:
 * - Desenho livre que se completa como retângulo
 * - OCR ativo em tempo real sobre o texto
 * - Texto selecionável como em sites
 * - Animações fluidas e interface moderna
 * - IA integrada para detecção inteligente
 * - Cópia automática para clipboard
 */
class UltimateImageViewerActivity : Activity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val TAG = "UltimateImageViewer"
    }

    // Views principais
    private lateinit var mainContainer: FrameLayout
    private lateinit var imageView: MinimalImageView
    private lateinit var magicalBrushView: MagicalBrushView
    private lateinit var elegantSelectionView: ElegantSelectionView
    private lateinit var ocrOverlay: LiveOCROverlay
    private lateinit var actionMenu: FloatingActionMenu
    private lateinit var aiResultsDialog: AIResultsDialog

    // Dados da imagem
    private var imageUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var selectedRegion: RectF? = null

    // IA Engines
    private lateinit var smartSelectionEngine: SmartSelectionEngine
    private lateinit var aiDetectionService: AIDetectionService
    private var aiAnalysisJob: Job? = null

    // Estados
    private var currentMode = ViewMode.MAGICAL_DRAWING
    
    enum class ViewMode { MAGICAL_DRAWING, SMART_SELECTION, OCR_ACTIVE, MENU_VISIBLE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "Iniciando Ultimate Image Viewer...")
        
        // PRIMEIRO: Configurar views
        setupViews()
        setupInteractions()
        initializeAI()
        
        // SEGUNDO: Processar imagem compartilhada (se houver)
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            // RECEBEU IMAGEM VIA SHARE - PROCESSAR DIRETO
            handleSharedImage(intent)
        } else {
            // NÃO RECEBEU IMAGEM - FECHAR
            Log.w(TAG, "Nenhuma imagem recebida, fechando...")
            finish()
            return
        }
        
        // TERCEIRO: Inicia no modo de pincel mágico
        enterMagicalDrawingMode()
    }

    // ADICIONAR FUNÇÃO PARA PROCESSAR IMAGEM COMPARTILHADA
    private fun handleSharedImage(intent: Intent) {
        val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
        
        if (imageUri != null) {
            Log.d(TAG, "handleSharedImage: URI da imagem: $imageUri")
            this.imageUri = imageUri
            loadImage()
        } else {
            Log.e(TAG, "handleSharedImage: URI da imagem é null!")
            finish()
        }
    }

    /**
     * Configura as views com layout moderno
     */
    private fun setupViews() {
        // Container principal com gradiente elegante
        mainContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundResource(com.example.floatingbutton.R.drawable.app_background_gradient)
        }

        // Minimal ImageView com efeitos sutis nos cantos
        imageView = MinimalImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            // Inicia com estado normal minimalista
            showNormal()
            // Força redesenho das bordas
            post { 
                invalidate()
                Log.d(TAG, "MinimalImageView forçado a redesenhar bordas")
            }
        }

        // SmartDrawingView removido - substituído por ElegantSelectionView

        // Magical Brush View (pincel com efeitos visuais)
        magicalBrushView = MagicalBrushView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            
            // Callback quando desenho é concluído
            onDrawingCompleted = { points ->
                handleDrawingCompleted(points)
            }
        }

        // Elegant Selection View (retângulo redimensionável)
        elegantSelectionView = ElegantSelectionView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
            
            // Callbacks para redimensionamento
            onSelectionChanged = { rect ->
                selectedRegion = rect
                // Converte coordenadas para o ImageView
                val imageRect = convertToImageCoordinates(rect)
                imageView.setSelectionHighlight(imageRect)
                updateActionMenuPosition(rect)
            }
            
            onSelectionCompleted = { rect ->
                selectedRegion = rect
                actionMenu.showMenu()
            }
        }

        // Live OCR Overlay (texto selecionável)
        ocrOverlay = LiveOCROverlay(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.GONE
        }

        // Menu de ações flutuantes
        actionMenu = FloatingActionMenu(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                marginEnd = 24
            }
            visibility = View.GONE
        }

        // Dialog de resultados de IA
        aiResultsDialog = AIResultsDialog(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#80000000")) // Fundo semi-transparente
        }

        // Monta a hierarquia (ordem importa para eventos de toque)
        mainContainer.addView(imageView)
        mainContainer.addView(magicalBrushView) // 🖌️ Pincel mágico
        mainContainer.addView(elegantSelectionView) // Seleção elegante redimensionável
        mainContainer.addView(ocrOverlay)
        mainContainer.addView(actionMenu)
        mainContainer.addView(aiResultsDialog)
        
        setContentView(mainContainer)
        
        // Views configuradas
    }

    /**
     * Carrega a imagem recebida
     */
    private fun loadImage() {
        // Usa a URI já processada em handleSharedImage()
        if (imageUri == null) {
            Log.e(TAG, "❌ URI da imagem não fornecida")
            showError("Erro: Imagem não encontrada")
            return
        }

        try {
            contentResolver.openInputStream(imageUri!!)?.use { inputStream ->
                originalBitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(originalBitmap)
                
                // Ativa efeitos minimalistas com animação suave
                imageView.alpha = 0f
                imageView.scaleX = 0.95f
                imageView.scaleY = 0.95f
                imageView.showNormal()
                
                // Força redesenho da moldura após carregar imagem
                imageView.post {
                    imageView.forceBorderRedraw()
                    Log.d(TAG, "🖼️ Imagem carregada, forçando moldura")
                }
                
                // Animação de entrada mais sutil
                imageView.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .setInterpolator(DecelerateInterpolator())
                    .start()
                
                // Imagem carregada
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao carregar imagem: ${e.message}", e)
            showError("Erro ao carregar imagem")
        }
    }

    /**
     * Configura todas as interações
     */
    private fun setupInteractions() {
        setupOCRInteractions()
        setupMenuInteractions()
    }

    // setupDrawingInteractions removida - agora usamos MagicalBrushView + ElegantSelectionView

    /**
     * Configura interações de OCR
     */
    private fun setupOCRInteractions() {
        ocrOverlay.setOnTextSelectedListener { text, mode ->
            val modeText = when (mode) {
                LiveOCROverlay.SelectionMode.WORD -> "palavra"
                LiveOCROverlay.SelectionMode.LINE -> "linha"
                LiveOCROverlay.SelectionMode.BLOCK -> "bloco"
            }
            
            Log.d(TAG, "📝 Texto selecionado ($modeText): $text")
            // Texto selecionado silenciosamente
        }

        ocrOverlay.setOnOCRCompleteListener { textBlocks ->
            Log.d(TAG, "🤖 OCR completo: ${textBlocks.size} blocos detectados")
            
            val totalText = textBlocks.sumOf { it.text.length }
            // OCR concluído silenciosamente
            
            // Mostra instruções de uso
            showOCRInstructions()
        }
    }

    /**
     * Configura interações do menu
     */
    private fun setupMenuInteractions() {
        actionMenu.setOnActionClickListener { action ->
            when (action) {
                FloatingActionMenu.Action.OCR -> {
                    activateOCR()
                    currentMode = ViewMode.OCR_ACTIVE
                }
                FloatingActionMenu.Action.SAVE_AREA -> {
                    saveSelectedArea()
                }
                // Botão CROP removido
                FloatingActionMenu.Action.SEARCH -> {
                    searchSelectedContent()
                }
                FloatingActionMenu.Action.AI_SCAN -> {
                    performAIScan()
                }
                FloatingActionMenu.Action.CLOSE -> {
                    // Volta ao pincel sem bugs
                    actionMenu.hideMenu()
                    enterMagicalDrawingMode()
                }
            }
        }
    }

    /**
     * Inicializa IA
     */
    private fun initializeAI() {
        try {
            smartSelectionEngine = SmartSelectionEngine(this)
            aiDetectionService = AIDetectionService(this)
            
            // Configura callback do dialog de resultados
            aiResultsDialog.setOnCloseListener {
                Log.d(TAG, "🤖 Dialog de IA fechado")
            }
            
            Log.d(TAG, "🤖 IA inicializada com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao inicializar IA: ${e.message}", e)
        }
    }

    /**
     * Mostra dica de boas-vindas
     */
    private fun showWelcomeHint() {
        // Boas-vindas silenciosas
    }

    /**
     * Ativa modo OCR
     */
    private fun activateOCR() {
        val region = selectedRegion ?: return
        val bitmap = originalBitmap ?: return
        
        Log.d(TAG, "📝 Ativando OCR para região: $region")
        
        // Esconde menu e mostra OCR overlay
        actionMenu.hideMenu()
        ocrOverlay.visibility = View.VISIBLE
        // smartDrawingView removido
        
        currentMode = ViewMode.OCR_ACTIVE
        
        // Executa OCR
        // OCR executando silenciosamente
        ocrOverlay.performOCR(bitmap, region)
    }

    /**
     * Processa desenho concluído do pincel mágico
     */
    private fun handleDrawingCompleted(points: List<PointF>) {
        Log.d(TAG, "🖌️ handleDrawingCompleted chamado com ${points.size} pontos")
        
        if (points.size < 3) {
            Log.w(TAG, "⚠️ Poucos pontos para criar seleção: ${points.size}")
            return
        }
        
        // Converte pontos livres em retângulo elegante
        val bounds = calculateBounds(points)
        
        // VALIDA SE A SELEÇÃO É VÁLIDA
        if (!isValidSelection(bounds)) {
            Log.w(TAG, "⚠️ Seleção inválida, voltando ao modo de desenho")
            magicalBrushView.clearDrawing()
            return
        }
        
        selectedRegion = bounds
        
        Log.d(TAG, "📐 Bounds calculados: $bounds")
        
        // Esconde pincel e mostra seleção elegante
        magicalBrushView.visibility = View.GONE
        elegantSelectionView.setSelection(bounds)
        imageView.setSelectionHighlight(bounds) // 🌑 Ativa destaque
        currentMode = ViewMode.SMART_SELECTION
        
        // Mostra menu de ações
        actionMenu.showMenu()
        
        // Seleção ativada
    }
    
    /**
     * Valida se a seleção é adequada para uso
     */
    private fun isValidSelection(bounds: RectF): Boolean {
        // Verifica tamanho mínimo
        if (bounds.width() < 80f || bounds.height() < 80f) {
            Log.w(TAG, "⚠️ Seleção muito pequena: ${bounds.width()}x${bounds.height()}")
            return false
        }
        
        // Verifica se está dentro dos limites da tela
        val screenWidth = mainContainer.width.toFloat()
        val screenHeight = mainContainer.height.toFloat()
        
        if (bounds.left < 0f || bounds.top < 0f || 
            bounds.right > screenWidth || bounds.bottom > screenHeight) {
            Log.w(TAG, "⚠️ Seleção fora dos limites da tela")
            return false
        }
        
        return true
    }
    
    /**
     * Calcula bounds dos pontos com limites seguros
     */
    private fun calculateBounds(points: List<PointF>): RectF {
        if (points.isEmpty()) return RectF()
        
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        
        points.forEach { point ->
            minX = minOf(minX, point.x)
            maxX = maxOf(maxX, point.x)
            minY = minOf(minY, point.y)
            maxY = maxOf(maxY, point.y)
        }
        
        // Adiciona margem
        val margin = 20f
        
        // Calcula bounds iniciais
        val bounds = RectF(
            maxOf(0f, minX - margin),
            maxOf(0f, minY - margin),
            maxX + margin,
            maxY + margin
        )
        
        // APLICA LIMITES DE SEGURANÇA
        return applySafeBounds(bounds)
    }
    
    /**
     * Aplica limites seguros para a seleção
     */
    private fun applySafeBounds(bounds: RectF): RectF {
        // Dimensões da tela (usando mainContainer)
        val screenWidth = mainContainer.width.toFloat()
        val screenHeight = mainContainer.height.toFloat()
        
        // Margens de segurança para botões e elementos da UI
        val safeMargin = 80f // Margem para botões e elementos
        val minSelectionSize = 100f // Tamanho mínimo da seleção
        
        // Limita largura e altura
        var safeWidth = bounds.width().coerceAtLeast(minSelectionSize)
        var safeHeight = bounds.height().coerceAtLeast(minSelectionSize)
        
        // Garante que não ultrapasse a largura da tela
        if (bounds.left + safeWidth > screenWidth - safeMargin) {
            safeWidth = screenWidth - bounds.left - safeMargin
        }
        
        // Garante que não ultrapasse a altura da tela
        if (bounds.top + safeHeight > screenHeight - safeMargin) {
            safeHeight = screenHeight - bounds.top - safeMargin
        }
        
        // Garante que não ultrapasse a esquerda
        var safeLeft = bounds.left.coerceAtLeast(safeMargin)
        if (safeLeft + safeWidth > screenWidth - safeMargin) {
            safeLeft = screenWidth - safeWidth - safeMargin
        }
        
        // Garante que não ultrapasse o topo
        var safeTop = bounds.top.coerceAtLeast(safeMargin)
        if (safeTop + safeHeight > screenHeight - safeMargin) {
            safeTop = screenHeight - safeHeight - safeMargin
        }
        
        // Retorna bounds seguros
        return RectF(safeLeft, safeTop, safeLeft + safeWidth, safeTop + safeHeight)
    }

    /**
     * Entra no modo de desenho mágico
     */
    private fun enterMagicalDrawingMode() {
        currentMode = ViewMode.MAGICAL_DRAWING
        
        // Mostra apenas o pincel mágico
        magicalBrushView.visibility = View.VISIBLE
        // smartDrawingView removido
        elegantSelectionView.visibility = View.GONE
        ocrOverlay.visibility = View.GONE
        actionMenu.hideMenu()
        
        // Limpa TUDO para começar fresh
        selectedRegion = null
        magicalBrushView.clearDrawing()
        // smartDrawingView removido
        elegantSelectionView.clearSelection()
        imageView.clearSelectionHighlight() // 🌑 Remove destaque
        
        // Configura clique fora para reset
        setupOutsideClickReset()
        
        Log.d(TAG, "🖌️ Modo pincel mágico ativado - sempre funcional")
    }
    
    /**
     * Configura reset ao clicar fora da área
     */
    private fun setupOutsideClickReset() {
        mainContainer.setOnClickListener { 
            // Sempre permite nova seleção
            when (currentMode) {
                ViewMode.SMART_SELECTION -> {
                    // Se tem seleção ativa, permite nova
                    enterMagicalDrawingMode()
                    // Removido toast desnecessário
                }
                ViewMode.MENU_VISIBLE -> {
                    // Esconde menu e volta ao pincel
                    enterMagicalDrawingMode()
                }
                ViewMode.OCR_ACTIVE -> {
                    // Sai do OCR e volta ao pincel
                    enterMagicalDrawingMode()
                }
                else -> {
                    // Já está no modo pincel, só limpa
                    magicalBrushView.clearDrawing()
                }
            }
        }
    }

    /**
     * Executa análise inteligente
     */
    private fun performIntelligentAnalysis(region: RectF) {
        val bitmap = originalBitmap ?: return
        
        aiAnalysisJob?.cancel()
        aiAnalysisJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Análise silenciosa - sem toast
                
                withContext(Dispatchers.Default) {
                    // Simula análise (substitua pela IA real)
                    delay(300) // ⚡ Análise mais rápida
                    
                    withContext(Dispatchers.Main) {
                        // Simula resultados
                        val hasText = region.width() > 200 && region.height() > 50
                        // Análise silenciosa - resultados mostrados apenas no menu
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro na análise: ${e.message}", e)
            }
        }
    }

    /**
     * Salva área selecionada com qualidade otimizada
     */
    private fun saveSelectedArea() {
        val region = selectedRegion ?: run {
            // Área não selecionada - silencioso
            return
        }
        val bitmap = originalBitmap ?: run {
            // Imagem não carregada - silencioso
            return
        }
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Recorta com margem para melhor qualidade
                val croppedBitmap = cropBitmapToRegion(bitmap, region)
                
                // Otimiza qualidade da imagem
                val optimizedBitmap = optimizeBitmapQuality(croppedBitmap)
                
                // Salva com nome descritivo
                val savedFile = saveBitmapToFile(optimizedBitmap, "lumin_selection")
                
                // Libera memória
                if (croppedBitmap != optimizedBitmap) {
                    croppedBitmap.recycle()
                }
                optimizedBitmap.recycle()
                
                withContext(Dispatchers.Main) {
                    // Área salva silenciosamente
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    // Erro ao salvar - silencioso
                }
            }
        }
    }

    /**
     * ✂️ Recorta para área selecionada
     */
    private fun cropToSelectedArea() {
        val region = selectedRegion ?: return
        val bitmap = originalBitmap ?: return
        
        try {
            val croppedBitmap = cropBitmapToRegion(bitmap, region)
            imageView.setImageBitmap(croppedBitmap)
            originalBitmap = croppedBitmap
            
            enterMagicalDrawingMode()
            // Imagem recortada silenciosamente
            
        } catch (e: Exception) {
            // Erro ao recortar - silencioso
        }
    }

    /**
     * Pesquisa conteúdo selecionado
     */
    private fun searchSelectedContent() {
        // Pesquisa em desenvolvimento - silencioso
        // TODO: Implementar pesquisa
    }

    /**
     * Executa scan de IA na área selecionada
     */
    private fun performAIScan() {
        val region = selectedRegion
        val bitmap = originalBitmap
        
        if (region == null || bitmap == null) {
            // Área não selecionada - silencioso
            return
        }

        Log.d(TAG, "🤖 Iniciando scan de IA para região: $region")
        
        // Ativa estado visual de processamento minimalista
        imageView.showProcessing()
        
        // Esconde menu
        actionMenu.hideMenu()
        
        aiAnalysisJob?.cancel()
        aiAnalysisJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Recorta área selecionada
                val croppedBitmap = cropBitmapToRegion(bitmap, region)
                
                // Executa detecção de IA
                val result = aiDetectionService.detectAIGenerated(croppedBitmap) { progress ->
                    // Progresso silencioso
                }
                
                result.fold(
                    onSuccess = { aiResult ->
                        // Scan de IA concluído
                        
                        // Ativa estado visual de sucesso
                        imageView.showSuccess()
                        
                        // Mostra resultados no dialog elegante
                        aiResultsDialog.showResults(aiResult)
                        
                        // Volta ao normal após 2 segundos (mais rápido)
                        launch {
                            delay(2000)
                            imageView.showNormal()
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Erro no scan de IA: ${error.message}", error)
                        
                        // Ativa estado visual de erro
                        imageView.showError()
                        
                        // Erro de IA silencioso
                        
                        // Volta para o menu
                        actionMenu.showMenu()
                        
                        // Volta ao normal após 600ms (ultra rápido)
                        launch {
                            delay(600)
                            imageView.showNormal()
                        }
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro no scan de IA: ${e.message}", e)
                // Erro de IA silencioso
                actionMenu.showMenu()
            }
        }
    }

    /**
     * Otimiza qualidade da imagem
     */
    private fun optimizeBitmapQuality(bitmap: Bitmap): Bitmap {
        // Se a imagem é muito pequena, não otimiza
        if (bitmap.width < 100 || bitmap.height < 100) return bitmap
        
        // Se a imagem é muito grande, redimensiona mantendo qualidade
        val maxDimension = 2048
        return if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = minOf(
                maxDimension.toFloat() / bitmap.width,
                maxDimension.toFloat() / bitmap.height
            )
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }

    // resetToDrawingMode removida - substituída por enterMagicalDrawingMode

    /**
     * Converte coordenadas da seleção para o ImageView
     */
    private fun convertToImageCoordinates(selectionRect: RectF): RectF {
        // Como ambas as views ocupam a tela toda, as coordenadas são as mesmas
        return RectF(selectionRect)
    }

    /**
     * Posiciona menu inteligentemente baseado na seleção
     */
    private fun updateActionMenuPosition(selectionRect: RectF) {
        val menuHeight = 400 // Altura real do menu expandido
        val menuWidth = 200 // Largura estimada
        val margin = 24f // Margem elegante
        
        // Dimensões da tela
        val screenHeight = mainContainer.height
        val screenWidth = mainContainer.width
        
        // Posicionamento inteligente com limites seguros
        val layoutParams = actionMenu.layoutParams as FrameLayout.LayoutParams
        
        // Prioridade: direita da seleção
        when {
            // Direita tem espaço suficiente
            selectionRect.right + menuWidth + margin < screenWidth -> {
                layoutParams.leftMargin = (selectionRect.right + margin).toInt()
                layoutParams.topMargin = calculateSafeTopMargin(selectionRect.centerY(), menuHeight, screenHeight)
                layoutParams.gravity = Gravity.LEFT or Gravity.TOP
            }
            // Esquerda tem espaço
            selectionRect.left - menuWidth - margin > 0 -> {
                layoutParams.rightMargin = (screenWidth - selectionRect.left + margin).toInt()
                layoutParams.topMargin = calculateSafeTopMargin(selectionRect.centerY(), menuHeight, screenHeight)
                layoutParams.gravity = Gravity.RIGHT or Gravity.TOP
            }
            // Acima da seleção
            selectionRect.top - menuHeight - margin > 50 -> {
                layoutParams.leftMargin = 0
                layoutParams.rightMargin = 0
                layoutParams.topMargin = (selectionRect.top - menuHeight - margin).toInt()
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            }
            // Abaixo da seleção (último recurso)
            else -> {
                layoutParams.leftMargin = 0
                layoutParams.rightMargin = 0
                layoutParams.topMargin = (selectionRect.bottom + margin).coerceAtMost(screenHeight - menuHeight - 50f).toInt()
                layoutParams.gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            }
        }
        
        actionMenu.layoutParams = layoutParams
    }
    
    /**
     * Calcula margem superior segura para o menu
     */
    private fun calculateSafeTopMargin(centerY: Float, menuHeight: Int, screenHeight: Int): Int {
        val desiredTop = centerY - menuHeight / 2
        
        // Garante que o menu não ultrapasse o topo
        val safeTop = desiredTop.coerceAtLeast(50f)
        
        // Garante que o menu não ultrapasse a parte inferior
        val maxTop = screenHeight - menuHeight - 50f
        return safeTop.coerceAtMost(maxTop).toInt()
    }

    /**
     * Mostra menu de ações
     */
    private fun showActionMenu() {
        val selectionRect = elegantSelectionView.getSelectionRect()
        if (selectionRect != null) {
            updateActionMenuPosition(selectionRect)
        }
        
        actionMenu.visibility = View.VISIBLE
        actionMenu.showMenu()
    }

    /**
     * Esconde menu e OCR
     */
    private fun hideMenuAndOCR() {
        actionMenu.hideMenu()
        ocrOverlay.clearOCR()
        ocrOverlay.visibility = View.GONE
    }

    /**
     * Mostra instruções do OCR
     */
    private fun showOCRInstructions() {
        // Instruções OCR silenciosas
    }

    /**
     * ✂️ Recorta bitmap para região
     */
    private fun cropBitmapToRegion(bitmap: Bitmap, region: RectF): Bitmap {
        val x = region.left.toInt().coerceAtLeast(0)
        val y = region.top.toInt().coerceAtLeast(0)
        val width = region.width().toInt().coerceAtMost(bitmap.width - x)
        val height = region.height().toInt().coerceAtMost(bitmap.height - y)
        
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    /**
     * Salva bitmap em arquivo com qualidade otimizada
     */
    private fun saveBitmapToFile(bitmap: Bitmap, prefix: String = "lumin"): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "${prefix}_$timeStamp.jpg"
        
        // Salva na pasta Pictures/Lumin
        val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val luminDir = File(picturesDir, "Lumin")
        if (!luminDir.exists()) {
            luminDir.mkdirs()
        }
        
        val file = File(luminDir, fileName)
        FileOutputStream(file).use { out ->
            // Qualidade alta para preservar detalhes
            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        
        return file
    }

    // Função showToast removida - operação silenciosa

    /**
     * ❌ Mostra erro e fecha
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // SIMPLES: apenas fechar a activity
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup
        aiAnalysisJob?.cancel()
        
        if (::smartSelectionEngine.isInitialized) {
            smartSelectionEngine.cleanup()
        }
        
        originalBitmap?.recycle()
        
        Log.d(TAG, "🧹 Ultimate Image Viewer finalizado")
    }
}
