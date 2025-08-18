package com.veridian.collateral.wire;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Fix44ExtendedCodec {
    private static final char SOH = 1;
    private final Fix44Parser base = new Fix44Parser();

    public Fix44Parser.FixMessage decode(byte[] data) {
        return base.parseWithSession(data);
    }

    public byte[] buildHeader(String msgType, int seq) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, msgType));
        fields.add(field(49, "VERIDIAN"));
        fields.add(field(56, "CUSTODY"));
        fields.add(field(34, String.valueOf(seq)));
        fields.add(field(52, "20240729-12:00:00"));
        return join(fields);
    }

    public byte[] buildCollateralAssignment(String account, String symbol, double qty, double haircut) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "CV"));
        fields.add(field(49, "VERIDIAN"));
        fields.add(field(56, account));
        fields.add(field(55, symbol));
        fields.add(field(53, formatQty(qty)));
        fields.add(field(909, formatHaircut(haircut)));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildMarginCallRequest(String account, double deficit, String deadline) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "MC"));
        fields.add(field(49, "VERIDIAN"));
        fields.add(field(56, account));
        fields.add(field(913, formatAmt(deficit)));
        fields.add(field(929, deadline));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildAllocationReport(String account, String symbol, long qty) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "AP"));
        fields.add(field(49, "VERIDIAN"));
        fields.add(field(56, account));
        fields.add(field(55, symbol));
        fields.add(field(53, String.valueOf(qty)));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildPledgeStatus(String envelopeId, String status) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "PS"));
        fields.add(field(920, envelopeId));
        fields.add(field(911, status));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildVaultLocationUpdate(String vault, String account) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "VL"));
        fields.add(field(914, vault));
        fields.add(field(1, account));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildBasisAdjustment(String legRef, double bps) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "BA"));
        fields.add(field(921, legRef));
        fields.add(field(922, String.valueOf(bps)));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildLiquidityTierUpdate(String account, int tier) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "LT"));
        fields.add(field(1, account));
        fields.add(field(923, String.valueOf(tier)));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildNostroSweepNotice(String nostro, double threshold) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "NS"));
        fields.add(field(924, nostro));
        fields.add(field(925, formatAmt(threshold)));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildComplianceAlert(String ruleId, String account) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "CA"));
        fields.add(field(926, ruleId));
        fields.add(field(1, account));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildCheckpointHashReport(String hash, int sequence) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "CH"));
        fields.add(field(928, hash));
        fields.add(field(927, String.valueOf(sequence)));
        fields.add(field(10, "000"));
        return join(fields);
    }

    public byte[] buildSwapConfirm(String confirmId, String payLeg, String recvLeg) {
        List<String> fields = new ArrayList<>();
        fields.add(field(8, "FIX.4.4"));
        fields.add(field(35, "SC"));
        fields.add(field(930, confirmId));
        fields.add(field(921, payLeg));
        fields.add(field(922, recvLeg));
        fields.add(field(10, "000"));
        return join(fields);
    }

    private String field(int tag, String value) {
        return tag + "=" + value;
    }

    private byte[] join(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (String f : fields) {
            sb.append(f).append(SOH);
        }
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String formatQty(double qty) {
        return String.valueOf((long) qty);
    }

    private String formatHaircut(double h) {
        return String.format("%.4f", h);
    }

    private String formatAmt(double amt) {
        return String.format("%.2f", amt);
    }
}
