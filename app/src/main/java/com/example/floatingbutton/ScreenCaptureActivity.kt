package com.example.floatingbutton

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

class ScreenCaptureActivity : Activity() {
    
    companion object {
        private const val TAG = "ScreenCaptureActivity"
        private const val REQUEST_MEDIA_PROJECTION = 1001
    }
    
    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var screenWidth = 0
    private var screenHeight = 0
    private var screenDensity = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configura a Activity para ser transparente
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
            android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
            android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS or
            android.view.WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
        )
        
        requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        
        Log.d(TAG, "onCreate: Iniciando captura de tela...")
        
        try {
            // Obtém as dimensões da tela
            val displayMetrics = resources.displayMetrics
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
            
            Log.d(TAG, "onCreate: Dimensões da tela: ${screenWidth}x${screenHeight}, Densidade: $screenDensity")
            
            // Inicia captura imediatamente
            startScreenCapture()
            
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Erro: ${e.message}", e)
            // 🔇 Erro silencioso
            finish()
        }
    }
    
    private fun startScreenCapture() {
        try {
            Log.d(TAG, "startScreenCapture: Iniciando captura...")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                
                val intent = mediaProjectionManager?.createScreenCaptureIntent()
                if (intent != null) {
                    startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
                } else {
                    Log.e(TAG, "startScreenCapture: Intent de captura é null")
                    // 🔇 Erro silencioso
                    finish()
                }
            } else {
                Log.w(TAG, "startScreenCapture: MediaProjection não suportado nesta versão")
                // 🔇 Versão não suportada - silencioso
                finish()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "startScreenCapture: Erro: ${e.message}", e)
            // 🔇 Erro silencioso
            finish()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "onActivityResult: Permissão concedida, iniciando captura...")
                createMediaProjection(resultCode, data)
            } else {
                Log.w(TAG, "onActivityResult: Permissão negada ou cancelada")
                // 🔇 Permissão negada - silencioso
                finish()
            }
        }
    }
    
    private fun createMediaProjection(resultCode: Int, data: Intent) {
        try {
            Log.d(TAG, "createMediaProjection: Criando MediaProjection...")
            
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            if (mediaProjection == null) {
                Log.e(TAG, "createMediaProjection: MediaProjection é null")
                // 🔇 Erro silencioso
                finish()
                return
            }
            
            // Registra o callback obrigatório
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection callback: onStop chamado")
                    super.onStop()
                }
            }, null)
            
            Log.d(TAG, "createMediaProjection: Callback registrado, iniciando captura...")
            
            // Inicia a captura da tela
            captureScreen()
            
        } catch (e: Exception) {
            Log.e(TAG, "createMediaProjection: Erro: ${e.message}", e)
            // 🔇 Erro silencioso
            finish()
        }
    }
    
    private fun captureScreen() {
        try {
            Log.d(TAG, "captureScreen: Capturando tela...")
            Log.d(TAG, "captureScreen: Dimensões: ${screenWidth}x${screenHeight}, Densidade: $screenDensity")
            
            // Cria o ImageReader para capturar frames
            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                PixelFormat.RGBA_8888, 2
            )
            Log.d(TAG, "captureScreen: ImageReader criado: ${screenWidth}x${screenHeight}")
            
            // Cria o VirtualDisplay para captura da tela
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, null
            )
            
            if (virtualDisplay == null) {
                Log.e(TAG, "captureScreen: VirtualDisplay é null")
                // 🔇 Erro silencioso
                finish()
                return
            }
            
            Log.d(TAG, "captureScreen: Captura iniciada, aguardando frame...")
            
            // Configura listener para capturar o frame
            var frameCaptured = false
            imageReader?.setOnImageAvailableListener({ reader ->
                try {
                    if (!frameCaptured) {
                        val image = reader.acquireLatestImage()
                        if (image != null) {
                            Log.d(TAG, "captureScreen: Frame capturado, processando...")
                            Log.d(TAG, "captureScreen: Imagem: ${image.width}x${image.height}, ${image.format}")
                            frameCaptured = true
                            processCapturedImage(image)
                            image.close()
                            
                            // Para a captura após processar o frame
                            stopCapture()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "captureScreen: Erro ao processar frame: ${e.message}", e)
                }
            }, null)
            
            // Aguarda um pouco para capturar o frame
            Thread.sleep(1000)
            
            // Se não capturou frame em 1s, para a captura
            if (!frameCaptured) {
                Log.w(TAG, "captureScreen: Timeout - nenhum frame capturado")
                stopCapture()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "captureScreen: Erro: ${e.message}", e)
            // 🔇 Erro silencioso
            finish()
        }
    }
    
    private fun stopCapture() {
        try {
            Log.d(TAG, "stopCapture: Parando captura...")
            
            // Remove o listener
            imageReader?.setOnImageAvailableListener(null, null)
            
            // Para o MediaProjection
            mediaProjection?.stop()
            
            Log.d(TAG, "stopCapture: Captura parada com sucesso")
            
        } catch (e: Exception) {
            Log.e(TAG, "stopCapture: Erro: ${e.message}", e)
        }
    }
    
    private fun processCapturedImage(image: android.media.Image) {
        try {
            Log.d(TAG, "processCapturedImage: Processando imagem...")
            Log.d(TAG, "processCapturedImage: Imagem: ${image.width}x${image.height}, ${image.format}")
            
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            Log.d(TAG, "processCapturedImage: pixelStride: $pixelStride, rowStride: $rowStride, rowPadding: $rowPadding")
            
            // Cria o bitmap com dimensões corretas
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight, Bitmap.Config.ARGB_8888
            )
            Log.d(TAG, "processCapturedImage: Bitmap criado: ${bitmap.width}x${bitmap.height}")
            
            bitmap.copyPixelsFromBuffer(buffer)
            
            Log.d(TAG, "processCapturedImage: Imagem processada com sucesso: ${bitmap.width}x${bitmap.height}")
            
            // Salva o screenshot
            saveScreenshot(bitmap)
            
        } catch (e: Exception) {
            Log.e(TAG, "processCapturedImage: Erro: ${e.message}", e)
            // 🔇 Erro silencioso
        }
    }
    
    private fun saveScreenshot(bitmap: Bitmap) {
        try {
            Log.d(TAG, "saveScreenshot: Salvando screenshot...")
            Log.d(TAG, "saveScreenshot: Bitmap: ${bitmap.width}x${bitmap.height}, Config: ${bitmap.config}")
            
            // Cria o diretório se não existir
            val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
            val screenshotsDir = File(picturesDir, "Screenshots")
            if (!screenshotsDir.exists()) {
                screenshotsDir.mkdirs()
            }
            
            // Cria o nome do arquivo
            val timestamp = System.currentTimeMillis()
            val fileName = "screenshot_$timestamp.png"
            val file = File(screenshotsDir, fileName)
            
            Log.d(TAG, "saveScreenshot: Salvando em: ${file.absolutePath}")
            
            // Salva o arquivo
            val outputStream = FileOutputStream(file)
            val compressed = bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Log.d(TAG, "saveScreenshot: Compressão PNG: $compressed")
            Log.d(TAG, "saveScreenshot: Arquivo salvo: ${file.absolutePath}")
            Log.d(TAG, "saveScreenshot: Tamanho do arquivo: ${file.length()} bytes")
            
            // Verifica se o arquivo foi criado
            if (file.exists() && file.length() > 0) {
                Log.d(TAG, "saveScreenshot: ✅ Arquivo criado com sucesso!")
                
                // Mostra toast de sucesso
                // 🔇 Screenshot salvo silenciosamente
                
                // Aguarda um pouco e finaliza
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    try {
                        Log.d(TAG, "saveScreenshot: Finalizando Activity...")
                        finish()
                    } catch (e: Exception) {
                        Log.w(TAG, "saveScreenshot: Erro ao finalizar: ${e.message}")
                        finish()
                    }
                }, 2000)
                
            } else {
                Log.e(TAG, "saveScreenshot: ❌ ERRO! Arquivo não foi criado ou está vazio!")
                Log.e(TAG, "saveScreenshot: Arquivo existe: ${file.exists()}")
                Log.e(TAG, "saveScreenshot: Tamanho: ${file.length()} bytes")
                
                // 🔇 Erro silencioso
                finish()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "saveScreenshot: Erro: ${e.message}", e)
            // 🔇 Erro silencioso
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Limpando recursos...")
        
        try {
            // Limpa os recursos
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
            
            Log.d(TAG, "onDestroy: Recursos limpos")
            
        } catch (e: Exception) {
            Log.e(TAG, "onDestroy: Erro ao limpar recursos: ${e.message}", e)
        }
    }
    



    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // ✅ SIMPLES: apenas fechar a activity
        finish()
    }
}
