# Componente decisório do projeto — gestão de cálculo por posição

## O que foi implementado

Além da gestão descrita abaixo, o projeto implementa `TacticalSearch.mateInOne`: enumera os lances legais, simula cada um em uma cópia do tabuleiro e testa se o adversário ficou em xeque-mate. Se encontrar, devolve esse lance sem consultar o Stockfish. Se não encontrar, delega a busca ao motor com o orçamento próprio. A seleção utiliza as regras da chesslib, não uma avaliação ou resposta da API do Stockfish. É executada no terminal e na interface. É uma busca exata limitada a um lance; não resolve combinações mais longas nem demonstra superioridade sobre o Stockfish.

O projeto utiliza o Stockfish 19 para busca e avaliação, a chesslib para regras e uma política Java específica do projeto para decidir a duração da busca em cada posição. A implementação está em `TimeManager.allocateForPosition(Board)`. Essa função é chamada efetivamente por `TerminalGame` e `ChessChallenge` antes de cada cálculo, e seu resultado determina o parâmetro `go movetime` enviado ao Stockfish. Não há relógio acumulado nem encerramento da partida por tempo dentro desses modos.

O FAQ fornecido pela equipe inclui gestão de tempo entre os componentes decisórios admitidos. Esta proposta gerencia o tempo de cálculo por lance externamente ao núcleo C++. A aceitação desse formato deve ser confirmada com a organização; este documento não certifica conformidade integral com o regulamento.

## Regras da política v1

Primeiro são considerados apenas lances legais. Se a partida já terminou, não há cálculo. Quando só existe um lance legal, são usados 100 ms: prolongar a busca não muda a única escolha disponível.

Para as demais posições, soma-se o material de ambos os lados, sem peões nem reis: cavalo e bispo valem 3, torre vale 5 e dama vale 9. Esse material serve para classificar a fase, não para avaliar quem está melhor.

| Situação | Tempo base |
| --- | ---: |
| Final: material somado até 20 | 18.000 ms |
| Abertura: até o lance 10 e material somado pelo menos 50 | 12.000 ms |
| Demais posições | 16.000 ms |

Ao tempo base são adicionados:

- `8000 * capturasLegais / totalDeLancesLegais` ms, com divisão inteira; en passant é contado como captura.
- 6.000 ms se o lado a jogar estiver em xeque.
- 6.000 ms se existir alguma promoção legal; subpromoções não multiplicam esse bônus.

O resultado é limitado a 30.000 ms. O limite não inclui inicialização e comunicação com o processo. Os testes de integração podem usar um teto menor para não aguardar esses intervalos; o uso normal mantém o teto de 30 segundos.

Exemplo: uma posição de meio-jogo com 30 lances legais, dos quais 6 são capturas, sem xeque nem promoção, recebe `16000 + 8000*6/30 = 17600 ms`.

## Por que essas regras

A hipótese é poupar cálculo em aberturas tranquilas e decisões forçadas e direcionar mais cálculo a posições com indicadores de atividade tática e aos finais. Capturas e xeques são indicadores incompletos: uma posição tranquila pode esconder uma combinação, e uma posição com muitas capturas pode ser simples. Os números são parâmetros experimentais escolhidos no projeto; não foram aprendidos de um dataset nem demonstrados ótimos.

Essa política pode produzir uma escolha diferente por interromper a busca em outro momento, mas não modifica diretamente a avaliação nem garante que o lance será diferente ou melhor. Ela também pode jogar pior que um cálculo fixo de 30 segundos ou a gestão original do Stockfish. Ainda não há evidência estatística de ganho de força.

## Como demonstrar

1. Abra `src/TimeManager.java` e explique a fórmula com suas próprias palavras.
2. Mostre a chamada `allocateForPosition(board)` em `TerminalGame` e `ChessChallenge` e o uso de `budget.millis()` no cálculo.
3. Mostre `Stockfish.play`, que envia `go movetime` ao processo local.
4. Execute `powershell -ExecutionPolicy Bypass -File verificar.ps1`. A verificação da política cobre abertura, meio-jogo, final, xeque, resposta forçada, en passant, promoção, limites e posições terminadas, sem modificar o tabuleiro.
5. Abra `tmp/gestao-propria-exemplos.csv`, gerado por esse teste, para mostrar os orçamentos e motivos. O relatório demonstra as decisões da política, não ganho de força.
6. Execute uma partida pelo terminal. O componente funciona sem imprimir explicações adicionais durante o jogo. Faça também testes com o computador desconectado da internet.

O comparador `comparar.ps1` ainda usa a política antiga com relógio acumulado. Seus resultados NÃO medem a nova política por posição. Para avaliar a nova política, ainda é necessário comparar muitas partidas com cores e aberturas alternadas, mesmos recursos computacionais e um controle experimental de tempo claramente definido.

## Respostas para a arguição

O novo `testar-forca.ps1` compara a seleção de mate imediato e a política por posição com busca fixa, com várias aberturas e cores invertidas. O teto é escalável e o tempo real é registrado por lance. O orçamento adaptativo pode usar menos CPU que o fixo; essa comparação não controla igualdade de tempo total de cálculo. Consulte o README para condições e limites. Execute `TacticalVerification` para demonstrar seleção própria de mate, promoção e rejeição de afogamento.

**O que é de terceiros?** A busca e a avaliação são do Stockfish 19; as regras e a geração de lances são da chesslib. As versões, origem e licenças estão em `ORIGENS.md`.

**Qual componente foi implementado no projeto?** A busca e seleção de mate imediato, mais a decisão de quanto tempo calcular nas demais posições, expressas em Java e utilizadas nos dois modos de partida interativos. A interface, o salvamento e a importação PGN também pertencem à aplicação, mas não são apresentados como modificação decisória do motor.

**Como funciona sem relógio?** A partida não tem um saldo de minutos. Cada posição recebe um orçamento independente, que termina a busca e permite ao motor devolver a jogada. Isso não remove o relógio físico nem as regras de tempo do evento.

**O dataset do Carlsen escolhe os lances?** Não. Ele serve para estudo e estatísticas; não participa da decisão do motor nesta versão.

**A vitória contra o bot prova superioridade?** Não. Confirma uma vitória naquela partida. Não determina Elo, taxa de vitória nem ganho causado por esta política, que foi implementada depois dessa partida.

**Houve ajuda de IA?** Sim. A implementação e esta documentação foram elaboradas com assistência do Codex. A equipe deve revisar, compreender, testar e seguir as regras da faculdade sobre autoria e uso de IA. Não afirmar que o código foi todo escrito manualmente pela equipe nem que o Stockfish foi criado por ela.

## Antes da entrega

Confirmar com a organização a aceitação da gestão externa por lance como componente significativo, especialmente porque o FAQ exige autoria própria. A equipe deve cumprir o credenciamento, declarar bases e versões, fornecer acesso ao repositório e preparar a arguição. O histórico Git registra as alterações, mas, sozinho, não comprova domínio técnico nem autoria independente. As demais condições do PDF precisam ser conferidas separadamente; não estão certificadas por esta implementação.
