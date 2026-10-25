package resqbridge;

import javax.swing.*;
import java.awt.*;

/** A JPanel with a rounded, filled background and optional border. */
class RoundedPanel extends JPanel {
    private Color bg;
    private Color border;
    private int radius;

    RoundedPanel(Color bg, Color border, int radius) {
        this.bg = bg;
        this.border = border;
        this.radius = radius;
        setOpaque(false);
    }

    void setBg(Color bg) { this.bg = bg; repaint(); }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        if (border != null) {
            g2.setColor(border);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
        }
        g2.dispose();
        super.paintComponent(g);
    }
}

/** Small hand-drawn pictograms so the app has no external image dependency. */
final class AppIcon implements Icon {
    enum Kind { TRUCK, TENT, PERSON, ALERT, DASHBOARD, CAMP, DONOR, INVENTORY, DISPATCH, DRIVER, REPORT, SETTINGS, BELL }

    private final Kind kind;
    private final int size;
    private final Color color;

    AppIcon(Kind kind, int size, Color color) {
        this.kind = kind;
        this.size = size;
        this.color = color;
    }

    Kind kind() { return kind; }

    @Override
    public int getIconWidth() { return size; }

    @Override
    public int getIconHeight() { return size; }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.translate(x, y);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(Math.max(1.5f, size / 12f)));
        int s = size;
        switch (kind) {
            case TRUCK: {
                g2.drawRoundRect(s / 12, s / 3, s / 2, s / 3, 3, 3);
                g2.drawRoundRect(s / 2, s / 2, s / 3, s / 5, 3, 3);
                g2.fillOval(s / 6, s * 2 / 3, s / 6, s / 6);
                g2.fillOval(s * 3 / 5, s * 2 / 3, s / 6, s / 6);
                break;
            }
            case TENT: {
                Polygon p = new Polygon();
                p.addPoint(s / 2, s / 8);
                p.addPoint(s * 15 / 16, s * 7 / 8);
                p.addPoint(s / 16, s * 7 / 8);
                g2.drawPolygon(p);
                g2.drawLine(s / 2, s / 8, s / 2, s * 7 / 8);
                break;
            }
            case PERSON: {
                g2.drawOval(s * 3 / 8, s / 8, s / 4, s / 4);
                g2.drawArc(s / 6, s / 2, s * 2 / 3, s / 2, 0, 180);
                break;
            }
            case ALERT: {
                Polygon p = new Polygon();
                p.addPoint(s / 2, s / 10);
                p.addPoint(s * 9 / 10, s * 9 / 10);
                p.addPoint(s / 10, s * 9 / 10);
                g2.drawPolygon(p);
                g2.drawLine(s / 2, s * 2 / 5, s / 2, s * 3 / 5);
                g2.fillOval(s * 15 / 32, s * 7 / 10, s / 16, s / 16);
                break;
            }
            case BELL: {
                g2.drawArc(s / 4, s / 6, s / 2, s / 2, 0, 180);
                g2.drawLine(s / 4, s * 5 / 12, s * 3 / 4, s * 5 / 12);
                g2.drawLine(s / 4, s * 5 / 12, s / 3, s * 3 / 4);
                g2.drawLine(s * 3 / 4, s * 5 / 12, s * 2 / 3, s * 3 / 4);
                break;
            }
            case CAMP: {
                g2.fillOval(0, 0, s, s);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.BOLD, size * 6 / 10));
                g2.drawString("A", s * 3 / 10, s * 3 / 4);
                break;
            }
            case DONOR: {
                int half = s / 2;
                Polygon heart = new Polygon();
                g2.drawOval(s / 8, s / 8, half - s / 16, half - s / 16);
                g2.drawOval(s / 2 - s / 16, s / 8, half - s / 16, half - s / 16);
                g2.drawLine(s / 8, s * 3 / 8, s * 7 / 8, s * 3 / 8);
                g2.drawLine(s / 2, s * 3 / 8, s / 2, s * 7 / 8);
                g2.drawLine(s / 5, s * 3 / 8, s / 2, s * 7 / 8);
                g2.drawLine(s * 4 / 5, s * 3 / 8, s / 2, s * 7 / 8);
                break;
            }
            case INVENTORY: {
                g2.drawRect(s / 6, s / 4, s * 2 / 3, s / 2);
                g2.drawLine(s / 6, s / 2, s * 5 / 6, s / 2);
                g2.drawLine(s / 2, s / 4, s / 2, s * 3 / 4);
                break;
            }
            case DISPATCH: {
                g2.drawLine(s / 6, s / 2, s * 5 / 6, s / 2);
                g2.drawLine(s * 2 / 3, s / 4, s * 5 / 6, s / 2);
                g2.drawLine(s * 2 / 3, s * 3 / 4, s * 5 / 6, s / 2);
                break;
            }
            case DRIVER: {
                g2.drawOval(s * 3 / 8, s / 8, s / 4, s / 4);
                g2.drawRoundRect(s / 6, s / 2, s * 2 / 3, s * 2 / 5, 4, 4);
                break;
            }
            case REPORT: {
                g2.drawRoundRect(s / 5, s / 8, s * 3 / 5, s * 3 / 4, 3, 3);
                g2.drawLine(s / 3, s / 3, s * 2 / 3, s / 3);
                g2.drawLine(s / 3, s / 2, s * 2 / 3, s / 2);
                g2.drawLine(s / 3, s * 2 / 3, s / 2, s * 2 / 3);
                break;
            }
            case SETTINGS: {
                g2.drawOval(s / 3, s / 3, s / 3, s / 3);
                for (int i = 0; i < 8; i++) {
                    double ang = Math.toRadians(i * 45);
                    int x1 = (int) (s / 2 + Math.cos(ang) * s * 0.3);
                    int y1 = (int) (s / 2 + Math.sin(ang) * s * 0.3);
                    int x2 = (int) (s / 2 + Math.cos(ang) * s * 0.42);
                    int y2 = (int) (s / 2 + Math.sin(ang) * s * 0.42);
                    g2.drawLine(x1, y1, x2, y2);
                }
                break;
            }
            case DASHBOARD:
            default: {
                g2.drawRect(s / 6, s / 6, s / 3, s / 3);
                g2.drawRect(s / 2, s / 6, s / 3, s / 5);
                g2.drawRect(s / 6, s / 2, s / 3, s / 5);
                g2.drawRect(s / 2, s * 3 / 8, s / 3, s * 3 / 8);
                break;
            }
        }
        g2.dispose();
    }
}
