import com.github.bhlangonijr.chesslib.*;
import java.nio.file.*;
import java.util.*;

/** Exercita diferenças de decisão e preservação do tabuleiro, sem esperar os orçamentos. */
public final class TimeManagerVerification {
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    private static Board board(String fen) { Board b=new Board(); b.loadFromFen(fen); return b; }
    public static void main(String[] args) throws Exception {
        Board opening=new Board(); String original=opening.getFen(); var originalHistory=List.copyOf(opening.getHistory());
        check(TimeManager.allocateForPosition(opening).millis()==12000,"Abertura tranquila");
        check(opening.getFen().equals(original) && originalHistory.equals(opening.getHistory()),"Política não deve alterar o tabuleiro");
        Board middle=board(original.replace(" 0 1"," 0 15"));
        check(TimeManager.allocateForPosition(middle).millis()==16000,"Fase após abertura");
        Board end=board("7k/8/8/8/8/8/P7/K7 w - - 0 30");
        check(TimeManager.allocateForPosition(end).millis()==18000,"Final tranquilo");
        Board attack=board("7k/8/8/8/8/8/1R6/K7 w - - 0 30");
        String attackFen=attack.getFen();
        var attackHistory=List.copyOf(attack.getHistory());
        check(!attack.isKingAttacked() && TimeManager.allocateForPosition(attack).millis()==19000,
            "Rb8+ e Rh2+ devem acrescentar 1 segundo");
        check(attack.getFen().equals(attackFen) && attackHistory.equals(attack.getHistory()),
            "Simular xeques não deve alterar o tabuleiro ou histórico");
        Board checked=board("r6k/8/8/8/8/8/8/K7 w - - 0 30");
        check(TimeManager.allocateForPosition(checked).millis()>TimeManager.allocateForPosition(end).millis(),"Xeque deve aumentar orçamento quando há alternativas");
        Board forced=board("7k/8/8/8/8/8/1r6/K7 w - - 0 30");
        check(forced.legalMoves().size()==1 && TimeManager.allocateForPosition(forced).millis()==100,"Resposta forçada");
        Board ep=board("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 30");
        Board noEp=board("4k3/8/8/3pP3/8/8/8/4K3 w - - 0 30");
        check(TimeManager.allocateForPosition(ep).millis()>TimeManager.allocateForPosition(noEp).millis(),"En passant deve contar como captura");
        Board promotion=board("4k3/P7/8/8/8/8/8/4K3 w - - 0 30");
        check(TimeManager.allocateForPosition(promotion).millis()>=24000,"Promoção deve receber cálculo adicional");
        Board critical=board("r6k/1P6/8/8/8/8/8/K7 w - - 0 30");
        check(TimeManager.allocateForPosition(critical).millis()==30000,"Xeque, captura e promoção devem respeitar o teto combinado");
        Board mate=board("7k/6Q1/6K1/8/8/8/8/8 b - - 0 1");
        boolean rejected=false;
        try { TimeManager.allocateForPosition(mate); } catch(IllegalArgumentException expected) { rejected=true; }
        check(rejected,"Mate não deve receber orçamento");
        Board draw=board("7k/8/8/8/8/8/8/K7 w - - 0 30");
        rejected=false;
        try { TimeManager.allocateForPosition(draw); } catch(IllegalArgumentException expected) { rejected=true; }
        check(rejected,"Material insuficiente não deve receber orçamento");
        StringBuilder report=new StringBuilder("caso,orcamento_ms,motivo\n");
        var cases=new LinkedHashMap<String,Board>();
        cases.put("abertura",opening); cases.put("meio_jogo",middle); cases.put("final",end);
        cases.put("dois_xeques_disponiveis",attack);
        cases.put("xeque",checked); cases.put("resposta_forcada",forced); cases.put("en_passant",ep); cases.put("promocao",promotion); cases.put("teto_combinado",critical);
        for(var entry:cases.entrySet()) {
            var budget=TimeManager.allocateForPosition(entry.getValue());
            check(budget.millis()>0 && budget.millis()<=30000,"Limites por lance");
            report.append(entry.getKey()).append(',').append(budget.millis()).append(',').append(budget.reason()).append('\n');
        }
        Path output=Path.of("tmp/gestao-propria-exemplos.csv"); Files.createDirectories(output.getParent()); Files.writeString(output,report);
        System.out.println("OK: política por posição, fases, xeque, lance forçado, en passant, promoção e fim de partida. Exemplos: "+output);
    }
}
