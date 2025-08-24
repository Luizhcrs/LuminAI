package com.example.floatingbutton

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.widget.Button
import android.widget.TextView
import android.widget.ImageView

class MainActivity : AppCompatActivity() {
    
    private lateinit var imageView: ImageView
    private lateinit var tvImageStatus: TextView
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // Activity Result Launcher para permissão de sobreposição
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Iniciando MainActivity...")
        try {
            setContentView(R.layout.activity_main)
            
            // Inicializa views
            initViews()
            
            // Verifica se recebeu uma imagem compartilhada
            handleSharedImage(intent)
            
            
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Erro crítico: ${e.message}", e)
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // NOVO: Trata quando o usuário compartilha uma nova imagem
        handleSharedImage(intent)
    }
    
    private fun handleSharedImage(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("image/") == true) {
            Log.d(TAG, "handleSharedImage: Recebeu imagem compartilhada!")
            
            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }
            
            if (imageUri != null) {
                Log.d(TAG, "handleSharedImage: URI da imagem: $imageUri")
                displaySharedImage(imageUri)
            } else {
                Log.e(TAG, "handleSharedImage: URI da imagem é null!")
            }
        }
    }
    
    private fun displaySharedImage(imageUri: Uri) {
        try {
            Log.d(TAG, "displaySharedImage: Exibindo imagem compartilhada...")
            
            // Exibe a imagem na ImageView
            // Views já inicializadas em initViews()
            
            if (imageView != null) {
                imageView.setImageURI(imageUri)
                tvImageStatus?.text = "✅ Imagem recebida com sucesso!"
                tvImageStatus?.setTextColor(getColor(R.color.success_text))
                
                Log.d(TAG, "displaySharedImage: Imagem exibida com sucesso!")
                // 🔇 Imagem processada silenciosamente
                
                // NOVO: Abre a ImageViewerActivity em fullscreen
                openImageViewer(imageUri)
                
            } else {
                Log.w(TAG, "displaySharedImage: ImageView não encontrada no layout!")
                // 🔇 Erro silencioso
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "displaySharedImage: Erro ao exibir imagem: ${e.message}", e)
            // 🔇 Erro silencioso
        }
    }
    
    private fun openImageViewer(imageUri: Uri) {
        try {
            Log.d(TAG, "openImageViewer: Abrindo visualizador fullscreen...")
            
                                val intent = Intent(this, UltimateImageViewerActivity::class.java).apply {
                        putExtra(UltimateImageViewerActivity.EXTRA_IMAGE_URI, imageUri)
                    }
            
            startActivity(intent)
            Log.d(TAG, "openImageViewer: ImageViewerActivity iniciada")
            
        } catch (e: Exception) {
            Log.e(TAG, "openImageViewer: Erro ao abrir visualizador: ${e.message}", e)
            // 🔇 Erro silencioso
        }
    }
    
    private fun initViews() {
        imageView = findViewById(R.id.imageView)
        tvImageStatus = findViewById(R.id.tvImageStatus)
    }
    
    
    override fun onResume() {
        super.onResume()
    }
    
    // 🔇 Função showToast removida - operação silenciosa
}
