import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Snapshot validado e substituído atomicamente quando o sistema de arquivos suporta. */
public final class SessionStore {
    public record Session(Side color, String clock, List<Move> moves) {}
    private final Path path;
    public SessionStore(Path path) { this.path=path; }
    public void save(Side color,String clock,List<Move> moves) throws IOException {
        try {
            String[] parts=clock.trim().split(":");
            if(parts.length!=2) throw new IllegalArgumentException();
            int minutes=Integer.parseInt(parts[0]), seconds=Integer.parseInt(parts[1]);
            if(minutes<0 || seconds<0 || seconds>59) throw new IllegalArgumentException();
            clock=String.format(Locale.ROOT,"%02d:%02d",minutes,seconds);
        } catch(RuntimeException e) { throw new IOException("Relógio inválido; sessão anterior preservada.",e); }
        Properties values=new Properties();
        values.setProperty("version","1"); values.setProperty("color",color.name());
        values.setProperty("clock",clock);
        values.setProperty("moves",String.join(" ",moves.stream().map(Move::toString).toList()));
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary=Files.createTempFile(path.toAbsolutePath().getParent(),"session-",".tmp");
        try {
            try(var writer=Files.newBufferedWriter(temporary)) { values.store(writer,"Chess Challenge - sessao local"); }
            try { Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public Session load() throws IOException {
        Properties values=new Properties();
        try(var reader=Files.newBufferedReader(path)) { values.load(reader); }
        try {
            if(!"1".equals(values.getProperty("version"))) throw new IllegalArgumentException("Versão de sessão desconhecida.");
            Side color=Side.valueOf(values.getProperty("color",""));
            String clock=values.getProperty("clock","");
            if(!clock.matches("[0-9]+:[0-5][0-9]")) throw new IllegalArgumentException("Relógio salvo inválido.");
            Board board=new Board(); ArrayList<Move> moves=new ArrayList<>();
            String text=values.getProperty("moves","").trim();
            if(!text.isEmpty()) for(String uci:text.split("\\s+")) {
                Move move=board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(uci)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Sessão contém lance ilegal: "+uci));
                if(!board.doMove(move,true)) throw new IllegalArgumentException("Sessão inválida.");
                moves.add(move);
            }
            return new Session(color,clock,List.copyOf(moves));
        } catch(RuntimeException e) { throw new IOException("Não foi possível validar a sessão: "+e.getMessage(),e); }
    }
}
