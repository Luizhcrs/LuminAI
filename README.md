# 🤖 IA Detection - Detecção Inteligente de Imagens com IA

![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)
![Android](https://img.shields.io/badge/platform-Android%2026%2B-green.svg)
![Kotlin](https://img.shields.io/badge/language-Kotlin-orange.svg)

**IA Detection** é um aplicativo Android avançado que combina captura inteligente de tela com análise de IA para detectar imagens geradas artificialmente e realizar seleção inteligente de objetos.

## ✨ **Principais Funcionalidades**

### 🎯 **Detecção de IA Avançada**
- **API SightEngine**: Detecta se imagens foram geradas por Inteligência Artificial
- **Análise de Confiança**: Percentual preciso de probabilidade de IA
- **Feedback Visual**: Interface elegante com resultados detalhados

### 🖼️ **Seleção Inteligente** 
- **Circle to Search**: Desenho livre que se completa automaticamente
- **Detecção de Objetos**: Identifica textos, imagens e botões
- **OCR Ao Vivo**: Reconhecimento de texto em tempo real
- **Smart Rectangle**: Conversão automática de formas livres para retângulos perfeitos

### 📱 **Integração Nativa**
- **Share Intent**: Recebe imagens de outros apps via compartilhamento
- **Botão Flutuante**: Acesso rápido em qualquer lugar do sistema
- **Interface Moderna**: Design Material seguindo padrões do Android

## 🔧 **Tecnologias Utilizadas**

### **🤖 Inteligência Artificial**
- **SightEngine API**: Detecção de imagens geradas por IA
- **TensorFlow Lite**: Reconhecimento de objetos on-device
- **ML Kit**: OCR e análise de texto avançada
- **Algoritmos Nativos**: Processamento de imagem otimizado

### **📱 Android Nativo**
- **Kotlin**: Linguagem principal
- **Coroutines**: Processamento assíncrono
- **Material Design**: Interface moderna
- **System Overlay**: Botão flutuante global

## 📱 **Requisitos do Sistema**

- **Android 8.0 (API 26)** ou superior
- **4GB RAM** recomendado para processamento de IA
- **Conexão com Internet** para detecção de IA
- **Permissão de Sobreposição** para botão flutuante

## 🚀 **Instalação**

Para instalação detalhada, consulte o [**Guia de Instalação**](INSTALLATION_GUIDE.md).

### **🛠️ Compilação Rápida**
```bash
# Clone o repositório
git clone https://github.com/seu-usuario/IA-Detection.git

# Abra no Android Studio
cd IA-Detection

# Compile e instale
./gradlew installDebug
```

## 📖 **Como Usar**

### **🎯 1. Configuração Inicial**
1. **Abra o app** e conceda as permissões
2. **Ative o botão flutuante** tocando em "Iniciar Serviço"

### **🖼️ 2. Analisando Imagens**
1. **Compartilhe uma imagem** de qualquer app
2. **Selecione "IA Detection"** na lista
3. **Desenhe livremente** ao redor da área desejada
4. **Toque no botão 🤖** para detectar se é IA
5. **Veja os resultados** no diálogo elegante

## 🏗️ **Arquitetura**

```
📱 IA-Detection v1.0.0
├── 🤖 AI Detection (SightEngine API)
├── 🎨 Smart Selection (Circle to Search)
├── 📝 OCR Live (ML Kit)
├── 🧠 Object Detection (TensorFlow)
├── 🔵 Floating Button (System Overlay)
└── 📱 Modern UI (Material Design)
```

## 📚 **Documentação**

- 📋 [**Changelog**](CHANGELOG.md) - Histórico de versões
- 🔧 [**API Documentation**](API_DOCUMENTATION.md) - Documentação técnica
- 🚀 [**Installation Guide**](INSTALLATION_GUIDE.md) - Guia de instalação
- 🏗️ [**Project Structure**](PROJECT_STRUCTURE.md) - Estrutura do projeto

## 🔑 **Configuração da API**

Para usar a detecção de IA, configure sua chave da **SightEngine**:

```kotlin
// Em AIDetectionService.kt
companion object {
    private const val API_USER = "sua_api_user"
    private const val API_SECRET = "sua_api_secret"
}
```

**Obtenha gratuitamente em**: [SightEngine.com](https://sightengine.com)

## 🚨 **Permissões Necessárias**

| Permissão | Finalidade | Status |
|-----------|------------|--------|
| `SYSTEM_ALERT_WINDOW` | Botão flutuante | ✅ Obrigatória |
| `INTERNET` | API de IA | ✅ Obrigatória |
| `FOREGROUND_SERVICE` | Serviço ativo | ✅ Obrigatória |
| `READ_EXTERNAL_STORAGE` | Imagens | ✅ Obrigatória |

## 🐛 **Solução de Problemas**

### **❌ Botão flutuante não aparece**
- Verifique permissão de sobreposição
- Reinicie o app após conceder permissões

### **🤖 Detecção de IA falha**
- Verifique conexão com internet
- Configure API key da SightEngine

### **📱 App trava**
- Limpe cache nas configurações
- Verifique memória disponível

## 🤝 **Contribuindo**

1. **Fork** o repositório
2. **Crie** uma branch (`git checkout -b feature/nova-funcionalidade`)
3. **Commit** (`git commit -am 'Adiciona nova funcionalidade'`)
4. **Push** (`git push origin feature/nova-funcionalidade`)
5. **Abra** um Pull Request

## 📄 **Licença**

Este projeto está licenciado sob a **MIT License** - veja [LICENSE](LICENSE).

## 👨‍💻 **Autor**

**Desenvolvido com ❤️ para detectar IA em imagens**

- 📧 Email: contato@iadetection.com
- 🐙 GitHub: [@IA-Detection](https://github.com/IA-Detection)

---

## 📊 **Estatísticas v1.0.0**

- **20+ Classes Kotlin** organizadas
- **5+ Componentes UI** personalizados
- **4+ APIs de IA** integradas
- **100% Thread-Safe** garantido
- **Material Design 3** completo

---

**⭐ Se este projeto foi útil, considere dar uma estrela!**

*Última atualização: Janeiro 2025 - Release v1.0.0*
