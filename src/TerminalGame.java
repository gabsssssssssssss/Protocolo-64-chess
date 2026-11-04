import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Modo de operação no terminal, sem Swing, internet ou VS Code obrigatório. */
public final class TerminalGame {
    private final BufferedReader input;
    private final PrintWriter output;
    private final SessionStore store;
    private final Path folder;
    private final Board board=new Board();
    private final ArrayList<Move> moves=new ArrayList<>();
    private final ArrayList<String> logRows=new ArrayList<>();
    private UUID matchId=UUID.randomUUID();
    private long remaining=TimeManager.INITIAL_MILLIS;
    private long increment=TimeManager.INCREMENT_MILLIS;
    private long turnStarted;
    private Side ours;
    private long searchMillis=30_000;
    private Stockfish engine;
    private boolean quit;
    public TerminalGame(BufferedReader input,PrintWriter output,Path folder) { this.input=input; this.output=output; this.folder=folder; store=new SessionStore(folder.resolve("sessao-terminal.properties")); }
    TerminalGame(BufferedReader input,PrintWriter output,Path folder,long searchMillis) {
        this(input,output,folder);
        if(searchMillis<1 || searchMillis>30000) throw new IllegalArgumentException("Cálculo por lance inválido.");
        this.searchMillis=searchMillis;
    }
    private String ask(String prompt) throws IOException { output.print(prompt+" "); output.flush(); String value=input.readLine(); if(value==null) { quit=true; return "sair"; } return value.trim(); }
    public void run() throws Exception {
        try {
            engine=new Stockfish(Stockfish.locate());
            playMatch();
        } finally { if(ours!=null) checkpoint(); if(engine!=null) engine.close(); }
        output.flush();
    }
    private void playMatch() throws IOException {
        while(ours==null && !quit) {
            String answer=ask("Cor (brancas/pretas/restaurar) ou armageddon COR INCREMENTO:").toLowerCase(Locale.ROOT);
            if(answer.equals("sair")) { quit=true; return; }
            if(answer.equals("brancas") || answer.equals("pretas")) ours=answer.equals("brancas") ? Side.WHITE : Side.BLACK;
            else if(answer.matches("armageddon\\s+(brancas|pretas)\\s+[0-9]{1,3}")) {
                String[] fields=answer.split("\\s+");
                ours=fields[1].equals("brancas") ? Side.WHITE : Side.BLACK;
                remaining=ours==Side.WHITE ? 300_000 : 240_000;
                increment=1000L*Integer.parseInt(fields[2]);
            }
            else if(answer.equals("restaurar")) {
                try {
                    var saved=store.load();
                    long physical=readClock(ask("Tempo restante da nossa cor no relógio físico (MM:SS):"));
                    if(quit) return;
                    ours=saved.color();
                    remaining=physical;
                    increment=saved.increment();
                    if(saved.matchId()!=null) matchId=saved.matchId();
                    for(Move move:saved.moves()) {
                        logRows.add(saved.matchId()==null ? logRow(move,"sessao_antiga",null,null) : saved.logRows().get(moves.size()));
                        board.doMove(move,true); moves.add(move);
                    }
                } catch(Exception e) { output.println("Não foi possível restaurar: "+e.getMessage()); }
            } else output.println("Escolha brancas ou pretas.");
        }
        if(ours==null) return;
        output.println("Log da partida: "+logPath().toAbsolutePath());
        if(board.getSideToMove()==ours) turnStarted=System.nanoTime();
        while(!quit) {
            if(board.isMated() || board.isStaleMate() || board.isDraw()) { output.println("Fim: "+result()); break; }
            if(board.getSideToMove()!=ours) {
                String text=ask("Lance do adversário:");
                if(command(text)) continue;
                try {
                    String[] fields=text.split("\\s+");
                    if(!fields[0].matches("[a-h][1-8][a-h][1-8][qrbn]?") || fields.length>2)
                        throw new IllegalArgumentException("Use UCI.");
                    Move move=parseMove(board,fields[0]);
                    if(fields.length==2) remaining=readClock(fields[1]);
                    turnStarted=System.nanoTime();
                    record(move,"adversario",null,null);
                }
                catch(Exception e) { output.println("Lance inválido. Digite origem e destino, como e7e5."); }
            } else {
                long left=Math.max(0,remaining-elapsed(turnStarted));
                if(left==0) {
                    String clock=ask("Confira o relógio físico. Digite nosso tempo (MM:SS) ou sair:");
                    if(clock.equalsIgnoreCase("sair")) break;
                    try { remaining=readClock(clock); turnStarted=System.nanoTime(); }
                    catch(IllegalArgumentException e) { output.println("Tempo inválido."); }
                    continue;
                }
                Stockfish.Analysis analysis;
                String source;
                try {
                    long probeStart=System.nanoTime();
                    var mate=TacticalSearch.probe(board);
                    if(mate.move().isPresent()) {
                        String uci=uci(mate.move().get());
                        analysis=new Stockfish.Analysis(uci,null,1,1,List.of(uci),(long)mate.nodes(),elapsed(probeStart));
                        source="mate_java";
                    }
                    else {
                        if(engine==null) engine=new Stockfish(Stockfish.locate());
                        long budget=TimeManager.tournamentBudget(board,remaining-elapsed(turnStarted),increment,searchMillis);
                        analysis=engine.playLogged(String.join(" ",moves.stream().map(TerminalGame::uci).toList()),budget);
                        source="stockfish";
                    }
                } catch(Exception e) {
                    if(engine!=null) engine.close(); engine=null;
                    output.println("Falha no motor: "+e.getMessage());
                    if(!ask("tentar / sair:").equalsIgnoreCase("tentar")) quit=true;
                    continue;
                }
                Move move;
                try { move=parseMove(board,analysis.bestMove()); } catch(Exception e) {
                    output.println("Motor retornou lance inválido. Não mova peças.");
                    if(!ask("tentar / sair:").equalsIgnoreCase("tentar")) quit=true;
                    continue;
                }
                output.println();
                output.println("Anuncie: "+uci(move));
                output.println(MoveInstruction.describe(board,move));
                while(!quit) {
                    String confirmation=ask("feito [MM:SS] / cancelar / sair:").toLowerCase(Locale.ROOT);
                    String[] fields=confirmation.split("\\s+");
                    if(fields[0].equals("feito") && fields.length<=2) {
                        long spent=elapsed(turnStarted);
                        try {
                            remaining=fields.length==2 ? readClock(fields[1])
                                : spent>=remaining ? 0 : remaining-spent+increment;
                        } catch(IllegalArgumentException e) { output.println("Tempo inválido."); continue; }
                        record(move,source,analysis,spent);
                        break;
                    }
                    if(confirmation.equals("cancelar")) break;
                    if(confirmation.equals("sair")) { quit=true; break; }
                    output.println("Use feito, cancelar ou sair. A posição ainda não foi alterada.");
                }
            }
        }
    }
    private boolean command(String text) throws IOException {
        switch(text.toLowerCase(Locale.ROOT)) {
            case "sair" -> { quit=true; return true; }
            case "tabuleiro" -> { output.println(board); return true; }
            case "historico", "histórico" -> { MoveList list=new MoveList(); list.addAll(moves); output.println(list.isEmpty() ? "Nenhum lance registrado." : list.toSanWithMoveNumbers()); return true; }
            case "desfazer" -> {
                if(!moves.isEmpty()) {
                    String clock=ask("Corrija o tabuleiro físico e informe nosso tempo (MM:SS):");
                    if(quit || clock.equalsIgnoreCase("sair")) return true;
                    try { remaining=readClock(clock); }
                    catch(IllegalArgumentException e) { output.println("Tempo inválido. Histórico preservado."); return true; }
                    turnStarted=System.nanoTime();
                    board.undoMove(); moves.removeLast(); logRows.removeLast(); checkpoint();
                } else output.println("Nenhum lance para desfazer.");
                return true;
            }
            case "salvar" -> { try { export(); } catch(IOException e) { output.println("Falha ao salvar PGN: "+e.getMessage()); } return true; }
            default -> { return false; }
        }
    }
    private void checkpoint() {
        try {
            store.save(ours,String.format(Locale.ROOT,"%02d:%02d",remaining/60000,remaining/1000%60),moves,matchId,logRows,increment);
            Path temporary=Files.createTempFile(folder,"log-",".tmp");
            try {
                Files.writeString(temporary,"lance\tcor\tuci\tprofundidade\tnos\tavaliacao_lado_a_jogar\tpv_uci\ttempo_calculo_ms\ttempo_turno_estimado_ms\torigem\n"
                    +String.join("\n",logRows)+(logRows.isEmpty() ? "" : "\n"));
                try { Files.move(temporary,logPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
                catch(FileSystemException e) { Files.move(temporary,logPath(),StandardCopyOption.REPLACE_EXISTING); }
            } finally { Files.deleteIfExists(temporary); }
        }
        catch(IOException e) { output.println("ATENÇÃO: não foi possível salvar a sessão: "+e.getMessage()); }
    }
    private Path logPath() { return folder.resolve("partida-"+matchId+".log"); }
    private void record(Move move,String source,Stockfish.Analysis analysis,Long spent) {
        logRows.add(logRow(move,source,analysis,spent));
        board.doMove(move,true);
        moves.add(move);
        checkpoint();
    }
    private String logRow(Move move,String source,Stockfish.Analysis analysis,Long spent) {
        String score=analysis==null ? "NA" : analysis.mate()!=null ? "mate "+analysis.mate()
            : analysis.centipawns()!=null ? "cp "+analysis.centipawns() : "NA";
        return String.join("\t",Integer.toString(moves.size()/2+1),board.getSideToMove()==Side.WHITE ? "brancas" : "pretas",
            uci(move),analysis==null || analysis.depth()==0 ? "NA" : Integer.toString(analysis.depth()),
            metric(analysis==null ? null : analysis.nodes()),score,
            analysis==null || analysis.line().isEmpty() ? "NA" : String.join(" ",analysis.line()),
            metric(analysis==null ? null : analysis.timeMillis()),metric(spent),source);
    }
    private static String metric(Long value) { return value==null ? "NA" : value.toString(); }
    private static String uci(Move move) { return move.toString().toLowerCase(Locale.ROOT); }
    private static long elapsed(long started) { return TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started); }
    static long readClock(String value) {
        if(!value.matches("[0-9]{1,3}:[0-5][0-9]")) throw new IllegalArgumentException("Use MM:SS.");
        String[] parts=value.split(":");
        return 60000L*Integer.parseInt(parts[0])+1000L*Integer.parseInt(parts[1]);
    }
    private String result() { return board.isMated() ? (board.getSideToMove()==Side.WHITE ? "0-1" : "1-0") : ((board.isStaleMate() || board.isDraw()) ? "1/2-1/2" : "*"); }
    private void export() throws IOException {
        MoveList list=new MoveList(); list.addAll(moves); Files.createDirectories(folder);
        Path file=folder.resolve("terminal-"+System.currentTimeMillis()+".pgn");
        Files.writeString(file,"[Event \"Chess Challenge - terminal\"]\n[Result \""+result()+"\"]\n\n"+list.toSanWithMoveNumbers()+" "+result()+"\n"); output.println("PGN salvo: "+file);
    }
    static Move parseMove(Board board,String text) {
        String uci=text.replace(" ","").replace("-","");
        var coordinate=board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(uci)).findFirst();
        if(coordinate.isPresent()) return coordinate.get();
        MoveList list=new MoveList(board.getFen()); list.loadFromSan(text.trim());
        if(list.size()!=1 || !board.legalMoves().contains(list.getFirst())) throw new IllegalArgumentException("Use origem e destino, por exemplo e7e5.");
        return list.getFirst();
    }
    public static void main(String[] args) throws Exception {
        // Usa o charset nativo do console para entrada em Windows.
        new TerminalGame(new BufferedReader(new InputStreamReader(System.in)),new PrintWriter(System.out,true),Path.of("partidas")).run();
    }
}
