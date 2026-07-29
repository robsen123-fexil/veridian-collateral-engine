package com.veridian.collateral.desk;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class CollateralScheduleEngine {
    public static final class ScheduledEvent {
        public String id;
        public LocalDate date;
        public String type;
        public String accountId;
        public double amount;
        public boolean completed;
    }

    private final List<ScheduledEvent> events = new ArrayList<>();
    private long seq;

    public ScheduledEvent schedule(LocalDate date, String type, String accountId, double amount) {
        ScheduledEvent ev = new ScheduledEvent();
        ev.id = "SCH-" + (++seq);
        ev.date = date;
        ev.type = type;
        ev.accountId = accountId;
        ev.amount = amount;
        events.add(ev);
        return ev;
    }

    public List<ScheduledEvent> dueOn(LocalDate date) {
        List<ScheduledEvent> out = new ArrayList<>();
        for (ScheduledEvent ev : events) {
            if (!ev.completed && ev.date.equals(date)) {
                out.add(ev);
            }
        }
        return out;
    }

    public boolean complete(String id) {
        for (ScheduledEvent ev : events) {
            if (ev.id.equals(id)) {
                ev.completed = true;
                return true;
            }
        }
        return false;
    }
}
