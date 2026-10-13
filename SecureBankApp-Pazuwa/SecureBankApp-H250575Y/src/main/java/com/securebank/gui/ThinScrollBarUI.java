package com.securebank.gui;

import javax.swing.*;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

/** Slim, modern scroll bars with no arrow buttons. */
public final class ThinScrollBarUI extends BasicScrollBarUI {

    public static ComponentUI createUI(JComponent c) {
        return new ThinScrollBarUI();
    }

    @Override protected void installDefaults() {
        super.installDefaults();
        scrollbar.setOpaque(false);
    }

    @Override protected JButton createDecreaseButton(int orientation) { return zero(); }
    @Override protected JButton createIncreaseButton(int orientation) { return zero(); }

    private static JButton zero() {
        JButton b = new JButton();
        Dimension d = new Dimension(0, 0);
        b.setPreferredSize(d);
        b.setMinimumSize(d);
        b.setMaximumSize(d);
        return b;
    }

    @Override public Dimension getPreferredSize(JComponent c) { return new Dimension(12, 12); }

    @Override protected void paintTrack(Graphics g, JComponent c, Rectangle r) { /* transparent */ }

    @Override protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
        if (r.isEmpty() || !scrollbar.isEnabled()) return;
        Graphics2D g2 = Theme.aa(g);
        g2.setColor(isDragging ? Theme.YELLOW_DARK : isThumbRollover() ? new Color(0xC9BE8C) : new Color(0xDDD5B3));
        g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, 8, 8);
        g2.dispose();
    }
}
