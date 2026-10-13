package com.securebank.gui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;

/** Flat rounded buttons. The look comes from the client property {@link Theme#STYLE_KEY} (default: PRIMARY). */
public final class RoundedButtonUI extends BasicButtonUI {
    private static final int ARC = 12;

    public static ComponentUI createUI(JComponent c) {
        return new RoundedButtonUI();
    }

    @Override public void installUI(JComponent c) {
        super.installUI(c);
        AbstractButton b = (AbstractButton) c;
        b.setOpaque(false);
        b.setBorder(BorderFactory.createEmptyBorder(9, 18, 9, 18));
        b.setFocusPainted(false);
        b.setRolloverEnabled(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setFont(Theme.font(Font.BOLD, 13));
    }

    @Override public Dimension getPreferredSize(JComponent c) {
        Dimension d = super.getPreferredSize(c);
        if (d == null) return null;
        return new Dimension(d.width, Math.max(d.height, 38));
    }

    /** Returns {fill, border, text} colours for the button's current state. */
    private static Color[] colours(AbstractButton b) {
        Object prop = b.getClientProperty(Theme.STYLE_KEY);
        Theme.Style s = prop instanceof Theme.Style ? (Theme.Style) prop : Theme.Style.PRIMARY;
        ButtonModel m = b.getModel();
        boolean pressed = m.isArmed() && m.isPressed();
        boolean hover = m.isRollover();
        if (!b.isEnabled()) return new Color[]{new Color(0xEAE6D6), null, new Color(0xA39E8A)};
        switch (s) {
            case SECONDARY:
                return new Color[]{pressed ? Theme.YELLOW_SOFT : hover ? Theme.YELLOW_TINT : Color.WHITE,
                        hover || pressed ? Theme.YELLOW_DARK : Theme.BORDER_STRONG, Theme.INK};
            case DARK:
                return new Color[]{pressed ? Color.BLACK : hover ? new Color(0x3A3314) : Theme.INK, null, Color.WHITE};
            case GHOST:
                return new Color[]{pressed ? Theme.YELLOW_SOFT : hover ? Theme.YELLOW_TINT : null, null, Theme.INK};
            case OUTLINE_LIGHT:
                return new Color[]{pressed ? new Color(255, 255, 255, 50) : hover ? new Color(255, 255, 255, 28) : null,
                        new Color(255, 255, 255, 110), Color.WHITE};
            case PRIMARY:
            default:
                return new Color[]{pressed ? Theme.YELLOW_PRESSED : hover ? Theme.YELLOW_HOVER : Theme.YELLOW, null, Theme.INK};
        }
    }

    @Override public void paint(Graphics g, JComponent c) {
        AbstractButton b = (AbstractButton) c;
        Color[] col = colours(b);
        Graphics2D g2 = Theme.aa(g);
        int w = c.getWidth(), h = c.getHeight();
        if (col[0] != null) {
            g2.setColor(col[0]);
            g2.fillRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
        }
        if (col[1] != null) {
            g2.setColor(col[1]);
            g2.drawRoundRect(0, 0, w - 1, h - 1, ARC, ARC);
        }
        if (b.hasFocus() && b.isEnabled()) {
            g2.setColor(new Color(0x1F1A0A, false));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{2f, 2f}, 0f));
            g2.drawRoundRect(3, 3, w - 7, h - 7, ARC - 4, ARC - 4);
        }
        g2.dispose();
        super.paint(g, c);
    }

    @Override protected void paintText(Graphics g, AbstractButton b, Rectangle textRect, String text) {
        Graphics2D g2 = Theme.aa(g);
        g2.setFont(b.getFont());
        g2.setColor(colours(b)[2]);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, textRect.x, textRect.y + fm.getAscent());
        g2.dispose();
    }
}
