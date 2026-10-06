import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

/** Política experimental própria; precisa de comparação com o motor original. */
public final class TimeManager {
    public record Budget(long millis, String reason) {}
    public static Budget allocate(Board board, long remaining) {
        if (remaining <= 0) throw new IllegalArgumentException("Tempo esgotado.");
        var moves = board.legalMoves();
        if (moves.isEmpty()) throw new IllegalArgumentException("Não existem lances legais.");
        if (moves.size() == 1) return new Budget(Math.max(1, Math.min(100, remaining / 10)), "Único lance legal");
        int fullmove = Integer.parseInt(board.getFen().split(" ")[5]);
        int horizon = Math.max(18, 45 - fullmove / 2);
        long reserve = Math.min(60000, remaining / 5);
        int tactical = 0;
        for (Move move : moves) {
            if (board.getPiece(move.getTo()) != Piece.NONE || move.getPromotion() != Piece.NONE) tactical++;
        }
        double factor = 1 + Math.min(.6, (double)tactical / moves.size());
        String reason = "Reserva operacional + lances estimados + atividade tática";
        if (board.isKingAttacked()) { factor += .25; reason += "; rei em xeque"; }
        long budget = (long)((remaining - reserve) / (double)horizon * factor);
        return new Budget(Math.max(1, Math.min(30000, Math.min(remaining * 8 / 100, Math.max(50, budget)))), reason);
    }
}
