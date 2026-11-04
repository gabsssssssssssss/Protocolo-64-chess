import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;

/** Política experimental própria; precisa de comparação com o motor original. */
public final class TimeManager {
    public record Budget(long millis, String reason) {}
    /** Política própria por posição, sem relógio acumulado de partida. */
    public static Budget allocateForPosition(Board board) {
        var moves=board.legalMoves();
        if(moves.isEmpty() || board.isDraw()) throw new IllegalArgumentException("A partida já terminou.");
        if(moves.size()==1) return new Budget(100,"Único lance legal");
        int material=0;
        for(Square square:Square.values()) {
            if(square==Square.NONE) continue;
            material+=switch(board.getPiece(square)) {
                case WHITE_KNIGHT,BLACK_KNIGHT,WHITE_BISHOP,BLACK_BISHOP -> 3;
                case WHITE_ROOK,BLACK_ROOK -> 5;
                case WHITE_QUEEN,BLACK_QUEEN -> 9;
                default -> 0;
            };
        }
        int fullmove=Integer.parseInt(board.getFen().split(" ")[5]);
        boolean endgame=material<=20, opening=fullmove<=10 && material>=50;
        long base=endgame ? 18000 : (opening ? 12000 : 16000);
        int captures=0, promotions=0, attackingChecks=0;
        Board simulation=new Board();
        simulation.loadFromFen(board.getFen());
        for(Move move:moves) {
            Piece piece=board.getPiece(move.getFrom());
            boolean pawn=piece==Piece.WHITE_PAWN || piece==Piece.BLACK_PAWN;
            boolean enPassant=pawn && board.getPiece(move.getTo())==Piece.NONE
                && move.getFrom().toString().charAt(0)!=move.getTo().toString().charAt(0);
            if(board.getPiece(move.getTo())!=Piece.NONE || enPassant) captures++;
            if(move.getPromotion()!=Piece.NONE) promotions++;
            if(!simulation.doMove(move,true)) throw new IllegalStateException("Falha ao simular lance legal.");
            if(simulation.isKingAttacked()) attackingChecks++;
            simulation.undoMove();
        }
        long activity=8000L*captures/moves.size();
        long check=board.isKingAttacked() ? 6000 : 0;
        long promotion=promotions>0 ? 6000 : 0;
        long attack=Math.min(2000,500L*attackingChecks);
        long budget=Math.min(30000,base+activity+check+promotion+attack);
        String phase=endgame ? "final" : (opening ? "abertura" : "meio-jogo");
        return new Budget(budget,"Política por posição v2: "+phase+"; base="+base
            +"ms; capturas="+captures+"/"+moves.size()+" (+"+activity+"ms); xeque=+"+check
            +"ms; promoção=+"+promotion+"ms; xeques disponíveis="+attackingChecks
            +" (+"+attack+"ms); teto=30000ms");
    }

    /** Política com relógio, usada somente no comparador experimental. */
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
