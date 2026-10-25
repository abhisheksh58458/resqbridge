package resqbridge;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OperationalReportPanel extends JPanel {
    private final DataStore store;

    public OperationalReportPanel(DataStore store) {
        this.store = store;
        setOpaque(false);
        setLayout(new BorderLayout(12, 12));
        setBorder(new EmptyBorder(16, 16, 16, 16));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Relief Operations Report");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);

        JButton downloadPdf = new JButton("Download PDF");
        downloadPdf.setBackground(Theme.ACCENT_BLUE);
        downloadPdf.setForeground(Color.WHITE);
        downloadPdf.setFocusPainted(false);
        downloadPdf.addActionListener(e -> exportPdf());

        header.add(title, BorderLayout.WEST);
        header.add(downloadPdf, BorderLayout.EAST);

        JPanel content = buildReportBody();
        JScrollPane scroll = new JScrollPane(content);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setOpaque(false);
        scroll.setOpaque(false);

        add(header, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
    }

    private JPanel buildReportBody() {
        JPanel body = new JPanel();
        body.setOpaque(false);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(new EmptyBorder(8, 0, 8, 0));

        JPanel summary = new JPanel(new GridLayout(1, 4, 12, 12));
        summary.setOpaque(false);
        summary.add(metricCard("Supplies Dispatched", String.valueOf(store.totalSuppliesDispatched), "Total units"));
        summary.add(metricCard("Active Drivers", String.valueOf(activeDrivers()), "On assignment"));
        summary.add(metricCard("Open Alerts", String.valueOf(store.alerts.size()), "Needs attention"));
        summary.add(metricCard("Camps in Network", String.valueOf(store.camps.size()), "Operational units"));
        body.add(summary);
        body.add(Box.createVerticalStrut(16));

        body.add(sectionCard("1. Donor Contributions", donorTable()));
        body.add(Box.createVerticalStrut(12));
        body.add(sectionCard("2. Driver Work & Delivery Activity", driverTable()));
        body.add(Box.createVerticalStrut(12));
        body.add(sectionCard("3. Camp Supply Needs & Remaining Inventory", campTable()));
        body.add(Box.createVerticalStrut(12));
        body.add(sectionCard("4. Inventory Snapshot", inventoryTable()));

        return body;
    }

    private JPanel metricCard(String label, String value, String hint) {
        RoundedPanel card = new RoundedPanel(Theme.BG_CARD, Theme.BORDER, 14);
        card.setLayout(new BorderLayout());
        card.setBorder(new EmptyBorder(14, 14, 14, 14));

        JLabel title = new JLabel(label);
        title.setFont(Theme.FONT_SMALL);
        title.setForeground(Theme.TEXT_MUTED);

        JLabel amount = new JLabel(value);
        amount.setFont(Theme.FONT_TITLE.deriveFont(24f));
        amount.setForeground(Theme.ACCENT_CYAN);

        JLabel hintLabel = new JLabel(hint);
        hintLabel.setFont(Theme.FONT_SMALL);
        hintLabel.setForeground(Theme.TEXT_FAINT);

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.add(title);
        text.add(Box.createVerticalStrut(6));
        text.add(amount);
        text.add(Box.createVerticalStrut(4));
        text.add(hintLabel);

        card.add(text, BorderLayout.CENTER);
        return card;
    }

    private JPanel sectionCard(String title, JTable table) {
        RoundedPanel card = new RoundedPanel(Theme.BG_CARD, Theme.BORDER, 16);
        card.setLayout(new BorderLayout(10, 10));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel label = new JLabel(title);
        label.setFont(Theme.FONT_CARD_TITLE);
        label.setForeground(Theme.TEXT_PRIMARY);

        table.setFillsViewportHeight(true);
        table.setRowHeight(28);
        table.setOpaque(false);
        ((JComponent) table.getDefaultRenderer(Object.class)).setOpaque(false);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);

        card.add(label, BorderLayout.NORTH);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    private JTable donorTable() {
        Object[][] rows = new Object[store.donors.size()][3];
        for (int i = 0; i < store.donors.size(); i++) {
            Donor donor = store.donors.get(i);
            rows[i][0] = donor.name;
            rows[i][1] = money(donor.amount);
            rows[i][2] = donorAmountCategory(donor.amount);
        }
        String[] cols = {"Donor Name", "Amount Received", "Status"};
        JTable table = new JTable(rows, cols);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(Theme.FONT_SMALL);
        table.getColumnModel().getColumn(0).setPreferredWidth(220);
        table.getColumnModel().getColumn(1).setPreferredWidth(140);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        return table;
    }

    private JTable driverTable() {
        List<Object[]> rows = new ArrayList<>();
        for (DeliveryRun run : store.deliveryRuns) {
            String itemSummary = "";
            for (ManifestItem item : run.items) {
                if (!itemSummary.isEmpty()) itemSummary += ", ";
                itemSummary += item.name + " x" + item.qty;
            }
            if (itemSummary.isEmpty()) itemSummary = "No manifest items";
            rows.add(new Object[]{run.driverName, run.fromLocation + " → " + run.toCamp, itemSummary, run.status, run.etaMinutes + " min"});
        }
        if (rows.isEmpty()) {
            rows.add(new Object[]{"No active routes", "-", "-", "-", "-"});
        }
        Object[][] data = rows.toArray(new Object[0][]);
        String[] cols = {"Driver", "Route", "Supplies Carried", "Status", "ETA"};
        JTable table = new JTable(data, cols);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(Theme.FONT_SMALL);
        return table;
    }

    private JTable campTable() {
        List<Object[]> rows = new ArrayList<>();
        for (Camp camp : store.camps) {
            String needText = camp.supplyPercent < 45 ? "Urgent restock" : camp.supplyPercent < 70 ? "Monitor closely" : "Stable";
            int remaining = Math.max(0, 100 - camp.supplyPercent);
            rows.add(new Object[]{camp.name, camp.supplyPercent + "%", remaining + "% remaining gap", needText, camp.operational ? "Open" : "Closed"});
        }
        Object[][] data = rows.toArray(new Object[0][]);
        String[] cols = {"Camp", "Supply Left", "Gap to Full", "Priority", "Status"};
        JTable table = new JTable(data, cols);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(Theme.FONT_SMALL);
        return table;
    }

    private JTable inventoryTable() {
        List<Object[]> rows = new ArrayList<>();
        if (store.inventory.isEmpty()) {
            rows.add(new Object[]{"No stock categories", "0%", "0 units", "-"});
        } else {
            for (java.util.Map.Entry<String, Integer> entry : store.inventory.entrySet()) {
                rows.add(new Object[]{entry.getKey(), entry.getValue() + "%", estimatedUnits(entry.getKey(), entry.getValue()), stockLabel(entry.getValue())});
            }
        }
        Object[][] data = rows.toArray(new Object[0][]);
        String[] cols = {"Category", "In Stock", "Estimated Units", "Stock Level"};
        JTable table = new JTable(data, cols);
        table.setDefaultEditor(Object.class, null);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFont(Theme.FONT_SMALL);
        return table;
    }

    private int activeDrivers() {
        int count = 0;
        for (Driver driver : store.drivers) {
            if (driver.onDuty) count++;
        }
        return count;
    }

    private String donorAmountCategory(long amount) {
        if (amount >= 2000000) return "Major donor";
        if (amount >= 1000000) return "Strong support";
        return "Regular donor";
    }

    private String money(long amount) {
        DecimalFormat formatter = new DecimalFormat("#,##0");
        return "₹ " + formatter.format(amount);
    }

    private String estimatedUnits(String category, int percent) {
        int units;
        String lower = category.toLowerCase(Locale.ROOT);
        if ("medical".equals(lower)) {
            units = 180 + (percent * 3);
        } else if ("food".equals(lower)) {
            units = 300 + (percent * 4);
        } else if ("water".equals(lower)) {
            units = 420 + (percent * 5);
        } else if ("shelter".equals(lower)) {
            units = 220 + (percent * 3);
        } else {
            units = 100 + percent * 2;
        }
        return String.valueOf(units);
    }

    private String stockLabel(int percent) {
        if (percent >= 80) return "Healthy";
        if (percent >= 60) return "Stable";
        if (percent >= 40) return "Low";
        return "Critical";
    }

    private void exportPdf() {
        File output = choosePdfFile();
        if (output == null) return;
        try {
            String content = buildPdfText();
            writePdf(output, content);
            JOptionPane.showMessageDialog(this, "PDF report saved to:\n" + output.getAbsolutePath(), "Download complete", JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Unable to generate PDF: " + ex.getMessage(), "PDF export failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private File choosePdfFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("PDF Documents", "pdf"));
        chooser.setSelectedFile(new File("ResQBridge-Operations-Report.pdf"));
        int result = chooser.showSaveDialog(this);
        return result == JFileChooser.APPROVE_OPTION ? chooser.getSelectedFile() : null;
    }

    private String buildPdfText() {
        StringBuilder sb = new StringBuilder();
        sb.append("ResQBridge Relief Operations Report\n");
        sb.append("Generated: ").append(java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))).append("\n\n");
        sb.append("Total supplies dispatched: ").append(store.totalSuppliesDispatched).append(" units\n");
        sb.append("Active drivers: ").append(activeDrivers()).append("\n");
        sb.append("Open alerts: ").append(store.alerts.size()).append("\n");
        sb.append("Camp locations monitored: ").append(store.camps.size()).append("\n\n");

        sb.append("Donor Contributions\n");
        for (Donor donor : store.donors) {
            sb.append("- ").append(donor.name).append(" : ").append(money(donor.amount)).append("\n");
        }
        sb.append("\n");

        sb.append("Driver Work\n");
        for (DeliveryRun run : store.deliveryRuns) {
            sb.append("- Driver: ").append(run.driverName)
              .append(" | Route: ").append(run.fromLocation).append(" -> ").append(run.toCamp)
              .append(" | Status: ").append(run.status)
              .append(" | ETA: ").append(run.etaMinutes).append(" min\n");
            if (!run.items.isEmpty()) {
                for (ManifestItem item : run.items) {
                    sb.append("    * ").append(item.name).append(" x").append(item.qty)
                      .append(" (Priority: ").append(item.priority).append(")\n");
                }
            }
        }
        sb.append("\n");

        sb.append("Camp Needs\n");
        for (Camp camp : store.camps) {
            sb.append("- ").append(camp.name)
              .append(" | Supply: ").append(camp.supplyPercent).append("%")
              .append(" | Remaining gap: ").append(Math.max(0, 100 - camp.supplyPercent)).append("%")
              .append(" | Priority: ").append(camp.supplyPercent < 45 ? "Urgent" : camp.supplyPercent < 70 ? "Monitor" : "Stable")
              .append(" | Status: ").append(camp.operational ? "Open" : "Closed").append("\n");
        }
        sb.append("\n");

        sb.append("Inventory Snapshot\n");
        for (java.util.Map.Entry<String, Integer> entry : store.inventory.entrySet()) {
            sb.append("- ").append(entry.getKey())
              .append(" : ").append(entry.getValue()).append("% in stock")
              .append(" | Estimated units: ").append(estimatedUnits(entry.getKey(), entry.getValue())).append("\n");
        }

        return sb.toString();
    }

    private void writePdf(File output, String text) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\\R")) {
            lines.add(line);
        }

        StringBuilder content = new StringBuilder();
        int y = 780;
        for (String line : lines) {
            String safe = line.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
            content.append("BT\n/F1 11 Tf\n50 ").append(y).append(" Td\n(").append(safe).append(") Tj\nET\n");
            y -= 14;
        }

        String contentStream = content.toString();
        byte[] contentBytes = contentStream.getBytes(StandardCharsets.ISO_8859_1);

        String obj1 = "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n";
        String obj2 = "2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n";
        String obj3 = "3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>\nendobj\n";
        String obj4 = "4 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>\nendobj\n";
        String obj5 = "5 0 obj\n<< /Length " + contentBytes.length + " >>\nstream\n" + contentStream + "endstream\nendobj\n";

        java.util.List<String> objects = java.util.Arrays.asList(obj1, obj2, obj3, obj4, obj5);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write("%PDF-1.4\n".getBytes(StandardCharsets.ISO_8859_1));

        long[] offsets = new long[objects.size() + 1];
        offsets[0] = 0;
        for (int i = 0; i < objects.size(); i++) {
            offsets[i + 1] = baos.size();
            baos.write(objects.get(i).getBytes(StandardCharsets.ISO_8859_1));
        }

        long xrefStart = baos.size();
        baos.write(("xref\n0 " + (objects.size() + 1) + "\n").getBytes(StandardCharsets.ISO_8859_1));
        baos.write("0000000000 65535 f \n".getBytes(StandardCharsets.ISO_8859_1));
        for (int i = 1; i <= objects.size(); i++) {
            baos.write(String.format("%010d 00000 n \n", offsets[i]).getBytes(StandardCharsets.ISO_8859_1));
        }

        baos.write(("trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\nstartxref\n" + xrefStart + "\n%%EOF\n").getBytes(StandardCharsets.ISO_8859_1));
        Files.write(output.toPath(), baos.toByteArray());
    }
}
