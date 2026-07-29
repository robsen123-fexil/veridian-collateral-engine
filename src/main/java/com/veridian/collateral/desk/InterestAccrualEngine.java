package com.veridian.collateral.desk;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class InterestAccrualEngine {
    public enum DayCount { ACT_360, ACT_365, THIRTY_360 }

    public static final class AccrualSchedule {
        public String legId;
        public LocalDate start;
        public LocalDate end;
        public double notional;
        public double rateBps;
        public DayCount convention;
        public final List<AccrualPeriod> periods = new ArrayList<>();
    }

    public static final class AccrualPeriod {
        public LocalDate periodStart;
        public LocalDate periodEnd;
        public int days;
        public double accrual;
    }

    public AccrualSchedule buildSchedule(String legId, LocalDate start, LocalDate end, double notional,
                                         double rateBps, DayCount convention, int paymentFrequencyMonths) {
        AccrualSchedule schedule = new AccrualSchedule();
        schedule.legId = legId;
        schedule.start = start;
        schedule.end = end;
        schedule.notional = notional;
        schedule.rateBps = rateBps;
        schedule.convention = convention;

        LocalDate cursor = start;
        while (cursor.isBefore(end)) {
            LocalDate periodEnd = cursor.plusMonths(paymentFrequencyMonths);
            if (periodEnd.isAfter(end)) {
                periodEnd = end;
            }
            AccrualPeriod period = new AccrualPeriod();
            period.periodStart = cursor;
            period.periodEnd = periodEnd;
            period.days = dayCount(cursor, periodEnd, convention);
            period.accrual = accrue(notional, rateBps, period.days, convention);
            schedule.periods.add(period);
            cursor = periodEnd;
        }
        return schedule;
    }

    public double totalAccrued(AccrualSchedule schedule) {
        double sum = 0;
        for (AccrualPeriod p : schedule.periods) {
            sum += p.accrual;
        }
        return sum;
    }

    public double accrueToDate(AccrualSchedule schedule, LocalDate asOf) {
        double sum = 0;
        for (AccrualPeriod p : schedule.periods) {
            if (p.periodEnd.isAfter(asOf)) {
                int partialDays = dayCount(p.periodStart, asOf, schedule.convention);
                sum += accrue(schedule.notional, schedule.rateBps, partialDays, schedule.convention);
                break;
            }
            sum += p.accrual;
        }
        return sum;
    }

    public double accrue(double notional, double rateBps, int days, DayCount convention) {
        double yearBasis = switch (convention) {
            case ACT_360, THIRTY_360 -> 360.0;
            case ACT_365 -> 365.0;
        };
        return notional * (rateBps / 10_000.0) * (days / yearBasis);
    }

    public int dayCount(LocalDate start, LocalDate end, DayCount convention) {
        if (convention == DayCount.THIRTY_360) {
            return thirty360(start, end);
        }
        return (int) ChronoUnit.DAYS.between(start, end);
    }

    private int thirty360(LocalDate start, LocalDate end) {
        int d1 = Math.min(30, start.getDayOfMonth());
        int d2 = end.getDayOfMonth();
        if (d2 == 31 && d1 >= 30) {
            d2 = 30;
        }
        int months = (end.getYear() - start.getYear()) * 12 + (end.getMonthValue() - start.getMonthValue());
        return months * 30 + (d2 - d1);
    }

    public List<AccrualPeriod> overduePeriods(AccrualSchedule schedule, LocalDate asOf) {
        List<AccrualPeriod> out = new ArrayList<>();
        for (AccrualPeriod p : schedule.periods) {
            if (p.periodEnd.isBefore(asOf) && p.accrual > 0) {
                out.add(p);
            }
        }
        return out;
    }
}
