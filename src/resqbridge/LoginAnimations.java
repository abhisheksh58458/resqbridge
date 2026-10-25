package resqbridge;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleConsumer;

/** Tiny tween helpers shared by the sign-in animations. */
final class Anim {
    private Anim() {}

    /** Runs step(0..1) every ~15 ms for the given duration, then done. */
    static Timer run(int ms, DoubleConsumer step, Runnable done) {
        final long start = System.nanoTime();
        final Timer t = new Timer(15, null);
        t.addActionListener(e -> {
            double p = Math.min(1.0, (System.nanoTime() - start) / (ms * 1_000_000.0));
            step.accept(p);
            if (p >= 1.0) {
                t.stop();
                if (done != null) done.run();
            }
        });
        t.start();
        return t;
    }

    static double easeOut(double p) {
        double q = 1 - p;
        return 1 - q * q * q;
    }

    static Color lerp(Color a, Color b, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        return new Color(
                a.getRed() + Math.round((b.getRed() - a.getRed()) * k),
                a.getGreen() + Math.round((b.getGreen() - a.getGreen()) * k),
                a.getBlue() + Math.round((b.getBlue() - a.getBlue()) * k));
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }
}

/** Full-window animated backdrop: drifting glows, flowing "relief route" lines and rising particles. */
class AnimatedBackground extends JPanel {

    private static final class Particle {
        float x, y, speed, phase, radius;
    }

    private final List<Particle> particles = new ArrayList<>();
    private final Random rnd = new Random();
    private float t = 0f;
    private final Timer timer;

    AnimatedBackground() {
        setOpaque(true);
        for (int i = 0; i < 34; i++) {
            Particle p = new Particle();
            p.x = rnd.nextFloat();
            p.y = rnd.nextFloat();
            p.speed = 0.0004f + rnd.nextFloat() * 0.0012f;
            p.phase = rnd.nextFloat() * 6.28f;
            p.radius = 2f + rnd.nextFloat() * 7f;
            particles.add(p);
        }
        timer = new Timer(33, e -> {
            t += 0.033f;
            for (Particle p : particles) {
                p.y -= p.speed;
                if (p.y < -0.05f) {
                    p.y = 1.05f;
                    p.x = rnd.nextFloat();
                }
            }
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();

        g2.setPaint(new GradientPaint(0, 0, new Color(0x070B16), 0, h, new Color(0x0F1B36)));
        g2.fillRect(0, 0, w, h);

        float big = Math.max(w, h);
        glow(g2, w * 0.18f + (float) Math.sin(t * 0.5) * w * 0.05f,
                h * 0.25f + (float) Math.cos(t * 0.4) * h * 0.05f, big * 0.45f, Theme.ACCENT_BLUE, 75);
        glow(g2, w * 0.85f + (float) Math.sin(t * 0.35 + 1) * w * 0.04f,
                h * 0.80f + (float) Math.cos(t * 0.3 + 2) * h * 0.05f, big * 0.42f, Theme.ACCENT_CYAN, 55);
        glow(g2, w * 0.62f + (float) Math.cos(t * 0.28) * w * 0.05f,
                h * 0.08f + (float) Math.sin(t * 0.45) * h * 0.04f, big * 0.35f, Theme.PURPLE, 45);

        // Flowing routes along the bottom: solid blue = active route, dashed orange = in transit.
        wave(g2, w, h * 0.80f, 18, 0.011, t * 1.2, Theme.ACCENT_BLUE, 95, false);
        wave(g2, w, h * 0.87f, 12, 0.017, -t * 1.6 + 2, Theme.ORANGE, 85, true);

        for (Particle p : particles) {
            float px = (p.x + (float) Math.sin(t * 0.8 + p.phase) * 0.015f) * w;
            float py = p.y * h;
            int a = (int) (35 + 55 * (0.5 + 0.5 * Math.sin(t + p.phase)));
            g2.setColor(Anim.alpha(Theme.ACCENT_CYAN, a));
            g2.fillOval((int) (px - p.radius), (int) (py - p.radius), (int) (p.radius * 2), (int) (p.radius * 2));
        }
        g2.dispose();
    }

    private void glow(Graphics2D g2, float cx, float cy, float r, Color c, int alpha) {
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), r,
                new float[]{0f, 1f},
                new Color[]{Anim.alpha(c, alpha), Anim.alpha(c, 0)}));
        g2.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
    }

    private void wave(Graphics2D g2, int w, float baseY, float amp, double freq, double phase,
                      Color c, int alpha, boolean dashed) {
        GeneralPath path = new GeneralPath();
        for (int x = 0; x <= w; x += 8) {
            float y = baseY + (float) (Math.sin(x * freq + phase) * amp);
            if (x == 0) path.moveTo(x, y);
            else path.lineTo(x, y);
        }
        g2.setColor(Anim.alpha(c, alpha));
        if (dashed) {
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f,
                    new float[]{8f, 10f}, (float) (-t * 30)));
        } else {
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        g2.draw(path);
    }
}

/** Logo with expanding radar-style rings and a gentle breathing icon. */
class PulsingLogo extends JComponent {
    private float phase = 0f;
    private final Timer timer;

    PulsingLogo() {
        Dimension d = new Dimension(110, 110);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        timer = new Timer(30, e -> {
            phase += 0.012f;
            if (phase > 1f) phase -= 1f;
            repaint();
        });
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int cx = getWidth() / 2;
        int cy = getHeight() / 2;

        for (int i = 0; i < 3; i++) {
            float p = (phase + i / 3f) % 1f;
            float radius = 30 + p * 24;
            g2.setColor(Anim.alpha(Theme.ACCENT_CYAN, (int) ((1f - p) * 110)));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval((int) (cx - radius), (int) (cy - radius), (int) (radius * 2), (int) (radius * 2));
        }

        g2.setPaint(new RadialGradientPaint(new Point2D.Float(cx, cy), 34f,
                new float[]{0f, 1f},
                new Color[]{Anim.alpha(Theme.ACCENT_CYAN, 70), Anim.alpha(Theme.ACCENT_CYAN, 0)}));
        g2.fillOval(cx - 34, cy - 34, 68, 68);

        double breathe = 1.0 + 0.06 * Math.sin(phase * Math.PI * 2);
        int size = (int) (44 * breathe);
        new AppIcon(AppIcon.Kind.TENT, size, Theme.ACCENT_CYAN).paintIcon(this, g2, cx - size / 2, cy - size / 2);
        g2.dispose();
    }
}

/** The card itself: fades in/out via setAlpha(). */
class AnimatedCard extends RoundedPanel {
    private float alpha = 0f;

    AnimatedCard() {
        super(Theme.BG_CARD, Theme.BORDER_LIGHT, 22);
    }

    void setAlpha(float a) {
        alpha = Math.max(0f, Math.min(1f, a));
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        if (alpha >= 0.999f) {
            super.paint(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.SrcOver.derive(alpha));
        super.paint(g2);
        g2.dispose();
    }
}

/** Centers a child and lets it be nudged by (dx, dy): used for the slide-in, shake and slide-out. */
class Stage extends JPanel {
    private final JComponent child;
    private int dx = 0;
    private int dy = 0;

    Stage(JComponent child) {
        super(null);
        this.child = child;
        setOpaque(false);
        add(child);
    }

    void setOffset(int x, int y) {
        dx = x;
        dy = y;
        doLayout();
        repaint();
    }

    @Override
    public void doLayout() {
        Dimension d = child.getPreferredSize();
        child.setBounds((getWidth() - d.width) / 2 + dx, (getHeight() - d.height) / 2 + dy, d.width, d.height);
    }
}

/** Gradient button with animated hover, a spinner while busy, and a green success state. */
class AnimatedButton extends JButton {
    private float hover = 0f;
    private float hoverTarget = 0f;
    private boolean busy = false;
    private boolean success = false;
    private float angle = 0f;
    private final Timer hoverTimer;
    private final Timer spinTimer;

    AnimatedButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setForeground(Color.WHITE);
        setFont(Theme.FONT_BOLD);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        hoverTimer = new Timer(15, e -> {
            hover += (hoverTarget - hover) * 0.25f;
            if (Math.abs(hoverTarget - hover) < 0.01f) {
                hover = hoverTarget;
                ((Timer) e.getSource()).stop();
            }
            repaint();
        });
        spinTimer = new Timer(25, e -> {
            angle = (angle + 14f) % 360f;
            repaint();
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoverTarget = 1f;
                hoverTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverTarget = 0f;
                hoverTimer.start();
            }
        });
    }

    void setBusy(boolean b) {
        busy = b;
        setEnabled(!b);
        if (b) spinTimer.start();
        else spinTimer.stop();
        repaint();
    }

    void setSuccess(boolean s) {
        success = s;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        int w = getWidth();
        int h = getHeight();
        int press = getModel().isPressed() ? 1 : 0;

        Color left;
        Color right;
        if (success) {
            left = new Color(0x16A34A);
            right = Theme.GREEN;
        } else {
            left = Anim.lerp(Theme.ACCENT_BLUE, Theme.ACCENT_CYAN, hover * 0.55f);
            right = Anim.lerp(new Color(0x1D4ED8), Theme.ACCENT_BLUE, hover);
        }
        g2.setPaint(new GradientPaint(0, 0, left, w, 0, right));
        g2.fillRoundRect(0, press, w, h - 1, 12, 12);

        if (hover > 0.01f && !success) {
            g2.setColor(Anim.alpha(Theme.ACCENT_CYAN, (int) (hover * 90)));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(1, 1 + press, w - 3, h - 4, 12, 12);
        }

        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String text = getText();
        int textW = fm.stringWidth(text);
        int spinnerW = busy ? 26 : 0;
        int startX = (w - (textW + spinnerW)) / 2;
        int baseline = (h - fm.getHeight()) / 2 + fm.getAscent() + press;

        if (busy) {
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawArc(startX, h / 2 - 8 + press, 16, 16, (int) -angle, 250);
        }
        g2.setColor(Color.WHITE);
        g2.drawString(text, startX + spinnerW, baseline);
        g2.dispose();
    }
}

/** Text-field border whose outline glows cyan while the field has focus. */
class GlowBorder extends AbstractBorder {
    private float level = 0f;
    private float target = 0f;
    private final Timer timer;

    private GlowBorder(JComponent owner) {
        timer = new Timer(15, e -> {
            level += (target - level) * 0.22f;
            if (Math.abs(target - level) < 0.01f) {
                level = target;
                ((Timer) e.getSource()).stop();
            }
            owner.repaint();
        });
    }

    /** Styles the field and wires the focus animation. */
    static void attach(JTextField field) {
        GlowBorder gb = new GlowBorder(field);
        field.setBorder(BorderFactory.createCompoundBorder(gb, BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                gb.target = 1f;
                gb.timer.start();
            }

            @Override
            public void focusLost(FocusEvent e) {
                gb.target = 0f;
                gb.timer.start();
            }
        });
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        if (level > 0.01f) {
            g2.setColor(Anim.alpha(Theme.ACCENT_CYAN, (int) (level * 60)));
            g2.setStroke(new BasicStroke(3f));
            g2.drawRoundRect(x, y, w - 1, h - 1, 10, 10);
        }
        g2.setColor(Anim.lerp(Theme.BORDER_LIGHT, Theme.ACCENT_CYAN, level));
        g2.setStroke(new BasicStroke(1.2f));
        g2.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 8, 8);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(3, 3, 3, 3);
    }
}

/** Pulsing status dot + message showing whether MySQL is reachable. Click to re-check. */
class DbStatus extends JPanel {
    enum State { CHECKING, OK, FAIL }

    private State state = State.CHECKING;
    private float phase = 0f;
    private final JLabel text = new JLabel();
    private final Timer timer;
    private final JComponent dot;

    DbStatus() {
        super(new BorderLayout(8, 0));
        setOpaque(false);
        Dimension d = new Dimension(312, 36);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);

        dot = new JComponent() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c = state == State.OK ? Theme.GREEN : state == State.FAIL ? Theme.RED : Theme.ORANGE;
                int cx = getWidth() / 2;
                int cy = 10;
                if (state != State.FAIL) {
                    float r = 4 + 6 * phase;
                    g2.setColor(Anim.alpha(c, (int) ((1f - phase) * 140)));
                    g2.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
                }
                g2.setColor(c);
                g2.fillOval(cx - 4, cy - 4, 8, 8);
                g2.dispose();
            }
        };
        dot.setPreferredSize(new Dimension(20, 20));
        add(dot, BorderLayout.WEST);

        text.setFont(Theme.FONT_SMALL);
        text.setForeground(Theme.TEXT_MUTED);
        text.setVerticalAlignment(SwingConstants.TOP);
        add(text, BorderLayout.CENTER);

        timer = new Timer(40, e -> {
            phase += 0.03f;
            if (phase > 1f) phase -= 1f;
            dot.repaint();
        });
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText("Click to re-check the database connection");
    }

    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    State state() {
        return state;
    }

    void set(State s, String message) {
        state = s;
        text.setText("<html><div style='width:268px;'>" + message.replace("&", "&amp;").replace("<", "&lt;") + "</div></html>");
        dot.repaint();
    }
}
