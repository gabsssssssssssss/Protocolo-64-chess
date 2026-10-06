import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.pgn.PgnIterator;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Importação offline para estudo; nenhuma alteração na escolha de lances do motor. */
public final class PgnCollection {
    public record Replay(String title,List<String> positions,List<String> san) {
        @Override public String toString() { return title; }
    }
    public static List<Replay> read(Path path) throws Exception {
        if(Files.size(path)>20*1024*1024) throw new IllegalArgumentException("Selecione um PGN de até 20 MB. Divida coleções maiores.");
        List<String> lines=Files.readAllLines(path,StandardCharsets.UTF_8);
        if(!lines.isEmpty()) lines.set(0,lines.getFirst().replace("\uFEFF",""));
        ArrayList<Replay> games=new ArrayList<>();
        try(PgnIterator iterator=new PgnIterator(lines)) {
            for(var game:iterator) {
                if(games.size()>=1000) throw new IllegalArgumentException("Limite de 1000 partidas por arquivo; divida a coleção.");
                String variant=game.getProperty()==null ? null : game.getProperty().get("Variant");
                if(variant!=null && !variant.equalsIgnoreCase("Standard")) throw new IllegalArgumentException("Somente xadrez padrão é suportado.");
                game.loadMoveText();
                var moves=game.getHalfMoves();
                Board board=new Board();
                if(game.getFen()!=null && !game.getFen().isBlank()) board.loadFromFen(game.getFen());
                ArrayList<String> positions=new ArrayList<>(); positions.add(board.getFen());
                for(var move:moves) {
                    if(!board.legalMoves().contains(move) || !board.doMove(move,true)) throw new IllegalArgumentException("Lance ilegal na partida "+(games.size()+1)+": "+move);
                    positions.add(board.getFen());
                }
                String white=game.getWhitePlayer()==null ? "?" : game.getWhitePlayer().getName();
                String black=game.getBlackPlayer()==null ? "?" : game.getBlackPlayer().getName();
                String result=game.getResult()==null ? "*" : game.getResult().getDescription();
                games.add(new Replay((games.size()+1)+". "+white+" × "+black+" — "+result+" — "+game.getDate(),List.copyOf(positions),List.of(moves.toSanArray())));
            }
        }
        if(games.isEmpty()) throw new IllegalArgumentException("Nenhuma partida PGN encontrada.");
        return List.copyOf(games);
    }
}
