import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;
import java.nio.file.*;

/** Testes de integração executáveis sem JUnit. */
public final class Verification {
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    private static Move legal(Board b,String uci) {
        return b.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(uci)).findFirst().orElseThrow();
    }
    private static long perft(Board b,int depth) {
        if(depth==0) return 1;
        long total=0;
        for(Move move:b.legalMoves()) { b.doMove(move,true); total+=perft(b,depth-1); b.undoMove(); }
        return total;
    }
    public static void main(String[] args) throws Exception {
        Board b=new Board();
        check(perft(b,3)==8902,"Geração de lances: perft inicial");
        b.loadFromFen("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        b.doMove(legal(b,"e1g1"),true);
        check(b.getPiece(Square.F1)==Piece.WHITE_ROOK && b.getPiece(Square.G1)==Piece.WHITE_KING,"Roque");
        b.loadFromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2");
        b.doMove(legal(b,"e5d6"),true);
        check(b.getPiece(Square.D5)==Piece.NONE && b.getPiece(Square.D6)==Piece.WHITE_PAWN,"En passant");
        b.loadFromFen("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        b.doMove(legal(b,"a7a8n"),true);
        check(b.getPiece(Square.A8)==Piece.WHITE_KNIGHT,"Subpromoção");
        b=new Board();
        MoveList list=new MoveList();
        for(String text:new String[]{"f2f3","e7e5","g2g4","d8h4"}) { Move move=legal(b,text); list.add(move); b.doMove(move,true); }
        check(b.isMated(),"Detecção de mate");
        check(list.toSanArray()[3].equals("Qh4#"),"Exportação SAN");
        b=new Board();
        for(String move:new String[]{"g1f3","g8f6","f3g1","f6g8","g1f3","g8f6","f3g1","f6g8"}) b.doMove(legal(b,move),true);
        check(b.isDraw(),"Empate por tripla repetição com histórico completo");
        b.loadFromFen("4k3/8/8/8/8/8/8/R3K3 w - - 99 1");
        b.doMove(legal(b,"a1a2"),true);
        check(b.isDraw(),"Empate pela regra dos 50 lances");
        b=new Board();
        for(long remaining:new long[]{1,50,1000,10000,1500000}) {
            long allocation=TimeManager.allocate(b,remaining).millis();
            check(allocation>0 && allocation<=remaining,"Orçamento excede relógio");
        }
        System.out.println("OK: perft, roque, en passant, subpromoção, mate, SAN e limites de tempo.");
        Path temporary=Files.createTempDirectory("chess-session-test-");
        Path file=temporary.resolve("sessao.properties");
        try {
            SessionStore store=new SessionStore(file);
            Board position=new Board();
            Move first=legal(position,"e2e4");
            store.save(Side.BLACK,"23:41",java.util.List.of(first));
            var restored=store.load();
            check(restored.color()==Side.BLACK && restored.clock().equals("23:41") && restored.moves().equals(java.util.List.of(first)),"Persistência de sessão");
            boolean invalidClock=false;
            try { store.save(Side.WHITE,"tempo inválido",java.util.List.of()); } catch(java.io.IOException expected) { invalidClock=true; }
            check(invalidClock && store.load().moves().size()==1,"Falha de salvamento deve preservar sessão anterior");
            Files.writeString(file,"version=1\ncolor=WHITE\nclock=25:00\nmoves=e2e5\n");
            boolean rejected=false;
            try { store.load(); } catch(java.io.IOException expected) { rejected=true; }
            check(rejected,"Sessão corrompida deve ser rejeitada");
            System.out.println("OK: salvamento, restauração e rejeição de sessão inválida.");
        } finally { Files.deleteIfExists(file); Files.deleteIfExists(temporary); }
        if(args.length>0 && args[0].equals("--engine")) {
            try(Stockfish engine=new Stockfish(Stockfish.locate())) {
                for(String text:new String[]{"","e2e4","e2e4 e7e5 g1f3"}) {
                    Board position=new Board();
                    if(!text.isBlank()) for(String move:text.split(" ")) position.doMove(legal(position,move),true);
                    String result=engine.play(text,200);
                    legal(position,result);
                    System.out.println("OK: Stockfish retornou lance legal "+result);
                }
                engine.newGame();
            }
        }
    }
}
