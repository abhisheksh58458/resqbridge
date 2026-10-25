package resqbridge;

import javax.swing.*;
import java.awt.*;

public class EditCampDialog extends JDialog {

    public EditCampDialog(Frame owner, DataStore store, Camp camp) {
        super(owner, "Edit " + camp.name, true);
        getContentPane().setBackground(Theme.BG_PANEL);
        setLayout(new BorderLayout(0, 12));
        ((JComponent) getContentPane()).setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));
        setSize(360, 260);
        setLocationRelativeTo(owner);
        setResizable(false);

        JLabel title = new JLabel(camp.name + (camp.hub ? "  (Distribution Hub)" : ""));
        title.setFont(Theme.FONT_CARD_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));

        JLabel sliderLabel = new JLabel("Supply level: " + camp.supplyPercent + "%");
        sliderLabel.setForeground(Theme.TEXT_MUTED);
        sliderLabel.setFont(Theme.FONT_BASE);
        sliderLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JSlider slider = new JSlider(0, 100, camp.supplyPercent);
        slider.setOpaque(false);
        slider.setMajorTickSpacing(25);
        slider.setPaintTicks(true);
        slider.setForeground(Theme.TEXT_MUTED);
        slider.addChangeListener(e -> sliderLabel.setText("Supply level: " + slider.getValue() + "%"));
        slider.setAlignmentX(Component.LEFT_ALIGNMENT);

        JCheckBox operational = new JCheckBox("Camp operational", camp.operational);
        operational.setOpaque(false);
        operational.setForeground(Theme.TEXT_MUTED);
        operational.setFont(Theme.FONT_BASE);
        operational.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(sliderLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(slider);
        form.add(Box.createVerticalStrut(14));
        form.add(operational);
        add(form, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton cancel = new JButton("Cancel");
        JButton save = new JButton("Save changes");
        save.setBackground(Theme.ACCENT_BLUE);
        save.setForeground(Color.WHITE);
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> {
            camp.supplyPercent = slider.getValue();
            camp.operational = operational.isSelected();
            store.addAudit("Updated " + camp.name + ": " + camp.supplyPercent + "% supply, "
                    + (camp.operational ? "operational" : "offline") + ".");
            dispose();
        });
        buttons.add(cancel);
        buttons.add(save);
        add(buttons, BorderLayout.SOUTH);
    }
}
