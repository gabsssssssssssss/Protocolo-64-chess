import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public final class UiTheme {
    public static final Color BACKGROUND=new Color(24,32,43), PANEL=new Color(32,43,56), TEXT=new Color(232,238,241), MUTED=new Color(156,173,181);
    public static void install() {
        UIManager.put("Label.font",new Font("Segoe UI",Font.PLAIN,13));
        UIManager.put("Button.font",new Font("Segoe UI",Font.BOLD,13));
        UIManager.put("TextField.font",new Font("Segoe UI",Font.PLAIN,15));
        UIManager.put("ComboBox.font",new Font("Segoe UI",Font.PLAIN,13));
        UIManager.put("TabbedPane.background",PANEL);
        UIManager.put("TabbedPane.foreground",TEXT);
        UIManager.put("TabbedPane.selected",new Color(55,78,85));
    }
    public static void style(Component component) {
        if(component instanceof JPanel panel) panel.setBackground(PANEL);
        if(component instanceof JLabel label) label.setForeground(TEXT);
        if(component instanceof AbstractButton button) {
            button.setBackground(new Color(55,78,85)); button.setForeground(TEXT); button.setFocusPainted(false);
            button.setBorder(BorderFactory.createCompoundBorder(new LineBorder(new Color(75,100,108),1,true),new EmptyBorder(9,14,9,14)));
            button.setAlignmentX(Component.LEFT_ALIGNMENT);
        }
        if(component instanceof JTextArea text) { text.setBackground(BACKGROUND); text.setForeground(TEXT); text.setFont(new Font("Consolas",Font.PLAIN,14)); text.setMargin(new Insets(12,12,12,12)); }
        if(component instanceof JTextField text) { text.setBackground(BACKGROUND); text.setForeground(TEXT); text.setCaretColor(TEXT); text.setBorder(new CompoundBorder(new LineBorder(new Color(68,86,100)),new EmptyBorder(8,10,8,10))); }
        if(component instanceof JComboBox<?> combo) { combo.setBackground(BACKGROUND); combo.setForeground(TEXT); }
        if(component instanceof Container container) for(Component child:container.getComponents()) style(child);
        // BoardView mantém sua cor própria.
        if(component instanceof BoardView view) view.setBackground(BACKGROUND);
    }
    public static void sidebar(JPanel panel,int width) {
        panel.setPreferredSize(new Dimension(width,620));
        for(Component child:panel.getComponents()) if(child instanceof JComponent c) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            if(child instanceof JTextField || child instanceof JComboBox || child instanceof JButton || child instanceof JSpinner) c.setMaximumSize(new Dimension(width-32,42));
        }
    }
}
