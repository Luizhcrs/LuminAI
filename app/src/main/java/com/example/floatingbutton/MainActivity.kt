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
import com.example.floatingbutton.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    
    companion object {
        private const val TAG = "MainActivity"
    }
    
    // Activity Result Launcher para permissão de sobreposição
    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Verifica se a permissão foi concedida
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Permissão concedida! Agora você pode iniciar o serviço.", Toast.LENGTH_LONG).show()
                checkOverlayPermission()
            } else {
                Toast.makeText(this, "Permissão negada. O botão flutuante não funcionará.", Toast.LENGTH_LONG).show()
                checkOverlayPermission()
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Iniciando MainActivity...")
        try {
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            Log.d(TAG, "onCreate: Layout configurado com sucesso")
            
            setupUI()
            Log.d(TAG, "onCreate: UI configurada")
            
            checkOverlayPermission()
            Log.d(TAG, "onCreate: Permissão verificada")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Erro crítico: ${e.message}", e)
        }
    }
    
    private fun setupUI() {
        binding.btnStartService.setOnClickListener {
            if (Settings.canDrawOverlays(this)) {
                startFloatingButtonService()
            } else {
                requestOverlayPermission()
            }
        }
        
        binding.btnStopService.setOnClickListener {
            stopFloatingButtonService()
        }
        
        binding.btnCheckPermission.setOnClickListener {
            checkOverlayPermission()
        }
    }
    
    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                binding.tvPermissionStatus.text = "✅ Permissão concedida"
                binding.btnStartService.isEnabled = true
                binding.btnStopService.isEnabled = true
            } else {
                binding.tvPermissionStatus.text = "❌ Permissão negada"
                binding.btnStartService.isEnabled = false
                binding.btnStopService.isEnabled = false
            }
        } else {
            binding.tvPermissionStatus.text = "✅ Permissão automática (Android < 6.0)"
            binding.btnStartService.isEnabled = true
            binding.btnStopService.isEnabled = true
        }
    }
    
    private fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        }
    }
    
    private fun startFloatingButtonService() {
        Log.d(TAG, "startFloatingButtonService: Iniciando serviço...")
        try {
            // Verifica novamente se a permissão está ativa
            Log.d(TAG, "startFloatingButtonService: Verificando permissão...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.w(TAG, "startFloatingButtonService: Permissão de sobreposição necessária!")
                Toast.makeText(this, "Permissão de sobreposição necessária!", Toast.LENGTH_LONG).show()
                requestOverlayPermission()
                return
            }
            Log.d(TAG, "startFloatingButtonService: Permissão confirmada")
            
            val serviceIntent = Intent(this, FloatingButtonService::class.java)
            Log.d(TAG, "startFloatingButtonService: Intent criado")
            
            // Usa startForegroundService para Android 8.0+ (requerido para MediaProjection)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "startFloatingButtonService: Iniciando foreground service...")
                startForegroundService(serviceIntent)
            } else {
                Log.d(TAG, "startFloatingButtonService: Iniciando service normal...")
                startService(serviceIntent)
            }
            Log.d(TAG, "startFloatingButtonService: Serviço iniciado com sucesso")
            Toast.makeText(this, "Serviço iniciado! O botão flutuante deve aparecer.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e(TAG, "startFloatingButtonService: Erro ao iniciar serviço: ${e.message}", e)
            Toast.makeText(this, "Erro ao iniciar serviço: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    
    private fun stopFloatingButtonService() {
        val serviceIntent = Intent(this, FloatingButtonService::class.java)
        stopService(serviceIntent)
        Toast.makeText(this, "Serviço parado! O botão flutuante foi removido.", Toast.LENGTH_SHORT).show()
    }
    
    override fun onResume() {
        super.onResume()
        checkOverlayPermission()
    }
}
