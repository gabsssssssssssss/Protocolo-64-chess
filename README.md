# Chess Challenge — Java + Stockfish

Primeira versão local de treinamento. Requer Windows x64 e JDK 21 ou superior; desenvolvido no JDK 26. Interface Swing, regras pela chesslib e motor Stockfish em processo separado, via UCI.

## Configuração de força e diagnóstico

O motor usa 512 MB de Hash (antes 256 MB) e até 4 threads, conforme a quantidade de processadores lógicos. Skill Level 20, UCI_LimitStrength=false e MultiPV=1 são definidos explicitamente; esses três valores já eram os padrões do Stockfish fornecido, portanto não representam aumento de força por si mesmos. A memória de busca é mantida entre lances da mesma sessão. O aumento de Hash oferece mais espaço à tabela de busca, mas não comprova ganho de força e não aumenta o limite de 30 segundos por lance.

Para experimentar recursos diferentes, a execução Java aceita `-Dchess.threads=N` (1 até o número de processadores lógicos) e `-Dchess.hashMb=N` (16 a 2048 MB), antes de `-cp`. Mais threads não garantem melhor desempenho, especialmente executando dois motores simultaneamente. Compare configurações com o mesmo controle de tempo antes do campeonato.

Execute `powershell -ExecutionPolicy Bypass -File diagnosticar.ps1` para verificar quatro posições simples de mate e captura de dama, com busca de até 2 segundos por posição. O relatório CSV fica em `partidas/diagnostico-tatico-*.csv`. Esse diagnóstico verifica falhas básicas; não mede Elo, precisão geral ou superioridade sobre a versão anterior. Para comparar a gestão de tempo em partidas, use `comparar.ps1`, descrito adiante.

## Executar

### Campeonato com tabuleiro físico: terminal do VS Code

A saída durante a partida é reduzida às instruções da nossa jogada e aos prompts de cor, lance adversário e confirmação. Não pergunta tempo nem imprime orçamento de cálculo, mensagens de registro ou logs do motor. Erros de entrada/operação continuam aparecendo quando necessário; comandos explícitos como `tabuleiro` e `historico` mostram o conteúdo solicitado.

No VS Code, pressione **Ctrl+Shift+B** para compilar e iniciar o jogo no terminal integrado. Outra opção é executar `powershell -ExecutionPolicy Bypass -File jogar-terminal.ps1` no terminal. Para depurar, pressione F5 e selecione **Jogar no terminal (tabuleiro físico)**. Não exige abrir a janela gráfica.

1. Escolha `brancas` ou `pretas` (ou `restaurar` para recuperar a sessão do terminal).
2. Na vez do adversário, registre a origem/destino: `e7e5`, `e7 e5` ou SAN como `Nf3`, `O-O`. Para promoção por coordenadas, use `e7e8q` (q=dama, r=torre, b=bispo, n=cavalo).
3. Na nossa vez, a gestão própria decide a duração do cálculo conforme a posição, até 30 segundos. Não existe relógio acumulado nem derrota por tempo dentro do aplicativo.
4. Leia a instrução **MOVA O PEÃO DE E2 PARA E4**. Captura, roque, en passant e promoção têm instruções adicionais.
5. Execute no tabuleiro físico e digite `feito`. Só então o programa registra nosso lance. `cancelar` volta para recalcular; `sair` encerra sem confirmar a recomendação.

Comandos no prompt de lance adversário: `tabuleiro`, `historico`, `desfazer`, `salvar`, `sair`. `desfazer` exige corrigir também as peças físicas. A sessão fica em `partidas/sessao-terminal.properties`, separada da interface gráfica, marcada como `sem-limite`. Sessões antigas continuam restauráveis, inclusive com relógio zerado; o valor antigo é ignorado. Se sair ou ocorrer uma falha antes de `feito`, o último lance sugerido não estará no histórico; confira as peças antes de restaurar.

Tudo funciona localmente usando o mesmo Stockfish da interface. O dataset de estudo não participa da escolha dos lances nesta versão.

O tabuleiro usa tema escuro, moldura com detalhe dourado, casas verde/marfim e peças vetoriais próprias com gradientes e contornos. Não depende de fontes de símbolos, imagens externas ou downloads para desenhar as peças. Inclui coordenadas externas, destaque do último lance, indicação de seleção, hover e destinos legais (ponto para casa vazia, anel para captura). O mesmo componente é usado na partida, no estudo PGN e no confronto automático.

1. Na primeira preparação, execute `powershell -ExecutionPolicy Bypass -File preparar.ps1` com internet.
2. Abra `iniciar.bat`. Ele compila e abre a interface.
3. Escolha nossa cor antes do primeiro lance. Com brancas, clique em **Calcular nosso lance**. Com pretas, registre primeiro o lance adversário.
4. Registre o adversário clicando na origem e no destino, ou digitando coordenadas como `e2e4`. Roque curto branco: `e1g1`; promoção: `e7e8q`.
5. Clique em **Calcular nosso lance**. A gestão própria distribui até 30 segundos por posição; a partida não tem relógio interno nem campo de tempo restante.
6. Mova a peça indicada no tabuleiro físico. A posição da tela já inclui o lance calculado.
7. Salve o PGN para revisar a partida; os arquivos ficam em `partidas/`.

Não há acesso à rede durante o uso do aplicativo. Downloads ocorrem somente no script de preparação. Não fechar durante a análise; o botão de fechar informa que é necessário aguardar.

## Componentes e autoria

### Dataset e reprodução PGN

**Estatísticas de abertura:** a aba **Aberturas** consulta a coleção carregada (ou o subconjunto do filtro de jogador). Mostra frequência e vitórias/empates/derrotas do lado a jogar. O índice considera os primeiros 24 meios-lances de cada sequência e deduplica sequências completas idênticas, contando uma posição no máximo uma vez por sequência. A chave preserva peças, lado, roque e en passant legal; ignora contadores de lances para reconhecer transposições. Frequência não é qualidade, e o resultado de uma partida não prova que seu lance de abertura foi bom. Esta etapa não altera as decisões do motor nem as mensagens do terminal de jogo.

Para gerar CSV local, execute `powershell -ExecutionPolicy Bypass -File repertorio.ps1`. Por padrão usa a amostra do Carlsen. Em PowerShell, `./repertorio.ps1 -Arquivos @('datasets/carlsen-mundial-2021-1.pgn','datasets/historicas.pgn')` combina coleções; os arquivos ficam em `partidas/repertorio-...csv`. A atribuição/licença dos dados de origem continua aplicável aos dados derivados.

**Análise de posições:** pause a reprodução e clique em **Analisar posição com Stockfish**. O motor local busca por 2 segundos e apresenta avaliação na perspectiva das brancas, eventual mate previsto, lance sugerido, profundidade e até 10 meios-lances da linha principal em SAN. O histórico anterior da partida é enviado ao motor, inclusive a posição FEN inicial. A análise não aplica lances e não altera a partida histórica. Se você mudar de posição durante a análise, a resposta da posição anterior é descartada. A inicialização do motor pode levar tempo adicional. A avaliação é uma estimativa e não comprova qualidade de um repertório.

Há uma amostra real do Magnus Carlsen em `datasets/carlsen-mundial-2021-1.pgn` (89 meios-lances, Mundial 2021). No estudo, clique em **Abrir amostra do Magnus Carlsen**. Para coleções maiores, use **Abrir PGN** e digite `Carlsen` no filtro de jogador. O filtro procura nas brancas e nas pretas, sem diferenciar maiúsculas. Fonte e licença estão em `datasets/CARLSEN-FONTE.md`.

### Uso offline

Após a preparação inicial, `iniciar.bat`, `estudar.bat` e `assistir.bat` usam apenas o Java instalado, os JARs e o Stockfish em `vendor/`, e arquivos PGN locais. Nenhum deles baixa dependências durante a execução. `preparar.ps1` é o único script de instalação que usa internet; execute antes do evento. O VS Code não é necessário para jogar. Para confirmar no computador da competição, desligue o Wi-Fi/desconecte a rede e teste cálculo de lance, leitura do PGN do Carlsen e restauração de sessão. Não é necessário bloquear rede ou alterar firewall para usar o aplicativo.

Abra `estudar.bat` ou **Estudar partidas PGN** na interface. Clique em **Abrir PGN** para importar uma coleção local UTF-8 (até 20 MB e 1000 partidas). Escolha uma partida na lista; use Início/Anterior/Próximo/Fim ou Reproduzir/Pausar. A velocidade vai de 0,25 a 10 segundos entre lances e afeta apenas a reprodução visual. O tabuleiro é reconstruído com lances legais; erros de importação preservam a coleção anterior. São suportados roque, promoção e posição inicial FEN. Comentários e linhas alternativas são ignorados na reprodução; fica a linha principal.

Coloque suas coleções em `datasets/` e registre sua origem/licença conforme `datasets/README.md`. A tela abre inicialmente a Partida da Ópera de Paul Morphy, completa até o mate (33 meios-lances). Clique em **Reproduzir** para assistir; o padrão é 2 segundos por lance. `exemplo.pgn` mantém as sequências didáticas curtas. O visualizador funciona offline e não usa o Stockfish. O dataset ainda não é um repertório de aberturas nem treinamento do motor.

### Recuperação de sessão

A interface salva `partidas/sessao.properties` após registrar/desfazer lances, iniciar análise, reiniciar a partida e fechar normalmente. Para recuperar após reabrir, clique em **Restaurar sessão**. O histórico é validado antes de substituir a partida. Confirme a posição no tabuleiro físico. Iniciar uma nova partida substitui a sessão; exporte o PGN para preservar partidas anteriores. Sessões antigas são aceitas, mas seu relógio é ignorado.

O limite de cálculo por lance permite ao motor concluir a busca; não encerra a partida. O controle de tempo externo do campeonato continua sendo definido pela organização.

O salvamento usa caminhos absolutos e tenta substituição atômica, com alternativa de substituição normal para falhas do sistema de arquivos no Windows e até três tentativas para bloqueios transitórios. Se todas falharem, a mensagem informa o caminho de um arquivo `session-*.tmp` preservado com a sessão nova completa. Guarde essa cópia para recuperação; não a apague. A alternativa de substituição normal não oferece a mesma garantia contra interrupções que uma operação atômica.

- `ChessChallenge.java`: interface, histórico, entrada de lances legais e exportação PGN.
- `Stockfish.java`: transporte UCI, histórico completo enviado ao motor, limites de espera e encerramento de processo.
- `TimeManager.java`: a política `allocateForPosition` funciona no terminal e na interface e escolhe o orçamento por fase, capturas, xeque e promoção, com teto de 30 segundos e 100 ms quando só existe um lance legal. A política antiga `allocate(board,remaining)` permanece exclusiva do comparador.
- `Verification.java`: verificações de regras especiais, perft e integração com motor real.

A busca e a avaliação do Stockfish não foram alteradas. A contribuição decisória proposta é a gestão externa do tempo de cálculo por posição, funcional nas partidas interativas, sem relógio acumulado. Consulte `DEFESA-TECNICA.md` para a fórmula, justificativas, limites, demonstração e respostas para a arguição. É necessário confirmar com a organização a aceitação dessa abordagem e tratar com transparência a assistência de IA; não declarar conformidade integral nem autoria da equipe sobre o Stockfish e a chesslib.

Código inicial elaborado com assistência do Codex. Mantenham um registro transparente das contribuições e sigam eventuais orientações da faculdade sobre ferramentas de IA.

## Verificação

Para assistir aos confrontos automáticos, abra `assistir.bat`. O tabuleiro acompanha as duas partidas e mostra a cor da gestão própria, o lance e os tempos (atualizados ao fim de cada lance). Fechar o visualizador não interrompe os motores; os PGNs são preservados ao finalizar o confronto.

Execute `powershell -ExecutionPolicy Bypass -File verificar.ps1`.

No VS Code, o projeto reconhece `src/` e os JARs de `vendor/`. Com a extensão recomendada **Extension Pack for Java**, abra Executar e Depurar e selecione **Abrir Chess Challenge**. Os scripts continuam funcionando sem extensões.

Para comparar a política própria com a gestão original, execute `powershell -ExecutionPolicy Bypass -File comparar.ps1 -Segundos 30`. São duas partidas com cores invertidas e mesma abertura, usando processos separados, mesmo número de threads e mesma memória. PGNs e tempos por lance ficam em `partidas/comparacao-.../`. O orçamento original usa `go wtime/btime`; o próprio usa `go movetime`. Inicialização dos processos fica fora do relógio; duração da comunicação e busca entra no relógio. Há limite de segurança de 400 meios-lances, que produz partida inacabada (`*`). O teste rápido verifica integração; para avaliar força são necessárias muitas partidas e aberturas diferentes. A comparação automática não inclui tempo de movimentação de peças físicas.

Esse comparador usa a política antiga com saldo de tempo, não a nova política por posição das partidas interativas. Não usar seus resultados como evidência de força da política nova.

Antes do campeonato ainda são necessários: validação visual no computador da equipe, simulações de partidas físicas completas, teste sem internet, teste de aquecimento, comparação da gestão própria contra a original com mesmo hardware e controle de tempo, e preparação da arguição. Não há evidência de ganho de força desta política inicial.

## Licenças e origem

Stockfish: release `sf_19` do repositório oficial, GPLv3. Ver `ORIGENS.md` e `LICENSE`. O pacote local preserva os arquivos distribuídos pelo projeto, incluindo código-fonte e redes quando presentes. O aplicativo deste repositório adota GPLv3; mantenha os avisos e forneça o código correspondente ao distribuir binários.

chesslib 1.3.7 e Apache Commons Lang 3.18.0: Apache-2.0. Seus avisos e licenças devem acompanhar a distribuição. Os JARs preservam seus arquivos META-INF. Não há serviços online usados nas partidas.

## Entrega

### Registrar alterações no GitHub

Após configurar o repositório remoto, salve os arquivos no VS Code, revise suas alterações no painel Controle do Código-Fonte e execute `powershell -ExecutionPolicy Bypass -File sincronizar.ps1 -Mensagem "Descreva a melhoria realizada"`. O comando prepara todas as alterações não ignoradas, cria um commit e envia a branch atual ao GitHub. Se já houver commits locais sem alterações novas, ele tenta enviá-los novamente. Salvar um arquivo no editor não cria um commit automaticamente.

Cada melhoria concluída deve receber uma descrição clara. Não coloque senhas ou tokens no código; arquivos `.env`, chaves privadas, dependências, compilações e partidas estão ignorados. Os commits anteriores do projeto são preservados no envio. O script não executa os testes: use `verificar.ps1` quando a alteração exigir validação.

O código-fonte está versionado; dependências, binários e partidas ficam fora do Git. A entrega deve incluir as versões e licenças, instruções reproduzíveis, o componente próprio e sua explicação. Data indicada no regulamento: 04/11/2026. Confirme com a organização o formato de envio e os pontos contraditórios do regulamento antes da entrega.
