import com.github.bhlangonijr.chesslib.*;
import java.nio.file.*;
import java.util.*;

/** Diagnóstico tático pequeno e reproduzível; não estima Elo nem taxa geral de acerto. */
public final class StrengthVerification {
    record Position(String name,String fen,boolean mate,String expected) {}
    public static void main(String[] args) throws Exception {
        var positions=List.of(
            new Position("Mate com brancas","7k/5Q2/6K1/8/8/8/8/8 w - - 0 1",true,""),
            new Position("Mate com pretas","8/8/8/8/8/6k1/5q2/7K b - - 0 1",true,""),
            new Position("Torre captura dama: brancas","q5k1/8/8/8/8/8/8/R5K1 w - - 0 1",false,"a1a8"),
            new Position("Torre captura dama: pretas","r5k1/8/8/8/8/8/8/Q5K1 b - - 0 1",false,"a8a1")
        );
        StringBuilder report=new StringBuilder("posicao,lance,profundidade,aprovado\n");
        int passed=0;
        try(Stockfish engine=new Stockfish(Stockfish.locate())) {
            for(var p:positions) {
                engine.newGame();
                var analysis=engine.analyse(p.fen(),"",2000);
                Board board=new Board(); board.loadFromFen(p.fen());
                var move=board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(analysis.bestMove())).findFirst().orElseThrow();
                board.doMove(move,true);
                boolean ok=p.mate() ? board.isMated() : analysis.bestMove().equals(p.expected());
                if(ok) passed++;
                report.append(p.name()).append(',').append(analysis.bestMove()).append(',').append(analysis.depth()).append(',').append(ok).append('\n');
                System.out.println((ok ? "OK: " : "FALHOU: ")+p.name()+" — "+analysis.bestMove());
            }
        }
        Files.createDirectories(Path.of("partidas"));
        Path output=Path.of("partidas","diagnostico-tatico-"+System.currentTimeMillis()+".csv");
        Files.writeString(output,report);
        System.out.println(passed+"/"+positions.size()+" posições; relatório: "+output.toAbsolutePath());
        System.out.println("Este teste pequeno não representa precisão geral nem ganho de força.");
        if(passed!=positions.size()) throw new AssertionError("Falha no diagnóstico tático.");
    }
}
