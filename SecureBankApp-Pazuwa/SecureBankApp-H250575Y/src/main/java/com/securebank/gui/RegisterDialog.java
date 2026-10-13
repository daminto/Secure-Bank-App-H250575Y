package com.securebank.gui;

import com.securebank.service.AuthService;
import com.securebank.service.BankException;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;

/** "Create account" dialog with a live password-strength meter and inline error messages. */
final class RegisterDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private final AuthService auth;
    private final JTextField userField = new JTextField(20);
    private final JPasswordField pass1 = new JPasswordField(20);
    private final JPasswordField pass2 = new JPasswordField(20);
    private final StrengthBar strength = new StrengthBar();
    private final JLabel error = Theme.label(" ", 12, Font.PLAIN, Theme.ERROR);
    private String result;

    /** Shows the dialog; returns the new username, or null if cancelled. */
    static String show(Frame owner, AuthService auth, String presetUser) {
        RegisterDialog d = new RegisterDialog(owner, auth, presetUser);
        d.setVisible(true);
        return d.result;
    }

    private RegisterDialog(Frame owner, AuthService auth, String presetUser) {
        super(owner, "Create account", true);
        this.auth = auth;
        userField.setText(presetUser == null ? "" : presetUser);

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(28, 34, 24, 34));

        p.add(Theme.left(Theme.label("Create your account", 22, Font.BOLD, Theme.INK)));
        p.add(Theme.gap(4));
        p.add(Theme.left(Theme.label("It only takes a moment.", 13, Font.PLAIN, Theme.MUTED)));
        p.add(Theme.gap(22));

        p.add(Theme.left(Theme.label("Username", 12, Font.BOLD, Theme.TEXT)));
        p.add(Theme.gap(6));
        Theme.styleField(userField);
        p.add(Theme.left(userField));
        p.add(Theme.gap(4));
        p.add(Theme.left(Theme.label("3-20 letters, digits or underscores", 12, Font.PLAIN, Theme.MUTED)));
        p.add(Theme.gap(14));

        p.add(Theme.left(Theme.label("Password", 12, Font.BOLD, Theme.TEXT)));
        p.add(Theme.gap(6));
        Theme.styleField(pass1);
        p.add(Theme.left(pass1));
        p.add(Theme.gap(8));
        p.add(Theme.left(strength));
        p.add(Theme.gap(2));
        p.add(Theme.left(Theme.label("8-64 characters: upper + lower case, a digit, a symbol", 12, Font.PLAIN, Theme.MUTED)));
        p.add(Theme.gap(14));

        p.add(Theme.left(Theme.label("Confirm password", 12, Font.BOLD, Theme.TEXT)));
        p.add(Theme.gap(6));
        Theme.styleField(pass2);
        p.add(Theme.left(pass2));
        p.add(Theme.gap(10));

        JPanel errBox = Theme.panel(new BorderLayout());
        errBox.add(error, BorderLayout.NORTH);
        errBox.setPreferredSize(new Dimension(10, 36));
        errBox.setMaximumSize(new Dimension(Short.MAX_VALUE, 36));
        p.add(Theme.left(errBox));
        p.add(Theme.gap(6));

        JButton cancel = Theme.button("Cancel", Theme.Style.SECONDARY);
        JButton create = Theme.button("Create account", Theme.Style.PRIMARY);
        cancel.addActionListener(e -> dispose());
        create.addActionListener(e -> submit());
        JPanel buttons = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.add(cancel);
        buttons.add(create);
        buttons.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
        p.add(Theme.left(buttons));

        Theme.onChange(pass1, this::refreshStrength);
        getRootPane().setDefaultButton(create);

        p.setPreferredSize(new Dimension(440, p.getPreferredSize().height));
        setContentPane(p);
        pack();
        setResizable(false);
        setLocationRelativeTo(owner);
        refreshStrength();
    }

    private void refreshStrength() {
        char[] pw = pass1.getPassword();
        strength.update(pw);
        Arrays.fill(pw, '\0');
    }

    private void submit() {
        char[] a = pass1.getPassword();
        char[] b = pass2.getPassword();
        try {
            auth.register(userField.getText(), a, b);
            result = userField.getText().trim();
            dispose();
        } catch (BankException ex) {
            error.setText(Theme.wrap(ex.getMessage(), 340));
        } finally {
            Arrays.fill(a, '\0');
            Arrays.fill(b, '\0');
            pass1.setText("");
            pass2.setText("");
        }
    }

    /** Four-segment strength bar with a text verdict. */
    private static final class StrengthBar extends JComponent {
        private static final long serialVersionUID = 1L;
        private int level;
        private String text = " ";

        StrengthBar() {
            setPreferredSize(new Dimension(100, 20));
            setMaximumSize(new Dimension(Short.MAX_VALUE, 20));
        }

        void update(char[] pw) {
            boolean lo = false, up = false, di = false, sy = false;
            for (char c : pw) {
                if (Character.isLowerCase(c)) lo = true;
                else if (Character.isUpperCase(c)) up = true;
                else if (Character.isDigit(c)) di = true;
                else sy = true;
            }
            int score = 0;
            if (pw.length >= 8) score++;
            if (pw.length >= 12) score++;
            if (lo && up) score++;
            if (di) score++;
            if (sy) score++;
            if (pw.length == 0) { level = 0; text = " "; }
            else if (score <= 1) { level = 1; text = "Weak"; }
            else if (score <= 3) { level = 2; text = "Fair"; }
            else if (score == 4) { level = 3; text = "Good"; }
            else { level = 4; text = "Strong"; }
            repaint();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            Color[] colours = {Theme.ERROR, new Color(0xE67E22), Theme.YELLOW, Theme.SUCCESS};
            int barW = getWidth() - 70, gap = 5, seg = (barW - 3 * gap) / 4, y = (getHeight() - 6) / 2;
            for (int i = 0; i < 4; i++) {
                g2.setColor(i < level ? colours[level - 1] : Theme.BORDER);
                g2.fillRoundRect(i * (seg + gap), y, seg, 6, 6, 6);
            }
            g2.setFont(Theme.font(Font.BOLD, 12));
            g2.setColor(level == 0 ? Theme.MUTED : Theme.INK);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, barW + 12, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }
}
