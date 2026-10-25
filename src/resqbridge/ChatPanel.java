package resqbridge;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.stream.Collectors;

/** A simple channelled chat used for Driver <-> Dispatch and Camp <-> Dispatch communication. */
public class ChatPanel extends RoundedPanel implements DataListener {

    private final DataStore store;
    private final String channel;
    private final String myName;
    private final String myRole;
    private final JPanel messagesHolder = new JPanel();
    private final JScrollPane scroll;

    public ChatPanel(DataStore store, String channel, String myName, String myRole, String title) {
        super(Theme.BG_CARD, Theme.BORDER, 14);
        this.store = store;
        this.channel = channel;
        this.myName = myName;
        this.myRole = myRole;
        store.addListener(this);

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        JLabel t = new JLabel(title);
        t.setFont(Theme.FONT_CARD_TITLE);
        t.setForeground(Theme.TEXT_PRIMARY);
        add(t, BorderLayout.NORTH);

        messagesHolder.setOpaque(false);
        messagesHolder.setLayout(new BoxLayout(messagesHolder, BoxLayout.Y_AXIS));
        scroll = new JScrollPane(messagesHolder);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.setPreferredSize(new Dimension(10, 180));
        add(scroll, BorderLayout.CENTER);

        JPanel inputRow = new JPanel(new BorderLayout(8, 0));
        inputRow.setOpaque(false);
        JTextField input = new JTextField();
        input.setBackground(Theme.BG_CARD_ALT);
        input.setForeground(Theme.TEXT_PRIMARY);
        input.setCaretColor(Theme.TEXT_PRIMARY);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_LIGHT),
                BorderFactory.createEmptyBorder(6, 8, 6, 8)));
        JButton send = DispatchCardPanel.smallButton("Send");
        Runnable doSend = () -> {
            String text = input.getText().trim();
            if (text.isEmpty()) return;
            store.chatMessages.add(new ChatMessage(channel, myName, myRole, text, DataStore.nowTime()));
            input.setText("");
            store.fireChanged();
        };
        send.addActionListener(e -> doSend.run());
        input.addActionListener(e -> doSend.run());
        inputRow.add(input, BorderLayout.CENTER);
        inputRow.add(send, BorderLayout.EAST);
        add(inputRow, BorderLayout.SOUTH);

        rebuild();
    }

    private void rebuild() {
        messagesHolder.removeAll();
        List<ChatMessage> msgs = store.chatMessages.stream()
                .filter(m -> m.channel.equals(channel))
                .collect(Collectors.toList());
        if (msgs.isEmpty()) {
            JLabel empty = new JLabel("No messages yet. Say hello.");
            empty.setForeground(Theme.TEXT_MUTED);
            empty.setFont(Theme.FONT_SMALL);
            messagesHolder.add(empty);
        }
        for (ChatMessage m : msgs) {
            boolean mine = m.fromName.equals(myName);
            JPanel line = new JPanel(new BorderLayout());
            line.setOpaque(false);
            line.setBorder(BorderFactory.createEmptyBorder(2, 0, 6, 0));
            JLabel who = new JLabel(m.time + "  " + m.fromName + " (" + m.fromRole + ")");
            who.setFont(Theme.FONT_SMALL);
            who.setForeground(mine ? Theme.ACCENT_CYAN : Theme.TEXT_MUTED);
            JLabel text = new JLabel("<html><div style='width:280px;'>" + escape(m.text) + "</div></html>");
            text.setFont(Theme.FONT_BASE);
            text.setForeground(Theme.TEXT_PRIMARY);
            JPanel col = new JPanel();
            col.setOpaque(false);
            col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
            col.add(who);
            col.add(text);
            line.add(col, BorderLayout.CENTER);
            messagesHolder.add(line);
        }
        messagesHolder.revalidate();
        messagesHolder.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    @Override
    public void onDataChanged() { rebuild(); }
}
