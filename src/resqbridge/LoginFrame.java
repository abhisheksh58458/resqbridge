package resqbridge;

import javax.swing.*;
import java.awt.*;

public class LoginFrame extends JFrame {

    public LoginFrame() {
        super("ResQBridge \u2014 Sign In");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(440, 560);
        setMinimumSize(new Dimension(380, 520));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_APP);
        setLayout(new GridBagLayout());

        DataStore store = new DataStore();

        RoundedPanel card = new RoundedPanel(Theme.BG_CARD, Theme.BORDER, 18);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(BorderFactory.createEmptyBorder(32, 32, 28, 32));
        card.setPreferredSize(new Dimension(340, 460));
        card.setMaximumSize(new Dimension(340, 460));

        JLabel logo = new JLabel(new AppIcon(AppIcon.Kind.TENT, 42, Theme.ACCENT_CYAN));
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("ResQBridge");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.ACCENT_CYAN);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = new JLabel("Disaster Relief Logistics System");
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_MUTED);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(logo);
        card.add(Box.createVerticalStrut(10));
        card.add(title);
        card.add(sub);
        card.add(Box.createVerticalStrut(26));

        JTextField userField = new JTextField();
        JPasswordField passField = new JPasswordField();
        for (JTextField f : new JTextField[]{userField, passField}) {
            f.setBackground(Theme.BG_CARD_ALT);
            f.setForeground(Theme.TEXT_PRIMARY);
            f.setCaretColor(Theme.TEXT_PRIMARY);
            f.setMaximumSize(new Dimension(276, 34));
            f.setAlignmentX(Component.LEFT_ALIGNMENT);
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        }

        card.add(fieldBlock("Username", userField));
        card.add(Box.createVerticalStrut(12));
        card.add(fieldBlock("Password", passField));
        card.add(Box.createVerticalStrut(18));

        JLabel error = new JLabel(" ");
        error.setForeground(Theme.RED);
        error.setFont(Theme.FONT_SMALL);
        error.setAlignmentX(Component.CENTER_ALIGNMENT);

        JButton signIn = new JButton("Sign In");
        signIn.setBackground(Theme.ACCENT_BLUE);
        signIn.setForeground(Color.WHITE);
        signIn.setFocusPainted(false);
        signIn.setAlignmentX(Component.CENTER_ALIGNMENT);
        signIn.setMaximumSize(new Dimension(276, 36));
        signIn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Runnable attempt = () -> {
            String u = userField.getText().trim();
            String p = new String(passField.getPassword());
            LoginAccount match = null;
            for (LoginAccount a : store.accounts) {
                if (a.username.equalsIgnoreCase(u) && a.password.equals(p)) {
                    match = a;
                    break;
                }
            }
            if (match == null) {
                error.setText("Invalid username or password.");
                return;
            }
            final LoginAccount account = match;
            dispose();
            SwingUtilities.invokeLater(() -> new ResQBridgeApp(store, account).setVisible(true));
        };
        signIn.addActionListener(e -> attempt.run());
        passField.addActionListener(e -> attempt.run());

        card.add(signIn);
        card.add(Box.createVerticalStrut(6));
        card.add(error);
        card.add(Box.createVerticalStrut(14));

        JLabel hint = new JLabel("<html><div style='text-align:center;'>"
                + "<b>Demo accounts</b><br>"
                + "admin / admin123 &mdash; Admin<br>"
                + "driver1 / driver123 &mdash; Driver<br>"
                + "camp1 / camp123 &mdash; Camp Employee"
                + "</div></html>");
        hint.setFont(Theme.FONT_SMALL);
        hint.setForeground(Theme.TEXT_FAINT);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(hint);

        add(card);
    }

    private JPanel fieldBlock(String label, JComponent field) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(276, 60));
        JLabel l = new JLabel(label);
        l.setForeground(Theme.TEXT_MUTED);
        l.setFont(Theme.FONT_SMALL);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(l);
        p.add(Box.createVerticalStrut(4));
        p.add(field);
        return p;
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
