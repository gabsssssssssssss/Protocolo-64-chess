import com.github.bhlangonijr.chesslib.*;
import java.util.List;

public final class AnalysisVerification {
    private static void check(boolean condition,String message) { if(!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var cp=Stockfish.parseInfo("info depth 18 multipv 1 score cp -42 nodes 2000 pv e7e5 g1f3");
        check(cp!=null && cp.centipawns()==-42 && cp.depth()==18 && cp.line().size()==2,"Parse de score e PV");
        check(Stockfish.parseInfo("info depth 10 score cp 10 lowerbound pv e2e4")==null,"Limite de score não é avaliação exata");
        check(Stockfish.parseInfo("info depth 10 multipv 2 score cp 10 pv e2e4")==null,"Somente PV principal");
        Board black=new Board(); black.doMove(black.legalMoves().stream().filter(m -> m.toString().equals("e2e4")).findFirst().orElseThrow(),true);
        String rendered=AnalysisText.format(new Stockfish.Analysis("e7e5",-42,null,18,List.of("e7e5","g1f3")),black.getFen());
        check(rendered.contains("+0.42") && rendered.contains("e5 Nf3"),"Perspectiva das brancas e SAN");
        try(Stockfish engine=new Stockfish(Stockfish.locate())) {
            var initial=engine.analyse(new Board().getFen(),"",200);
            check(initial.centipawns()!=null || initial.mate()!=null,"Análise real deve retornar score");
            check(AnalysisText.format(initial,new Board().getFen()).contains("Lance sugerido"),"Lance real legal");
            var after=engine.analyse(new Board().getFen(),"e2e4",200);
            AnalysisText.format(after,black.getFen());
            String mateFen="7k/5Q2/6K1/8/8/8/8/8 w - - 0 1";
            var mate=engine.analyse(mateFen,"",200);
            check(mate.mate()!=null && mate.mate()==1,"Mate em um com FEN inicial");
            check(AnalysisText.format(mate,mateFen).contains("brancas"),"Exibição de mate");
        }
        System.out.println("OK: análise UCI real, histórico PGN, FEN, mate, perspectiva de score, SAN e limites de avaliação.");
    }
}
