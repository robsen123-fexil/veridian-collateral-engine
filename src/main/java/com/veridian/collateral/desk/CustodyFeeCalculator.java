package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class CustodyFeeCalculator {
    public static final class FeeSchedule {
        public double safekeepingBps;
        public double transactionFee;
        public double minimumMonthly;
    }

    public static final class FeeLine {
        public String accountId;
        public double safekeepingFee;
        public double transactionFees;
        public double total;
    }

    public FeeLine calculate(String accountId, double averageAum, int transactionCount, FeeSchedule schedule) {
        FeeLine line = new FeeLine();
        line.accountId = accountId;
        line.safekeepingFee = averageAum * (schedule.safekeepingBps / 10_000.0) / 12.0;
        line.transactionFees = transactionCount * schedule.transactionFee;
        line.total = Math.max(schedule.minimumMonthly, line.safekeepingFee + line.transactionFees);
        return line;
    }

    public List<FeeLine> calculateBatch(List<String> accounts, List<Double> aums, List<Integer> txCounts,
                                        FeeSchedule schedule) {
        List<FeeLine> out = new ArrayList<>();
        for (int i = 0; i < accounts.size(); i++) {
            double aum = i < aums.size() ? aums.get(i) : 0;
            int tx = i < txCounts.size() ? txCounts.get(i) : 0;
            out.add(calculate(accounts.get(i), aum, tx, schedule));
        }
        return out;
    }

    public double totalFees(List<FeeLine> lines) {
        double sum = 0;
        for (FeeLine line : lines) {
            sum += line.total;
        }
        return sum;
    }
}
