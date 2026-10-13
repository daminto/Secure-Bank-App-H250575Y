package com.securebank.gui;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.BitSet;
import java.util.Locale;

/** Table styling: zebra rows, tidy header, mono / signed-amount renderers and coloured type badges. */
final class Tables {
    private Tables() { }

    static void style(JTable t, int... rightAlignedColumns) {
        t.setRowHeight(36);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setFillsViewportHeight(true);
        t.setFont(Theme.font(Font.PLAIN, 13));
        t.setForeground(Theme.TEXT);
        t.setBackground(Color.WHITE);
        t.setSelectionBackground(Theme.YELLOW_SOFT);
        t.setSelectionForeground(Theme.INK);
        t.setDefaultRenderer(Object.class, new Base(SwingConstants.LEFT, Theme.TEXT, Font.PLAIN, false, false));

        BitSet right = new BitSet();
        for (int c : rightAlignedColumns) right.set(c);
        JTableHeader h = t.getTableHeader();
        h.setReorderingAllowed(false);
        h.setDefaultRenderer(new Header(right));
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 38));
    }

    static void column(JTable t, int index, TableCellRenderer renderer, int width) {
        if (renderer != null) t.getColumnModel().getColumn(index).setCellRenderer(renderer);
        t.getColumnModel().getColumn(index).setPreferredWidth(width);
    }

    static JScrollPane scroll(JTable t) {
        t.setPreferredScrollableViewportSize(new Dimension(300, 90));
        JScrollPane sp = new JScrollPane(t);
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        sp.getViewport().setBackground(Color.WHITE);
        sp.setBackground(Color.WHITE);
        return sp;
    }

    // ---- renderer factories
    static TableCellRenderer mono()   { return new Base(SwingConstants.LEFT, Theme.TEXT, Font.PLAIN, true, false); }
    static TableCellRenderer muted()  { return new Base(SwingConstants.LEFT, Theme.MUTED, Font.PLAIN, false, false); }
    static TableCellRenderer money(boolean bold) {
        return new Base(SwingConstants.RIGHT, Theme.INK, bold ? Font.BOLD : Font.PLAIN, true, false);
    }
    static TableCellRenderer signed() { return new Base(SwingConstants.RIGHT, Theme.INK, Font.BOLD, true, true); }
    static TableCellRenderer badge()  { return new Badge(); }

    private static Color rowColour(boolean selected, int row) {
        return selected ? Theme.YELLOW_SOFT : (row % 2 == 0 ? Color.WHITE : Theme.ZEBRA);
    }

    private static class Base extends DefaultTableCellRenderer {
        private final int align;
        private final Color fg;
        private final Font font;
        private final boolean colourBySign;

        Base(int align, Color fg, int fontStyle, boolean mono, boolean colourBySign) {
            this.align = align;
            this.fg = fg;
            this.font = mono ? Theme.mono(fontStyle, 13) : Theme.font(fontStyle, 13);
            this.colourBySign = colourBySign;
        }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, false, row, col);
            setHorizontalAlignment(align);
            setFont(font);
            setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            setBackground(rowColour(sel, row));
            Color c = fg;
            if (colourBySign) c = String.valueOf(v).startsWith("-") ? Theme.ERROR : Theme.SUCCESS;
            setForeground(c);
            return this;
        }
    }

    private static final class Header extends DefaultTableCellRenderer {
        private final BitSet right;

        Header(BitSet right) { this.right = right; }

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            super.getTableCellRendererComponent(t, v == null ? "" : v.toString().toUpperCase(Locale.ROOT), false, false, row, col);
            setFont(Theme.font(Font.BOLD, 11));
            setForeground(Theme.MUTED);
            setBackground(Theme.YELLOW_TINT);
            setHorizontalAlignment(right.get(t.convertColumnIndexToModel(col)) ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 2, 0, Theme.YELLOW),
                    BorderFactory.createEmptyBorder(0, 14, 0, 14)));
            return this;
        }
    }

    /** Coloured pill used for account and transaction types. */
    private static final class Badge extends JLabel implements TableCellRenderer {
        private Color rowBg = Color.WHITE, pillBg = Theme.YELLOW_SOFT, pillFg = Theme.WARN;

        @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int row, int col) {
            String raw = v == null ? "" : v.toString();
            String key = raw.toUpperCase(Locale.ROOT);
            setText(raw.isEmpty() ? "" : raw.charAt(0) + raw.substring(1).toLowerCase(Locale.ROOT));
            rowBg = rowColour(sel, row);
            switch (key) {
                case "DEPOSIT": case "SAVINGS":
                    pillBg = new Color(0xDDF3E4); pillFg = new Color(0x1E7A45); break;
                case "WITHDRAWAL":
                    pillBg = new Color(0xFBE0DC); pillFg = new Color(0xB3382C); break;
                case "CURRENT":
                    pillBg = new Color(0xE5EAF2); pillFg = new Color(0x3D5273); break;
                default:
                    pillBg = new Color(0xFFF0B3); pillFg = Theme.WARN;
            }
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            g2.setColor(rowBg);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setFont(Theme.font(Font.BOLD, 11));
            FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int pw = fm.stringWidth(text) + 22, ph = 22;
            int x = 14, y = (getHeight() - ph) / 2;
            g2.setColor(pillBg);
            g2.fillRoundRect(x, y, pw, ph, ph, ph);
            g2.setColor(pillFg);
            g2.drawString(text, x + 11, y + (ph - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
