package com.example.floatingbutton.ai.opencv

import android.graphics.Bitmap
import android.graphics.RectF
import android.util.Log
import com.example.floatingbutton.ai.SmartSelectionEngine
import kotlin.math.*

/**
 * 👁️ Native Shape Detector (substituindo OpenCV)
 * 
 * Usa algoritmos nativos do Android para:
 * - Detecção de contornos e formas geométricas
 * - Análise de bordas usando diferenças de pixels
 * - Classificação de formas básicas
 * - Detecção de padrões visuais
 */
class OpenCVObjectDetector {
    
    companion object {
        private const val TAG = "NativeShapeDetector"
        
        init {
            Log.d(TAG, "🔧 Usando algoritmos nativos de detecção de formas")
        }
    }
    
    /**
     * 🔍 Detecta formas usando algoritmos nativos
     */
    fun detectShapes(bitmap: Bitmap, region: RectF): List<SmartSelectionEngine.DetectedObject> {
        Log.d(TAG, "🔍 Iniciando detecção com algoritmos nativos...")
        return detectAdvancedShapes(bitmap, region)
    }
    
    /**
     * 🔍 Detecção avançada usando algoritmos nativos
     */
    private fun detectAdvancedShapes(bitmap: Bitmap, region: RectF): List<SmartSelectionEngine.DetectedObject> {
        val detectedObjects = mutableListOf<SmartSelectionEngine.DetectedObject>()
        
        try {
            // Análise de características visuais
            val edgeStrength = calculateAdvancedEdgeStrength(bitmap, region)
            val colorVariation = calculateColorVariation(bitmap, region)
            val aspectRatio = region.width() / region.height()
            val area = region.width() * region.height()
            
            Log.d(TAG, "📊 Análise: bordas=$edgeStrength, variação=$colorVariation, aspecto=$aspectRatio")
            
            // Classificação baseada em características
            val objectType = classifyByFeatures(edgeStrength, colorVariation, aspectRatio, area)
            val confidence = calculateAdvancedConfidence(edgeStrength, colorVariation, aspectRatio)
            
            if (confidence > 0.4f) {
                detectedObjects.add(
                    SmartSelectionEngine.DetectedObject(
                        bounds = region,
                        type = objectType,
                        confidence = confidence,
                        label = "forma_${objectType.name.lowercase()}"
                    )
                )
            }
            
            // Detecção de padrões geométricos específicos
            val geometricObjects = detectNativeGeometricPatterns(bitmap, region)
            detectedObjects.addAll(geometricObjects)
            
            Log.d(TAG, "✅ Detecção nativa encontrou ${detectedObjects.size} objetos")
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na detecção nativa: ${e.message}", e)
        }
        
        return detectedObjects
    }
    
    private fun calculateAdvancedEdgeStrength(bitmap: Bitmap, region: RectF): Float {
        var edgeStrength = 0f
        var pixelCount = 0
        
        val left = region.left.toInt().coerceAtLeast(1)
        val top = region.top.toInt().coerceAtLeast(1)
        val right = (region.right.toInt() - 1).coerceAtMost(bitmap.width - 1)
        val bottom = (region.bottom.toInt() - 1).coerceAtMost(bitmap.height - 1)
        
        // Algoritmo Sobel simplificado para detecção de bordas
        for (x in left until right step 2) {
            for (y in top until bottom step 2) {
                // Gradiente horizontal (Gx)
                val leftPixel = bitmap.getPixel(x - 1, y)
                val rightPixel = bitmap.getPixel(x + 1, y)
                val gx = getBrightness(rightPixel) - getBrightness(leftPixel)
                
                // Gradiente vertical (Gy)
                val topPixel = bitmap.getPixel(x, y - 1)
                val bottomPixel = bitmap.getPixel(x, y + 1)
                val gy = getBrightness(bottomPixel) - getBrightness(topPixel)
                
                // Magnitude do gradiente
                val magnitude = sqrt(gx * gx + gy * gy)
                edgeStrength += magnitude
                pixelCount++
            }
        }
        
        return if (pixelCount > 0) edgeStrength / pixelCount / 255f else 0f
    }
    
    private fun calculateColorVariation(bitmap: Bitmap, region: RectF): Float {
        val colors = mutableListOf<Triple<Int, Int, Int>>()
        
        val left = region.left.toInt().coerceAtLeast(0)
        val top = region.top.toInt().coerceAtLeast(0)
        val right = region.right.toInt().coerceAtMost(bitmap.width)
        val bottom = region.bottom.toInt().coerceAtMost(bitmap.height)
        
        // Amostra cores da região
        for (x in left until right step 8) {
            for (y in top until bottom step 8) {
                val pixel = bitmap.getPixel(x, y)
                colors.add(
                    Triple(
                        (pixel shr 16) and 0xFF,
                        (pixel shr 8) and 0xFF,
                        pixel and 0xFF
                    )
                )
            }
        }
        
        if (colors.size < 2) return 0f
        
        // Calcula variação de cores
        val avgR = colors.map { it.first }.average()
        val avgG = colors.map { it.second }.average()
        val avgB = colors.map { it.third }.average()
        
        val totalVariation = colors.map { (r, g, b) ->
            sqrt((r - avgR).pow(2.0) + (g - avgG).pow(2.0) + (b - avgB).pow(2.0))
        }.average()
        
        return (totalVariation / 255.0).coerceIn(0.0, 1.0).toFloat()
    }
    
    private fun classifyByFeatures(
        edgeStrength: Float,
        colorVariation: Float,
        aspectRatio: Float,
        area: Float
    ): SmartSelectionEngine.ObjectType {
        
        return when {
            // Área com bordas definidas e proporção quadrada -> botão
            edgeStrength > 0.3f && aspectRatio in 0.7f..1.4f && area in 2000f..50000f ->
                SmartSelectionEngine.ObjectType.BUTTON
            
            // Área retangular com bordas -> imagem
            edgeStrength > 0.2f && (aspectRatio > 1.5f || aspectRatio < 0.7f) && area > 10000f ->
                SmartSelectionEngine.ObjectType.IMAGE
            
            // Área pequena com bordas circulares -> ícone
            edgeStrength > 0.25f && aspectRatio in 0.8f..1.2f && area < 10000f ->
                SmartSelectionEngine.ObjectType.ICON
            
            // Área horizontal com variação moderada -> texto
            aspectRatio > 2f && colorVariation in 0.2f..0.6f ->
                SmartSelectionEngine.ObjectType.TEXT
            
            // Alta variação de cor -> imagem complexa
            colorVariation > 0.7f ->
                SmartSelectionEngine.ObjectType.IMAGE
            
            // Forma geométrica genérica
            edgeStrength > 0.15f ->
                SmartSelectionEngine.ObjectType.SHAPE
            
            else -> SmartSelectionEngine.ObjectType.UNKNOWN
        }
    }
    
    private fun calculateAdvancedConfidence(
        edgeStrength: Float,
        colorVariation: Float,
        aspectRatio: Float
    ): Float {
        
        // Confiança baseada na força das características
        var confidence = 0f
        
        // Bordas bem definidas aumentam confiança
        confidence += edgeStrength * 0.4f
        
        // Variação de cor moderada é boa
        val colorScore = if (colorVariation in 0.2f..0.8f) colorVariation else 0.3f
        confidence += colorScore * 0.3f
        
        // Proporções reconhecíveis aumentam confiança
        val aspectScore = when {
            aspectRatio in 0.8f..1.2f -> 0.8f  // Quadrado
            aspectRatio in 1.3f..2.0f -> 0.7f  // Retângulo
            aspectRatio > 2f -> 0.6f           // Horizontal (texto)
            else -> 0.4f                       // Outras proporções
        }
        confidence += aspectScore * 0.3f
        
        return confidence.coerceIn(0f, 1f)
    }
    
    private fun detectNativeGeometricPatterns(
        bitmap: Bitmap,
        region: RectF
    ): List<SmartSelectionEngine.DetectedObject> {
        
        val objects = mutableListOf<SmartSelectionEngine.DetectedObject>()
        
        try {
            // Detecta padrões circulares
            val circularityScore = calculateCircularity(bitmap, region)
            if (circularityScore > 0.7f) {
                objects.add(
                    SmartSelectionEngine.DetectedObject(
                        bounds = region,
                        type = SmartSelectionEngine.ObjectType.ICON,
                        confidence = circularityScore,
                        label = "círculo_detectado"
                    )
                )
            }
            
            // Detecta padrões retangulares
            val rectangularityScore = calculateRectangularity(bitmap, region)
            if (rectangularityScore > 0.8f) {
                objects.add(
                    SmartSelectionEngine.DetectedObject(
                        bounds = region,
                        type = SmartSelectionEngine.ObjectType.BUTTON,
                        confidence = rectangularityScore,
                        label = "retângulo_detectado"
                    )
                )
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Erro na detecção geométrica: ${e.message}", e)
        }
        
        return objects
    }
    
    private fun calculateCircularity(bitmap: Bitmap, region: RectF): Float {
        // Verifica se a região tem características circulares
        val aspectRatio = region.width() / region.height()
        
        // Círculos têm proporção próxima a 1:1
        if (aspectRatio !in 0.8f..1.2f) return 0f
        
        // Analisa distribuição de bordas ao redor do centro
        val centerX = region.centerX()
        val centerY = region.centerY()
        val radius = minOf(region.width(), region.height()) / 2f
        
        var edgeCount = 0
        var totalCount = 0
        
        // Verifica pontos em círculo
        for (angle in 0 until 360 step 15) {
            val radians = Math.toRadians(angle.toDouble())
            val x = (centerX + radius * cos(radians)).toInt()
            val y = (centerY + radius * sin(radians)).toInt()
            
            if (x in 0 until bitmap.width && y in 0 until bitmap.height) {
                val edgeStrength = getLocalEdgeStrength(bitmap, x, y)
                if (edgeStrength > 0.3f) edgeCount++
                totalCount++
            }
        }
        
        return if (totalCount > 0) edgeCount.toFloat() / totalCount else 0f
    }
    
    private fun calculateRectangularity(bitmap: Bitmap, region: RectF): Float {
        // Verifica se as bordas da região formam um retângulo
        var edgeScore = 0f
        
        // Verifica bordas horizontais (topo e base)
        edgeScore += checkHorizontalEdge(bitmap, region, true)  // Topo
        edgeScore += checkHorizontalEdge(bitmap, region, false) // Base
        
        // Verifica bordas verticais (esquerda e direita)
        edgeScore += checkVerticalEdge(bitmap, region, true)    // Esquerda
        edgeScore += checkVerticalEdge(bitmap, region, false)   // Direita
        
        return edgeScore / 4f
    }
    
    private fun checkHorizontalEdge(bitmap: Bitmap, region: RectF, isTop: Boolean): Float {
        val y = if (isTop) region.top.toInt() else region.bottom.toInt() - 1
        if (y !in 0 until bitmap.height) return 0f
        
        var edgeStrength = 0f
        var count = 0
        
        for (x in region.left.toInt() until region.right.toInt() step 3) {
            if (x in 0 until bitmap.width) {
                edgeStrength += getLocalEdgeStrength(bitmap, x, y)
                count++
            }
        }
        
        return if (count > 0) edgeStrength / count else 0f
    }
    
    private fun checkVerticalEdge(bitmap: Bitmap, region: RectF, isLeft: Boolean): Float {
        val x = if (isLeft) region.left.toInt() else region.right.toInt() - 1
        if (x !in 0 until bitmap.width) return 0f
        
        var edgeStrength = 0f
        var count = 0
        
        for (y in region.top.toInt() until region.bottom.toInt() step 3) {
            if (y in 0 until bitmap.height) {
                edgeStrength += getLocalEdgeStrength(bitmap, x, y)
                count++
            }
        }
        
        return if (count > 0) edgeStrength / count else 0f
    }
    
    private fun getLocalEdgeStrength(bitmap: Bitmap, x: Int, y: Int): Float {
        if (x <= 0 || x >= bitmap.width - 1 || y <= 0 || y >= bitmap.height - 1) return 0f
        
        val center = bitmap.getPixel(x, y)
        val left = bitmap.getPixel(x - 1, y)
        val right = bitmap.getPixel(x + 1, y)
        val top = bitmap.getPixel(x, y - 1)
        val bottom = bitmap.getPixel(x, y + 1)
        
        val centerBrightness = getBrightness(center)
        val maxDiff = maxOf(
            abs(centerBrightness - getBrightness(left)),
            abs(centerBrightness - getBrightness(right)),
            abs(centerBrightness - getBrightness(top)),
            abs(centerBrightness - getBrightness(bottom))
        )
        
        return maxDiff / 255f
    }
    
    private fun getBrightness(pixel: Int): Float {
        val r = (pixel shr 16) and 0xFF
        val g = (pixel shr 8) and 0xFF
        val b = pixel and 0xFF
        return (r + g + b) / 3f
    }
    
    fun cleanup() {
        Log.d(TAG, "🧹 Native shape detector cleanup")
    }
}