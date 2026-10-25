package resqbridge;

import javax.swing.*;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.util.List;
import java.util.Set;

public class SettingsPanel extends JPanel implements DataListener {

    private final DataStore store;
    private final Frame ownerFrame;

    private JPanel pendingHolder;
    private JPanel auditHolder;
    private JPanel categoryHolder;
    private JLabel weatherStatus, mappingStatus, seismicStatus;
    private JLabel maintenanceStatus;

    public SettingsPanel(DataStore store, Frame ownerFrame) {
        this.store = store;
        this.ownerFrame = ownerFrame;
        store.addListener(this);
        setOpaque(false);
        setLayout(new BorderLayout(0, 14));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel pageTitle = new JLabel("Settings");
        pageTitle.setFont(Theme.FONT_TITLE);
        pageTitle.setForeground(Theme.TEXT_PRIMARY);
        add(pageTitle, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setOpaque(false);
        content.add(buildProfileCard(), BorderLayout.NORTH);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setBackground(Theme.BG_PANEL);
        tabs.setForeground(Theme.TEXT_PRIMARY);
        tabs.addTab("Users & Roles", scrollable(buildUsersAndRolesTab()));
        tabs.addTab("Alerts & Communication", scrollable(buildCommsTab()));
        tabs.addTab("Integrations & Data", scrollable(buildIntegrationsTab()));
        tabs.addTab("Security & System", scrollable(buildSecurityTab()));
        content.add(tabs, BorderLayout.CENTER);

        add(content, BorderLayout.CENTER);
    }

    private JScrollPane scrollable(JComponent inner) {
        JScrollPane sp = new JScrollPane(inner);
        sp.setBorder(null);
        sp.getVerticalScrollBar().setUnitIncrement(14);
        sp.setBackground(Theme.BG_APP);
        sp.getViewport().setBackground(Theme.BG_APP);
        return sp;
    }

    // ---------------------------------------------------------------- profile

    private JComponent buildProfileCard() {
        RoundedPanel card = new RoundedPanel(Theme.BG_CARD, Theme.BORDER, 14);
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        JLabel avatar = new JLabel(new AppIcon(AppIcon.Kind.PERSON, 46, Theme.ACCENT_CYAN));
        card.add(avatar, BorderLayout.WEST);

        JPanel grid = new JPanel(new GridLayout(2, 2, 16, 8));
        grid.setOpaque(false);

        JTextField nameField = textField(store.adminProfile.name);
        JTextField emailField = textField(store.adminProfile.email);
        JTextField phoneField = textField(store.adminProfile.phone);
        JTextField roleField = textField(store.adminProfile.role);
        roleField.setEditable(false);
        roleField.setForeground(Theme.TEXT_MUTED);

        grid.add(labeled("Full name", nameField));
        grid.add(labeled("Email", emailField));
        grid.add(labeled("Phone", phoneField));
        grid.add(labeled("Role", roleField));
        card.add(grid, BorderLayout.CENTER);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JButton save = DispatchCardPanel.smallButton("Save profile");
        save.addActionListener(e -> {
            store.adminProfile.name = nameField.getText().trim();
            store.adminProfile.email = emailField.getText().trim();
            store.adminProfile.phone = phoneField.getText().trim();
            store.addAudit("Updated admin profile details.");
        });
        right.add(Box.createVerticalGlue());
        right.add(save);
        right.add(Box.createVerticalGlue());
        card.add(right, BorderLayout.EAST);

        return card;
    }

    // ---------------------------------------------------------- users/roles tab

    private JComponent buildUsersAndRolesTab() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        col.add(sectionCard("Role-Based Access Control",
                "Assign permissions for admins, field rescue teams, volunteers and citizens.",
                buildPermissionMatrix()));
        col.add(Box.createVerticalStrut(14));

        pendingHolder = new JPanel();
        pendingHolder.setOpaque(false);
        pendingHolder.setLayout(new BoxLayout(pendingHolder, BoxLayout.Y_AXIS));
        rebuildPending();
        col.add(sectionCard("Staff Verification",
                "Approve or reject new administrative and responder accounts.",
                pendingHolder));
        col.add(Box.createVerticalStrut(14));

        auditHolder = new JPanel();
        auditHolder.setOpaque(false);
        auditHolder.setLayout(new BoxLayout(auditHolder, BoxLayout.Y_AXIS));
        rebuildAudit();
        JScrollPane auditScroll = new JScrollPane(auditHolder);
        auditScroll.setBorder(null);
        auditScroll.setOpaque(false);
        auditScroll.getViewport().setOpaque(false);
        auditScroll.setPreferredSize(new Dimension(10, 220));
        col.add(sectionCard("Activity Audit Logs",
                "Every admin action, data change and alert broadcast, for post-incident review.",
                auditScroll));

        return col;
    }

    private JComponent buildPermissionMatrix() {
        List<String> roles = store.roleNames;
        List<String> perms = store.permissionNames;

        JPanel grid = new JPanel(new GridLayout(perms.size() + 1, roles.size() + 1, 10, 8));
        grid.setOpaque(false);

        grid.add(new JLabel(""));
        for (String role : roles) {
            JLabel l = new JLabel(role);
            l.setFont(Theme.FONT_SMALL.deriveFont(Font.BOLD));
            l.setForeground(Theme.TEXT_PRIMARY);
            grid.add(l);
        }

        for (String perm : perms) {
            JLabel l = new JLabel(perm);
            l.setFont(Theme.FONT_SMALL);
            l.setForeground(Theme.TEXT_MUTED);
            grid.add(l);
            for (String role : roles) {
                Set<String> granted = store.rolePermissions.get(role);
                JCheckBox cb = new JCheckBox("", granted.contains(perm));
                cb.setOpaque(false);
                cb.addActionListener(e -> {
                    if (cb.isSelected()) granted.add(perm); else granted.remove(perm);
                    store.addAudit((cb.isSelected() ? "Granted " : "Revoked ") + "\"" + perm + "\" for role " + role + ".");
                });
                grid.add(cb);
            }
        }
        return grid;
    }

    private void rebuildPending() {
        pendingHolder.removeAll();
        if (store.pendingAccounts.isEmpty()) {
            JLabel empty = new JLabel("No accounts awaiting verification.");
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setFont(Theme.FONT_BASE);
            pendingHolder.add(empty);
        }
        for (PendingAccount p : new java.util.ArrayList<>(store.pendingAccounts)) {
            JPanel row = new JPanel(new BorderLayout(10, 0));
            row.setOpaque(false);
            row.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

            JPanel textCol = new JPanel();
            textCol.setOpaque(false);
            textCol.setLayout(new BoxLayout(textCol, BoxLayout.Y_AXIS));
            JLabel name = new JLabel(p.name + "  \u00B7  requesting " + p.requestedRole);
            name.setFont(Theme.FONT_BOLD);
            name.setForeground(Theme.TEXT_PRIMARY);
            JLabel meta = new JLabel(p.email + "  \u00B7  " + p.dateRequested);
            meta.setFont(Theme.FONT_SMALL);
            meta.setForeground(Theme.TEXT_MUTED);
            textCol.add(name);
            textCol.add(meta);
            row.add(textCol, BorderLayout.CENTER);

            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            buttons.setOpaque(false);
            JButton approve = DispatchCardPanel.smallButton("Approve");
            approve.setForeground(Theme.GREEN);
            approve.addActionListener(e -> {
                store.pendingAccounts.remove(p);
                store.addAudit("Approved " + p.name + " as " + p.requestedRole + ".");
            });
            JButton reject = DispatchCardPanel.smallButton("Reject");
            reject.setForeground(Theme.RED);
            reject.addActionListener(e -> {
                store.pendingAccounts.remove(p);
                store.addAudit("Rejected account request from " + p.name + ".");
            });
            buttons.add(approve);
            buttons.add(reject);
            row.add(buttons, BorderLayout.EAST);

            pendingHolder.add(row);
            pendingHolder.add(Box.createVerticalStrut(6));
        }
        pendingHolder.revalidate();
        pendingHolder.repaint();
    }

    private void rebuildAudit() {
        auditHolder.removeAll();
        if (store.auditLog.isEmpty()) {
            JLabel empty = new JLabel("No activity recorded yet.");
            empty.setForeground(Theme.TEXT_MUTED);
            auditHolder.add(empty);
        }
        for (AuditLogEntry a : store.auditLog) {
            JLabel line = new JLabel(a.time + "  \u2014  " + a.description);
            line.setFont(Theme.FONT_SMALL);
            line.setForeground(Theme.TEXT_MUTED);
            line.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            auditHolder.add(line);
        }
        auditHolder.revalidate();
        auditHolder.repaint();
    }

    // ------------------------------------------------------------- comms tab

    private JComponent buildCommsTab() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JPanel channels = new JPanel(new GridLayout(2, 2, 12, 8));
        channels.setOpaque(false);
        JCheckBox push = channelBox("Push Notifications", store.commsSettings.pushEnabled, v -> store.commsSettings.pushEnabled = v);
        JCheckBox sms = channelBox("SMS", store.commsSettings.smsEnabled, v -> store.commsSettings.smsEnabled = v);
        JCheckBox email = channelBox("Email", store.commsSettings.emailEnabled, v -> store.commsSettings.emailEnabled = v);
        JCheckBox voice = channelBox("Emergency Voice Alerts", store.commsSettings.voiceEnabled, v -> store.commsSettings.voiceEnabled = v);
        channels.add(push);
        channels.add(sms);
        channels.add(email);
        channels.add(voice);
        col.add(sectionCard("Broadcast Channels",
                "Enable or disable push notifications, SMS, email and emergency voice alerts.",
                channels));
        col.add(Box.createVerticalStrut(14));

        JPanel cap = new JPanel(new GridLayout(0, 2, 12, 8));
        cap.setOpaque(false);
        JTextField senderId = textField(store.commsSettings.capSenderId);
        JComboBox<String> category = comboBox(new String[]{"Geo", "Met", "Safety", "Security", "Rescue", "Fire", "Health", "Env", "Transport", "Infra", "CBRNE", "Other"}, store.commsSettings.capCategory);
        JComboBox<String> urgency = comboBox(new String[]{"Immediate", "Expected", "Future", "Past", "Unknown"}, store.commsSettings.capUrgency);
        JComboBox<String> severity = comboBox(new String[]{"Extreme", "Severe", "Moderate", "Minor", "Unknown"}, store.commsSettings.capSeverity);
        JComboBox<String> certainty = comboBox(new String[]{"Observed", "Likely", "Possible", "Unlikely", "Unknown"}, store.commsSettings.capCertainty);
        cap.add(labeled("Sender ID", senderId));
        cap.add(labeled("Category", category));
        cap.add(labeled("Urgency", urgency));
        cap.add(labeled("Severity", severity));
        cap.add(labeled("Certainty", certainty));

        JButton saveCap = DispatchCardPanel.smallButton("Save CAP template");
        saveCap.addActionListener(e -> {
            store.commsSettings.capSenderId = senderId.getText().trim();
            store.commsSettings.capCategory = (String) category.getSelectedItem();
            store.commsSettings.capUrgency = (String) urgency.getSelectedItem();
            store.commsSettings.capSeverity = (String) severity.getSelectedItem();
            store.commsSettings.capCertainty = (String) certainty.getSelectedItem();
            store.addAudit("Updated Common Alerting Protocol (CAP) template.");
        });
        JPanel capWrap = new JPanel(new BorderLayout(0, 10));
        capWrap.setOpaque(false);
        capWrap.add(cap, BorderLayout.CENTER);
        JPanel capBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        capBtnRow.setOpaque(false);
        capBtnRow.add(saveCap);
        capWrap.add(capBtnRow, BorderLayout.SOUTH);

        col.add(sectionCard("Common Alerting Protocol (CAP)",
                "Standard message formatting for government and emergency networks.",
                capWrap));
        col.add(Box.createVerticalStrut(14));

        JPanel geo = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        geo.setOpaque(false);
        JSpinner radius = new JSpinner(new SpinnerNumberModel(store.commsSettings.geoFenceRadiusKm, 1, 500, 1));
        styleSpinner(radius);
        JButton saveGeo = DispatchCardPanel.smallButton("Save");
        saveGeo.addActionListener(e -> {
            store.commsSettings.geoFenceRadiusKm = (Integer) radius.getValue();
            store.addAudit("Set default geo-fence radius to " + store.commsSettings.geoFenceRadiusKm + " km.");
        });
        geo.add(new JLabel("Default radius (km):") {{ setForeground(Theme.TEXT_MUTED); setFont(Theme.FONT_SMALL); }});
        geo.add(radius);
        geo.add(saveGeo);
        col.add(sectionCard("Geo-Fencing Rules",
                "Default geographic radius limits for sending local hazard warnings.",
                geo));

        return col;
    }

    private JCheckBox channelBox(String label, boolean initial, java.util.function.Consumer<Boolean> onChange) {
        JCheckBox cb = new JCheckBox(label, initial);
        cb.setOpaque(false);
        cb.setForeground(Theme.TEXT_PRIMARY);
        cb.setFont(Theme.FONT_BASE);
        cb.addActionListener(e -> {
            onChange.accept(cb.isSelected());
            store.addAudit(label + (cb.isSelected() ? " enabled." : " disabled."));
        });
        return cb;
    }

    // ------------------------------------------------------ integrations tab

    private JComponent buildIntegrationsTab() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JPanel apiPanel = new JPanel();
        apiPanel.setOpaque(false);
        apiPanel.setLayout(new BoxLayout(apiPanel, BoxLayout.Y_AXIS));
        weatherStatus = new JLabel();
        mappingStatus = new JLabel();
        seismicStatus = new JLabel();
        apiPanel.add(apiKeyRow("Weather feed (NOAA)", store.integrationSettings.weatherApiKey,
                store.integrationSettings.weatherConnected, weatherStatus,
                key -> store.integrationSettings.weatherApiKey = key,
                connected -> store.integrationSettings.weatherConnected = connected));
        apiPanel.add(Box.createVerticalStrut(10));
        apiPanel.add(apiKeyRow("Mapping tools", store.integrationSettings.mappingApiKey,
                store.integrationSettings.mappingConnected, mappingStatus,
                key -> store.integrationSettings.mappingApiKey = key,
                connected -> store.integrationSettings.mappingConnected = connected));
        apiPanel.add(Box.createVerticalStrut(10));
        apiPanel.add(apiKeyRow("Seismic data (USGS)", store.integrationSettings.seismicApiKey,
                store.integrationSettings.seismicConnected, seismicStatus,
                key -> store.integrationSettings.seismicApiKey = key,
                connected -> store.integrationSettings.seismicConnected = connected));

        col.add(sectionCard("API Key Management",
                "Connect and manage live weather feeds, mapping tools and seismic data sources.",
                apiPanel));
        col.add(Box.createVerticalStrut(14));

        categoryHolder = new JPanel();
        categoryHolder.setOpaque(false);
        categoryHolder.setLayout(new BoxLayout(categoryHolder, BoxLayout.Y_AXIS));
        rebuildCategories();

        JTextField newCategory = textField("");
        JButton addCategory = DispatchCardPanel.smallButton("+ Add category");
        addCategory.addActionListener(e -> {
            String v = newCategory.getText().trim();
            if (!v.isEmpty()) {
                store.resourceCategories.add(v);
                newCategory.setText("");
                store.addAudit("Added resource inventory category \"" + v + "\".");
            }
        });
        JPanel addRow = new JPanel(new BorderLayout(8, 0));
        addRow.setOpaque(false);
        addRow.add(newCategory, BorderLayout.CENTER);
        addRow.add(addCategory, BorderLayout.EAST);

        JPanel resourceCol = new JPanel(new BorderLayout(0, 10));
        resourceCol.setOpaque(false);
        resourceCol.add(categoryHolder, BorderLayout.NORTH);
        resourceCol.add(addRow, BorderLayout.SOUTH);

        col.add(sectionCard("Resource Inventory Defaults",
                "Categories for tracking emergency vehicles, medical supplies and shelter capacities.",
                resourceCol));
        col.add(Box.createVerticalStrut(14));

        JPanel sync = new JPanel(new GridLayout(0, 2, 12, 10));
        sync.setOpaque(false);
        JSpinner bandwidth = new JSpinner(new SpinnerNumberModel(store.integrationSettings.bandwidthLimitMbps, 1, 1000, 1));
        JSpinner interval = new JSpinner(new SpinnerNumberModel(store.integrationSettings.syncIntervalMinutes, 1, 1440, 1));
        styleSpinner(bandwidth);
        styleSpinner(interval);
        JCheckBox offlineQueue = new JCheckBox("Queue updates while offline", store.integrationSettings.offlineQueueEnabled);
        offlineQueue.setOpaque(false);
        offlineQueue.setForeground(Theme.TEXT_PRIMARY);
        offlineQueue.setFont(Theme.FONT_BASE);
        sync.add(labeled("Bandwidth limit (Mbps)", bandwidth));
        sync.add(labeled("Sync interval (minutes)", interval));
        sync.add(offlineQueue);

        JButton saveSync = DispatchCardPanel.smallButton("Save sync settings");
        saveSync.addActionListener(e -> {
            store.integrationSettings.bandwidthLimitMbps = (Integer) bandwidth.getValue();
            store.integrationSettings.syncIntervalMinutes = (Integer) interval.getValue();
            store.integrationSettings.offlineQueueEnabled = offlineQueue.isSelected();
            store.addAudit("Updated offline sync settings (" + store.integrationSettings.bandwidthLimitMbps
                    + " Mbps, every " + store.integrationSettings.syncIntervalMinutes + " min).");
        });
        JPanel syncWrap = new JPanel(new BorderLayout(0, 10));
        syncWrap.setOpaque(false);
        syncWrap.add(sync, BorderLayout.CENTER);
        JPanel syncBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        syncBtnRow.setOpaque(false);
        syncBtnRow.add(saveSync);
        syncWrap.add(syncBtnRow, BorderLayout.SOUTH);

        col.add(sectionCard("Offline Sync Settings",
                "Data synchronization rules and bandwidth limits for low-connectivity field operations.",
                syncWrap));

        return col;
    }

    private JComponent apiKeyRow(String label, String initialKey, boolean connected, JLabel statusLabel,
                                  java.util.function.Consumer<String> onSave,
                                  java.util.function.Consumer<Boolean> onConnectChange) {
        JPanel row = new JPanel(new BorderLayout(10, 6));
        row.setOpaque(false);

        JLabel name = new JLabel(label);
        name.setFont(Theme.FONT_BOLD);
        name.setForeground(Theme.TEXT_PRIMARY);

        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setText(connected ? "Connected" : "Not connected");
        statusLabel.setForeground(connected ? Theme.GREEN : Theme.TEXT_FAINT);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(name, BorderLayout.WEST);
        top.add(statusLabel, BorderLayout.EAST);
        row.add(top, BorderLayout.NORTH);

        JPasswordField keyField = new JPasswordField(initialKey);
        keyField.setEchoChar('\u2022');
        styleTextComponent(keyField);

        JButton save = DispatchCardPanel.smallButton("Connect / Save");
        save.addActionListener(e -> {
            String key = new String(keyField.getPassword());
            onSave.accept(key);
            boolean nowConnected = !key.trim().isEmpty();
            onConnectChange.accept(nowConnected);
            statusLabel.setText(nowConnected ? "Connected" : "Not connected");
            statusLabel.setForeground(nowConnected ? Theme.GREEN : Theme.TEXT_FAINT);
            store.addAudit((nowConnected ? "Connected " : "Disconnected ") + label + " API integration.");
        });

        JPanel bottom = new JPanel(new BorderLayout(8, 0));
        bottom.setOpaque(false);
        bottom.add(keyField, BorderLayout.CENTER);
        bottom.add(save, BorderLayout.EAST);
        row.add(bottom, BorderLayout.CENTER);

        return row;
    }

    private void rebuildCategories() {
        categoryHolder.removeAll();
        for (String cat : new java.util.ArrayList<>(store.resourceCategories)) {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            JLabel l = new JLabel("\u2022  " + cat);
            l.setFont(Theme.FONT_BASE);
            l.setForeground(Theme.TEXT_PRIMARY);
            JButton remove = new JButton("\u2715");
            remove.setFont(Theme.FONT_SMALL);
            remove.setForeground(Theme.RED);
            remove.setBackground(Theme.BG_CARD_ALT);
            remove.setFocusPainted(false);
            remove.setMargin(new Insets(0, 6, 0, 6));
            remove.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
            remove.addActionListener(e -> {
                store.resourceCategories.remove(cat);
                store.addAudit("Removed resource inventory category \"" + cat + "\".");
            });
            row.add(l, BorderLayout.WEST);
            row.add(remove, BorderLayout.EAST);
            categoryHolder.add(row);
            categoryHolder.add(Box.createVerticalStrut(4));
        }
        categoryHolder.revalidate();
        categoryHolder.repaint();
    }

    // ----------------------------------------------------------- security tab

    private JComponent buildSecurityTab() {
        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JPanel mfaPanel = new JPanel(new GridLayout(0, 2, 12, 10));
        mfaPanel.setOpaque(false);
        JCheckBox mfaRequired = new JCheckBox("Require MFA for all elevated admin accounts", store.securitySettings.mfaRequired);
        mfaRequired.setOpaque(false);
        mfaRequired.setForeground(Theme.TEXT_PRIMARY);
        mfaRequired.setFont(Theme.FONT_BASE);
        JComboBox<String> mfaMethod = comboBox(new String[]{"Authenticator App", "SMS OTP", "Email OTP"}, store.securitySettings.mfaMethod);
        mfaPanel.add(mfaRequired);
        mfaPanel.add(labeled("MFA method", mfaMethod));

        JButton saveMfa = DispatchCardPanel.smallButton("Save");
        saveMfa.addActionListener(e -> {
            store.securitySettings.mfaRequired = mfaRequired.isSelected();
            store.securitySettings.mfaMethod = (String) mfaMethod.getSelectedItem();
            store.addAudit("Updated MFA policy (" + (store.securitySettings.mfaRequired ? "required, " : "optional, ")
                    + store.securitySettings.mfaMethod + ").");
        });
        JPanel mfaWrap = new JPanel(new BorderLayout(0, 10));
        mfaWrap.setOpaque(false);
        mfaWrap.add(mfaPanel, BorderLayout.CENTER);
        JPanel mfaBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        mfaBtnRow.setOpaque(false);
        mfaBtnRow.add(saveMfa);
        mfaWrap.add(mfaBtnRow, BorderLayout.SOUTH);

        col.add(sectionCard("Multi-Factor Authentication (MFA)",
                "Enforce mandatory 2FA/OTP for all elevated admin accounts.",
                mfaWrap));
        col.add(Box.createVerticalStrut(14));

        JPanel privacy = new JPanel(new GridLayout(0, 2, 12, 10));
        privacy.setOpaque(false);
        JSpinner retention = new JSpinner(new SpinnerNumberModel(store.securitySettings.dataRetentionDays, 1, 3650, 1));
        styleSpinner(retention);
        JComboBox<String> residency = comboBox(new String[]{
                "India (Mumbai region)", "India (Hyderabad region)", "EU (Frankfurt)", "US (N. Virginia)"
        }, store.securitySettings.dataResidency);
        privacy.add(labeled("Data retention (days)", retention));
        privacy.add(labeled("Data residency", residency));

        JButton savePrivacy = DispatchCardPanel.smallButton("Save");
        savePrivacy.addActionListener(e -> {
            store.securitySettings.dataRetentionDays = (Integer) retention.getValue();
            store.securitySettings.dataResidency = (String) residency.getSelectedItem();
            store.addAudit("Updated data privacy settings (" + store.securitySettings.dataRetentionDays
                    + " day retention, " + store.securitySettings.dataResidency + ").");
        });
        JPanel privacyWrap = new JPanel(new BorderLayout(0, 10));
        privacyWrap.setOpaque(false);
        privacyWrap.add(privacy, BorderLayout.CENTER);
        JPanel privacyBtnRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
        privacyBtnRow.setOpaque(false);
        privacyBtnRow.add(savePrivacy);
        privacyWrap.add(privacyBtnRow, BorderLayout.SOUTH);

        col.add(sectionCard("Data Privacy Settings",
                "Data retention periods and residency compliance for sensitive user reports.",
                privacyWrap));
        col.add(Box.createVerticalStrut(14));

        JPanel maintenance = new JPanel();
        maintenance.setOpaque(false);
        maintenance.setLayout(new BoxLayout(maintenance, BoxLayout.Y_AXIS));

        JPanel maintRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        maintRow.setOpaque(false);
        JToggleButton maintToggle = new JToggleButton(
                store.securitySettings.maintenanceMode ? "Maintenance Mode: ON" : "Maintenance Mode: OFF",
                store.securitySettings.maintenanceMode);
        maintToggle.setFocusPainted(false);
        maintToggle.setForeground(Color.WHITE);
        maintToggle.setBackground(store.securitySettings.maintenanceMode ? Theme.RED : Theme.GREEN);
        maintenanceStatus = new JLabel(store.securitySettings.maintenanceMode
                ? "The platform is in read-only maintenance mode."
                : "The platform is operating normally.");
        maintenanceStatus.setFont(Theme.FONT_SMALL);
        maintenanceStatus.setForeground(Theme.TEXT_MUTED);
        maintToggle.addActionListener(e -> {
            store.securitySettings.maintenanceMode = maintToggle.isSelected();
            maintToggle.setText(store.securitySettings.maintenanceMode ? "Maintenance Mode: ON" : "Maintenance Mode: OFF");
            maintToggle.setBackground(store.securitySettings.maintenanceMode ? Theme.RED : Theme.GREEN);
            maintenanceStatus.setText(store.securitySettings.maintenanceMode
                    ? "The platform is in read-only maintenance mode."
                    : "The platform is operating normally.");
            store.addAudit("System maintenance mode turned " + (store.securitySettings.maintenanceMode ? "ON." : "OFF."));
        });
        maintRow.add(maintToggle);
        maintRow.add(maintenanceStatus);
        maintenance.add(maintRow);
        maintenance.add(Box.createVerticalStrut(12));

        JLabel noticeLabel = new JLabel("Platform-wide safety notice");
        noticeLabel.setFont(Theme.FONT_SMALL);
        noticeLabel.setForeground(Theme.TEXT_MUTED);
        noticeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        JTextArea noticeArea = new JTextArea(store.securitySettings.platformNotice, 3, 40);
        noticeArea.setLineWrap(true);
        noticeArea.setWrapStyleWord(true);
        styleTextComponent(noticeArea);
        JScrollPane noticeScroll = new JScrollPane(noticeArea);
        noticeScroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER_LIGHT));
        noticeScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        JButton publishNotice = DispatchCardPanel.smallButton("Publish notice");
        publishNotice.setAlignmentX(Component.LEFT_ALIGNMENT);
        publishNotice.addActionListener(e -> {
            store.securitySettings.platformNotice = noticeArea.getText().trim();
            store.addAudit("Published a platform-wide safety notice.");
        });

        maintenance.add(noticeLabel);
        maintenance.add(Box.createVerticalStrut(4));
        maintenance.add(noticeScroll);
        maintenance.add(Box.createVerticalStrut(8));
        maintenance.add(publishNotice);

        col.add(sectionCard("System Maintenance Mode",
                "Emergency override switch and platform-wide safety notices.",
                maintenance));

        return col;
    }

    // -------------------------------------------------------------- helpers

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

    private JTextField textField(String initial) {
        JTextField f = new JTextField(initial);
        styleTextComponent(f);
        return f;
    }

    private void styleTextComponent(JTextComponent f) {
        f.setBackground(Theme.BG_CARD_ALT);
        f.setForeground(Theme.TEXT_PRIMARY);
        f.setCaretColor(Theme.TEXT_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
    }

    private JComboBox<String> comboBox(String[] options, String selected) {
        JComboBox<String> box = new JComboBox<>(options);
        box.setSelectedItem(selected);
        box.setBackground(Theme.BG_CARD_ALT);
        box.setForeground(Theme.TEXT_PRIMARY);
        return box;
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setPreferredSize(new Dimension(90, 24));
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setBackground(Theme.BG_CARD_ALT);
            ((JSpinner.DefaultEditor) editor).getTextField().setForeground(Theme.TEXT_PRIMARY);
        }
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

    @Override
    public void onDataChanged() {
        if (pendingHolder != null) rebuildPending();
        if (auditHolder != null) rebuildAudit();
        if (categoryHolder != null) rebuildCategories();
    }
}
