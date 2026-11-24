# Proposta de nome — Deskora (não executada)

Status: **apenas proposta**. Nenhuma renomeação foi feita. O `applicationId` continua
`com.example.desktopenv` e o nome do app continua o atual.

## Nome proposto: Deskora
- Nome provisório escolhido no roteiro.
- Não verificado: disponibilidade de marca, domínio e nome na Play Store. Antes de adotar,
  é preciso consultar o INPI e a loja.

## Alternativas consideradas
Harbor Desk, Pane, Tilewell, Quadrant. Mesma ressalva: marca e loja não verificadas.

## O que a renomeação exigiria (se aprovada)
1. Trocar `rootProject.name` em `settings.gradle.kts` e o `app_name` em `strings.xml`.
2. Trocar `applicationId` e `namespace` (hoje `com.example.desktopenv`) para um domínio próprio
   do titular. Isso faz o Android tratar o app como outro: quem tiver a versão atual precisará
   desinstalá-la e os dados locais não migram.
3. Ajustar a documentação e os nomes de pacote Kotlin, se o namespace mudar.

## Decisão pendente
Aprovar o nome e, separadamente, o novo `applicationId`. Enquanto isso, o pacote atual é mantido.
