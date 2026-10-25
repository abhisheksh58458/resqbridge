package resqbridge;

import javax.swing.*;
import java.awt.*;

public class ResQBridgeApp extends JFrame {

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel pages = new JPanel();

    public ResQBridgeApp(DataStore store, LoginAccount account) {
        super("ResQBridge - Disaster Relief Logistics System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1440, 900);
        setMinimumSize(new Dimension(1100, 700));
        setLocationRelativeTo(null);

        getContentPane().setBackground(Theme.BG_APP);
        setLayout(new BorderLayout());

        HeaderPanel header = new HeaderPanel();
        add(header, BorderLayout.NORTH);

        SidebarPanel sidebar = new SidebarPanel(this::showPage);
        add(sidebar, BorderLayout.WEST);

        pages.setLayout(cardLayout);
        pages.setBackground(Theme.BG_APP);

        pages.add(wrap(new DashboardPanel(store, this)), "Dashboard");
        pages.add(wrap(new CampsPanel(store)), "Relief Camps");
        pages.add(wrap(centered(new DonorsCardPanel(store, this, 0))), "Donors");
        pages.add(wrap(centered(new InventoryCardPanel(store))), "Supply Inventory");
        pages.add(wrap(centered(new DispatchCardPanel(store, this, 0))), "Dispatch");
        pages.add(wrap(new DriversPanel(store, this)), "Drivers");
        pages.add(wrap(new AlertsPanel(store)), "Alerts");
        pages.add(wrap(new OperationalReportPanel(store)), "Reports");
        pages.add(wrap(new SettingsPanel(store, this)), "Settings");

        add(pages, BorderLayout.CENTER);
    }

    private JPanel wrap(JComponent content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(Theme.BG_APP);
        wrapper.add(content, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel centered(JComponent content) {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.fill = GridBagConstraints.BOTH;
        content.setPreferredSize(new Dimension(700, 560));
        outer.add(content, gbc);
        return outer;
    }

    private void showPage(String name) {
        cardLayout.show(pages, name);
    }

    public ResQBridgeApp() {
        this(new DataStore(), new LoginAccount("admin", "admin123", "Asha Nair", "Admin", "", ""));
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        SwingUtilities.invokeLater(() -> new ResQBridgeApp().setVisible(true));
    }
}
