import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.Optional;

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
}
