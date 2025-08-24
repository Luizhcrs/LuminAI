package com.example.floatingbutton.ai.mlkit

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.floatingbutton.ai.SmartSelectionEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.*

/**
 * 📝 ML Kit Text & Object Detector
 * 
 * Usa Google ML Kit para:
 * - OCR (Reconhecimento de texto)
 * - Detecção de objetos genéricos
 * - Análise de layout e estrutura
 * - Detecção de faces (opcional)
 */
class MLKitTextDetector {
    
    companion object {
        private const val TAG = "MLKitTextDetector"
        private const val MIN_TEXT_CONFIDENCE = 0.7f
        private const val MIN_OBJECT_CONFIDENCE = 0.5f
    }
    
    // Detectores ML Kit
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    
    private val objectDetector = ObjectDetection.getClient(
        ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()
    )
    
    /**
     * 📝 Detecta texto na região especificada
     */
    suspend fun detectText(bitmap: Bitmap, region: RectF): List<SmartSelectionEngine.DetectedObject> {
        val detectedObjects = mutableListOf<SmartSelectionEngine.DetectedObject>()
        
        try {
            Log.d(TAG, "📝 Iniciando detecção de texto ML Kit...")
            
            // Extrai região de interesse
            val regionBitmap = extractRegion(bitmap, region)
            if (regionBitmap == null) {
                Log.w(TAG, "Falha ao extrair região para texto")
                return emptyList()
            }
            
            // Converte para InputImage do ML Kit
            val inputImage = InputImage.fromBitmap(regionBitmap, 0)
            
            // Executa reconhecimento de texto
            val textResult = recognizeText(inputImage)
            
            if (textResult == null) {
                Log.w(TAG, "⚠️ Falha no reconhecimento de texto")
                // Continua sem o reconhecimento de texto
            } else {
                // Processa blocos de texto detectados
                for (textBlock in textResult.textBlocks) {
                    val blockBounds = convertToAbsoluteBounds(textBlock.boundingBox, region, regionBitmap)
                    
                    if (blockBounds != null) {
                        val confidence = calculateTextConfidence(textBlock.text)
                        
                        if (confidence >= MIN_TEXT_CONFIDENCE) {
                            detectedObjects.add(
                                SmartSelectionEngine.DetectedObject(
                                    bounds = blockBounds,
                                    type = SmartSelectionEngine.ObjectType.TEXT,
                                    confidence = confidence,
                                    label = "texto: \"${textBlock.text.take(30)}${if (textBlock.text.length > 30) "..." else ""}\""
                                )
                            )
                            
                            Log.d(TAG, "📝 Texto detectado: \"${textBlock.text.take(50)}\" (confiança: $confidence)")
                        }
                    }
                }
            }
            
            // Detecta também objetos genéricos
            try {
                val objectResults = detectObjects(inputImage, region, regionBitmap)
                detectedObjects.addAll(objectResults)
            } catch (objectException: Exception) {
                Log.w(TAG, "⚠️ Erro na detecção de objetos: ${objectException.message}")
                // Continua sem a detecção de objetos
            }
            
            Log.d(TAG, "✅ ML Kit detectou ${detectedObjects.size} objetos/textos")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ML Kit: ${e.message}", e)
        }
        
        return detectedObjects
    }
    
    private suspend fun recognizeText(inputImage: InputImage): com.google.mlkit.vision.text.Text? = suspendCancellableCoroutine { continuation ->
        textRecognizer.process(inputImage)
            .addOnSuccessListener { result ->
                continuation.resume(result)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Erro no reconhecimento de texto: ${exception.message}", exception)
                // Em caso de erro, retorna null
                continuation.resume(null)
            }
    }
    
    private suspend fun detectObjects(
        inputImage: InputImage, 
        originalRegion: RectF, 
        regionBitmap: Bitmap
    ): List<SmartSelectionEngine.DetectedObject> = suspendCancellableCoroutine { continuation ->
        
        objectDetector.process(inputImage)
            .addOnSuccessListener { detectedObjects ->
                val results = mutableListOf<SmartSelectionEngine.DetectedObject>()
                
                for (detectedObject in detectedObjects) {
                    val bounds = convertToAbsoluteBounds(detectedObject.boundingBox, originalRegion, regionBitmap)
                    
                    if (bounds != null && detectedObject.labels.isNotEmpty()) {
                        val bestLabel = detectedObject.labels.maxByOrNull { it.confidence }
                        
                        if (bestLabel != null && bestLabel.confidence >= MIN_OBJECT_CONFIDENCE) {
                            val objectType = mapMLKitLabelToObjectType(bestLabel.text)
                            
                            results.add(
                                SmartSelectionEngine.DetectedObject(
                                    bounds = bounds,
                                    type = objectType,
                                    confidence = bestLabel.confidence,
                                    label = bestLabel.text
                                )
                            )
                            
                            Log.d(TAG, "🔍 Objeto detectado: ${bestLabel.text} (${bestLabel.confidence})")
                        }
                    }
                }
                
                continuation.resume(results)
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Erro na detecção de objetos: ${exception.message}", exception)
                continuation.resume(emptyList())
            }
    }
    
    private fun extractRegion(bitmap: Bitmap, region: RectF): Bitmap? {
        return try {
            val left = region.left.toInt().coerceAtLeast(0)
            val top = region.top.toInt().coerceAtLeast(0)
            val width = region.width().toInt().coerceAtMost(bitmap.width - left)
            val height = region.height().toInt().coerceAtMost(bitmap.height - top)
            
            if (width > 0 && height > 0) {
                Bitmap.createBitmap(bitmap, left, top, width, height)
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao extrair região: ${e.message}", e)
            null
        }
    }
    
    private fun convertToAbsoluteBounds(
        relativeBounds: android.graphics.Rect?,
        originalRegion: RectF,
        regionBitmap: Bitmap
    ): RectF? {
        
        if (relativeBounds == null) return null
        
        try {
            // Calcula escala entre região original e bitmap extraído
            val scaleX = originalRegion.width() / regionBitmap.width
            val scaleY = originalRegion.height() / regionBitmap.height
            
            return RectF(
                originalRegion.left + relativeBounds.left * scaleX,
                originalRegion.top + relativeBounds.top * scaleY,
                originalRegion.left + relativeBounds.right * scaleX,
                originalRegion.top + relativeBounds.bottom * scaleY
            )
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao converter bounds: ${e.message}", e)
            return null
        }
    }
    
    private fun calculateTextConfidence(text: String): Float {
        // Heurísticas para calcular confiança do texto
        val length = text.length
        val wordCount = text.split("\\s+".toRegex()).size
        val hasNumbers = text.any { it.isDigit() }
        val hasLetters = text.any { it.isLetter() }
        val hasSpecialChars = text.any { !it.isLetterOrDigit() && !it.isWhitespace() }
        
        var confidence = 0.5f
        
        // Texto com tamanho adequado
        if (length in 3..100) confidence += 0.2f
        
        // Texto com palavras reconhecíveis
        if (wordCount >= 2) confidence += 0.1f
        
        // Texto misto (letras + números) é mais confiável
        if (hasLetters && hasNumbers) confidence += 0.1f
        
        // Penaliza texto com muitos caracteres especiais (pode ser ruído)
        if (hasSpecialChars && text.count { !it.isLetterOrDigit() && !it.isWhitespace() } > length / 3) {
            confidence -= 0.2f
        }
        
        // Texto muito curto ou muito longo é menos confiável
        if (length < 2 || length > 200) confidence -= 0.2f
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private fun mapMLKitLabelToObjectType(label: String): SmartSelectionEngine.ObjectType {
        val lowerLabel = label.lowercase()
        
        return when {
            // Texto e documentos
            lowerLabel.contains("text") || lowerLabel.contains("document") || 
            lowerLabel.contains("paper") || lowerLabel.contains("book") -> 
                SmartSelectionEngine.ObjectType.TEXT
            
            // Pessoas e faces
            lowerLabel.contains("person") || lowerLabel.contains("face") || 
            lowerLabel.contains("human") -> SmartSelectionEngine.ObjectType.FACE
            
            // Elementos de interface
            lowerLabel.contains("button") || lowerLabel.contains("icon") || 
            lowerLabel.contains("symbol") -> SmartSelectionEngine.ObjectType.BUTTON
            
            // Imagens e mídia
            lowerLabel.contains("picture") || lowerLabel.contains("photo") || 
            lowerLabel.contains("image") -> SmartSelectionEngine.ObjectType.IMAGE
            
            // Objetos pequenos
            lowerLabel.contains("logo") || lowerLabel.contains("sign") || 
            lowerLabel.contains("badge") -> SmartSelectionEngine.ObjectType.ICON
            
            // Padrão
            else -> SmartSelectionEngine.ObjectType.OBJECT
        }
    }
    
    /**
     * 🔍 Análise avançada de layout para detectar padrões de interface
     */
    fun analyzeLayoutPatterns(bitmap: Bitmap, region: RectF): List<SmartSelectionEngine.DetectedObject> {
        val objects = mutableListOf<SmartSelectionEngine.DetectedObject>()
        
        try {
            // Detecta padrões comuns de UI
            val buttonPatterns = detectButtonPatterns(bitmap, region)
            objects.addAll(buttonPatterns)
            
            val textPatterns = detectTextPatterns(bitmap, region)
            objects.addAll(textPatterns)
            
            val imagePatterns = detectImagePatterns(bitmap, region)
            objects.addAll(imagePatterns)
            
            Log.d(TAG, "📐 Análise de layout encontrou ${objects.size} padrões")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na análise de layout: ${e.message}", e)
        }
        
        return objects
    }
    
    private fun detectButtonPatterns(bitmap: Bitmap, region: RectF): List<SmartSelectionEngine.DetectedObject> {
        // Detecta padrões visuais típicos de botões:
        // - Bordas definidas
        // - Cores uniformes
        // - Proporções específicas
        
        val aspectRatio = region.width() / region.height()
        val area = region.width() * region.height()
        
        // Heurística para botões
        if (aspectRatio in 1.5f..4f && area in 2000f..50000f) {
            val uniformity = calculateColorUniformity(bitmap, region)
            
            if (uniformity > 0.7f) {
                return listOf(
                    SmartSelectionEngine.DetectedObject(
                        bounds = region,
                        type = SmartSelectionEngine.ObjectType.BUTTON,
                        confidence = uniformity,
                        label = "botão_detectado"
                    )
                )
            }
        }
        
        return emptyList()
    }
    
    private fun detectTextPatterns(bitmap: Bitmap, region: RectF): List<SmartSelectionEngine.DetectedObject> {
        // Detecta padrões visuais típicos de texto:
        // - Linhas horizontais
        // - Espaçamento regular
        // - Contraste adequado
        
        val aspectRatio = region.width() / region.height()
        
        // Texto geralmente tem proporção horizontal
        if (aspectRatio > 2f) {
            val contrast = calculateRegionContrast(bitmap, region)
            
            if (contrast > 0.4f) {
                return listOf(
                    SmartSelectionEngine.DetectedObject(
                        bounds = region,
                        type = SmartSelectionEngine.ObjectType.TEXT,
                        confidence = contrast,
                        label = "padrão_texto"
                    )
                )
            }
        }
        
        return emptyList()
    }
    
    private fun detectImagePatterns(bitmap: Bitmap, region: RectF): List<SmartSelectionEngine.DetectedObject> {
        // Detecta padrões visuais típicos de imagens:
        // - Alta variação de cores
        // - Gradientes
        // - Texturas complexas
        
        val colorVariation = calculateColorVariation(bitmap, region)
        val aspectRatio = region.width() / region.height()
        
        // Imagens têm alta variação de cor e proporções variadas
        if (colorVariation > 0.6f && aspectRatio in 0.5f..2f) {
            return listOf(
                SmartSelectionEngine.DetectedObject(
                    bounds = region,
                    type = SmartSelectionEngine.ObjectType.IMAGE,
                    confidence = colorVariation,
                    label = "padrão_imagem"
                )
            )
        }
        
        return emptyList()
    }
    
    private fun calculateColorUniformity(bitmap: Bitmap, region: RectF): Float {
        val colors = mutableListOf<Int>()
        
        val left = region.left.toInt().coerceAtLeast(0)
        val top = region.top.toInt().coerceAtLeast(0)
        val right = region.right.toInt().coerceAtMost(bitmap.width)
        val bottom = region.bottom.toInt().coerceAtMost(bitmap.height)
        
        // Amostra cores da região
        for (x in left until right step 10) {
            for (y in top until bottom step 10) {
                colors.add(bitmap.getPixel(x, y))
            }
        }
        
        if (colors.isEmpty()) return 0f
        
        // Calcula uniformidade baseada na variação de cores
        val avgR = colors.map { (it shr 16) and 0xFF }.average()
        val avgG = colors.map { (it shr 8) and 0xFF }.average()
        val avgB = colors.map { it and 0xFF }.average()
        
        val variance = colors.map { color ->
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            
            (r - avgR).pow(2.0) + (g - avgG).pow(2.0) + (b - avgB).pow(2.0)
        }.average()
        
        return (1f - (sqrt(variance.toFloat()) / 255.0f)).coerceIn(0f, 1f)
    }
    
    private fun calculateRegionContrast(bitmap: Bitmap, region: RectF): Float {
        val brightnesses = mutableListOf<Float>()
        
        val left = region.left.toInt().coerceAtLeast(0)
        val top = region.top.toInt().coerceAtLeast(0)
        val right = region.right.toInt().coerceAtMost(bitmap.width)
        val bottom = region.bottom.toInt().coerceAtMost(bitmap.height)
        
        for (x in left until right step 5) {
            for (y in top until bottom step 5) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                brightnesses.add((r + g + b) / 3f)
            }
        }
        
        if (brightnesses.size < 2) return 0f
        
        val avg = brightnesses.average().toFloat()
        val variance = brightnesses.map { (it - avg).pow(2f) }.average().toFloat()
        
        return (sqrt(variance) / 255f).coerceIn(0f, 1f)
    }
    
    private fun calculateColorVariation(bitmap: Bitmap, region: RectF): Float {
        val pixels = mutableListOf<Triple<Int, Int, Int>>()
        
        val left = region.left.toInt().coerceAtLeast(0)
        val top = region.top.toInt().coerceAtLeast(0)
        val right = region.right.toInt().coerceAtMost(bitmap.width)
        val bottom = region.bottom.toInt().coerceAtMost(bitmap.height)
        
        for (x in left until right step 8) {
            for (y in top until bottom step 8) {
                val pixel = bitmap.getPixel(x, y)
                pixels.add(
                    Triple(
                        (pixel shr 16) and 0xFF,
                        (pixel shr 8) and 0xFF,
                        pixel and 0xFF
                    )
                )
            }
        }
        
        if (pixels.size < 2) return 0f
        
        // Calcula variação total de cores
        val avgR = pixels.map { it.first }.average()
        val avgG = pixels.map { it.second }.average()
        val avgB = pixels.map { it.third }.average()
        
        val totalVariation = pixels.map { (r, g, b) ->
            sqrt((r - avgR).pow(2.0) + (g - avgG).pow(2.0) + (b - avgB).pow(2.0))
        }.average()
        
        return (totalVariation / 255.0).coerceIn(0.0, 1.0).toFloat()
    }
    
    fun cleanup() {
        textRecognizer.close()
        objectDetector.close()
        Log.d(TAG, "🧹 ML Kit detector cleanup")
    }
    

}
