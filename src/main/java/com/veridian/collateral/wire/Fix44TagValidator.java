package com.veridian.collateral.wire;

import com.veridian.collateral.desk.FixTagDictionary;

import java.util.HashMap;
import java.util.Map;

public final class Fix44TagValidator {
    public static final class ValidationResult {
        public boolean ok;
        public String field;
        public String message;
    }

    private final FixTagDictionary dictionary = new FixTagDictionary();
    private final Map<Integer, TagRule> rules = new HashMap<>();

    public Fix44TagValidator() {
        registerDefaults();
    }

    private interface TagRule {
        ValidationResult validate(String value);
    }

    private void register(int tag, TagRule rule) {
        rules.put(tag, rule);
    }

    private void registerDefaults() {
        register(8, v -> required(v, "BeginString"));
        register(35, v -> required(v, "MsgType"));
        register(49, v -> required(v, "SenderCompID"));
        register(56, v -> required(v, "TargetCompID"));
        register(34, v -> positiveInt(v, "MsgSeqNum"));
        register(52, v -> required(v, "SendingTime"));
        register(10, v -> checksum(v));
        register(55, v -> symbol(v));
        register(53, v -> positiveQty(v));
        register(909, v -> pct(v, "CollateralHaircut"));
        register(910, v -> amount(v, "CollateralValue"));
        register(911, v -> enumStatus(v));
        register(912, v -> amount(v, "MarginExcess"));
        register(913, v -> amount(v, "MarginDeficit"));
        register(914, v -> required(v, "VaultLocation"));
        register(916, v -> assetClass(v));
        register(917, v -> pct(v, "ConcentrationLimit"));
        register(919, v -> positiveInt(v, "SettlementCycle"));
        register(920, v -> required(v, "PledgeEnvelopeId"));
        register(921, v -> required(v, "LegReference"));
        register(922, v -> signedDecimal(v, "BasisAdjustment"));
        register(923, v -> tier(v));
        register(924, v -> required(v, "NostroAccount"));
        register(925, v -> amount(v, "SweepThreshold"));
        register(926, v -> required(v, "ComplianceRuleId"));
        register(927, v -> positiveInt(v, "AuditSequence"));
        register(928, v -> hexHash(v));
        register(929, v -> timestamp(v));
        register(930, v -> required(v, "SwapConfirmId"));
    }

    public ValidationResult validateTag(int tag, String value) {
        TagRule rule = rules.get(tag);
        if (rule == null) {
            ValidationResult r = new ValidationResult();
            r.ok = dictionary.lookupByTag(tag) != null;
            r.field = "tag:" + tag;
            r.message = r.ok ? "ok" : "unknown tag";
            return r;
        }
        return rule.validate(value);
    }

    public Map<Integer, ValidationResult> validateAll(Map<Integer, String> tags) {
        Map<Integer, ValidationResult> out = new HashMap<>();
        for (Map.Entry<Integer, String> e : tags.entrySet()) {
            out.put(e.getKey(), validateTag(e.getKey(), e.getValue()));
        }
        return out;
    }

    public boolean allOk(Map<Integer, ValidationResult> results) {
        for (ValidationResult r : results.values()) {
            if (!r.ok) {
                return false;
            }
        }
        return true;
    }

    private static ValidationResult ok(String field) {
        ValidationResult r = new ValidationResult();
        r.ok = true;
        r.field = field;
        r.message = "ok";
        return r;
    }

    private static ValidationResult fail(String field, String msg) {
        ValidationResult r = new ValidationResult();
        r.ok = false;
        r.field = field;
        r.message = msg;
        return r;
    }

    private static TagRule required(String field) {
        return v -> v != null && !v.isEmpty() ? ok(field) : fail(field, "required");
    }

    private static ValidationResult required(String v, String field) {
        return v != null && !v.isEmpty() ? ok(field) : fail(field, "required");
    }

    private static TagRule positiveInt(String field) {
        return v -> {
            try {
                return Integer.parseInt(v) > 0 ? ok(field) : fail(field, "must be >0");
            } catch (NumberFormatException ex) {
                return fail(field, "not int");
            }
        };
    }

    private static ValidationResult positiveInt(String v, String field) {
        try {
            return Integer.parseInt(v) > 0 ? ok(field) : fail(field, "must be >0");
        } catch (NumberFormatException ex) {
            return fail(field, "not int");
        }
    }

    private static TagRule checksum(String field) {
        return v -> v != null && v.length() == 3 ? ok(field) : fail(field, "bad checksum");
    }

    private static ValidationResult checksum(String v) {
        return v != null && v.length() == 3 ? ok("CheckSum") : fail("CheckSum", "bad checksum");
    }

    private static TagRule symbol(String field) {
        return v -> v != null && v.length() <= 32 ? ok(field) : fail(field, "symbol too long");
    }

    private static ValidationResult symbol(String v) {
        return v != null && v.length() <= 32 ? ok("Symbol") : fail("Symbol", "symbol too long");
    }

    private static TagRule positiveQty(String field) {
        return v -> {
            try {
                return Long.parseLong(v) >= 0 ? ok(field) : fail(field, "negative qty");
            } catch (NumberFormatException ex) {
                return fail(field, "not qty");
            }
        };
    }

    private static ValidationResult positiveQty(String v) {
        try {
            return Long.parseLong(v) >= 0 ? ok("Quantity") : fail("Quantity", "negative qty");
        } catch (NumberFormatException ex) {
            return fail("Quantity", "not qty");
        }
    }

    private static TagRule pct(String field) {
        return v -> parseDouble(v, 0, 1, field);
    }

    private static ValidationResult pct(String v, String field) {
        return parseDouble(v, 0, 1, field);
    }

    private static TagRule amount(String field) {
        return v -> parseDouble(v, 0, Double.MAX_VALUE, field);
    }

    private static ValidationResult amount(String v, String field) {
        return parseDouble(v, 0, Double.MAX_VALUE, field);
    }

    private static TagRule signedDecimal(String field) {
        return v -> parseDouble(v, -Double.MAX_VALUE, Double.MAX_VALUE, field);
    }

    private static ValidationResult signedDecimal(String v, String field) {
        return parseDouble(v, -Double.MAX_VALUE, Double.MAX_VALUE, field);
    }

    private static ValidationResult parseDouble(String v, double min, double max, String field) {
        try {
            double d = Double.parseDouble(v);
            if (d < min || d > max) {
                return fail(field, "out of range");
            }
            return ok(field);
        } catch (NumberFormatException ex) {
            return fail(field, "not decimal");
        }
    }

    private static TagRule enumStatus(String field) {
        return v -> {
            if ("Open".equals(v) || "Released".equals(v) || "Disputed".equals(v)) {
                return ok(field);
            }
            return fail(field, "bad status");
        };
    }

    private static ValidationResult enumStatus(String v) {
        if ("Open".equals(v) || "Released".equals(v) || "Disputed".equals(v)) {
            return ok("PledgeStatus");
        }
        return fail("PledgeStatus", "bad status");
    }

    private static TagRule assetClass(String field) {
        return v -> v != null && !v.isEmpty() ? ok(field) : fail(field, "asset class required");
    }

    private static ValidationResult assetClass(String v) {
        return v != null && !v.isEmpty() ? ok("EligibleAssetClass") : fail("EligibleAssetClass", "required");
    }

    private static TagRule tier(String field) {
        return v -> {
            try {
                int t = Integer.parseInt(v);
                return t >= 1 && t <= 3 ? ok(field) : fail(field, "tier 1-3");
            } catch (NumberFormatException ex) {
                return fail(field, "not int");
            }
        };
    }

    private static ValidationResult tier(String v) {
        try {
            int t = Integer.parseInt(v);
            return t >= 1 && t <= 3 ? ok("LiquidityTier") : fail("LiquidityTier", "tier 1-3");
        } catch (NumberFormatException ex) {
            return fail("LiquidityTier", "not int");
        }
    }

    private static TagRule hexHash(String field) {
        return v -> v != null && v.matches("[0-9A-Fa-f]+") ? ok(field) : fail(field, "hex required");
    }

    private static ValidationResult hexHash(String v) {
        return v != null && v.matches("[0-9A-Fa-f]+") ? ok("CheckpointHash") : fail("CheckpointHash", "hex required");
    }

    private static TagRule timestamp(String field) {
        return v -> v != null && v.length() >= 8 ? ok(field) : fail(field, "bad timestamp");
    }

    private static ValidationResult timestamp(String v) {
        return v != null && v.length() >= 8 ? ok("MarginCallDeadline") : fail("MarginCallDeadline", "bad timestamp");
    }
}
