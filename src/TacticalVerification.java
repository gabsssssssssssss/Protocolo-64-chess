import com.github.bhlangonijr.chesslib.*;
import java.util.*;

public final class TacticalVerification {
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        String[] mates={
            "7k/5Q2/6K1/8/8/8/8/8 w - - 0 1",
            "8/8/8/8/8/6k1/5q2/7K b - - 0 1"
        };
        for(String mate:mates) {
            Board board=new Board(); board.loadFromFen(mate);
            String fen=board.getFen(); var history=List.copyOf(board.getHistory());
            var choice=TacticalSearch.mateInOne(board).orElseThrow();
            check(board.getFen().equals(fen) && history.equals(board.getHistory()),"Busca não deve modificar a partida");
            check(board.legalMoves().contains(choice),"Seleção deve ser legal");
            board.doMove(choice,true); check(board.isMated(),"Seleção deve dar mate");
        }
        check(TacticalSearch.mateInOne(new Board()).isEmpty(),"Abertura deve ser delegada ao Stockfish");
        Board stalemate=new Board(); stalemate.loadFromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        check(stalemate.isStaleMate() && TacticalSearch.mateInOne(stalemate).isEmpty(),"Afogamento não é vitória");
        Board promotion=new Board(); promotion.loadFromFen("7k/5P2/6K1/8/8/8/8/8 w - - 0 1");
        var promoted=TacticalSearch.mateInOne(promotion).orElseThrow();
        check(promoted.getPromotion()!=Piece.NONE,"Busca deve considerar promoções");
        promotion.doMove(promoted,true); check(promotion.isMated(),"Promoção escolhida deve dar mate");
        Board capture=new Board(); capture.loadFromFen("4k3/8/8/8/8/8/q7/R3K3 w - - 0 1");
        String captureFen=capture.getFen();
        var captureHistory=List.copyOf(capture.getHistory());
        var assessed=TacticalSearch.assessMove(capture,TerminalGame.parseMove(capture,"a1a2"));
        check(assessed.centipawns()==500 && assessed.depth()==1 && assessed.nodes()==1
            && assessed.line().equals(List.of("a1a2")) && assessed.timeMillis()>=0,"Avaliação material local mede a folha real");
        check(capture.getFen().equals(captureFen) && capture.getHistory().equals(captureHistory),"Avaliação de log não muda a partida");
        Board blackCapture=new Board(); blackCapture.loadFromFen("r3k3/Q7/8/8/8/8/8/4K3 b - - 0 1");
        check(TacticalSearch.assessMove(blackCapture,TerminalGame.parseMove(blackCapture,"a8a7")).centipawns()==500,
            "Avaliação local na perspectiva da cor que jogou");
        Board mateAssessment=new Board(); mateAssessment.loadFromFen(mates[0]);
        check(TacticalSearch.assessMove(mateAssessment,TacticalSearch.mateInOne(mateAssessment).orElseThrow()).mate()==1,
            "Log local distingue mate de avaliação material");
        System.out.println("OK: seleção própria de mate com ambas as cores, promoção, rejeição de afogamento e preservação da partida.");
    }
}
