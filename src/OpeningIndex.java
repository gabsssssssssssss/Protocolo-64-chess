import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import java.nio.file.*;
import java.util.*;

/** Frequências observadas em PGNs; não são avaliações de qualidade do motor. */
public final class OpeningIndex {
    public record Entry(String uci,int games,int wins,int draws,int losses) {
        public int knownResults() { return wins+draws+losses; }
    }
    private final Map<String,Map<String,int[]>> index=new TreeMap<>();
    private int uniqueGames;
    public OpeningIndex(List<PgnCollection.Replay> games) {
        Set<String> seenGames=new HashSet<>();
        for(var game:games) {
            if(Thread.currentThread().isInterrupted()) throw new java.util.concurrent.CancellationException();
            if(game.uci().isEmpty()) continue;
            String signature=game.positions().getFirst()+"|"+String.join(" ",game.uci());
            if(!seenGames.add(signature)) continue;
            uniqueGames++;
            Set<String> seenPositions=new HashSet<>();
            for(int ply=0;ply<Math.min(24,game.uci().size());ply++) {
                Board board=new Board(); board.loadFromFen(game.positions().get(ply));
                String key=key(board);
                if(!seenPositions.add(key)) continue;
                int[] values=index.computeIfAbsent(key,k -> new TreeMap<>()).computeIfAbsent(game.uci().get(ply),k -> new int[4]);
                values[0]++;
                boolean white=board.getSideToMove()==Side.WHITE;
                if(game.result().equals("1/2-1/2")) values[2]++;
                else if(game.result().equals("1-0") || game.result().equals("0-1")) {
                    boolean won=white==game.result().equals("1-0"); values[won ? 1 : 3]++;
                }
            }
        }
    }
    public int uniqueGames() { return uniqueGames; }
    public List<Entry> lookup(Board board) {
        Set<String> legal=new HashSet<>(); for(Move move:board.legalMoves()) legal.add(move.toString().toLowerCase(Locale.ROOT));
        var values=index.getOrDefault(key(board),Map.of());
        return values.entrySet().stream().filter(e -> legal.contains(e.getKey())).map(e -> new Entry(e.getKey(),e.getValue()[0],e.getValue()[1],e.getValue()[2],e.getValue()[3]))
            .sorted(Comparator.comparingInt(Entry::games).reversed().thenComparing(Entry::uci)).toList();
    }
    static String key(Board board) {
        String[] fen=board.getFen().split(" ");
        // Um alvo en passant sem captura legal não muda os lances disponíveis.
        if(!fen[3].equals("-")) {
            boolean capturable=board.legalMoves().stream().anyMatch(m -> {
                Piece p=board.getPiece(m.getFrom());
                return (p==Piece.WHITE_PAWN || p==Piece.BLACK_PAWN) && board.getPiece(m.getTo())==Piece.NONE && m.getTo().toString().equalsIgnoreCase(fen[3]) && m.getFrom().toString().charAt(0)!=m.getTo().toString().charAt(0);
            });
            if(!capturable) fen[3]="-";
        }
        return String.join(" ",Arrays.copyOf(fen,4));
    }
    public String describe(Board board) {
        var entries=lookup(board);
        if(entries.isEmpty()) return "Sem dados de abertura para esta posição.\nO motor continua disponível na aba Análise.";
        int total=entries.stream().mapToInt(Entry::games).sum();
        StringBuilder text=new StringBuilder("Coleção: "+uniqueGames+" sequências únicas.\nPosição: "+total+" ocorrências.\n\n");
        for(Entry entry:entries) {
            Move move=board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(entry.uci())).findFirst().orElseThrow();
            var san=new com.github.bhlangonijr.chesslib.move.MoveList(board.getFen()); san.add(move);
            text.append(san.toSanArray()[0]).append(" (").append(entry.uci()).append(") — ").append(entry.games()).append(" ocorrência(s)");
            text.append(String.format(Locale.ROOT," · %.0f%%%n",100.0*entry.games()/total));
            text.append("Resultados do lado a jogar: ").append(entry.wins()).append("V / ").append(entry.draws()).append("E / ").append(entry.losses()).append("D\n\n");
        }
        text.append("Amostras pequenas não comprovam força.\nResultados desconhecidos não entram em V/E/D.\nA consulta usa os primeiros 12 lances por lado.\nNão substitui a escolha do Stockfish.");
        return text.toString();
    }
    public void exportCsv(Path path) throws Exception {
        Files.createDirectories(path.toAbsolutePath().getParent());
        StringBuilder csv=new StringBuilder("fen,uci,ocorrencias,vitorias,empates,derrotas\n");
        for(var position:index.entrySet()) for(var candidate:position.getValue().entrySet()) {
            int[] v=candidate.getValue(); csv.append('"').append(position.getKey()).append("\",").append(candidate.getKey()).append(',').append(v[0]).append(',').append(v[1]).append(',').append(v[2]).append(',').append(v[3]).append('\n');
        }
        Files.writeString(path,csv);
    }
    public static void main(String[] args) throws Exception {
        ArrayList<PgnCollection.Replay> games=new ArrayList<>();
        for(String file:args) games.addAll(PgnCollection.read(Path.of(file)));
        if(games.isEmpty()) throw new IllegalArgumentException("Informe um ou mais arquivos PGN locais.");
        OpeningIndex book=new OpeningIndex(games);
        Path output=Path.of("partidas","repertorio-"+System.currentTimeMillis()+".csv"); book.exportCsv(output);
        System.out.println(book.describe(new Board())); System.out.println("Estatísticas salvas: "+output.toAbsolutePath());
    }
}
