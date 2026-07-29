package com.veridian.collateral.desk;

import java.util.HashMap;
import java.util.Map;

public final class TriPartyInstructionRouter {
    public static final class RouteDecision {
        public String instructionId;
        public String selectedAgent;
        public String reason;
        public int priority;
    }

    private final Map<String, Integer> agentPriority = new HashMap<>();
    private final LiquidityRouteTable liquidity = new LiquidityRouteTable();

    public TriPartyInstructionRouter() {
        agentPriority.put("BNYM", 3);
        agentPriority.put("JPM", 2);
        agentPriority.put("STATESTREET", 1);
    }

    public RouteDecision route(String instructionId, String accountId, double notional, String preferredAgent) {
        RouteDecision decision = new RouteDecision();
        decision.instructionId = instructionId;
        if (preferredAgent != null && agentPriority.containsKey(preferredAgent)) {
            decision.selectedAgent = preferredAgent;
            decision.reason = "preferred";
            decision.priority = agentPriority.get(preferredAgent);
            return decision;
        }
        String venue = liquidity.route(notional, notional > 5_000_000 ? 3 : 1);
        decision.selectedAgent = mapVenueToAgent(venue);
        decision.reason = "liquidity:" + venue;
        decision.priority = agentPriority.getOrDefault(decision.selectedAgent, 0);
        return decision;
    }

    private String mapVenueToAgent(String venue) {
        return switch (venue) {
            case "FEDWIRE" -> "BNYM";
            case "CHIPS" -> "JPM";
            case "INTERNAL" -> "STATESTREET";
            default -> "BNYM";
        };
    }
}
