package com.veridian.collateral.ledger;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class DailySettlementSummary {
    public static final class DaySummary {
        public LocalDate date;
        public int tradeCount;
        public double cashTotal;
        public long quantityTotal;
        public int failCount;
    }

    public DaySummary summarize(LocalDate date, List<SettlementBlotter.BlotterEntry> entries) {
        DaySummary summary = new DaySummary();
        summary.date = date;
        for (SettlementBlotter.BlotterEntry e : entries) {
            summary.tradeCount++;
            summary.cashTotal += e.cash;
            summary.quantityTotal += e.quantity;
            if ("FAIL".equals(e.status)) {
                summary.failCount++;
            }
        }
        return summary;
    }

    public List<DaySummary> rollingSummaries(List<LocalDate> dates, List<SettlementBlotter.BlotterEntry> entries) {
        List<DaySummary> out = new ArrayList<>();
        for (LocalDate date : dates) {
            out.add(summarize(date, entries));
        }
        return out;
    }

    public double failRate(DaySummary summary) {
        if (summary.tradeCount == 0) {
            return 0;
        }
        return summary.failCount / (double) summary.tradeCount;
    }
}
