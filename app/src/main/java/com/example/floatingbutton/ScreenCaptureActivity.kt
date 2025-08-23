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
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer

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
        Log.d(TAG, "onCreate: Iniciando captura de tela...")
        
        try {
            // Obtém as dimensões da tela
            val displayMetrics = resources.displayMetrics
            screenWidth = displayMetrics.widthPixels
            screenHeight = displayMetrics.heightPixels
            screenDensity = displayMetrics.densityDpi
            
            Log.d(TAG, "onCreate: Dimensões da tela: ${screenWidth}x${screenHeight}, Densidade: $screenDensity")
            
            // Solicita permissão de captura de tela
            requestScreenCapturePermission()
            
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Erro: ${e.message}", e)
            Toast.makeText(this, "Erro ao inicializar captura: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun requestScreenCapturePermission() {
        try {
            Log.d(TAG, "requestScreenCapturePermission: Solicitando permissão...")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                
                val intent = mediaProjectionManager?.createScreenCaptureIntent()
                if (intent != null) {
                    startActivityForResult(intent, REQUEST_MEDIA_PROJECTION)
                } else {
                    Log.e(TAG, "requestScreenCapturePermission: Intent de captura é null")
                    Toast.makeText(this, "Erro ao criar intent de captura", Toast.LENGTH_LONG).show()
                    finish()
                }
            } else {
                Log.w(TAG, "requestScreenCapturePermission: MediaProjection não suportado nesta versão")
                Toast.makeText(this, "Captura de tela não suportada nesta versão", Toast.LENGTH_LONG).show()
                finish()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "requestScreenCapturePermission: Erro: ${e.message}", e)
            Toast.makeText(this, "Erro ao solicitar permissão: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                Log.d(TAG, "onActivityResult: Permissão concedida, iniciando captura...")
                startScreenCapture(resultCode, data)
            } else {
                Log.w(TAG, "onActivityResult: Permissão negada ou cancelada")
                Toast.makeText(this, "Permissão de captura negada", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }
    
    private fun startScreenCapture(resultCode: Int, data: Intent) {
        try {
            Log.d(TAG, "startScreenCapture: Iniciando captura...")
            
            // Cria o MediaProjection
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
            if (mediaProjection == null) {
                Log.e(TAG, "startScreenCapture: MediaProjection é null")
                Toast.makeText(this, "Erro ao criar MediaProjection", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            // Registra o callback obrigatório
            Log.d(TAG, "startScreenCapture: Registrando callback do MediaProjection...")
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.d(TAG, "MediaProjection callback: onStop chamado")
                    super.onStop()
                }
            }, null)
            Log.d(TAG, "startScreenCapture: Callback registrado com sucesso")
            
            // Captura a tela atual diretamente
            captureCurrentScreen()
            
        } catch (e: Exception) {
            Log.e(TAG, "startScreenCapture: Erro: ${e.message}", e)
            Toast.makeText(this, "Erro ao iniciar captura: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun captureCurrentScreen() {
        try {
            Log.d(TAG, "captureCurrentScreen: Capturando tela atual...")
            
            // Cria o ImageReader para capturar frames
            imageReader = ImageReader.newInstance(
                screenWidth, screenHeight,
                PixelFormat.RGBA_8888, 2
            )
            
            // Cria o VirtualDisplay para captura estática
            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                screenWidth, screenHeight, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, // Espelha a tela atual
                imageReader?.surface, null, null
            )
            
            if (virtualDisplay == null) {
                Log.e(TAG, "captureCurrentScreen: VirtualDisplay é null")
                Toast.makeText(this, "Erro ao criar VirtualDisplay", Toast.LENGTH_LONG).show()
                finish()
                return
            }
            
            Log.d(TAG, "captureCurrentScreen: Captura iniciada, aguardando frame...")
            
            // Configura listener para capturar apenas um frame
            var frameCaptured = false
            imageReader?.setOnImageAvailableListener({ reader ->
                try {
                    if (!frameCaptured) {
                        val image = reader.acquireLatestImage()
                        if (image != null) {
                            Log.d(TAG, "captureCurrentScreen: Frame capturado, processando...")
                            frameCaptured = true
                            processCapturedImage(image)
                            image.close()
                            
                            // Para a captura imediatamente após processar o frame
                            stopCapture()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "captureCurrentScreen: Erro ao processar frame: ${e.message}", e)
                }
            }, null)
            
            // Aguarda um pouco para capturar o frame (reduzido para captura mais rápida)
            Thread.sleep(300)
            
            // Se não capturou frame em 300ms, para a captura
            if (!frameCaptured) {
                Log.w(TAG, "captureCurrentScreen: Timeout - nenhum frame capturado")
                stopCapture()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "captureCurrentScreen: Erro: ${e.message}", e)
            Toast.makeText(this, "Erro ao capturar tela: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun stopCapture() {
        try {
            Log.d(TAG, "stopCapture: Parando captura...")
            
            // Remove o listener para evitar capturas adicionais
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
            
            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * screenWidth
            
            // Cria o bitmap
            val bitmap = Bitmap.createBitmap(
                screenWidth + rowPadding / pixelStride,
                screenHeight, Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Salva o arquivo
            saveScreenshot(bitmap)
            
        } catch (e: Exception) {
            Log.e(TAG, "processCapturedImage: Erro: ${e.message}", e)
            Toast.makeText(this, "Erro ao processar imagem: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun saveScreenshot(bitmap: Bitmap) {
        try {
            Log.d(TAG, "saveScreenshot: Salvando screenshot...")
            
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
            
            // Salva o arquivo
            val outputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Log.d(TAG, "saveScreenshot: Screenshot salvo: ${file.absolutePath}")
            Toast.makeText(this, "Screenshot salvo: $fileName", Toast.LENGTH_LONG).show()
            
            // Finaliza a Activity
            finish()
            
        } catch (e: Exception) {
            Log.e(TAG, "saveScreenshot: Erro: ${e.message}", e)
            Toast.makeText(this, "Erro ao salvar screenshot: ${e.message}", Toast.LENGTH_LONG).show()
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
}
