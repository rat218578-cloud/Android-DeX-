# Desempenho e acessibilidade — Deskora (fase 8)

Este documento registra o que foi feito na fase 8 e o que ainda depende de medição em aparelho.

## Animações
- Janelas: entrada com fade + escala (180 ms). Fechar remove a janela sem animação de saída, de propósito, para não manter estado de janela já fechada.
- Menu Iniciar: entrada e saída com fade + deslizamento vertical (120–160 ms).
- Nenhuma animação altera o estado das janelas; são apenas transições de apresentação.

## Layout adaptativo
- Menu Iniciar: largura entre 260 dp e 360 dp; altura de até 80% da área, limitada a 500 dp. Em telas estreitas ele encolhe em vez de sair da tela.
- Janelas: maximizadas e encaixadas derivam o retângulo da área atual; a rotação não quebra o layout.

## Acessibilidade
- Botões da barra de título têm descrições de semântica (minimizar, maximizar, fechar).
- Atalhos de teclado estão listados em Configurações.
- Pendente: revisão com TalkBack em aparelho real; contraste dos temas claro e escuro não foi medido.

## Desempenho
- Persistência do layout com debounce de 400 ms: cada movimento de janela não grava no disco.
- Notificações são mantidas só em memória e limitadas a 100 itens.
- Miniaturas de imagem de arquivos usam `inSampleSize` para reduzir a memória; textos de pré-visualização são limitados a 64 KB.
- Pendente: medição de quadros por segundo e de memória em aparelho (não há aparelho nesta sessão).

## Limitações honestas
- Nada disto foi testado em aparelho físico ou emulador ainda; os testes automatizados cobrem a lógica pura (JVM).
