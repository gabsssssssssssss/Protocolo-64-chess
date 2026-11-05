import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.Optional;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** Busca exata de profundidade 1: escolhe somente mates confirmados pelas regras. */
public final class TacticalSearch {
    private TacticalSearch() {}
    record Probe(Optional<Move> move,int nodes) {}
    public static Optional<Move> mateInOne(Board position) {
        return probe(position).move();
    }
    static Probe probe(Board position) {
        if(position.isDraw() || position.isMated() || position.isStaleMate()) return new Probe(Optional.empty(),0);
        Board candidate=new Board(); candidate.loadFromFen(position.getFen());
        int nodes=0;
        for(Move move:position.legalMoves()) {
            nodes++;
            if(!candidate.doMove(move,true)) throw new IllegalStateException("Falha na simulação de lance legal.");
            boolean mate=candidate.isMated();
            candidate.undoMove();
            if(mate) return new Probe(Optional.of(move),nodes);
        }
        return new Probe(Optional.empty(),nodes);
    }
    /** Avaliação local do lance informado: uma posição folha, sem escolher pelo adversário. */
    static Stockfish.Analysis assessMove(Board position,Move move) {
        long started=System.nanoTime();
        Board leaf=new Board();
        leaf.loadFromFen(position.getFen());
        if(!leaf.doMove(move,true)) throw new IllegalArgumentException("Lance ilegal para avaliação local.");
        int material=0;
        for(Square square:Square.values()) {
            if(square==Square.NONE) continue;
            Piece piece=leaf.getPiece(square);
            int value=switch(piece) {
                case WHITE_PAWN,BLACK_PAWN -> 100;
                case WHITE_KNIGHT,BLACK_KNIGHT -> 320;
                case WHITE_BISHOP,BLACK_BISHOP -> 330;
                case WHITE_ROOK,BLACK_ROOK -> 500;
                case WHITE_QUEEN,BLACK_QUEEN -> 900;
                default -> 0;
            };
            material+=piece.getPieceSide()==Side.WHITE ? value : -value;
        }
        Integer mate=leaf.isMated() ? 1 : null;
        Integer score=mate!=null ? null : leaf.isDraw() || leaf.isStaleMate() ? 0
            : position.getSideToMove()==Side.WHITE ? material : -material;
        String uci=move.toString().toLowerCase(Locale.ROOT);
        return new Stockfish.Analysis(uci,score,mate,1,List.of(uci),1L,
            TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started));
    }
}
