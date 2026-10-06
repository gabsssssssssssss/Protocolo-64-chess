import com.github.bhlangonijr.chesslib.*;
import javax.swing.*;
import java.awt.*;

/** Visualização isolada da partida; relógios são atualizados ao terminar cada lance. */
public final class MatchViewer {
    private final JFrame frame=new JFrame("Confronto ao vivo • Gestão própria × Stockfish");
    private final JLabel[] cells=new JLabel[64];
    private final JLabel heading=new JLabel("Preparando motores..."), clocks=new JLabel(), status=new JLabel();
    public MatchViewer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel grid=new JPanel(new GridLayout(8,8)); grid.setPreferredSize(new Dimension(560,560));
            for(int i=0;i<64;i++) {
                cells[i]=new JLabel("",SwingConstants.CENTER); cells[i].setOpaque(true);
                cells[i].setFont(new Font("Segoe UI Symbol",Font.PLAIN,42)); grid.add(cells[i]);
            }
            JPanel top=new JPanel(new GridLayout(2,1)); top.add(heading); top.add(clocks);
            frame.add(top,BorderLayout.NORTH); frame.add(grid); frame.add(status,BorderLayout.SOUTH);
            frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true);
        });
    }
    public void update(String fen,int game,Side custom,long white,long black,String message) {
        SwingUtilities.invokeLater(() -> {
            if(!frame.isDisplayable()) return;
            Board board=new Board(); board.loadFromFen(fen);
            heading.setText("Partida "+game+"/2 — gestão própria: "+(custom==Side.WHITE ? "brancas" : "pretas"));
            clocks.setText("Brancas: "+time(white)+" | Pretas: "+time(black)+" (tempo após cada lance)"); status.setText(message);
            for(int i=0;i<64;i++) {
                Square sq=Square.squareAt((7-i/8)*8+i%8);
                String glyph=switch(board.getPiece(sq)) {
                    case WHITE_KING -> "♔"; case WHITE_QUEEN -> "♕"; case WHITE_ROOK -> "♖";
                    case WHITE_BISHOP -> "♗"; case WHITE_KNIGHT -> "♘"; case WHITE_PAWN -> "♙";
                    case BLACK_KING -> "♚"; case BLACK_QUEEN -> "♛"; case BLACK_ROOK -> "♜";
                    case BLACK_BISHOP -> "♝"; case BLACK_KNIGHT -> "♞"; case BLACK_PAWN -> "♟";
                    default -> "";
                };
                cells[i].setText("<html><center>"+glyph+"<br><font size='2'>"+sq.toString().toLowerCase()+"</font></center></html>");
                cells[i].setBackground((i/8+i%8)%2==0 ? new Color(235,236,208) : new Color(119,149,86));
            }
        });
    }
    private static String time(long millis) { long seconds=Math.max(0,millis/1000); return String.format("%02d:%02d",seconds/60,seconds%60); }
}
