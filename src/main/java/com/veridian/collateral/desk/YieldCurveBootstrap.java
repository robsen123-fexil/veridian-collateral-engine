package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.List;

public final class YieldCurveBootstrap {
    public static final class CurvePoint {
        public int tenorYears;
        public double zeroRate;
        public double discountFactor;
    }

    public List<CurvePoint> bootstrapFromPar(List<Double> parYields) {
        List<CurvePoint> curve = new ArrayList<>();
        for (int i = 0; i < parYields.size(); i++) {
            CurvePoint point = new CurvePoint();
            point.tenorYears = i + 1;
            point.zeroRate = parYields.get(i);
            point.discountFactor = Math.exp(-point.zeroRate * point.tenorYears);
            curve.add(point);
        }
        return curve;
    }

    public double interpolateZeroRate(List<CurvePoint> curve, double tenor) {
        if (curve.isEmpty()) {
            return 0;
        }
        if (tenor <= curve.get(0).tenorYears) {
            return curve.get(0).zeroRate;
        }
        for (int i = 1; i < curve.size(); i++) {
            CurvePoint prev = curve.get(i - 1);
            CurvePoint next = curve.get(i);
            if (tenor <= next.tenorYears) {
                double w = (tenor - prev.tenorYears) / (next.tenorYears - prev.tenorYears);
                return prev.zeroRate + w * (next.zeroRate - prev.zeroRate);
            }
        }
        return curve.get(curve.size() - 1).zeroRate;
    }

    public double discount(List<CurvePoint> curve, double tenor) {
        double r = interpolateZeroRate(curve, tenor);
        return Math.exp(-r * tenor);
    }
}
