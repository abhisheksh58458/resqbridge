package resqbridge;

import java.awt.Color;
import java.awt.Font;

/** Central color and font palette matching the ResQBridge dashboard. */
public class Theme {
    public static final Color BG_APP        = new Color(0x0A0E1A);
    public static final Color BG_PANEL      = new Color(0x0D1526);
    public static final Color BG_CARD       = new Color(0x101B30);
    public static final Color BG_CARD_ALT   = new Color(0x0F1A2E);
    public static final Color BORDER        = new Color(0x1E2C42);
    public static final Color BORDER_LIGHT  = new Color(0x27384f);

    public static final Color ACCENT_BLUE   = new Color(0x3B82F6);
    public static final Color ACCENT_CYAN   = new Color(0x22D3EE);
    public static final Color GREEN         = new Color(0x22C55E);
    public static final Color ORANGE        = new Color(0xF59E0B);
    public static final Color RED           = new Color(0xEF4444);
    public static final Color PURPLE        = new Color(0x8B5CF6);

    public static final Color TEXT_PRIMARY  = new Color(0xE7ECF3);
    public static final Color TEXT_MUTED    = new Color(0x8A97AC);
    public static final Color TEXT_FAINT    = new Color(0x5C6B82);

    public static final Font FONT_BASE      = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL     = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_BOLD      = new Font("SansSerif", Font.BOLD, 13);
    public static final Font FONT_TITLE     = new Font("SansSerif", Font.BOLD, 18);
    public static final Font FONT_CARD_TITLE= new Font("SansSerif", Font.BOLD, 14);
    public static final Font FONT_BIG_NUM   = new Font("SansSerif", Font.BOLD, 28);
    public static final Font FONT_HUGE_NUM  = new Font("SansSerif", Font.BOLD, 24);

    private Theme() {}
}
