import com.github.bhlangonijr.chesslib.*;
import java.util.*;

/** Formata a análise sem alterar a posição que está sendo estudada. */
public final class AnalysisText {
    public static String format(Stockfish.Analysis result,String fen) {
        Board board=new Board(); board.loadFromFen(fen);
        StringBuilder text=new StringBuilder();
        int sign=board.getSideToMove()==Side.WHITE ? 1 : -1;
        if(result.mate()!=null) {
            int whiteMate=result.mate()*sign;
            text.append("Mate previsto para ").append(whiteMate>=0 ? "brancas" : "pretas").append(" em ").append(Math.abs(result.mate())).append(" lance(s).");
        } else if(result.centipawns()!=null) text.append(String.format(Locale.ROOT,"Avaliação (brancas): %+.2f",result.centipawns()*sign/100.0));
        else text.append("Avaliação indisponível nesta análise.");
        String best=board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(result.bestMove())).findFirst()
            .map(m -> { var list=new com.github.bhlangonijr.chesslib.move.MoveList(fen); list.add(m); return list.toSanArray()[0]; })
            .orElseThrow(() -> new IllegalArgumentException("O motor não retornou um lance legal para esta posição."));
        text.append("\nLance sugerido: ").append(best).append(" (").append(result.bestMove()).append(")");
        text.append("\nProfundidade: ").append(result.depth()).append(" meios-lances");
        var variation=new com.github.bhlangonijr.chesslib.move.MoveList(fen);
        for(String uci:result.line()) {
            var move=board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(uci)).findFirst().orElse(null);
            if(move==null) break; variation.add(move); board.doMove(move,true);
            if(variation.size()>=10) break;
        }
        if(!variation.isEmpty()) text.append("\nLinha principal: ").append(String.join(" ",variation.toSanArray()));
        text.append("\nEstimativa local; o lance histórico permanece no tabuleiro.");
        return text.toString();
    }
}
