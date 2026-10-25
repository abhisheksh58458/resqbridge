package resqbridge;

import javax.swing.*;
import java.awt.*;

public class AlertsPanel extends JPanel implements DataListener {

    private final DataStore store;
    private final JPanel listHolder = new JPanel();

    public AlertsPanel(DataStore store) {
        this.store = store;
        store.addListener(this);
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Alerts");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 4, 12, 0));
        add(title, BorderLayout.NORTH);

        listHolder.setOpaque(false);
        listHolder.setLayout(new BoxLayout(listHolder, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listHolder);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        rebuild();
    }

    private void rebuild() {
        listHolder.removeAll();
        if (store.alerts.isEmpty()) {
            JLabel empty = new JLabel("No active alerts. All camps are within normal range.");
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setFont(Theme.FONT_BASE);
            listHolder.add(empty);
        }
        for (AlertItem a : new java.util.ArrayList<>(store.alerts)) {
            listHolder.add(row(a));
            listHolder.add(Box.createVerticalStrut(10));
        }
        listHolder.revalidate();
        listHolder.repaint();
    }

    private JComponent row(AlertItem a) {
        boolean critical = "Critical".equalsIgnoreCase(a.severity);
        Color color = critical ? Theme.RED : Theme.ORANGE;

        RoundedPanel card = new RoundedPanel(Theme.BG_CARD, color, 14);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        JLabel icon = new JLabel(new AppIcon(AppIcon.Kind.ALERT, 26, color));
        card.add(icon, BorderLayout.WEST);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        JLabel title = new JLabel(a.title);
        title.setFont(Theme.FONT_BOLD);
        title.setForeground(Theme.TEXT_PRIMARY);
        JLabel detail = new JLabel(a.detail);
        detail.setFont(Theme.FONT_SMALL);
        detail.setForeground(Theme.TEXT_MUTED);
        JLabel sev = new JLabel(a.severity);
        sev.setFont(Theme.FONT_SMALL);
        sev.setForeground(color);
        col.add(title);
        col.add(detail);
        col.add(sev);
        card.add(col, BorderLayout.CENTER);

        JButton resolve = DispatchCardPanel.smallButton("Resolve");
        resolve.addActionListener(e -> {
            store.alerts.remove(a);
            store.addAudit("Resolved alert: " + a.title + ".");
        });
        card.add(resolve, BorderLayout.EAST);
        return card;
    }

    @Override
    public void onDataChanged() { rebuild(); }
}
