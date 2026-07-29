package com.veridian.collateral.desk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SettlementCheckpoint {
    public static final class CheckpointRecord {
        public String tradeId;
        public LocalDate tradeDate;
        public LocalDate settlementDate;
        public String cusip;
        public long quantity;
        public double cashAmount;
        public int cycleDay;
        public boolean matched;
        public String failReason = "";
    }

    private final Map<String, CheckpointRecord> records = new HashMap<>();
    private final List<String> eventLog = new ArrayList<>();

    public void registerTrade(String tradeId, LocalDate tradeDate, int tPlus, String cusip, long qty, double cash) {
        CheckpointRecord rec = new CheckpointRecord();
        rec.tradeId = tradeId;
        rec.tradeDate = tradeDate;
        rec.settlementDate = tradeDate.plusDays(tPlus);
        rec.cusip = cusip;
        rec.quantity = qty;
        rec.cashAmount = cash;
        rec.cycleDay = tPlus;
        records.put(tradeId, rec);
        eventLog.add("register:" + tradeId);
    }

    public boolean markMatched(String tradeId) {
        CheckpointRecord rec = records.get(tradeId);
        if (rec == null) {
            return false;
        }
        rec.matched = true;
        eventLog.add("matched:" + tradeId);
        return true;
    }

    public boolean markFailed(String tradeId, String reason) {
        CheckpointRecord rec = records.get(tradeId);
        if (rec == null) {
            return false;
        }
        rec.matched = false;
        rec.failReason = reason;
        eventLog.add("fail:" + tradeId + ":" + reason);
        return true;
    }

    public List<CheckpointRecord> dueOn(LocalDate date) {
        List<CheckpointRecord> out = new ArrayList<>();
        for (CheckpointRecord rec : records.values()) {
            if (rec.settlementDate.equals(date)) {
                out.add(rec);
            }
        }
        return out;
    }

    public List<CheckpointRecord> unmatched() {
        List<CheckpointRecord> out = new ArrayList<>();
        for (CheckpointRecord rec : records.values()) {
            if (!rec.matched && rec.failReason.isEmpty()) {
                out.add(rec);
            }
        }
        return out;
    }

    public double totalCashDue(LocalDate date) {
        double sum = 0;
        for (CheckpointRecord rec : dueOn(date)) {
            sum += rec.cashAmount;
        }
        return sum;
    }

    public long totalQuantityDue(LocalDate date) {
        long sum = 0;
        for (CheckpointRecord rec : dueOn(date)) {
            sum += rec.quantity;
        }
        return sum;
    }

    public int countByCycle(int cycleDay) {
        int n = 0;
        for (CheckpointRecord rec : records.values()) {
            if (rec.cycleDay == cycleDay) {
                n++;
            }
        }
        return n;
    }

    public Map<String, Long> quantityByCusip() {
        Map<String, Long> out = new HashMap<>();
        for (CheckpointRecord rec : records.values()) {
            out.merge(rec.cusip, rec.quantity, Long::sum);
        }
        return out;
    }

    public List<String> eventLog() {
        return new ArrayList<>(eventLog);
    }
}
