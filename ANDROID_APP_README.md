# Desktop Env — ambiente desktop para Android

Aplicativo Android próprio (Kotlin + Jetpack Compose) que transforma a tela do celular em um
ambiente de área de trabalho: papel de parede, barra de tarefas, menu Iniciar, gaveta de apps,
janelas internas, configurações rápidas, calendário, atalhos de teclado e suporte a mouse.

> Implementação independente. Não usa código, assets, marcas nem componentes do Android-Dex original.

## Requisitos

| Item | Valor |
|---|---|
| Android mínimo | 12 (API 31) |
| Android alvo | 15 (API 35) |
| Linguagem | Kotlin 2.0.21 |
| UI | Jetpack Compose (BOM 2024.12.01), Material 3 |
| Persistência | DataStore Preferences |
| Build | Android Gradle Plugin 8.7.3, Gradle 8.10.2, JDK 17 |

## Funcionalidades

**Área de trabalho**
- Papel de parede com 4 opções (gradientes originais), troca pelo menu de contexto ou pelas configurações.
- Ícones de janelas internas e de apps fixados. Toque longo em um app fixado o remove.
- Menu de contexto: pressão longa em área vazia ou clique direito com mouse.
- Temas claro, escuro e automático (segue o sistema). Persistido.

**Barra de tarefas**
- Botão Iniciar, lista de janelas abertas (toque alterna entre mostrar, focar e minimizar).
- Indicadores de rede e bateria, relógio e data. Toque abre as configurações rápidas.

**Menu Iniciar**
- Busca de apps instalados, janelas internas, toque abre o app, toque longo fixa na área de trabalho.
- Ação para fechar todas as janelas.

**Janelas internas** (Configurações, Calendário, Recentes)
- Arrastar pela barra de título (sempre mantendo a barra visível na área).
- Redimensionar pelo canto inferior direito, com tamanho mínimo.
- Minimizar, fechar e trazer ao topo ao tocar.
- Uma instância por tipo: abrir de novo apenas foca a existente.

**Teclado e mouse**
| Atalho | Ação |
|---|---|
| Alt + Tab | Alterna entre janelas internas |
| Ctrl + W | Fecha a janela do topo |
| Ctrl + M | Minimiza a janela do topo |
| Esc | Fecha menus |
| Tecla Windows / Meta | Abre ou fecha o menu Iniciar |
| Clique direito | Menu de contexto |

**Layout**
- Grade de ícones adaptável ao tamanho da tela (`GridCells.Adaptive`).
- Janelas e posições recalculadas a partir da área real medida em `onSizeChanged`.
- Activity com `resizeableActivity` e `configChanges` para não recriar a tela ao girar.

**Estado**
- Preferências (tema, papel de parede, apps fixados, recentes) salvas com DataStore e restauradas ao reabrir.
- Janelas internas são efêmeras; ao fechar o processo elas são perdidas, por decisão de escopo.

## Limitações do Android 12 (documentadas, não contornadas)

- **Não há janelas flutuantes livres de apps de terceiros.** Apps externos abrem pelo sistema (tela cheia ou multijanela, conforme o aparelho e o app). O Desktop Env não embute nem redimensiona esses apps.
- **Não há lista confiável de apps em execução.** A barra de tarefas mostra apenas as janelas internas do próprio ambiente.
- **Gaveta de apps** depende de `QUERY_ALL_PACKAGES`. Esta permissão é restrita pelo Google Play e o APK deve ser distribuído diretamente, o que é o escopo deste projeto.
- **Launcher padrão**: a categoria `HOME` está declarada, então o Android pode oferecer o app como launcher. Não é forçado.
- **Widgets, gerenciador de arquivos e central de notificações** ficaram fora desta versão. Notificações exigiriam `NotificationListenerService` e acesso concedido pelo usuário; widgets exigiriam hospedagem via `AppWidgetHost`. Não foram implementados para não entregar código não testado.
- **Textos da interface** estão em português diretamente no código, sem `strings.xml` localizado.

## Estrutura

```
app/src/main/java/com/example/desktopenv/
├── MainActivity.kt                 Activity, edge-to-edge, abertura de apps externos
├── model/
│   ├── WindowLogic.kt              Estado e regras das janelas (puro Kotlin, testável)
│   └── CalendarGrid.kt             Grade mensal (puro Kotlin, testável)
├── data/
│   ├── DesktopPrefs.kt             DataStore: tema, papel de parede, fixados, recentes
│   ├── AppCatalog.kt               Lista de apps e ícones via PackageManager
│   └── SystemStatus.kt             Bateria (broadcast) e rede (NetworkCallback)
├── viewmodel/
│   └── DesktopViewModel.kt         Estado exposto como StateFlow, ações do desktop
└── ui/
    ├── DesktopScreen.kt            Tela principal, atalhos, menu de contexto
    ├── Taskbar.kt                  Barra de tarefas
    ├── StartMenu.kt                Menu Iniciar com busca e grade de apps
    ├── QuickSettingsPanel.kt       Configurações rápidas
    ├── Windows.kt                  Moldura de janela e conteúdo das janelas internas
    ├── Components.kt               Componentes compartilhados
    └── theme/Theme.kt              Esquemas claro/escuro, formas, papéis de parede
```

Testes unitários: `app/src/test/java/com/example/desktopenv/model/`.

## Como compilar

### Pelo GitHub Actions (recomendado)
1. Envie a branch `feature/android-desktop-apk` ao repositório.
2. O workflow `.github/workflows/android.yml` roda os testes e gera o APK debug.
3. Baixe o artefato `desktop-env-apk` na aba **Actions** do run. Ele contém os APKs e o `SHA256SUMS.txt`.

### Localmente
Requer JDK 17 e Android SDK com a plataforma 35.
```bash
# Uma vez, para criar o wrapper (requer Gradle 8.10.2 instalado):
gradle wrapper --gradle-version 8.10.2
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## Release assinado (opcional)

Nenhuma chave é incluída no repositório. Para gerar o release pelo Actions, crie um keystore e cadastre
os secrets abaixo em *Settings → Secrets and variables → Actions*:

| Secret | Conteúdo |
|---|---|
| `KEYSTORE_BASE64` | Keystore codificado em base64 (`base64 -w0 release.jks`) |
| `KEYSTORE_PASSWORD` | Senha do keystore |
| `KEY_ALIAS` | Alias da chave |
| `KEY_PASSWORD` | Senha da chave |

Sem esses secrets, o workflow gera apenas o debug. Nunca comite o `.jks`.

## Instalar no Android 12

1. Copie o `app-debug.apk` (ou o release) para o celular ou baixe pelo artefato.
2. Em **Configurações → Segurança** (ou no caminho equivalente do fabricante), permita a instalação do app de origem usada (navegador, gerenciador de arquivos).
3. Abra o arquivo APK e confirme a instalação.
4. Abra **Desktop Env**. A área de trabalho aparece com o menu Iniciar na barra inferior.
5. Opcional: para usar como launcher padrão, pressione o botão Home e escolha **Desktop Env** nas opções do sistema.

Para desinstalar, use o gerenciamento de apps normal. O app não tem serviço em segundo plano nem envia dados.

## Privacidade

- Nenhuma rede é usada além da leitura de estado (`ACCESS_NETWORK_STATE`).
- Nenhuma telemetria, nenhum login, nenhum servidor.
- Preferências ficam apenas no armazenamento do próprio app.

## Checklist de validação física (Android 12)

Ainda não executado em aparelho. Verificar manualmente:

- [ ] Instalação do APK debug e do release.
- [ ] Abertura do app e exibição da área de trabalho.
- [ ] Lista de apps no menu Iniciar com ícones.
- [ ] Abrir um app externo e voltar ao Desktop Env.
- [ ] Arrastar, redimensionar, minimizar e fechar janelas internas por toque.
- [ ] Alt+Tab, Ctrl+W, Ctrl+M, Esc e tecla Windows com teclado físico ou Bluetooth.
- [ ] Clique direito com mouse USB ou Bluetooth.
- [ ] Girar a tela sem perder janelas nem preferências.
- [ ] Troca de tema e de papel de parede, e persistência após fechar o app.
- [ ] Indicadores de bateria e rede atualizando ao mudar o estado.
- [ ] Comportamento em tela dividida e em telas pequenas e grandes.
- [ ] Permissão `QUERY_ALL_PACKAGES` aceita em todos os fabricantes testados.

## Licença

Defina a licença antes de distribuir. Este repositório ainda não inclui arquivo `LICENSE`.
