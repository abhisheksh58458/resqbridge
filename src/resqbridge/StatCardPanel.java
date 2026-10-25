package resqbridge;

import javax.swing.*;
import java.awt.*;

/** A rounded card: small muted title, big number, colored delta line, icon on the right. */
public class StatCardPanel extends RoundedPanel {

    private final JLabel valueLabel;
    private final JLabel deltaLabel;

    public StatCardPanel(String title, String value, String delta, Color deltaColor, AppIcon.Kind iconKind, Color iconColor) {
        super(Theme.BG_CARD, Theme.BORDER, 14);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel textCol = new JPanel();
        textCol.setOpaque(false);
        textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(Theme.FONT_SMALL);
        titleLabel.setForeground(Theme.TEXT_MUTED);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        valueLabel = new JLabel(value);
        valueLabel.setFont(Theme.FONT_BIG_NUM);
        valueLabel.setForeground(Theme.TEXT_PRIMARY);
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLabel.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        deltaLabel = new JLabel(delta);
        deltaLabel.setFont(Theme.FONT_SMALL);
        deltaLabel.setForeground(deltaColor);
        deltaLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        textCol.add(titleLabel);
        textCol.add(valueLabel);
        textCol.add(deltaLabel);

        JLabel iconLabel = new JLabel(new AppIcon(iconKind, 40, iconColor));
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);

        add(textCol, BorderLayout.CENTER);
        add(iconLabel, BorderLayout.EAST);
    }

    public void setValue(String value) { valueLabel.setText(value); }

    public void setDelta(String delta, Color color) {
        deltaLabel.setText(delta);
        deltaLabel.setForeground(color);
    }
}
