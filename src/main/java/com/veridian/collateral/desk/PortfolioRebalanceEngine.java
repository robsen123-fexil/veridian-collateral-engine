package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PortfolioRebalanceEngine {
    public static final class TargetWeight {
        public String symbol;
        public double targetWeight;
    }

    public static final class CurrentHolding {
        public String symbol;
        public double marketValue;
    }

    public static final class RebalanceOrder {
        public String symbol;
        public double currentValue;
        public double targetValue;
        public double delta;
        public String side;
    }

    public List<RebalanceOrder> computeOrders(double portfolioValue, List<CurrentHolding> holdings,
                                              List<TargetWeight> targets, double minTradeSize) {
        Map<String, Double> current = new HashMap<>();
        for (CurrentHolding h : holdings) {
            current.merge(h.symbol, h.marketValue, Double::sum);
        }
        double weightSum = 0;
        for (TargetWeight t : targets) {
            weightSum += Math.max(0, t.targetWeight);
        }
        List<RebalanceOrder> orders = new ArrayList<>();
        for (TargetWeight t : targets) {
            double normalized = weightSum > 0 ? t.targetWeight / weightSum : 0;
            double targetValue = portfolioValue * normalized;
            double cur = current.getOrDefault(t.symbol, 0.0);
            RebalanceOrder order = new RebalanceOrder();
            order.symbol = t.symbol;
            order.currentValue = cur;
            order.targetValue = targetValue;
            order.delta = targetValue - cur;
            if (Math.abs(order.delta) < minTradeSize) {
                continue;
            }
            order.side = order.delta > 0 ? "BUY" : "SELL";
            orders.add(order);
        }
        return orders;
    }

    public double trackingError(List<CurrentHolding> holdings, List<TargetWeight> targets, double portfolioValue) {
        Map<String, Double> currentWeights = new HashMap<>();
        for (CurrentHolding h : holdings) {
            if (portfolioValue > 0) {
                currentWeights.merge(h.symbol, h.marketValue / portfolioValue, Double::sum);
            }
        }
        double err = 0;
        for (TargetWeight t : targets) {
            double cur = currentWeights.getOrDefault(t.symbol, 0.0);
            err += Math.pow(cur - t.targetWeight, 2);
        }
        return Math.sqrt(err);
    }

    public List<RebalanceOrder> scaleOrders(List<RebalanceOrder> orders, double scaleFactor) {
        List<RebalanceOrder> scaled = new ArrayList<>();
        for (RebalanceOrder o : orders) {
            RebalanceOrder copy = new RebalanceOrder();
            copy.symbol = o.symbol;
            copy.currentValue = o.currentValue;
            copy.targetValue = o.currentValue + (o.delta * scaleFactor);
            copy.delta = o.delta * scaleFactor;
            copy.side = copy.delta > 0 ? "BUY" : "SELL";
            scaled.add(copy);
        }
        return scaled;
    }

    public double grossTradeNotional(List<RebalanceOrder> orders) {
        double sum = 0;
        for (RebalanceOrder o : orders) {
            sum += Math.abs(o.delta);
        }
        return sum;
    }
}
