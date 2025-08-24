package com.example.floatingbutton.ai.cache

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import android.util.LruCache
import com.example.floatingbutton.ai.SmartSelectionEngine
import kotlinx.coroutines.*
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * 🚀 AI Cache System
 * 
 * Sistema de cache inteligente para otimizar performance:
 * - Cache de resultados de análise
 * - Cache de bitmaps processados
 * - Invalidação automática
 * - Compressão de dados
 */
class AICache {
    
    companion object {
        private const val TAG = "AICache"
        private const val MAX_CACHE_SIZE_MB = 50 // 50MB de cache
        private const val BITMAP_CACHE_SIZE = 20 // 20 bitmaps
        private const val ANALYSIS_CACHE_SIZE = 100 // 100 análises
    }
    
    // Cache de resultados de análise
    private val analysisCache = LruCache<String, CachedAnalysisResult>(ANALYSIS_CACHE_SIZE)
    
    // Cache de bitmaps processados
    private val bitmapCache = object : LruCache<String, Bitmap>(BITMAP_CACHE_SIZE) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount
        }
    }
    
    // Cache de metadados de imagem
    private val metadataCache = LruCache<String, ImageMetadata>(200)
    
    // Jobs de limpeza automática
    private var cleanupJob: Job? = null
    private val cacheScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    
    data class CachedAnalysisResult(
        val detectedObjects: List<SmartSelectionEngine.DetectedObject>,
        val timestamp: Long,
        val imageHash: String,
        val regionHash: String,
        val confidence: Float
    )
    
    data class ImageMetadata(
        val width: Int,
        val height: Int,
        val averageColor: Triple<Float, Float, Float>,
        val complexity: Float, // Medida de complexidade visual
        val dominantColors: List<Int>,
        val timestamp: Long
    )
    
    init {
        startPeriodicCleanup()
        Log.d(TAG, "🚀 AI Cache inicializado")
    }
    
    /**
     * 🔍 Busca resultado de análise no cache
     */
    fun getAnalysisResult(
        bitmap: Bitmap,
        region: RectF,
        analysisType: String
    ): CachedAnalysisResult? {
        
        try {
            val imageHash = generateImageHash(bitmap)
            val regionHash = generateRegionHash(region)
            val cacheKey = "$analysisType:$imageHash:$regionHash"
            
            val cachedResult = analysisCache.get(cacheKey)
            
            if (cachedResult != null) {
                // Verifica se o cache ainda é válido (não muito antigo)
                val ageMinutes = (System.currentTimeMillis() - cachedResult.timestamp) / (1000 * 60)
                
                if (ageMinutes < 30) { // Cache válido por 30 minutos
                    Log.d(TAG, "✅ Cache hit para análise: $analysisType")
                    return cachedResult
                } else {
                    // Remove cache expirado
                    analysisCache.remove(cacheKey)
                    Log.d(TAG, "🕐 Cache expirado removido: $analysisType")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar cache: ${e.message}", e)
        }
        
        return null
    }
    
    /**
     * 💾 Armazena resultado de análise no cache
     */
    fun cacheAnalysisResult(
        bitmap: Bitmap,
        region: RectF,
        analysisType: String,
        detectedObjects: List<SmartSelectionEngine.DetectedObject>
    ) {
        
        try {
            val imageHash = generateImageHash(bitmap)
            val regionHash = generateRegionHash(region)
            val cacheKey = "$analysisType:$imageHash:$regionHash"
            
            val avgConfidence = if (detectedObjects.isNotEmpty()) {
                detectedObjects.map { it.confidence }.average().toFloat()
            } else 0f
            
            val cachedResult = CachedAnalysisResult(
                detectedObjects = detectedObjects,
                timestamp = System.currentTimeMillis(),
                imageHash = imageHash,
                regionHash = regionHash,
                confidence = avgConfidence
            )
            
            analysisCache.put(cacheKey, cachedResult)
            Log.d(TAG, "💾 Resultado cached: $analysisType (${detectedObjects.size} objetos)")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cachear resultado: ${e.message}", e)
        }
    }
    
    /**
     * 🖼️ Busca bitmap processado no cache
     */
    fun getProcessedBitmap(originalBitmap: Bitmap, processType: String): Bitmap? {
        try {
            val imageHash = generateImageHash(originalBitmap)
            val cacheKey = "$processType:$imageHash"
            
            val cachedBitmap = bitmapCache.get(cacheKey)
            
            if (cachedBitmap != null && !cachedBitmap.isRecycled) {
                Log.d(TAG, "✅ Bitmap cache hit: $processType")
                return cachedBitmap
            } else if (cachedBitmap?.isRecycled == true) {
                bitmapCache.remove(cacheKey)
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar bitmap cache: ${e.message}", e)
        }
        
        return null
    }
    
    /**
     * 💾 Armazena bitmap processado no cache
     */
    fun cacheProcessedBitmap(originalBitmap: Bitmap, processType: String, processedBitmap: Bitmap) {
        try {
            val imageHash = generateImageHash(originalBitmap)
            val cacheKey = "$processType:$imageHash"
            
            // Verifica se há espaço suficiente
            val bitmapSize = processedBitmap.byteCount
            val maxSize = MAX_CACHE_SIZE_MB * 1024 * 1024
            
            if (bitmapSize < maxSize / 10) { // Não cache bitmaps muito grandes
                bitmapCache.put(cacheKey, processedBitmap)
                Log.d(TAG, "💾 Bitmap cached: $processType (${bitmapSize / 1024}KB)")
            } else {
                Log.w(TAG, "⚠️ Bitmap muito grande para cache: ${bitmapSize / 1024}KB")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cachear bitmap: ${e.message}", e)
        }
    }
    
    /**
     * 📊 Busca metadados de imagem no cache
     */
    fun getImageMetadata(bitmap: Bitmap): ImageMetadata? {
        try {
            val imageHash = generateImageHash(bitmap)
            val cachedMetadata = metadataCache.get(imageHash)
            
            if (cachedMetadata != null) {
                Log.d(TAG, "✅ Metadata cache hit")
                return cachedMetadata
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar metadata cache: ${e.message}", e)
        }
        
        return null
    }
    
    /**
     * 💾 Armazena metadados de imagem no cache
     */
    fun cacheImageMetadata(bitmap: Bitmap, metadata: ImageMetadata) {
        try {
            val imageHash = generateImageHash(bitmap)
            metadataCache.put(imageHash, metadata)
            Log.d(TAG, "💾 Metadata cached")
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao cachear metadata: ${e.message}", e)
        }
    }
    
    /**
     * 📊 Calcula e cacheia metadados de imagem
     */
    suspend fun calculateAndCacheMetadata(bitmap: Bitmap): ImageMetadata = withContext(Dispatchers.Default) {
        // Verifica cache primeiro
        getImageMetadata(bitmap)?.let { return@withContext it }
        
        // Calcula metadados
        val metadata = calculateImageMetadata(bitmap)
        
        // Armazena no cache
        cacheImageMetadata(bitmap, metadata)
        
        return@withContext metadata
    }
    
    private fun calculateImageMetadata(bitmap: Bitmap): ImageMetadata {
        var totalR = 0f
        var totalG = 0f
        var totalB = 0f
        var pixelCount = 0
        val colorMap = mutableMapOf<Int, Int>()
        var complexity = 0f
        
        // Amostra pixels para calcular estatísticas
        val sampleRate = maxOf(1, bitmap.width * bitmap.height / 10000) // Máximo 10k pixels
        
        for (x in 0 until bitmap.width step sampleRate) {
            for (y in 0 until bitmap.height step sampleRate) {
                val pixel = bitmap.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                
                totalR += r
                totalG += g
                totalB += b
                pixelCount++
                
                // Conta cores dominantes
                val quantizedColor = quantizeColor(pixel)
                colorMap[quantizedColor] = (colorMap[quantizedColor] ?: 0) + 1
                
                // Calcula complexidade baseada na variação local
                if (x > 0 && y > 0) {
                    val prevPixel = bitmap.getPixel(x - sampleRate, y - sampleRate)
                    val diff = calculateColorDifference(pixel, prevPixel)
                    complexity += diff
                }
            }
        }
        
        val averageColor = if (pixelCount > 0) {
            Triple(totalR / pixelCount, totalG / pixelCount, totalB / pixelCount)
        } else {
            Triple(0f, 0f, 0f)
        }
        
        val dominantColors = colorMap.entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
        
        val normalizedComplexity = if (pixelCount > 0) complexity / pixelCount / 255f else 0f
        
        return ImageMetadata(
            width = bitmap.width,
            height = bitmap.height,
            averageColor = averageColor,
            complexity = normalizedComplexity,
            dominantColors = dominantColors,
            timestamp = System.currentTimeMillis()
        )
    }
    
    private fun quantizeColor(color: Int): Int {
        // Quantiza cor para reduzir variações (agrupa cores similares)
        val r = ((color shr 16) and 0xFF) / 32 * 32
        val g = ((color shr 8) and 0xFF) / 32 * 32
        val b = (color and 0xFF) / 32 * 32
        return (r shl 16) or (g shl 8) or b
    }
    
    private fun calculateColorDifference(color1: Int, color2: Int): Float {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        return kotlin.math.sqrt(
            ((r1 - r2) * (r1 - r2) + (g1 - g2) * (g1 - g2) + (b1 - b2) * (b1 - b2)).toFloat()
        )
    }
    
    private fun generateImageHash(bitmap: Bitmap): String {
        try {
            // Cria hash baseado em características da imagem
            val features = "${bitmap.width}x${bitmap.height}"
            
            // Amostra alguns pixels para criar hash único
            val samplePixels = mutableListOf<Int>()
            val sampleRate = maxOf(1, bitmap.width / 10)
            
            for (x in 0 until bitmap.width step sampleRate) {
                for (y in 0 until bitmap.height step sampleRate) {
                    samplePixels.add(bitmap.getPixel(x, y))
                    if (samplePixels.size >= 100) break // Limita amostras
                }
                if (samplePixels.size >= 100) break
            }
            
            val pixelHash = samplePixels.joinToString("")
            val fullString = "$features:$pixelHash"
            
            return generateMD5Hash(fullString)
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar hash da imagem: ${e.message}", e)
            return "error_${System.currentTimeMillis()}"
        }
    }
    
    private fun generateRegionHash(region: RectF): String {
        val regionString = "${region.left.roundToInt()},${region.top.roundToInt()}," +
                          "${region.right.roundToInt()},${region.bottom.roundToInt()}"
        return generateMD5Hash(regionString)
    }
    
    private fun generateMD5Hash(input: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(input.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao gerar MD5: ${e.message}", e)
            "fallback_${input.hashCode()}"
        }
    }
    
    private fun startPeriodicCleanup() {
        cleanupJob = cacheScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000) // Limpeza a cada 5 minutos
                
                try {
                    cleanupExpiredEntries()
                } catch (e: Exception) {
                    Log.e(TAG, "Erro na limpeza automática: ${e.message}", e)
                }
            }
        }
    }
    
    private fun cleanupExpiredEntries() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = mutableListOf<String>()
        
        // Limpa análises expiradas (mais de 1 hora)
        val analysisSnapshot = analysisCache.snapshot()
        for ((key, result) in analysisSnapshot) {
            if (currentTime - result.timestamp > 60 * 60 * 1000) { // 1 hora
                expiredKeys.add(key)
            }
        }
        
        expiredKeys.forEach { analysisCache.remove(it) }
        
        // Limpa metadados antigos (mais de 2 horas)
        val metadataSnapshot = metadataCache.snapshot()
        val expiredMetadataKeys = mutableListOf<String>()
        
        for ((key, metadata) in metadataSnapshot) {
            if (currentTime - metadata.timestamp > 2 * 60 * 60 * 1000) { // 2 horas
                expiredMetadataKeys.add(key)
            }
        }
        
        expiredMetadataKeys.forEach { metadataCache.remove(it) }
        
        if (expiredKeys.isNotEmpty() || expiredMetadataKeys.isNotEmpty()) {
            Log.d(TAG, "🧹 Limpeza automática: ${expiredKeys.size} análises + ${expiredMetadataKeys.size} metadados removidos")
        }
    }
    
    /**
     * 📊 Estatísticas do cache
     */
    fun getCacheStats(): String {
        return """
            📊 AI Cache Stats:
            - Análises: ${analysisCache.size()}/${ANALYSIS_CACHE_SIZE}
            - Bitmaps: ${bitmapCache.size()}/${BITMAP_CACHE_SIZE}
            - Metadados: ${metadataCache.size()}/200
            - Uso de memória: ~${estimateMemoryUsage()}MB
        """.trimIndent()
    }
    
    private fun estimateMemoryUsage(): Int {
        var totalBytes = 0
        
        // Estima uso dos bitmaps
        val bitmapSnapshot = bitmapCache.snapshot()
        for (bitmap in bitmapSnapshot.values) {
            if (!bitmap.isRecycled) {
                totalBytes += bitmap.byteCount
            }
        }
        
        // Adiciona overhead dos outros caches (estimativa)
        totalBytes += analysisCache.size() * 1000 // ~1KB por análise
        totalBytes += metadataCache.size() * 200  // ~200 bytes por metadata
        
        return totalBytes / (1024 * 1024)
    }
    
    /**
     * 🧹 Limpa todo o cache
     */
    fun clearAll() {
        analysisCache.evictAll()
        bitmapCache.evictAll()
        metadataCache.evictAll()
        Log.d(TAG, "🧹 Cache completamente limpo")
    }
    
    /**
     * 🔚 Cleanup e liberação de recursos
     */
    fun cleanup() {
        cleanupJob?.cancel()
        clearAll()
        cacheScope.cancel()
        Log.d(TAG, "🔚 AI Cache finalizado")
    }
}
