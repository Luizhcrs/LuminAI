package com.example.floatingbutton

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.floatingbutton.ai.SmartSelectionEngine
import com.example.floatingbutton.ai.semantic.SemanticSegmentationEngine
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

class ImageViewerActivity : Activity() {
    
    companion object {
        private const val TAG = "ImageViewerActivity"
        const val EXTRA_IMAGE_URI = "extra_image_uri"
    }
    
    private lateinit var fullscreenImageView: ImageView
    private lateinit var btnClose: ImageButton
    private lateinit var btnCrop: ImageButton
    private lateinit var btnClear: ImageButton
    private lateinit var btnSave: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var freeDrawCropView: FreeDrawCropView
    
    private var imageUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var croppedBitmap: Bitmap? = null
    private var isDrawMode = false
    
    // 🤖 IA AVANÇADA - Engines de detecção inteligente
    private lateinit var smartSelectionEngine: SmartSelectionEngine
    private lateinit var semanticSegmentationEngine: SemanticSegmentationEngine
    private var aiAnalysisJob: Job? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configura fullscreen
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN,
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        setContentView(R.layout.activity_image_viewer)
        
        // Obtém a URI da imagem passada pela MainActivity
        imageUri = intent.getParcelableExtra(EXTRA_IMAGE_URI)
        if (imageUri == null) {
            Log.e(TAG, "onCreate: URI da imagem é null!")
            Toast.makeText(this, "Erro: Imagem não encontrada", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        setupViews()
        loadImage()
        setupClickListeners()
        setupDrawView()
        
        // 🤖 Inicializa engines de IA
        initializeAIEngines()
        
        // Ativa o modo de desenho automaticamente
        activateDrawModeAutomatically()
    }
    
    private fun setupViews() {
        fullscreenImageView = findViewById(R.id.fullscreenImageView)
        btnClose = findViewById(R.id.btnClose)
        btnCrop = findViewById(R.id.btnCrop)
        btnClear = findViewById(R.id.btnClear)
        btnSave = findViewById(R.id.btnSave)
        btnShare = findViewById(R.id.btnShare)
        freeDrawCropView = findViewById(R.id.freeDrawCropView)
    }
    
    private fun setupDrawView() {
        freeDrawCropView.setOnDrawingChangedListener { path, points ->
            Log.d(TAG, "setupDrawView: Desenho alterado - ${points.size} pontos")
            // Análise inteligente em tempo real
            analyzeDrawingForSmartSelection(points)
        }
        
        freeDrawCropView.setOnDrawingCompletedListener { path, points ->
            Log.d(TAG, "setupDrawView: Desenho concluído - ${points.size} pontos")
            // 🤖 Análise final com IA avançada
            performAdvancedIntelligentSelection(points)
        }
        
        // Inicialmente esconde a view de desenho (será ativada automaticamente)
        freeDrawCropView.visibility = View.GONE
    }
    
    private fun activateDrawModeAutomatically() {
        // Aguarda um pouco para a imagem carregar completamente
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isDestroyed && !isFinishing) {
                isDrawMode = true
                freeDrawCropView.visibility = View.VISIBLE
                btnCrop.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
                btnClear.visibility = View.VISIBLE
                
                // Toast mais sutil
                Toast.makeText(this, "✏️ Desenhe ao redor da área desejada", Toast.LENGTH_LONG).show()
                Log.d(TAG, "activateDrawModeAutomatically: Modo de desenho ativado automaticamente")
            }
        }, 500) // 500ms de delay
    }
    
    private fun analyzeDrawingForSmartSelection(points: List<PointF>) {
        if (points.size < 15) return // Aguarda ter pontos suficientes
        
        Log.d(TAG, "analyzeDrawingForSmartSelection: Analisando ${points.size} pontos para seleção inteligente")
        
        // Análise básica da forma desenhada
        val bounds = calculateBounds(points)
        val aspectRatio = bounds.width() / bounds.height()
        val area = bounds.width() * bounds.height()
        
        Log.d(TAG, "analyzeDrawingForSmartSelection: Bounds: $bounds, AspectRatio: $aspectRatio, Area: $area")
        
        // Detecção básica de padrões
        when {
            isCircularShape(points) -> {
                Log.d(TAG, "analyzeDrawingForSmartSelection: Forma circular detectada - provável objeto/imagem")
                // Pode sugerir seleção automática de objetos circulares
            }
            isRectangularShape(points) -> {
                Log.d(TAG, "analyzeDrawingForSmartSelection: Forma retangular detectada - provável imagem/botão")
                // Pode sugerir seleção automática de elementos retangulares
            }
            aspectRatio > 3.0f -> {
                Log.d(TAG, "analyzeDrawingForSmartSelection: Forma alongada detectada - provável texto")
                // Pode sugerir seleção automática de linhas de texto
            }
            else -> {
                Log.d(TAG, "analyzeDrawingForSmartSelection: Forma irregular detectada - seleção livre")
            }
        }
    }
    
    private fun calculateBounds(points: List<PointF>): RectF {
        if (points.isEmpty()) return RectF()
        
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        
        for (point in points) {
            minX = minOf(minX, point.x)
            maxX = maxOf(maxX, point.x)
            minY = minOf(minY, point.y)
            maxY = maxOf(maxY, point.y)
        }
        
        return RectF(minX, minY, maxX, maxY)
    }
    
    private fun isCircularShape(points: List<PointF>): Boolean {
        if (points.size < 10) return false
        
        val bounds = calculateBounds(points)
        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val avgRadius = (bounds.width() + bounds.height()) / 4f
        
        // Verifica se os pontos estão aproximadamente em um círculo
        var circularPoints = 0
        for (point in points) {
            val distance = sqrt((point.x - centerX).pow(2) + (point.y - centerY).pow(2))
            val radiusDiff = abs(distance - avgRadius) / avgRadius
            if (radiusDiff < 0.3f) { // 30% de tolerância
                circularPoints++
            }
        }
        
        return circularPoints.toFloat() / points.size > 0.7f // 70% dos pontos devem estar no círculo
    }
    
    private fun isRectangularShape(points: List<PointF>): Boolean {
        if (points.size < 8) return false
        
        val bounds = calculateBounds(points)
        
        // Verifica se os pontos estão próximos das bordas do retângulo
        var edgePoints = 0
        for (point in points) {
            val nearLeft = abs(point.x - bounds.left) < 20f
            val nearRight = abs(point.x - bounds.right) < 20f
            val nearTop = abs(point.y - bounds.top) < 20f
            val nearBottom = abs(point.y - bounds.bottom) < 20f
            
            if ((nearLeft || nearRight) && (point.y >= bounds.top - 20f && point.y <= bounds.bottom + 20f) ||
                (nearTop || nearBottom) && (point.x >= bounds.left - 20f && point.x <= bounds.right + 20f)) {
                edgePoints++
            }
        }
        
        return edgePoints.toFloat() / points.size > 0.6f // 60% dos pontos devem estar nas bordas
    }
    
    private fun performIntelligentSelection(points: List<PointF>) {
        if (originalBitmap == null || points.isEmpty()) {
            Log.w(TAG, "performIntelligentSelection: Bitmap ou pontos inválidos")
            return
        }
        
        Log.d(TAG, "performIntelligentSelection: Iniciando análise inteligente...")
        
        // Converte coordenadas da tela para coordenadas da imagem
        val drawingBounds = calculateBounds(points)
        val imageBounds = convertScreenBoundsToImageBounds(drawingBounds)
        
        // Analisa a região da imagem para detectar objetos
        val detectedObjects = analyzeImageRegion(imageBounds)
        
        if (detectedObjects.isNotEmpty()) {
            // Encontrou objetos - ajusta a seleção para o objeto mais provável
            val bestObject = selectBestObject(detectedObjects, imageBounds)
            
            if (bestObject != null) {
                Log.d(TAG, "performIntelligentSelection: Objeto detectado: $bestObject")
                
                // Converte as coordenadas do objeto de volta para a tela
                val screenBounds = convertImageBoundsToScreenBounds(bestObject)
                
                // Ajusta automaticamente a seleção
                freeDrawCropView.adjustSelectionToRect(screenBounds)
                
                // Feedback visual
                Toast.makeText(this, "🎯 Objeto detectado e selecionado automaticamente!", Toast.LENGTH_SHORT).show()
                return
            }
        }
        
        Log.d(TAG, "performIntelligentSelection: Nenhum objeto claro detectado - mantendo seleção manual")
    }
    
    private fun convertScreenBoundsToImageBounds(screenBounds: RectF): RectF {
        val imageWidth = originalBitmap!!.width
        val imageHeight = originalBitmap!!.height
        val viewWidth = fullscreenImageView.width
        val viewHeight = fullscreenImageView.height
        
        // Calcula a escala da imagem (considera o scaleType fitCenter)
        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val viewAspectRatio = viewWidth.toFloat() / viewHeight.toFloat()
        
        val scale: Float
        val offsetX: Float
        val offsetY: Float
        
        if (imageAspectRatio > viewAspectRatio) {
            // Imagem é mais larga - ajusta pela altura
            scale = imageHeight.toFloat() / viewHeight.toFloat()
            offsetX = (viewWidth - imageWidth / scale) / 2f
            offsetY = 0f
        } else {
            // Imagem é mais alta - ajusta pela largura
            scale = imageWidth.toFloat() / viewWidth.toFloat()
            offsetX = 0f
            offsetY = (viewHeight - imageHeight / scale) / 2f
        }
        
        return RectF(
            (screenBounds.left - offsetX) * scale,
            (screenBounds.top - offsetY) * scale,
            (screenBounds.right - offsetX) * scale,
            (screenBounds.bottom - offsetY) * scale
        )
    }
    
    private fun convertImageBoundsToScreenBounds(imageBounds: RectF): RectF {
        val imageWidth = originalBitmap!!.width
        val imageHeight = originalBitmap!!.height
        val viewWidth = fullscreenImageView.width
        val viewHeight = fullscreenImageView.height
        
        // Calcula a escala da imagem (considera o scaleType fitCenter)
        val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val viewAspectRatio = viewWidth.toFloat() / viewHeight.toFloat()
        
        val scale: Float
        val offsetX: Float
        val offsetY: Float
        
        if (imageAspectRatio > viewAspectRatio) {
            // Imagem é mais larga - ajusta pela altura
            scale = viewHeight.toFloat() / imageHeight.toFloat()
            offsetX = (viewWidth - imageWidth * scale) / 2f
            offsetY = 0f
        } else {
            // Imagem é mais alta - ajusta pela largura
            scale = viewWidth.toFloat() / imageWidth.toFloat()
            offsetX = 0f
            offsetY = (viewHeight - imageHeight * scale) / 2f
        }
        
        return RectF(
            imageBounds.left * scale + offsetX,
            imageBounds.top * scale + offsetY,
            imageBounds.right * scale + offsetX,
            imageBounds.bottom * scale + offsetY
        )
    }
    
    private fun analyzeImageRegion(bounds: RectF): List<RectF> {
        val detectedObjects = mutableListOf<RectF>()
        
        try {
            // Extrai a região de interesse da imagem
            val regionBitmap = extractImageRegion(bounds)
            if (regionBitmap == null) return detectedObjects
            
            // Análise básica de detecção de objetos usando contraste
            val objects = detectObjectsByContrast(regionBitmap, bounds)
            detectedObjects.addAll(objects)
            
            // Análise de detecção de retângulos (imagens, botões)
            val rectangles = detectRectangularObjects(regionBitmap, bounds)
            detectedObjects.addAll(rectangles)
            
            Log.d(TAG, "analyzeImageRegion: Detectados ${detectedObjects.size} objetos na região")
            
        } catch (e: Exception) {
            Log.e(TAG, "analyzeImageRegion: Erro na análise: ${e.message}", e)
        }
        
        return detectedObjects
    }
    
    private fun extractImageRegion(bounds: RectF): Bitmap? {
        return try {
            val left = bounds.left.toInt().coerceAtLeast(0)
            val top = bounds.top.toInt().coerceAtLeast(0)
            val width = (bounds.width().toInt()).coerceAtMost(originalBitmap!!.width - left)
            val height = (bounds.height().toInt()).coerceAtMost(originalBitmap!!.height - top)
            
            if (width <= 0 || height <= 0) return null
            
            Bitmap.createBitmap(originalBitmap!!, left, top, width, height)
        } catch (e: Exception) {
            Log.e(TAG, "extractImageRegion: Erro: ${e.message}", e)
            null
        }
    }
    
    private fun detectObjectsByContrast(bitmap: Bitmap, originalBounds: RectF): List<RectF> {
        // Algoritmo simples de detecção por contraste
        // TODO: Implementar algoritmo mais sofisticado de visão computacional
        
        // Por enquanto, retorna a região original expandida se detectar alto contraste
        val avgBrightness = calculateAverageBrightness(bitmap)
        val contrastLevel = calculateContrastLevel(bitmap)
        
        Log.d(TAG, "detectObjectsByContrast: Brilho médio: $avgBrightness, Contraste: $contrastLevel")
        
        if (contrastLevel > 50) { // Threshold para alto contraste
            // Expande ligeiramente a região original
            val expandedBounds = RectF(originalBounds)
            expandedBounds.inset(-20f, -20f)
            return listOf(expandedBounds)
        }
        
        return emptyList()
    }
    
    private fun detectRectangularObjects(bitmap: Bitmap, originalBounds: RectF): List<RectF> {
        // Detecção simples de objetos retangulares
        // TODO: Implementar detecção de bordas mais sofisticada
        
        val edges = detectEdges(bitmap)
        if (edges > 0.3f) { // Threshold para detecção de bordas
            Log.d(TAG, "detectRectangularObjects: Bordas detectadas: $edges")
            return listOf(originalBounds)
        }
        
        return emptyList()
    }
    
    private fun calculateAverageBrightness(bitmap: Bitmap): Float {
        var totalBrightness = 0f
        var pixelCount = 0
        
        val width = bitmap.width
        val height = bitmap.height
        
        // Amostra pixels em uma grade para performance
        for (x in 0 until width step 10) {
            for (y in 0 until height step 10) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                totalBrightness += (r + g + b) / 3f
                pixelCount++
            }
        }
        
        return if (pixelCount > 0) totalBrightness / pixelCount else 0f
    }
    
    private fun calculateContrastLevel(bitmap: Bitmap): Float {
        // Calcula a variação de brilho como medida de contraste
        val brightness = mutableListOf<Float>()
        
        val width = bitmap.width
        val height = bitmap.height
        
        for (x in 0 until width step 10) {
            for (y in 0 until height step 10) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                brightness.add((r + g + b) / 3f)
            }
        }
        
        if (brightness.size < 2) return 0f
        
        val avg = brightness.average().toFloat()
        val variance = brightness.map { (it - avg).pow(2) }.average().toFloat()
        
        return sqrt(variance)
    }
    
    private fun detectEdges(bitmap: Bitmap): Float {
        // Detecção simples de bordas usando diferença de pixels
        var edgeCount = 0
        var totalPixels = 0
        
        val width = bitmap.width
        val height = bitmap.height
        
        for (x in 1 until width - 1 step 5) {
            for (y in 1 until height - 1 step 5) {
                val center = bitmap.getPixel(x, y)
                val right = bitmap.getPixel(x + 1, y)
                val bottom = bitmap.getPixel(x, y + 1)
                
                val diffRight = abs(getBrightness(center) - getBrightness(right))
                val diffBottom = abs(getBrightness(center) - getBrightness(bottom))
                
                if (diffRight > 30 || diffBottom > 30) { // Threshold para detecção de borda
                    edgeCount++
                }
                totalPixels++
            }
        }
        
        return if (totalPixels > 0) edgeCount.toFloat() / totalPixels else 0f
    }
    
    private fun getBrightness(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r + g + b) / 3f
    }
    
    private fun selectBestObject(objects: List<RectF>, originalBounds: RectF): RectF? {
        if (objects.isEmpty()) return null
        
        // Seleciona o objeto que melhor se adequa à seleção original
        return objects.minByOrNull { obj ->
            // Calcula a distância entre o centro do objeto e o centro da seleção original
            val objCenterX = obj.centerX()
            val objCenterY = obj.centerY()
            val origCenterX = originalBounds.centerX()
            val origCenterY = originalBounds.centerY()
            
            sqrt((objCenterX - origCenterX).pow(2) + (objCenterY - origCenterY).pow(2))
        }
    }
    
    private fun loadImage() {
        try {
            Log.d(TAG, "loadImage: Carregando imagem: $imageUri")
            
            // Carrega a imagem em fullscreen
            fullscreenImageView.setImageURI(imageUri)
            
            // Carrega o bitmap original para operações
            val inputStream = contentResolver.openInputStream(imageUri!!)
            originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            Log.d(TAG, "loadImage: Imagem carregada com sucesso!")
            
        } catch (e: Exception) {
            Log.e(TAG, "loadImage: Erro ao carregar imagem: ${e.message}", e)
            Toast.makeText(this, "Erro ao carregar imagem: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun setupClickListeners() {
        // Botão fechar
        btnClose.setOnClickListener {
            Log.d(TAG, "setupClickListeners: Botão fechar clicado")
            finish()
        }
        
        // Botão desenho livre
        btnCrop.setOnClickListener {
            Log.d(TAG, "setupClickListeners: Botão desenho clicado")
            toggleDrawMode()
        }
        
        // Botão limpar desenho
        btnClear.setOnClickListener {
            Log.d(TAG, "setupClickListeners: Botão limpar clicado")
            clearDrawing()
        }
        
        // Botão salvar
        btnSave.setOnClickListener {
            Log.d(TAG, "setupClickListeners: Botão salvar clicado")
            if (isDrawMode && freeDrawCropView.hasDrawing()) {
                performFreeDrawCrop()
                if (croppedBitmap != null) {
                    saveCroppedImage()
                }
            } else {
                saveImage()
            }
        }
        
        // Botão compartilhar
        btnShare.setOnClickListener {
            Log.d(TAG, "setupClickListeners: Botão compartilhar clicado")
            if (isDrawMode && freeDrawCropView.hasDrawing()) {
                performFreeDrawCrop()
                if (croppedBitmap != null) {
                    shareCroppedImage()
                }
            } else {
                shareImage()
            }
        }
    }
    
    private fun shareImage() {
        try {
            Log.d(TAG, "shareImage: Compartilhando imagem...")
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Compartilhar imagem"))
            Log.d(TAG, "shareImage: Intent de compartilhamento enviado")
            
        } catch (e: Exception) {
            Log.e(TAG, "shareImage: Erro ao compartilhar: ${e.message}", e)
            Toast.makeText(this, "Erro ao compartilhar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun saveImage() {
        try {
            Log.d(TAG, "saveImage: Salvando imagem...")
            
            if (originalBitmap == null) {
                Log.w(TAG, "saveImage: Bitmap original é null!")
                Toast.makeText(this, "Erro: Imagem não pode ser salva", Toast.LENGTH_SHORT).show()
                return
            }
            
            // Cria nome único para o arquivo
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "Screenshot_$timeStamp.png"
            
            // Salva na pasta Pictures
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val imageFile = File(picturesDir, fileName)
            
            val outputStream = FileOutputStream(imageFile)
            originalBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            Log.d(TAG, "saveImage: Imagem salva em: ${imageFile.absolutePath}")
            Toast.makeText(this, "Imagem salva com sucesso!", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "saveImage: Erro ao salvar: ${e.message}", e)
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun editImage() {
        try {
            Log.d(TAG, "editImage: Abrindo editor de imagem...")
            
            // Abre o editor padrão do sistema
            val editIntent = Intent(Intent.ACTION_EDIT).apply {
                setDataAndType(imageUri, "image/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            if (editIntent.resolveActivity(packageManager) != null) {
                startActivity(editIntent)
            } else {
                Toast.makeText(this, "Nenhum editor de imagem encontrado", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "editImage: Erro ao abrir editor: ${e.message}", e)
            Toast.makeText(this, "Erro ao abrir editor: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun toggleDrawMode() {
        isDrawMode = !isDrawMode
        
        if (isDrawMode) {
            // Ativa o modo de desenho livre
            freeDrawCropView.visibility = View.VISIBLE
            btnCrop.setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            btnClear.visibility = View.VISIBLE
            Toast.makeText(this, "Modo de desenho ativado. Desenhe a área desejada!", Toast.LENGTH_LONG).show()
            Log.d(TAG, "toggleDrawMode: Modo de desenho ativado")
        } else {
            // Desativa o modo de desenho
            freeDrawCropView.visibility = View.GONE
            freeDrawCropView.clearDrawing()
            btnCrop.setImageResource(android.R.drawable.ic_menu_edit)
            btnClear.visibility = View.GONE
            croppedBitmap = null
            Toast.makeText(this, "Modo de desenho desativado", Toast.LENGTH_SHORT).show()
            Log.d(TAG, "toggleDrawMode: Modo de desenho desativado")
        }
    }
    
    private fun clearDrawing() {
        freeDrawCropView.clearDrawing()
        croppedBitmap = null
        Toast.makeText(this, "Desenho limpo!", Toast.LENGTH_SHORT).show()
        Log.d(TAG, "clearDrawing: Desenho limpo")
    }
    
    private fun performFreeDrawCrop(): Bitmap? {
        try {
            if (originalBitmap == null) {
                Log.w(TAG, "performFreeDrawCrop: Bitmap original é null!")
                return null
            }
            
            if (!freeDrawCropView.hasDrawing()) {
                Log.w(TAG, "performFreeDrawCrop: Nenhum desenho encontrado!")
                return null
            }
            
            val drawingPath = freeDrawCropView.getDrawingPath()
            val drawingBounds = freeDrawCropView.getDrawingBounds()
            
            Log.d(TAG, "performFreeDrawCrop: Recortando área desenhada - Bounds: $drawingBounds")
            
            // Converte as coordenadas da tela para coordenadas da imagem
            val imageWidth = originalBitmap!!.width
            val imageHeight = originalBitmap!!.height
            val viewWidth = fullscreenImageView.width
            val viewHeight = fullscreenImageView.height
            
            // Calcula a escala da imagem (considera o scaleType fitCenter)
            val imageAspectRatio = imageWidth.toFloat() / imageHeight.toFloat()
            val viewAspectRatio = viewWidth.toFloat() / viewHeight.toFloat()
            
            val scale: Float
            val offsetX: Float
            val offsetY: Float
            
            if (imageAspectRatio > viewAspectRatio) {
                // Imagem é mais larga - ajusta pela altura
                scale = imageHeight.toFloat() / viewHeight.toFloat()
                offsetX = (viewWidth - imageWidth / scale) / 2f
                offsetY = 0f
            } else {
                // Imagem é mais alta - ajusta pela largura
                scale = imageWidth.toFloat() / viewWidth.toFloat()
                offsetX = 0f
                offsetY = (viewHeight - imageHeight / scale) / 2f
            }
            
            // Converte o path para coordenadas da imagem
            val imageDrawingPath = Path()
            val pathMatrix = Matrix()
            pathMatrix.setTranslate(-offsetX, -offsetY)
            pathMatrix.postScale(scale, scale)
            drawingPath.transform(pathMatrix, imageDrawingPath)
            
            // Calcula bounds na imagem
            val imageBounds = RectF()
            imageDrawingPath.computeBounds(imageBounds, true)
            
            // Cria o bitmap recortado com fundo transparente
            val croppedWidth = imageBounds.width().toInt()
            val croppedHeight = imageBounds.height().toInt()
            
            if (croppedWidth <= 0 || croppedHeight <= 0) {
                Log.w(TAG, "performFreeDrawCrop: Dimensões inválidas: ${croppedWidth}x${croppedHeight}")
                return null
            }
            
            val croppedBitmap = Bitmap.createBitmap(croppedWidth, croppedHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(croppedBitmap)
            
            // Move o path para a origem do bitmap recortado
            val finalPath = Path(imageDrawingPath)
            finalPath.offset(-imageBounds.left, -imageBounds.top)
            
            // Aplica o clipping com a forma desenhada
            canvas.clipPath(finalPath)
            
            // Desenha a parte recortada da imagem
            val srcRect = Rect(
                imageBounds.left.toInt().coerceAtLeast(0),
                imageBounds.top.toInt().coerceAtLeast(0),
                imageBounds.right.toInt().coerceAtMost(imageWidth),
                imageBounds.bottom.toInt().coerceAtMost(imageHeight)
            )
            val dstRect = Rect(0, 0, croppedWidth, croppedHeight)
            
            canvas.drawBitmap(originalBitmap!!, srcRect, dstRect, null)
            
            Log.d(TAG, "performFreeDrawCrop: Recorte livre concluído - Tamanho: ${croppedWidth}x${croppedHeight}")
            this.croppedBitmap = croppedBitmap
            return croppedBitmap
            
        } catch (e: Exception) {
            Log.e(TAG, "performFreeDrawCrop: Erro ao recortar: ${e.message}", e)
            return null
        }
    }
    
    private fun saveCroppedImage() {
        try {
            Log.d(TAG, "saveCroppedImage: Salvando imagem recortada...")
            
            if (croppedBitmap == null) {
                croppedBitmap = performFreeDrawCrop()
                if (croppedBitmap == null) {
                    Toast.makeText(this, "Erro: Não foi possível recortar a imagem", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            
            // Cria nome único para o arquivo
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "FreeDrawCrop_$timeStamp.png"
            
            // Salva na pasta Pictures
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val imageFile = File(picturesDir, fileName)
            
            val outputStream = FileOutputStream(imageFile)
            croppedBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            Log.d(TAG, "saveCroppedImage: Imagem recortada salva em: ${imageFile.absolutePath}")
            Toast.makeText(this, "Imagem recortada salva com sucesso!", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Log.e(TAG, "saveCroppedImage: Erro ao salvar: ${e.message}", e)
            Toast.makeText(this, "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun shareCroppedImage() {
        try {
            Log.d(TAG, "shareCroppedImage: Compartilhando imagem recortada...")
            
            if (croppedBitmap == null) {
                croppedBitmap = performFreeDrawCrop()
                if (croppedBitmap == null) {
                    Toast.makeText(this, "Erro: Não foi possível recortar a imagem", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            
            // Salva temporariamente para compartilhar
            val tempFile = File(cacheDir, "temp_crop.png")
            val outputStream = FileOutputStream(tempFile)
            croppedBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            val tempUri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                tempFile
            )
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_STREAM, tempUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Compartilhar imagem recortada"))
            Log.d(TAG, "shareCroppedImage: Intent de compartilhamento enviado")
            
        } catch (e: Exception) {
            Log.e(TAG, "shareCroppedImage: Erro ao compartilhar: ${e.message}", e)
            Toast.makeText(this, "Erro ao compartilhar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 🤖 Inicializa os engines de IA avançada
     */
    private fun initializeAIEngines() {
        try {
            Log.d(TAG, "🤖 Inicializando engines de IA avançada...")
            
            smartSelectionEngine = SmartSelectionEngine(this)
            semanticSegmentationEngine = SemanticSegmentationEngine()
            
            Log.d(TAG, "✅ Engines de IA inicializados com sucesso!")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao inicializar IA: ${e.message}", e)
            Toast.makeText(this, "⚠️ IA avançada não disponível", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 🧠 Análise inteligente avançada com múltiplas IAs
     */
    private fun performAdvancedIntelligentSelection(points: List<PointF>) {
        if (originalBitmap == null || points.isEmpty()) {
            Log.w(TAG, "performAdvancedIntelligentSelection: Bitmap ou pontos inválidos")
            return
        }
        
        // Cancela análise anterior se ainda estiver rodando
        aiAnalysisJob?.cancel()
        
        aiAnalysisJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                Log.d(TAG, "🧠 Iniciando análise inteligente avançada...")
                
                // Mostra feedback visual
                Toast.makeText(this@ImageViewerActivity, "🤖 Analisando com IA...", Toast.LENGTH_SHORT).show()
                
                withContext(Dispatchers.Default) {
                    // Análise com Smart Selection Engine (combina todas as IAs)
                    val detectedObjects = smartSelectionEngine.analyzeRegion(
                        bitmap = originalBitmap!!,
                        userDrawnPoints = points
                    ) { progress ->
                        // Atualiza progresso na UI thread
                        launch(Dispatchers.Main) {
                            // Poderia mostrar um ProgressBar aqui
                            Log.d(TAG, "🔄 Progresso IA: $progress")
                        }
                    }
                    
                    // Análise adicional com segmentação semântica
                    val userBounds = calculateBounds(points)
                    val semanticObjects = semanticSegmentationEngine.segmentImage(
                        bitmap = originalBitmap!!,
                        userDrawnRegion = userBounds
                    ) { progress ->
                        launch(Dispatchers.Main) {
                            Log.d(TAG, "🧩 Progresso Segmentação: $progress")
                        }
                    }
                    
                    // Combina resultados de todas as análises
                    val allDetectedObjects = detectedObjects + semanticObjects
                    
                    withContext(Dispatchers.Main) {
                        processAIResults(allDetectedObjects, userBounds)
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Erro na análise IA: ${e.message}", e)
                
                // Fallback para análise básica
                performBasicIntelligentSelection(points)
                
                Toast.makeText(this@ImageViewerActivity, "⚠️ Usando detecção básica", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 🎯 Processa os resultados da análise de IA
     */
    private fun processAIResults(
        detectedObjects: List<SmartSelectionEngine.DetectedObject>,
        userBounds: RectF
    ) {
        Log.d(TAG, "🎯 Processando ${detectedObjects.size} objetos detectados...")
        
        if (detectedObjects.isEmpty()) {
            Log.d(TAG, "Nenhum objeto detectado - mantendo seleção manual")
            Toast.makeText(this, "🔍 Nenhum objeto específico detectado", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Seleciona o melhor objeto baseado na intenção do usuário
        val bestObject = smartSelectionEngine.selectBestObject(detectedObjects, userBounds)
        
        if (bestObject != null) {
            Log.d(TAG, "🎯 Melhor objeto: ${bestObject.type} (confiança: ${bestObject.confidence})")
            
            // Converte coordenadas da imagem para coordenadas da tela
            val screenBounds = convertImageBoundsToScreenBounds(bestObject.bounds)
            
            // Ajusta automaticamente a seleção
            freeDrawCropView.adjustSelectionToRect(screenBounds)
            
            // Feedback visual detalhado
            val objectTypeEmoji = getObjectTypeEmoji(bestObject.type)
            val confidencePercent = (bestObject.confidence * 100).toInt()
            
            Toast.makeText(
                this, 
                "$objectTypeEmoji ${bestObject.label ?: bestObject.type.name.lowercase()} detectado! ($confidencePercent%)",
                Toast.LENGTH_LONG
            ).show()
            
            Log.d(TAG, "✅ Seleção ajustada automaticamente para: ${bestObject.type}")
            
        } else {
            Log.d(TAG, "Nenhum objeto adequado encontrado - mantendo seleção manual")
            
            // Mostra informações sobre os objetos detectados
            val objectTypes = detectedObjects.map { it.type }.distinct()
            val typesText = objectTypes.joinToString(", ") { it.name.lowercase() }
            
            Toast.makeText(
                this,
                "🔍 Detectados: $typesText (mantendo sua seleção)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * 🔧 Análise básica quando IA avançada falha
     */
    private fun performBasicIntelligentSelection(points: List<PointF>) {
        // Mantém a análise básica original como fallback
        performIntelligentSelection(points)
    }
    
    /**
     * 🎨 Retorna emoji apropriado para cada tipo de objeto
     */
    private fun getObjectTypeEmoji(objectType: SmartSelectionEngine.ObjectType): String {
        return when (objectType) {
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
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Cleanup dos engines de IA
        aiAnalysisJob?.cancel()
        
        if (::smartSelectionEngine.isInitialized) {
            smartSelectionEngine.cleanup()
        }
        
        Log.d(TAG, "🧹 Cleanup dos engines de IA concluído")
    }
}
