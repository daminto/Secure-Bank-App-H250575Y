package com.securebank.gui;

import javax.swing.*;
import java.awt.*;

/** White rounded panel with a soft shadow; the building block of the dashboard. */
public class Card extends JPanel {
    private static final int RADIUS = 16;
    private Color fill = Color.WHITE;

    public Card(LayoutManager layout, Insets pad) {
        super(layout);
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(pad.top + 4, pad.left + 4, pad.bottom + 6, pad.right + 4));
    }

    public Card setFill(Color c) {
        this.fill = c;
        return this;
    }

    /** Card with a bold title (and an optional component at the right of the title). */
    public static Card titled(String title, JComponent right, JComponent content) {
        Card c = new Card(new BorderLayout(0, 12), new Insets(16, 18, 16, 18));
        JPanel head = Theme.panel(new BorderLayout());
        head.add(Theme.label(title, 15, Font.BOLD, Theme.INK), BorderLayout.WEST);
        if (right != null) head.add(right, BorderLayout.EAST);
        c.add(head, BorderLayout.NORTH);
        c.add(content, BorderLayout.CENTER);
        return c;
    }

    @Override protected void paintComponent(Graphics g) {
        Graphics2D g2 = Theme.aa(g);
        int x = 3, y = 2, w = getWidth() - 6, h = getHeight() - 7;
        for (int i = 3; i >= 1; i--) {
            g2.setColor(new Color(80, 60, 0, 8));
            g2.fillRoundRect(x - i, y - i + 2, w + 2 * i, h + 2 * i, RADIUS + 2 * i, RADIUS + 2 * i);
        }
        g2.setColor(fill);
        g2.fillRoundRect(x, y, w, h, RADIUS, RADIUS);
        g2.setColor(Theme.BORDER);
        g2.drawRoundRect(x, y, w - 1, h - 1, RADIUS, RADIUS);
        g2.dispose();
    }
}
