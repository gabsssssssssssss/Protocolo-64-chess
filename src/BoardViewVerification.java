import com.github.bhlangonijr.chesslib.*;
import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.concurrent.atomic.AtomicInteger;

public final class BoardViewVerification {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            BoardView view=new BoardView(); view.setSize(640,640);
            AtomicInteger clicked=new AtomicInteger(-1); view.onClick(clicked::set);
            Board board=new Board(); view.display(board,Side.WHITE,Square.E2,null,true);
            // Margem 32, casas 72. Centro visual de e2: coluna 4, linha 6.
            view.dispatchEvent(new MouseEvent(view,MouseEvent.MOUSE_PRESSED,0,0,356,500,1,false));
            if(clicked.get()!=52) throw new AssertionError("Clique na casa e2");
            clicked.set(-1);
            view.dispatchEvent(new MouseEvent(view,MouseEvent.MOUSE_PRESSED,0,0,5,5,1,false));
            if(clicked.get()!=-1) throw new AssertionError("Margem não deve gerar lance");
            view.display(board,Side.BLACK,null,null,false);
            view.dispatchEvent(new MouseEvent(view,MouseEvent.MOUSE_PRESSED,0,0,356,500,1,false));
            if(clicked.get()!=-1) throw new AssertionError("Tabuleiro desabilitado deve ignorar cliques");
            view.setSize(800,640); view.display(board,Side.BLACK,null,null,true);
            view.dispatchEvent(new MouseEvent(view,MouseEvent.MOUSE_PRESSED,0,0,436,500,1,false));
            if(clicked.get()!=52) throw new AssertionError("Clique com janela redimensionada");
            System.out.println("OK: seleção por clique, margem, bloqueio de entrada e redimensionamento do tabuleiro.");
        });
    }
}
