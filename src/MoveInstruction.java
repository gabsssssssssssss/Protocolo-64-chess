import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import java.util.Locale;

/** Instruções para executar um lance legal no tabuleiro físico. */
public final class MoveInstruction {
    public static String describe(Board board,Move move) {
        if(!board.legalMoves().contains(move)) throw new IllegalArgumentException("Lance inválido para esta posição.");
        Piece piece=board.getPiece(move.getFrom());
        String from=move.getFrom().toString(),to=move.getTo().toString();
        String name=name(piece),color=piece.getPieceSide()==Side.WHITE ? "brancas" : "pretas";
        String text="MOVA "+name.toUpperCase(Locale.ROOT)+" DE "+from+" PARA "+to+" ("+color+").";
        if((piece==Piece.WHITE_KING || piece==Piece.BLACK_KING) && Math.abs(move.getTo().ordinal()-move.getFrom().ordinal())==2) {
            String rank=from.substring(1); boolean shortCastle=to.startsWith("G");
            text+="\nROQUE "+(shortCastle ? "CURTO" : "LONGO")+": mova também a torre de "+(shortCastle ? "H" : "A")+rank+" para "+(shortCastle ? "F" : "D")+rank+".";
        } else if(board.getPiece(move.getTo())!=Piece.NONE) text+="\nCAPTURA: retire "+name(board.getPiece(move.getTo()))+" do adversário de "+to+".";
        else if((piece==Piece.WHITE_PAWN || piece==Piece.BLACK_PAWN) && from.charAt(0)!=to.charAt(0)) {
            Square captured=Square.squareAt(move.getTo().ordinal()+(piece.getPieceSide()==Side.WHITE ? -8 : 8));
            text+="\nEN PASSANT: retire o peão adversário de "+captured+".";
        }
        if(move.getPromotion()!=Piece.NONE) {
            String promoted=name(move.getPromotion());
            text+="\nPROMOÇÃO: substitua o peão em "+to+" "+(promoted.startsWith("o ") ? "pelo "+promoted.substring(2) : "pela "+promoted.substring(2))+" da nossa cor.";
        }
        return text;
    }
    private static String name(Piece piece) {
        return switch(piece) {
            case WHITE_KING,BLACK_KING -> "o rei";
            case WHITE_QUEEN,BLACK_QUEEN -> "a dama";
            case WHITE_ROOK,BLACK_ROOK -> "a torre";
            case WHITE_BISHOP,BLACK_BISHOP -> "o bispo";
            case WHITE_KNIGHT,BLACK_KNIGHT -> "o cavalo";
            case WHITE_PAWN,BLACK_PAWN -> "o peão";
            default -> throw new IllegalArgumentException("Casa sem peça.");
        };
    }
}
