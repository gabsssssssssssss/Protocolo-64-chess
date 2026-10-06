import com.github.bhlangonijr.chesslib.*;
import com.github.bhlangonijr.chesslib.move.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Locale;

public final class ChessChallenge extends JFrame {
    private final Board board = new Board();
    private final ArrayList<Move> moves = new ArrayList<>();
    private final JButton[] squares = new JButton[64];
    private final JComboBox<String> color = new JComboBox<>(new String[]{"Brancas", "Pretas"});
    private final JTextField clock = new JTextField("25:00"), entry = new JTextField();
    private final JTextArea history = new JTextArea(12, 25);
    private final JLabel status = new JLabel("Escolha nossa cor. O relógio físico é a referência.");
    private Stockfish engine;
    private Square selected;
    private boolean busy;
    private String lastMove = "";
    private final ArrayList<JButton> controls = new ArrayList<>();
    private final SessionStore session;

    private ChessChallenge(Path sessionPath) {
        super("Chess Challenge • Java + Stockfish local");
        session=new SessionStore(sessionPath);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        JPanel grid = new JPanel(new GridLayout(8,8));
        grid.setPreferredSize(new Dimension(560,560));
        for (int i=0;i<64;i++) {
            final int cell=i;
            squares[i]=new JButton();
            squares[i].setFont(new Font("Segoe UI Symbol",Font.PLAIN,42));
            squares[i].setMargin(new Insets(0,0,0,0));
            squares[i].setFocusPainted(false);
            squares[i].addActionListener(e -> click(cell));
            grid.add(squares[i]);
        }
        JPanel side=new JPanel();
        side.setBorder(BorderFactory.createEmptyBorder(16,16,16,16));
        side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS));
        side.add(new JLabel("CHESS CHALLENGE"));
        side.add(new JLabel("Nossa cor")); side.add(color);
        side.add(new JLabel("Tempo restante MM:SS — atualize pelo relógio físico")); side.add(clock);
        button(side,"Calcular nosso lance",this::calculate);
        side.add(new JLabel("Lance adversário: e2e4 (roque: e1g1)")); side.add(entry);
        entry.addActionListener(e -> submit());
        button(side,"Registrar lance",this::submit);
        button(side,"Desfazer último lance",this::undo);
        button(side,"Nova partida",this::reset);
        button(side,"Salvar PGN",this::save);
        button(side,"Restaurar última sessão",this::restore);
        history.setEditable(false);
        side.add(new JScrollPane(history));
        add(grid,BorderLayout.CENTER); add(side,BorderLayout.EAST);
        status.setBorder(BorderFactory.createEmptyBorder(12,12,12,12)); add(status,BorderLayout.SOUTH);
        color.addActionListener(e -> draw());
        addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) {
            if (busy) { status.setText("Aguarde a análise terminar antes de fechar."); return; }
            if(!moves.isEmpty()) checkpoint();
            if (engine!=null) engine.close(); dispose();
        }});
        draw(); pack(); setLocationRelativeTo(null);
    }
    private void button(JPanel panel,String text,Runnable action) {
        JButton b=new JButton(text); b.addActionListener(e -> action.run()); controls.add(b); panel.add(b);
    }
    private Side ours() { return color.getSelectedIndex()==0 ? Side.WHITE : Side.BLACK; }
    private Square square(int cell) {
        int row=cell/8,col=cell%8;
        return Square.squareAt(ours()==Side.WHITE ? (7-row)*8+col : row*8+7-col);
    }
    private boolean over() { return board.isMated() || board.isStaleMate() || board.isDraw(); }
    private void draw() {
        for(int i=0;i<64;i++) {
            Square sq=square(i); Piece p=board.getPiece(sq);
            String glyph=switch(p) {
                case WHITE_KING -> "♔"; case WHITE_QUEEN -> "♕"; case WHITE_ROOK -> "♖";
                case WHITE_BISHOP -> "♗"; case WHITE_KNIGHT -> "♘"; case WHITE_PAWN -> "♙";
                case BLACK_KING -> "♚"; case BLACK_QUEEN -> "♛"; case BLACK_ROOK -> "♜";
                case BLACK_BISHOP -> "♝"; case BLACK_KNIGHT -> "♞"; case BLACK_PAWN -> "♟";
                default -> "";
            };
            squares[i].setText("<html><center>"+glyph+"<br><font size='2'>"+sq.toString().toLowerCase(Locale.ROOT)+"</font></center></html>");
            Color bg=(i/8+i%8)%2==0 ? new Color(235,236,208) : new Color(119,149,86);
            if(lastMove.length()>=4 && (sq.toString().equalsIgnoreCase(lastMove.substring(0,2)) || sq.toString().equalsIgnoreCase(lastMove.substring(2,4)))) bg=new Color(207,191,98);
            if(sq==selected) bg=new Color(114,185,207);
            squares[i].setBackground(bg); squares[i].setForeground(Color.BLACK);
            squares[i].setEnabled(!busy);
        }
        color.setEnabled(moves.isEmpty() && !busy);
        controls.forEach(b -> b.setEnabled(!busy)); entry.setEnabled(!busy); clock.setEnabled(!busy);
        StringBuilder text=new StringBuilder();
        for(int i=0;i<moves.size();i++) { if(i%2==0) text.append(i/2+1).append(". "); text.append(moves.get(i)).append(i%2==0 ? " " : "\n"); }
        history.setText(text.toString());
        if(over()) status.setText(board.isMated() ? "Xeque-mate. Fim de partida." : "Posição de empate: confirme com a organização.");
    }
    private void click(int cell) {
        if(busy || over() || board.getSideToMove()==ours()) return;
        Square target=square(cell);
        if(selected==null) {
            if(board.getPiece(target)!=Piece.NONE && board.getPiece(target).getPieceSide()==board.getSideToMove()) selected=target;
        } else {
            Square from=selected; selected=null;
            var candidates=board.legalMoves().stream().filter(m -> m.getFrom()==from && m.getTo()==target).toList();
            if(candidates.isEmpty()) status.setText("Lance inválido. Escolha novamente a origem.");
            else {
                Move move=candidates.get(0);
                if(move.getPromotion()!=Piece.NONE) {
                    Object choice=JOptionPane.showInputDialog(this,"Escolha a promoção","Promoção",JOptionPane.QUESTION_MESSAGE,null,new String[]{"q","r","b","n"},"q");
                    move=choice==null ? null : candidates.stream().filter(m -> m.toString().toLowerCase(Locale.ROOT).endsWith(choice.toString())).findFirst().orElse(null);
                }
                if(move!=null) push(move);
            }
        }
        draw();
    }
    private Move legal(String uci) {
        return board.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(uci.trim())).findFirst().orElseThrow(() -> new IllegalArgumentException("Lance inválido. Use e2e4 ou clique nas casas; promoção: e7e8q."));
    }
    private void push(Move move) {
        if(!board.doMove(move,true)) throw new IllegalArgumentException("Lance rejeitado.");
        moves.add(move); lastMove=move.toString(); selected=null;
        status.setText("Lance registrado. Confira o tabuleiro físico."); draw();
        checkpoint();
    }
    private void submit() {
        if(busy || over()) return;
        if(board.getSideToMove()==ours()) { status.setText("É nossa vez. Clique em calcular."); return; }
        try { push(legal(entry.getText())); entry.setText(""); } catch(Exception e) { error(e); }
    }
    private void calculate() {
        if(busy || over()) return;
        if(board.getSideToMove()!=ours()) { status.setText("Registre primeiro o lance adversário."); return; }
        try {
            String[] parts=clock.getText().trim().split(":");
            if(parts.length!=2) throw new IllegalArgumentException("Use MM:SS no relógio.");
            int min=Integer.parseInt(parts[0]),sec=Integer.parseInt(parts[1]);
            if(min<0 || sec<0 || sec>59) throw new IllegalArgumentException("Tempo inválido.");
            long remaining=(min*60L+sec)*1000;
            var budget=TimeManager.allocate(board,remaining);
            checkpoint();
            String position=String.join(" ",moves.stream().map(m -> m.toString().toLowerCase(Locale.ROOT)).toList());
            busy=true; selected=null; draw();
            status.setText("Analisando por até "+budget.millis()/1000.0+"s. "+budget.reason());
            new SwingWorker<String,Void>() {
                long elapsed;
                protected String doInBackground() throws Exception {
                    long start=System.nanoTime();
                    try {
                        if(engine==null) engine=new Stockfish(Stockfish.locate());
                        String result=engine.play(position,budget.millis());
                        elapsed=(System.nanoTime()-start)/1000000;
                        return result;
                    } catch(Exception e) { if(engine!=null) engine.close(); engine=null; throw e; }
                }
                protected void done() {
                    busy=false;
                    try {
                        String result=get();
                        long left=Math.max(0,(remaining-elapsed)/1000);
                        clock.setText(String.format("%02d:%02d",left/60,left%60));
                        if(elapsed>=remaining) {
                            status.setText("Tempo informado esgotado durante o cálculo. Lance não aplicado; confira o relógio físico.");
                            checkpoint(); draw(); return;
                        }
                        push(legal(result));
                        status.setText("Jogue "+result.substring(0,2)+" → "+result.substring(2)+". Atualize o tempo pelo relógio físico.");
                    } catch(Exception e) { error(e.getCause()!=null ? e.getCause() : e); }
                    draw();
                }
            }.execute();
        } catch(Exception e) { error(e); }
    }
    private void undo() {
        if(busy || moves.isEmpty()) return;
        board.undoMove(); moves.remove(moves.size()-1); selected=null;
        lastMove=moves.isEmpty() ? "" : moves.getLast().toString(); draw();
        status.setText("Lance desfeito. Ajuste o relógio físico.");
        checkpoint();
    }
    private void reset() {
        if(busy) return;
        if(!moves.isEmpty() && JOptionPane.showConfirmDialog(this,"Descartar a partida? Salve o PGN antes.","Nova partida",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        board.loadFromFen(new Board().getFen()); moves.clear(); selected=null; lastMove=""; clock.setText("25:00");
        // Novo processo garante limpeza de estado entre partidas sem bloquear a interface em handshake.
        if(engine!=null) { engine.close(); engine=null; }
        draw(); status.setText("Nova partida. Escolha nossa cor.");
        checkpoint();
    }
    private void save() {
        try {
            MoveList list=new MoveList(); list.addAll(moves);
            String result=board.isMated() ? (board.getSideToMove()==Side.WHITE ? "0-1" : "1-0") : (over() ? "1/2-1/2" : "*");
            String text="[Event \"Chess Challenge - treinamento\"]\n[Result \""+result+"\"]\n\n";
            String[] san=list.toSanArray();
            for(int i=0;i<san.length;i++) text+=(i%2==0 ? (i/2+1)+". " : "")+san[i]+" ";
            Path folder=Path.of("partidas"); Files.createDirectories(folder);
            Path path=folder.resolve("partida-"+System.currentTimeMillis()+".pgn");
            Files.writeString(path,text+result+"\n"); status.setText("Salvo: "+path);
        } catch(Exception e) { error(e); }
    }
    private void error(Throwable e) { status.setText("Erro: "+e.getMessage()); }
    private void checkpoint() {
        try { session.save(ours(),clock.getText().trim(),moves); }
        catch(Exception e) { status.setText("Falha ao salvar sessão: "+e.getMessage()+". Salve o PGN e confira o disco."); }
    }
    private void restore() {
        if(busy) return;
        if(!moves.isEmpty() && JOptionPane.showConfirmDialog(this,"Substituir a partida atual pela sessão salva?","Restaurar",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION) return;
        try { applySession(session.load()); }
        catch(Exception e) { error(e); }
    }
    private void applySession(SessionStore.Session saved) {
        board.loadFromFen(new Board().getFen()); moves.clear();
        for(Move move:saved.moves()) { board.doMove(move,true); moves.add(move); }
        color.setSelectedIndex(saved.color()==Side.WHITE ? 0 : 1);
        clock.setText(saved.clock()); selected=null; entry.setText("");
        lastMove=moves.isEmpty() ? "" : moves.getLast().toString();
        if(engine!=null) { engine.close(); engine=null; }
        draw(); status.setText("Sessão restaurada. Confira posição e ajuste o tempo pelo relógio físico antes de continuar.");
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            boolean test=args.length>0 && (args[0].equals("--ui-test") || args[0].equals("--preview"));
            ChessChallenge app=new ChessChallenge(Path.of(test ? "tmp/sessao-teste.properties" : "partidas/sessao.properties"));
            if(args.length>0 && args[0].equals("--ui-test")) {
                app.testInterface();
            } else if(args.length>0 && args[0].equals("--preview")) {
                try {
                    app.setVisible(false);
                    app.getContentPane().doLayout();
                    layoutTree(app.getContentPane());
                    var content=app.getContentPane();
                    var image=new java.awt.image.BufferedImage(content.getWidth(),content.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                    var graphics=image.createGraphics(); content.printAll(graphics); graphics.dispose();
                    Files.createDirectories(Path.of("tmp"));
                    javax.imageio.ImageIO.write(image,"png",Path.of("tmp/interface.png").toFile());
                    System.out.println("Preview salvo em tmp/interface.png");
                } catch(Exception e) { e.printStackTrace(); System.exit(1); }
                finally { app.dispose(); }
            } else app.setVisible(true);
        });
    }
    private void testInterface() {
        clock.setText("00:05");
        calculate();
        if(!busy || entry.isEnabled()) { System.err.println("Falha: interface não bloqueou entrada durante cálculo."); dispose(); System.exit(1); }
        long deadline=System.nanoTime()+30_000_000_000L;
        var timer=new javax.swing.Timer(100,null);
        timer.addActionListener(e -> {
            if(busy && System.nanoTime()<deadline) return;
            timer.stop();
            try {
                if(busy || moves.size()!=1 || board.getSideToMove()!=Side.BLACK || !entry.isEnabled()) throw new AssertionError("Falha no retorno do cálculo à interface: "+status.getText());
                entry.setText("e7e4"); submit();
                if(moves.size()!=1) throw new AssertionError("Entrada ilegal mudou a partida.");
                entry.setText(board.legalMoves().getFirst().toString()); submit();
                if(moves.size()!=2 || board.getSideToMove()!=Side.WHITE) throw new AssertionError("Entrada do adversário não registrada.");
                undo();
                if(moves.size()!=1 || board.getSideToMove()!=Side.BLACK) throw new AssertionError("Desfazer não restaurou a vez.");
                String before=board.getFen();
                var saved=session.load();
                board.loadFromFen(new Board().getFen()); moves.clear();
                applySession(saved);
                if(!board.getFen().equals(before) || moves.size()!=1) throw new AssertionError("Sessão não restaurou a posição.");
                System.out.println("OK: cálculo assíncrono, bloqueio de entrada, retorno à interface, rejeição de lance ilegal, registro e desfazer.");
            } catch(Throwable failure) { failure.printStackTrace(); System.exit(1); }
            finally { if(engine!=null) engine.close(); dispose(); }
        });
        timer.start();
    }
    private static void layoutTree(Container container) {
        container.doLayout();
        for(Component child:container.getComponents()) if(child instanceof Container nested) layoutTree(nested);
    }
}
