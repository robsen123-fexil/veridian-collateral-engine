package com.veridian.collateral.ledger;

import java.util.ArrayList;
import java.util.List;

public final class SettlementBlotter {
    public static final class BlotterEntry {
        public long id;
        public String tradeId;
        public String cusip;
        public long quantity;
        public double cash;
        public String status;
        public long timestamp;
    }

    private final List<BlotterEntry> entries = new ArrayList<>();
    private long nextId = 1;

    public BlotterEntry post(String tradeId, String cusip, long qty, double cash, String status) {
        BlotterEntry e = new BlotterEntry();
        e.id = nextId++;
        e.tradeId = tradeId;
        e.cusip = cusip;
        e.quantity = qty;
        e.cash = cash;
        e.status = status;
        e.timestamp = System.currentTimeMillis();
        entries.add(e);
        return e;
    }

    public List<BlotterEntry> byStatus(String status) {
        List<BlotterEntry> out = new ArrayList<>();
        for (BlotterEntry e : entries) {
            if (status.equals(e.status)) {
                out.add(e);
            }
        }
        return out;
    }

    public double totalCashByStatus(String status) {
        double sum = 0;
        for (BlotterEntry e : byStatus(status)) {
            sum += e.cash;
        }
        return sum;
    }

    public int size() { return entries.size(); }
}
