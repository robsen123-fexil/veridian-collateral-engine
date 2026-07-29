package com.veridian.collateral.desk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class VaultTransferScheduler {
    public static final class TransferRequest {
        public String id;
        public String fromVault;
        public String toVault;
        public String cusip;
        public long quantity;
        public LocalDate requestedDate;
        public int priority;
        public boolean completed;
    }

    private final List<TransferRequest> queue = new ArrayList<>();
    private long seq;

    public TransferRequest enqueue(String from, String to, String cusip, long qty, LocalDate date, int priority) {
        TransferRequest req = new TransferRequest();
        req.id = "XFR-" + (++seq);
        req.fromVault = from;
        req.toVault = to;
        req.cusip = cusip;
        req.quantity = qty;
        req.requestedDate = date;
        req.priority = priority;
        queue.add(req);
        return req;
    }

    public List<TransferRequest> planForDate(LocalDate date) {
        List<TransferRequest> out = new ArrayList<>();
        for (TransferRequest req : queue) {
            if (!req.completed && !req.requestedDate.isAfter(date)) {
                out.add(req);
            }
        }
        out.sort(Comparator.comparingInt((TransferRequest r) -> r.priority).reversed());
        return out;
    }

    public boolean complete(String id) {
        for (TransferRequest req : queue) {
            if (req.id.equals(id)) {
                req.completed = true;
                return true;
            }
        }
        return false;
    }

    public long pendingQuantity(String cusip) {
        long sum = 0;
        for (TransferRequest req : queue) {
            if (!req.completed && req.cusip.equals(cusip)) {
                sum += req.quantity;
            }
        }
        return sum;
    }
}
