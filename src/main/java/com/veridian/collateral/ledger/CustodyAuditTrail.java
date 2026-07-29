package com.veridian.collateral.ledger;

import java.util.ArrayList;
import java.util.List;

public final class CustodyAuditTrail {
    public static final class TrailEvent {
        public long sequence;
        public String actor;
        public String action;
        public String subject;
        public String detail;
        public long timestamp;
    }

    private final List<TrailEvent> events = new ArrayList<>();
    private long nextSeq = 1;

    public long record(String actor, String action, String subject, String detail) {
        TrailEvent ev = new TrailEvent();
        ev.sequence = nextSeq++;
        ev.actor = actor;
        ev.action = action;
        ev.subject = subject;
        ev.detail = detail;
        ev.timestamp = System.currentTimeMillis();
        events.add(ev);
        return ev.sequence;
    }

    public List<TrailEvent> queryBySubject(String subject, int limit) {
        List<TrailEvent> out = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0 && out.size() < limit; i--) {
            TrailEvent ev = events.get(i);
            if (subject.equals(ev.subject)) {
                out.add(ev);
            }
        }
        return out;
    }

    public List<TrailEvent> queryByAction(String action, int limit) {
        List<TrailEvent> out = new ArrayList<>();
        for (int i = events.size() - 1; i >= 0 && out.size() < limit; i--) {
            TrailEvent ev = events.get(i);
            if (action.equals(ev.action)) {
                out.add(ev);
            }
        }
        return out;
    }

    public int size() { return events.size(); }

    public TrailEvent latest() {
        if (events.isEmpty()) {
            return null;
        }
        return events.get(events.size() - 1);
    }
}
