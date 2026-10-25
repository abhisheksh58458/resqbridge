package resqbridge;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

public class MapPanel extends RoundedPanel implements DataListener {

    private final DataStore store;
    private final java.awt.Frame ownerFrame;
    private final List<Rectangle> hitBoxes = new ArrayList<>();

    public MapPanel(DataStore store, java.awt.Frame ownerFrame) {
        super(Theme.BG_CARD, Theme.BORDER, 16);
        this.store = store;
        this.ownerFrame = ownerFrame;
        store.addListener(this);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JPanel titleCol = new JPanel();
        titleCol.setOpaque(false);
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Kerala Relief Network");
        title.setFont(Theme.FONT_CARD_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        JLabel sub = new JLabel("Real-time Distribution Map");
        sub.setFont(Theme.FONT_SMALL);
        sub.setForeground(Theme.TEXT_MUTED);
        titleCol.add(title);
        titleCol.add(sub);
        header.add(titleCol, BorderLayout.WEST);

        JLabel live = new JLabel("\u25CF LIVE");
        live.setFont(Theme.FONT_SMALL);
        live.setForeground(Theme.GREEN);
        header.add(live, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        MapCanvas canvas = new MapCanvas();
        canvas.setOpaque(false);
        canvas.setPreferredSize(new Dimension(10, 10));
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                for (int i = 0; i < hitBoxes.size(); i++) {
                    if (hitBoxes.get(i).contains(e.getPoint())) {
                        new EditCampDialog(ownerFrame, store, store.camps.get(i)).setVisible(true);
                        return;
                    }
                }
            }
        });
        canvas.setToolTipText("Click a marker to edit that camp's supply level");
        add(canvas, BorderLayout.CENTER);
    }

    @Override
    public void onDataChanged() { repaint(); }

    private class MapCanvas extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            // Stylized coastline / state outline (decorative, not geographically precise).
            GeneralPath coast = new GeneralPath();
            coast.moveTo(w * 0.30, h * 0.02);
            coast.curveTo(w * 0.42, h * 0.18, w * 0.30, h * 0.28, w * 0.40, h * 0.40);
            coast.curveTo(w * 0.52, h * 0.52, w * 0.36, h * 0.58, w * 0.46, h * 0.70);
            coast.curveTo(w * 0.56, h * 0.80, w * 0.40, h * 0.86, w * 0.50, h * 0.98);
            GeneralPath coast2 = new GeneralPath();
            coast2.moveTo(w * 0.18, h * 0.05);
            coast2.curveTo(w * 0.30, h * 0.20, w * 0.16, h * 0.30, w * 0.26, h * 0.42);
            coast2.curveTo(w * 0.38, h * 0.54, w * 0.22, h * 0.62, w * 0.32, h * 0.72);
            coast2.curveTo(w * 0.42, h * 0.82, w * 0.26, h * 0.90, w * 0.36, h * 1.00);

            g2.setColor(new Color(0x16233A));
            g2.setStroke(new BasicStroke(5f));
            g2.draw(coast2);
            g2.setColor(new Color(0x1B2C48));
            g2.setStroke(new BasicStroke(2f));
            g2.draw(coast);
            g2.draw(coast2);

            List<Camp> camps = store.camps;
            hitBoxes.clear();

            // Route lines connecting camps in sequence (alternating active/in-transit look).
            for (int i = 0; i < camps.size() - 1; i++) {
                Camp a = camps.get(i);
                Camp b = camps.get(i + 1);
                int x1 = (int) (a.xFrac * w), y1 = (int) (a.yFrac * h);
                int x2 = (int) (b.xFrac * w), y2 = (int) (b.yFrac * h);
                boolean inTransit = (i % 2 == 1);
                g2.setColor(inTransit ? Theme.ORANGE : Theme.ACCENT_BLUE);
                if (inTransit) {
                    g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{6, 6}, 0));
                } else {
                    g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                }
                g2.drawLine(x1, y1, x2, y2);
            }

            // Markers + labels.
            g2.setFont(Theme.FONT_SMALL);
            FontMetrics fm = g2.getFontMetrics();
            for (Camp c : camps) {
                int cx = (int) (c.xFrac * w);
                int cy = (int) (c.yFrac * h);
                int r = c.hub ? 12 : 9;

                Color markerColor = !c.operational ? Theme.TEXT_FAINT
                        : c.supplyPercent < 45 ? Theme.RED
                        : c.supplyPercent < 65 ? Theme.ORANGE
                        : Theme.ACCENT_CYAN;

                g2.setColor(new Color(markerColor.getRed(), markerColor.getGreen(), markerColor.getBlue(), 70));
                g2.fillOval(cx - r - 5, cy - r - 5, (r + 5) * 2, (r + 5) * 2);
                g2.setColor(markerColor);
                g2.fillOval(cx - r, cy - r, r * 2, r * 2);
                g2.setColor(Theme.BG_CARD);
                g2.setFont(new Font("SansSerif", Font.BOLD, r));
                g2.drawString("A", cx - r / 3, cy + r / 3);

                hitBoxes.add(new Rectangle(cx - r - 5, cy - r - 5, (r + 5) * 2, (r + 5) * 2));

                String line1 = c.name;
                String line2 = "Supplies: " + c.supplyPercent + "%";
                int textW = Math.max(fm.stringWidth(line1), fm.stringWidth(line2)) + 16;
                int textH = 34;
                int bx = cx + r + 8;
                int by = cy - textH / 2;
                if (bx + textW > w) bx = cx - r - 8 - textW;
                g2.setColor(new Color(0x0A1220));
                g2.fillRoundRect(bx, by, textW, textH, 8, 8);
                g2.setColor(Theme.BORDER_LIGHT);
                g2.drawRoundRect(bx, by, textW, textH, 8, 8);
                g2.setColor(Theme.TEXT_PRIMARY);
                g2.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
                g2.drawString(line1, bx + 8, by + 14);
                g2.setFont(Theme.FONT_SMALL);
                g2.setColor(Theme.TEXT_MUTED);
                g2.drawString(line2, bx + 8, by + 27);
            }

            // Legend, bottom-left.
            int lx = 12, ly = h - 90;
            g2.setColor(new Color(0x0A1220));
            g2.fillRoundRect(lx - 8, ly - 14, 168, 96, 10, 10);
            legendRow(g2, lx, ly, Theme.ACCENT_BLUE, "Active Route", false);
            legendRow(g2, lx, ly + 18, Theme.ORANGE, "In Transit", true);
            legendRow(g2, lx, ly + 40, Theme.ACCENT_CYAN, "Relief Camp", false);
            legendRow(g2, lx, ly + 58, Theme.PURPLE, "Distribution Hub", false);

            g2.dispose();
        }

        private void legendRow(Graphics2D g2, int x, int y, Color color, String label, boolean dashed) {
            g2.setColor(color);
            if (dashed) {
                g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
                g2.drawLine(x, y + 5, x + 18, y + 5);
            } else if (label.contains("Route")) {
                g2.setStroke(new BasicStroke(2.6f));
                g2.drawLine(x, y + 5, x + 18, y + 5);
            } else {
                g2.fillOval(x + 4, y, 10, 10);
            }
            g2.setFont(Theme.FONT_SMALL);
            g2.setColor(Theme.TEXT_MUTED);
            g2.drawString(label, x + 26, y + 10);
        }
    }
}
