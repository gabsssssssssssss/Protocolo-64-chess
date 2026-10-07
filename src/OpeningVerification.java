import com.github.bhlangonijr.chesslib.*;
import java.nio.file.*;
import java.util.*;

public final class OpeningVerification {
    private static void check(boolean value,String message) { if(!value) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        var games=new ArrayList<>(PgnCollection.read(Path.of("datasets/carlsen-mundial-2021-1.pgn")));
        games.addAll(PgnCollection.read(Path.of("datasets/historicas.pgn")));
        games.add(games.getFirst());
        OpeningIndex index=new OpeningIndex(games);
        check(index.uniqueGames()==2,"Sequência duplicada deve ser deduplicada");
        var initial=index.lookup(new Board()).getFirst();
        check(initial.uci().equals("e2e4") && initial.games()==2 && initial.wins()==1 && initial.draws()==1,"Frequência e resultados das brancas");
        Board board=new Board(); board.doMove(board.legalMoves().stream().filter(m -> m.toString().equals("e2e4")).findFirst().orElseThrow(),true);
        var black=index.lookup(board).getFirst();
        check(black.losses()==1 && black.draws()==1,"Resultados na perspectiva das pretas");
        String[] noEp=board.getFen().split(" "); noEp[3]="-";
        Board equivalent=new Board(); equivalent.loadFromFen(String.join(" ",noEp));
        check(OpeningIndex.key(board).equals(OpeningIndex.key(equivalent)),"En passant impossível não altera chave");
        board.loadFromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 2");
        equivalent.loadFromFen("4k3/8/8/3pP3/8/8/8/4K3 w - - 0 2");
        check(!OpeningIndex.key(board).equals(OpeningIndex.key(equivalent)),"En passant legal precisa alterar chave");
        Path fixture=Files.createTempFile("opening-fixture-",".pgn"); Path csv=Files.createTempFile("opening-export-",".csv");
        try {
            Files.writeString(fixture,"[Event \"A\"]\n[Result \"*\"]\n\n1. Nf3 Nf6 2. g3 g6 3. Bg2 Bg7 *\n\n[Event \"B\"]\n[Result \"*\"]\n\n1. g3 g6 2. Nf3 Nf6 3. Bg2 Bg7 *\n");
            var transpositions=PgnCollection.read(fixture); OpeningIndex transposed=new OpeningIndex(transpositions);
            board.loadFromFen(transpositions.getFirst().positions().get(4));
            var entry=transposed.lookup(board).getFirst();
            check(entry.uci().equals("f1g2") && entry.games()==2 && entry.knownResults()==0,"Transposição e resultados desconhecidos");
            transposed.exportCsv(csv); check(Files.readString(csv).contains("f1g2,2,0,0,0"),"Exportação CSV");
        } finally { Files.deleteIfExists(fixture); Files.deleteIfExists(csv); }
        System.out.println("OK: frequências, deduplicação, resultados por cor, transposições, en passant e exportação CSV.");
    }
}
