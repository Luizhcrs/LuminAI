package com.example.floatingbutton
import android.app.Activity
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
    
    // ❌ REMOVER estas variáveis (não são mais necessárias)
    // private lateinit var imageView: ImageView
    // private lateinit var tvImageStatus: TextView
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Iniciando MainActivity...")
        try {
            setContentView(R.layout.activity_main)
            
            // ✅ APENAS inicialização da tela inicial
            initViews()
            setupUI()
            checkOverlayPermission()
            
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Erro crítico: ${e.message}", e)
        }
    }
    
    // ❌ REMOVER estas funções (não são mais necessárias)
    // override fun onNewIntent(intent: Intent?)
    // private fun handleSharedImage(intent: Intent?)
    // private fun displaySharedImage(imageUri: Uri)
    // private fun openImageViewer(imageUri: Uri)
    
    // ✅ MANTER apenas funções da tela inicial
    private fun initViews() {
        // ... views da tela inicial (botões, etc.)
    }
    
    private fun setupUI() {
        // ... configuração dos botões
    }
    
    private fun checkOverlayPermission() {
        // ... verificação de permissões
    }
}
