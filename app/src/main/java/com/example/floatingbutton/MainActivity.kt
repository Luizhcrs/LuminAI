package com.example.floatingbutton

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.floatingbutton.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private val REQUEST_CODE_OVERLAY_PERMISSION = 1234
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        checkOverlayPermission()
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
            startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
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
    }
    
    private fun startFloatingButtonService() {
        val serviceIntent = Intent(this, FloatingButtonService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Toast.makeText(this, "Serviço iniciado! O botão flutuante deve aparecer.", Toast.LENGTH_SHORT).show()
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
