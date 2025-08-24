package com.example.floatingbutton.ai

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * 🤖 AI Detection Service - Detecta se imagens foram geradas por IA
 * 
 * Usa a API do SightEngine para análise avançada:
 * - Detecção de conteúdo gerado por IA
 * - Score de confiança de 0 a 1
 * - Análise rápida e precisa
 * - Interface moderna e intuitiva
 */
class AIDetectionService(private val context: Context) {

    companion object {
        private const val TAG = "AIDetectionService"
        private const val BASE_URL = "https://api.sightengine.com/1.0/check.json"
        private const val API_USER = "752671589"
        private const val API_SECRET = "Pjy6PESUoUQNYSEijwcZRMsvbHn5oSQG"
        private const val TIMEOUT_SECONDS = 20L // ⚡ Reduzido para melhor UX
        private const val MAX_IMAGE_SIZE = 1024 // 📏 Máximo 1024px para otimização
        private const val JPEG_QUALITY = 80 // 🎨 Qualidade otimizada
    }

    // 📊 Resultado da detecção
    data class AIDetectionResult(
        val isAIGenerated: Boolean,
        val confidence: Float, // 0.0 a 1.0
        val confidencePercentage: Int, // 0 a 100
        val analysisDetails: String,
        val recommendationText: String,
        val emoji: String
    )

    // 🌐 Cliente HTTP
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()

    // 🎯 Handler para executar callbacks na Main Thread
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * 🔍 Analisa se uma imagem foi gerada por IA
     */
    suspend fun detectAIGenerated(
        bitmap: Bitmap,
        onProgress: (String) -> Unit = {}
    ): Result<AIDetectionResult> = withContext(Dispatchers.IO) {
        
        // Helper para executar callbacks na Main Thread
        fun safeProgress(message: String) {
            mainHandler.post {
                onProgress(message)
            }
        }
        
        try {
            Log.d(TAG, "🤖 Iniciando detecção de IA...")
            safeProgress("🤖 Preparando imagem para análise...")

            // Converte bitmap para bytes
            val imageBytes = bitmapToByteArray(bitmap)
            Log.d(TAG, "📷 Imagem convertida: ${imageBytes.size} bytes")

            safeProgress("🌐 Enviando para análise...")

            // Cria request multipart
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("media", "image.jpg", 
                    imageBytes.toRequestBody("image/jpeg".toMediaType()))
                .addFormDataPart("models", "genai")
                .addFormDataPart("api_user", API_USER)
                .addFormDataPart("api_secret", API_SECRET)
                .build()

            val request = Request.Builder()
                .url(BASE_URL)
                .post(requestBody)
                .build()

            Log.d(TAG, "🌐 Enviando request para SightEngine...")

            // Executa request
            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorMsg = "Erro HTTP: ${response.code} - ${response.message}"
                Log.e(TAG, errorMsg)
                return@withContext Result.failure(Exception(errorMsg))
            }

            val responseBody = response.body?.string()
            if (responseBody == null) {
                Log.e(TAG, "Response body é null")
                return@withContext Result.failure(Exception("Resposta vazia da API"))
            }

            Log.d(TAG, "📊 Response recebido: $responseBody")
            safeProgress("📊 Analisando resultados...")

            // Processa resposta JSON
            val result = parseAIDetectionResponse(responseBody)
            Log.d(TAG, "✅ Análise concluída: ${result.confidencePercentage}% IA")

            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro na detecção de IA: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 📊 Processa a resposta JSON da API
     */
    private fun parseAIDetectionResponse(jsonResponse: String): AIDetectionResult {
        try {
            val json = JSONObject(jsonResponse)
            
            // Verifica se houve erro
            if (json.getString("status") != "success") {
                throw Exception("API retornou erro: ${json.optString("error", "Erro desconhecido")}")
            }

            // Extrai score de IA
            val typeObject = json.getJSONObject("type")
            val aiScore = typeObject.getDouble("ai_generated").toFloat()
            
            Log.d(TAG, "📊 Score de IA: $aiScore")

            // Calcula métricas
            val confidencePercentage = (aiScore * 100).toInt()
            val isAIGenerated = aiScore >= 0.5f // Threshold de 50%

            // Gera análise detalhada
            val analysisDetails = generateAnalysisDetails(aiScore)
            val recommendationText = generateRecommendationText(aiScore, isAIGenerated)
            val emoji = getAIEmoji(aiScore)

            return AIDetectionResult(
                isAIGenerated = isAIGenerated,
                confidence = aiScore,
                confidencePercentage = confidencePercentage,
                analysisDetails = analysisDetails,
                recommendationText = recommendationText,
                emoji = emoji
            )

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao processar JSON: ${e.message}", e)
            throw Exception("Erro ao processar resposta da API: ${e.message}")
        }
    }

    /**
     * 📝 Gera análise detalhada baseada no score
     */
    private fun generateAnalysisDetails(score: Float): String {
        return when {
            score >= 0.9f -> "Altamente provável que seja gerada por IA. Características típicas de modelos generativos detectadas."
            score >= 0.7f -> "Provável que seja gerada por IA. Padrões artificiais identificados na imagem."
            score >= 0.5f -> "Possível que seja gerada por IA. Alguns indicadores artificiais presentes."
            score >= 0.3f -> "Provavelmente real. Poucos indicadores de geração artificial."
            score >= 0.1f -> "Muito provável que seja real. Características naturais predominantes."
            else -> "Altamente provável que seja real. Nenhum indicador significativo de IA detectado."
        }
    }

    /**
     * 💡 Gera texto de recomendação
     */
    private fun generateRecommendationText(score: Float, isAI: Boolean): String {
        return if (isAI) {
            when {
                score >= 0.8f -> "⚠️ Recomendamos verificar a fonte desta imagem antes de usar ou compartilhar."
                score >= 0.6f -> "💡 Considere investigar mais sobre a origem desta imagem."
                else -> "🔍 Mantenha cautela - pode ser conteúdo sintético."
            }
        } else {
            when {
                score <= 0.2f -> "✅ Imagem parece ser autêntica e segura para uso."
                score <= 0.4f -> "👍 Provavelmente real, mas sempre bom verificar a fonte."
                else -> "🤔 Resultado inconclusivo - análise manual recomendada."
            }
        }
    }

    /**
     * 🎨 Retorna emoji baseado no score
     */
    private fun getAIEmoji(score: Float): String {
        return when {
            score >= 0.8f -> "🤖" // Definitivamente IA
            score >= 0.6f -> "⚠️"  // Provável IA
            score >= 0.4f -> "🤔"  // Incerto
            score >= 0.2f -> "👍"  // Provável real
            else -> "✅"           // Definitivamente real
        }
    }

    /**
     * 🖼️ Converte Bitmap para ByteArray com otimização automática
     */
    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val outputStream = ByteArrayOutputStream()
        
        // ⚡ Redimensiona se muito grande
        val optimizedBitmap = if (bitmap.width > MAX_IMAGE_SIZE || bitmap.height > MAX_IMAGE_SIZE) {
            val scale = MAX_IMAGE_SIZE.toFloat() / maxOf(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            
            Log.d(TAG, "📏 Redimensionando: ${bitmap.width}x${bitmap.height} → ${newWidth}x${newHeight}")
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        
        // 🎨 Comprime com qualidade otimizada
        optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        val byteArray = outputStream.toByteArray()
        
        // 🧹 Libera bitmap temporário se foi criado
        if (optimizedBitmap != bitmap) {
            optimizedBitmap.recycle()
        }
        
        Log.d(TAG, "🖼️ Bitmap otimizado: ${byteArray.size} bytes")
        return byteArray
    }

    /**
     * 📊 Verifica se a API está disponível
     */
    suspend fun checkAPIStatus(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$BASE_URL?models=genai&api_user=$API_USER&api_secret=$API_SECRET&url=https://via.placeholder.com/100")
                .get()
                .build()

            val response = httpClient.newCall(request).execute()
            val isAvailable = response.isSuccessful
            
            Log.d(TAG, "🌐 Status da API: ${if (isAvailable) "Disponível" else "Indisponível"}")
            
            isAvailable
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro ao verificar API: ${e.message}", e)
            false
        }
    }

    /**
     * 🧹 Cleanup
     */
    fun cleanup() {
        try {
            httpClient.dispatcher.executorService.shutdown()
            httpClient.connectionPool.evictAll()
            Log.d(TAG, "🧹 AI Detection Service cleanup concluído")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no cleanup: ${e.message}", e)
        }
    }
}
