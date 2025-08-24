package com.example.floatingbutton

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class FreeDrawCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "FreeDrawCropView"
        private const val STROKE_WIDTH = 4f
        private const val MIN_DISTANCE = 8f
        private const val GLOW_WIDTH = 12f
    }

    private var isDrawing = false
    private val drawPath = Path()
    private val drawPoints = mutableListOf<PointF>()
    private val smoothPath = Path()
    
    // Paint principal - linha branca suave
    private val strokePaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = STROKE_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    
    // Paint para glow/sombra - efeito Circle to Search
    private val glowPaint = Paint().apply {
        color = Color.parseColor("#80FFFFFF")
        style = Paint.Style.STROKE
        strokeWidth = GLOW_WIDTH
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
        maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.OUTER)
    }

    private val fillPaint = Paint().apply {
        color = Color.parseColor("#20FFFFFF") // Muito sutil
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#60000000") // Menos escuro
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var onDrawingChangedListener: ((Path, List<PointF>) -> Unit)? = null
    private var onDrawingCompletedListener: ((Path, List<PointF>) -> Unit)? = null

    fun setOnDrawingChangedListener(listener: (Path, List<PointF>) -> Unit) {
        onDrawingChangedListener = listener
    }
    
    fun setOnDrawingCompletedListener(listener: (Path, List<PointF>) -> Unit) {
        onDrawingCompletedListener = listener
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (drawPoints.isNotEmpty()) {
            // Cria path suavizado
            updateSmoothPath()
            
            // Se estamos desenhando, apenas mostra a linha (sem fundo escuro)
            if (isDrawing) {
                // Apenas a linha de desenho, sem overlay
                canvas.drawPath(smoothPath, glowPaint)
                canvas.drawPath(smoothPath, strokePaint)
            } else {
                // Quando terminou o desenho, mostra a seleção final
                if (drawPoints.size > 2) {
                    // Desenha o overlay escuro sobre toda a tela
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
                    
                    val closedPath = Path(smoothPath)
                    closedPath.close()
                    
                    // Remove a área selecionada do overlay (fica transparente)
                    val save = canvas.save()
                    canvas.clipPath(closedPath)
                    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
                    canvas.restoreToCount(save)
                    
                    // Desenha o preenchimento sutil na área selecionada
                    canvas.drawPath(closedPath, fillPaint)
                }
                
                // Desenha o glow e linha principal
                canvas.drawPath(smoothPath, glowPaint)
                canvas.drawPath(smoothPath, strokePaint)
            }
        }
    }
    
    private fun updateSmoothPath() {
        smoothPath.reset()
        
        if (drawPoints.isEmpty()) return
        
        if (drawPoints.size == 1) {
            smoothPath.moveTo(drawPoints[0].x, drawPoints[0].y)
            return
        }
        
        // Algoritmo de suavização usando curvas quadráticas
        smoothPath.moveTo(drawPoints[0].x, drawPoints[0].y)
        
        for (i in 1 until drawPoints.size) {
            val p1 = drawPoints[i - 1]
            val p2 = drawPoints[i]
            
            if (i < drawPoints.size - 1) {
                val p3 = drawPoints[i + 1]
                val midX = (p2.x + p3.x) / 2f
                val midY = (p2.y + p3.y) / 2f
                smoothPath.quadTo(p2.x, p2.y, midX, midY)
            } else {
                smoothPath.lineTo(p2.x, p2.y)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startDrawing(x, y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                continueDrawing(x, y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                finishDrawing()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun startDrawing(x: Float, y: Float) {
        isDrawing = true
        drawPath.reset()
        drawPoints.clear()
        
        drawPath.moveTo(x, y)
        drawPoints.add(PointF(x, y))
        
        Log.d(TAG, "startDrawing: Iniciando desenho em ($x, $y)")
        invalidate()
    }

    private fun continueDrawing(x: Float, y: Float) {
        if (!isDrawing) return
        
        // Só adiciona pontos se estiver longe o suficiente do último ponto
        val lastPoint = drawPoints.lastOrNull()
        if (lastPoint == null || getDistance(lastPoint.x, lastPoint.y, x, y) > MIN_DISTANCE) {
            drawPath.lineTo(x, y)
            drawPoints.add(PointF(x, y))
            
            Log.d(TAG, "continueDrawing: Adicionando ponto ($x, $y) - Total: ${drawPoints.size}")
            invalidate()
            
            // Notifica sobre a mudança
            onDrawingChangedListener?.invoke(drawPath, drawPoints.toList())
        }
    }

    private fun finishDrawing() {
        if (!isDrawing) return
        
        isDrawing = false
        
        // Fecha o path se temos pontos suficientes
        if (drawPoints.size > 2) {
            drawPath.close()
            Log.d(TAG, "finishDrawing: Desenho concluído com ${drawPoints.size} pontos")
            
            // Notifica sobre a conclusão para análise inteligente
            onDrawingCompletedListener?.invoke(drawPath, drawPoints.toList())
        } else {
            Log.w(TAG, "finishDrawing: Muito poucos pontos para criar uma área")
            clearDrawing()
        }
        
        invalidate()
    }

    private fun getDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        return sqrt((x2 - x1).pow(2) + (y2 - y1).pow(2))
    }

    fun clearDrawing() {
        drawPath.reset()
        drawPoints.clear()
        invalidate()
        Log.d(TAG, "clearDrawing: Desenho limpo")
    }

    fun hasDrawing(): Boolean {
        return drawPoints.size > 2
    }

    fun getDrawingPath(): Path {
        val closedPath = Path(drawPath)
        if (drawPoints.size > 2) {
            closedPath.close()
        }
        return closedPath
    }

    fun getDrawingPoints(): List<PointF> {
        return drawPoints.toList()
    }

    // Retorna o retângulo que contém a área desenhada
    fun getDrawingBounds(): RectF {
        if (drawPoints.isEmpty()) return RectF()
        
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        
        for (point in drawPoints) {
            minX = minOf(minX, point.x)
            maxX = maxOf(maxX, point.x)
            minY = minOf(minY, point.y)
            maxY = maxOf(maxY, point.y)
        }
        
        return RectF(minX, minY, maxX, maxY)
    }
    
    // Método para ajustar automaticamente a seleção para um retângulo específico
    fun adjustSelectionToRect(rect: RectF) {
        Log.d(TAG, "adjustSelectionToRect: Ajustando seleção para: $rect")
        
        // Limpa os pontos atuais
        drawPoints.clear()
        drawPath.reset()
        smoothPath.reset()
        
        // Cria pontos para um retângulo perfeito
        drawPoints.add(PointF(rect.left, rect.top))
        drawPoints.add(PointF(rect.right, rect.top))
        drawPoints.add(PointF(rect.right, rect.bottom))
        drawPoints.add(PointF(rect.left, rect.bottom))
        drawPoints.add(PointF(rect.left, rect.top)) // Fecha o retângulo
        
        // Reconstrói o path
        drawPath.moveTo(rect.left, rect.top)
        drawPath.lineTo(rect.right, rect.top)
        drawPath.lineTo(rect.right, rect.bottom)
        drawPath.lineTo(rect.left, rect.bottom)
        drawPath.close()
        
        isDrawing = false
        invalidate()
        
        Log.d(TAG, "adjustSelectionToRect: Seleção ajustada automaticamente!")
    }
}
