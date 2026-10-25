package resqbridge;

import javax.swing.*;
import java.awt.*;

public class CampsPanel extends JPanel implements DataListener {

    private final DataStore store;
    private final JPanel listHolder = new JPanel();

    public CampsPanel(DataStore store) {
        this.store = store;
        store.addListener(this);
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Relief Camps");
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
        for (Camp c : store.camps) {
            listHolder.add(row(c));
            listHolder.add(Box.createVerticalStrut(10));
        }
        listHolder.revalidate();
        listHolder.repaint();
    }

    private JComponent row(Camp c) {
        RoundedPanel card = new RoundedPanel(Theme.BG_CARD, Theme.BORDER, 14);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));

        JLabel icon = new JLabel(new AppIcon(c.hub ? AppIcon.Kind.INVENTORY : AppIcon.Kind.TENT, 28,
                c.operational ? Theme.ACCENT_CYAN : Theme.TEXT_FAINT));
        card.add(icon, BorderLayout.WEST);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(c.name + (c.hub ? "  \u00B7 Distribution Hub" : ""));
        name.setFont(Theme.FONT_BOLD);
        name.setForeground(Theme.TEXT_PRIMARY);
        JLabel status = new JLabel(c.operational ? "Operational" : "Offline");
        status.setFont(Theme.FONT_SMALL);
        status.setForeground(c.operational ? Theme.GREEN : Theme.TEXT_FAINT);
        col.add(name);
        col.add(status);
        card.add(col, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.setOpaque(false);
        JLabel pct = new JLabel(c.supplyPercent + "%");
        pct.setFont(Theme.FONT_BIG_NUM);
        pct.setForeground(c.supplyPercent < 45 ? Theme.RED : c.supplyPercent < 65 ? Theme.ORANGE : Theme.ACCENT_CYAN);
        JSlider slider = new JSlider(0, 100, c.supplyPercent);
        slider.setOpaque(false);
        slider.setPreferredSize(new Dimension(140, 24));
        slider.addChangeListener(e -> {
            c.supplyPercent = slider.getValue();
            pct.setForeground(c.supplyPercent < 45 ? Theme.RED : c.supplyPercent < 65 ? Theme.ORANGE : Theme.ACCENT_CYAN);
            pct.setText(c.supplyPercent + "%");
            if (!slider.getValueIsAdjusting()) store.fireChanged();
        });
        JCheckBox opBox = new JCheckBox("Operational", c.operational);
        opBox.setOpaque(false);
        opBox.setForeground(Theme.TEXT_MUTED);
        opBox.setFont(Theme.FONT_SMALL);
        opBox.addActionListener(e -> {
            c.operational = opBox.isSelected();
            store.addAudit(c.name + " marked " + (c.operational ? "operational." : "offline."));
        });
        right.add(opBox);
        right.add(slider);
        right.add(pct);
        card.add(right, BorderLayout.EAST);

        return card;
    }

    @Override
    public void onDataChanged() { rebuild(); }
}
