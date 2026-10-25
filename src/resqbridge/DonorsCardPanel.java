package resqbridge;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Locale;

public class DonorsCardPanel extends RoundedPanel implements DataListener {

    private final DataStore store;
    private final Frame ownerFrame;
    private final JPanel listHolder = new JPanel();
    private final int maxRows;
    private static final NumberFormat INR = NumberFormat.getNumberInstance(new Locale("en", "IN"));

    public DonorsCardPanel(DataStore store, Frame ownerFrame) { this(store, ownerFrame, 4); }

    public DonorsCardPanel(DataStore store, Frame ownerFrame, int maxRows) {
        super(Theme.BG_CARD, Theme.BORDER, 16);
        this.store = store;
        this.ownerFrame = ownerFrame;
        this.maxRows = maxRows;
        store.addListener(this);
        setLayout(new BorderLayout(0, 10));
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Top Donors");
        title.setFont(Theme.FONT_CARD_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JButton add = DispatchCardPanel.smallButton("+ Add");
        add.addActionListener(e -> new AddDonorDialog(ownerFrame, store).setVisible(true));
        header.add(add, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

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
        store.donors.sort(Comparator.comparingLong((Donor d) -> d.amount).reversed());
        int shown = 0;
        for (Donor d : store.donors) {
            if (maxRows > 0 && shown >= maxRows) break;
            listHolder.add(row(d));
            listHolder.add(Box.createVerticalStrut(8));
            shown++;
        }
        listHolder.revalidate();
        listHolder.repaint();
    }

    private JComponent row(Donor d) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        JLabel name = new JLabel(d.name);
        name.setFont(Theme.FONT_BASE);
        name.setForeground(Theme.TEXT_PRIMARY);
        JLabel amount = new JLabel("\u20B9" + INR.format(d.amount));
        amount.setFont(Theme.FONT_BOLD);
        amount.setForeground(Theme.GREEN);

        JButton delete = new JButton("\u2715");
        delete.setToolTipText("Remove donor");
        delete.setFont(Theme.FONT_SMALL);
        delete.setForeground(Theme.RED);
        delete.setBackground(Theme.BG_CARD_ALT);
        delete.setFocusPainted(false);
        delete.setMargin(new Insets(0, 6, 0, 6));
        delete.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        delete.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        delete.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove " + d.name + " from the donor list?", "Remove donor",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                store.donors.remove(d);
                store.addAudit("Removed donor " + d.name + " from the donor list.");
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(amount);
        right.add(delete);

        row.add(name, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    @Override
    public void onDataChanged() { rebuild(); }
}

class AddDonorDialog extends JDialog {
    AddDonorDialog(Frame owner, DataStore store) {
        super(owner, "Add donor", true);
        getContentPane().setBackground(Theme.BG_PANEL);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        setLayout(new GridLayout(0, 1, 0, 10));
        setSize(340, 240);
        setLocationRelativeTo(owner);
        setResizable(false);

        JTextField nameField = new JTextField();
        JTextField amountField = new JTextField();
        for (JTextField f : new JTextField[]{nameField, amountField}) {
            f.setBackground(Theme.BG_CARD_ALT);
            f.setForeground(Theme.TEXT_PRIMARY);
            f.setCaretColor(Theme.TEXT_PRIMARY);
            f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        }

        add(labeled("Donor name", nameField));
        add(labeled("Amount (\u20B9)", amountField));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Add donor");
        save.setBackground(Theme.ACCENT_BLUE);
        save.setForeground(Color.WHITE);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a donor name.", "Missing info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            long amount;
            try {
                amount = Long.parseLong(amountField.getText().trim().replaceAll("[,\\s]", ""));
            } catch (NumberFormatException ex) {
                amount = 0;
            }
            store.donors.add(new Donor(name, amount));
            store.addAudit("Added donor " + name + " (\u20B9" + amount + ").");
            dispose();
        });
        buttons.add(cancel);
        buttons.add(save);
        add(buttons);
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
