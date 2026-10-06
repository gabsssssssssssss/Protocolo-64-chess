import com.github.bhlangonijr.chesslib.*;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.List;

public final class PgnViewer extends JFrame {
    private final BoardView boardView=new BoardView();
    private final JTextField playerFilter=new JTextField();
    private List<PgnCollection.Replay> collection=List.of();
    private final JComboBox<PgnCollection.Replay> games=new JComboBox<>();
    private final JLabel status=new JLabel("Abra uma coleção PGN local."), file=new JLabel("Nenhum arquivo carregado");
    private final JTextArea history=new JTextArea(18,26);
    private final JSpinner speed=new JSpinner(new SpinnerNumberModel(2.0,.25,10.0,.25));
    private final JButton play=new JButton("Reproduzir"), open=new JButton("Abrir PGN");
    private final javax.swing.Timer timer=new javax.swing.Timer(2000,e -> step(1));
    private int ply=0;
    private PgnCollection.Replay current;
    private final JButton analyse=new JButton("Analisar posição com Stockfish");
    private final JTextArea analysis=new JTextArea(6,26);
    private final JTabbedPane studyTabs=new JTabbedPane();
    private long positionVersion=0,loadVersion=0;
    private boolean analysing=false;
    public PgnViewer() {
        super("Partidas PGN • Estudo offline"); setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        JPanel side=new JPanel(); side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS));
        side.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JLabel title=new JLabel("BIBLIOTECA DE PARTIDAS"); title.setFont(new Font("Segoe UI",Font.BOLD,21)); side.add(title);
        side.add(new JLabel("● ESTUDO OFFLINE")); side.add(Box.createVerticalStrut(12));
        open.addActionListener(e -> choose()); side.add(open); side.add(file);
        JButton carlsen=new JButton("Abrir amostra do Magnus Carlsen"); carlsen.addActionListener(e -> load(Path.of("datasets/carlsen-mundial-2021-1.pgn"))); side.add(carlsen);
        side.add(new JLabel("Filtrar jogador (ex.: Carlsen)")); side.add(playerFilter);
        playerFilter.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
        });
        games.setPreferredSize(new Dimension(360,32)); games.setMaximumSize(new Dimension(360,32));
        games.addActionListener(e -> { pause(); current=(PgnCollection.Replay)games.getSelectedItem(); ply=0; draw(); }); side.add(games);
        JPanel navigation=new JPanel();
        button(navigation,"Início",() -> { pause(); ply=0; draw(); });
        button(navigation,"Anterior",() -> { pause(); step(-1); });
        button(navigation,"Próximo",() -> { pause(); step(1); });
        button(navigation,"Fim",() -> { pause(); if(current!=null) ply=current.san().size(); draw(); }); side.add(navigation);
        play.addActionListener(e -> { if(timer.isRunning()) pause(); else if(current!=null) { if(ply==current.san().size()) ply=0; draw(); timer.start(); play.setText("Pausar"); } }); side.add(play);
        side.add(new JLabel("Segundos entre lances (não é relógio de partida)")); side.add(speed);
        speed.addChangeListener(e -> { int delay=(int)(((Number)speed.getValue()).doubleValue()*1000); timer.setDelay(delay); timer.setInitialDelay(delay); });
        history.setEditable(false); history.setLineWrap(true); history.setWrapStyleWord(true);
        side.add(Box.createVerticalStrut(8)); side.add(analyse);
        analyse.addActionListener(e -> analysePosition());
        analysis.setEditable(false); analysis.setLineWrap(true); analysis.setWrapStyleWord(true);
        analysis.setText("Pause em uma posição e consulte a análise local (2 segundos de busca).");
        studyTabs.addTab("Lances",new JScrollPane(history));
        studyTabs.addTab("Análise",new JScrollPane(analysis));
        studyTabs.setMinimumSize(new Dimension(300,140)); side.add(studyTabs);
        add(boardView,BorderLayout.CENTER); add(side,BorderLayout.EAST); add(status,BorderLayout.SOUTH);
        addWindowListener(new WindowAdapter() { public void windowClosed(WindowEvent e) { timer.stop(); } });
        UiTheme.sidebar(side,430); UiTheme.style(getContentPane());
        status.setBorder(BorderFactory.createEmptyBorder(10,16,10,16)); setMinimumSize(new Dimension(980,700));
        draw(); pack(); setLocationRelativeTo(null);
    }
    private void button(JPanel panel,String label,Runnable task) { JButton b=new JButton(label); b.addActionListener(e -> task.run()); panel.add(b); }
    private void pause() { timer.stop(); play.setText("Reproduzir"); }
    private void step(int change) { if(current==null) return; ply=Math.max(0,Math.min(current.san().size(),ply+change)); draw(); if(ply==current.san().size()) pause(); }
    private void choose() {
        JFileChooser chooser=new JFileChooser(Path.of("datasets").toFile()); chooser.setFileFilter(new FileNameExtensionFilter("Partidas de xadrez (*.pgn)","pgn"));
        if(chooser.showOpenDialog(this)==JFileChooser.APPROVE_OPTION) load(chooser.getSelectedFile().toPath());
    }
    public void load(Path path) {
        long request=++loadVersion;
        pause(); open.setEnabled(false); status.setText("Importando e validando partidas...");
        new SwingWorker<List<PgnCollection.Replay>,Void>() {
            protected List<PgnCollection.Replay> doInBackground() throws Exception { return PgnCollection.read(path); }
            protected void done() {
                if(request!=loadVersion || !isDisplayable()) return;
                open.setEnabled(true);
                try {
                    var imported=get(); collection=imported; playerFilter.setText(""); filter();
                    file.setText(path.getFileName()+" — "+imported.size()+" partidas"); draw();
                } catch(Exception e) { Throwable cause=e.getCause()==null ? e : e.getCause(); status.setText("Falha de importação: "+cause.getMessage()); }
            }
        }.execute();
    }
    private void filter() {
        pause(); var filtered=PgnCollection.filterPlayer(collection,playerFilter.getText());
        games.setModel(new DefaultComboBoxModel<>(filtered.toArray(PgnCollection.Replay[]::new)));
        current=filtered.isEmpty() ? null : filtered.getFirst(); ply=0; draw();
        if(filtered.isEmpty()) status.setText("Nenhuma partida corresponde ao jogador informado.");
    }
    private void draw() {
        positionVersion++;
        analysis.setText("Pause em uma posição e consulte a análise local (2 segundos de busca).");
        analyse.setEnabled(current!=null && !analysing);
        Board board=new Board(); if(current!=null) board.loadFromFen(current.positions().get(ply));
        com.github.bhlangonijr.chesslib.move.Move last=null;
        if(current!=null && ply>0) {
            Board before=new Board(); before.loadFromFen(current.positions().get(ply-1));
            String uci=current.uci().get(ply-1);
            last=before.legalMoves().stream().filter(m -> m.toString().equalsIgnoreCase(uci)).findFirst().orElse(null);
        }
        boardView.display(board,Side.WHITE,null,last,false);
        if(current==null) history.setText("");
        if(current!=null) {
            StringBuilder text=new StringBuilder();
            for(int i=0;i<current.san().size();i++) { if(i==ply-1) text.append("▶ "); if(i%2==0) text.append(i/2+1).append(". "); text.append(current.san().get(i)).append(i%2==0 ? " " : "\n"); }
            history.setText(text.toString()); status.setText("Lance "+ply+"/"+current.san().size()+" — "+(ply==0 ? "Posição inicial" : current.san().get(ply-1)));
        }
    }
    private void analysePosition() {
        if(current==null || analysing) return;
        pause();
        String fen=current.positions().get(ply);
        Board board=new Board(); board.loadFromFen(fen);
        if(board.isMated() || board.isStaleMate()) { analysis.setText(board.isMated() ? "A posição já está em xeque-mate." : "Afogamento: não existem lances legais."); return; }
        String initial=current.positions().getFirst();
        String moves=String.join(" ",current.uci().subList(0,ply));
        long version=positionVersion;
        analysing=true; analyse.setEnabled(false); analysis.setText("Preparando Stockfish e analisando a posição...");
        studyTabs.setSelectedIndex(1);
        new SwingWorker<String,Void>() {
            protected String doInBackground() throws Exception {
                try(Stockfish engine=new Stockfish(Stockfish.locate())) {
                    return AnalysisText.format(engine.analyse(initial,moves,2000),fen);
                }
            }
            protected void done() {
                analysing=false; analyse.setEnabled(current!=null);
                if(!isDisplayable() || version!=positionVersion) return;
                try { analysis.setText(get()); }
                catch(Exception e) { Throwable cause=e.getCause()==null ? e : e.getCause(); analysis.setText("Erro na análise: "+cause.getMessage()); }
            }
        }.execute();
    }
    public static void main(String[] args) { SwingUtilities.invokeLater(() -> {
        UiTheme.install();
        PgnViewer viewer=new PgnViewer();
        if(args.length>0 && args[0].equals("--preview")) {
            try {
                var imported=PgnCollection.read(Path.of("datasets/exemplo.pgn"));
                viewer.games.setModel(new DefaultComboBoxModel<>(imported.toArray(PgnCollection.Replay[]::new)));
                viewer.current=imported.getFirst(); viewer.step(2);
                viewer.step(-1);
                if(viewer.ply!=1) throw new AssertionError("Navegação para trás");
                viewer.games.setSelectedIndex(1);
                if(viewer.ply!=0 || viewer.current!=imported.get(1)) throw new AssertionError("Troca de partida");
                viewer.step(10); viewer.speed.setValue(3.0);
                if(viewer.timer.getDelay()!=3000) throw new AssertionError("Controle de velocidade");
                viewer.file.setText("exemplo.pgn — 2 partidas");
                try(Stockfish engine=new Stockfish(Stockfish.locate())) {
                    var result=engine.analyse(viewer.current.positions().getFirst(),String.join(" ",viewer.current.uci().subList(0,viewer.ply)),200);
                    viewer.analysis.setText(AnalysisText.format(result,viewer.current.positions().get(viewer.ply)));
                    viewer.studyTabs.setSelectedIndex(1);
                }
                layoutTree(viewer.getContentPane());
                var content=viewer.getContentPane();
                var image=new java.awt.image.BufferedImage(content.getWidth(),content.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);
                var graphics=image.createGraphics(); content.printAll(graphics); graphics.dispose();
                Files.createDirectories(Path.of("tmp"));
                javax.imageio.ImageIO.write(image,"png",Path.of("tmp/pgn-viewer.png").toFile());
                System.out.println("OK: navegação, seleção de partida, velocidade e renderização PGN.");
            } catch(Throwable e) { e.printStackTrace(); System.exit(1); }
            finally { viewer.dispose(); }
        } else { viewer.setVisible(true); if(args.length>0) viewer.load(Path.of(args[0])); }
    }); }
    private static void layoutTree(Container container) {
        container.doLayout();
        for(Component child:container.getComponents()) if(child instanceof Container nested) layoutTree(nested);
    }
}
