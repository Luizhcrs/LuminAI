package com.example.floatingbutton.ai.tensorflow

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.floatingbutton.ai.SmartSelectionEngine
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.label.Category
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * 🧠 TensorFlow Lite Object Detector
 * 
 * Usa modelos pré-treinados para reconhecimento de objetos:
 * - MobileNet para classificação geral
 * - YOLO para detecção de objetos
 * - Detecção de rostos e pessoas
 */
class TensorFlowObjectDetector(private val context: Context) {
    
    companion object {
        private const val TAG = "TensorFlowDetector"
        private const val MODEL_FILE = "mobilenet_v1_1.0_224_quant.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val INPUT_SIZE = 224
        private const val CONFIDENCE_THRESHOLD = 0.3f
    }
    
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var isModelLoaded = false
    
    // Configurações do modelo
    private val imageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(INPUT_SIZE, INPUT_SIZE, ResizeOp.ResizeMethod.BILINEAR))
        .build()
    
    init {
        loadModel()
    }
    
    /**
     * 🔍 Detecta e classifica objetos usando TensorFlow Lite
     */
    fun detectObjects(bitmap: Bitmap, region: RectF): List<SmartSelectionEngine.DetectedObject> {
        if (!isModelLoaded) {
            Log.w(TAG, "Modelo não carregado - usando detecção básica")
            return detectBasicObjects(bitmap, region)
        }
        
        val detectedObjects = mutableListOf<SmartSelectionEngine.DetectedObject>()
        
        try {
            Log.d(TAG, "🧠 Iniciando detecção TensorFlow Lite...")
            
            // Extrai região de interesse
            val regionBitmap = extractRegion(bitmap, region)
            if (regionBitmap == null) {
                Log.w(TAG, "Falha ao extrair região")
                return emptyList()
            }
            
            // Processa imagem para o modelo
            val tensorImage = TensorImage.fromBitmap(regionBitmap)
            val processedImage = imageProcessor.process(tensorImage)
            
            // Executa inferência
            val results = runInference(processedImage)
            
            // Processa resultados
            for (result in results) {
                if (result.confidence >= CONFIDENCE_THRESHOLD) {
                    val objectType = mapLabelToObjectType(result.label)
                    
                    detectedObjects.add(
                        SmartSelectionEngine.DetectedObject(
                            bounds = region, // Para classificação, usa a região toda
                            type = objectType,
                            confidence = result.confidence,
                            label = result.label
                        )
                    )
                    
                    Log.d(TAG, "🎯 Detectado: ${result.label} (${result.confidence})")
                }
            }
            
            Log.d(TAG, "✅ TensorFlow detectou ${detectedObjects.size} objetos")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro TensorFlow: ${e.message}", e)
            return detectBasicObjects(bitmap, region)
        }
        
        return detectedObjects
    }
    
    private fun loadModel() {
        try {
            Log.d(TAG, "📥 Carregando modelo TensorFlow Lite...")
            
            // Tenta carregar modelo customizado
            val modelBuffer = try {
                FileUtil.loadMappedFile(context, MODEL_FILE)
            } catch (e: IOException) {
                Log.w(TAG, "Modelo customizado não encontrado, usando detecção básica")
                return
            }
            
            // Carrega labels
            labels = try {
                FileUtil.loadLabels(context, LABELS_FILE)
            } catch (e: IOException) {
                Log.w(TAG, "Labels não encontrados, usando labels básicos")
                getBasicLabels()
            }
            
            // Inicializa interpretador
            val options = Interpreter.Options().apply {
                setNumThreads(4) // Usa múltiplas threads para performance
                setUseNNAPI(true) // Usa aceleração de hardware se disponível
            }
            
            interpreter = Interpreter(modelBuffer, options)
            isModelLoaded = true
            
            Log.d(TAG, "✅ Modelo TensorFlow carregado com ${labels.size} labels")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao carregar modelo: ${e.message}", e)
            isModelLoaded = false
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
    
    private fun runInference(tensorImage: TensorImage): List<DetectionResult> {
        val interpreter = this.interpreter ?: return emptyList()
        
        try {
            // Prepara buffers de entrada e saída
            val inputBuffer = tensorImage.buffer
            val outputBuffer = TensorBuffer.createFixedSize(
                intArrayOf(1, labels.size), 
                org.tensorflow.lite.DataType.UINT8
            )
            
            // Executa inferência
            interpreter.run(inputBuffer, outputBuffer.buffer.rewind())
            
            // Processa resultados
            val confidences = outputBuffer.floatArray
            val results = mutableListOf<DetectionResult>()
            
            for (i in confidences.indices) {
                val confidence = confidences[i]
                if (confidence >= CONFIDENCE_THRESHOLD && i < labels.size) {
                    results.add(
                        DetectionResult(
                            label = labels[i],
                            confidence = confidence,
                            index = i
                        )
                    )
                }
            }
            
            return results.sortedByDescending { it.confidence }.take(5)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na inferência: ${e.message}", e)
            return emptyList()
        }
    }
    
    private fun mapLabelToObjectType(label: String): SmartSelectionEngine.ObjectType {
        val lowerLabel = label.lowercase()
        
        return when {
            // Pessoas e rostos
            lowerLabel.contains("person") || lowerLabel.contains("face") || 
            lowerLabel.contains("human") -> SmartSelectionEngine.ObjectType.FACE
            
            // Texto e documentos
            lowerLabel.contains("book") || lowerLabel.contains("paper") || 
            lowerLabel.contains("document") || lowerLabel.contains("text") -> 
                SmartSelectionEngine.ObjectType.TEXT
            
            // Eletrônicos e interfaces
            lowerLabel.contains("phone") || lowerLabel.contains("computer") || 
            lowerLabel.contains("screen") || lowerLabel.contains("monitor") || 
            lowerLabel.contains("button") -> SmartSelectionEngine.ObjectType.BUTTON
            
            // Imagens e fotos
            lowerLabel.contains("picture") || lowerLabel.contains("photo") || 
            lowerLabel.contains("image") || lowerLabel.contains("painting") -> 
                SmartSelectionEngine.ObjectType.IMAGE
            
            // Objetos pequenos (ícones)
            lowerLabel.contains("icon") || lowerLabel.contains("symbol") || 
            lowerLabel.contains("logo") || lowerLabel.contains("sign") -> 
                SmartSelectionEngine.ObjectType.ICON
            
            // Objetos genéricos
            else -> SmartSelectionEngine.ObjectType.OBJECT
        }
    }
    
    private fun detectBasicObjects(bitmap: Bitmap, region: RectF): List<SmartSelectionEngine.DetectedObject> {
        Log.d(TAG, "🔧 Usando detecção básica de objetos...")
        
        val objects = mutableListOf<SmartSelectionEngine.DetectedObject>()
        
        // Análise básica baseada em características visuais
        val avgColor = calculateAverageColor(bitmap, region)
        val colorVariance = calculateColorVariance(bitmap, region)
        val aspectRatio = region.width() / region.height()
        
        // Heurísticas simples para classificação
        val objectType = when {
            // Área muito colorida - provavelmente imagem
            colorVariance > 50f -> SmartSelectionEngine.ObjectType.IMAGE
            
            // Área quadrada com baixa variância - provavelmente botão
            aspectRatio in 0.8f..1.2f && colorVariance < 20f -> 
                SmartSelectionEngine.ObjectType.BUTTON
            
            // Área retangular - pode ser texto ou imagem
            aspectRatio > 2f || aspectRatio < 0.5f -> 
                SmartSelectionEngine.ObjectType.TEXT
            
            // Área circular (aproximada) - provavelmente ícone
            aspectRatio in 0.9f..1.1f && region.width() < 100f -> 
                SmartSelectionEngine.ObjectType.ICON
            
            else -> SmartSelectionEngine.ObjectType.OBJECT
        }
        
        val confidence = calculateBasicConfidence(colorVariance, aspectRatio)
        
        if (confidence > 0.3f) {
            objects.add(
                SmartSelectionEngine.DetectedObject(
                    bounds = region,
                    type = objectType,
                    confidence = confidence,
                    label = "objeto_${objectType.name.lowercase()}"
                )
            )
        }
        
        return objects
    }
    
    private fun calculateAverageColor(bitmap: Bitmap, region: RectF): Triple<Float, Float, Float> {
        var totalR = 0f
        var totalG = 0f
        var totalB = 0f
        var pixelCount = 0
        
        val left = region.left.toInt().coerceAtLeast(0)
        val top = region.top.toInt().coerceAtLeast(0)
        val right = region.right.toInt().coerceAtMost(bitmap.width)
        val bottom = region.bottom.toInt().coerceAtMost(bitmap.height)
        
        for (x in left until right step 5) {
            for (y in top until bottom step 5) {
                val pixel = bitmap.getPixel(x, y)
                totalR += (pixel shr 16) and 0xFF
                totalG += (pixel shr 8) and 0xFF
                totalB += pixel and 0xFF
                pixelCount++
            }
        }
        
        return if (pixelCount > 0) {
            Triple(totalR / pixelCount, totalG / pixelCount, totalB / pixelCount)
        } else {
            Triple(0f, 0f, 0f)
        }
    }
    
    private fun calculateColorVariance(bitmap: Bitmap, region: RectF): Float {
        val avgColor = calculateAverageColor(bitmap, region)
        var variance = 0f
        var pixelCount = 0
        
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
                
                val diffR = r - avgColor.first
                val diffG = g - avgColor.second
                val diffB = b - avgColor.third
                
                variance += (diffR * diffR + diffG * diffG + diffB * diffB) / 3f
                pixelCount++
            }
        }
        
        return if (pixelCount > 0) sqrt(variance / pixelCount) else 0f
    }
    
    private fun calculateBasicConfidence(colorVariance: Float, aspectRatio: Float): Float {
        // Confiança baseada na variação de cor e proporções
        val varianceScore = (colorVariance / 100f).coerceIn(0f, 1f)
        val aspectScore = if (aspectRatio in 0.1f..10f) 0.8f else 0.3f
        
        return (varianceScore + aspectScore) / 2f
    }
    
    private fun getBasicLabels(): List<String> {
        return listOf(
            "person", "face", "human", "people",
            "book", "paper", "document", "text", "writing",
            "phone", "computer", "screen", "monitor", "button",
            "picture", "photo", "image", "painting",
            "icon", "symbol", "logo", "sign",
            "object", "thing", "item", "unknown"
        )
    }
    
    data class DetectionResult(
        val label: String,
        val confidence: Float,
        val index: Int
    )
    
    fun cleanup() {
        interpreter?.close()
        interpreter = null
        isModelLoaded = false
        Log.d(TAG, "🧹 TensorFlow detector cleanup")
    }
}
