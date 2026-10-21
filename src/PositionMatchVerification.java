import com.github.bhlangonijr.chesslib.*;
import java.nio.file.*;

public final class PositionMatchVerification {
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        for(var opening:PositionMatchRunner.OPENINGS) {
            Board board=new Board();
            for(int i=1;i<opening.length;i++) board.doMove(PositionMatchRunner.legal(board,opening[i]),true);
        }
        Board start=new Board();
        check(PositionMatchRunner.budget(start,30000,true)==TimeManager.allocateForPosition(start).millis(),"Teto normal deve executar a politica real");
        check(PositionMatchRunner.budget(start,1000,true)==400,"Escala deve preservar diferenças entre orçamentos");
        check(PositionMatchRunner.budget(start,1000,false)==1000,"Baseline deve usar teto fixo");
        Path folder=Files.createTempDirectory("position-match-");
        try {
            PositionMatchRunner.run(30,1,5,folder);
            var first=PgnCollection.read(folder.resolve("partida-1.pgn")).getFirst();
            var second=PgnCollection.read(folder.resolve("partida-2.pgn")).getFirst();
            check(first.white().equals(second.black()) && first.black().equals(second.white()),"Cores invertidas");
            check(first.uci().subList(0,4).equals(second.uci().subList(0,4)),"Mesma abertura");
            check(first.result().equals("*") && second.result().equals("*"),"Partidas incompletas não são empates");
            String summary=Files.readString(folder.resolve("resumo.txt"));
            check(summary.contains("empates=0") && summary.contains("inacabadas=2"),"Resumo não deve inventar resultados");
            check(Files.readAllLines(folder.resolve("lances.csv")).size()==3,"Auditoria dos lances calculados");
            System.out.println("OK: aberturas legais, escala da politica real, motores locais, cores alternadas, PGNs e resumo sem falsos empates.");
        } finally { try(var files=Files.list(folder)) { for(Path file:files.toList()) Files.delete(file); } Files.delete(folder); }
    }
}
