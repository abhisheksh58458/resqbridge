package resqbridge;

import javax.swing.*;
import java.awt.*;
import java.text.NumberFormat;
import java.util.Locale;

public class DashboardPanel extends JPanel implements DataListener {

    private final DataStore store;
    private StatCardPanel suppliesCard;
    private StatCardPanel campsCard;
    private StatCardPanel driversCard;
    private StatCardPanel alertsCard;
    private static final NumberFormat NUM = NumberFormat.getNumberInstance(Locale.US);

    public DashboardPanel(DataStore store, Frame ownerFrame) {
        this.store = store;
        store.addListener(this);
        setOpaque(false);
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 16, 16);

        // Map card: left column, top, large.
        MapPanel map = new MapPanel(store, ownerFrame);
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0.68; gbc.weighty = 0.66;
        gbc.gridheight = 1;
        gbc.fill = GridBagConstraints.BOTH;
        add(map, gbc);

        // Bottom-left row: dispatches + inventory, side by side.
        JPanel bottomRow = new JPanel(new GridLayout(1, 2, 16, 0));
        bottomRow.setOpaque(false);
        bottomRow.add(new DispatchCardPanel(store, ownerFrame, 4));
        bottomRow.add(new InventoryCardPanel(store));

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0.68; gbc.weighty = 0.34;
        gbc.insets = new Insets(0, 0, 0, 16);
        add(bottomRow, gbc);

        // Right column: stat cards stacked, spans both rows.
        JPanel rightCol = new JPanel();
        rightCol.setOpaque(false);
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));

        suppliesCard = new StatCardPanel("Total Supplies Dispatched",
                NUM.format(store.totalSuppliesDispatched), "+18% from yesterday", Theme.GREEN,
                AppIcon.Kind.TRUCK, Theme.ACCENT_BLUE);
        campsCard = new StatCardPanel("Active Relief Camps",
                store.activeCamps() + " / " + store.totalCampsInNetwork, "50% Operational", Theme.GREEN,
                AppIcon.Kind.TENT, Theme.GREEN);
        driversCard = new StatCardPanel("Active Drivers",
                String.valueOf(store.activeDrivers()), "On Duty", Theme.ACCENT_CYAN,
                AppIcon.Kind.PERSON, Theme.ACCENT_CYAN);
        alertsCard = new StatCardPanel("Critical Alerts",
                String.valueOf(store.alerts.size()), "Requires Attention", Theme.RED,
                AppIcon.Kind.ALERT, Theme.RED);

        for (JComponent c : new JComponent[]{suppliesCard, campsCard, driversCard, alertsCard}) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
            rightCol.add(c);
            rightCol.add(Box.createVerticalStrut(16));
        }

        DonorsCardPanel donors = new DonorsCardPanel(store, ownerFrame, 4);
        donors.setAlignmentX(Component.LEFT_ALIGNMENT);
        rightCol.add(donors);

        JScrollPane rightScroll = new JScrollPane(rightCol);
        rightScroll.setOpaque(false);
        rightScroll.getViewport().setOpaque(false);
        rightScroll.setBorder(null);
        rightScroll.getVerticalScrollBar().setUnitIncrement(14);

        gbc.gridx = 1; gbc.gridy = 0;
        gbc.gridheight = 2;
        gbc.weightx = 0.32; gbc.weighty = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0);
        add(rightScroll, gbc);

        refreshStats();
    }

    private void refreshStats() {
        suppliesCard.setValue(NUM.format(store.totalSuppliesDispatched));
        int active = store.activeCamps();
        campsCard.setValue(active + " / " + store.totalCampsInNetwork);
        int pct = store.totalCampsInNetwork == 0 ? 0 : Math.round(100f * active / store.totalCampsInNetwork);
        campsCard.setDelta(pct + "% Operational", Theme.GREEN);
        driversCard.setValue(String.valueOf(store.activeDrivers()));
        alertsCard.setValue(String.valueOf(store.alerts.size()));
        alertsCard.setDelta(store.alerts.isEmpty() ? "All clear" : "Requires Attention",
                store.alerts.isEmpty() ? Theme.GREEN : Theme.RED);
    }

    @Override
    public void onDataChanged() { refreshStats(); }
}
