package resqbridge;

import javax.swing.*;
import java.awt.*;

public class DriversPanel extends JPanel implements DataListener {

    private final DataStore store;
    private final Frame ownerFrame;
    private final JPanel listHolder = new JPanel();

    public DriversPanel(DataStore store, Frame ownerFrame) {
        this.store = store;
        this.ownerFrame = ownerFrame;
        store.addListener(this);
        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel title = new JLabel("Drivers");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);
        header.setBorder(BorderFactory.createEmptyBorder(0, 4, 12, 0));

        JButton add = DispatchCardPanel.smallButton("+ Add Driver");
        add.addActionListener(e -> new AddDriverDialog(ownerFrame, store).setVisible(true));
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

    private void rebuild() {
        listHolder.removeAll();
        if (store.drivers.isEmpty()) {
            JLabel empty = new JLabel("No drivers on file. Add one to get started.");
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setFont(Theme.FONT_BASE);
            listHolder.add(empty);
        }
        for (Driver d : new java.util.ArrayList<>(store.drivers)) {
            listHolder.add(row(d));
            listHolder.add(Box.createVerticalStrut(10));
        }
        listHolder.revalidate();
        listHolder.repaint();
    }

    private JComponent row(Driver d) {
        RoundedPanel card = new RoundedPanel(Theme.BG_CARD, Theme.BORDER, 14);
        card.setLayout(new BorderLayout(12, 0));
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 74));

        JLabel icon = new JLabel(new AppIcon(AppIcon.Kind.DRIVER, 26, d.onDuty ? Theme.ACCENT_CYAN : Theme.TEXT_FAINT));
        card.add(icon, BorderLayout.WEST);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        JLabel name = new JLabel(d.name);
        name.setFont(Theme.FONT_BOLD);
        name.setForeground(Theme.TEXT_PRIMARY);
        JLabel vehicle = new JLabel(d.vehicle);
        vehicle.setFont(Theme.FONT_SMALL);
        vehicle.setForeground(Theme.TEXT_MUTED);

        String statusText;
        Color statusColor;
        if (!d.onDuty) {
            statusText = "Off duty";
            statusColor = Theme.TEXT_FAINT;
        } else if (d.destination.isEmpty()) {
            statusText = "On duty \u00B7 no active route";
            statusColor = Theme.TEXT_MUTED;
        } else if (d.reached) {
            statusText = "Reached " + d.destination;
            statusColor = Theme.GREEN;
        } else {
            statusText = "En route to " + d.destination;
            statusColor = Theme.ORANGE;
        }
        JLabel status = new JLabel(statusText);
        status.setFont(Theme.FONT_SMALL);
        status.setForeground(statusColor);

        col.add(name);
        col.add(vehicle);
        col.add(status);
        card.add(col, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.X_AXIS));

        if (d.onDuty && !d.destination.isEmpty()) {
            JButton reachedBtn = DispatchCardPanel.smallButton(d.reached ? "Mark En Route" : "Mark Reached");
            reachedBtn.addActionListener(e -> {
                d.reached = !d.reached;
                store.addAudit((d.reached ? "Marked " : "Un-marked ") + d.name + " as reached " + d.destination + ".");
            });
            right.add(reachedBtn);
            right.add(Box.createHorizontalStrut(6));
        }

        JToggleButton dutyToggle = new JToggleButton(d.onDuty ? "On Duty" : "Off Duty", d.onDuty);
        dutyToggle.setFocusPainted(false);
        dutyToggle.setForeground(Color.WHITE);
        dutyToggle.setBackground(d.onDuty ? Theme.GREEN : Theme.TEXT_FAINT);
        dutyToggle.addActionListener(e -> {
            d.onDuty = dutyToggle.isSelected();
            if (!d.onDuty) d.reached = false;
            store.addAudit(d.name + " set to " + (d.onDuty ? "on duty" : "off duty") + ".");
        });
        right.add(dutyToggle);
        right.add(Box.createHorizontalStrut(6));

        JButton remove = DispatchCardPanel.smallButton("Remove");
        remove.setForeground(Theme.RED);
        remove.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Remove " + d.name + " from the driver roster?", "Remove driver",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                store.drivers.remove(d);
                store.addAudit("Removed driver " + d.name + " from the roster.");
            }
        });
        right.add(remove);

        card.add(right, BorderLayout.EAST);
        return card;
    }

    @Override
    public void onDataChanged() { rebuild(); }
}

class AddDriverDialog extends JDialog {
    AddDriverDialog(Frame owner, DataStore store) {
        super(owner, "Add driver", true);
        getContentPane().setBackground(Theme.BG_PANEL);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        setLayout(new GridLayout(0, 1, 0, 10));
        setSize(360, 360);
        setLocationRelativeTo(owner);
        setResizable(false);

        JTextField nameField = field();
        JTextField vehicleField = field();
        JTextField destinationField = field();
        JCheckBox onDutyBox = new JCheckBox("Start on duty", true);
        onDutyBox.setOpaque(false);
        onDutyBox.setForeground(Theme.TEXT_MUTED);
        onDutyBox.setFont(Theme.FONT_BASE);

        add(labeled("Driver name", nameField));
        add(labeled("Vehicle (e.g. Truck - KL07 A 1234)", vehicleField));
        add(labeled("Destination (optional)", destinationField));
        add(onDutyBox);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Add driver");
        save.setBackground(Theme.ACCENT_BLUE);
        save.setForeground(Color.WHITE);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            String name = nameField.getText().trim();
            String vehicle = vehicleField.getText().trim();
            if (name.isEmpty() || vehicle.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter both a name and a vehicle.", "Missing info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String destination = destinationField.getText().trim();
            store.drivers.add(new Driver(name, vehicle, onDutyBox.isSelected(), destination, false));
            store.addAudit("Added new driver " + name + " to the roster.");
            dispose();
        });
        buttons.add(cancel);
        buttons.add(save);
        add(buttons);
    }

    private JTextField field() {
        JTextField f = new JTextField();
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
