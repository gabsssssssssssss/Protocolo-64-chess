import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.Move;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.function.IntConsumer;

/** Tabuleiro compartilhado, desenhado localmente e redimensionável. */
public final class BoardView extends JPanel {
    private Board position=new Board();
    private Side orientation=Side.WHITE;
    private Square selected;
    private Move last;
    private Set<Square> targets=Set.of();
    private boolean interactive;
    private IntConsumer handler;
    private static final Color LIGHT=new Color(230,222,205), DARK=new Color(96,127,111);
    public BoardView() {
        setPreferredSize(new Dimension(620,620)); setMinimumSize(new Dimension(400,400));
        setBackground(new Color(24,32,43));
        addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) {
            double cell=(Math.min(getWidth(),getHeight())-64)/8.0;
            double x=(getWidth()-8*cell)/2.0,y=(getHeight()-8*cell)/2.0;
            int col=(int)Math.floor((e.getX()-x)/cell),row=(int)Math.floor((e.getY()-y)/cell);
            if(interactive && handler!=null && col>=0 && col<8 && row>=0 && row<8) handler.accept(row*8+col);
        }});
    }
    public void onClick(IntConsumer handler) { this.handler=handler; }
    public void display(Board board,Side side,Square selected,Move last,boolean interactive) {
        position=new Board(); position.loadFromFen(board.getFen()); orientation=side;
        this.selected=selected; this.last=last; this.interactive=interactive;
        HashSet<Square> legal=new HashSet<>();
        if(selected!=null) for(Move move:board.legalMoves()) if(move.getFrom()==selected) legal.add(move.getTo());
        targets=legal; repaint();
    }
    private Square square(int row,int col) { return Square.squareAt(orientation==Side.WHITE ? (7-row)*8+col : row*8+7-col); }
    @Override protected void paintComponent(Graphics original) {
        super.paintComponent(original); Graphics2D g=(Graphics2D)original.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        double cell=(Math.min(getWidth(),getHeight())-64)/8.0;
        double left=(getWidth()-8*cell)/2,top=(getHeight()-8*cell)/2;
        g.setColor(new Color(10,16,23)); g.fill(new RoundRectangle2D.Double(left-7,top-7,cell*8+14,cell*8+14,14,14));
        for(int row=0;row<8;row++) for(int col=0;col<8;col++) {
            Square sq=square(row,col); double x=left+col*cell,y=top+row*cell;
            Color base=(row+col)%2==0 ? LIGHT : DARK;
            if(last!=null && (last.getFrom()==sq || last.getTo()==sq)) base=(row+col)%2==0 ? new Color(226,211,136) : new Color(164,172,92);
            if(sq==selected) base=new Color(93,171,164);
            g.setColor(base); g.fill(new Rectangle2D.Double(x,y,cell,cell));
            if(targets.contains(sq)) { g.setColor(new Color(28,61,53,100)); g.fill(new Ellipse2D.Double(x+cell*.4,y+cell*.4,cell*.2,cell*.2)); }
            Piece p=position.getPiece(sq);
            if(p!=Piece.NONE) {
                String symbol=switch(p) {
                    case WHITE_KING,BLACK_KING -> "♚"; case WHITE_QUEEN,BLACK_QUEEN -> "♛";
                    case WHITE_ROOK,BLACK_ROOK -> "♜"; case WHITE_BISHOP,BLACK_BISHOP -> "♝";
                    case WHITE_KNIGHT,BLACK_KNIGHT -> "♞"; default -> "♟";
                };
                Font font=new Font("Segoe UI Symbol",Font.PLAIN,(int)(cell*.77));
                Shape shape=font.createGlyphVector(g.getFontRenderContext(),symbol).getOutline();
                Rectangle2D bounds=shape.getBounds2D();
                Shape translated=AffineTransform.getTranslateInstance(x+(cell-bounds.getWidth())/2-bounds.getX(),y+(cell-bounds.getHeight())/2-bounds.getY()).createTransformedShape(shape);
                g.setColor(p.getPieceSide()==Side.WHITE ? new Color(250,246,234) : new Color(27,35,45)); g.fill(translated);
                g.setStroke(new BasicStroke((float)Math.max(1,cell*.016)));
                g.setColor(p.getPieceSide()==Side.WHITE ? new Color(48,53,58) : new Color(204,212,215)); g.draw(translated);
            }
        }
        g.setFont(new Font("Segoe UI",Font.BOLD,13)); g.setColor(new Color(186,199,203));
        for(int i=0;i<8;i++) {
            String file=square(7,i).toString().substring(0,1).toLowerCase(Locale.ROOT);
            String rank=square(i,0).toString().substring(1);
            g.drawString(file,(float)(left+(i+.5)*cell-4),(float)(top+8*cell+22));
            g.drawString(rank,(float)(left-22),(float)(top+(i+.5)*cell+5));
        }
        g.dispose();
    }
}
