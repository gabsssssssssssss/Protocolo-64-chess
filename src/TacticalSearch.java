import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.Optional;

/** Busca exata de profundidade 1: escolhe somente mates confirmados pelas regras. */
public final class TacticalSearch {
    private TacticalSearch() {}
    public static Optional<Move> mateInOne(Board position) {
        if(position.isDraw() || position.isMated() || position.isStaleMate()) return Optional.empty();
        Board candidate=new Board(); candidate.loadFromFen(position.getFen());
        for(Move move:position.legalMoves()) {
            if(!candidate.doMove(move,true)) throw new IllegalStateException("Falha na simulação de lance legal.");
            boolean mate=candidate.isMated();
            candidate.undoMove();
            if(mate) return Optional.of(move);
        }
        return Optional.empty();
    }
}
