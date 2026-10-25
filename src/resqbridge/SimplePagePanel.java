package resqbridge;

import javax.swing.*;
import java.awt.*;

public class SimplePagePanel extends JPanel {

    public SimplePagePanel(String heading, String message, AppIcon.Kind icon) {
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        RoundedPanel card = new RoundedPanel(Theme.BG_CARD, Theme.BORDER, 16);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(40, 40, 40, 40));
        card.setPreferredSize(new Dimension(420, 220));

        JLabel iconLabel = new JLabel(new AppIcon(icon, 48, Theme.ACCENT_CYAN));
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(iconLabel, BorderLayout.NORTH);

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
        JLabel head = new JLabel(heading);
        head.setFont(Theme.FONT_TITLE);
        head.setForeground(Theme.TEXT_PRIMARY);
        head.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel msg = new JLabel("<html><div style='text-align:center;width:280px;'>" + message + "</div></html>");
        msg.setFont(Theme.FONT_BASE);
        msg.setForeground(Theme.TEXT_MUTED);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);
        msg.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        textCol.add(head);
        textCol.add(msg);
        card.add(textCol, BorderLayout.CENTER);

        add(card);
    }
}
