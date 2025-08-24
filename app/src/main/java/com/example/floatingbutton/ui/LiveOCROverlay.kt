package com.example.floatingbutton.ui

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.*
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.*

/**
 * 📝 Live OCR Overlay - OCR ativo em tempo real sobre texto selecionado
 * 
 * Características:
 * - OCR em tempo real da área selecionada
 * - Texto selecionável como em sites
 * - Highlight de palavras individuais
 * - Cópia para clipboard
 * - Animações suaves
 * - Interface moderna
 */
class LiveOCROverlay @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "LiveOCROverlay"
        private const val SELECTION_ANIMATION_DURATION = 200L
        private const val HIGHLIGHT_ALPHA = 100
        private const val TEXT_PADDING = 16f
    }

    // 📝 OCR Data
    data class OCRTextBlock(
        val text: String,
        val bounds: RectF,
        val confidence: Float,
        val lines: List<OCRTextLine> = emptyList()
    )

    data class OCRTextLine(
        val text: String,
        val bounds: RectF,
        val words: List<OCRWord> = emptyList()
    )

    data class OCRWord(
        val text: String,
        val bounds: RectF,
        val confidence: Float
    )

    // 🎨 Estado do OCR
    private var ocrTextBlocks = mutableListOf<OCRTextBlock>()
    private var selectedWord: OCRWord? = null
    private var selectedLine: OCRTextLine? = null
    private var selectedBlock: OCRTextBlock? = null
    private var isOCRActive = false
    private var selectionMode = SelectionMode.WORD

    enum class SelectionMode { WORD, LINE, BLOCK }

    // 🎨 Paints para renderização
    private val textPaint = TextPaint().apply {
        isAntiAlias = true
        textSize = 18f // 🎯 Tamanho base ajustável
        color = Color.WHITE
        typeface = Typeface.DEFAULT
        setShadowLayer(2f, 1f, 1f, Color.BLACK)
    }

    private val highlightPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#4285F4")
        alpha = HIGHLIGHT_ALPHA
    }

    private val selectionPaint = Paint().apply {
        isAntiAlias = true
        color = Color.parseColor("#34A853")
        alpha = HIGHLIGHT_ALPHA + 50
    }

    private val boundsPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.parseColor("#80FFFFFF")
        pathEffect = DashPathEffect(floatArrayOf(8f, 4f), 0f)
    }

    // 🎯 Gesture Detection
    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onSingleTapUp(e: MotionEvent): Boolean {
            handleTap(e.x, e.y)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            handleDoubleTap(e.x, e.y)
            return true
        }

        override fun onLongPress(e: MotionEvent) {
            handleLongPress(e.x, e.y)
        }
    })

    // 🤖 ML Kit OCR
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var ocrJob: Job? = null

    // 📱 Callbacks
    private var onTextSelectedListener: ((String, SelectionMode) -> Unit)? = null
    private var onOCRCompleteListener: ((List<OCRTextBlock>) -> Unit)? = null

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        if (!isOCRActive) return

        // Desenha todos os blocos de texto detectados
        for (block in ocrTextBlocks) {
            drawTextBlock(canvas, block)
        }

        // Desenha seleções ativas
        selectedWord?.let { drawWordSelection(canvas, it) }
        selectedLine?.let { drawLineSelection(canvas, it) }
        selectedBlock?.let { drawBlockSelection(canvas, it) }
    }

    /**
     * 🎨 Desenha um bloco de texto
     */
    private fun drawTextBlock(canvas: Canvas, block: OCRTextBlock) {
        // Fundo sutil para o bloco
        val bgPaint = Paint().apply {
            color = Color.parseColor("#20FFFFFF")
            isAntiAlias = true
        }
        canvas.drawRoundRect(
            block.bounds.left - TEXT_PADDING,
            block.bounds.top - TEXT_PADDING,
            block.bounds.right + TEXT_PADDING,
            block.bounds.bottom + TEXT_PADDING,
            8f, 8f, bgPaint
        )

        // Bordas do bloco (opcional, para debug)
        if (block == selectedBlock) {
            canvas.drawRoundRect(
                block.bounds.left - TEXT_PADDING,
                block.bounds.top - TEXT_PADDING,
                block.bounds.right + TEXT_PADDING,
                block.bounds.bottom + TEXT_PADDING,
                8f, 8f, boundsPaint
            )
        }

        // Desenha as linhas do texto
        for (line in block.lines) {
            drawTextLine(canvas, line)
        }
    }

    /**
     * 🎨 Desenha uma linha de texto
     */
    private fun drawTextLine(canvas: Canvas, line: OCRTextLine) {
        // Highlight da linha se selecionada
        if (line == selectedLine) {
            canvas.drawRoundRect(
                line.bounds.left - 4f,
                line.bounds.top - 2f,
                line.bounds.right + 4f,
                line.bounds.bottom + 2f,
                4f, 4f, highlightPaint
            )
        }

        // Desenha as palavras
        for (word in line.words) {
            drawWord(canvas, word)
        }
    }

    /**
     * 🎨 Desenha uma palavra
     */
    private fun drawWord(canvas: Canvas, word: OCRWord) {
        // 🎯 Calcula tamanho do texto baseado na altura da palavra
        val originalHeight = word.bounds.height()
        val scaledTextSize = (originalHeight * 0.9f).coerceIn(14f, 40f)
        
        // 🎨 Cria paint específico para esta palavra
        val wordPaint = TextPaint().apply {
            isAntiAlias = true
            textSize = scaledTextSize
            color = Color.WHITE
            typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(3f, 1f, 1f, Color.BLACK)
        }
        
        // Highlight da palavra se selecionada
        if (word == selectedWord) {
            canvas.drawRoundRect(
                word.bounds.left - 2f,
                word.bounds.top - 1f,
                word.bounds.right + 2f,
                word.bounds.bottom + 1f,
                4f, 4f, selectionPaint
            )
        }

        // 📏 Centraliza texto verticalmente na bounding box
        val textHeight = wordPaint.fontMetrics.let { it.bottom - it.top }
        val yPosition = word.bounds.top + (word.bounds.height() + textHeight) / 2 - wordPaint.fontMetrics.bottom

        // Desenha o texto da palavra com tamanho correto
        canvas.drawText(
            word.text,
            word.bounds.left,
            yPosition,
            wordPaint
        )
    }

    /**
     * ✨ Desenha seleção de palavra
     */
    private fun drawWordSelection(canvas: Canvas, word: OCRWord) {
        val selectionRect = RectF(
            word.bounds.left - 4f,
            word.bounds.top - 2f,
            word.bounds.right + 4f,
            word.bounds.bottom + 2f
        )
        
        canvas.drawRoundRect(selectionRect, 6f, 6f, selectionPaint)
        
        // Animação de pulsação (opcional)
        drawSelectionAnimation(canvas, selectionRect)
    }

    /**
     * ✨ Desenha seleção de linha
     */
    private fun drawLineSelection(canvas: Canvas, line: OCRTextLine) {
        val selectionRect = RectF(
            line.bounds.left - 8f,
            line.bounds.top - 4f,
            line.bounds.right + 8f,
            line.bounds.bottom + 4f
        )
        
        canvas.drawRoundRect(selectionRect, 8f, 8f, selectionPaint)
        drawSelectionAnimation(canvas, selectionRect)
    }

    /**
     * ✨ Desenha seleção de bloco
     */
    private fun drawBlockSelection(canvas: Canvas, block: OCRTextBlock) {
        val selectionRect = RectF(
            block.bounds.left - TEXT_PADDING,
            block.bounds.top - TEXT_PADDING,
            block.bounds.right + TEXT_PADDING,
            block.bounds.bottom + TEXT_PADDING
        )
        
        canvas.drawRoundRect(selectionRect, 12f, 12f, selectionPaint)
        drawSelectionAnimation(canvas, selectionRect)
    }

    /**
     * ✨ Desenha animação de seleção
     */
    private fun drawSelectionAnimation(canvas: Canvas, rect: RectF) {
        val animationPaint = Paint(selectionPaint).apply {
            alpha = 50
            strokeWidth = 4f
            style = Paint.Style.STROKE
        }
        
        canvas.drawRoundRect(rect, 8f, 8f, animationPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return if (isOCRActive) {
            gestureDetector.onTouchEvent(event)
        } else {
            false
        }
    }

    /**
     * 👆 Toque simples - seleciona palavra
     */
    private fun handleTap(x: Float, y: Float) {
        val tappedWord = findWordAt(x, y)
        if (tappedWord != null) {
            selectWord(tappedWord)
            selectionMode = SelectionMode.WORD
            animateSelection()
        }
    }

    /**
     * 👆👆 Toque duplo - seleciona linha
     */
    private fun handleDoubleTap(x: Float, y: Float) {
        val tappedLine = findLineAt(x, y)
        if (tappedLine != null) {
            selectLine(tappedLine)
            selectionMode = SelectionMode.LINE
            animateSelection()
        }
    }

    /**
     * 👆📱 Toque longo - seleciona bloco e copia
     */
    private fun handleLongPress(x: Float, y: Float) {
        val tappedBlock = findBlockAt(x, y)
        if (tappedBlock != null) {
            selectBlock(tappedBlock)
            selectionMode = SelectionMode.BLOCK
            animateSelection()
            copySelectedText()
        }
    }

    /**
     * 🔍 Encontra palavra na posição
     */
    private fun findWordAt(x: Float, y: Float): OCRWord? {
        for (block in ocrTextBlocks) {
            for (line in block.lines) {
                for (word in line.words) {
                    if (word.bounds.contains(x, y)) {
                        return word
                    }
                }
            }
        }
        return null
    }

    /**
     * 🔍 Encontra linha na posição
     */
    private fun findLineAt(x: Float, y: Float): OCRTextLine? {
        for (block in ocrTextBlocks) {
            for (line in block.lines) {
                if (line.bounds.contains(x, y)) {
                    return line
                }
            }
        }
        return null
    }

    /**
     * 🔍 Encontra bloco na posição
     */
    private fun findBlockAt(x: Float, y: Float): OCRTextBlock? {
        for (block in ocrTextBlocks) {
            if (block.bounds.contains(x, y)) {
                return block
            }
        }
        return null
    }

    /**
     * ✅ Seleciona uma palavra
     */
    private fun selectWord(word: OCRWord) {
        clearSelection()
        selectedWord = word
        onTextSelectedListener?.invoke(word.text, SelectionMode.WORD)
        invalidate()
    }

    /**
     * ✅ Seleciona uma linha
     */
    private fun selectLine(line: OCRTextLine) {
        clearSelection()
        selectedLine = line
        onTextSelectedListener?.invoke(line.text, SelectionMode.LINE)
        invalidate()
    }

    /**
     * ✅ Seleciona um bloco
     */
    private fun selectBlock(block: OCRTextBlock) {
        clearSelection()
        selectedBlock = block
        onTextSelectedListener?.invoke(block.text, SelectionMode.BLOCK)
        invalidate()
    }

    /**
     * ✨ Animação de seleção
     */
    private fun animateSelection() {
        val scaleAnimator = ObjectAnimator.ofFloat(this, "scaleX", 0.98f, 1.02f, 1f)
        val alphaAnimator = ObjectAnimator.ofFloat(this, "alpha", 0.8f, 1f)
        
        AnimatorSet().apply {
            playTogether(scaleAnimator, alphaAnimator)
            duration = SELECTION_ANIMATION_DURATION
            start()
        }
    }

    /**
     * 📋 Copia texto selecionado
     */
    private fun copySelectedText() {
        val textToCopy = when (selectionMode) {
            SelectionMode.WORD -> selectedWord?.text
            SelectionMode.LINE -> selectedLine?.text
            SelectionMode.BLOCK -> selectedBlock?.text
        }

        if (textToCopy != null) {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("OCR Text", textToCopy)
            clipboard.setPrimaryClip(clip)
            
            // 🔇 Texto copiado silenciosamente
        }
    }

    /**
     * 🧹 Limpa seleção
     */
    private fun clearSelection() {
        selectedWord = null
        selectedLine = null
        selectedBlock = null
    }

    /**
     * 🤖 Executa OCR na área especificada
     */
    fun performOCR(bitmap: Bitmap, region: RectF) {
        ocrJob?.cancel()
        ocrJob = CoroutineScope(Dispatchers.Main).launch {
            try {
                isOCRActive = false
                ocrTextBlocks.clear()
                invalidate()

                // Extrai região da imagem
                val croppedBitmap = cropBitmapToRegion(bitmap, region)
                val inputImage = InputImage.fromBitmap(croppedBitmap, 0)

                withContext(Dispatchers.Default) {
                    // Executa OCR
                    val result = textRecognizer.process(inputImage).await()
                    val processedBlocks = mutableListOf<OCRTextBlock>()

                    for (textBlock in result.textBlocks) {
                        val blockBounds = textBlock.boundingBox?.let { 
                            RectF(it).apply {
                                offset(region.left, region.top) // Ajusta para coordenadas da tela
                            }
                        } ?: continue

                        val lines = mutableListOf<OCRTextLine>()
                        for (line in textBlock.lines) {
                            val lineBounds = line.boundingBox?.let {
                                RectF(it).apply {
                                    offset(region.left, region.top)
                                }
                            } ?: continue

                            val words = mutableListOf<OCRWord>()
                            for (element in line.elements) {
                                val wordBounds = element.boundingBox?.let {
                                    RectF(it).apply {
                                        offset(region.left, region.top)
                                    }
                                } ?: continue

                                words.add(OCRWord(
                                    text = element.text,
                                    bounds = wordBounds,
                                    confidence = 0.9f // Confiança padrão para elementos
                                ))
                            }

                            lines.add(OCRTextLine(
                                text = line.text,
                                bounds = lineBounds,
                                words = words
                            ))
                        }

                        processedBlocks.add(OCRTextBlock(
                            text = textBlock.text,
                            bounds = blockBounds,
                            confidence = 0.8f, // Confiança padrão para blocos
                            lines = lines
                        ))
                    }

                    withContext(Dispatchers.Main) {
                        ocrTextBlocks.clear()
                        ocrTextBlocks.addAll(processedBlocks)
                        isOCRActive = true
                        onOCRCompleteListener?.invoke(processedBlocks)
                        invalidate()
                    }
                }

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Erro no OCR: ${e.message}", e)
                // 🔇 Erro OCR silencioso
            }
        }
    }

    /**
     * ✂️ Recorta bitmap para região específica
     */
    private fun cropBitmapToRegion(bitmap: Bitmap, region: RectF): Bitmap {
        val x = region.left.toInt().coerceAtLeast(0)
        val y = region.top.toInt().coerceAtLeast(0)
        val width = region.width().toInt().coerceAtMost(bitmap.width - x)
        val height = region.height().toInt().coerceAtMost(bitmap.height - y)
        
        return Bitmap.createBitmap(bitmap, x, y, width, height)
    }

    /**
     * 🧹 Limpa OCR
     */
    fun clearOCR() {
        ocrJob?.cancel()
        isOCRActive = false
        ocrTextBlocks.clear()
        clearSelection()
        invalidate()
    }

    /**
     * 📱 Callbacks
     */
    fun setOnTextSelectedListener(listener: (String, SelectionMode) -> Unit) {
        onTextSelectedListener = listener
    }

    fun setOnOCRCompleteListener(listener: (List<OCRTextBlock>) -> Unit) {
        onOCRCompleteListener = listener
    }

    /**
     * 🎯 Getters
     */
    fun isOCRActive(): Boolean = isOCRActive
    fun getDetectedTextBlocks(): List<OCRTextBlock> = ocrTextBlocks.toList()
    fun getSelectedText(): String? {
        return when (selectionMode) {
            SelectionMode.WORD -> selectedWord?.text
            SelectionMode.LINE -> selectedLine?.text
            SelectionMode.BLOCK -> selectedBlock?.text
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ocrJob?.cancel()
        textRecognizer.close()
    }
}

/**
 * 🔄 Extensão para aguardar Task do ML Kit
 */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T {
    return kotlinx.coroutines.suspendCancellableCoroutine { cont ->
        addOnCompleteListener { task ->
            if (task.exception != null) {
                cont.cancel(task.exception!!)
            } else {
                cont.resume(task.result, null)
            }
        }
    }
}
