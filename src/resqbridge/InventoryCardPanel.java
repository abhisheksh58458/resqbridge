package resqbridge;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

public class InventoryCardPanel extends RoundedPanel implements DataListener {

    private final DataStore store;
    private final DonutCanvas donut = new DonutCanvas();
    private final JPanel legendHolder = new JPanel();

    private static final Map<String, Color> CATEGORY_COLORS = new LinkedHashMap<>();
    static {
        CATEGORY_COLORS.put("Medical", Theme.ACCENT_BLUE);
        CATEGORY_COLORS.put("Food", Theme.ORANGE);
        CATEGORY_COLORS.put("Water", Theme.ACCENT_CYAN);
        CATEGORY_COLORS.put("Shelter", Theme.GREEN);
    }

    public InventoryCardPanel(DataStore store) {
        super(Theme.BG_CARD, Theme.BORDER, 16);
        this.store = store;
        store.addListener(this);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel title = new JLabel("Supply Inventory");
        title.setFont(Theme.FONT_CARD_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(14, 0));
        body.setOpaque(false);

        donut.setPreferredSize(new Dimension(120, 120));
        JPanel donutWrap = new JPanel(new GridBagLayout());
        donutWrap.setOpaque(false);
        donutWrap.add(donut);
        body.add(donutWrap, BorderLayout.WEST);

        legendHolder.setOpaque(false);
        legendHolder.setLayout(new BoxLayout(legendHolder, BoxLayout.Y_AXIS));
        body.add(legendHolder, BorderLayout.CENTER);

        add(body, BorderLayout.CENTER);
        rebuild();
    }

    private void rebuild() {
        legendHolder.removeAll();
        for (Map.Entry<String, Integer> e : store.inventory.entrySet()) {
            legendHolder.add(legendRow(e.getKey(), e.getValue()));
            legendHolder.add(Box.createVerticalStrut(6));
        }
        legendHolder.revalidate();
        legendHolder.repaint();
        donut.repaint();
    }

    private JComponent legendRow(String category, int value) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);
        JLabel dot = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CATEGORY_COLORS.getOrDefault(category, Theme.TEXT_MUTED));
                g2.fillOval(0, 2, 9, 9);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(12, 12));
        JLabel name = new JLabel(category);
        name.setFont(Theme.FONT_SMALL);
        name.setForeground(Theme.TEXT_MUTED);
        left.add(dot);
        left.add(name);
        row.add(left, BorderLayout.WEST);

        JSpinner spinner = new JSpinner(new SpinnerNumberModel(value, 0, 100, 1));
        spinner.setPreferredSize(new Dimension(58, 22));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setBackground(Theme.BG_CARD_ALT);
            ((JSpinner.DefaultEditor) editor).getTextField().setForeground(Theme.TEXT_PRIMARY);
        }
        spinner.addChangeListener(e -> {
            store.inventory.put(category, (Integer) spinner.getValue());
            store.fireChanged();
        });
        row.add(spinner, BorderLayout.EAST);
        return row;
    }

    @Override
    public void onDataChanged() { rebuild(); }

    private class DonutCanvas extends JPanel {
        DonutCanvas() { setOpaque(false); }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int size = Math.min(getWidth(), getHeight());
            int x = (getWidth() - size) / 2;
            int y = (getHeight() - size) / 2;
            int thickness = Math.max(10, size / 8);

            int total = 0;
            for (int v : store.inventory.values()) total += v;
            if (total == 0) total = 1;

            float start = 90f;
            g2.setStroke(new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
            for (Map.Entry<String, Integer> e : store.inventory.entrySet()) {
                float extent = 360f * e.getValue() / total;
                g2.setColor(CATEGORY_COLORS.getOrDefault(e.getKey(), Theme.TEXT_MUTED));
                g2.drawArc(x + thickness / 2, y + thickness / 2, size - thickness, size - thickness,
                        Math.round(start), -Math.round(extent));
                start -= extent;
            }

            String pct = store.overallStockPercent() + "%";
            g2.setFont(Theme.FONT_HUGE_NUM);
            g2.setColor(Theme.TEXT_PRIMARY);
            FontMetrics fm = g2.getFontMetrics();
            int tw = fm.stringWidth(pct);
            g2.drawString(pct, x + size / 2 - tw / 2, y + size / 2 + fm.getAscent() / 3);

            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_MUTED);
            String label = "Overall Stock";
            int lw = g2.getFontMetrics().stringWidth(label);
            g2.drawString(label, x + size / 2 - lw / 2, y + size / 2 + fm.getAscent() / 3 + 16);

            g2.dispose();
        }
    }
}
