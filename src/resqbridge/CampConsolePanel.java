package resqbridge;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

public class CampConsolePanel extends JPanel implements DataListener {

    private final DataStore store;
    private final LoginAccount account;
    private final Frame ownerFrame;
    private final String myCamp;

    private JPanel etaHolder;
    private JPanel equipmentHolder;
    private JPanel gateHolder;

    public CampConsolePanel(DataStore store, LoginAccount account, Frame ownerFrame) {
        this.store = store;
        this.account = account;
        this.ownerFrame = ownerFrame;
        this.myCamp = account.linkedCampName;
        store.addListener(this);

        setOpaque(false);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Camp Console \u2014 " + account.displayName + "  (" + myCamp + ")");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 4, 12, 0));
        add(title, BorderLayout.NORTH);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        // -------------------------------------------------- Inbound supply visibility
        etaHolder = new JPanel();
        etaHolder.setOpaque(false);
        etaHolder.setLayout(new BoxLayout(etaHolder, BoxLayout.Y_AXIS));

        JPanel etaWrap = new JPanel(new BorderLayout(0, 10));
        etaWrap.setOpaque(false);
        JButton tick = DispatchCardPanel.smallButton("\u23E9 Simulate 5 min passing");
        tick.addActionListener(e -> {
            for (DeliveryRun r : incomingRuns()) r.etaMinutes = Math.max(0, r.etaMinutes - 5);
            store.addAudit("Advanced inbound delivery clocks by 5 minutes for " + myCamp + ".");
        });
        JPanel tickRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tickRow.setOpaque(false);
        tickRow.add(tick);
        etaWrap.add(tickRow, BorderLayout.NORTH);
        etaWrap.add(etaHolder, BorderLayout.CENTER);

        col.add(sectionCard("Live ETA Dashboard & Pre-Arrival Manifests",
                "Real-time countdown, incoming inventory, and priority matching against current shortages.",
                etaWrap));
        col.add(Box.createVerticalStrut(14));

        // -------------------------------------------------------------- Gate & capacity
        gateHolder = new JPanel();
        gateHolder.setOpaque(false);
        gateHolder.setLayout(new BoxLayout(gateHolder, BoxLayout.Y_AXIS));
        col.add(sectionCard("Gate Capacity Tracker",
                "Prevents bottlenecks by holding incoming trucks when unloading zones are full.",
                gateHolder));
        col.add(Box.createVerticalStrut(14));

        // ------------------------------------------------ Resource & task management
        equipmentHolder = new JPanel();
        equipmentHolder.setOpaque(false);
        equipmentHolder.setLayout(new BoxLayout(equipmentHolder, BoxLayout.Y_AXIS));
        col.add(sectionCard("Equipment Allocation",
                "Forklifts, hand trucks and pallets available to unload heavy or specialized cargo.",
                equipmentHolder));
        col.add(Box.createVerticalStrut(14));

        // ------------------------------------------------------------- Dispute chat
        ChatPanel dispute = new ChatPanel(store, "camp:" + myCamp, account.displayName, "Camp Employee",
                "Instant Dispute Resolution \u2014 Dispatch / Warehouse Chat");
        col.add(dispute);

        JScrollPane scroll = new JScrollPane(col);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        rebuildEta();
        rebuildGate();
        rebuildEquipment();
    }

    private List<DeliveryRun> incomingRuns() {
        return store.deliveryRuns.stream()
                .filter(r -> myCamp.equals(r.toCamp) && !"Completed".equals(r.status))
                .collect(Collectors.toList());
    }

    /** The inventory category this camp is shortest on, used for priority matching. */
    private String shortestCategory() {
        String worst = null;
        int worstVal = Integer.MAX_VALUE;
        for (var e : store.inventory.entrySet()) {
            if (e.getValue() < worstVal) {
                worstVal = e.getValue();
                worst = e.getKey();
            }
        }
        return worst;
    }

    private String suggestStorage(DeliveryRun run) {
        boolean coldChain = run.items.stream().anyMatch(m -> "Perishable".equals(m.priority)
                || m.name.toLowerCase().contains("medical") || m.name.toLowerCase().contains("medicine")
                || m.name.toLowerCase().contains("vaccine"));
        boolean critical = run.items.stream().anyMatch(m -> "Critical".equals(m.priority));
        if (coldChain) return "Cold-Storage Unit A (medical / perishable)";
        if (critical) return "Priority Warehouse Bay 1";
        return "Central Warehouse \u2014 General Storage";
    }

    private void rebuildEta() {
        etaHolder.removeAll();
        List<DeliveryRun> runs = incomingRuns();
        String shortage = shortestCategory();

        if (runs.isEmpty()) {
            JLabel none = new JLabel("No inbound deliveries expected right now.");
            none.setForeground(Theme.TEXT_MUTED);
            etaHolder.add(none);
        }

        for (DeliveryRun run : runs) {
            boolean matchesShortage = shortage != null && run.items.stream()
                    .anyMatch(m -> m.name.toLowerCase().contains(shortage.toLowerCase()));

            RoundedPanel card = new RoundedPanel(Theme.BG_CARD_ALT, Theme.BORDER_LIGHT, 12);
            card.setLayout(new BorderLayout(0, 8));
            card.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
            card.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel head = new JPanel();
            head.setOpaque(false);
            head.setLayout(new BoxLayout(head, BoxLayout.Y_AXIS));
            JLabel route = new JLabel(run.driverName + "  \u00B7  " + run.fromLocation + " \u2192 " + myCamp
                    + (matchesShortage ? "   \u2605 MATCHES CAMP SHORTAGE" : ""));
            route.setFont(Theme.FONT_BOLD);
            route.setForeground(matchesShortage ? Theme.GREEN : Theme.TEXT_PRIMARY);
            JLabel eta = new JLabel("Status: " + run.status + "   \u00B7   ETA " + run.etaMinutes + " min"
                    + (run.hasCriticalOrPerishable() ? "   \u2022 PRIORITY CARGO" : ""));
            eta.setFont(Theme.FONT_SMALL);
            eta.setForeground(run.hasCriticalOrPerishable() ? Theme.ORANGE : Theme.TEXT_MUTED);
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

            card.add(receivingRow(run), BorderLayout.SOUTH);
            etaHolder.add(card);
            etaHolder.add(Box.createVerticalStrut(8));
        }
        etaHolder.revalidate();
        etaHolder.repaint();
    }

    /** Digital check-in/out, discrepancy reporting, storage allocation, and offload team assignment for one run. */
    private JComponent receivingRow(DeliveryRun run) {
        JPanel wrap = new JPanel();
        wrap.setOpaque(false);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);

        if (run.checkInTime.isEmpty()) {
            JButton checkIn = DispatchCardPanel.smallButton("Check In (gate)");
            checkIn.addActionListener(e -> {
                if (store.holdIncomingTrucks && store.gateCurrentCount() >= store.gateMaxCapacity) {
                    JOptionPane.showMessageDialog(this,
                            "Gate is holding incoming trucks \u2014 unloading zones are full.",
                            "Gate on hold", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                run.checkInTime = DataStore.nowTime();
                run.status = "Arrived at Gate";
                store.addAudit(account.displayName + " checked in " + run.driverName + " at the " + myCamp + " gate.");
            });
            actions.add(checkIn);
        } else if (run.checkOutTime.isEmpty()) {
            JLabel inAt = new JLabel("Checked in " + run.checkInTime);
            inAt.setFont(Theme.FONT_SMALL);
            inAt.setForeground(Theme.TEXT_MUTED);
            actions.add(inAt);

            JButton unloading = DispatchCardPanel.smallButton("Mark Unloading");
            unloading.setEnabled(!"Unloading".equals(run.status));
            unloading.addActionListener(e -> {
                run.status = "Unloading";
                store.addAudit(account.displayName + " marked " + run.driverName + "'s truck as unloading.");
            });
            actions.add(unloading);

            JButton checkOut = DispatchCardPanel.smallButton("Check Out (complete)");
            checkOut.addActionListener(e -> {
                run.checkOutTime = DataStore.nowTime();
                run.status = "Completed";
                if (run.offloadTeam.isEmpty()) run.offloadTeam = "Unassigned";
                if (!run.proofSigned) {
                    run.proofSigned = true;
                    run.proofNote = "Checked out by " + account.displayName + " at " + myCamp + ".";
                    int totalUnits = run.items.stream().mapToInt(mi -> mi.qty).sum();
                    store.dispatches.add(0, new Dispatch(
                            run.items.size() == 1 ? run.items.get(0).name : run.items.size() + " item types",
                            run.fromLocation, run.toCamp, "Just now", totalUnits));
                    store.totalSuppliesDispatched += totalUnits;
                }
                store.addAudit(account.displayName + " checked out " + run.driverName + " \u2014 delivery complete.");
            });
            actions.add(checkOut);

            JButton discrepancy = DispatchCardPanel.smallButton("Report Discrepancy");
            discrepancy.setForeground(Theme.ORANGE);
            discrepancy.addActionListener(e -> new DiscrepancyDialog(ownerFrame, store, run, account.displayName).setVisible(true));
            actions.add(discrepancy);
        } else {
            JLabel done = new JLabel("Completed \u00B7 in " + run.checkInTime + " / out " + run.checkOutTime);
            done.setFont(Theme.FONT_SMALL);
            done.setForeground(Theme.GREEN);
            actions.add(done);
        }
        wrap.add(actions);

        // Driver welfare flag
        Driver d = store.findDriver(run.driverName);
        if (d != null && d.hoursOnShift() >= 8) {
            JLabel welfare = new JLabel("\u26A0 " + d.name + " has driven " + String.format("%.1f", d.hoursOnShift())
                    + "h \u2014 offer food, water and a rest area.");
            welfare.setFont(Theme.FONT_SMALL);
            welfare.setForeground(Theme.RED);
            wrap.add(welfare);
        }

        // Storage allocation guide
        JPanel storageRow = new JPanel(new BorderLayout(8, 0));
        storageRow.setOpaque(false);
        storageRow.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        JTextField storageField = new JTextField(run.storageAllocation);
        storageField.setBackground(Theme.BG_CARD_ALT);
        storageField.setForeground(Theme.TEXT_PRIMARY);
        storageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        JButton suggest = DispatchCardPanel.smallButton("Suggest");
        suggest.addActionListener(e -> {
            run.storageAllocation = suggestStorage(run);
            storageField.setText(run.storageAllocation);
            store.addAudit("Suggested storage for " + run.driverName + "'s load: " + run.storageAllocation);
        });
        JButton saveStorage = DispatchCardPanel.smallButton("Save");
        saveStorage.addActionListener(e -> {
            run.storageAllocation = storageField.getText().trim();
            store.fireChanged();
        });
        JLabel storageLabel = new JLabel("Storage:");
        storageLabel.setFont(Theme.FONT_SMALL);
        storageLabel.setForeground(Theme.TEXT_MUTED);
        JPanel storageBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        storageBtns.setOpaque(false);
        storageBtns.add(suggest);
        storageBtns.add(saveStorage);
        storageRow.add(storageLabel, BorderLayout.WEST);
        storageRow.add(storageField, BorderLayout.CENTER);
        storageRow.add(storageBtns, BorderLayout.EAST);
        wrap.add(storageRow);

        // Offload team coordinator
        JPanel teamRow = new JPanel(new BorderLayout(8, 0));
        teamRow.setOpaque(false);
        teamRow.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
        JTextField teamField = new JTextField(run.offloadTeam);
        teamField.setBackground(Theme.BG_CARD_ALT);
        teamField.setForeground(Theme.TEXT_PRIMARY);
        teamField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(4, 6, 4, 6)));
        JButton assign = DispatchCardPanel.smallButton("Assign");
        assign.addActionListener(e -> {
            run.offloadTeam = teamField.getText().trim();
            store.addAudit(account.displayName + " assigned \"" + run.offloadTeam + "\" to unload " + run.driverName + "'s truck.");
        });
        JLabel teamLabel = new JLabel("Offload team:");
        teamLabel.setFont(Theme.FONT_SMALL);
        teamLabel.setForeground(Theme.TEXT_MUTED);
        teamRow.add(teamLabel, BorderLayout.WEST);
        teamRow.add(teamField, BorderLayout.CENTER);
        teamRow.add(assign, BorderLayout.EAST);
        wrap.add(teamRow);

        if (!run.discrepancies.isEmpty()) {
            JLabel discLabel = new JLabel("Discrepancies: " + String.join("; ", run.discrepancies));
            discLabel.setFont(Theme.FONT_SMALL);
            discLabel.setForeground(Theme.RED);
            wrap.add(discLabel);
        }

        return wrap;
    }

    private void rebuildGate() {
        gateHolder.removeAll();
        int current = store.gateCurrentCount();
        boolean full = current >= store.gateMaxCapacity;

        JLabel status = new JLabel("Unloading bays: " + current + " / " + store.gateMaxCapacity + " in use"
                + (full ? "  \u2014  AT CAPACITY" : ""));
        status.setFont(Theme.FONT_BOLD);
        status.setForeground(full ? Theme.RED : Theme.TEXT_PRIMARY);
        gateHolder.add(status);
        gateHolder.add(Box.createVerticalStrut(8));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        JToggleButton hold = new JToggleButton(store.holdIncomingTrucks ? "Holding Incoming Trucks" : "Accepting Incoming Trucks",
                store.holdIncomingTrucks);
        hold.setFocusPainted(false);
        hold.setForeground(Color.WHITE);
        hold.setBackground(store.holdIncomingTrucks ? Theme.RED : Theme.GREEN);
        hold.addActionListener(e -> {
            store.holdIncomingTrucks = hold.isSelected();
            store.addAudit(myCamp + " gate set to " + (store.holdIncomingTrucks ? "HOLD incoming trucks." : "accept incoming trucks."));
        });
        row.add(hold);

        JSpinner capacity = new JSpinner(new SpinnerNumberModel(store.gateMaxCapacity, 1, 20, 1));
        capacity.setPreferredSize(new Dimension(70, 24));
        JComponent editor = capacity.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setBackground(Theme.BG_CARD_ALT);
            ((JSpinner.DefaultEditor) editor).getTextField().setForeground(Theme.TEXT_PRIMARY);
        }
        JButton saveCap = DispatchCardPanel.smallButton("Set bay capacity");
        saveCap.addActionListener(e -> {
            store.gateMaxCapacity = (Integer) capacity.getValue();
            store.addAudit(account.displayName + " set " + myCamp + " unloading capacity to " + store.gateMaxCapacity + " bays.");
        });
        JLabel bayLabel = new JLabel("Bays:");
        bayLabel.setForeground(Theme.TEXT_MUTED);
        bayLabel.setFont(Theme.FONT_SMALL);
        row.add(bayLabel);
        row.add(capacity);
        row.add(saveCap);
        gateHolder.add(row);

        gateHolder.revalidate();
        gateHolder.repaint();
    }

    private void rebuildEquipment() {
        equipmentHolder.removeAll();
        for (EquipmentItem eq : store.equipment) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));

            JLabel name = new JLabel(eq.name);
            name.setFont(Theme.FONT_BASE);
            name.setForeground(Theme.TEXT_PRIMARY);
            row.add(name, BorderLayout.WEST);

            int available = eq.total - eq.inUse;
            JLabel countLabel = new JLabel(available + " available / " + eq.total + " total");
            countLabel.setFont(Theme.FONT_SMALL);
            countLabel.setForeground(available <= 0 ? Theme.RED : Theme.TEXT_MUTED);

            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            btns.setOpaque(false);
            JButton allocate = DispatchCardPanel.smallButton("Allocate 1");
            allocate.setEnabled(available > 0);
            allocate.addActionListener(e -> {
                eq.inUse = Math.min(eq.total, eq.inUse + 1);
                store.addAudit(account.displayName + " allocated 1 " + eq.name + " at " + myCamp + ".");
            });
            JButton release = DispatchCardPanel.smallButton("Release 1");
            release.setEnabled(eq.inUse > 0);
            release.addActionListener(e -> {
                eq.inUse = Math.max(0, eq.inUse - 1);
                store.fireChanged();
            });
            btns.add(countLabel);
            btns.add(allocate);
            btns.add(release);
            row.add(btns, BorderLayout.EAST);

            equipmentHolder.add(row);
        }
        equipmentHolder.revalidate();
        equipmentHolder.repaint();
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
        rebuildEta();
        rebuildGate();
        rebuildEquipment();
    }
}

class DiscrepancyDialog extends JDialog {
    DiscrepancyDialog(Frame owner, DataStore store, DeliveryRun run, String reporterName) {
        super(owner, "Report discrepancy \u2014 " + run.driverName, true);
        getContentPane().setBackground(Theme.BG_PANEL);
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        setLayout(new GridLayout(0, 1, 0, 10));
        setSize(360, 300);
        setLocationRelativeTo(owner);
        setResizable(false);

        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Damaged goods", "Missing items", "Unexpected extras", "Other"});
        typeBox.setBackground(Theme.BG_CARD_ALT);
        typeBox.setForeground(Theme.TEXT_PRIMARY);

        JTextField detailField = new JTextField();
        detailField.setBackground(Theme.BG_CARD_ALT);
        detailField.setForeground(Theme.TEXT_PRIMARY);
        detailField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));

        add(labeled("Issue type", typeBox));
        add(labeled("Details (optional)", detailField));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Log discrepancy");
        save.setBackground(Theme.ACCENT_BLUE);
        save.setForeground(Color.WHITE);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            String type = (String) typeBox.getSelectedItem();
            String detail = detailField.getText().trim();
            String entry = type + (detail.isEmpty() ? "" : " \u2014 " + detail);
            run.discrepancies.add(entry);
            store.addAudit(reporterName + " logged a discrepancy for " + run.driverName + "'s delivery: " + entry);
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
