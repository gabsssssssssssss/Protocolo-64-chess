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
    private String clock="25:00";
    private Stockfish engine;
    private boolean quit;
    public TerminalGame(BufferedReader input,PrintWriter output,Path folder) { this.input=input; this.output=output; this.folder=folder; store=new SessionStore(folder.resolve("sessao-terminal.properties")); }
    private String ask(String prompt) throws IOException { output.print(prompt+" "); output.flush(); String value=input.readLine(); if(value==null) { quit=true; return "sair"; } return value.trim(); }
    public void run() throws Exception {
        while(ours==null && !quit) {
            String answer=ask("Cor (brancas/pretas/restaurar):").toLowerCase(Locale.ROOT);
            if(answer.equals("sair")) { quit=true; return; }
            if(answer.equals("brancas") || answer.equals("pretas")) ours=answer.equals("brancas") ? Side.WHITE : Side.BLACK;
            else if(answer.equals("restaurar")) {
                try {
                    var saved=store.load(); ours=saved.color(); clock=saved.clock();
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
                    String text=ask("Tempo (MM:SS):");
                    if(command(text)) continue;
                    long remaining;
                    try { remaining=parseClock(text); clock=formatClock(remaining); }
                    catch(Exception e) { output.println("Tempo inválido. Exemplo: 24:30. Informe um tempo maior que zero."); continue; }
                    checkpoint();
                    var budget=TimeManager.allocate(board,remaining);
                    String reply; long start=System.nanoTime();
                    try {
                        if(engine==null) engine=new Stockfish(Stockfish.locate());
                        reply=engine.play(String.join(" ",moves.stream().map(m -> m.toString().toLowerCase(Locale.ROOT)).toList()),budget.millis());
                    } catch(Exception e) {
                        if(engine!=null) engine.close(); engine=null;
                        output.println("FALHA NO MOTOR: "+e.getMessage()+". Confira o relógio antes de tentar novamente."); continue;
                    }
                    long elapsed=(System.nanoTime()-start)/1_000_000;
                    clock=formatClock(Math.max(0,remaining-elapsed));
                    checkpoint();
                    if(elapsed>=remaining) { output.println("Tempo informado esgotado durante o cálculo. Não mova peças; confira o relógio físico."); continue; }
                    Move move;
                    try { move=parseMove(board,reply); } catch(Exception e) { output.println("Motor retornou lance inválido. Não mova peças."); continue; }
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
                    if(confirmed) { board.doMove(move,true); moves.add(move); checkpoint(); }
                }
            }
        } finally { if(engine!=null) engine.close(); }
        output.flush();
    }
    private boolean command(String text) throws IOException {
        switch(text.toLowerCase(Locale.ROOT)) {
            case "sair" -> { quit=true; return true; }
            case "tabuleiro" -> { output.println(board); return true; }
            case "historico", "histórico" -> { MoveList list=new MoveList(); list.addAll(moves); output.println(list.isEmpty() ? "Nenhum lance registrado." : list.toSanWithMoveNumbers()); return true; }
            case "desfazer" -> { if(!moves.isEmpty()) { board.undoMove(); moves.removeLast(); checkpoint(); output.println("Lance desfeito. Corrija também o tabuleiro físico e atualize o tempo."); } else output.println("Nenhum lance para desfazer."); return true; }
            case "salvar" -> { try { export(); } catch(IOException e) { output.println("Falha ao salvar PGN: "+e.getMessage()); } return true; }
            default -> { return false; }
        }
    }
    private void checkpoint() {
        try { store.save(ours,clock,moves); }
        catch(IOException e) { output.println("ATENÇÃO: não foi possível salvar a sessão: "+e.getMessage()); }
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
