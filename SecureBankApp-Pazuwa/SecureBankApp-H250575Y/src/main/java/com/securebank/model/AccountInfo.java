package com.securebank.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Read-only snapshot of an account handed to the GUI, so the UI cannot change balances directly. */
public record AccountInfo(String number, Account.Type type, BigDecimal balance, LocalDateTime createdAt) {
    public static AccountInfo from(Account a) {
        return new AccountInfo(a.getNumber(), a.getType(), a.getBalance(), a.getCreatedAt());
    }
}
