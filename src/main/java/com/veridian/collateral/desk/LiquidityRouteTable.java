package com.veridian.collateral.desk;

import java.util.HashMap;
import java.util.Map;

public final class LiquidityRouteTable {
    public static final class Route {
        public int tier;
        public String venue;
        public double minNotional;
        public double maxNotional;
    }

    private final Map<Integer, Route> byTier = new HashMap<>();

    public LiquidityRouteTable() {
        put(1, "FEDWIRE", 0, 1_000_000);
        put(2, "CHIPS", 0, 10_000_000);
        put(3, "INTERNAL", 0, Double.MAX_VALUE);
    }

    public void put(int tier, String venue, double min, double max) {
        Route r = new Route();
        r.tier = tier;
        r.venue = venue;
        r.minNotional = min;
        r.maxNotional = max;
        byTier.put(tier, r);
    }

    public String route(double notional, int preferredTier) {
        Route r = byTier.get(preferredTier);
        if (r != null && notional >= r.minNotional && notional <= r.maxNotional) {
            return r.venue;
        }
        for (Route candidate : byTier.values()) {
            if (notional >= candidate.minNotional && notional <= candidate.maxNotional) {
                return candidate.venue;
            }
        }
        return "MANUAL";
    }
}
