import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;

/** Comunicação UCI local. A fila permite detectar timeout e encerramento do processo. */
public final class Stockfish implements AutoCloseable {
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
            send("setoption name Threads value " + Math.min(4, Math.max(1, Runtime.getRuntime().availableProcessors()/2)));
            send("setoption name Hash value 256");
            send("isready"); waitFor("readyok", 15000);
        } catch (Exception e) { close(); throw e; }
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
