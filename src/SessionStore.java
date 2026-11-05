import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Snapshot validado e substituído atomicamente quando o sistema de arquivos suporta. */
public final class SessionStore {
    public record Session(Side color, String clock, List<Move> moves, UUID matchId, List<String> logRows, long increment, boolean armageddon) {}
    private final Path path;
    @FunctionalInterface interface Mover { void move(Path from,Path to,boolean atomic) throws IOException; }
    private final Mover mover;
    public SessionStore(Path path) { this(path,(from,to,atomic) -> {
        if(atomic) Files.move(from,to,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        else Files.move(from,to,StandardCopyOption.REPLACE_EXISTING);
    }); }
    SessionStore(Path path,Mover mover) { this.path=path.toAbsolutePath().normalize(); this.mover=mover; }
    public void save(Side color,String clock,List<Move> moves) throws IOException {
        save(color,clock,moves,null,List.of(),TimeManager.INCREMENT_MILLIS);
    }
    public void save(Side color,String clock,List<Move> moves,UUID matchId,List<String> logRows,long increment) throws IOException {
        save(color,clock,moves,matchId,logRows,increment,false);
    }
    public void save(Side color,String clock,List<Move> moves,UUID matchId,List<String> logRows,long increment,boolean armageddon) throws IOException {
        try { if(!"sem-limite".equals(clock)) {
            String[] parts=clock.trim().split(":");
            if(parts.length!=2) throw new IllegalArgumentException();
            int minutes=Integer.parseInt(parts[0]), seconds=Integer.parseInt(parts[1]);
            if(minutes<0 || seconds<0 || seconds>59) throw new IllegalArgumentException();
            clock=String.format(Locale.ROOT,"%02d:%02d",minutes,seconds);
        }
        } catch(RuntimeException e) { throw new IOException("Relógio inválido; sessão anterior preservada.",e); }
        Properties values=new Properties();
        values.setProperty("version","1"); values.setProperty("color",color.name());
        values.setProperty("clock",clock);
        values.setProperty("moves",String.join(" ",moves.stream().map(Move::toString).toList()));
        if(matchId!=null) {
            values.setProperty("matchId",matchId.toString());
            values.setProperty("log",String.join("\n",logRows));
            values.setProperty("incrementMillis",Long.toString(increment));
            values.setProperty("armageddon",Boolean.toString(armageddon));
        }
        Files.createDirectories(path.toAbsolutePath().getParent());
        Path temporary=Files.createTempFile(path.toAbsolutePath().getParent(),"session-",".tmp");
        try {
            try(var writer=Files.newBufferedWriter(temporary)) { values.store(writer,"Chess Challenge - sessao local"); }
        } catch(IOException e) { Files.deleteIfExists(temporary); throw e; }
        IOException failure=null;
        for(int attempt=0;attempt<3;attempt++) {
            try {
                try { mover.move(temporary,path,true); }
                // No Windows, ATOMIC_MOVE pode recusar substituir um destino existente.
                catch(FileSystemException e) { mover.move(temporary,path,false); }
                return;
            } catch(IOException e) {
                failure=e;
                if(attempt<2) try { Thread.sleep(50L*(attempt+1)); }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt(); break; }
            }
        }
        // Preserva o snapshot completo se um bloqueio impedir todas as tentativas.
        throw new IOException("Falha ao atualizar "+path+". Cópia recuperável preservada em "+temporary+". "+failure.getMessage(),failure);
    }
    public Session load() throws IOException {
        Properties values=new Properties();
        try(var reader=Files.newBufferedReader(path)) { values.load(reader); }
        try {
            if(!"1".equals(values.getProperty("version"))) throw new IllegalArgumentException("Versão de sessão desconhecida.");
            Side color=Side.valueOf(values.getProperty("color",""));
            String clock=values.getProperty("clock","");
            if(!clock.equals("sem-limite") && !clock.matches("[0-9]+:[0-5][0-9]")) throw new IllegalArgumentException("Relógio salvo inválido.");
            Board board=new Board(); ArrayList<Move> moves=new ArrayList<>();
            String text=values.getProperty("moves","").trim();
            if(!text.isEmpty()) for(String uci:text.split("\\s+")) {
                Move move=board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(uci)).findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Sessão contém lance ilegal: "+uci));
                if(!board.doMove(move,true)) throw new IllegalArgumentException("Sessão inválida.");
                moves.add(move);
            }
            String matchId=values.getProperty("matchId");
            List<String> rows=values.getProperty("log","").lines().toList();
            if(matchId!=null && rows.size()!=moves.size()) throw new IllegalArgumentException("Log da sessão incompleto.");
            long increment=Long.parseLong(values.getProperty("incrementMillis",Long.toString(TimeManager.INCREMENT_MILLIS)));
            if(increment<0 || increment>999_000) throw new IllegalArgumentException("Incremento inválido.");
            String mode=values.getProperty("armageddon","false");
            if(!mode.equals("true") && !mode.equals("false")) throw new IllegalArgumentException("Modo da partida inválido.");
            return new Session(color,clock,List.copyOf(moves),matchId==null ? null : UUID.fromString(matchId),rows,increment,Boolean.parseBoolean(mode));
        } catch(RuntimeException e) { throw new IOException("Não foi possível validar a sessão: "+e.getMessage(),e); }
    }
}
