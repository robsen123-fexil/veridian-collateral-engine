package com.veridian.collateral.desk;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class BasisAdjustmentLedger {
    public static final class BasisEntry {
        public String legId;
        public String currencyPair;
        public double notional;
        public double basisPoints;
        public LocalDate accrualStart;
        public LocalDate accrualEnd;
        public double accrued;
    }

    public double accrue(BasisEntry entry) {
        if (entry.accrualStart == null || entry.accrualEnd == null) {
            return 0;
        }
        long days = ChronoUnit.DAYS.between(entry.accrualStart, entry.accrualEnd);
        if (days <= 0) {
            return 0;
        }
        double rate = entry.basisPoints / 10_000.0;
        entry.accrued = entry.notional * rate * (days / 360.0);
        return entry.accrued;
    }

    public double rollForward(List<BasisEntry> entries, LocalDate asOf) {
        double total = 0;
        for (BasisEntry e : entries) {
            BasisEntry copy = e;
            copy.accrualEnd = asOf;
            total += accrue(copy);
        }
        return total;
    }

    public List<BasisEntry> splitByCurrency(List<BasisEntry> entries, String ccy) {
        List<BasisEntry> out = new ArrayList<>();
        for (BasisEntry e : entries) {
            if (e.currencyPair != null && e.currencyPair.contains(ccy)) {
                out.add(e);
            }
        }
        return out;
    }
}
