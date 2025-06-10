package com.veridian.collateral.ledger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PositionLedger {
    public static final class Position {
        public String accountId;
        public String symbol;
        public long quantity;
        public double avgPrice;
        public double marketValue;
        public long version;
    }

    public static final class LedgerEntry {
        public long sequence;
        public String accountId;
        public String symbol;
        public long deltaQty;
        public double price;
        public long timestamp;
    }

    private final Map<String, Position> positions = new HashMap<>();
    private final List<LedgerEntry> journal = new ArrayList<>();
    private long nextSequence = 1;

    public void applyFill(String accountId, String symbol, long qty, double price) {
        String key = accountId + "|" + symbol;
        Position pos = positions.computeIfAbsent(key, k -> {
            Position p = new Position();
            p.accountId = accountId;
            p.symbol = symbol;
            return p;
        });
        long newQty = pos.quantity + qty;
        if (newQty != 0) {
            pos.avgPrice = ((pos.avgPrice * pos.quantity) + (price * qty)) / newQty;
        }
        pos.quantity = newQty;
        pos.marketValue = pos.quantity * price;
        pos.version++;
        LedgerEntry entry = new LedgerEntry();
        entry.sequence = nextSequence++;
        entry.accountId = accountId;
        entry.symbol = symbol;
        entry.deltaQty = qty;
        entry.price = price;
        entry.timestamp = System.currentTimeMillis();
        journal.add(entry);
    }

    public Position getPosition(String accountId, String symbol) {
        return positions.get(accountId + "|" + symbol);
    }

    public List<Position> listByAccount(String accountId) {
        List<Position> out = new ArrayList<>();
        for (Position p : positions.values()) {
            if (p.accountId.equals(accountId)) {
                out.add(p);
            }
        }
        return out;
    }

    public double totalMarketValue(String accountId) {
        double sum = 0;
        for (Position p : listByAccount(accountId)) {
            sum += p.marketValue;
        }
        return sum;
    }

    public long journalSize() {
        return journal.size();
    }

    public List<LedgerEntry> sliceJournal(long fromSeq, int limit) {
        List<LedgerEntry> out = new ArrayList<>();
        for (LedgerEntry e : journal) {
            if (e.sequence >= fromSeq && out.size() < limit) {
                out.add(e);
            }
        }
        return out;
    }
}
