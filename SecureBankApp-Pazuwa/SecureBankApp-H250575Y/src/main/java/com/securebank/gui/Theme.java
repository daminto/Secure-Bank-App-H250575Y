package com.securebank.gui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.Path2D;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/** Central place for the application's colours, fonts and small UI factories (yellow professional theme). */
public final class Theme {

    // ---- palette
    public static final Color INK            = new Color(0x1F1A0A);   // near-black warm
    public static final Color TEXT           = new Color(0x2B2615);
    public static final Color MUTED          = new Color(0x7A7461);
    public static final Color BG             = new Color(0xF6F3E8);   // app background
    public static final Color BORDER         = new Color(0xE8E2CC);
    public static final Color BORDER_STRONG  = new Color(0xD3CCAF);
    public static final Color YELLOW         = new Color(0xF7C600);   // brand
    public static final Color YELLOW_HOVER   = new Color(0xFFD21F);
    public static final Color YELLOW_PRESSED = new Color(0xDDAE00);
    public static final Color YELLOW_DARK    = new Color(0xB38F00);
    public static final Color YELLOW_SOFT    = new Color(0xFFF1B8);   // selection
    public static final Color YELLOW_TINT    = new Color(0xFFF9DB);   // table header
    public static final Color ZEBRA          = new Color(0xFCFAF2);
    public static final Color SUCCESS        = new Color(0x1E8449);
    public static final Color ERROR          = new Color(0xC0392B);
    public static final Color WARN           = new Color(0x8A6500);

    /** Button looks, see {@link RoundedButtonUI}. */
    public enum Style { PRIMARY, SECONDARY, DARK, GHOST, OUTLINE_LIGHT }
    public static final String STYLE_KEY = "securebank.button.style";

    private static String family = Font.SANS_SERIF;
    private static String monoFamily = Font.MONOSPACED;

    private Theme() { }

    // ---- fonts
    public static Font font(int style, float size) {
        return new Font(family, Font.PLAIN, 13).deriveFont(style, size);
    }

    public static Font mono(int style, float size) {
        return new Font(monoFamily, Font.PLAIN, 13).deriveFont(style, size);
    }

    private static String pick(String fallback, String... wanted) {
        Set<String> have = new HashSet<>();
        Collections.addAll(have, GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        for (String w : wanted) if (have.contains(w)) return w;
        return fallback;
    }

    // ---- setup

    /** Call once, before any window is created. */
    public static void apply() {
        family = pick(Font.SANS_SERIF, "Segoe UI", "SF Pro Text", "Helvetica Neue", "Inter", "Roboto", "Noto Sans", "Arial");
        monoFamily = pick(Font.MONOSPACED, "Consolas", "Menlo", "DejaVu Sans Mono", "Liberation Mono");
        try {
            MetalLookAndFeel.setCurrentTheme(new YellowMetalTheme());
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception ignored) { /* fall back to default look and feel */ }

        for (Object key : Collections.list(UIManager.getDefaults().keys())) {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(font(Font.PLAIN, 13)));
            }
        }

        String buttonUi = RoundedButtonUI.class.getName();
        UIManager.put("ButtonUI", buttonUi);
        UIManager.put(buttonUi, RoundedButtonUI.class);
        String scrollUi = ThinScrollBarUI.class.getName();
        UIManager.put("ScrollBarUI", scrollUi);
        UIManager.put(scrollUi, ThinScrollBarUI.class);

        ColorUIResource white = new ColorUIResource(Color.WHITE);
        ColorUIResource ink = new ColorUIResource(INK);
        ColorUIResource yellow = new ColorUIResource(YELLOW);
        UIManager.put("Panel.background", white);
        UIManager.put("OptionPane.background", white);
        UIManager.put("OptionPane.messageForeground", ink);
        UIManager.put("OptionPane.buttonAreaBorder", BorderFactory.createEmptyBorder(8, 12, 12, 12));
        UIManager.put("Label.foreground", ink);
        UIManager.put("CheckBox.background", white);
        UIManager.put("CheckBox.foreground", ink);
        UIManager.put("TextField.selectionBackground", yellow);
        UIManager.put("PasswordField.selectionBackground", yellow);
        UIManager.put("TextField.selectionForeground", ink);
        UIManager.put("PasswordField.selectionForeground", ink);
        UIManager.put("ComboBox.background", white);
        UIManager.put("ComboBox.selectionBackground", yellow);
        UIManager.put("ComboBox.selectionForeground", ink);
        UIManager.put("FileChooser.background", white);
        UIManager.put("ToolTip.background", new ColorUIResource(INK));
        UIManager.put("ToolTip.foreground", white);
    }

    // ---- component factories

    public static JButton button(String text, Style style) {
        JButton b = new JButton(text);
        b.putClientProperty(STYLE_KEY, style);
        return b;
    }

    public static JLabel label(String text, float size, int style, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    /** A transparent panel (so cards and coloured backgrounds show through). */
    public static JPanel panel(LayoutManager lm) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        return p;
    }

    public static <T extends JComponent> T left(T c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    public static Component gap(int height) {
        return Box.createVerticalStrut(height);
    }

    /** HTML-wraps (and escapes) text so long messages wrap inside a label of the given pixel width. */
    public static String wrap(String text, int widthPx) {
        if (text == null || text.isBlank()) return " ";
        String esc = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        return "<html><body style='width:" + widthPx + "px'>" + esc + "</body></html>";
    }

    public static void onChange(JTextComponent c, Runnable r) {
        c.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e)  { r.run(); }
            @Override public void removeUpdate(DocumentEvent e)  { r.run(); }
            @Override public void changedUpdate(DocumentEvent e) { r.run(); }
        });
    }

    /** Rounded, focus-aware input field. */
    public static void styleField(JTextComponent c) {
        c.setFont(font(Font.PLAIN, 14));
        c.setForeground(INK);
        c.setBackground(Color.WHITE);
        c.setCaretColor(INK);
        c.setBorder(new FieldBorder());
        c.setMaximumSize(new Dimension(Short.MAX_VALUE, c.getPreferredSize().height));   // don't stretch in BoxLayout
        c.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { c.repaint(); }
            @Override public void focusLost(FocusEvent e)   { c.repaint(); }
        });
    }

    public static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    // ---- icons

    /** Shield with a check mark: the application logo. */
    public static Icon shield(int size, Color fill, Color mark) {
        return new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = aa(g);
                g2.translate(x, y);
                double s = size;
                Path2D p = new Path2D.Double();
                p.moveTo(0.50 * s, 0.04 * s);
                p.lineTo(0.90 * s, 0.19 * s);
                p.lineTo(0.90 * s, 0.50 * s);
                p.curveTo(0.90 * s, 0.75 * s, 0.70 * s, 0.90 * s, 0.50 * s, 0.97 * s);
                p.curveTo(0.30 * s, 0.90 * s, 0.10 * s, 0.75 * s, 0.10 * s, 0.50 * s);
                p.lineTo(0.10 * s, 0.19 * s);
                p.closePath();
                g2.setColor(fill);
                g2.fill(p);
                Path2D tick = new Path2D.Double();
                tick.moveTo(0.32 * s, 0.50 * s);
                tick.lineTo(0.45 * s, 0.63 * s);
                tick.lineTo(0.69 * s, 0.37 * s);
                g2.setColor(mark);
                g2.setStroke(new BasicStroke((float) (0.09 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(tick);
                g2.dispose();
            }
        };
    }

    /** Round avatar showing the first letter of the user name. */
    public static Icon avatar(String name, int size) {
        String letter = (name == null || name.isEmpty()) ? "?" : name.substring(0, 1).toUpperCase();
        return new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = aa(g);
                g2.setColor(YELLOW);
                g2.fillOval(x, y, size, size);
                g2.setColor(INK);
                g2.setFont(font(Font.BOLD, size * 0.45f));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(letter, x + (size - fm.stringWidth(letter)) / 2,
                        y + (size - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
    }

    public static Icon dot(Color color, int size) {
        return new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = aa(g);
                g2.setColor(color);
                g2.fillOval(x, y, size, size);
                g2.dispose();
            }
        };
    }

    // ---- internals

    private static final class FieldBorder extends AbstractBorder {
        @Override public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = aa(g);
            boolean focus = c.hasFocus();
            g2.setColor(focus ? YELLOW_DARK : BORDER_STRONG);
            g2.setStroke(new BasicStroke(focus ? 2f : 1f));
            float inset = focus ? 1f : 0.5f;
            g2.draw(new java.awt.geom.RoundRectangle2D.Float(x + inset, y + inset, w - 2 * inset - (focus ? 0 : 0), h - 2 * inset, 10, 10));
            g2.dispose();
        }
        @Override public Insets getBorderInsets(Component c) { return new Insets(9, 12, 9, 12); }
        @Override public Insets getBorderInsets(Component c, Insets i) {
            i.set(9, 12, 9, 12);
            return i;
        }
    }

    /** Metal colour theme so any stock component (dialogs, file chooser) also turns yellow. */
    private static final class YellowMetalTheme extends DefaultMetalTheme {
        private final ColorUIResource p1 = new ColorUIResource(0xB38F00);
        private final ColorUIResource p2 = new ColorUIResource(YELLOW);
        private final ColorUIResource p3 = new ColorUIResource(YELLOW_SOFT);
        private final ColorUIResource s1 = new ColorUIResource(0xB8B08C);
        private final ColorUIResource s2 = new ColorUIResource(0xE3DDC3);
        private final ColorUIResource s3 = new ColorUIResource(0xFFFFFF);
        private final ColorUIResource black = new ColorUIResource(INK);
        @Override public String getName() { return "Yellow"; }
        @Override protected ColorUIResource getPrimary1() { return p1; }
        @Override protected ColorUIResource getPrimary2() { return p2; }
        @Override protected ColorUIResource getPrimary3() { return p3; }
        @Override protected ColorUIResource getSecondary1() { return s1; }
        @Override protected ColorUIResource getSecondary2() { return s2; }
        @Override protected ColorUIResource getSecondary3() { return s3; }
        @Override protected ColorUIResource getBlack() { return black; }
    }
}
