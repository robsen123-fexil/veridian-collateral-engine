package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ConstraintAllocator {
    public static final class Request {
        public String accountId;
        public double minAllocation;
        public double maxAllocation;
        public double weight;
        public double allocated;
    }

    public List<Request> allocateWithConstraints(double total, List<Request> requests) {
        List<Request> working = copy(requests);
        if (total <= 0 || working.isEmpty()) {
            return working;
        }
        double remaining = total;
        for (Request r : working) {
            double min = Math.max(0, r.minAllocation);
            r.allocated = Math.min(min, remaining);
            remaining -= r.allocated;
        }
        if (remaining <= 0) {
            return working;
        }
        double weightSum = 0;
        for (Request r : working) {
            weightSum += Math.max(0, r.weight);
        }
        if (weightSum <= 0) {
            distributeEqual(working, remaining);
            return working;
        }
        for (Request r : working) {
            double headroom = r.maxAllocation - r.allocated;
            if (headroom <= 0) {
                continue;
            }
            double share = (r.weight / weightSum) * remaining;
            double add = Math.min(headroom, share);
            r.allocated += add;
        }
        capToMax(working);
        normalizeTotal(working, total);
        return working;
    }

    private void distributeEqual(List<Request> working, double amount) {
        int n = working.size();
        if (n == 0) {
            return;
        }
        double each = amount / n;
        for (Request r : working) {
            double headroom = r.maxAllocation - r.allocated;
            r.allocated += Math.min(headroom, each);
        }
    }

    private void capToMax(List<Request> working) {
        for (Request r : working) {
            if (r.allocated > r.maxAllocation) {
                r.allocated = r.maxAllocation;
            }
        }
    }

    private void normalizeTotal(List<Request> working, double total) {
        double sum = 0;
        for (Request r : working) {
            sum += r.allocated;
        }
        double delta = total - sum;
        if (Math.abs(delta) < 0.0001) {
            return;
        }
        working.sort(Comparator.comparingDouble((Request r) -> r.weight).reversed());
        int i = 0;
        while (Math.abs(delta) > 0.0001 && !working.isEmpty()) {
            Request r = working.get(i % working.size());
            if (delta > 0 && r.allocated < r.maxAllocation) {
                r.allocated += 0.01;
                delta -= 0.01;
            } else if (delta < 0 && r.allocated > r.minAllocation) {
                r.allocated -= 0.01;
                delta += 0.01;
            }
            i++;
            if (i > 10000) {
                break;
            }
        }
    }

    private List<Request> copy(List<Request> requests) {
        List<Request> out = new ArrayList<>();
        for (Request src : requests) {
            Request r = new Request();
            r.accountId = src.accountId;
            r.minAllocation = src.minAllocation;
            r.maxAllocation = src.maxAllocation;
            r.weight = src.weight;
            r.allocated = 0;
            out.add(r);
        }
        return out;
    }

    public double sumAllocated(List<Request> requests) {
        double sum = 0;
        for (Request r : requests) {
            sum += r.allocated;
        }
        return sum;
    }
}
