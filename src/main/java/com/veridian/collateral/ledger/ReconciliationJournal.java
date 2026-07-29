package com.veridian.collateral.ledger;

import java.util.ArrayList;
import java.util.List;

public final class ReconciliationJournal {
    public static final class Entry {
        public long id;
        public String leftRef;
        public String rightRef;
        public double leftAmount;
        public double rightAmount;
        public double tolerance;
        public boolean matched;
        public long timestamp;
    }

    private final List<Entry> entries = new ArrayList<>();
    private long nextId = 1;

    public Entry record(String leftRef, String rightRef, double leftAmount, double rightAmount, double tolerance) {
        Entry e = new Entry();
        e.id = nextId++;
        e.leftRef = leftRef;
        e.rightRef = rightRef;
        e.leftAmount = leftAmount;
        e.rightAmount = rightAmount;
        e.tolerance = tolerance;
        e.matched = Math.abs(leftAmount - rightAmount) <= tolerance;
        e.timestamp = System.currentTimeMillis();
        entries.add(e);
        return e;
    }

    public List<Entry> unmatched() {
        List<Entry> out = new ArrayList<>();
        for (Entry e : entries) {
            if (!e.matched) {
                out.add(e);
            }
        }
        return out;
    }

    public double totalResidual() {
        double sum = 0;
        for (Entry e : entries) {
            if (!e.matched) {
                sum += Math.abs(e.leftAmount - e.rightAmount);
            }
        }
        return sum;
    }

    public int size() { return entries.size(); }
}
