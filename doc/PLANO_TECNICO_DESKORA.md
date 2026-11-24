# Plano técnico: Deskora (ambiente desktop para Android)

Nome provisório. O projeto mantém `rootProject.name`, `namespace` e `applicationId`
(`com.example.desktopenv`) até aprovação de uma renomeação com proposta explícita.

## 1. Restrições reais do Android

Estas restrições definem o escopo. Nenhuma funcionalidade abaixo pode depender de algo fora desta lista.

| Desejo | Situação no Android | Abordagem adotada |
|---|---|---|
| Controlar janelas de outros apps | Não permitido a apps comuns | Janelas são internas ao Deskora. Apps externos abrem como o sistema decide. |
| Ler notificações de outros apps | Só com `NotificationListenerService` concedido pelo usuário em Ajustes | Permissão explicada na tela; processamento local; histórico só em memória |
| Controlar mídia de outros apps | `MediaSessionManager` exige o mesmo listener | Mesma permissão; controles play/pause/próxima via `MediaController` |
| Volume do sistema | `AudioManager.adjustStreamVolume` é permitido | Usado para STREAM_MUSIC |
| Ligar/desligar Wi-Fi, Bluetooth, brilho | Bloqueado para apps comuns | Atalhos para os painéis e telas nativas de Ajustes |
| Acesso a arquivos | Scoped storage e Storage Access Framework | Só pastas que o usuário autoriza com `OPEN_DOCUMENT_TREE` |
| Teclado e mouse | Eventos chegam ao app quando ele tem foco | Atalhos internos. Combinações reservadas pelo sistema (ex.: Alt+Tab global) não são prometidas |
| Modo desktop nativo / multijanela | Depende do fabricante e da versão | Não é implementado nem prometido. O layout se adapta ao tamanho da janela do app |

## 2. Arquitetura

- `model/` — lógica pura, testável na JVM, sem `android.*`:
  - `WindowLogic` (janelas, maximizar, encaixar, ordem Z, foco)
  - `WindowLayoutCodec` (persistência do layout)
  - `NotificationHistory` (histórico limitado e contagem de não lidas)
  - `SearchIndex` (busca de apps e janelas)
  - `ShortcutMap` (tecla → ação, a partir de keycodes Android)
  - `FileRules` (nomes válidos e ordenação de pastas)
- `data/` — persistência DataStore e acesso ao sistema:
  - `DesktopPrefs` (tema, papel de parede, fixados, recentes, layout, pastas autorizadas, preferências de UI)
  - `SystemStatus` (bateria e rede, existente)
  - `NotificationStore` e `DeskoraNotificationListener` (Fase 3)
  - `MediaController` (Fase 4)
  - `FileRepository` (SAF, Fase 5)
- `viewmodel/DesktopViewModel` — estado de UI e ações. Chama a lógica pura e persiste.
- `ui/` — Compose. Cada janela interna tem seu corpo em `ui/windows/`.

## 3. Persistência

- Layout de janelas: string codificada por linha (`kind|id|x|y|w|h|z|min|max|rx|ry|rw|rh|payload`), payload em URL-encoding. Linhas inválidas são descartadas, sem derrubar o app.
- Pastas autorizadas: lista de URIs persistentes (`takePersistableUriPermission`). Ao remover, `releasePersistableUriPermission`.
- Notificações: **não** são persistidas em disco. Motivo: conteúdo de mensagens pessoais não deve ficar gravado sem necessidade. O histórico existe enquanto o processo estiver vivo.

## 4. Testes

- Unitários (JVM): lógica pura de cada fase.
- Instrumentados (emulador x86_64 na CI): fluxo principal, ciclo de vida (recriar Activity com janelas abertas), persistência do layout, telas em tamanhos diferentes.
- Android Lint: já executado na CI, sem erros.
- Testes em aparelho físico: pendentes. Ficam registrados em `doc/PENDENCIAS_TESTE_FISICO.md`.

## 5. Ordem de execução e commits

Uma fase por commit, com CI verde antes da fase seguinte.

1. Janelas (maximizar, encaixar, persistência, ordem Z, múltiplas instâncias por tipo com payload)
2. Desktop, barra de tarefas, busca, fixados e recentes
3. Central de notificações
4. Mídia
5. Arquivos (SAF)
6. Teclado e mouse
7. Configurações e personalização
8. Experiência (animações, layout adaptável, acessibilidade)
9. Testes instrumentados no emulador e estabilidade
10. Documentação de instalação e APK de depuração. Release assinado depende de chave fornecida pelo responsável do projeto.

## 6. Bloqueios conhecidos

- **Assinatura release:** o workflow só assina se os secrets `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` e `KEY_PASSWORD` existirem. Nenhuma chave é criada ou enviada por este plano, porque a chave de assinatura é de responsabilidade do titular do projeto.
- **Teste físico:** notificações, mídia e SAF dependem de concessão manual de permissões em aparelho real. O emulador cobre o fluxo sem concessão.
- **Renomeação e applicationId:** só após proposta aprovada.
