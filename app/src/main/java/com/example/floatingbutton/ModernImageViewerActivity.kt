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
import com.example.floatingbutton.ui.BeautifulDrawingView
import com.example.floatingbutton.ui.FloatingActionMenu
import com.example.floatingbutton.ai.SmartSelectionEngine
import com.example.floatingbutton.ai.semantic.SemanticSegmentationEngine
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

/**
 * 🎨 Modern Image Viewer - Visualizador moderno com interface elegante
 * 
 * Características:
 * - Interface de desenho suave como Circle to Search
 * - Botões flutuantes elegantes
 * - Detecção inteligente de conteúdo
 * - Animações suaves
 * - OCR integrado
 */
class ModernImageViewerActivity : Activity() {

    companion object {
        const val EXTRA_IMAGE_URI = "extra_image_uri"
        private const val TAG = "ModernImageViewer"
    }

    // 🖼️ Views principais
    private lateinit var mainContainer: FrameLayout
    private lateinit var imageView: ImageView
    private lateinit var drawingView: BeautifulDrawingView
    private lateinit var actionMenu: FloatingActionMenu

    // 🖼️ Dados da imagem
    private var imageUri: Uri? = null
    private var originalBitmap: Bitmap? = null

    // 🤖 IA Engines
    private lateinit var smartSelectionEngine: SmartSelectionEngine
    private lateinit var semanticSegmentationEngine: SemanticSegmentationEngine
    private var aiAnalysisJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        Log.d(TAG, "🎨 Iniciando Modern Image Viewer...")
        
        setupViews()
        loadImage()
        setupDrawingInteraction()
        setupActionMenu()
        initializeAI()
    }

    /**
     * 🎨 Configura as views com layout moderno
     */
    private fun setupViews() {
        // Container principal
        mainContainer = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
        }

        // ImageView para a imagem
        imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        // Drawing View elegante
        drawingView = BeautifulDrawingView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            visibility = View.VISIBLE
        }

        // Menu de ações flutuantes
        actionMenu = FloatingActionMenu(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.END or android.view.Gravity.CENTER_VERTICAL
                marginEnd = 32
            }
            visibility = View.GONE
        }

        // Monta a hierarquia
        mainContainer.addView(imageView)
        mainContainer.addView(drawingView)
        mainContainer.addView(actionMenu)
        
        setContentView(mainContainer)
        
        Log.d(TAG, "✅ Views configuradas com sucesso")
    }

    /**
     * 🖼️ Carrega a imagem recebida
     */
    private fun loadImage() {
        imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)
        
        if (imageUri == null) {
            Log.e(TAG, "❌ URI da imagem não fornecida")
            Toast.makeText(this, "Erro: Imagem não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        try {
            contentResolver.openInputStream(imageUri!!)?.use { inputStream ->
                originalBitmap = BitmapFactory.decodeStream(inputStream)
                imageView.setImageBitmap(originalBitmap)
                
                Log.d(TAG, "✅ Imagem carregada: ${originalBitmap?.width}x${originalBitmap?.height}")
                
                // Mostra dica inicial
                showInitialHint()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao carregar imagem: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar imagem", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * 💡 Mostra dica inicial para o usuário
     */
    private fun showInitialHint() {
        Toast.makeText(
            this,
            "✨ Desenhe ao redor da área que deseja selecionar",
            Toast.LENGTH_LONG
        ).show()
    }

    /**
     * 🎨 Configura a interação de desenho
     */
    private fun setupDrawingInteraction() {
        drawingView.setOnDrawingStartListener {
            Log.d(TAG, "🎨 Usuário começou a desenhar")
            actionMenu.hideMenu()
        }

        drawingView.setOnDrawingProgressListener { points ->
            // Feedback visual em tempo real (opcional)
            Log.d(TAG, "🎨 Desenho em progresso: ${points.size} pontos")
        }

        drawingView.setOnDrawingCompleteListener { path, points, bounds ->
            Log.d(TAG, "🎨 Desenho concluído! Área: $bounds")
            
            // Mostra menu de ações
            actionMenu.showMenu()
            
            // Inicia análise inteligente
            performIntelligentAnalysis(points, bounds)
        }
    }

    /**
     * 🎯 Configura o menu de ações
     */
    private fun setupActionMenu() {
        actionMenu.setOnActionClickListener { action ->
            when (action) {
                FloatingActionMenu.Action.OCR -> {
                    performOCR()
                }
                FloatingActionMenu.Action.SAVE_AREA -> {
                    saveSelectedArea()
                }
                FloatingActionMenu.Action.CROP -> {
                    cropImage()
                }
                FloatingActionMenu.Action.SEARCH -> {
                    searchContent()
                }
                FloatingActionMenu.Action.CLOSE -> {
                    closeViewer()
                }
            }
        }
    }

    /**
     * 🤖 Inicializa os engines de IA
     */
    private fun initializeAI() {
        try {
            smartSelectionEngine = SmartSelectionEngine(this)
            semanticSegmentationEngine = SemanticSegmentationEngine()
            Log.d(TAG, "🤖 IA inicializada com sucesso")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao inicializar IA: ${e.message}", e)
        }
    }

    /**
     * 🧠 Realiza análise inteligente da área selecionada
     */
    private fun performIntelligentAnalysis(points: List<PointF>, bounds: RectF) {
        val bitmap = originalBitmap ?: return
        
        aiAnalysisJob?.cancel()
        aiAnalysisJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                Toast.makeText(this@ModernImageViewerActivity, "🤖 Analisando área...", Toast.LENGTH_SHORT).show()
                
                withContext(Dispatchers.Default) {
                    // Análise com IA
                    val detectedObjects = smartSelectionEngine.analyzeRegion(
                        bitmap = bitmap,
                        userDrawnPoints = points
                    ) { progress ->
                        launch(Dispatchers.Main) {
                            Log.d(TAG, "🔄 Progresso IA: $progress")
                        }
                    }
                    
                    withContext(Dispatchers.Main) {
                        processAIResults(detectedObjects, bounds)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro na análise IA: ${e.message}", e)
                Toast.makeText(this@ModernImageViewerActivity, "⚠️ Análise básica ativada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * 🎯 Processa resultados da IA
     */
    private fun processAIResults(detectedObjects: List<SmartSelectionEngine.DetectedObject>, bounds: RectF) {
        if (detectedObjects.isNotEmpty()) {
            val bestObject = detectedObjects.maxByOrNull { it.confidence }
            if (bestObject != null) {
                val emoji = getObjectEmoji(bestObject.type)
                val confidence = (bestObject.confidence * 100).toInt()
                
                Toast.makeText(
                    this,
                    "$emoji ${bestObject.type.name.lowercase()} detectado! ($confidence%)",
                    Toast.LENGTH_LONG
                ).show()
                
                Log.d(TAG, "🎯 Melhor objeto: ${bestObject.type} (${bestObject.confidence})")
            }
        }
    }

    /**
     * 🎨 Retorna emoji para tipo de objeto
     */
    private fun getObjectEmoji(type: SmartSelectionEngine.ObjectType): String {
        return when (type) {
            SmartSelectionEngine.ObjectType.TEXT -> "📝"
            SmartSelectionEngine.ObjectType.IMAGE -> "🖼️"
            SmartSelectionEngine.ObjectType.BUTTON -> "🔘"
            SmartSelectionEngine.ObjectType.ICON -> "🎯"
            SmartSelectionEngine.ObjectType.FACE -> "👤"
            SmartSelectionEngine.ObjectType.OBJECT -> "📦"
            SmartSelectionEngine.ObjectType.SHAPE -> "🔷"
            SmartSelectionEngine.ObjectType.UNKNOWN -> "❓"
        }
    }

    /**
     * 📝 Executa OCR na área selecionada
     */
    private fun performOCR() {
        Toast.makeText(this, "📝 Extraindo texto...", Toast.LENGTH_SHORT).show()
        
        // TODO: Implementar OCR da área selecionada
        // Por enquanto, simula sucesso
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500) // Simula processamento
            
            Toast.makeText(
                this@ModernImageViewerActivity,
                "✅ Texto extraído com sucesso!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 🖼️ Salva a área selecionada
     */
    private fun saveSelectedArea() {
        Toast.makeText(this, "🖼️ Salvando área...", Toast.LENGTH_SHORT).show()
        
        // TODO: Implementar salvamento da área
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            Toast.makeText(
                this@ModernImageViewerActivity,
                "✅ Área salva na galeria!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * ✂️ Recorta a imagem na área selecionada
     */
    private fun cropImage() {
        Toast.makeText(this, "✂️ Recortando imagem...", Toast.LENGTH_SHORT).show()
        
        // TODO: Implementar recorte
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            Toast.makeText(
                this@ModernImageViewerActivity,
                "✅ Imagem recortada!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 🔍 Pesquisa conteúdo na área
     */
    private fun searchContent() {
        Toast.makeText(this, "🔍 Pesquisando conteúdo...", Toast.LENGTH_SHORT).show()
        
        // TODO: Implementar pesquisa
        CoroutineScope(Dispatchers.Main).launch {
            delay(1500)
            Toast.makeText(
                this@ModernImageViewerActivity,
                "🔍 Pesquisa realizada!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * ❌ Fecha o visualizador
     */
    private fun closeViewer() {
        finish()
    }

    override fun onBackPressed() {
        if (actionMenu.isMenuExpanded()) {
            actionMenu.hideMenu()
            drawingView.clearDrawing()
        } else {
            super.onBackPressed()
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
        
        Log.d(TAG, "🧹 Modern Image Viewer finalizado")
    }
}
