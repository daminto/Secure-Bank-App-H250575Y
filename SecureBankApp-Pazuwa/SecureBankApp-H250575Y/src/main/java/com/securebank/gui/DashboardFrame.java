package com.securebank.gui;

import com.securebank.model.Account;
import com.securebank.model.AccountInfo;
import com.securebank.model.Transaction;
import com.securebank.pdf.StatementExporter;
import com.securebank.service.BankException;
import com.securebank.service.BankService;
import com.securebank.service.InputValidator;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Main window: accounts, deposits / withdrawals, history and PDF statement export. */
public final class DashboardFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private static final int IDLE_TIMEOUT_MS = 5 * 60 * 1000;
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String user;
    private final BankService bank;
    private final Consumer<String> onLogout;
    private final DecimalFormat money = new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final DefaultTableModel accountModel = readOnlyModel("Account No.", "Type", "Balance", "Opened");
    private final DefaultTableModel historyModel = readOnlyModel("Date / time", "Ref", "Type", "Amount", "Balance after");
    private final JTable accountTable = new JTable(accountModel);
    private final JTable historyTable = new JTable(historyModel);
    private final JTextField amountField = new JTextField(10);
    private final JLabel status = Theme.label("Ready", 12, Font.PLAIN, Theme.MUTED);
    private final JLabel historyCount = Theme.label(" ", 12, Font.PLAIN, Theme.MUTED);
    private final StatCard totalCard = new StatCard("TOTAL BALANCE", true);
    private final StatCard countCard = new StatCard("ACCOUNTS", false);
    private final StatCard selectedCard = new StatCard("SELECTED ACCOUNT", false);
    private List<AccountInfo> accounts = List.of();

    private final javax.swing.Timer idleTimer;
    private final AWTEventListener activityListener;

    public DashboardFrame(String user, BankService bank, Consumer<String> onLogout) {
        super("Secure Banking Application - " + user);
        this.user = user;
        this.bank = bank;
        this.onLogout = onLogout;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BG);
        setContentPane(root);
        root.add(topBar(), BorderLayout.NORTH);

        // summary cards
        JPanel stats = Theme.panel(new GridLayout(1, 3, 16, 0));
        stats.add(totalCard);
        stats.add(countCard);
        stats.add(selectedCard);

        // tables
        accountTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        accountTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) refreshHistory();
        });
        Tables.style(accountTable, 2);
        Tables.column(accountTable, 0, Tables.mono(), 150);
        Tables.column(accountTable, 1, Tables.badge(), 110);
        Tables.column(accountTable, 2, Tables.money(true), 140);
        Tables.column(accountTable, 3, Tables.muted(), 170);
        Tables.style(historyTable, 3, 4);
        Tables.column(historyTable, 0, Tables.muted(), 170);
        Tables.column(historyTable, 1, Tables.mono(), 130);
        Tables.column(historyTable, 2, Tables.badge(), 120);
        Tables.column(historyTable, 3, Tables.signed(), 130);
        Tables.column(historyTable, 4, Tables.money(false), 140);
        Card accountsCard = Card.titled("My accounts", null, Tables.scroll(accountTable));
        Card historyCard = Card.titled("Transaction history", historyCount, Tables.scroll(historyTable));

        // actions
        JButton deposit = Theme.button("Deposit", Theme.Style.PRIMARY);
        JButton withdraw = Theme.button("Withdraw", Theme.Style.DARK);
        JButton newAccount = Theme.button("Open new account", Theme.Style.SECONDARY);
        JButton export = Theme.button("Export statement (PDF)", Theme.Style.SECONDARY);
        deposit.addActionListener(e -> transact(true));
        withdraw.addActionListener(e -> transact(false));
        newAccount.addActionListener(e -> openAccount());
        export.addActionListener(e -> exportPdf());

        Theme.styleField(amountField);
        JPanel left = Theme.panel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.add(Theme.label("Amount", 13, Font.BOLD, Theme.TEXT));
        left.add(amountField);
        left.add(deposit);
        left.add(withdraw);
        JPanel right = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        right.add(newAccount);
        right.add(export);
        Card actions = new Card(new BorderLayout(16, 0), new Insets(10, 14, 10, 14));
        actions.add(left, BorderLayout.WEST);
        actions.add(right, BorderLayout.EAST);

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(18, 24, 6, 24));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.gridy = 0; c.weighty = 0;   c.insets = new Insets(0, 0, 12, 0); content.add(stats, c);
        c.gridy = 1; c.weighty = 0.4; content.add(accountsCard, c);
        c.gridy = 2; c.weighty = 0.6; content.add(historyCard, c);
        c.gridy = 3; c.weighty = 0;   c.insets = new Insets(0, 0, 0, 0); content.add(actions, c);
        root.add(content, BorderLayout.CENTER);
        root.add(statusBar(), BorderLayout.SOUTH);

        // auto-logout after inactivity
        idleTimer = new javax.swing.Timer(IDLE_TIMEOUT_MS, e -> logout("You were signed out due to inactivity."));
        idleTimer.setRepeats(false);
        activityListener = ev -> idleTimer.restart();
        Toolkit.getDefaultToolkit().addAWTEventListener(activityListener,
                AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        idleTimer.start();

        refreshAccounts(null);
        setSize(1020, 740);
        setMinimumSize(new Dimension(900, 640));
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------ layout helpers

    private JPanel topBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.INK);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, Theme.YELLOW),
                BorderFactory.createEmptyBorder(12, 24, 12, 24)));

        JLabel brand = new JLabel("SecureBank", Theme.shield(32, Theme.YELLOW, Theme.INK), SwingConstants.LEFT);
        brand.setIconTextGap(12);
        brand.setForeground(Color.WHITE);
        brand.setFont(Theme.font(Font.BOLD, 19));
        bar.add(brand, BorderLayout.WEST);

        JPanel names = Theme.panel(null);
        names.setLayout(new BoxLayout(names, BoxLayout.Y_AXIS));
        names.add(Theme.label("Signed in as", 11, Font.PLAIN, new Color(0xC9C3A8)));
        names.add(Theme.label(user, 14, Font.BOLD, Color.WHITE));

        JButton logout = Theme.button("Logout", Theme.Style.OUTLINE_LIGHT);
        logout.addActionListener(e -> logout(null));

        JPanel who = Theme.panel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        who.add(new JLabel(Theme.avatar(user, 38)));
        who.add(names);
        who.add(Box.createHorizontalStrut(6));
        who.add(logout);
        bar.add(who, BorderLayout.EAST);
        return bar;
    }

    private JPanel statusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(Theme.BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Theme.BORDER),
                BorderFactory.createEmptyBorder(9, 28, 9, 28)));
        bar.add(status, BorderLayout.WEST);
        bar.add(Theme.label("Auto sign-out after " + (IDLE_TIMEOUT_MS / 60000) + " minutes of inactivity",
                12, Font.PLAIN, Theme.MUTED), BorderLayout.EAST);
        return bar;
    }

    /** Small summary tile at the top of the dashboard. */
    private static final class StatCard extends Card {
        private static final long serialVersionUID = 1L;
        private final JLabel value;
        private final JLabel sub;

        StatCard(String title, boolean dark) {
            super(new BorderLayout(0, 4), new Insets(14, 18, 14, 18));
            Color soft = dark ? new Color(0xC9C3A8) : Theme.MUTED;
            if (dark) setFill(Theme.INK);
            value = Theme.label("-", 25, Font.BOLD, dark ? Theme.YELLOW : Theme.INK);
            sub = Theme.label(" ", 12, Font.PLAIN, soft);
            add(Theme.label(title, 11, Font.BOLD, soft), BorderLayout.NORTH);
            add(value, BorderLayout.CENTER);
            add(sub, BorderLayout.SOUTH);
        }

        void update(String v, String s) {
            value.setText(v);
            sub.setText(s);
        }
    }

    // ------------------------------------------------------------------ actions

    private void transact(boolean deposit) {
        AccountInfo acc = selectedAccount();
        if (acc == null) { warn("Select an account first."); return; }
        try {
            BigDecimal amount = InputValidator.parseAmount(amountField.getText(), false);
            Transaction tx = deposit ? bank.deposit(user, acc.number(), amount)
                                     : bank.withdraw(user, acc.number(), amount);
            amountField.setText("");
            refreshAccounts(acc.number());
            setStatus((deposit ? "Deposited " : "Withdrew ") + money.format(amount)
                    + " - new balance " + money.format(tx.balanceAfter()) + " (" + tx.id() + ")");
        } catch (BankException ex) {
            warn(ex.getMessage());
        }
    }

    private void openAccount() {
        JComboBox<Account.Type> type = new JComboBox<>(Account.Type.values());
        JTextField initial = new JTextField("0.00", 10);
        Theme.styleField(initial);
        type.setFont(Theme.font(Font.PLAIN, 14));
        JPanel p = new JPanel(new GridLayout(0, 2, 10, 12));
        p.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        p.add(Theme.label("Account type", 13, Font.BOLD, Theme.TEXT));   p.add(type);
        p.add(Theme.label("Initial deposit", 13, Font.BOLD, Theme.TEXT)); p.add(initial);
        if (JOptionPane.showConfirmDialog(this, p, "Open new account",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) return;
        try {
            BigDecimal amount = InputValidator.parseAmount(initial.getText(), true);
            AccountInfo created = bank.openAccount(user, (Account.Type) type.getSelectedItem(), amount);
            refreshAccounts(created.number());
            setStatus("Opened " + created.type() + " account " + created.number());
        } catch (BankException ex) {
            warn(ex.getMessage());
        }
    }

    private void exportPdf() {
        AccountInfo acc = selectedAccount();
        if (acc == null) { warn("Select an account to export."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save statement as PDF");
        chooser.setFileFilter(new FileNameExtensionFilter("PDF document (*.pdf)", "pdf"));
        chooser.setSelectedFile(new File("Statement-" + acc.number() + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        Path target = chooser.getSelectedFile().toPath();
        if (!target.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            target = target.resolveSibling(target.getFileName() + ".pdf");
        }
        if (Files.exists(target) && JOptionPane.showConfirmDialog(this,
                target.getFileName() + " already exists. Overwrite it?", "Confirm overwrite",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            StatementExporter.export(user, acc, bank.historyOf(user, acc.number()), target);
            setStatus("Statement saved to " + target.toAbsolutePath());
            if (JOptionPane.showConfirmDialog(this, "Statement saved.\nOpen it now?", "Export complete",
                    JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION
                    && Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                Desktop.getDesktop().open(target.toFile());
            }
        } catch (BankException ex) {
            warn(ex.getMessage());
        } catch (IOException ex) {
            warn("Could not write the PDF: " + ex.getMessage());
        }
    }

    private void logout(String notice) {
        idleTimer.stop();
        Toolkit.getDefaultToolkit().removeAWTEventListener(activityListener);
        dispose();
        onLogout.accept(notice);
    }

    // ------------------------------------------------------------------ refresh helpers

    private void refreshAccounts(String keepNumber) {
        accounts = bank.accountsOf(user);
        accountModel.setRowCount(0);
        int select = accounts.isEmpty() ? -1 : 0;
        for (int i = 0; i < accounts.size(); i++) {
            AccountInfo a = accounts.get(i);
            accountModel.addRow(new Object[]{a.number(), a.type(), money.format(a.balance()), a.createdAt().format(STAMP)});
            if (a.number().equals(keepNumber)) select = i;
        }
        if (select >= 0) accountTable.setRowSelectionInterval(select, select);
        refreshHistory();
    }

    private void refreshStats() {
        BigDecimal total = BigDecimal.ZERO;
        long savings = 0, current = 0;
        for (AccountInfo a : accounts) {
            total = total.add(a.balance());
            if (a.type() == Account.Type.SAVINGS) savings++; else current++;
        }
        totalCard.update(money.format(total), accounts.size() + (accounts.size() == 1 ? " account" : " accounts"));
        countCard.update(String.valueOf(accounts.size()), savings + " Savings  \u00b7  " + current + " Current");
        AccountInfo sel = selectedAccount();
        if (sel == null) selectedCard.update("\u2014", "Select an account below");
        else selectedCard.update(sel.number(), sel.type() + "  \u00b7  Balance " + money.format(sel.balance()));
    }

    private void refreshHistory() {
        refreshStats();
        historyModel.setRowCount(0);
        AccountInfo acc = selectedAccount();
        historyCount.setText(" ");
        if (acc == null) return;
        try {
            for (Transaction t : bank.historyOf(user, acc.number())) {
                String sign = t.type() == Transaction.Type.WITHDRAWAL ? "-" : "+";
                historyModel.addRow(new Object[]{t.timestamp().format(STAMP), t.id(), t.type(),
                        sign + money.format(t.amount()), money.format(t.balanceAfter())});
            }
            int n = historyModel.getRowCount();
            historyCount.setText(n + (n == 1 ? " transaction" : " transactions"));
        } catch (BankException ex) {
            warn(ex.getMessage());
        }
    }

    private AccountInfo selectedAccount() {
        int row = accountTable.getSelectedRow();
        return row < 0 ? null : accounts.get(accountTable.convertRowIndexToModel(row));
    }

    private void setStatus(String text) {
        status.setForeground(Theme.SUCCESS);
        status.setText(text);
    }

    private void warn(String text) {
        JOptionPane.showMessageDialog(this, text, "Secure Banking Application", JOptionPane.WARNING_MESSAGE);
    }

    private static JPanel titled(String title, JComponent inner) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(title));
        p.add(inner, BorderLayout.CENTER);
        return p;
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }
}
