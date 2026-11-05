import com.github.bhlangonijr.chesslib.*;
import java.io.*;
import java.nio.file.*;

public final class TerminalVerification {
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Board board=new Board();
        check(MoveInstruction.describe(board,TerminalGame.parseMove(board,"e2 e4")).contains("MOVA O PEÃO DE E2 PARA E4"),"Instrução de peão");
        board.loadFromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        check(MoveInstruction.describe(board,TerminalGame.parseMove(board,"O-O")).contains("torre de H1 para F1"),"Roque curto");
        board.loadFromFen("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1");
        check(MoveInstruction.describe(board,TerminalGame.parseMove(board,"e8c8")).contains("torre de A8 para D8"),"Roque longo preto");
        board.loadFromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2");
        check(MoveInstruction.describe(board,TerminalGame.parseMove(board,"e5d6")).contains("peão adversário de D5"),"En passant");
        board.loadFromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        check(MoveInstruction.describe(board,TerminalGame.parseMove(board,"a7a8n")).contains("pelo cavalo"),"Subpromoção");
        Path folder=Files.createTempDirectory("terminal-verification-");
        try {
            StringWriter buffer=new StringWriter();
            new TerminalGame(new BufferedReader(new StringReader("pretas\ne2e5\ne2e4\nfeito\nsalvar\nsair\n")),new PrintWriter(buffer,true),folder,200).run();
            String transcript=buffer.toString();
            check(transcript.contains("Lance inválido") && transcript.contains("MOVA"),"Fluxo terminal com motor real");
            var saved=new SessionStore(folder.resolve("sessao-terminal.properties")).load();
            check(saved.moves().size()==2 && saved.color()==Side.BLACK,"Sessão deve conter adversário e nosso lance");
            check(TerminalGame.readClock(saved.clock())>=900_000,"Incremento registrado");
            Path log=folder.resolve("partida-"+saved.matchId()+".log");
            var logLines=Files.readAllLines(log);
            check(logLines.size()==3 && logLines.get(1).startsWith("1\tbrancas\te2e4\t1\t1\tcp 0\te2e4\t"),"Avaliação local real do lance adversário no log");
            check(logLines.get(1).endsWith("adversario_avaliacao_local_java"),"Origem da avaliação adversária explícita");
            var metrics=logLines.get(2).split("\t");
            check(metrics[1].equals("pretas") && Integer.parseInt(metrics[3])>0 && Long.parseLong(metrics[4])>0
                && !metrics[5].equals("NA") && metrics[6].startsWith(metrics[2]) && Long.parseLong(metrics[7])>=0,
                "Métricas reais do Stockfish");
            new TerminalGame(new BufferedReader(new StringReader("restaurar\n14:30\nsair\n")),new PrintWriter(new StringWriter(),true),folder).run();
            check(Files.readAllLines(log).equals(logLines),"Restauração conserva o mesmo log");
            new SessionStore(folder.resolve("sessao-terminal.properties")).save(saved.color(),"00:00",saved.moves());
            StringWriter restore=new StringWriter();
            new TerminalGame(new BufferedReader(new StringReader("restaurar\n14:30\nsair\n")),new PrintWriter(restore,true),folder).run();
            check(!restore.toString().contains("Não foi possível restaurar"),"Restauração terminal");
            StringWriter resumed=new StringWriter();
            new SessionStore(folder.resolve("sessao-terminal.properties")).save(Side.WHITE,"00:00",java.util.List.of());
            new TerminalGame(new BufferedReader(new StringReader("restaurar\n00:00\n14:30\nfeito 14:35\nsair\n")),new PrintWriter(resumed,true),folder,200).run();
            check(resumed.toString().contains("Confira o relógio físico") && resumed.toString().contains("MOVA"),"Relógio esgotado exige sincronização");
            check(new SessionStore(folder.resolve("sessao-terminal.properties")).load().clock().equals("14:35"),"Relógio físico prevalece após confirmação");
            Board beforeMate=new Board();
            var mateHistory=new java.util.ArrayList<com.github.bhlangonijr.chesslib.move.Move>();
            for(String uci:new String[]{"f2f3","e7e5","g2g4"}) {
                var move=TerminalGame.parseMove(beforeMate,uci); mateHistory.add(move); beforeMate.doMove(move,true);
            }
            new SessionStore(folder.resolve("sessao-terminal.properties")).save(Side.BLACK,"sem-limite",mateHistory);
            StringWriter finish=new StringWriter();
            new TerminalGame(new BufferedReader(new StringReader("restaurar\n14:30\nfeito\n")),new PrintWriter(finish,true),folder,1).run();
            check(finish.toString().contains("Fim: 0-1"),"Busca própria deve concluir o mate no fluxo do terminal");
            check(new SessionStore(folder.resolve("sessao-terminal.properties")).load().moves().size()==4,"Mate confirmado deve ficar salvo");
            var mateSession=new SessionStore(folder.resolve("sessao-terminal.properties")).load();
            var mateRow=mateSession.logRows().getLast().split("\t");
            check(mateRow[2].equals("d8h4") && mateRow[3].equals("1") && Integer.parseInt(mateRow[4])>0
                && mateRow[5].equals("mate 1") && mateRow[9].equals("mate_java"),"Métricas da busca Java");
            new TerminalGame(new BufferedReader(new StringReader("armageddon pretas 0\nsair\n")),new PrintWriter(new StringWriter(),true),folder).run();
            var armageddon=new SessionStore(folder.resolve("sessao-terminal.properties")).load();
            check(armageddon.clock().equals("04:00") && armageddon.increment()==0 && armageddon.armageddon(),"Armageddon preto com incremento informado");
            new TerminalGame(new BufferedReader(new StringReader("restaurar\n03:25\nencerrar 1/2-1/2 03:25 04:30\n")),new PrintWriter(new StringWriter(),true),folder).run();
            check(Files.readString(folder.resolve("resultado-"+armageddon.matchId()+".txt")).contains("pretas classificadas"),
                "Armageddon restaurado conserva regra de classificação por empate");
            new TerminalGame(new BufferedReader(new StringReader("armageddon brancas 0\nsair\n")),new PrintWriter(new StringWriter(),true),folder,1).run();
            check(new SessionStore(folder.resolve("sessao-terminal.properties")).load().clock().equals("05:00"),
                "Armageddon branco começa com cinco minutos sem confirmar lance pendente");
            StringWriter ending=new StringWriter();
            new TerminalGame(new BufferedReader(new StringReader("pretas\nencerrar 0-1 errado 01:12\nencerrar 0-1 02:34 00:00\n")),new PrintWriter(ending,true),folder).run();
            var completed=new SessionStore(folder.resolve("sessao-terminal.properties")).load();
            check(completed.clock().equals("02:34") && completed.moves().isEmpty(),"Encerramento registra saldo físico sem inventar lance");
            String report=Files.readString(folder.resolve("resultado-"+completed.matchId()+".txt"));
            check(report.contains("154000") && report.contains("brancas-pretas): 0-1"),"Resultado com cor e saldo para desempate");
            check(ending.toString().contains("Tempos inválidos"),"Encerramento inválido permite nova tentativa");
            StringWriter closed=new StringWriter();
            new TerminalGame(new BufferedReader(new StringReader("restaurar\nsair\n")),new PrintWriter(closed,true),folder).run();
            check(closed.toString().contains("Partida já encerrada"),"Partida encerrada preservada na restauração");
            check(TimeManager.INITIAL_MILLIS==900_000 && TimeManager.INCREMENT_MILLIS==10_000,"Ritmo exigido pelo FAQ");
            new TerminalGame(new BufferedReader(new StringReader("brancas\nfeito\nsair\n")),new PrintWriter(new StringWriter(),true),folder,1).run();
            var fast=new SessionStore(folder.resolve("sessao-terminal.properties")).load();
            String[] fastMetrics=fast.logRows().getFirst().split("\t");
            for(int column=3;column<=7;column++) check(!fastMetrics[column].equals("NA"),"Log completo mesmo com cálculo de 1 ms");
            try(var files=Files.list(folder)) { check(files.anyMatch(p -> p.toString().endsWith(".pgn")),"Exportação PGN"); }
            System.out.println("OK: comandos físicos, roque, en passant, promoção, lance ilegal, motor real, confirmação, PGN e restauração no terminal.");
            System.out.println(transcript.lines().filter(line -> line.contains("MOVA")).findFirst().orElseThrow());
        } finally {
            try(var files=Files.list(folder)) { for(Path file:files.toList()) Files.deleteIfExists(file); }
            Files.deleteIfExists(folder);
        }
    }
}
