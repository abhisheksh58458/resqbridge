package resqbridge;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DriverConsolePanel extends JPanel implements DataListener {

    private final DataStore store;
    private final LoginAccount account;
    private final Frame ownerFrame;

    private JPanel shiftHolder;
    private JPanel deliveryHolder;
    private JPanel hazardHolder;

    public DriverConsolePanel(DataStore store, LoginAccount account, Frame ownerFrame) {
        this.store = store;
        this.account = account;
        this.ownerFrame = ownerFrame;
        store.addListener(this);

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Driver Console \u2014 " + account.displayName);
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 4, 12, 0));
        add(title, BorderLayout.NORTH);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        shiftHolder = new JPanel();
        shiftHolder.setOpaque(false);
        shiftHolder.setLayout(new BorderLayout());
        col.add(sectionCard("My Shift & Vehicle", "Fuel & maintenance alerts, and a shift timer to prevent fatigue.", shiftHolder));
        col.add(Box.createVerticalStrut(14));

        deliveryHolder = new JPanel();
        deliveryHolder.setOpaque(false);
        deliveryHolder.setLayout(new BoxLayout(deliveryHolder, BoxLayout.Y_AXIS));
        col.add(sectionCard("Cargo & Delivery Manifest", "Digital manifest, status updates, and proof of delivery.", deliveryHolder));
        col.add(Box.createVerticalStrut(14));

        MapPanel map = new MapPanel(store, ownerFrame);
        map.setPreferredSize(new Dimension(10, 320));
        col.add(sectionCard("Offline-Ready Navigation", "Full network map with routes, available even with weak signal.", map));
        col.add(Box.createVerticalStrut(14));

        hazardHolder = new JPanel();
        hazardHolder.setOpaque(false);
        hazardHolder.setLayout(new BoxLayout(hazardHolder, BoxLayout.Y_AXIS));
        JPanel hazardWrap = new JPanel(new BorderLayout(0, 10));
        hazardWrap.setOpaque(false);
        JButton reportHazard = DispatchCardPanel.smallButton("\u26A0 Report Hazard");
        reportHazard.addActionListener(e -> new HazardReportDialog(ownerFrame, store, account.displayName).setVisible(true));
        JPanel hazardBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        hazardBtnRow.setOpaque(false);
        hazardBtnRow.add(reportHazard);
        hazardWrap.add(hazardBtnRow, BorderLayout.NORTH);
        hazardWrap.add(hazardHolder, BorderLayout.CENTER);
        col.add(sectionCard("Live Hazard Reporting", "Alert dispatch to blocked roads, flooding, landslides or danger zones. Routes reroute around active hazards.", hazardWrap));
        col.add(Box.createVerticalStrut(14));

        JPanel sos = new JPanel(new BorderLayout(0, 10));
        sos.setOpaque(false);
        JButton sosBtn = new JButton("\uD83D\uDEA8 SOS \u2014 Emergency");
        sosBtn.setBackground(Theme.RED);
        sosBtn.setForeground(Color.WHITE);
        sosBtn.setFont(Theme.FONT_BOLD);
        sosBtn.setFocusPainted(false);
        sosBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "This will broadcast your exact location and an emergency alert to dispatch. Continue?",
                    "Confirm SOS", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm == JOptionPane.YES_OPTION) {
                Driver d = store.findDriver(account.linkedDriverName);
                String where = (d != null && !d.destination.isEmpty()) ? "en route to " + d.destination : "on route";
                store.alerts.add(0, new AlertItem("SOS from " + account.displayName,
                        account.displayName + " triggered an emergency SOS while " + where + ".", "Critical"));
                store.addAudit("SOS triggered by " + account.displayName + ".");
            }
        });
        JButton checkIn = DispatchCardPanel.smallButton("Send Check-in Ping");
        checkIn.addActionListener(e -> {
            store.chatMessages.add(new ChatMessage("driver:" + account.linkedDriverName, account.displayName, "Driver",
                    "\u2713 Check-in ping \u2014 all normal.", DataStore.nowTime()));
            store.addAudit(account.displayName + " sent a check-in ping.");
        });
        JPanel emergencyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        emergencyRow.setOpaque(false);
        emergencyRow.add(sosBtn);
        emergencyRow.add(checkIn);
        sos.add(emergencyRow, BorderLayout.NORTH);

        ChatPanel chat = new ChatPanel(store, "driver:" + account.linkedDriverName, account.displayName, "Driver",
                "Coordinator Chat");
        sos.add(chat, BorderLayout.CENTER);
        col.add(sectionCard("Communication & Emergency Support", "One-touch SOS, automated check-ins, and direct dispatch chat.", sos));
        col.add(Box.createVerticalStrut(14));

        JPanel safeZones = new JPanel();
        safeZones.setOpaque(false);
        safeZones.setLayout(new BoxLayout(safeZones, BoxLayout.Y_AXIS));
        for (SafeZone z : store.safeZones) {
            JLabel l = new JLabel(z.name + "  \u00B7  " + z.type + "  \u00B7  " + z.distanceKm + " km ahead");
            l.setFont(Theme.FONT_BASE);
            l.setForeground(Theme.TEXT_PRIMARY);
            l.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
            safeZones.add(l);
        }
        col.add(sectionCard("Safe Zone Locator", "Authorized rest stops, fuel stations and secure parking along your route.", safeZones));

        JScrollPane scroll = new JScrollPane(col);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        rebuildShift();
        rebuildDelivery();
        rebuildHazards();
    }

    private void rebuildShift() {
        shiftHolder.removeAll();
        Driver d = store.findDriver(account.linkedDriverName);
        if (d == null) {
            shiftHolder.add(new JLabel("No linked driver record found."), BorderLayout.CENTER);
            shiftHolder.revalidate();
            shiftHolder.repaint();
            return;
        }

        JPanel top = new JPanel(new GridLayout(1, 3, 16, 0));
        top.setOpaque(false);

        JPanel shiftCol = new JPanel();
        shiftCol.setOpaque(false);
        shiftCol.setLayout(new BoxLayout(shiftCol, BoxLayout.Y_AXIS));
        double hours = d.hoursOnShift();
        JLabel shiftLabel = new JLabel(d.onDuty ? String.format("On duty \u00B7 %.1f h this shift", hours) : "Off duty");
        shiftLabel.setFont(Theme.FONT_BOLD);
        shiftLabel.setForeground(hours >= 8 ? Theme.RED : Theme.TEXT_PRIMARY);
        JButton shiftToggle = DispatchCardPanel.smallButton(d.onDuty ? "End Shift" : "Start Shift");
        shiftToggle.addActionListener(e -> {
            if (d.onDuty) {
                d.onDuty = false;
                d.shiftStart = null;
            } else {
                d.onDuty = true;
                d.shiftStart = java.time.LocalDateTime.now();
            }
            store.addAudit(account.displayName + (d.onDuty ? " started their shift." : " ended their shift."));
        });
        shiftCol.add(shiftLabel);
        if (hours >= 8) {
            JLabel warn = new JLabel("\u26A0 Fatigue risk \u2014 consider a rest stop.");
            warn.setFont(Theme.FONT_SMALL);
            warn.setForeground(Theme.RED);
            shiftCol.add(warn);
        }
        shiftCol.add(Box.createVerticalStrut(6));
        shiftCol.add(shiftToggle);

        JPanel fuelCol = new JPanel();
        fuelCol.setOpaque(false);
        fuelCol.setLayout(new BoxLayout(fuelCol, BoxLayout.Y_AXIS));
        JLabel fuelLabel = new JLabel("Fuel: " + d.fuelPercent + "%");
        fuelLabel.setFont(Theme.FONT_BOLD);
        fuelLabel.setForeground(d.fuelPercent < 20 ? Theme.RED : Theme.TEXT_PRIMARY);
        JSlider fuelSlider = new JSlider(0, 100, d.fuelPercent);
        fuelSlider.setOpaque(false);
        fuelSlider.addChangeListener(e -> {
            d.fuelPercent = fuelSlider.getValue();
            fuelLabel.setText("Fuel: " + d.fuelPercent + "%");
            fuelLabel.setForeground(d.fuelPercent < 20 ? Theme.RED : Theme.TEXT_PRIMARY);
            if (!fuelSlider.getValueIsAdjusting()) store.fireChanged();
        });
        fuelCol.add(fuelLabel);
        fuelCol.add(fuelSlider);

        JPanel maintCol = new JPanel();
        maintCol.setOpaque(false);
        maintCol.setLayout(new BoxLayout(maintCol, BoxLayout.Y_AXIS));
        JLabel maintLabel = new JLabel("Maintenance issue");
        maintLabel.setFont(Theme.FONT_SMALL);
        maintLabel.setForeground(Theme.TEXT_MUTED);
        JTextField maintField = new JTextField(d.maintenanceIssue);
        maintField.setBackground(Theme.BG_CARD_ALT);
        maintField.setForeground(Theme.TEXT_PRIMARY);
        maintField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JButton logIssue = DispatchCardPanel.smallButton("Log issue");
        logIssue.addActionListener(e -> {
            d.maintenanceIssue = maintField.getText().trim();
            if (!d.maintenanceIssue.isEmpty()) {
                store.addAudit(account.displayName + " logged a maintenance issue: " + d.maintenanceIssue);
            } else {
                store.fireChanged();
            }
        });
        maintCol.add(maintLabel);
        maintCol.add(maintField);
        maintCol.add(Box.createVerticalStrut(6));
        maintCol.add(logIssue);

        top.add(shiftCol);
        top.add(fuelCol);
        top.add(maintCol);
        shiftHolder.add(top, BorderLayout.CENTER);
        shiftHolder.revalidate();
        shiftHolder.repaint();
    }

    private void rebuildDelivery() {
        deliveryHolder.removeAll();
        List<DeliveryRun> myRuns = store.deliveryRuns.stream()
                .filter(r -> r.driverName.equals(account.linkedDriverName) && !"Completed".equals(r.status))
                .collect(java.util.stream.Collectors.toList());

        if (myRuns.isEmpty()) {
            JLabel none = new JLabel("No active delivery assigned right now.");
            none.setForeground(Theme.TEXT_MUTED);
            deliveryHolder.add(none);
        }

        String[] statuses = {"Loading", "En Route", "Arrived at Gate", "Unloading", "Completed"};

        for (DeliveryRun run : myRuns) {
            RoundedPanel card = new RoundedPanel(Theme.BG_CARD_ALT, Theme.BORDER_LIGHT, 12);
            card.setLayout(new BorderLayout(0, 8));
            card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel route = new JLabel(run.fromLocation + "  \u2192  " + run.toCamp
                    + (run.hasCriticalOrPerishable() ? "   \u2022 PRIORITY" : ""));
            route.setFont(Theme.FONT_BOLD);
            route.setForeground(run.hasCriticalOrPerishable() ? Theme.ORANGE : Theme.TEXT_PRIMARY);
            JLabel eta = new JLabel("Status: " + run.status + "   \u00B7   ETA " + run.etaMinutes + " min");
            eta.setFont(Theme.FONT_SMALL);
            eta.setForeground(Theme.TEXT_MUTED);

            JPanel head = new JPanel();
            head.setOpaque(false);
            head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
            head.add(route);
            head.add(eta);
            card.add(head, BorderLayout.NORTH);

            JPanel itemsCol = new JPanel();
            itemsCol.setOpaque(false);
            itemsCol.setLayout(new BoxLayout(itemsCol, BoxLayout.Y_AXIS));
            for (ManifestItem mi : run.items) {
                Color pColor = "Critical".equals(mi.priority) ? Theme.RED
                        : "Perishable".equals(mi.priority) ? Theme.ORANGE : Theme.TEXT_MUTED;
                JLabel line = new JLabel("\u2022 " + mi.name + " \u00D7 " + mi.qty + "   [" + mi.priority + "]");
                line.setFont(Theme.FONT_SMALL);
                line.setForeground(pColor);
                itemsCol.add(line);
            }
            card.add(itemsCol, BorderLayout.CENTER);

            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            actions.setOpaque(false);

            int idx = java.util.Arrays.asList(statuses).indexOf(run.status);
            if (idx >= 0 && idx < statuses.length - 1) {
                String next = statuses[idx + 1];
                JButton advance = DispatchCardPanel.smallButton("Mark \"" + next + "\"");
                advance.addActionListener(e -> {
                    run.status = next;
                    if ("Arrived at Gate".equals(next) && run.checkInTime.isEmpty()) {
                        run.checkInTime = DataStore.nowTime();
                    }
                    store.addAudit(account.displayName + " marked delivery to " + run.toCamp + " as \"" + next + "\".");
                });
                actions.add(advance);
            }

            if ("Completed".equals(run.status) && !run.proofSigned) {
                JButton confirm = DispatchCardPanel.smallButton("Confirm Proof of Delivery");
                confirm.addActionListener(e -> {
                    run.proofSigned = true;
                    run.proofNote = "Signed by camp representative on arrival.";
                    int totalUnits = run.items.stream().mapToInt(mi -> mi.qty).sum();
                    store.dispatches.add(0, new Dispatch(
                            run.items.size() == 1 ? run.items.get(0).name : run.items.size() + " item types",
                            run.fromLocation, run.toCamp, "Just now", totalUnits));
                    store.totalSuppliesDispatched += totalUnits;
                    store.addAudit("Proof of delivery confirmed for " + run.toCamp + " (driver " + account.displayName + ").");
                });
                actions.add(confirm);
            }

            card.add(actions, BorderLayout.SOUTH);
            deliveryHolder.add(card);
            deliveryHolder.add(Box.createVerticalStrut(8));
        }
        deliveryHolder.revalidate();
        deliveryHolder.repaint();
    }

    private void rebuildHazards() {
        hazardHolder.removeAll();
        if (store.hazardReports.isEmpty()) {
            JLabel none = new JLabel("No hazards reported. Routes are clear.");
            none.setForeground(Theme.GREEN);
            none.setFont(Theme.FONT_SMALL);
            hazardHolder.add(none);
        } else {
            JLabel banner = new JLabel("\u26A0 " + store.hazardReports.size() + " active hazard report(s) \u2014 dynamic rerouting suggested.");
            banner.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            banner.setForeground(Theme.ORANGE);
            hazardHolder.add(banner);
            hazardHolder.add(Box.createVerticalStrut(6));
            for (HazardReport h : store.hazardReports) {
                JLabel l = new JLabel(h.time + "  \u00B7  " + h.type + " (" + h.severity + ") near " + h.location
                        + "  \u2014 reported by " + h.reporter);
                l.setFont(Theme.FONT_SMALL);
                l.setForeground(Theme.TEXT_MUTED);
                hazardHolder.add(l);
            }
        }
        hazardHolder.revalidate();
        hazardHolder.repaint();
    }

    private JComponent sectionCard(String title, String subtitle, JComponent body) {
        RoundedPanel card = new RoundedPanel(Theme.BG_CARD, Theme.BORDER, 14);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JLabel t = new JLabel(title);
        t.setFont(Theme.FONT_CARD_TITLE);
        t.setForeground(Theme.TEXT_PRIMARY);
        t.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel s = new JLabel(subtitle);
        s.setFont(Theme.FONT_SMALL);
        s.setForeground(Theme.TEXT_MUTED);
        s.setAlignmentX(Component.LEFT_ALIGNMENT);
        header.add(t);
        header.add(s);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    @Override
    public void onDataChanged() {
        rebuildShift();
        rebuildDelivery();
        rebuildHazards();
    }
}

class HazardReportDialog extends JDialog {
    HazardReportDialog(Frame owner, DataStore store, String reporterName) {
        super(owner, "Report a hazard", true);
        getContentPane().setBackground(Theme.BG_PANEL);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        setLayout(new GridLayout(0, 1, 0, 10));
        setSize(360, 320);
        setLocationRelativeTo(owner);
        setResizable(false);

        JTextField locationField = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Flooding", "Landslide", "Blocked Road", "Other"});
        JComboBox<String> severityBox = new JComboBox<>(new String[]{"High", "Medium", "Low"});
        for (JComponent c : new JComponent[]{locationField, typeBox, severityBox}) {
            c.setBackground(Theme.BG_CARD_ALT);
            c.setForeground(Theme.TEXT_PRIMARY);
        }
        locationField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        add(labeled("Location / road name", locationField));
        add(labeled("Hazard type", typeBox));
        add(labeled("Severity", severityBox));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Report hazard");
        save.setBackground(Theme.ACCENT_BLUE);
        save.setForeground(Color.WHITE);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            String loc = locationField.getText().trim();
            if (loc.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a location.", "Missing info", JOptionPane.WARNING_MESSAGE);
                return;
            }
            store.hazardReports.add(0, new HazardReport(reporterName, loc,
                    (String) typeBox.getSelectedItem(), (String) severityBox.getSelectedItem(), DataStore.nowTime()));
            store.addAudit(reporterName + " reported a hazard: " + typeBox.getSelectedItem() + " near " + loc + ".");
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
