# Instalação — Deskora (versão 0.1.0, APK debug)

## O que está disponível
- **APK debug** gerado pelo CI a cada commit da branch `feature/android-desktop-apk`.
  É assinado com a chave de debug do Android, por isso é instalável diretamente, mas **não é uma
  versão de loja**.
- **APK release assinado**: pendente. Depende do keystore do titular do projeto, que deve ser
  configurado nos secrets do GitHub (ver abaixo). Nenhuma chave foi criada ou commitada.

## Requisitos
- Android 12 (API 31) ou superior.
- Permitir instalação de fontes desconhecidas para o app que você usar para abrir o APK
  (Ajustes → Apps → acesso especial → Instalar apps desconhecidos).

## Como obter e verificar o APK
1. Abra a execução do workflow "Android APK" no GitHub (aba Actions) e baixe o artefato
   `desktop-env-apk`. O artefato traz `SHA256SUMS.txt` e `SIZES.txt`.
2. Confira o SHA-256 antes de instalar:
   ```
   sha256sum app-debug.apk
   ```
   O valor deve ser igual ao do `SHA256SUMS.txt` da mesma execução.

Versão 0.1.0 (commit `5e968f1`, execução 45379686550):
- SHA-256: `3b9fd8d22db9f9738efe38f8f615c962bd508dfe6af1a294fe6b85526d5b560b`
- Tamanho: 24 MB, `app/build/outputs/apk/debug/app-debug.apk`

## Instalação
1. Copie o APK para o celular ou abra-o pelo gerenciador de arquivos.
2. Toque no arquivo e confirme a instalação.
3. Abra o app **Deskora** (o pacote instalado é `com.example.desktopenv.debug`).

Se uma versão anterior do mesmo pacote estiver instalada com outra assinatura, desinstale-a antes.

## Permissões e o que cada uma habilita
O Deskora pede acesso apenas quando você usa o recurso correspondente:

| Recurso | Como conceder | Sem a permissão |
|---|---|---|
| Central de notificações | Ajustes → Notificações → Acesso às notificações | A central fica vazia; o resto funciona |
| Controle de mídia | Ajustes de acesso ao app de notificações (o mesmo do item anterior) | Mídia não é controlada |
| Pastas de arquivos | Dentro do app, escolha a pasta pelo seletor do sistema | Nenhuma pasta é acessada |

As notificações são processadas no próprio aparelho e mantidas só em memória. Nada é enviado a servidores.

## Limitações conhecidas
- O Deskora controla apenas as janelas internas. Apps externos abrem conforme o sistema e o
  fabricante (tela cheia ou multijanela); o Deskora não os redimensiona nem os fecha.
- Atalhos como Alt+F4 e Windows+setas funcionam só quando o teclado chega ao app. Combinações
  reservadas pelo sistema não são garantidas.
- Ativar ou desativar Wi-Fi e Bluetooth não é feito pelo app: o Android não permite isso a
  apps comuns. O Deskora apenas abre as telas de ajustes do sistema.

## Desinstalação
Ajustes → Apps → Deskora → Desinstalar. Os arquivos das pastas escolhidas não são apagados.

## Assinatura de release (para o titular do projeto)
O workflow gera o APK release somente se os secrets abaixo existirem no repositório:

- `KEYSTORE_BASE64`: keystore em base64 (`base64 -w0 minha.jks`)
- `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`

Sem eles, o CI gera apenas o APK debug. A chave de assinatura deve ser guardada pelo titular e
nunca commitada.
