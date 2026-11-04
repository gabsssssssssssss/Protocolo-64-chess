import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;
import java.nio.file.*;
import java.util.*;

/** Compara a política usada nas partidas com orçamento fixo, sem relógio total. */
public final class PositionMatchRunner {
    static final String[][] OPENINGS={
        {"Aberta","e2e4","e7e5","g1f3","b8c6"},
        {"Siciliana","e2e4","c7c5","g1f3","d7d6"},
        {"Gambito da dama","d2d4","d7d5","c2c4","e7e6"},
        {"Indiana","d2d4","g8f6","c2c4","e7e6"},
        {"Francesa","e2e4","e7e6","d2d4","d7d5"},
        {"Caro-Kann","e2e4","c7c6","d2d4","d7d5"}
    };
    static long budget(Board board,long ceiling,boolean own) {
        if(!own) return ceiling;
        // Escala toda a política, em vez de limitar igualmente todos os casos.
        return Math.max(1,(TimeManager.allocateForPosition(board).millis()*ceiling+29999)/30000);
    }
    static String result(Board board) {
        if(board.isMated()) return board.getSideToMove()==Side.WHITE ? "0-1" : "1-0";
        return board.isStaleMate() || board.isDraw() ? "1/2-1/2" : "*";
    }
    static Move legal(Board board,String uci) {
        return board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(uci)).findFirst()
            .orElseThrow(() -> new IllegalStateException("Lance ilegal: "+uci));
    }
    static void saveGame(Path path,MoveList moves,Side custom,String opening,String result,String termination,long ceiling) throws Exception {
        String ours="Stockfish + politica por posicao",fixed="Stockfish + orcamento fixo";
        String text="[Event \"Teste experimental por posicao\"]\n[White \""+(custom==Side.WHITE ? ours : fixed)
            +"\"]\n[Black \""+(custom==Side.BLACK ? ours : fixed)+"\"]\n[Result \""+result
            +"\"]\n[Opening \""+opening+"\"]\n[TimeControl \"-\"]\n[SearchCeilingMs \""+ceiling
            +"\"]\n[Termination \""+termination+"\"]\n\n"+moves.toSanWithMoveNumbers()+" "+result+"\n";
        Files.writeString(path,text);
    }
    public static Path run(long ceiling,int openingCount,int maxPlies,Path folder) throws Exception {
        if(ceiling<1 || ceiling>30000 || openingCount<1 || openingCount>OPENINGS.length || maxPlies<5 || maxPlies>400)
            throw new IllegalArgumentException("Use teto 1..30000 ms, 1..6 aberturas e limite 5..400 meios-lances.");
        Files.createDirectories(folder);
        Files.writeString(folder.resolve("configuracao.txt"),"politica=mate-em-1+por-posicao-v2\nbaseline=tempo-fixo\nteto_ms="+ceiling
            +"\naberturas="+openingCount+"\nlimite_meios_lances="+maxPlies
            +"\nthreads="+Stockfish.setting("chess.threads",Math.min(4,Math.max(1,Runtime.getRuntime().availableProcessors()/2)),1,Math.max(1,Runtime.getRuntime().availableProcessors()))
            +"\nhash_mb="+Stockfish.setting("chess.hashMb",512,16,2048)
            +"\nMesmo teto por lance; a politica adaptativa pode consumir menos calculo. Nao e igualdade de tempo total de CPU.\n");
        Path log=folder.resolve("lances.csv");
        Files.writeString(log,"partida,abertura,meio_lance,cor,politica,orcamento_ms,duracao_ms,lance\n");
        int wins=0,draws=0,losses=0,unfinished=0;
        long ownTime=0,fixedTime=0; int ownMoves=0,fixedMoves=0;
        for(int opening=0;opening<openingCount;opening++) for(int pair=0;pair<2;pair++) {
            int game=opening*2+pair+1;
            Side custom=pair==0 ? Side.WHITE : Side.BLACK;
            Board board=new Board(); MoveList moves=new MoveList(); ArrayList<String> uci=new ArrayList<>();
            for(int i=1;i<OPENINGS[opening].length;i++) {
                String text=OPENINGS[opening][i]; Move move=legal(board,text); board.doMove(move,true); moves.add(move); uci.add(text);
            }
            Path pgn=folder.resolve("partida-"+game+".pgn");
            saveGame(pgn,moves,custom,OPENINGS[opening][0],"*","Em andamento",ceiling);
            try(Stockfish white=new Stockfish(Stockfish.locate()); Stockfish black=new Stockfish(Stockfish.locate())) {
                while(uci.size()<maxPlies && result(board).equals("*")) {
                    Side side=board.getSideToMove(); boolean own=side==custom;
                    long allocation=budget(board,ceiling,own),start=System.nanoTime();
                    var mate=own ? TacticalSearch.mateInOne(board) : Optional.<Move>empty();
                    String reply=mate.isPresent() ? mate.get().toString().toLowerCase(Locale.ROOT)
                        : (side==Side.WHITE ? white : black).play(String.join(" ",uci),allocation);
                    long elapsed=(System.nanoTime()-start+999999)/1000000;
                    Move move=legal(board,reply); board.doMove(move,true); moves.add(move); uci.add(reply);
                    if(own) { ownTime+=elapsed; ownMoves++; } else { fixedTime+=elapsed; fixedMoves++; }
                    Files.writeString(log,game+","+OPENINGS[opening][0]+","+uci.size()+","+side+","+(own ? "posicao" : "fixo")+","+allocation+","+elapsed+","+reply+"\n",StandardOpenOption.APPEND);
                    saveGame(pgn,moves,custom,OPENINGS[opening][0],result(board),result(board).equals("*") ? "Em andamento" : "Fim pelas regras",ceiling);
                }
            } catch(Exception e) {
                saveGame(pgn,moves,custom,OPENINGS[opening][0],"*","Falha operacional",ceiling);
                throw e;
            }
            String score=result(board);
            if(score.equals("*")) unfinished++;
            else if(score.equals("1/2-1/2")) draws++;
            else if(score.equals(custom==Side.WHITE ? "1-0" : "0-1")) wins++;
            else losses++;
            saveGame(pgn,moves,custom,OPENINGS[opening][0],score,score.equals("*") ? "Limite de meios-lances; sem resultado" : "Fim pelas regras",ceiling);
            String summary="vitorias="+wins+"\nempates="+draws+"\nderrotas="+losses+"\ninacabadas="+unfinished
                +"\ncalculo_posicao_ms="+ownTime+"\ncalculo_fixo_ms="+fixedTime+"\nlances_posicao="+ownMoves+"\nlances_fixo="+fixedMoves
                +"\nPartidas inacabadas nao contam como empate. Amostra pequena nao comprova ganho de forca ou Elo.\n";
            Files.writeString(folder.resolve("resumo.txt"),summary);
            System.out.println("Partida "+game+"/"+(openingCount*2)+" — "+OPENINGS[opening][0]+"; nossa cor="+custom+"; resultado="+score);
        }
        System.out.println("Relatorios: "+folder.toAbsolutePath());
        return folder;
    }
    public static void main(String[] args) throws Exception {
        long ceiling=args.length>0 ? Long.parseLong(args[0]) : 1000;
        int openings=args.length>1 ? Integer.parseInt(args[1]) : 3;
        int maxPlies=args.length>2 ? Integer.parseInt(args[2]) : 200;
        run(ceiling,openings,maxPlies,Path.of("partidas","teste-posicao-"+System.currentTimeMillis()));
    }
}
