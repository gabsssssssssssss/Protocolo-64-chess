import com.github.bhlangonijr.chesslib.*;
import javax.swing.*;
import java.awt.*;

/** Visualização isolada da partida; relógios são atualizados ao terminar cada lance. */
public final class MatchViewer {
    private final JFrame frame=new JFrame("Confronto ao vivo • Gestão própria × Stockfish");
    private final BoardView boardView=new BoardView();
    private final JLabel heading=new JLabel("Preparando motores..."), clocks=new JLabel(), status=new JLabel();
    public MatchViewer() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UiTheme.install();
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            JPanel top=new JPanel(new GridLayout(2,1)); top.add(heading); top.add(clocks);
            top.setBorder(BorderFactory.createEmptyBorder(12,16,12,16)); status.setBorder(BorderFactory.createEmptyBorder(12,16,12,16));
            frame.add(top,BorderLayout.NORTH); frame.add(boardView); frame.add(status,BorderLayout.SOUTH);
            UiTheme.style(frame.getContentPane());
            frame.pack(); frame.setLocationRelativeTo(null); frame.setVisible(true);
        });
    }
    public void update(String fen,int game,Side custom,long white,long black,String message) {
        SwingUtilities.invokeLater(() -> {
            if(!frame.isDisplayable()) return;
            Board board=new Board(); board.loadFromFen(fen);
            heading.setText("Partida "+game+"/2 — gestão própria: "+(custom==Side.WHITE ? "brancas" : "pretas"));
            clocks.setText("Brancas: "+time(white)+" | Pretas: "+time(black)+" (tempo após cada lance)"); status.setText(message);
            boardView.display(board,Side.WHITE,null,null,false);
        });
    }
    private static String time(long millis) { long seconds=Math.max(0,millis/1000); return String.format("%02d:%02d",seconds/60,seconds%60); }
}
