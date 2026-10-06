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
    private static final Color LIGHT=new Color(236,226,207), DARK=new Color(87,117,103);
    private int hovered=-1;
    public BoardView() {
        setPreferredSize(new Dimension(620,620)); setMinimumSize(new Dimension(400,400));
        setBackground(new Color(24,32,43));
        addMouseListener(new MouseAdapter() { public void mousePressed(MouseEvent e) {
            double cell=(Math.min(getWidth(),getHeight())-64)/8.0;
            double x=(getWidth()-8*cell)/2.0,y=(getHeight()-8*cell)/2.0;
            int col=(int)Math.floor((e.getX()-x)/cell),row=(int)Math.floor((e.getY()-y)/cell);
            if(interactive && handler!=null && col>=0 && col<8 && row>=0 && row<8) handler.accept(row*8+col);
        }
            public void mouseExited(MouseEvent e) { hovered=-1; repaint(); }
        });
        addMouseMotionListener(new MouseMotionAdapter() { public void mouseMoved(MouseEvent e) {
            double cell=(Math.min(getWidth(),getHeight())-64)/8.0;
            double x=(getWidth()-8*cell)/2.0,y=(getHeight()-8*cell)/2.0;
            int col=(int)Math.floor((e.getX()-x)/cell),row=(int)Math.floor((e.getY()-y)/cell);
            hovered=interactive && col>=0 && col<8 && row>=0 && row<8 ? row*8+col : -1;
            setCursor(Cursor.getPredefinedCursor(hovered>=0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR)); repaint();
        }});
    }
    public void onClick(IntConsumer handler) { this.handler=handler; }
    public void display(Board board,Side side,Square selected,Move last,boolean interactive) {
        position=new Board(); position.loadFromFen(board.getFen()); orientation=side;
        this.selected=selected; this.last=last; this.interactive=interactive;
        if(!interactive) { hovered=-1; setCursor(Cursor.getDefaultCursor()); }
        HashSet<Square> legal=new HashSet<>();
        if(selected!=null) for(Move move:board.legalMoves()) if(move.getFrom()==selected) legal.add(move.getTo());
        targets=legal; repaint();
    }
    private Square square(int row,int col) { return Square.squareAt(orientation==Side.WHITE ? (7-row)*8+col : row*8+7-col); }
    @Override protected void paintComponent(Graphics original) {
        super.paintComponent(original); Graphics2D g=(Graphics2D)original.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL,RenderingHints.VALUE_STROKE_PURE);
        double cell=(Math.min(getWidth(),getHeight())-64)/8.0;
        double left=(getWidth()-8*cell)/2,top=(getHeight()-8*cell)/2;
        double size=cell*8;
        for(int i=12;i>0;i--) {
            g.setColor(new Color(0,0,0,Math.max(1,14-i)));
            g.fill(new RoundRectangle2D.Double(left-15-i*.5,top-13+i*.3,size+30+i,size+30+i,28,28));
        }
        g.setPaint(new GradientPaint((float)left,(float)top,new Color(60,70,70),(float)(left+size),(float)(top+size),new Color(23,34,37)));
        g.fill(new RoundRectangle2D.Double(left-16,top-16,size+32,size+32,24,24));
        g.setColor(new Color(160,143,105)); g.setStroke(new BasicStroke(1));
        g.draw(new RoundRectangle2D.Double(left-15,top-15,size+30,size+30,22,22));
        Shape boardClip=g.getClip();
        g.clip(new RoundRectangle2D.Double(left,top,size,size,8,8));
        for(int row=0;row<8;row++) for(int col=0;col<8;col++) {
            Square sq=square(row,col); double x=left+col*cell,y=top+row*cell;
            Color base=(row+col)%2==0 ? LIGHT : DARK;
            if(last!=null && (last.getFrom()==sq || last.getTo()==sq)) base=(row+col)%2==0 ? new Color(226,211,136) : new Color(164,172,92);
            if(sq==selected) base=new Color(93,171,164);
            g.setPaint(new GradientPaint((float)x,(float)y,base,(float)x,(float)(y+cell),new Color(Math.max(0,base.getRed()-7),Math.max(0,base.getGreen()-7),Math.max(0,base.getBlue()-7))));
            g.fill(new Rectangle2D.Double(x,y,cell,cell));
            if(row*8+col==hovered || sq==selected) {
                g.setColor(new Color(244,216,154,190)); g.setStroke(new BasicStroke(sq==selected ? 3 : 1.5f));
                g.draw(new RoundRectangle2D.Double(x+2,y+2,cell-4,cell-4,6,6));
            }
            if(targets.contains(sq)) {
                g.setColor(new Color(22,58,48,120));
                if(position.getPiece(sq)==Piece.NONE) g.fill(new Ellipse2D.Double(x+cell*.41,y+cell*.41,cell*.18,cell*.18));
                else { g.setStroke(new BasicStroke(4)); g.draw(new Ellipse2D.Double(x+cell*.08,y+cell*.08,cell*.84,cell*.84)); }
            }
            Piece p=position.getPiece(sq);
            if(p!=Piece.NONE) {
                paintPiece(g,p,x,y,cell);
            }
        }
        g.setClip(boardClip);
        g.setFont(new Font("Segoe UI",Font.BOLD,12)); g.setColor(new Color(202,184,143));
        for(int i=0;i<8;i++) {
            String file=square(7,i).toString().substring(0,1).toLowerCase(Locale.ROOT);
            String rank=square(i,0).toString().substring(1);
            g.drawString(file,(float)(left+(i+.5)*cell-4),(float)(top+8*cell+22));
            g.drawString(rank,(float)(left-22),(float)(top+(i+.5)*cell+5));
        }
        g.dispose();
    }
    private static void paintPiece(Graphics2D parent,Piece piece,double x,double y,double cell) {
        Graphics2D g=(Graphics2D)parent.create();
        g.translate(x+cell*.09,y+cell*.05); g.scale(cell*.0082,cell*.0082);
        boolean white=piece.getPieceSide()==Side.WHITE;
        g.setColor(new Color(0,0,0,42)); g.fill(new Ellipse2D.Double(19,90,65,7));
        java.awt.geom.Area silhouette=new java.awt.geom.Area();
        Path2D.Double body=new Path2D.Double();
        String type=piece.name().substring(piece.name().indexOf('_')+1);
        switch(type) {
            case "PAWN" -> {
                silhouette.add(new java.awt.geom.Area(new Ellipse2D.Double(37,24,26,26)));
                body.moveTo(39,51); body.curveTo(42,65,34,74,29,81); body.lineTo(71,81); body.curveTo(66,74,58,65,61,51); body.closePath();
            }
            case "ROOK" -> {
                body.moveTo(28,24); body.lineTo(39,24); body.lineTo(39,34); body.lineTo(45,34); body.lineTo(45,24); body.lineTo(55,24); body.lineTo(55,34); body.lineTo(61,34); body.lineTo(61,24); body.lineTo(72,24); body.lineTo(72,44); body.lineTo(63,50); body.lineTo(65,76); body.lineTo(72,81); body.lineTo(28,81); body.lineTo(35,76); body.lineTo(37,50); body.lineTo(28,44); body.closePath();
            }
            case "KNIGHT" -> {
                body.moveTo(26,81); body.curveTo(28,63,49,62,53,44); body.lineTo(38,51); body.lineTo(26,48); body.lineTo(26,39); body.lineTo(43,23); body.lineTo(45,15); body.lineTo(53,22); body.lineTo(62,21); body.curveTo(78,34,78,56,68,76); body.lineTo(73,81); body.closePath();
            }
            case "BISHOP" -> {
                body.moveTo(35,57); body.curveTo(20,45,34,31,50,19); body.curveTo(66,31,80,45,65,57); body.lineTo(59,62); body.curveTo(58,70,64,75,71,81); body.lineTo(29,81); body.curveTo(36,75,42,70,41,62); body.closePath();
                silhouette.add(new java.awt.geom.Area(new Ellipse2D.Double(46,11,8,8)));
            }
            case "QUEEN" -> {
                body.moveTo(25,31); body.lineTo(37,46); body.lineTo(39,24); body.lineTo(50,42); body.lineTo(61,24); body.lineTo(63,46); body.lineTo(75,31); body.lineTo(65,60); body.lineTo(59,65); body.curveTo(59,73,65,77,72,81); body.lineTo(28,81); body.curveTo(35,77,41,73,41,65); body.lineTo(35,60); body.closePath();
                for(double[] dot:new double[][]{{21,24},{35,17},{57,17},{71,24}}) silhouette.add(new java.awt.geom.Area(new Ellipse2D.Double(dot[0],dot[1],8,8)));
            }
            case "KING" -> {
                body.moveTo(36,39); body.curveTo(21,27,16,51,36,61); body.lineTo(41,65); body.curveTo(41,73,35,77,28,81); body.lineTo(72,81); body.curveTo(65,77,59,73,59,65); body.lineTo(64,61); body.curveTo(84,51,79,27,64,39); body.closePath();
                silhouette.add(new java.awt.geom.Area(new RoundRectangle2D.Double(46,11,8,30,2,2)));
                silhouette.add(new java.awt.geom.Area(new RoundRectangle2D.Double(37,20,26,8,2,2)));
            }
        }
        silhouette.add(new java.awt.geom.Area(body));
        silhouette.add(new java.awt.geom.Area(new RoundRectangle2D.Double(26,78,48,8,4,4)));
        silhouette.add(new java.awt.geom.Area(new RoundRectangle2D.Double(20,85,60,9,4,4)));
        g.setPaint(new GradientPaint(27,20,white ? new Color(255,252,242) : new Color(77,86,91),74,94,white ? new Color(212,198,170) : new Color(21,29,33)));
        g.fill(silhouette); g.setColor(white ? new Color(89,77,61) : new Color(183,195,191));
        g.setStroke(new BasicStroke(1.65f,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); g.draw(silhouette);
        g.setColor(white ? new Color(118,101,78) : new Color(144,160,159)); g.setStroke(new BasicStroke(1.4f));
        g.draw(new Line2D.Double(29,84,71,84));
        if(type.equals("ROOK")) g.draw(new Line2D.Double(36,48,64,48));
        if(type.equals("QUEEN") || type.equals("KING") || type.equals("BISHOP")) g.draw(new Line2D.Double(38,62,62,62));
        if(type.equals("BISHOP")) { g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND)); g.draw(new Line2D.Double(51,30,41,43)); }
        if(type.equals("KNIGHT")) { g.fill(new Ellipse2D.Double(43,31,4,4)); g.draw(new Line2D.Double(29,43,34,43)); }
        g.dispose();
    }
}
