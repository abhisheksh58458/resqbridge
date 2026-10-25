package resqbridge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SidebarPanel extends JPanel {

    private final Map<String, RoundedPanel> navRows = new LinkedHashMap<>();
    private String selected = "Dashboard";

    public SidebarPanel(Consumer<String> onSelect) {
        setLayout(new BorderLayout());
        setBackground(Theme.BG_PANEL);
        setPreferredSize(new Dimension(220, 0));
        setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Theme.BORDER));

        JPanel nav = new JPanel();
        nav.setOpaque(false);
        nav.setLayout(new BoxLayout(nav, BoxLayout.Y_AXIS));
        nav.setBorder(BorderFactory.createEmptyBorder(16, 12, 16, 12));

        String[][] items = {
                {"Dashboard", "DASHBOARD"},
                {"Relief Camps", "CAMP"},
                {"Donors", "DONOR"},
                {"Supply Inventory", "INVENTORY"},
                {"Dispatch", "DISPATCH"},
                {"Drivers", "DRIVER"},
                {"Alerts", "ALERT"},
                {"Reports", "REPORT"},
                {"Settings", "SETTINGS"},
        };

        for (String[] item : items) {
            String label = item[0];
            AppIcon.Kind kind = AppIcon.Kind.valueOf(item[1]);
            RoundedPanel row = buildRow(label, kind, onSelect);
            navRows.put(label, row);
            nav.add(row);
            nav.add(Box.createVerticalStrut(4));
        }

        add(nav, BorderLayout.NORTH);
        applySelectionStyle();

        // footer status
        RoundedPanel footer = new RoundedPanel(Theme.BG_CARD, Theme.BORDER, 10);
        footer.setLayout(new FlowLayout(FlowLayout.LEFT, 8, 8));
        footer.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        JPanel dotWrap = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Theme.GREEN);
                g2.fillOval(0, 0, 9, 9);
                g2.dispose();
            }
        };
        dotWrap.setOpaque(false);
        dotWrap.setPreferredSize(new Dimension(9, 9));
        footer.add(dotWrap);
        JLabel statusText = new JLabel("SYSTEM STATUS");
        statusText.setFont(Theme.FONT_SMALL);
        statusText.setForeground(Theme.TEXT_MUTED);
        JPanel statusCol = new JPanel();
        statusCol.setOpaque(false);
        statusCol.setLayout(new BoxLayout(statusCol, BoxLayout.Y_AXIS));
        JLabel line1 = new JLabel("System Status");
        line1.setFont(Theme.FONT_SMALL);
        line1.setForeground(Theme.TEXT_MUTED);
        JLabel line2 = new JLabel("All Systems Operational");
        line2.setFont(Theme.FONT_SMALL);
        line2.setForeground(Theme.GREEN);
        statusCol.add(line1);
        statusCol.add(line2);
        footer.add(statusCol);

        JPanel footerWrap = new JPanel(new BorderLayout());
        footerWrap.setOpaque(false);
        footerWrap.setBorder(BorderFactory.createEmptyBorder(8, 12, 16, 12));
        footerWrap.add(footer, BorderLayout.CENTER);
        add(footerWrap, BorderLayout.SOUTH);
    }

    private RoundedPanel buildRow(String label, AppIcon.Kind kind, Consumer<String> onSelect) {
        RoundedPanel row = new RoundedPanel(Theme.BG_PANEL, null, 8);
        row.setLayout(new BorderLayout());
        row.setBorder(BorderFactory.createEmptyBorder(9, 12, 9, 12));
        row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel iconLabel = new JLabel(new AppIcon(kind, 18, Theme.TEXT_MUTED));
        JLabel text = new JLabel(label);
        text.setFont(Theme.FONT_BASE);
        text.setForeground(Theme.TEXT_MUTED);
        text.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));

        row.add(iconLabel, BorderLayout.WEST);
        row.add(text, BorderLayout.CENTER);

        row.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                selected = label;
                applySelectionStyle();
                onSelect.accept(label);
            }
        });
        return row;
    }

    private void applySelectionStyle() {
        for (Map.Entry<String, RoundedPanel> entry : navRows.entrySet()) {
            RoundedPanel row = entry.getValue();
            boolean isSelected = entry.getKey().equals(selected);
            row.setBg(isSelected ? Theme.ACCENT_BLUE : Theme.BG_PANEL);
            JLabel icon = (JLabel) row.getComponent(0);
            JLabel text = (JLabel) row.getComponent(1);
            Color fg = isSelected ? Color.WHITE : Theme.TEXT_MUTED;
            text.setForeground(fg);
            AppIcon original = (AppIcon) icon.getIcon();
            icon.setIcon(new AppIcon(iconKindOf(original), 18, fg));
        }
    }

    private AppIcon.Kind iconKindOf(AppIcon icon) {
        // Small helper since AppIcon doesn't expose its kind publicly.
        return icon.kind();
    }
}
