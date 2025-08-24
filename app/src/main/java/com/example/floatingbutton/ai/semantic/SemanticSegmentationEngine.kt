package com.example.floatingbutton.ai.semantic

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.RectF
import android.util.Log
import com.example.floatingbutton.ai.SmartSelectionEngine
import kotlinx.coroutines.*
import kotlin.math.*

/**
 * 🧩 Semantic Segmentation Engine
 * 
 * Realiza segmentação semântica avançada para:
 * - Identificar regiões homogêneas
 * - Detectar bordas semânticas
 * - Agrupar pixels por similaridade
 * - Análise de contexto espacial
 */
class SemanticSegmentationEngine {
    
    companion object {
        private const val TAG = "SemanticSegmentation"
        private const val SIMILARITY_THRESHOLD = 30f
        private const val MIN_SEGMENT_SIZE = 100
        private const val MAX_SEGMENTS = 20
    }
    
    data class Segment(
        val id: Int,
        val bounds: RectF,
        val pixels: List<PointF>,
        val averageColor: Triple<Float, Float, Float>,
        val type: SegmentType,
        val confidence: Float
    )
    
    enum class SegmentType {
        BACKGROUND,     // Fundo
        TEXT_REGION,    // Região de texto
        IMAGE_REGION,   // Região de imagem
        UI_ELEMENT,     // Elemento de interface
        BORDER,         // Borda/divisor
        UNKNOWN         // Desconhecido
    }
    
    /**
     * 🧩 Segmenta a imagem em regiões semânticas
     */
    suspend fun segmentImage(
        bitmap: Bitmap,
        userDrawnRegion: RectF,
        onProgress: (String) -> Unit = {}
    ): List<SmartSelectionEngine.DetectedObject> = withContext(Dispatchers.Default) {
        
        Log.d(TAG, "🧩 Iniciando segmentação semântica...")
        val detectedObjects = mutableListOf<SmartSelectionEngine.DetectedObject>()
        
        try {
            onProgress("Analisando regiões...")
            
            // Extrai região de interesse
            val regionBitmap = extractRegion(bitmap, userDrawnRegion)
            if (regionBitmap == null) {
                Log.w(TAG, "Falha ao extrair região")
                return@withContext emptyList()
            }
            
            // ETAPA 1: Segmentação por cor
            onProgress("Segmentando por cores...")
            val colorSegments = performColorSegmentation(regionBitmap)
            Log.d(TAG, "🎨 Encontrados ${colorSegments.size} segmentos de cor")
            
            // ETAPA 2: Análise de textura
            onProgress("Analisando texturas...")
            val textureSegments = analyzeTextures(regionBitmap, colorSegments)
            Log.d(TAG, "🔍 Analisados ${textureSegments.size} segmentos de textura")
            
            // ETAPA 3: Detecção de bordas semânticas
            onProgress("Detectando bordas...")
            val edgeSegments = detectSemanticEdges(regionBitmap)
            Log.d(TAG, "📐 Encontradas ${edgeSegments.size} bordas semânticas")
            
            // ETAPA 4: Fusão e classificação
            onProgress("Classificando regiões...")
            val allSegments = mergeSegments(colorSegments + textureSegments + edgeSegments)
            val classifiedSegments = classifySegments(allSegments, regionBitmap)
            
            // Converte segmentos para objetos detectados
            for (segment in classifiedSegments) {
                val absoluteBounds = convertToAbsoluteBounds(segment.bounds, userDrawnRegion)
                val objectType = mapSegmentTypeToObjectType(segment.type)
                
                detectedObjects.add(
                    SmartSelectionEngine.DetectedObject(
                        bounds = absoluteBounds,
                        type = objectType,
                        confidence = segment.confidence,
                        label = "segmento_${segment.type.name.lowercase()}"
                    )
                )
            }
            
            Log.d(TAG, "✅ Segmentação concluída: ${detectedObjects.size} objetos")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na segmentação: ${e.message}", e)
        }
        
        return@withContext detectedObjects
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
    
    private fun performColorSegmentation(bitmap: Bitmap): List<Segment> {
        val segments = mutableListOf<Segment>()
        val visited = Array(bitmap.width) { BooleanArray(bitmap.height) }
        var segmentId = 0
        
        try {
            // Algoritmo de flood fill para segmentação por cor
            for (x in 0 until bitmap.width step 5) {
                for (y in 0 until bitmap.height step 5) {
                    if (!visited[x][y]) {
                        val segment = floodFillSegmentation(bitmap, x, y, visited, segmentId++)
                        
                        if (segment != null && segment.pixels.size >= MIN_SEGMENT_SIZE) {
                            segments.add(segment)
                        }
                        
                        if (segments.size >= MAX_SEGMENTS) break
                    }
                }
                if (segments.size >= MAX_SEGMENTS) break
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na segmentação por cor: ${e.message}", e)
        }
        
        return segments
    }
    
    private fun floodFillSegmentation(
        bitmap: Bitmap,
        startX: Int,
        startY: Int,
        visited: Array<BooleanArray>,
        segmentId: Int
    ): Segment? {
        
        if (startX < 0 || startX >= bitmap.width || startY < 0 || startY >= bitmap.height) {
            return null
        }
        
        val startColor = bitmap.getPixel(startX, startY)
        val pixels = mutableListOf<PointF>()
        val queue = mutableListOf<Pair<Int, Int>>()
        
        queue.add(Pair(startX, startY))
        
        var minX = startX
        var maxX = startX
        var minY = startY
        var maxY = startY
        
        var totalR = 0f
        var totalG = 0f
        var totalB = 0f
        
        while (queue.isNotEmpty() && pixels.size < 10000) { // Limita tamanho do segmento
            val (x, y) = queue.removeAt(0)
            
            if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height || visited[x][y]) {
                continue
            }
            
            val currentColor = bitmap.getPixel(x, y)
            if (!colorsAreSimilar(startColor, currentColor, SIMILARITY_THRESHOLD)) {
                continue
            }
            
            visited[x][y] = true
            pixels.add(PointF(x.toFloat(), y.toFloat()))
            
            // Atualiza bounds
            minX = minOf(minX, x)
            maxX = maxOf(maxX, x)
            minY = minOf(minY, y)
            maxY = maxOf(maxY, y)
            
            // Acumula cor
            totalR += (currentColor shr 16) and 0xFF
            totalG += (currentColor shr 8) and 0xFF
            totalB += currentColor and 0xFF
            
            // Adiciona vizinhos
            queue.add(Pair(x + 1, y))
            queue.add(Pair(x - 1, y))
            queue.add(Pair(x, y + 1))
            queue.add(Pair(x, y - 1))
        }
        
        if (pixels.isEmpty()) return null
        
        val avgColor = Triple(
            totalR / pixels.size,
            totalG / pixels.size,
            totalB / pixels.size
        )
        
        return Segment(
            id = segmentId,
            bounds = RectF(minX.toFloat(), minY.toFloat(), maxX.toFloat(), maxY.toFloat()),
            pixels = pixels,
            averageColor = avgColor,
            type = SegmentType.UNKNOWN,
            confidence = calculateSegmentConfidence(pixels, avgColor)
        )
    }
    
    private fun colorsAreSimilar(color1: Int, color2: Int, threshold: Float): Boolean {
        val r1 = (color1 shr 16) and 0xFF
        val g1 = (color1 shr 8) and 0xFF
        val b1 = color1 and 0xFF
        
        val r2 = (color2 shr 16) and 0xFF
        val g2 = (color2 shr 8) and 0xFF
        val b2 = color2 and 0xFF
        
        val distance = sqrt(
            (r1 - r2).toFloat().pow(2) +
            (g1 - g2).toFloat().pow(2) +
            (b1 - b2).toFloat().pow(2)
        )
        
        return distance <= threshold
    }
    
    private fun analyzeTextures(bitmap: Bitmap, segments: List<Segment>): List<Segment> {
        return segments.map { segment ->
            val textureType = analyzeSegmentTexture(bitmap, segment)
            segment.copy(type = textureType)
        }
    }
    
    private fun analyzeSegmentTexture(bitmap: Bitmap, segment: Segment): SegmentType {
        // Análise de textura baseada em padrões locais
        val bounds = segment.bounds
        val sampleSize = 20
        val textures = mutableListOf<Float>()
        
        try {
            for (x in bounds.left.toInt() until bounds.right.toInt() step sampleSize) {
                for (y in bounds.top.toInt() until bounds.bottom.toInt() step sampleSize) {
                    if (x + sampleSize < bitmap.width && y + sampleSize < bitmap.height) {
                        val localVariance = calculateLocalVariance(bitmap, x, y, sampleSize)
                        textures.add(localVariance)
                    }
                }
            }
            
            if (textures.isEmpty()) return SegmentType.UNKNOWN
            
            val avgTexture = textures.average().toFloat()
            val textureVariance = textures.map { (it - avgTexture).pow(2) }.average().toFloat()
            
            // Classificação baseada na textura
            return when {
                avgTexture < 10f && textureVariance < 5f -> SegmentType.BACKGROUND  // Área lisa
                avgTexture > 50f && textureVariance > 20f -> SegmentType.IMAGE_REGION  // Área com muita textura
                avgTexture in 15f..40f -> SegmentType.TEXT_REGION  // Textura média (texto)
                else -> SegmentType.UI_ELEMENT  // Elementos de interface
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na análise de textura: ${e.message}", e)
            return SegmentType.UNKNOWN
        }
    }
    
    private fun calculateLocalVariance(bitmap: Bitmap, startX: Int, startY: Int, size: Int): Float {
        var totalBrightness = 0f
        var pixelCount = 0
        
        // Primeira passagem: calcula média
        for (x in startX until minOf(startX + size, bitmap.width)) {
            for (y in startY until minOf(startY + size, bitmap.height)) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = ((pixel shr 16) and 0xFF) + ((pixel shr 8) and 0xFF) + (pixel and 0xFF)
                totalBrightness += brightness / 3f
                pixelCount++
            }
        }
        
        if (pixelCount == 0) return 0f
        val avgBrightness = totalBrightness / pixelCount
        
        // Segunda passagem: calcula variância
        var variance = 0f
        for (x in startX until minOf(startX + size, bitmap.width)) {
            for (y in startY until minOf(startY + size, bitmap.height)) {
                val pixel = bitmap.getPixel(x, y)
                val brightness = ((pixel shr 16) and 0xFF) + ((pixel shr 8) and 0xFF) + (pixel and 0xFF)
                variance += (brightness / 3f - avgBrightness).pow(2)
            }
        }
        
        return sqrt(variance / pixelCount)
    }
    
    private fun detectSemanticEdges(bitmap: Bitmap): List<Segment> {
        val edgeSegments = mutableListOf<Segment>()
        
        try {
            // Detecção de bordas usando operador Sobel simplificado
            val edges = mutableListOf<PointF>()
            
            for (x in 1 until bitmap.width - 1 step 3) {
                for (y in 1 until bitmap.height - 1 step 3) {
                    val edgeStrength = calculateEdgeStrength(bitmap, x, y)
                    
                    if (edgeStrength > 50f) { // Threshold para bordas
                        edges.add(PointF(x.toFloat(), y.toFloat()))
                    }
                }
            }
            
            // Agrupa bordas próximas em segmentos
            if (edges.isNotEmpty()) {
                val edgeBounds = calculateBounds(edges)
                
                edgeSegments.add(
                    Segment(
                        id = -1,
                        bounds = edgeBounds,
                        pixels = edges,
                        averageColor = Triple(128f, 128f, 128f), // Cor neutra para bordas
                        type = SegmentType.BORDER,
                        confidence = 0.8f
                    )
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na detecção de bordas: ${e.message}", e)
        }
        
        return edgeSegments
    }
    
    private fun calculateEdgeStrength(bitmap: Bitmap, x: Int, y: Int): Float {
        // Operador Sobel simplificado
        val gx = (
            -1 * getBrightness(bitmap, x - 1, y - 1) +
            1 * getBrightness(bitmap, x + 1, y - 1) +
            -2 * getBrightness(bitmap, x - 1, y) +
            2 * getBrightness(bitmap, x + 1, y) +
            -1 * getBrightness(bitmap, x - 1, y + 1) +
            1 * getBrightness(bitmap, x + 1, y + 1)
        )
        
        val gy = (
            -1 * getBrightness(bitmap, x - 1, y - 1) +
            -2 * getBrightness(bitmap, x, y - 1) +
            -1 * getBrightness(bitmap, x + 1, y - 1) +
            1 * getBrightness(bitmap, x - 1, y + 1) +
            2 * getBrightness(bitmap, x, y + 1) +
            1 * getBrightness(bitmap, x + 1, y + 1)
        )
        
        return sqrt(gx * gx + gy * gy)
    }
    
    private fun getBrightness(bitmap: Bitmap, x: Int, y: Int): Float {
        if (x < 0 || x >= bitmap.width || y < 0 || y >= bitmap.height) return 0f
        
        val pixel = bitmap.getPixel(x, y)
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        
        return (r + g + b) / 3f
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
    
    private fun mergeSegments(segments: List<Segment>): List<Segment> {
        // Remove segmentos sobrepostos ou muito pequenos
        val merged = mutableListOf<Segment>()
        
        for (segment in segments.sortedByDescending { it.confidence }) {
            val overlapping = merged.find { existing ->
                calculateOverlap(segment.bounds, existing.bounds) > 0.6f
            }
            
            if (overlapping == null && segment.pixels.size >= MIN_SEGMENT_SIZE) {
                merged.add(segment)
            }
        }
        
        return merged
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
    
    private fun classifySegments(segments: List<Segment>, bitmap: Bitmap): List<Segment> {
        return segments.map { segment ->
            val refinedType = refineSegmentClassification(segment, bitmap)
            val confidence = calculateFinalConfidence(segment, refinedType)
            
            segment.copy(type = refinedType, confidence = confidence)
        }
    }
    
    private fun refineSegmentClassification(segment: Segment, bitmap: Bitmap): SegmentType {
        val bounds = segment.bounds
        val aspectRatio = bounds.width() / bounds.height()
        val area = bounds.width() * bounds.height()
        val avgColor = segment.averageColor
        
        // Refinamento baseado em características geométricas e de cor
        return when {
            // Área muito grande e cor uniforme -> fundo
            area > bitmap.width * bitmap.height * 0.3f && 
            isColorUniform(avgColor) -> SegmentType.BACKGROUND
            
            // Proporção horizontal e área média -> texto
            aspectRatio > 3f && area in 1000f..20000f -> SegmentType.TEXT_REGION
            
            // Área quadrada/retangular com tamanho médio -> UI element
            aspectRatio in 0.5f..2f && area in 2000f..50000f -> SegmentType.UI_ELEMENT
            
            // Alta variação de cor -> imagem
            !isColorUniform(avgColor) && area > 5000f -> SegmentType.IMAGE_REGION
            
            // Mantém classificação original se não se encaixa em nenhuma categoria
            else -> segment.type
        }
    }
    
    private fun isColorUniform(color: Triple<Float, Float, Float>): Boolean {
        val (r, g, b) = color
        val avg = (r + g + b) / 3f
        val variance = ((r - avg).pow(2) + (g - avg).pow(2) + (b - avg).pow(2)) / 3f
        return sqrt(variance) < 20f  // Threshold para uniformidade
    }
    
    private fun calculateSegmentConfidence(pixels: List<PointF>, avgColor: Triple<Float, Float, Float>): Float {
        val pixelCount = pixels.size
        val colorUniformity = if (isColorUniform(avgColor)) 0.8f else 0.4f
        val sizeScore = (pixelCount / 1000f).coerceIn(0f, 1f)
        
        return (colorUniformity + sizeScore) / 2f
    }
    
    private fun calculateFinalConfidence(segment: Segment, refinedType: SegmentType): Float {
        var confidence = segment.confidence
        
        // Boost de confiança baseado no tipo final
        confidence *= when (refinedType) {
            SegmentType.TEXT_REGION -> 1.2f  // Texto é importante
            SegmentType.UI_ELEMENT -> 1.1f   // UI elements são relevantes
            SegmentType.IMAGE_REGION -> 1.0f // Imagens são neutras
            SegmentType.BORDER -> 0.8f       // Bordas são menos importantes
            SegmentType.BACKGROUND -> 0.5f   // Fundo é menos relevante
            SegmentType.UNKNOWN -> 0.6f      // Desconhecido tem baixa confiança
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private fun convertToAbsoluteBounds(relativeBounds: RectF, originalRegion: RectF): RectF {
        val scaleX = originalRegion.width() / relativeBounds.width().coerceAtLeast(1f)
        val scaleY = originalRegion.height() / relativeBounds.height().coerceAtLeast(1f)
        
        return RectF(
            originalRegion.left + relativeBounds.left * scaleX,
            originalRegion.top + relativeBounds.top * scaleY,
            originalRegion.left + relativeBounds.right * scaleX,
            originalRegion.top + relativeBounds.bottom * scaleY
        )
    }
    
    private fun mapSegmentTypeToObjectType(segmentType: SegmentType): SmartSelectionEngine.ObjectType {
        return when (segmentType) {
            SegmentType.TEXT_REGION -> SmartSelectionEngine.ObjectType.TEXT
            SegmentType.IMAGE_REGION -> SmartSelectionEngine.ObjectType.IMAGE
            SegmentType.UI_ELEMENT -> SmartSelectionEngine.ObjectType.BUTTON
            SegmentType.BORDER -> SmartSelectionEngine.ObjectType.SHAPE
            SegmentType.BACKGROUND -> SmartSelectionEngine.ObjectType.UNKNOWN
            SegmentType.UNKNOWN -> SmartSelectionEngine.ObjectType.UNKNOWN
        }
    }
}
