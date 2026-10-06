import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Modo de operação no terminal, sem Swing, internet ou VS Code obrigatório. */
public final class TerminalGame {
    private final BufferedReader input;
    private final PrintWriter output;
    private final SessionStore store;
    private final Path folder;
    private final Board board=new Board();
    private final ArrayList<Move> moves=new ArrayList<>();
    private Side ours;
    private long remainingMillis=1_500_000;
    private long turnStarted;
    private Stockfish engine;
    private boolean quit;
    public TerminalGame(BufferedReader input,PrintWriter output,Path folder) { this.input=input; this.output=output; this.folder=folder; store=new SessionStore(folder.resolve("sessao-terminal.properties")); }
    TerminalGame(BufferedReader input,PrintWriter output,Path folder,long initialMillis) {
        this(input,output,folder); remainingMillis=initialMillis;
    }
    private String ask(String prompt) throws IOException { output.print(prompt+" "); output.flush(); String value=input.readLine(); if(value==null) { quit=true; return "sair"; } return value.trim(); }
    public void run() throws Exception {
        while(ours==null && !quit) {
            String answer=ask("Cor (brancas/pretas/restaurar):").toLowerCase(Locale.ROOT);
            if(answer.equals("sair")) { quit=true; return; }
            if(answer.equals("brancas") || answer.equals("pretas")) ours=answer.equals("brancas") ? Side.WHITE : Side.BLACK;
            else if(answer.equals("restaurar")) {
                try {
                    var saved=store.load();
                    long savedMillis=saved.clock().equals("00:00") ? 0 : parseClock(saved.clock());
                    ours=saved.color(); remainingMillis=savedMillis;
                    for(Move move:saved.moves()) { board.doMove(move,true); moves.add(move); }
                } catch(Exception e) { output.println("Não foi possível restaurar: "+e.getMessage()); }
            } else output.println("Escolha brancas ou pretas.");
        }
        try {
            while(!quit) {
                if(board.isMated() || board.isStaleMate() || board.isDraw()) { output.println("Fim: "+result()); break; }
                if(board.getSideToMove()!=ours) {
                    String text=ask("Lance do adversário:");
                    if(command(text)) continue;
                    try { Move move=parseMove(board,text); board.doMove(move,true); moves.add(move); checkpoint(); }
                    catch(Exception e) { output.println("Lance inválido. Digite origem e destino, como e7e5."); }
                } else {
                    if(turnStarted==0) turnStarted=System.nanoTime();
                    long remaining=currentRemaining();
                    if(remaining<=0) { output.println("Tempo interno estimado esgotado. Confira o relógio físico."); break; }
                    checkpoint();
                    var budget=TimeManager.allocate(board,remaining);
                    String reply;
                    try {
                        if(engine==null) engine=new Stockfish(Stockfish.locate());
                        reply=engine.play(String.join(" ",moves.stream().map(m -> m.toString().toLowerCase(Locale.ROOT)).toList()),budget.millis());
                    } catch(Exception e) {
                        if(engine!=null) engine.close(); engine=null;
                        output.println("Falha no motor: "+e.getMessage());
                        if(!ask("tentar / sair:").equalsIgnoreCase("tentar")) quit=true;
                        continue;
                    }
                    checkpoint();
                    if(currentRemaining()<=0) { output.println("Tempo interno estimado esgotado. Confira o relógio físico."); break; }
                    Move move;
                    try { move=parseMove(board,reply); } catch(Exception e) {
                        output.println("Motor retornou lance inválido. Não mova peças.");
                        if(!ask("tentar / sair:").equalsIgnoreCase("tentar")) quit=true;
                        continue;
                    }
                    output.println();
                    output.println(MoveInstruction.describe(board,move));
                    boolean confirmed=false;
                    while(!quit) {
                        String confirmation=ask("feito / cancelar / sair:").toLowerCase(Locale.ROOT);
                        if(confirmation.equals("feito")) { confirmed=true; break; }
                        if(confirmation.equals("cancelar")) break;
                        if(confirmation.equals("sair")) { quit=true; break; }
                        output.println("Use feito, cancelar ou sair. A posição ainda não foi alterada.");
                    }
                    if(confirmed) {
                        remainingMillis=currentRemaining(); turnStarted=0;
                        board.doMove(move,true); moves.add(move); checkpoint();
                    }
                }
            }
        } finally { checkpoint(); if(engine!=null) engine.close(); }
        output.flush();
    }
    private boolean command(String text) throws IOException {
        switch(text.toLowerCase(Locale.ROOT)) {
            case "sair" -> { quit=true; return true; }
            case "tabuleiro" -> { output.println(board); return true; }
            case "historico", "histórico" -> { MoveList list=new MoveList(); list.addAll(moves); output.println(list.isEmpty() ? "Nenhum lance registrado." : list.toSanWithMoveNumbers()); return true; }
            case "desfazer" -> { if(!moves.isEmpty()) { remainingMillis=currentRemaining(); turnStarted=0; board.undoMove(); moves.removeLast(); checkpoint(); output.println("Lance desfeito. Corrija também o tabuleiro físico."); } else output.println("Nenhum lance para desfazer."); return true; }
            case "salvar" -> { try { export(); } catch(IOException e) { output.println("Falha ao salvar PGN: "+e.getMessage()); } return true; }
            default -> { return false; }
        }
    }
    private void checkpoint() {
        try { store.save(ours,formatClock(currentRemaining()),moves); }
        catch(IOException e) { output.println("ATENÇÃO: não foi possível salvar a sessão: "+e.getMessage()); }
    }
    private long currentRemaining() {
        return Math.max(0,remainingMillis-(turnStarted==0 ? 0 : (System.nanoTime()-turnStarted)/1_000_000));
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
    static long parseClock(String text) {
        if(!text.matches("[0-9]{1,4}:[0-5][0-9]")) throw new IllegalArgumentException("Formato MM:SS");
        String[] parts=text.split(":"); long value=(Long.parseLong(parts[0])*60+Long.parseLong(parts[1]))*1000;
        if(value<=0) throw new IllegalArgumentException("Tempo esgotado"); return value;
    }
    private static String formatClock(long millis) { long seconds=millis/1000; return String.format(Locale.ROOT,"%02d:%02d",seconds/60,seconds%60); }
    public static void main(String[] args) throws Exception {
        // Usa o charset nativo do console para entrada em Windows.
        new TerminalGame(new BufferedReader(new InputStreamReader(System.in)),new PrintWriter(System.out,true),Path.of("partidas")).run();
    }
}
