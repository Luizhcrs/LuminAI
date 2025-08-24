package com.example.floatingbutton.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.example.floatingbutton.ai.opencv.OpenCVObjectDetector
import com.example.floatingbutton.ai.tensorflow.TensorFlowObjectDetector
import com.example.floatingbutton.ai.mlkit.MLKitTextDetector
import com.example.floatingbutton.ai.cache.AICache
import kotlinx.coroutines.*

/**
 * 🤖 Engine Principal de Seleção Inteligente
 * 
 * Combina múltiplas IAs para detecção automática de objetos:
 * - OpenCV: Detecção de contornos e formas
 * - TensorFlow Lite: Reconhecimento de objetos
 * - ML Kit: OCR e detecção de texto
 */
class SmartSelectionEngine(private val context: Context) {
    
    companion object {
        private const val TAG = "SmartSelectionEngine"
    }
    
    // Detectores de IA
    private val openCVDetector = OpenCVObjectDetector()
    private val tensorFlowDetector = TensorFlowObjectDetector(context)
    private val mlKitDetector = MLKitTextDetector()
    
    // 🚀 Sistema de cache para performance
    private val aiCache = AICache()
    
    // Configurações de detecção
    private val detectionScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    data class DetectedObject(
        val bounds: RectF,
        val type: ObjectType,
        val confidence: Float,
        val label: String? = null
    )
    
    enum class ObjectType {
        TEXT,           // Texto detectado pelo ML Kit
        IMAGE,          // Imagem/foto detectada
        BUTTON,         // Botão de interface
        ICON,           // Ícone
        FACE,           // Rosto humano
        OBJECT,         // Objeto genérico (TensorFlow)
        SHAPE,          // Forma geométrica (OpenCV)
        UNKNOWN         // Não identificado
    }
    
    /**
     * 🎯 Análise principal - combina todas as IAs
     */
    suspend fun analyzeRegion(
        bitmap: Bitmap,
        userDrawnPoints: List<PointF>,
        onProgress: (String) -> Unit = {}
    ): List<DetectedObject> = withContext(Dispatchers.Default) {
        
        Log.d(TAG, "🤖 Iniciando análise inteligente da região...")
        val userBounds = calculateBounds(userDrawnPoints)
        
        // 🚀 VERIFICA CACHE PRIMEIRO
        onProgress("Verificando cache...")
        val cachedResult = aiCache.getAnalysisResult(bitmap, userBounds, "smart_selection")
        
        if (cachedResult != null) {
            Log.d(TAG, "⚡ Cache hit! Usando resultado anterior")
            onProgress("Resultado encontrado no cache!")
            return@withContext cachedResult.detectedObjects
        }
        
        val detectedObjects = mutableListOf<DetectedObject>()
        
        try {
            onProgress("Analisando área selecionada...")
            
            // 🔍 ETAPA 1: Detecção de texto com ML Kit
            onProgress("Detectando texto...")
            val textObjects = detectWithCache(bitmap, userBounds, "mlkit_text") {
                mlKitDetector.detectText(bitmap, userBounds)
            }
            detectedObjects.addAll(textObjects)
            Log.d(TAG, "📝 ML Kit detectou ${textObjects.size} textos")
            
            // 🔍 ETAPA 2: Detecção de objetos com TensorFlow Lite
            onProgress("Reconhecendo objetos...")
            val tfObjects = detectWithCache(bitmap, userBounds, "tensorflow") {
                tensorFlowDetector.detectObjects(bitmap, userBounds)
            }
            detectedObjects.addAll(tfObjects)
            Log.d(TAG, "🧠 TensorFlow detectou ${tfObjects.size} objetos")
            
            // 🔍 ETAPA 3: Detecção de formas com OpenCV
            onProgress("Analisando formas...")
            val cvObjects = detectWithCache(bitmap, userBounds, "opencv") {
                openCVDetector.detectShapes(bitmap, userBounds)
            }
            detectedObjects.addAll(cvObjects)
            Log.d(TAG, "👁️ OpenCV detectou ${cvObjects.size} formas")
            
            // 🎯 ETAPA 4: Fusão e ranking dos resultados
            onProgress("Processando resultados...")
            val rankedObjects = rankAndMergeDetections(detectedObjects, userBounds)
            
            // 💾 ARMAZENA NO CACHE
            aiCache.cacheAnalysisResult(bitmap, userBounds, "smart_selection", rankedObjects)
            
            Log.d(TAG, "✅ Análise concluída: ${rankedObjects.size} objetos finais")
            return@withContext rankedObjects
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na análise inteligente: ${e.message}", e)
            return@withContext emptyList()
        }
    }
    
    /**
     * 🏆 Seleciona o melhor objeto baseado na intenção do usuário
     */
    fun selectBestObject(
        detectedObjects: List<DetectedObject>,
        userBounds: RectF
    ): DetectedObject? {
        
        if (detectedObjects.isEmpty()) return null
        
        // Scoring algorithm baseado em:
        // 1. Proximidade com a seleção do usuário
        // 2. Confiança da detecção
        // 3. Tipo de objeto (texto tem prioridade)
        
        return detectedObjects.maxByOrNull { obj ->
            val proximityScore = calculateProximityScore(obj.bounds, userBounds)
            val confidenceScore = obj.confidence
            val typeScore = getTypeScore(obj.type)
            
            Log.d(TAG, "🎯 Objeto ${obj.type}: proximidade=$proximityScore, confiança=$confidenceScore, tipo=$typeScore")
            
            proximityScore * 0.4f + confidenceScore * 0.3f + typeScore * 0.3f
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
    
    private fun rankAndMergeDetections(
        objects: List<DetectedObject>,
        userBounds: RectF
    ): List<DetectedObject> {
        
        // Remove duplicatas e mescla objetos sobrepostos
        val merged = mutableListOf<DetectedObject>()
        
        for (obj in objects.sortedByDescending { it.confidence }) {
            val overlapping = merged.find { existing ->
                calculateOverlap(obj.bounds, existing.bounds) > 0.5f
            }
            
            if (overlapping == null) {
                merged.add(obj)
            } else if (obj.confidence > overlapping.confidence) {
                merged.remove(overlapping)
                merged.add(obj)
            }
        }
        
        return merged.sortedByDescending { obj ->
            calculateProximityScore(obj.bounds, userBounds) * obj.confidence
        }
    }
    
    private fun calculateProximityScore(objBounds: RectF, userBounds: RectF): Float {
        val objCenterX = objBounds.centerX()
        val objCenterY = objBounds.centerY()
        val userCenterX = userBounds.centerX()
        val userCenterY = userBounds.centerY()
        
        val distance = kotlin.math.sqrt(
            (objCenterX - userCenterX) * (objCenterX - userCenterX) +
            (objCenterY - userCenterY) * (objCenterY - userCenterY)
        )
        
        val maxDistance = kotlin.math.sqrt(
            userBounds.width() * userBounds.width() + userBounds.height() * userBounds.height()
        )
        
        return 1f - (distance / maxDistance).coerceIn(0f, 1f)
    }
    
    private fun getTypeScore(type: ObjectType): Float {
        return when (type) {
            ObjectType.TEXT -> 1.0f      // Texto tem maior prioridade
            ObjectType.BUTTON -> 0.9f    // Botões são importantes
            ObjectType.IMAGE -> 0.8f     // Imagens são relevantes
            ObjectType.FACE -> 0.7f      // Rostos são interessantes
            ObjectType.ICON -> 0.6f      // Ícones são úteis
            ObjectType.OBJECT -> 0.5f    // Objetos genéricos
            ObjectType.SHAPE -> 0.4f     // Formas geométricas
            ObjectType.UNKNOWN -> 0.1f   // Desconhecido tem baixa prioridade
        }
    }
    
    private fun calculateOverlap(rect1: RectF, rect2: RectF): Float {
        val intersection = RectF()
        if (!intersection.setIntersect(rect1, rect2)) return 0f
        
        val intersectionArea = intersection.width() * intersection.height()
        val rect1Area = rect1.width() * rect1.height()
        val rect2Area = rect2.width() * rect2.height()
        val unionArea = rect1Area + rect2Area - intersectionArea
        
        return if (unionArea > 0) intersectionArea / unionArea else 0f
    }
    
    /**
     * 🚀 Detecção com cache inteligente
     */
    private suspend fun detectWithCache(
        bitmap: Bitmap,
        region: RectF,
        detectorType: String,
        detection: suspend () -> List<DetectedObject>
    ): List<DetectedObject> {
        
        // Verifica cache primeiro
        val cachedResult = aiCache.getAnalysisResult(bitmap, region, detectorType)
        
        if (cachedResult != null) {
            Log.d(TAG, "⚡ Cache hit para $detectorType")
            return cachedResult.detectedObjects
        }
        
        // Executa detecção se não estiver no cache
        val result = detection()
        
        // Armazena no cache
        aiCache.cacheAnalysisResult(bitmap, region, detectorType, result)
        
        return result
    }
    
    /**
     * 📊 Estatísticas de performance
     */
    fun getPerformanceStats(): String {
        return aiCache.getCacheStats()
    }
    
    fun cleanup() {
        detectionScope.cancel()
        aiCache.cleanup()
        openCVDetector.cleanup()
        tensorFlowDetector.cleanup()
        mlKitDetector.cleanup()
    }
}
