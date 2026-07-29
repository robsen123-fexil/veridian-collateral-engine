package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class MarginAttributionEngine {
    public static final class AttributionLine {
        public String desk;
        public String symbol;
        public double marginContribution;
        public double pctOfTotal;
    }

    public List<AttributionLine> attribute(List<MarginAggregator.MarginLeg> legs, double totalMargin) {
        List<AttributionLine> out = new ArrayList<>();
        MarginAggregator agg = new MarginAggregator();
        for (MarginAggregator.MarginLeg leg : legs) {
            List<MarginAggregator.MarginLeg> single = List.of(leg);
            MarginAggregator.MarginResult res = agg.aggregate(single, 0, false);
            AttributionLine line = new AttributionLine();
            line.desk = leg.symbol.contains("-") ? leg.symbol.split("-")[0] : "MAIN";
            line.symbol = leg.symbol;
            line.marginContribution = res.totalRequirement;
            line.pctOfTotal = totalMargin <= 0 ? 0 : line.marginContribution / totalMargin;
            out.add(line);
        }
        return out;
    }

    public double sumByDesk(List<AttributionLine> lines, String desk) {
        double sum = 0;
        for (AttributionLine line : lines) {
            if (desk.equals(line.desk)) {
                sum += line.marginContribution;
            }
        }
        return sum;
    }
}
