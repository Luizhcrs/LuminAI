package com.example.floatingbutton

import android.app.Activity
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.floatingbutton.ui.SmartRectangleDrawingView
import com.example.floatingbutton.ui.LiveOCROverlay
import com.example.floatingbutton.ui.FloatingActionMenu
import com.example.floatingbutton.ui.AIResultsDialog
import com.example.floatingbutton.ai.SmartSelectionEngine
import com.example.floatingbutton.ai.AIDetectionService
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🚀 Ultimate Image Viewer - A experiência definitiva de seleção inteligente
 * 
 * Características:
 * - 🔲 Desenho livre que se completa como retângulo
 * - 📝 OCR ativo em tempo real sobre o texto
 * - 🎯 Texto selecionável como em sites
 * - ✨ Animações fluidas e interface moderna
 * - 🤖 IA integrada para detecção inteligente
 * - 📋 Cópia automática para clipboard
 */
class UltimateImageViewerActivity : Activity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val TAG = "UltimateImageViewer"
    }

    // 🖼️ Views principais
    private lateinit var mainContainer: FrameLayout
    private lateinit var imageView: ImageView
    private lateinit var smartDrawingView: SmartRectangleDrawingView
    private lateinit var ocrOverlay: LiveOCROverlay
    private lateinit var actionMenu: FloatingActionMenu
    private lateinit var aiResultsDialog: AIResultsDialog

    // 🖼️ Dados da imagem
    private var imageUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var selectedRegion: RectF? = null

    // 🤖 IA Engines
    private lateinit var smartSelectionEngine: SmartSelectionEngine
    private lateinit var aiDetectionService: AIDetectionService
    private var aiAnalysisJob: Job? = null

    // 🎯 Estados
    private var currentMode = ViewMode.DRAWING
    
    enum class ViewMode { DRAWING, OCR_ACTIVE, MENU_VISIBLE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🚀 Iniciando Ultimate Image Viewer...")
        
        setupViews()
        loadImage()
        setupInteractions()
        initializeAI()
        
        showWelcomeHint()
    }

    /**
     * 🎨 Configura as views com layout moderno
     */
    private fun setupViews() {
        // Container principal com fundo elegante
        mainContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        // ImageView para a imagem de fundo
        imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // Smart Drawing View (desenho que vira retângulo)
        smartDrawingView = SmartRectangleDrawingView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
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
        mainContainer.addView(smartDrawingView)
        mainContainer.addView(ocrOverlay)
        mainContainer.addView(actionMenu)
        mainContainer.addView(aiResultsDialog)
        
        setContentView(mainContainer)
        
        Log.d(TAG, "✅ Views configuradas com layout elegante")
    }

    /**
     * 🖼️ Carrega a imagem recebida
     */
    private fun loadImage() {
        imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)
        
        if (imageUri == null) {
            Log.e(TAG, "❌ URI da imagem não fornecida")
            showError("Erro: Imagem não encontrada")
            return
        }

        try {
            contentResolver.openInputStream(imageUri!!)?.use { inputStream ->
                originalBitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(originalBitmap)
                
                Log.d(TAG, "✅ Imagem carregada: ${originalBitmap?.width}x${originalBitmap?.height}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao carregar imagem: ${e.message}", e)
            showError("Erro ao carregar imagem")
        }
    }

    /**
     * 🎯 Configura todas as interações
     */
    private fun setupInteractions() {
        setupDrawingInteractions()
        setupOCRInteractions()
        setupMenuInteractions()
    }

    /**
     * 🎨 Configura interações de desenho
     */
    private fun setupDrawingInteractions() {
        smartDrawingView.setOnDrawingStartListener {
            Log.d(TAG, "🎨 Usuário começou a desenhar")
            hideMenuAndOCR()
            currentMode = ViewMode.DRAWING
        }

        smartDrawingView.setOnRectangleDetectedListener { rect ->
            Log.d(TAG, "🔲 Retângulo detectado: $rect")
            selectedRegion = rect
            
            // Feedback visual
            showToast("🔲 Área retangular detectada!")
        }

        smartDrawingView.setOnSelectionCompleteListener { rect ->
            Log.d(TAG, "✅ Seleção completa: $rect")
            selectedRegion = rect
            
            // Mostra menu de ações
            showActionMenu()
            currentMode = ViewMode.MENU_VISIBLE
            
            // Inicia análise inteligente em background
            performIntelligentAnalysis(rect)
        }
    }

    /**
     * 📝 Configura interações de OCR
     */
    private fun setupOCRInteractions() {
        ocrOverlay.setOnTextSelectedListener { text, mode ->
            val modeText = when (mode) {
                LiveOCROverlay.SelectionMode.WORD -> "palavra"
                LiveOCROverlay.SelectionMode.LINE -> "linha"
                LiveOCROverlay.SelectionMode.BLOCK -> "bloco"
            }
            
            Log.d(TAG, "📝 Texto selecionado ($modeText): $text")
            showToast("📝 $modeText selecionada: \"${text.take(30)}...\"")
        }

        ocrOverlay.setOnOCRCompleteListener { textBlocks ->
            Log.d(TAG, "🤖 OCR completo: ${textBlocks.size} blocos detectados")
            
            val totalText = textBlocks.sumOf { it.text.length }
            showToast("📝 OCR concluído! $totalText caracteres detectados")
            
            // Mostra instruções de uso
            showOCRInstructions()
        }
    }

    /**
     * 🎯 Configura interações do menu
     */
    private fun setupMenuInteractions() {
        actionMenu.setOnActionClickListener { action ->
            when (action) {
                FloatingActionMenu.Action.OCR -> {
                    activateOCR()
                }
                FloatingActionMenu.Action.SAVE_AREA -> {
                    saveSelectedArea()
                }
                FloatingActionMenu.Action.CROP -> {
                    cropToSelectedArea()
                }
                FloatingActionMenu.Action.SEARCH -> {
                    searchSelectedContent()
                }
                FloatingActionMenu.Action.AI_SCAN -> {
                    performAIScan()
                }
                FloatingActionMenu.Action.CLOSE -> {
                    resetToDrawingMode()
                }
            }
        }
    }

    /**
     * 🤖 Inicializa IA
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
     * 💡 Mostra dica de boas-vindas
     */
    private fun showWelcomeHint() {
        showToast("✨ Desenhe livremente ao redor da área que deseja selecionar")
    }

    /**
     * 📝 Ativa modo OCR
     */
    private fun activateOCR() {
        val region = selectedRegion ?: return
        val bitmap = originalBitmap ?: return
        
        Log.d(TAG, "📝 Ativando OCR para região: $region")
        
        // Esconde menu e mostra OCR overlay
        actionMenu.hideMenu()
        ocrOverlay.visibility = View.VISIBLE
        smartDrawingView.visibility = View.GONE
        
        currentMode = ViewMode.OCR_ACTIVE
        
        // Executa OCR
        showToast("🤖 Executando OCR...")
        ocrOverlay.performOCR(bitmap, region)
    }

    /**
     * 🧠 Executa análise inteligente
     */
    private fun performIntelligentAnalysis(region: RectF) {
        val bitmap = originalBitmap ?: return
        
        aiAnalysisJob?.cancel()
        aiAnalysisJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                showToast("🤖 Analisando área selecionada...")
                
                withContext(Dispatchers.Default) {
                    // Simula análise (substitua pela IA real)
                    delay(1000)
                    
                    withContext(Dispatchers.Main) {
                        // Simula resultados
                        val hasText = region.width() > 200 && region.height() > 50
                        if (hasText) {
                            showToast("📝 Texto detectado! Use OCR para extrair")
                        } else {
                            showToast("🖼️ Área de imagem selecionada")
                        }
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro na análise: ${e.message}", e)
            }
        }
    }

    /**
     * 🖼️ Salva área selecionada
     */
    private fun saveSelectedArea() {
        val region = selectedRegion ?: return
        val bitmap = originalBitmap ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val croppedBitmap = cropBitmapToRegion(bitmap, region)
                val savedFile = saveBitmapToFile(croppedBitmap)
                
                withContext(Dispatchers.Main) {
                    showToast("✅ Área salva: ${savedFile.name}")
                }
                
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    showToast("❌ Erro ao salvar área")
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
            
            resetToDrawingMode()
            showToast("✂️ Imagem recortada com sucesso!")
            
        } catch (e: Exception) {
            showToast("❌ Erro ao recortar imagem")
        }
    }

    /**
     * 🔍 Pesquisa conteúdo selecionado
     */
    private fun searchSelectedContent() {
        showToast("🔍 Funcionalidade de pesquisa em desenvolvimento")
        // TODO: Implementar pesquisa
    }

    /**
     * 🤖 Executa scan de IA na área selecionada
     */
    private fun performAIScan() {
        val region = selectedRegion
        val bitmap = originalBitmap
        
        if (region == null || bitmap == null) {
            showToast("❌ Nenhuma área selecionada")
            return
        }

        Log.d(TAG, "🤖 Iniciando scan de IA para região: $region")
        
        // Esconde menu
        actionMenu.hideMenu()
        
        aiAnalysisJob?.cancel()
        aiAnalysisJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                // Recorta área selecionada
                val croppedBitmap = cropBitmapToRegion(bitmap, region)
                
                // Executa detecção de IA
                val result = aiDetectionService.detectAIGenerated(croppedBitmap) { progress ->
                    showToast(progress)
                }
                
                result.fold(
                    onSuccess = { aiResult ->
                        Log.d(TAG, "✅ Scan de IA concluído: ${aiResult.confidencePercentage}%")
                        
                        // Mostra resultados no dialog elegante
                        aiResultsDialog.showResults(aiResult)
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Erro no scan de IA: ${error.message}", error)
                        showToast("❌ Erro na detecção de IA: ${error.message}")
                        
                        // Volta para o menu
                        actionMenu.showMenu()
                    }
                )
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro no scan de IA: ${e.message}", e)
                showToast("❌ Erro inesperado no scan de IA")
                actionMenu.showMenu()
            }
        }
    }

    /**
     * 🔄 Volta para modo de desenho
     */
    private fun resetToDrawingMode() {
        hideMenuAndOCR()
        smartDrawingView.clearSelection()
        smartDrawingView.visibility = View.VISIBLE
        selectedRegion = null
        currentMode = ViewMode.DRAWING
        
        showToast("🎨 Modo desenho ativado")
    }

    /**
     * 🎯 Mostra menu de ações
     */
    private fun showActionMenu() {
        actionMenu.visibility = View.VISIBLE
        actionMenu.showMenu()
    }

    /**
     * 🫥 Esconde menu e OCR
     */
    private fun hideMenuAndOCR() {
        actionMenu.hideMenu()
        ocrOverlay.clearOCR()
        ocrOverlay.visibility = View.GONE
    }

    /**
     * 📝 Mostra instruções do OCR
     */
    private fun showOCRInstructions() {
        showToast("💡 Toque: palavra | Duplo toque: linha | Toque longo: bloco + copiar")
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
     * 💾 Salva bitmap em arquivo
     */
    private fun saveBitmapToFile(bitmap: Bitmap): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "selected_area_$timeStamp.jpg"
        
        val file = File(getExternalFilesDir(null), fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        
        return file
    }

    /**
     * 💬 Mostra toast elegante
     */
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * ❌ Mostra erro e fecha
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (currentMode) {
            ViewMode.OCR_ACTIVE -> {
                resetToDrawingMode()
            }
            ViewMode.MENU_VISIBLE -> {
                resetToDrawingMode()
            }
            ViewMode.DRAWING -> {
                @Suppress("DEPRECATION")
                super.onBackPressed()
            }
        }
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
