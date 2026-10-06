import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;
import java.nio.file.*;
import java.util.*;

/** Comparação local, com cores invertidas e mesma abertura. Não estima Elo. */
public final class MatchRunner {
    public static void main(String[] args) throws Exception {
        long initial = args.length > 0 ? Long.parseLong(args[0])*1000 : 30000;
        if(initial<1000 || initial>1500000) throw new IllegalArgumentException("Use de 1 a 1500 segundos por jogador.");
        MatchViewer viewer=Arrays.asList(args).contains("--watch") ? new MatchViewer() : null;
        Path folder=Path.of("partidas","comparacao-"+System.currentTimeMillis());
        Files.createDirectories(folder);
        StringBuilder log=new StringBuilder("partida,ply,cor,politica,orcamento_ms,duracao_ms,restante_ms,lance\n");
        for(int game=1;game<=2;game++) {
            Side custom=game==1 ? Side.WHITE : Side.BLACK;
            Board board=new Board();
            MoveList moves=new MoveList();
            ArrayList<String> uci=new ArrayList<>();
            long[] clock={initial,initial};
            String result="*", termination="Limite de segurança de 400 meios-lances";
            for(String opening:new String[]{"e2e4","e7e5","g1f3","b8c6"}) {
                Move move=legal(board,opening); board.doMove(move,true); moves.add(move); uci.add(opening);
            }
            if(viewer!=null) viewer.update(board.getFen(),game,custom,clock[0],clock[1],"Abertura: 1. e4 e5 2. Nf3 Nc6. Preparando motores...");
            try(Stockfish white=new Stockfish(Stockfish.locate()); Stockfish black=new Stockfish(Stockfish.locate())) {
                for(int ply=4;ply<400;ply++) {
                    if(board.isMated()) { result=board.getSideToMove()==Side.WHITE ? "0-1" : "1-0"; termination="Xeque-mate"; break; }
                    if(board.isStaleMate() || board.isDraw()) { result="1/2-1/2"; termination="Empate detectado pela biblioteca"; break; }
                    Side side=board.getSideToMove(); int index=side==Side.WHITE ? 0 : 1;
                    if(clock[index]<=0) { result=index==0 ? "0-1" : "1-0"; termination="Tempo esgotado"; break; }
                    boolean own=side==custom;
                    long budget=own ? TimeManager.allocate(board,clock[index]).millis() : 0;
                    Stockfish engine=index==0 ? white : black;
                    if(viewer!=null) viewer.update(board.getFen(),game,custom,clock[0],clock[1],"Analisando: "+side+" — "+(own ? "gestão própria" : "gestão original"));
                    long start=System.nanoTime();
                    String reply=own ? engine.play(String.join(" ",uci),budget) : engine.playWithClock(String.join(" ",uci),clock[0],clock[1]);
                    long elapsed=(System.nanoTime()-start+999999)/1000000;
                    clock[index]-=elapsed;
                    log.append(String.format(Locale.ROOT,"%d,%d,%s,%s,%d,%d,%d,%s%n",game,ply+1,side,own ? "propria" : "stockfish",budget,elapsed,clock[index],reply));
                    if(clock[index]<=0) { result=index==0 ? "0-1" : "1-0"; termination="Tempo esgotado"; break; }
                    Move move=legal(board,reply);
                    board.doMove(move,true); moves.add(move); uci.add(reply);
                    if(viewer!=null) viewer.update(board.getFen(),game,custom,clock[0],clock[1],"Último lance: "+reply+" — "+elapsed+" ms");
                }
            }
            if(result.equals("*") && board.isMated()) { result=board.getSideToMove()==Side.WHITE ? "0-1" : "1-0"; termination="Xeque-mate"; }
            String ownName="Stockfish + gestão própria", baseName="Stockfish original";
            StringBuilder pgn=new StringBuilder("[Event \"Comparação local experimental\"]\n[White \""+(custom==Side.WHITE ? ownName : baseName)+"\"]\n[Black \""+(custom==Side.BLACK ? ownName : baseName)+"\"]\n[Result \""+result+"\"]\n[TimeControl \""+initial/1000+"\"]\n[Termination \""+termination+"\"]\n\n");
            String[] san=moves.toSanArray();
            for(int i=0;i<san.length;i++) { if(i%2==0) pgn.append(i/2+1).append(". "); pgn.append(san[i]).append(' '); }
            Files.writeString(folder.resolve("partida-"+game+".pgn"),pgn.append(result).append('\n'));
            Files.writeString(folder.resolve("tempos.csv"),log);
            System.out.println("Partida "+game+": "+result+"; "+termination+"; gestão própria com "+custom+"; "+moves.size()+" meios-lances.");
            if(viewer!=null) viewer.update(board.getFen(),game,custom,clock[0],clock[1],"Resultado: "+result+" — "+termination+(game==2 ? ". Confronto concluído." : ""));
        }
        System.out.println("PGNs e tempos: "+folder.toAbsolutePath());
        System.out.println("Apenas teste de integração: duas partidas rápidas não demonstram superioridade.");
    }
    private static Move legal(Board board,String text) {
        return board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(text)).findFirst()
            .orElseThrow(() -> new IllegalStateException("Lance ilegal do motor: "+text+" na posição "+board.getFen()));
    }
}
