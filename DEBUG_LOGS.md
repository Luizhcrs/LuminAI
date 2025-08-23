# 🔍 Debug com Logs - Como Identificar Problemas

## 📱 **Como Ver os Logs:**

### **1. No Android Studio:**
```
View > Tool Windows > Logcat
```

### **2. Filtrar Logs:**
- **Package Name**: `com.example.floatingbutton`
- **Log Level**: `Debug` ou `Verbose`
- **Search**: `FloatingButtonService` ou `MainActivity`

## 🔍 **Logs Importantes para Debug:**

### **MainActivity:**
```
MainActivity: onCreate: Iniciando MainActivity...
MainActivity: onCreate: Layout configurado com sucesso
MainActivity: onCreate: UI configurada
MainActivity: onCreate: Permissão verificada
MainActivity: startFloatingButtonService: Iniciando serviço...
MainActivity: startFloatingButtonService: Permissão confirmada
MainActivity: startFloatingButtonService: Intent criado
MainActivity: startFloatingButtonService: Iniciando foreground service...
MainActivity: startFloatingButtonService: Serviço iniciado com sucesso
```

### **FloatingButtonService:**
```
FloatingButtonService: onCreate: Iniciando serviço...
FloatingButtonService: onCreate: Canal de notificação criado
FloatingButtonService: onCreate: Serviço em foreground iniciado
FloatingButtonService: setupFloatingButton: Iniciando configuração do botão...
FloatingButtonService: setupFloatingButton: Verificando permissão de sobreposição...
FloatingButtonService: setupFloatingButton: Permissão de sobreposição confirmada
FloatingButtonService: setupFloatingButton: Inflando layout...
FloatingButtonService: setupFloatingButton: Layout inflado com sucesso
FloatingButtonService: setupFloatingButton: Configurando parâmetros da janela...
FloatingButtonService: setupFloatingButton: Tipo de janela definido: 2038
FloatingButtonService: setupFloatingButton: Parâmetros da janela configurados
FloatingButtonService: setupFloatingButton: Obtendo WindowManager...
FloatingButtonService: setupFloatingButton: WindowManager obtido com sucesso
FloatingButtonService: setupFloatingButton: Adicionando view à janela...
FloatingButtonService: setupFloatingButton: View adicionada à janela com sucesso
FloatingButtonService: setupFloatingButton: Configurando comportamento do botão...
FloatingButtonService: setupFloatingButton: Comportamento configurado com sucesso
FloatingButtonService: setupFloatingButton: Botão flutuante criado com sucesso!
```

## 🚨 **Possíveis Problemas e Logs:**

### **1. App Travando no onCreate:**
```
MainActivity: onCreate: Iniciando MainActivity...
[CRASH - Sem mais logs]
```
**Solução**: Problema no layout ou binding

### **2. App Travando ao Iniciar Serviço:**
```
MainActivity: startFloatingButtonService: Iniciando serviço...
MainActivity: startFloatingButtonService: Permissão confirmada
MainActivity: startFloatingButtonService: Intent criado
[CRASH - Sem mais logs]
```
**Solução**: Problema no Intent ou Service

### **3. Serviço Não Inicia:**
```
FloatingButtonService: onCreate: Iniciando serviço...
[CRASH - Sem mais logs]
```
**Solução**: Problema no Service.onCreate()

### **4. Botão Não Aparece:**
```
FloatingButtonService: setupFloatingButton: Iniciando configuração do botão...
FloatingButtonService: setupFloatingButton: Verificando permissão de sobreposição...
FloatingButtonService: setupFloatingButton: Permissão de sobreposição confirmada
FloatingButtonService: setupFloatingButton: Inflando layout...
[CRASH - Sem mais logs]
```
**Solução**: Problema no layout ou inflater

### **5. Erro ao Adicionar View:**
```
FloatingButtonService: setupFloatingButton: Adicionando view à janela...
FloatingButtonService: setupFloatingButton: Erro ao adicionar botão à tela: [ERRO]
```
**Solução**: Problema de permissão ou WindowManager

## 🛠️ **Como Testar:**

### **Passo 1: Executar App**
1. Conecte dispositivo Android 15
2. Clique em "Run" (▶️)
3. Abra Logcat no Android Studio

### **Passo 2: Verificar Logs**
1. **Filtre por**: `com.example.floatingbutton`
2. **Procure por**: Logs de MainActivity
3. **Clique em**: "Iniciar Botão Flutuante"
4. **Observe**: Logs de FloatingButtonService

### **Passo 3: Identificar Problema**
- **Se parar em MainActivity**: Problema na Activity
- **Se parar no Service**: Problema no Service
- **Se parar no setupFloatingButton**: Problema na criação do botão
- **Se parar no addView**: Problema de permissão ou WindowManager

## 🔧 **Soluções Comuns:**

### **Problema de Layout:**
- Verificar se `floating_button_layout.xml` existe
- Verificar se `ic_floating_button` existe
- Verificar se ViewBinding está funcionando

### **Problema de Permissão:**
- Verificar se `SYSTEM_ALERT_WINDOW` está ativa
- Verificar se usuário concedeu permissão
- Verificar se permissão não foi revogada

### **Problema de WindowManager:**
- Verificar se `TYPE_APPLICATION_OVERLAY` é suportado
- Verificar se dispositivo é Android 8.0+
- Verificar se não há conflito com outros overlays

## 📋 **Comandos Úteis no Logcat:**

### **Filtrar por Tag:**
```
tag:FloatingButtonService
tag:MainActivity
```

### **Filtrar por Nível:**
```
level:ERROR
level:WARN
level:DEBUG
```

### **Buscar por Texto:**
```
setupFloatingButton
startFloatingButtonService
```

---

**Status**: 🔍 **DEBUG ATIVADO** - Use os logs para identificar problemas!
