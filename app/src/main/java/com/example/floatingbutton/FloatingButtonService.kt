package com.example.floatingbutton

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class FloatingButtonService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingButtonView: View
    private lateinit var layoutParams: WindowManager.LayoutParams
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "LuminChannel"
        private const val TAG = "FloatingButtonService"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Iniciando serviço...")
        try {
            // Primeiro configura o botão flutuante
            setupFloatingButton()
            Log.d(TAG, "onCreate: Botão flutuante configurado com sucesso!")
            
            // Depois inicia como foreground service para suportar MediaProjection
            createNotificationChannel()
            Log.d(TAG, "onCreate: Canal de notificação criado")
            
            startForeground(NOTIFICATION_ID, createNotification())
            Log.d(TAG, "onCreate: Serviço em foreground iniciado")
            
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Erro crítico: ${e.message}", e)
            stopSelf()
        }
    }

    private fun createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel: Criando canal de notificação...")
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lumin",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Lumin - Análise inteligente de imagens"
                    setShowBadge(false)
                }
                
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "createNotificationChannel: Canal criado com sucesso")
            } else {
                Log.d(TAG, "createNotificationChannel: Android < 8.0, pulando criação de canal")
            }
        } catch (e: Exception) {
            Log.e(TAG, "createNotificationChannel: Erro: ${e.message}", e)
            throw e
        }
    }

    private fun createNotification(): Notification {
        Log.d(TAG, "createNotification: Criando notificação...")
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Lumin Ativo")
                .setContentText("Compartilhe imagens para análise inteligente")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build()
            
            Log.d(TAG, "createNotification: Notificação criada com sucesso")
            return notification
        } catch (e: Exception) {
            Log.e(TAG, "createNotification: Erro: ${e.message}", e)
            throw e
        }
    }

    private fun setupFloatingButton() {
        Log.d(TAG, "setupFloatingButton: Iniciando configuração do botão...")
        try {
            // Verifica se a permissão ainda está ativa ANTES de qualquer operação
            Log.d(TAG, "setupFloatingButton: Verificando permissão de sobreposição...")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Log.w(TAG, "setupFloatingButton: Permissão de sobreposição foi revogada!")
                // 🔇 Permissão revogada - silencioso
                stopSelf()
                return
            }
            Log.d(TAG, "setupFloatingButton: Permissão de sobreposição confirmada")

            // Infla o layout do botão flutuante
            Log.d(TAG, "setupFloatingButton: Inflando layout...")
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            floatingButtonView = inflater.inflate(R.layout.floating_button_layout, null)
            Log.d(TAG, "setupFloatingButton: Layout inflado com sucesso")

            // Define o tipo de janela - sempre TYPE_APPLICATION_OVERLAY para Android 8.0+
            Log.d(TAG, "setupFloatingButton: Configurando parâmetros da janela...")
            val type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            Log.d(TAG, "setupFloatingButton: Tipo de janela definido: $type")

            // Define os parâmetros da janela
            layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            )

            layoutParams.gravity = Gravity.TOP or Gravity.START
            layoutParams.x = 100
            layoutParams.y = 200
            Log.d(TAG, "setupFloatingButton: Parâmetros da janela configurados")

            Log.d(TAG, "setupFloatingButton: Obtendo WindowManager...")
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            Log.d(TAG, "setupFloatingButton: WindowManager obtido com sucesso")
            
            // Adiciona a view com timeout para evitar travamentos
            Log.d(TAG, "setupFloatingButton: Adicionando view à janela...")
            try {
                windowManager.addView(floatingButtonView, layoutParams)
                Log.d(TAG, "setupFloatingButton: View adicionada à janela com sucesso")
                
                // Configura o botão
                Log.d(TAG, "setupFloatingButton: Configurando comportamento do botão...")
                setupButtonBehavior()
                Log.d(TAG, "setupFloatingButton: Comportamento configurado com sucesso")
                
                // 🔇 Botão criado silenciosamente
                Log.d(TAG, "setupFloatingButton: Botão flutuante criado com sucesso!")
                
            } catch (e: Exception) {
                Log.e(TAG, "setupFloatingButton: Erro ao adicionar botão à tela: ${e.message}", e)
                // 🔇 Erro silencioso
                e.printStackTrace()
                stopSelf()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "setupFloatingButton: Erro ao criar botão flutuante: ${e.message}", e)
            // 🔇 Erro silencioso
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun setupButtonBehavior() {
        val floatingButton = floatingButtonView.findViewById<ImageView>(R.id.floating_button)
        
        // Listener para clique no botão com timeout
        floatingButton.setOnClickListener {
            try {
                // Captura silenciosa
                // Captura a tela
                captureScreen()
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao capturar tela: ${e.message}", e)
                // 🔇 Erro silencioso
            }
        }

        // Listener para arrastar o botão com proteção contra travamentos
        floatingButtonView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var isDragging = false
            private var lastUpdateTime = 0L

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                try {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = layoutParams.x
                            initialY = layoutParams.y
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            lastUpdateTime = System.currentTimeMillis()
                            return true
                        }
                        
                        MotionEvent.ACTION_MOVE -> {
                            val currentTime = System.currentTimeMillis()
                            
                            // Limita atualizações para evitar sobrecarga
                            if (currentTime - lastUpdateTime < 16) { // ~60 FPS
                                return true
                            }
                            
                            val deltaX = (event.rawX - initialTouchX).toInt()
                            val deltaY = (event.rawY - initialTouchY).toInt()
                            
                            // Só considera como arrastar se o movimento for significativo
                            if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                                isDragging = true
                            }
                            
                            if (isDragging) {
                                layoutParams.x = initialX + deltaX
                                layoutParams.y = initialY + deltaY
                                
                                // Mantém o botão dentro dos limites da tela
                                val displayMetrics = resources.displayMetrics
                                val screenWidth = displayMetrics.widthPixels
                                val screenHeight = displayMetrics.heightPixels
                                
                                if (layoutParams.x < 0) layoutParams.x = 0
                                if (layoutParams.y < 0) layoutParams.y = 0
                                if (layoutParams.x > screenWidth - 200) layoutParams.x = screenWidth - 200
                                if (layoutParams.y > screenHeight - 200) layoutParams.y = screenHeight - 200
                                
                                try {
                                    windowManager.updateViewLayout(floatingButtonView, layoutParams)
                                    lastUpdateTime = currentTime
                                } catch (e: Exception) {
                                    // Ignora erros de layout para evitar travamentos
                                }
                            }
                            return true
                        }
                        
                        MotionEvent.ACTION_UP -> {
                            if (!isDragging) {
                                // Se não estava arrastando, é um clique
                                v.performClick()
                            }
                            return true
                        }
                        
                        else -> return false
                    }
                } catch (e: Exception) {
                    // Proteção contra qualquer erro no touch
                    return false
                }
            }
        })
    }
    
    private fun captureScreen() {
        try {
            Log.d(TAG, "captureScreen: Iniciando captura de tela...")
            
            // Inicia captura via ScreenCaptureActivity
            
            // Captura a tela usando MediaProjection (requer permissão)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Para Android 5.0+, usamos MediaProjection
                requestScreenCapturePermission()
            } else {
                // Para versões mais antigas, tentamos método alternativo
                captureScreenLegacy()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "captureScreen: Erro: ${e.message}", e)
            Toast.makeText(this, "Erro ao capturar tela: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun requestScreenCapturePermission() {
        try {
            Log.d(TAG, "requestScreenCapturePermission: Solicitando permissão...")
            
            // Cria uma Intent para solicitar permissão de captura de tela
            val intent = Intent(this, ScreenCaptureActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            }
            
            startActivity(intent)
            
        } catch (e: Exception) {
            Log.e(TAG, "requestScreenCapturePermission: Erro: ${e.message}", e)
            // 🔇 Erro silencioso
        }
    }
    
    private fun captureScreenLegacy() {
        try {
            Log.d(TAG, "captureScreen: Tentando captura legada...")
            
            // Método alternativo para versões mais antigas
            // Pode não funcionar em todas as versões
            // 🔇 Versão não suportada - silencioso
            
        } catch (e: Exception) {
            Log.e(TAG, "captureScreenLegacy: Erro: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Serviço sendo destruído...")
        // Remove a view da janela quando o serviço é destruído
        if (::floatingButtonView.isInitialized && ::windowManager.isInitialized) {
            try {
                Log.d(TAG, "onDestroy: Removendo view da janela...")
                windowManager.removeView(floatingButtonView)
                Log.d(TAG, "onDestroy: View removida com sucesso")
                // Remoção silenciosa
            } catch (e: Exception) {
                Log.e(TAG, "onDestroy: Erro ao remover botão: ${e.message}", e)
                // 🔇 Erro silencioso
                e.printStackTrace()
            }
        } else {
            Log.w(TAG, "onDestroy: View ou WindowManager não inicializados")
        }
        Log.d(TAG, "onDestroy: Serviço destruído")
    }
}
