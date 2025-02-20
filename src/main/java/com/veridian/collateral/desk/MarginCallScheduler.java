package com.veridian.collateral.desk;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class MarginCallScheduler {
    public static final class MarginCall {
        public String id;
        public String accountId;
        public double deficit;
        public LocalDateTime issuedAt;
        public LocalDate deadlineDate;
        public int escalationLevel;
        public boolean settled;
    }

    private final List<MarginCall> calls = new ArrayList<>();
    private long sequence;

    public MarginCall schedule(String accountId, double deficit, LocalDate startDate, int businessDays) {
        MarginCall call = new MarginCall();
        call.id = "MC-" + (++sequence);
        call.accountId = accountId;
        call.deficit = deficit;
        call.issuedAt = LocalDateTime.now();
        call.deadlineDate = addBusinessDays(startDate, businessDays);
        call.escalationLevel = 0;
        call.settled = false;
        calls.add(call);
        return call;
    }

    public LocalDate addBusinessDays(LocalDate start, int days) {
        LocalDate d = start;
        int added = 0;
        while (added < days) {
            d = d.plusDays(1);
            DayOfWeek dow = d.getDayOfWeek();
            if (dow != DayOfWeek.SATURDAY && dow != DayOfWeek.SUNDAY) {
                added++;
            }
        }
        return d;
    }

    public List<MarginCall> overdue(LocalDate asOf) {
        List<MarginCall> out = new ArrayList<>();
        for (MarginCall call : calls) {
            if (!call.settled && call.deadlineDate.isBefore(asOf)) {
                out.add(call);
            }
        }
        return out;
    }

    public void escalate(MarginCall call) {
        if (call != null && !call.settled) {
            call.escalationLevel++;
        }
    }

    public boolean settle(String id) {
        for (MarginCall call : calls) {
            if (call.id.equals(id)) {
                call.settled = true;
                return true;
            }
        }
        return false;
    }

    public double openDeficit() {
        double sum = 0;
        for (MarginCall call : calls) {
            if (!call.settled) {
                sum += call.deficit;
            }
        }
        return sum;
    }
}
