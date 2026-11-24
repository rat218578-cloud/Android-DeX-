# Pendências de teste em aparelho físico — Deskora

Estes itens não podem ser validados só com emulador ou testes JVM. Ficam registrados aqui para
serem feitos em um celular real, em pelo menos um aparelho Android 12+ e, se possível, um tablet.

## Permissões e acessos do sistema
- [ ] Central de notificações: conceder acesso às notificações em Ajustes, receber notificações reais
      de um app de mensagens e confirmar que o histórico aparece e que o botão Limpar funciona.
- [ ] Mídia: tocar música em um player de terceiro (ex.: Spotify, YouTube Music) e confirmar
      play/pause, próxima faixa, título e artista. Ajustes de volume pelo Deskora.
- [ ] Arquivos (SAF): escolher uma pasta pelo seletor do sistema, navegar, renomear, copiar, mover e
      excluir. Confirmar que a permissão persiste após reiniciar o aparelho.
- [ ] Provedores de terceiros (Google Drive, armazenamento de apps): verificar o comportamento de
      copiar/mover; o Android pode recusar operações em alguns provedores.

## Janelas e entrada
- [ ] Multijanela e janelas de apps externos: depende do fabricante e do modo do sistema; o Deskora
      não controla essas janelas.
- [ ] Atalhos de teclado físico (Alt+Tab, Alt+F4, Windows+setas): confirmar quais combinações chegam
      ao app e quais são reservadas pelo sistema ou pelo fabricante.
- [ ] Mouse e touchpad: clique direito, arrastar e redimensionar com mouse Bluetooth/USB.

## Telas e ciclo de vida
- [ ] Rotação de tela e mudança de tamanho (dobrável, tela externa): o layout de janelas deve se
      adaptar sem perder janelas.
- [ ] Telas pequenas (≈ 5") e grandes (tablet): menu Iniciar, barra de tarefas e janelas legíveis.
- [ ] Retorno de Ajustes (permissões concedidas fora do app): a central e a mídia devem reagir.
- [ ] Memória: abrir muitas janelas de arquivos com imagens e observar uso de memória.

## Acessibilidade
- [ ] Revisão com TalkBack: navegação entre janelas, barra de tarefas e menu Iniciar.
- [ ] Contraste dos temas claro e escuro.
- [ ] Redução de animações do sistema: confirmar que o app não depende de animação para funcionar.

## Não verificado nesta fase
- Medição de quadros por segundo e de consumo de bateria.
- Testes de tamanhos de tela além do emulador Pixel 6 (perfil único no CI).
