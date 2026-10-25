package resqbridge;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashSet;
import java.util.Set;

public class AdminMessagesPanel extends JPanel implements DataListener {

    private final DataStore store;
    private final JComboBox<String> channelBox = new JComboBox<>();
    private final JPanel chatHolder = new JPanel(new BorderLayout());
    private String currentChannel = null;
    private boolean suppressComboEvents = false;

    public AdminMessagesPanel(DataStore store) {
        this.store = store;
        store.addListener(this);
        setOpaque(false);
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JLabel title = new JLabel("Messages");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_PRIMARY);
        add(title, BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setOpaque(false);

        JPanel pickerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pickerRow.setOpaque(false);
        JLabel pickerLabel = new JLabel("Channel:");
        pickerLabel.setForeground(Theme.TEXT_MUTED);
        pickerLabel.setFont(Theme.FONT_SMALL);
        channelBox.setBackground(Theme.BG_CARD_ALT);
        channelBox.setForeground(Theme.TEXT_PRIMARY);
        channelBox.addActionListener(e -> {
            if (suppressComboEvents) return;
            showChannel((String) channelBox.getSelectedItem());
        });
        pickerRow.add(pickerLabel);
        pickerRow.add(channelBox);
        center.add(pickerRow, BorderLayout.NORTH);

        chatHolder.setOpaque(false);
        center.add(chatHolder, BorderLayout.CENTER);

        add(center, BorderLayout.CENTER);

        refreshChannelList();
    }

    private void refreshChannelList() {
        Set<String> channels = new LinkedHashSet<>();
        for (Driver d : store.drivers) channels.add("driver:" + d.name);
        for (Camp c : store.camps) channels.add("camp:" + c.name);
        for (ChatMessage m : store.chatMessages) channels.add(m.channel);

        String toSelect = currentChannel != null && channels.contains(currentChannel)
                ? currentChannel
                : (channels.isEmpty() ? null : channels.iterator().next());

        suppressComboEvents = true;
        channelBox.removeAllItems();
        for (String c : channels) channelBox.addItem(c);
        if (toSelect != null) channelBox.setSelectedItem(toSelect);
        suppressComboEvents = false;

        if (toSelect != null && !toSelect.equals(currentChannel)) {
            showChannel(toSelect);
        } else if (toSelect == null) {
            currentChannel = null;
            chatHolder.removeAll();
            JLabel empty = new JLabel("No channels yet.");
            empty.setForeground(Theme.TEXT_MUTED);
            chatHolder.add(empty, BorderLayout.NORTH);
            chatHolder.revalidate();
            chatHolder.repaint();
        }
    }

    /** Swaps in a fresh ChatPanel only when the selected channel actually changes. */
    private void showChannel(String channel) {
        if (channel == null || channel.equals(currentChannel)) return;
        currentChannel = channel;
        for (Component c : chatHolder.getComponents()) {
            if (c instanceof ChatPanel) store.removeListener((ChatPanel) c);
        }
        chatHolder.removeAll();
        String label = channel.startsWith("driver:") ? "Chat with " + channel.substring(7)
                : channel.startsWith("camp:") ? "Chat with " + channel.substring(5)
                : "Chat: " + channel;
        ChatPanel chat = new ChatPanel(store, channel, "Dispatch", "Admin", label);
        chatHolder.add(chat, BorderLayout.CENTER);
        chatHolder.revalidate();
        chatHolder.repaint();
    }

    @Override
    public void onDataChanged() { refreshChannelList(); }
}
