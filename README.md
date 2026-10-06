# Chess Challenge — Java + Stockfish

Primeira versão local de treinamento. Requer Windows x64 e JDK 21 ou superior; desenvolvido no JDK 26. Interface Swing, regras pela chesslib e motor Stockfish em processo separado, via UCI.

## Executar

### Campeonato com tabuleiro físico: terminal do VS Code

No VS Code, pressione **Ctrl+Shift+B** para compilar e iniciar o jogo no terminal integrado. Outra opção é executar `powershell -ExecutionPolicy Bypass -File jogar-terminal.ps1` no terminal. Para depurar, pressione F5 e selecione **Jogar no terminal (tabuleiro físico)**. Não exige abrir a janela gráfica.

1. Escolha `brancas` ou `pretas` (ou `restaurar` para recuperar a sessão do terminal).
2. Na vez do adversário, registre a origem/destino: `e7e5`, `e7 e5` ou SAN como `Nf3`, `O-O`. Para promoção por coordenadas, use `e7e8q` (q=dama, r=torre, b=bispo, n=cavalo).
3. Na nossa vez, informe o tempo real restante, como `24:30`. A gestão própria determina o tempo de análise.
4. Leia a instrução **MOVA O PEÃO DE E2 PARA E4**. Captura, roque, en passant e promoção têm instruções adicionais.
5. Execute no tabuleiro físico, acione o relógio e digite `feito`. Só então o programa registra nosso lance. `cancelar` volta para recalcular; `sair` encerra sem confirmar a recomendação.

Comandos nos prompts de lance/tempo: `tabuleiro`, `historico`, `desfazer`, `salvar`, `sair`. `desfazer` exige corrigir também as peças físicas. A sessão fica em `partidas/sessao-terminal.properties`, separada da interface gráfica. O tempo salvo desconta o cálculo, não a movimentação manual: sempre atualize pelo relógio físico. Se sair ou ocorrer uma falha antes de `feito`, o último lance sugerido não estará no histórico; confira as peças antes de restaurar. A recuperação não altera a regra do evento sobre derrota por falha.

Tudo funciona localmente usando o mesmo Stockfish e componente de gestão de tempo da interface. O dataset de estudo não participa da escolha dos lances nesta versão.

O tabuleiro usa tema escuro, moldura com detalhe dourado, casas verde/marfim e peças vetoriais próprias com gradientes e contornos. Não depende de fontes de símbolos, imagens externas ou downloads para desenhar as peças. Inclui coordenadas externas, destaque do último lance, indicação de seleção, hover e destinos legais (ponto para casa vazia, anel para captura). O mesmo componente é usado na partida, no estudo PGN e no confronto automático.

1. Na primeira preparação, execute `powershell -ExecutionPolicy Bypass -File preparar.ps1` com internet.
2. Abra `iniciar.bat`. Ele compila e abre a interface.
3. Escolha nossa cor antes do primeiro lance. Com brancas, clique em **Calcular nosso lance**. Com pretas, registre primeiro o lance adversário.
4. Registre o adversário clicando na origem e no destino, ou digitando coordenadas como `e2e4`. Roque curto branco: `e1g1`; promoção: `e7e8q`.
5. Atualize **nosso tempo restante** pelo relógio físico antes de calcular. O campo não é um cronômetro oficial: só desconta a duração da operação de cálculo, incluindo a inicialização do motor.
6. Mova a peça indicada no tabuleiro físico. A posição da tela já inclui o lance calculado.
7. Salve o PGN para revisar a partida; os arquivos ficam em `partidas/`.

Não há acesso à rede durante o uso do aplicativo. Downloads ocorrem somente no script de preparação. Não fechar durante a análise; o botão de fechar informa que é necessário aguardar.

## Componentes e autoria

### Dataset e reprodução PGN

**Análise de posições:** pause a reprodução e clique em **Analisar posição com Stockfish**. O motor local busca por 2 segundos e apresenta avaliação na perspectiva das brancas, eventual mate previsto, lance sugerido, profundidade e até 10 meios-lances da linha principal em SAN. O histórico anterior da partida é enviado ao motor, inclusive a posição FEN inicial. A análise não aplica lances e não altera a partida histórica. Se você mudar de posição durante a análise, a resposta da posição anterior é descartada. A inicialização do motor pode levar tempo adicional. A avaliação é uma estimativa e não comprova qualidade de um repertório.

Há uma amostra real do Magnus Carlsen em `datasets/carlsen-mundial-2021-1.pgn` (89 meios-lances, Mundial 2021). No estudo, clique em **Abrir amostra do Magnus Carlsen**. Para coleções maiores, use **Abrir PGN** e digite `Carlsen` no filtro de jogador. O filtro procura nas brancas e nas pretas, sem diferenciar maiúsculas. Fonte e licença estão em `datasets/CARLSEN-FONTE.md`.

### Uso offline

Após a preparação inicial, `iniciar.bat`, `estudar.bat` e `assistir.bat` usam apenas o Java instalado, os JARs e o Stockfish em `vendor/`, e arquivos PGN locais. Nenhum deles baixa dependências durante a execução. `preparar.ps1` é o único script de instalação que usa internet; execute antes do evento. O VS Code não é necessário para jogar. Para confirmar no computador da competição, desligue o Wi-Fi/desconecte a rede e teste cálculo de lance, leitura do PGN do Carlsen e restauração de sessão. Não é necessário bloquear rede ou alterar firewall para usar o aplicativo.

Abra `estudar.bat` ou **Estudar partidas PGN** na interface. Clique em **Abrir PGN** para importar uma coleção local UTF-8 (até 20 MB e 1000 partidas). Escolha uma partida na lista; use Início/Anterior/Próximo/Fim ou Reproduzir/Pausar. A velocidade vai de 0,25 a 10 segundos entre lances e afeta apenas a reprodução visual. O tabuleiro é reconstruído com lances legais; erros de importação preservam a coleção anterior. São suportados roque, promoção e posição inicial FEN. Comentários e linhas alternativas são ignorados na reprodução; fica a linha principal.

Coloque suas coleções em `datasets/` e registre sua origem/licença conforme `datasets/README.md`. A tela abre inicialmente a Partida da Ópera de Paul Morphy, completa até o mate (33 meios-lances). Clique em **Reproduzir** para assistir; o padrão é 2 segundos por lance. `exemplo.pgn` mantém as sequências didáticas curtas. O visualizador funciona offline e não usa o Stockfish. O dataset ainda não é um repertório de aberturas nem treinamento do motor.

### Recuperação de sessão

A interface salva `partidas/sessao.properties` após registrar/desfazer lances, iniciar análise, reiniciar a partida e fechar normalmente. Para recuperar após reabrir, clique em **Restaurar última sessão**. O histórico é validado antes de substituir a partida. Confirme a posição no tabuleiro físico e atualize o relógio: o tempo salvo não inclui o período em que o programa esteve fechado nem o tempo de operação manual. Iniciar uma nova partida substitui a sessão; exporte o PGN para preservar partidas anteriores. Esta função não altera a regra do campeonato sobre derrota por falha.

Se o cálculo consumir todo o tempo informado, o lance não é aplicado. O relógio físico continua sendo a referência oficial.

- `ChessChallenge.java`: interface, histórico, entrada de lances legais e exportação PGN.
- `Stockfish.java`: transporte UCI, histórico completo enviado ao motor, limites de espera e encerramento de processo.
- `TimeManager.java`: política própria experimental que determina `go movetime`. Reserva tempo operacional, estima um horizonte de lances e ajusta o orçamento conforme capturas, promoções e xeque. Limita cada cálculo a 30 segundos e 8% do tempo restante. Lance único recebe orçamento reduzido.
- `Verification.java`: verificações de regras especiais, perft e integração com motor real.

A busca e a avaliação do Stockfish não foram alteradas. A política Java substitui o orçamento de tempo automático pela instrução UCI `movetime`; não é um fork do núcleo C++. O componente influencia as partidas, mas a equipe precisa compreender, revisar, desenvolver e defender sua implementação. A aceitação da gestão externa como modificação significativa deve ser confirmada pela banca. Não declarar o Stockfish nem a biblioteca como criação da equipe.

Código inicial elaborado com assistência do Codex. Mantenham um registro transparente das contribuições e sigam eventuais orientações da faculdade sobre ferramentas de IA.

## Verificação

Para assistir aos confrontos automáticos, abra `assistir.bat`. O tabuleiro acompanha as duas partidas e mostra a cor da gestão própria, o lance e os tempos (atualizados ao fim de cada lance). Fechar o visualizador não interrompe os motores; os PGNs são preservados ao finalizar o confronto.

Execute `powershell -ExecutionPolicy Bypass -File verificar.ps1`.

No VS Code, o projeto reconhece `src/` e os JARs de `vendor/`. Com a extensão recomendada **Extension Pack for Java**, abra Executar e Depurar e selecione **Abrir Chess Challenge**. Os scripts continuam funcionando sem extensões.

Para comparar a política própria com a gestão original, execute `powershell -ExecutionPolicy Bypass -File comparar.ps1 -Segundos 30`. São duas partidas com cores invertidas e mesma abertura, usando processos separados, mesmo número de threads e mesma memória. PGNs e tempos por lance ficam em `partidas/comparacao-.../`. O orçamento original usa `go wtime/btime`; o próprio usa `go movetime`. Inicialização dos processos fica fora do relógio; duração da comunicação e busca entra no relógio. Há limite de segurança de 400 meios-lances, que produz partida inacabada (`*`). O teste rápido verifica integração; para avaliar força são necessárias muitas partidas e aberturas diferentes. A comparação automática não inclui tempo de movimentação de peças físicas.

Antes do campeonato ainda são necessários: validação visual no computador da equipe, simulações de partidas físicas completas, teste sem internet, teste de aquecimento, comparação da gestão própria contra a original com mesmo hardware e controle de tempo, e preparação da arguição. Não há evidência de ganho de força desta política inicial.

## Licenças e origem

Stockfish: release `sf_19` do repositório oficial, GPLv3. Ver `ORIGENS.md` e `LICENSE`. O pacote local preserva os arquivos distribuídos pelo projeto, incluindo código-fonte e redes quando presentes. O aplicativo deste repositório adota GPLv3; mantenha os avisos e forneça o código correspondente ao distribuir binários.

chesslib 1.3.7 e Apache Commons Lang 3.18.0: Apache-2.0. Seus avisos e licenças devem acompanhar a distribuição. Os JARs preservam seus arquivos META-INF. Não há serviços online usados nas partidas.

## Entrega

O código-fonte está versionado; dependências, binários e partidas ficam fora do Git. A entrega deve incluir as versões e licenças, instruções reproduzíveis, o componente próprio e sua explicação. Data indicada no regulamento: 04/11/2026. Confirme com a organização o formato de envio e os pontos contraditórios do regulamento antes da entrega.
