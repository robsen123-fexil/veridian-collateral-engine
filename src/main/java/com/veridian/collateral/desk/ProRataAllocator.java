package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProRataAllocator {
    public static final class AccountSlice {
        public String accountId;
        public long priority;
        public double weight;
        public long allocated;
    }

    public List<AccountSlice> allocate(long totalShares, List<AccountSlice> accounts) {
        List<AccountSlice> working = new ArrayList<>();
        for (AccountSlice a : accounts) {
            AccountSlice copy = new AccountSlice();
            copy.accountId = a.accountId;
            copy.priority = a.priority;
            copy.weight = a.weight;
            copy.allocated = 0;
            working.add(copy);
        }
        working.sort(Comparator.comparingLong((AccountSlice s) -> s.priority).reversed());
        double weightSum = 0;
        for (AccountSlice s : working) {
            if (s.weight > 0) {
                weightSum += s.weight;
            }
        }
        if (weightSum <= 0 || totalShares <= 0) {
            return working;
        }
        long assigned = 0;
        for (AccountSlice s : working) {
            if (s.weight <= 0) {
                continue;
            }
            long portion = (long) Math.floor((s.weight / weightSum) * totalShares);
            s.allocated = portion;
            assigned += portion;
        }
        long remainder = totalShares - assigned;
        int idx = 0;
        while (remainder > 0 && !working.isEmpty()) {
            AccountSlice s = working.get(idx % working.size());
            s.allocated++;
            remainder--;
            idx++;
        }
        return working;
    }

    public long sumAllocated(List<AccountSlice> slices) {
        long sum = 0;
        for (AccountSlice s : slices) {
            sum += s.allocated;
        }
        return sum;
    }
}
