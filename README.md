# Chess Challenge — Java + Stockfish

Primeira versão local de treinamento. Requer Windows x64 e JDK 21 ou superior; desenvolvido no JDK 26. Interface Swing, regras pela chesslib e motor Stockfish em processo separado, via UCI.

## Executar

1. Na primeira preparação, execute `powershell -ExecutionPolicy Bypass -File preparar.ps1` com internet.
2. Abra `iniciar.bat`. Ele compila e abre a interface.
3. Escolha nossa cor antes do primeiro lance. Com brancas, clique em **Calcular nosso lance**. Com pretas, registre primeiro o lance adversário.
4. Registre o adversário clicando na origem e no destino, ou digitando coordenadas como `e2e4`. Roque curto branco: `e1g1`; promoção: `e7e8q`.
5. Atualize **nosso tempo restante** pelo relógio físico antes de calcular. O campo não é um cronômetro oficial: só desconta a duração da operação de cálculo, incluindo a inicialização do motor.
6. Mova a peça indicada no tabuleiro físico. A posição da tela já inclui o lance calculado.
7. Salve o PGN para revisar a partida; os arquivos ficam em `partidas/`.

Não há acesso à rede durante o uso do aplicativo. Downloads ocorrem somente no script de preparação. Não fechar durante a análise; o botão de fechar informa que é necessário aguardar.

## Componentes e autoria

- `ChessChallenge.java`: interface, histórico, entrada de lances legais e exportação PGN.
- `Stockfish.java`: transporte UCI, histórico completo enviado ao motor, limites de espera e encerramento de processo.
- `TimeManager.java`: política própria experimental que determina `go movetime`. Reserva tempo operacional, estima um horizonte de lances e ajusta o orçamento conforme capturas, promoções e xeque. Limita cada cálculo a 30 segundos e 8% do tempo restante. Lance único recebe orçamento reduzido.
- `Verification.java`: verificações de regras especiais, perft e integração com motor real.

A busca e a avaliação do Stockfish não foram alteradas. A política Java substitui o orçamento de tempo automático pela instrução UCI `movetime`; não é um fork do núcleo C++. O componente influencia as partidas, mas a equipe precisa compreender, revisar, desenvolver e defender sua implementação. A aceitação da gestão externa como modificação significativa deve ser confirmada pela banca. Não declarar o Stockfish nem a biblioteca como criação da equipe.

Código inicial elaborado com assistência do Codex. Mantenham um registro transparente das contribuições e sigam eventuais orientações da faculdade sobre ferramentas de IA.

## Verificação

Execute `powershell -ExecutionPolicy Bypass -File verificar.ps1`.

Antes do campeonato ainda são necessários: validação visual no computador da equipe, simulações de partidas físicas completas, teste sem internet, teste de aquecimento, comparação da gestão própria contra a original com mesmo hardware e controle de tempo, e preparação da arguição. Não há evidência de ganho de força desta política inicial.

## Licenças e origem

Stockfish: release `sf_19` do repositório oficial, GPLv3. Ver `ORIGENS.md` e `LICENSE`. O pacote local preserva os arquivos distribuídos pelo projeto, incluindo código-fonte e redes quando presentes. O aplicativo deste repositório adota GPLv3; mantenha os avisos e forneça o código correspondente ao distribuir binários.

chesslib 1.3.7 e Apache Commons Lang 3.18.0: Apache-2.0. Seus avisos e licenças devem acompanhar a distribuição. Os JARs preservam seus arquivos META-INF. Não há serviços online usados nas partidas.

## Entrega

O código-fonte está versionado; dependências, binários e partidas ficam fora do Git. A entrega deve incluir as versões e licenças, instruções reproduzíveis, o componente próprio e sua explicação. Data indicada no regulamento: 04/11/2026. Confirme com a organização o formato de envio e os pontos contraditórios do regulamento antes da entrega.
