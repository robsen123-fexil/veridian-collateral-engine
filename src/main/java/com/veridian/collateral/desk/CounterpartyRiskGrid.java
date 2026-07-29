package com.veridian.collateral.desk;

import java.util.HashMap;
import java.util.Map;

public final class CounterpartyRiskGrid {
    public static final class CounterpartyProfile {
        public String id;
        public String name;
        public double creditLimit;
        public double currentExposure;
        public int internalRating;
        public boolean sanctioned;
    }

    private final Map<String, CounterpartyProfile> profiles = new HashMap<>();

    public void upsert(String id, String name, double limit, int rating) {
        CounterpartyProfile p = profiles.computeIfAbsent(id, k -> new CounterpartyProfile());
        p.id = id;
        p.name = name;
        p.creditLimit = limit;
        p.internalRating = rating;
        p.sanctioned = rating <= 0;
    }

    public void addExposure(String id, double delta) {
        CounterpartyProfile p = profiles.get(id);
        if (p != null) {
            p.currentExposure += delta;
        }
    }

    public boolean isBreached(String id) {
        CounterpartyProfile p = profiles.get(id);
        if (p == null || p.sanctioned) {
            return true;
        }
        return p.currentExposure > p.creditLimit;
    }

    public double headroom(String id) {
        CounterpartyProfile p = profiles.get(id);
        if (p == null) {
            return 0;
        }
        return Math.max(0, p.creditLimit - p.currentExposure);
    }

    public int countBreached() {
        int n = 0;
        for (CounterpartyProfile p : profiles.values()) {
            if (isBreached(p.id)) {
                n++;
            }
        }
        return n;
    }
}
