package com.veridian.collateral.wire;

import com.veridian.collateral.util.BoundedAscii;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SwiftMt940Scanner {
    public static final class StatementLine {
        public String tag;
        public String value;
        public String raw;
    }

    public static final class ParsedStatement {
        public String account = "";
        public String currency = "";
        public double openingBalance;
        public double closingBalance;
        public final List<StatementLine> lines = new ArrayList<>();
        public final List<Transaction61> transactions = new ArrayList<>();
        public boolean valid;
    }

    public static final class Transaction61 {
        public String valueDate = "";
        public String entryDate = "";
        public char debitCredit = 'C';
        public double amount;
        public String reference = "";
        public String narrative = "";
    }

    public ParsedStatement scanStatement(byte[] data) {
        ParsedStatement stmt = new ParsedStatement();
        if (data == null || data.length < 4) {
            return stmt;
        }
        String text = new String(data, StandardCharsets.US_ASCII);
        String[] blocks = text.split("\\$");
        for (String block : blocks) {
            parseBlock(block, stmt);
        }
        stmt.valid = !stmt.account.isEmpty() && stmt.currency.length() == 3;
        return stmt;
    }

    private void parseBlock(String block, ParsedStatement stmt) {
        int idx = 0;
        while (idx < block.length()) {
            if (block.charAt(idx) != ':') {
                idx++;
                continue;
            }
            int tagEnd = block.indexOf(':', idx + 1);
            if (tagEnd < 0) {
                break;
            }
            String tag = block.substring(idx + 1, tagEnd);
            int lineEnd = block.indexOf('\n', tagEnd + 1);
            if (lineEnd < 0) {
                lineEnd = block.length();
            }
            String value = block.substring(tagEnd + 1, lineEnd).trim();
            StatementLine line = new StatementLine();
            line.tag = tag;
            line.value = value;
            line.raw = ":" + tag + ":" + value;
            stmt.lines.add(line);
            dispatchTag(tag, value, stmt);
            idx = lineEnd + 1;
        }
    }

    private void dispatchTag(String tag, String value, ParsedStatement stmt) {
        switch (tag) {
            case "25" -> stmt.account = value;
            case "60F", "60M" -> parseOpeningBalance(value, stmt);
            case "62F", "62M" -> parseClosingBalance(value, stmt);
            case "61" -> stmt.transactions.add(parseField61(value));
            default -> { }
        }
    }

    private void parseOpeningBalance(String value, ParsedStatement stmt) {
        if (value.length() < 10) {
            return;
        }
        stmt.currency = value.substring(7, 10);
        stmt.openingBalance = parseAmount(value.substring(10));
    }

    private void parseClosingBalance(String value, ParsedStatement stmt) {
        if (value.length() < 10) {
            return;
        }
        stmt.closingBalance = parseAmount(value.substring(10));
    }

    private Transaction61 parseField61(String value) {
        Transaction61 tx = new Transaction61();
        if (value.length() < 6) {
            return tx;
        }
        tx.valueDate = value.substring(0, 6);
        int dcPos = indexOfDebitCredit(value);
        if (dcPos > 0) {
            tx.debitCredit = value.charAt(dcPos);
            tx.amount = parseAmount(value.substring(dcPos + 1));
        }
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        int refStart = BoundedAscii.findSubstring(bytes, 0, bytes.length, "//".getBytes(StandardCharsets.US_ASCII));
        if (refStart >= 0) {
            tx.reference = BoundedAscii.readString(bytes, refStart + 2, 64);
        }
        tx.narrative = value;
        return tx;
    }

    private int indexOfDebitCredit(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 'C' || c == 'D' || c == 'R') {
                return i;
            }
        }
        return -1;
    }

    private double parseAmount(String s) {
        if (s == null || s.isEmpty()) {
            return 0;
        }
        StringBuilder num = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || c == '.' || c == ',') {
                num.append(c == ',' ? '.' : c);
            } else if (num.length() > 0) {
                break;
            }
        }
        try {
            return Double.parseDouble(num.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
