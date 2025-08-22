# Botão Flutuante Android

Este projeto demonstra como criar um botão flutuante que fica visível em todas as aplicações do Android, usando janelas de sobreposição do sistema (system overlay windows).

## 🚀 Funcionalidades

- **Botão flutuante persistente**: Visível em todas as aplicações
- **Arrastável**: Pode ser movido pela tela
- **Clique funcional**: Executa ações quando tocado
- **Serviço em primeiro plano**: Continua funcionando em segundo plano
- **Interface intuitiva**: Controles simples para iniciar/parar o serviço

## 📱 Requisitos

- Android 6.0 (API 24) ou superior
- Permissão `SYSTEM_ALERT_WINDOW` concedida pelo usuário
- Dispositivo Android para teste

## 🔧 Configuração

### 1. Abrir o Projeto no Android Studio

1. Abra o Android Studio
2. Selecione "Open an existing project"
3. Navegue até a pasta do projeto e selecione-a
4. Aguarde a sincronização do Gradle

### 2. Conectar Dispositivo Android

1. Ative a "Depuração USB" no seu dispositivo Android:
   - Configurações > Sobre o telefone > Toque 7 vezes no "Número da versão"
   - Configurações > Opções do desenvolvedor > Depuração USB
2. Conecte o dispositivo via USB
3. Autorize a depuração quando solicitado

### 3. Executar o Aplicativo

1. Clique no botão "Run" (▶️) no Android Studio
2. Selecione seu dispositivo Android
3. Aguarde a instalação e execução

## 📋 Como Usar

### Primeira Execução

1. **Abrir o app**: O app será instalado e aberto automaticamente
2. **Verificar permissão**: Clique em "Verificar Permissão"
3. **Conceder permissão**: Se necessário, o app abrirá as configurações do sistema
4. **Habilitar sobreposição**: Ative a permissão "Permitir sobreposição" para o app

### Uso Diário

1. **Iniciar serviço**: Clique em "Iniciar Botão Flutuante"
2. **Botão aparece**: Um botão flutuante roxo aparecerá na tela
3. **Mover botão**: Arraste o botão para qualquer posição
4. **Usar botão**: Toque no botão para executar ações
5. **Parar serviço**: Clique em "Parar Botão Flutuante" quando não precisar

## 🏗️ Arquitetura do Projeto

### Estrutura de Arquivos

```
app/src/main/
├── java/com/example/floatingbutton/
│   ├── MainActivity.kt          # Activity principal
│   └── FloatingButtonService.kt # Serviço do botão flutuante
├── res/
│   ├── layout/
│   │   ├── activity_main.xml           # Layout da tela principal
│   │   └── floating_button_layout.xml  # Layout do botão flutuante
│   ├── drawable/                       # Ícones e backgrounds
│   ├── values/                         # Cores, temas e strings
│   └── xml/                           # Regras de backup
└── AndroidManifest.xml                 # Configurações do app
```

### Componentes Principais

#### MainActivity
- Gerencia permissões do sistema
- Controla início/parada do serviço
- Interface do usuário

#### FloatingButtonService
- Serviço em primeiro plano
- Cria e gerencia a janela flutuante
- Implementa funcionalidade de arrastar

## 🔒 Permissões

### SYSTEM_ALERT_WINDOW
- **O que faz**: Permite criar janelas sobre outras aplicações
- **Por que é necessária**: Essencial para o botão flutuante funcionar
- **Como conceder**: Configurações > Apps > [Nome do App] > Permissões > Sobreposição

### FOREGROUND_SERVICE
- **O que faz**: Permite executar serviços em primeiro plano
- **Por que é necessária**: Mantém o botão funcionando em segundo plano

## ⚠️ Considerações Importantes

### Segurança
- A permissão `SYSTEM_ALERT_WINDOW` é considerada de alto risco
- Use apenas para funcionalidades essenciais do app
- Explique claramente ao usuário por que é necessária

### Performance
- O serviço consome recursos do sistema
- Considere parar o serviço quando não for necessário
- Monitore o uso de bateria

### Compatibilidade
- Funciona em Android 6.0+
- Comportamento pode variar entre fabricantes
- Teste em diferentes dispositivos

## 🐛 Solução de Problemas

### Botão não aparece
1. Verifique se a permissão foi concedida
2. Reinicie o app
3. Verifique se o serviço está rodando

### Botão desaparece
1. O sistema pode ter parado o serviço
2. Reinicie o serviço
3. Verifique configurações de bateria

### Erro de permissão
1. Vá para Configurações > Apps > [Nome do App]
2. Verifique se "Sobreposição" está ativada
3. Reinicie o app

## 🚀 Próximos Passos

### Melhorias Possíveis
- [ ] Adicionar animações ao botão
- [ ] Implementar menu de opções
- [ ] Salvar posição preferida do usuário
- [ ] Adicionar diferentes estilos de botão
- [ ] Implementar atalhos personalizados

### Funcionalidades Avançadas
- [ ] Integração com outras apps
- [ ] Configurações avançadas
- [ ] Temas personalizáveis
- [ ] Backup de configurações

## 📚 Recursos Adicionais

- [Documentação Android - System Overlay Windows](https://developer.android.com/guide/topics/ui/windows)
- [Android Developers - Services](https://developer.android.com/guide/components/services)
- [Material Design Guidelines](https://material.io/design)

## 📄 Licença

Este projeto é fornecido como exemplo educacional. Sinta-se livre para usar, modificar e distribuir conforme suas necessidades.

---

**Desenvolvido com ❤️ para demonstrar funcionalidades avançadas do Android**
