package com.veridian.collateral.wire;

import com.veridian.collateral.util.BoundedAscii;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class SwiftMt940ParserCore {
    public static final class Block {
        public String tag;
        public String value;
        public int lineNumber;
    }

    public static final class Statement {
        public String transactionReference = "";
        public String account = "";
        public String statementNumber = "";
        public double openingBalance;
        public double closingBalance;
        public String openingCurrency = "";
        public String closingCurrency = "";
        public final List<Block> blocks = new ArrayList<>();
        public final List<SwiftMt940Scanner.Transaction61> transactions = new ArrayList<>();
        public boolean valid;
    }

    public Statement parseFull(byte[] data) {
        Statement stmt = new Statement();
        if (data == null || data.length == 0) {
            return stmt;
        }
        String text = new String(data, StandardCharsets.US_ASCII);
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }
            Block block = parseLine(line, i + 1);
            if (block == null) {
                continue;
            }
            stmt.blocks.add(block);
            applyBlock(stmt, block);
        }
        stmt.valid = !stmt.account.isEmpty() && stmt.closingCurrency.length() == 3;
        return stmt;
    }

    private Block parseLine(String line, int lineNo) {
        if (!line.startsWith(":")) {
            return null;
        }
        int second = line.indexOf(':', 1);
        if (second < 0) {
            return null;
        }
        Block block = new Block();
        block.tag = line.substring(0, second + 1);
        block.value = line.substring(second + 1);
        block.lineNumber = lineNo;
        return block;
    }

    private void applyBlock(Statement stmt, Block block) {
        switch (block.tag) {
            case ":20:" -> stmt.transactionReference = block.value;
            case ":25:" -> stmt.account = block.value;
            case ":28C:" -> stmt.statementNumber = block.value;
            case ":60F:", ":60M:" -> parseBalance(block.value, true, stmt);
            case ":62F:", ":62M:" -> parseBalance(block.value, false, stmt);
            case ":61:" -> stmt.transactions.add(parse61(block.value));
            default -> { }
        }
    }

    private void parseBalance(String value, boolean opening, Statement stmt) {
        if (value.length() < 10) {
            return;
        }
        char dc = value.charAt(0);
        String ccy = value.substring(7, 10);
        double amt = parseAmount(value.substring(10));
        if (opening) {
            stmt.openingCurrency = ccy;
            stmt.openingBalance = dc == 'D' ? -amt : amt;
        } else {
            stmt.closingCurrency = ccy;
            stmt.closingBalance = dc == 'D' ? -amt : amt;
        }
    }

    private SwiftMt940Scanner.Transaction61 parse61(String value) {
        SwiftMt940Scanner.Transaction61 tx = new SwiftMt940Scanner.Transaction61();
        if (value.length() < 6) {
            return tx;
        }
        tx.valueDate = value.substring(0, 6);
        int dcIdx = findDc(value);
        if (dcIdx > 0) {
            tx.debitCredit = value.charAt(dcIdx);
            tx.amount = parseAmount(value.substring(dcIdx + 1));
        }
        byte[] bytes = value.getBytes(StandardCharsets.US_ASCII);
        int ref = BoundedAscii.findSubstring(bytes, 0, bytes.length, "//".getBytes(StandardCharsets.US_ASCII));
        if (ref >= 0) {
            tx.reference = BoundedAscii.readString(bytes, ref + 2, 64);
        }
        tx.narrative = value;
        return tx;
    }

    private int findDc(String value) {
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == 'C' || c == 'D' || c == 'R') {
                return i;
            }
        }
        return -1;
    }

    private double parseAmount(String s) {
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

    public double reconcileBalances(Statement stmt) {
        double txSum = 0;
        for (SwiftMt940Scanner.Transaction61 tx : stmt.transactions) {
            double signed = tx.amount;
            if (tx.debitCredit == 'D') {
                signed = -signed;
            }
            txSum += signed;
        }
        return stmt.openingBalance + txSum - stmt.closingBalance;
    }

    public List<Block> blocksByTag(Statement stmt, String tag) {
        List<Block> out = new ArrayList<>();
        for (Block b : stmt.blocks) {
            if (tag.equals(b.tag)) {
                out.add(b);
            }
        }
        return out;
    }
}
