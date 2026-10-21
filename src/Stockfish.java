import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.*;

/** Comunicação UCI local. A fila permite detectar timeout e encerramento do processo. */
public final class Stockfish implements AutoCloseable {
    public record Analysis(String bestMove,Integer centipawns,Integer mate,int depth,List<String> line) {}
    private final Process process;
    private final BufferedWriter input;
    private final BlockingQueue<String> output = new LinkedBlockingQueue<>();
    public Stockfish(Path executable) throws Exception {
        process = new ProcessBuilder(executable.toAbsolutePath().toString()).redirectErrorStream(true).start();
        input = process.outputWriter(StandardCharsets.UTF_8);
        Thread reader = new Thread(() -> {
            try (var stream = process.inputReader(StandardCharsets.UTF_8)) {
                String line;
                while ((line = stream.readLine()) != null) output.offer(line);
            } catch (IOException ignored) {} finally { output.offer("__EXIT__"); }
        }, "uci-output");
        reader.setDaemon(true);
        reader.start();
        try {
            send("uci"); waitFor("uciok", 15000);
            send("setoption name Threads value " + setting("chess.threads",Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors()/2)),1,Math.max(1,Runtime.getRuntime().availableProcessors())));
            send("setoption name Hash value " + setting("chess.hashMb",512,16,2048));
            send("setoption name UCI_LimitStrength value false");
            send("setoption name Skill Level value 20");
            send("setoption name MultiPV value 1");
            send("isready"); waitFor("readyok", 15000);
        } catch (Exception e) { close(); throw e; }
    }
    static int setting(String name,int fallback,int minimum,int maximum) {
        String value=System.getProperty(name);
        if(value==null) return fallback;
        try {
            int parsed=Integer.parseInt(value);
            if(parsed>=minimum && parsed<=maximum) return parsed;
        } catch(NumberFormatException ignored) {}
        throw new IllegalArgumentException(name+" deve estar entre "+minimum+" e "+maximum+".");
    }
    private void send(String line) throws IOException { input.write(line); input.newLine(); input.flush(); }
    private String waitFor(String prefix, long timeout) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeout);
        while (true) {
            long left = deadline - System.nanoTime();
            String line = left > 0 ? output.poll(left, TimeUnit.NANOSECONDS) : null;
            if (line == null) throw new IOException("Stockfish não respondeu dentro do prazo.");
            if (line.equals("__EXIT__")) throw new IOException("Stockfish encerrou inesperadamente.");
            if (line.startsWith(prefix)) return line;
        }
    }
    public String play(String moves, long millis) throws Exception {
        send("position startpos" + (moves.isBlank() ? "" : " moves " + moves));
        send("go movetime " + millis);
        return waitFor("bestmove ", millis + 5000).split(" ")[1];
    }
    public String playWithClock(String moves, long whiteMillis, long blackMillis) throws Exception {
        send("position startpos" + (moves.isBlank() ? "" : " moves " + moves));
        send("go wtime " + whiteMillis + " btime " + blackMillis + " winc 0 binc 0");
        return waitFor("bestmove ", Math.max(whiteMillis, blackMillis) + 5000).split(" ")[1];
    }
    public void newGame() throws Exception {
        send("ucinewgame"); send("isready"); waitFor("readyok", 10000);
    }
    /** Mantém o histórico do PGN, inclusive para detectar repetições. Score: lado a jogar. */
    public Analysis analyse(String initialFen,String moves,long millis) throws Exception {
        if(millis<1 || millis>30000 || initialFen.contains("\n") || moves.contains("\n")) throw new IllegalArgumentException("Parâmetros de análise inválidos.");
        send("position fen " + initialFen + (moves.isBlank() ? "" : " moves " + moves));
        send("go movetime " + millis);
        long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(millis+5000);
        Analysis latest=new Analysis("",null,null,0,List.of());
        while(true) {
            long left=deadline-System.nanoTime();
            String message=left>0 ? output.poll(left,TimeUnit.NANOSECONDS) : null;
            if(message==null) throw new IOException("Stockfish não respondeu à análise dentro do prazo.");
            if(message.equals("__EXIT__")) throw new IOException("Stockfish encerrou durante a análise.");
            if(message.startsWith("info ")) {
                Analysis parsed=parseInfo(message);
                if(parsed!=null) latest=parsed;
            }
            if(message.startsWith("bestmove ")) return new Analysis(message.split(" ")[1],latest.centipawns(),latest.mate(),latest.depth(),latest.line());
        }
    }
    static Analysis parseInfo(String message) {
        if(!message.startsWith("info ") || message.contains(" lowerbound") || message.contains(" upperbound")) return null;
        String[] words=message.trim().split("\\s+"); Integer cp=null,mate=null; int depth=0;
        List<String> line=List.of();
        try {
            for(int i=1;i<words.length;i++) {
                if(words[i].equals("multipv") && i+1<words.length && !words[i+1].equals("1")) return null;
                if(words[i].equals("depth") && i+1<words.length) depth=Integer.parseInt(words[++i]);
                else if(words[i].equals("score") && i+2<words.length) {
                    String type=words[++i]; int value=Integer.parseInt(words[++i]);
                    if(type.equals("cp")) cp=value; else if(type.equals("mate")) mate=value;
                } else if(words[i].equals("pv")) { line=List.of(Arrays.copyOfRange(words,i+1,words.length)); break; }
            }
        } catch(NumberFormatException e) { return null; }
        return cp==null && mate==null ? null : new Analysis("",cp,mate,depth,line);
    }
    public static Path locate() throws IOException {
        try (var paths = Files.walk(Path.of("vendor/engine"))) {
            return paths.filter(p -> p.getFileName().toString().startsWith("stockfish") && p.toString().endsWith(".exe"))
                .findFirst().orElseThrow(() -> new IOException("Stockfish não encontrado. Execute preparar.ps1."));
        }
    }
    @Override public void close() {
        try { send("quit"); } catch (IOException ignored) {}
        process.destroy();
        try { if (!process.waitFor(2, TimeUnit.SECONDS)) process.destroyForcibly(); }
        catch (InterruptedException e) { process.destroyForcibly(); Thread.currentThread().interrupt(); }
    }
}
