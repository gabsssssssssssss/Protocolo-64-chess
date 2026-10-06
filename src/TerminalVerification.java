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
        check(TerminalGame.parseClock("25:00")==1500000,"Tempo físico");
        Path folder=Files.createTempDirectory("terminal-verification-");
        try {
            StringWriter buffer=new StringWriter();
            new TerminalGame(new BufferedReader(new StringReader("pretas\ne2e5\ne2e4\nfeito\nsalvar\nsair\n")),new PrintWriter(buffer,true),folder,10000).run();
            String transcript=buffer.toString();
            check(transcript.contains("Lance inválido") && transcript.contains("MOVA"),"Fluxo terminal com motor real");
            var saved=new SessionStore(folder.resolve("sessao-terminal.properties")).load();
            check(saved.moves().size()==2 && saved.color()==Side.BLACK,"Sessão deve conter adversário e nosso lance");
            StringWriter restore=new StringWriter();
            new TerminalGame(new BufferedReader(new StringReader("restaurar\nsair\n")),new PrintWriter(restore,true),folder).run();
            check(!restore.toString().contains("Não foi possível restaurar"),"Restauração terminal");
            try(var files=Files.list(folder)) { check(files.anyMatch(p -> p.toString().endsWith(".pgn")),"Exportação PGN"); }
            System.out.println("OK: comandos físicos, roque, en passant, promoção, lance ilegal, motor real, confirmação, PGN e restauração no terminal.");
            System.out.println(transcript.lines().filter(line -> line.contains("MOVA")).findFirst().orElseThrow());
        } finally {
            try(var files=Files.list(folder)) { for(Path file:files.toList()) Files.deleteIfExists(file); }
            Files.deleteIfExists(folder);
        }
    }
}
