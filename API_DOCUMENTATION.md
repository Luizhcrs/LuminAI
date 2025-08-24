# 📚 API Documentation - IA Detection v1.0.0

Esta documentação detalha as APIs internas e estrutura técnica do projeto IA Detection.

## 🏗️ **Arquitetura Geral**

O projeto segue uma arquitetura modular baseada em **MVVM** com separação clara de responsabilidades:

```
📱 Presentation Layer (UI)
├── Activities (MainActivity, UltimateImageViewerActivity)
├── Custom Views (BeautifulDrawingView, FloatingActionMenu)
└── Dialogs (AIResultsDialog)

🧠 Business Logic Layer
├── AI Services (SmartSelectionEngine, AIDetectionService)
├── Image Processing (OpenCVObjectDetector, SemanticSegmentationEngine)
└── Cache Management (AICache)

🔧 System Layer
├── Android Services (FloatingButtonService)
├── File Management (FileProvider)
└── Permission Handling
```

## 🤖 **AI Detection Module**

### **AIDetectionService.kt**

Serviço principal para detecção de imagens geradas por IA usando a SightEngine API.

#### **Métodos Principais**

```kotlin
suspend fun detectAIGenerated(
    bitmap: Bitmap,
    onProgress: (String) -> Unit = {}
): Result<AIDetectionResult>
```

**Parâmetros:**
- `bitmap`: Imagem a ser analisada
- `onProgress`: Callback para feedback de progresso (thread-safe)

**Retorno:**
- `Result<AIDetectionResult>`: Resultado encapsulado com tratamento de erro

**Exemplo de Uso:**
```kotlin
val aiService = AIDetectionService(context)
val result = aiService.detectAIGenerated(bitmap) { progress ->
    // Atualização de progresso na UI
    showToast(progress)
}

result.fold(
    onSuccess = { aiResult ->
        // Processar resultado
        val confidence = aiResult.confidencePercentage
        val isAI = aiResult.isAIGenerated
    },
    onFailure = { error ->
        // Tratar erro
        Log.e("AI", "Erro na detecção: ${error.message}")
    }
)
```

#### **Modelo de Dados**

```kotlin
data class AIDetectionResult(
    val isAIGenerated: Boolean,
    val confidencePercentage: Int,
    val analysisDetails: String,
    val recommendationText: String,
    val emoji: String
)
```

### **SmartSelectionEngine.kt**

Orquestrador que combina múltiplas tecnologias de IA para seleção inteligente.

#### **Métodos Principais**

```kotlin
suspend fun analyzeRegion(
    bitmap: Bitmap,
    region: Rect
): SmartSelectionResult

suspend fun detectObjects(bitmap: Bitmap): List<DetectedObject>

suspend fun recognizeText(bitmap: Bitmap): TextRecognitionResult?
```

#### **Tecnologias Integradas**

1. **ML Kit Text Recognition**
2. **TensorFlow Lite Object Detection** 
3. **Native Image Processing Algorithms**
4. **Semantic Segmentation**

## 🎨 **UI Components**

### **BeautifulDrawingView.kt**

View customizada para desenho suave com efeitos visuais.

#### **Características Técnicas**

```kotlin
class BeautifulDrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr)
```

**Funcionalidades:**
- Desenho suave com interpolação
- Efeitos de glow e sombra
- Path optimization para performance
- Touch handling responsivo

### **FloatingActionMenu.kt**

Menu flutuante animado com múltiplas ações.

#### **Ações Disponíveis**

```kotlin
enum class MenuAction {
    AI_SCAN,      // 🤖 Detectar IA
    OCR_TEXT,     // 📝 Reconhecer Texto  
    SAVE_IMAGE,   // 💾 Salvar Imagem
    SHARE_RESULT  // 📤 Compartilhar
}
```

### **AIResultsDialog.kt**

Diálogo elegante para exibição de resultados de IA.

#### **Configuração**

```kotlin
fun show(
    context: Context,
    result: AIDetectionResult,
    onAction: (String) -> Unit = {}
)
```

## 🔧 **System Services**

### **FloatingButtonService.kt**

Serviço em foreground que mantém o botão flutuante ativo.

#### **Características**

- **Foreground Service** com notificação persistente
- **System Overlay** para acesso global
- **Touch handling** com drag & drop
- **Lifecycle management** robusto

#### **Configuração no Manifest**

```xml
<service
    android:name=".FloatingButtonService"
    android:enabled="true"
    android:exported="false"
    android:foregroundServiceType="mediaProjection" />
```

## 💾 **Cache System**

### **AICache.kt**

Sistema de cache LRU otimizado para resultados de IA.

#### **Implementação**

```kotlin
class AICache(maxSize: Int = 50) {
    private val bitmapCache: LruCache<String, Bitmap>
    private val analysisCache: LruCache<String, SmartSelectionResult>
    
    fun cacheBitmap(key: String, bitmap: Bitmap)
    fun getCachedBitmap(key: String): Bitmap?
    fun cacheAnalysis(key: String, result: SmartSelectionResult)
    fun getCachedAnalysis(key: String): SmartSelectionResult?
}
```

## 🔒 **Permission Management**

### **Permissões Críticas**

```kotlin
// Verificação de permissão de overlay
if (!Settings.canDrawOverlays(context)) {
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
    startActivity(intent)
}

// Verificação de permissões de arquivo
if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) 
    != PackageManager.PERMISSION_GRANTED) {
    ActivityCompat.requestPermissions(this, arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE
    ), REQUEST_CODE)
}
```

## 🌐 **Network Layer**

### **HTTP Client Configuration**

```kotlin
private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
```

### **API Request Format**

```kotlin
// Multipart request para SightEngine
val requestBody = MultipartBody.Builder()
    .setType(MultipartBody.FORM)
    .addFormDataPart("media", "image.jpg", 
        imageBytes.toRequestBody("image/jpeg".toMediaType()))
    .addFormDataPart("models", "genai")
    .addFormDataPart("api_user", API_USER)
    .addFormDataPart("api_secret", API_SECRET)
    .build()
```

## 📱 **Activity Lifecycle**

### **UltimateImageViewerActivity.kt**

Atividade principal com gerenciamento completo de lifecycle.

#### **Estados Importantes**

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    // Configuração inicial
    setupUI()
    initializeAI()
    handleSharedImage()
}

override fun onDestroy() {
    super.onDestroy()
    // Limpeza de recursos
    aiAnalysisJob?.cancel()
    smartSelectionEngine.cleanup()
}
```

## 🔧 **Threading Model**

### **Coroutines Usage**

```kotlin
// Main Thread para UI
CoroutineScope(Dispatchers.Main).launch {
    // Atualização de UI
}

// Background Thread para processamento
withContext(Dispatchers.IO) {
    // Processamento pesado de IA
}

// Handler para callbacks thread-safe
private val mainHandler = Handler(Looper.getMainLooper())
```

## 📊 **Performance Considerations**

### **Memory Management**

1. **Bitmap Recycling**: Liberação explícita de bitmaps grandes
2. **LRU Cache**: Cache inteligente com limite de memória
3. **Lazy Loading**: Inicialização sob demanda de componentes pesados

### **Battery Optimization**

1. **Foreground Service**: Apenas quando necessário
2. **Network Batching**: Agrupamento de requisições
3. **CPU Throttling**: Processamento adaptativo baseado na bateria

## 🛡️ **Error Handling**

### **Exception Hierarchy**

```kotlin
sealed class AIDetectionException : Exception() {
    object NetworkError : AIDetectionException()
    object InvalidImage : AIDetectionException()
    object APILimitReached : AIDetectionException()
    data class UnknownError(override val message: String) : AIDetectionException()
}
```

### **Graceful Degradation**

```kotlin
try {
    val result = aiDetectionService.detectAIGenerated(bitmap)
    // Processo normal
} catch (e: NetworkError) {
    // Fallback para análise offline
    showOfflineMode()
} catch (e: Exception) {
    // Log error e continuar com funcionalidades básicas
    Log.e("AI", "Erro não crítico", e)
    showBasicImageViewer()
}
```

## 🔍 **Testing Strategy**

### **Unit Tests**

- AIDetectionService logic
- Cache functionality
- Image processing algorithms

### **Integration Tests**

- UI component interaction
- Service lifecycle
- Permission flows

### **Performance Tests**

- Memory usage under load
- Battery consumption
- Network efficiency

---

## 📝 **Development Guidelines**

### **Code Style**

- **Kotlin Coding Conventions**
- **Material Design Guidelines**
- **Android Architecture Components**

### **Commit Message Format**

```
🤖 feat: adiciona detecção de IA com SightEngine
🎨 ui: melhora interface do menu flutuante  
🐛 fix: corrige crash em thread de UI
📚 docs: atualiza documentação da API
🔧 refactor: otimiza cache de bitmaps
✅ test: adiciona testes para AIDetectionService
```

---

*Esta documentação é mantida atualizada a cada release. Para dúvidas técnicas, consulte o código fonte ou abra uma issue.*
