package com.securebank;

import com.securebank.gui.LoginFrame;
import com.securebank.gui.Theme;
import com.securebank.service.AuthService;
import com.securebank.service.BankService;
import com.securebank.storage.AuditLog;
import com.securebank.storage.Repository;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Entry point. Optional argument: path of the data folder (default: ./data). */
public final class Main {

    private Main() { }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        Path dataDir = Paths.get(args.length > 0 ? args[0] : "data");
        try {
            Repository repo = new Repository(dataDir);
            repo.load();                                   // load users, accounts, transactions
            AuditLog audit = new AuditLog(dataDir.resolve("audit.log"));
            AuthService auth = new AuthService(repo, audit);
            BankService bank = new BankService(repo, audit);

            SwingUtilities.invokeLater(() -> {
                Theme.apply();   // yellow theme (must run before any window is created)
                new LoginFrame(auth, bank, null).setVisible(true);
            });
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Could not start: " + e.getMessage(),
                    "Secure Banking Application", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
}
