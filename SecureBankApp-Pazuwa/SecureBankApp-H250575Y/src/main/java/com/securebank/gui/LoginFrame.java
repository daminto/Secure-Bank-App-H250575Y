package com.securebank.gui;

import com.securebank.service.AuthService;
import com.securebank.service.BankException;
import com.securebank.service.BankService;

import javax.swing.*;
import java.awt.*;

/** Sign-in window: brand panel on the left, form on the right. */
public final class LoginFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private final AuthService auth;
    private final BankService bank;
    private final JTextField userField = new JTextField(20);
    private final JPasswordField passField = new JPasswordField(20);
    private final JLabel message = Theme.label(" ", 13, Font.PLAIN, Theme.ERROR);

    public LoginFrame(AuthService auth, BankService bank, String notice) {
        super("Secure Banking Application - Login");
        this.auth = auth;
        this.bank = bank;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());
        root.add(new BrandPanel(), BorderLayout.WEST);
        root.add(buildForm(), BorderLayout.CENTER);
        setContentPane(root);

        if (notice != null) showMessage(notice, Theme.WARN);

        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    private JPanel buildForm() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(BorderFactory.createEmptyBorder(48, 52, 32, 52));
        p.setPreferredSize(new Dimension(450, 540));

        p.add(Theme.left(Theme.label("Welcome back", 27, Font.BOLD, Theme.INK)));
        p.add(Theme.gap(6));
        p.add(Theme.left(Theme.label("Sign in to access your accounts", 14, Font.PLAIN, Theme.MUTED)));
        p.add(Theme.gap(32));

        p.add(Theme.left(Theme.label("Username", 12, Font.BOLD, Theme.TEXT)));
        p.add(Theme.gap(6));
        Theme.styleField(userField);
        p.add(Theme.left(userField));
        p.add(Theme.gap(16));

        p.add(Theme.left(Theme.label("Password", 12, Font.BOLD, Theme.TEXT)));
        p.add(Theme.gap(6));
        Theme.styleField(passField);
        p.add(Theme.left(passField));
        p.add(Theme.gap(8));

        char defaultEcho = passField.getEchoChar();
        JCheckBox show = new JCheckBox("Show password");
        show.setOpaque(false);
        show.setFocusPainted(false);
        show.setFont(Theme.font(Font.PLAIN, 12));
        show.setForeground(Theme.MUTED);
        show.addActionListener(e -> passField.setEchoChar(show.isSelected() ? (char) 0 : defaultEcho));
        p.add(Theme.left(show));
        p.add(Theme.gap(8));

        message.setVerticalAlignment(SwingConstants.TOP);
        JPanel msgBox = Theme.panel(new BorderLayout());
        msgBox.add(message, BorderLayout.NORTH);
        msgBox.setPreferredSize(new Dimension(10, 40));
        msgBox.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        p.add(Theme.left(msgBox));
        p.add(Theme.gap(6));

        JButton login = Theme.button("Sign in", Theme.Style.PRIMARY);
        login.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
        login.addActionListener(e -> doLogin());
        p.add(Theme.left(login));
        p.add(Theme.gap(12));

        JButton register = Theme.button("Create an account", Theme.Style.SECONDARY);
        register.setMaximumSize(new Dimension(Short.MAX_VALUE, 44));
        register.addActionListener(e -> doRegister());
        p.add(Theme.left(register));

        p.add(Box.createVerticalGlue());
        p.add(Theme.left(Theme.label("Passwords are never stored in plain text.", 12, Font.PLAIN, Theme.MUTED)));

        getRootPane().setDefaultButton(login);
        return p;
    }

    private void showMessage(String text, Color colour) {
        message.setForeground(colour);
        message.setText(Theme.wrap(text, 340));
    }

    private void doLogin() {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        try {
            String user = auth.login(userField.getText(), passField.getPassword());
            DashboardFrame dash = new DashboardFrame(user, bank,
                    notice -> new LoginFrame(auth, bank, notice).setVisible(true));
            dash.setVisible(true);
            dispose();
        } catch (BankException ex) {
            showMessage(ex.getMessage(), Theme.ERROR);
        } finally {
            passField.setText("");
            setCursor(Cursor.getDefaultCursor());
        }
    }

    private void doRegister() {
        String created = RegisterDialog.show(this, auth, userField.getText());
        if (created != null) {
            userField.setText(created);
            showMessage("Account created. You can now sign in.", Theme.SUCCESS);
            passField.requestFocusInWindow();
        }
    }

    /** Yellow gradient side panel with logo, tagline and security highlights. */
    private static final class BrandPanel extends JPanel {
        private static final long serialVersionUID = 1L;

        BrandPanel() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(48, 40, 40, 32));
            setPreferredSize(new Dimension(320, 540));

            add(Theme.left(new JLabel(Theme.shield(76, Theme.INK, Theme.YELLOW))));
            add(Theme.gap(20));
            add(Theme.left(Theme.label("SecureBank", 31, Font.BOLD, Theme.INK)));
            add(Theme.gap(4));
            add(Theme.left(Theme.label("Banking you can trust.", 15, Font.PLAIN, new Color(0x4A3F10))));
            add(Box.createVerticalGlue());
            add(feature("Salted PBKDF2 password hashing"));
            add(Theme.gap(12));
            add(feature("Auto sign-out when you are idle"));
            add(Theme.gap(12));
            add(feature("Every action written to an audit log"));
        }

        private static JLabel feature(String text) {
            JLabel l = Theme.label(text, 13, Font.PLAIN, Theme.INK);
            l.setIcon(Theme.dot(Theme.INK, 7));
            l.setIconTextGap(12);
            return Theme.left(l);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = Theme.aa(g);
            int w = getWidth(), h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, new Color(0xFFD83D), w, h, new Color(0xF0B400)));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(255, 255, 255, 55));
            g2.fillOval(-90, h - 210, 290, 290);
            g2.fillOval(w - 130, -70, 230, 230);
            g2.setColor(new Color(255, 255, 255, 35));
            g2.fillOval(w - 60, h - 120, 150, 150);
            g2.dispose();
        }
    }
}
