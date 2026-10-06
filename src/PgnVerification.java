import com.github.bhlangonijr.chesslib.*;
import java.nio.file.*;

public final class PgnVerification {
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var examples=PgnCollection.read(Path.of("datasets/exemplo.pgn"));
        var historical=PgnCollection.read(Path.of("datasets/historicas.pgn")).getFirst();
        check(historical.san().size()==33,"Partida histórica completa: 33 meios-lances");
        Board historicalBoard=new Board(); historicalBoard.loadFromFen(historical.positions().getLast());
        check(historicalBoard.isMated(),"Partida histórica deve terminar em xeque-mate");
        check(examples.size()==2,"Coleção com várias partidas");
        Board board=new Board(); board.loadFromFen(examples.getFirst().positions().getLast());
        check(board.isMated(),"Reprodução deve alcançar o mate");
        board.loadFromFen(examples.get(1).positions().getLast());
        check(board.getPiece(Square.G1)==Piece.WHITE_KING && board.getPiece(Square.F1)==Piece.WHITE_ROOK,"Roque na reprodução");
        Path file=Files.createTempFile("pgn-verification-",".pgn");
        try {
            Files.writeString(file,"\uFEFF[Event \"Anotações\"]\n[White \"Teste\"]\n[Black \"Teste\"]\n[Result \"*\"]\n\n1. e4 {comentário} e5 (1... c5) 2. Nf3 $1 Nc6 *\n");
            var annotated=PgnCollection.read(file).getFirst();
            check(annotated.san().size()==4 && annotated.san().get(1).equals("e5"),"Ignorar variantes, NAG e comentários");
            Files.writeString(file,"[Event \"Promoção\"]\n[White \"Teste\"]\n[Black \"Teste\"]\n[Result \"*\"]\n[SetUp \"1\"]\n[FEN \"4k3/P7/8/8/8/8/8/4K3 w - - 0 1\"]\n\n1. a8=N *\n");
            var fen=PgnCollection.read(file).getFirst(); board.loadFromFen(fen.positions().getLast());
            check(board.getPiece(Square.A8)==Piece.WHITE_KNIGHT,"FEN e subpromoção");
            Files.writeString(file,"[Event \"Inválida\"]\n[Result \"*\"]\n\n1. e5 *\n");
            boolean rejected=false; try { PgnCollection.read(file); } catch(Exception expected) { rejected=true; }
            check(rejected,"Lance inválido deve impedir importação");
            Files.writeString(file,""); rejected=false; try { PgnCollection.read(file); } catch(Exception expected) { rejected=true; }
            check(rejected,"Arquivo vazio deve ser rejeitado");
        } finally { Files.deleteIfExists(file); }
        System.out.println("OK: PGN múltiplo, mate, roque, UTF-8/BOM, comentários, variantes, NAG, FEN, subpromoção e arquivos inválidos.");
    }
}
