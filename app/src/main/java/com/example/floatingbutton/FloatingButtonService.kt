package com.example.floatingbutton

import android.app.*
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
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
        private const val CHANNEL_ID = "FloatingButtonChannel"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        setupFloatingButton()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Botão Flutuante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Serviço do botão flutuante em execução"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Botão Flutuante Ativo")
            .setContentText("Toque para abrir o app")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun setupFloatingButton() {
        // Infla o layout do botão flutuante
        val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingButtonView = inflater.inflate(R.layout.floating_button_layout, null)

        // Define o tipo de janela baseado na versão do Android
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

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

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        windowManager.addView(floatingButtonView, layoutParams)

        // Configura o botão
        setupButtonBehavior()
    }

    private fun setupButtonBehavior() {
        val floatingButton = floatingButtonView.findViewById<ImageView>(R.id.floating_button)
        
        // Listener para clique no botão
        floatingButton.setOnClickListener {
            Toast.makeText(this, "Botão flutuante clicado!", Toast.LENGTH_SHORT).show()
            // Aqui você pode adicionar qualquer ação que desejar
            // Por exemplo, abrir uma nova Activity, mostrar um menu, etc.
        }

        // Listener para arrastar o botão
        floatingButtonView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isDragging = false
                        return true
                    }
                    
                    MotionEvent.ACTION_MOVE -> {
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
                            } catch (e: Exception) {
                                // Ignora erros de layout
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
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove a view da janela quando o serviço é destruído
        if (::floatingButtonView.isInitialized && ::windowManager.isInitialized) {
            try {
                windowManager.removeView(floatingButtonView)
            } catch (e: Exception) {
                // Ignora erros ao remover a view
            }
        }
    }
}
