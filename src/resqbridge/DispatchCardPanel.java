package resqbridge;

import javax.swing.*;
import java.awt.*;

public class DispatchCardPanel extends RoundedPanel implements DataListener {

    private final DataStore store;
    private final Frame ownerFrame;
    private final JPanel listHolder = new JPanel();
    private final int maxRows;

    public DispatchCardPanel(DataStore store, Frame ownerFrame) { this(store, ownerFrame, 4); }

    public DispatchCardPanel(DataStore store, Frame ownerFrame, int maxRows) {
        super(Theme.BG_CARD, Theme.BORDER, 16);
        this.store = store;
        this.ownerFrame = ownerFrame;
        this.maxRows = maxRows;
        store.addListener(this);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Recent Dispatches");
        title.setFont(Theme.FONT_CARD_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JButton add = smallButton("+ Add");
        add.addActionListener(e -> new AddDispatchDialog(ownerFrame, store).setVisible(true));
        header.add(add, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        listHolder.setOpaque(false);
        listHolder.setLayout(new BoxLayout(listHolder, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(listHolder);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        add(scroll, BorderLayout.CENTER);

        rebuild();
    }

    static JButton smallButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.FONT_SMALL);
        b.setForeground(Theme.ACCENT_CYAN);
        b.setBackground(Theme.BG_CARD_ALT);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private void rebuild() {
        listHolder.removeAll();
        int shown = 0;
        for (Dispatch d : store.dispatches) {
            if (maxRows > 0 && shown >= maxRows) break;
            listHolder.add(row(d));
            listHolder.add(Box.createVerticalStrut(8));
            shown++;
        }
        listHolder.revalidate();
        listHolder.repaint();
    }

    private JComponent row(Dispatch d) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JLabel icon = new JLabel(new AppIcon(AppIcon.Kind.DISPATCH, 22, Theme.ACCENT_BLUE));
        icon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
        row.add(icon, BorderLayout.WEST);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        JLabel item = new JLabel(d.item);
        item.setFont(Theme.FONT_BOLD);
        item.setForeground(Theme.TEXT_PRIMARY);
        JLabel route = new JLabel(d.from + " \u2192 " + d.to);
        route.setFont(Theme.FONT_SMALL);
        route.setForeground(Theme.TEXT_MUTED);
        col.add(item);
        col.add(route);
        row.add(col, BorderLayout.CENTER);

        JLabel time = new JLabel(d.timeAgo);
        time.setFont(Theme.FONT_SMALL);
        time.setForeground(Theme.TEXT_FAINT);
        time.setVerticalAlignment(SwingConstants.TOP);
        row.add(time, BorderLayout.EAST);
        return row;
    }

    @Override
    public void onDataChanged() { rebuild(); }
}

class AddDispatchDialog extends JDialog {
    AddDispatchDialog(Frame owner, DataStore store) {
        super(owner, "Add dispatch", true);
        getContentPane().setBackground(Theme.BG_PANEL);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        setLayout(new GridLayout(0, 1, 0, 10));
        setSize(360, 320);
        setLocationRelativeTo(owner);
        setResizable(false);

        JTextField itemField = field("e.g. Medical Supplies");
        JTextField fromField = field("From camp / hub");
        JTextField toField = field("To camp / hub");
        JTextField unitsField = field("Units (number)");

        add(labeled("Item", itemField));
        add(labeled("From", fromField));
        add(labeled("To", toField));
        add(labeled("Units", unitsField));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Dispatch");
        save.setBackground(Theme.ACCENT_BLUE);
        save.setForeground(Color.WHITE);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            String item = itemField.getText().trim();
            String from = fromField.getText().trim();
            String to = toField.getText().trim();
            if (item.isEmpty() || from.isEmpty() || to.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in item, from and to.", "Missing info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            int units;
            try {
                units = unitsField.getText().trim().isEmpty() ? 1 : Integer.parseInt(unitsField.getText().trim());
            } catch (NumberFormatException ex) {
                units = 1;
            }
            store.dispatches.add(0, new Dispatch(item, from, to, "Just now", units));
            store.totalSuppliesDispatched += units;
            store.addAudit("Dispatched " + item + " from " + from + " to " + to + ".");
            dispose();
        });
        buttons.add(cancel);
        buttons.add(save);
        add(buttons);
    }

    private JTextField field(String placeholder) {
        JTextField f = new JTextField();
        f.setToolTipText(placeholder);
        f.setBackground(Theme.BG_CARD_ALT);
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setCaretColor(Theme.TEXT_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        return f;
    }

    private JPanel labeled(String label, JComponent field) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setForeground(Theme.TEXT_MUTED);
        l.setFont(Theme.FONT_SMALL);
        p.add(l, BorderLayout.NORTH);
        p.add(field, BorderLayout.CENTER);
        return p;
    }
}
