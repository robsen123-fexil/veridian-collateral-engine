package com.veridian.collateral.desk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Iso8583FieldMap {
    public static final class FieldSpec {
        public final int field;
        public final String name;
        public final int maxLength;
        public final char format;

        public FieldSpec(int field, String name, int maxLength, char format) {
            this.field = field;
            this.name = name;
            this.maxLength = maxLength;
            this.format = format;
        }
    }

    private final Map<Integer, FieldSpec> fields = new HashMap<>();

    public Iso8583FieldMap() {
        registerCoreFields();
        registerAmountFields();
        registerTrackFields();
        registerPrivateFields();
    }

    private void reg(int f, String name, int len, char fmt) {
        fields.put(f, new FieldSpec(f, name, len, fmt));
    }

    private void registerCoreFields() {
        reg(2, "PrimaryAccountNumber", 19, 'N');
        reg(3, "ProcessingCode", 6, 'N');
        reg(4, "AmountTransaction", 12, 'N');
        reg(7, "TransmissionDateTime", 10, 'N');
        reg(11, "SystemTraceAuditNumber", 6, 'N');
        reg(12, "TimeLocalTransaction", 6, 'N');
        reg(13, "DateLocalTransaction", 4, 'N');
        reg(14, "DateExpiration", 4, 'N');
        reg(18, "MerchantType", 4, 'N');
        reg(22, "PointOfServiceEntryMode", 3, 'N');
        reg(25, "PointOfServiceConditionCode", 2, 'N');
        reg(32, "AcquiringInstitutionId", 11, 'N');
        reg(37, "RetrievalReferenceNumber", 12, 'A');
        reg(38, "AuthorizationIdentification", 6, 'A');
        reg(39, "ResponseCode", 2, 'A');
        reg(41, "CardAcceptorTerminalId", 8, 'A');
        reg(42, "CardAcceptorId", 15, 'A');
        reg(43, "CardAcceptorNameLocation", 40, 'A');
        reg(49, "CurrencyCodeTransaction", 3, 'A');
    }

    private void registerAmountFields() {
        reg(5, "AmountSettlement", 12, 'N');
        reg(6, "AmountCardholderBilling", 12, 'N');
        reg(8, "AmountCardholderBillingFee", 8, 'N');
        reg(9, "ConversionRateSettlement", 8, 'N');
        reg(10, "ConversionRateCardholderBilling", 8, 'N');
        reg(28, "AmountTransactionFee", 9, 'A');
        reg(30, "AmountTransactionProcessingFee", 9, 'A');
        reg(46, "AmountFees", 204, 'A');
        reg(54, "AdditionalAmounts", 120, 'A');
    }

    private void registerTrackFields() {
        reg(35, "Track2Data", 37, 'Z');
        reg(45, "Track1Data", 76, 'Z');
        reg(36, "Track3Data", 104, 'Z');
        reg(23, "CardSequenceNumber", 3, 'N');
        reg(26, "PointOfServicePinCapture", 2, 'N');
        reg(52, "PinData", 16, 'B');
        reg(53, "SecurityRelatedControlInfo", 16, 'N');
    }

    private void registerPrivateFields() {
        reg(48, "AdditionalDataPrivate", 999, 'A');
        reg(60, "ReservedNational", 999, 'A');
        reg(61, "ReservedPrivate", 999, 'A');
        reg(62, "ReservedPrivateUse", 999, 'A');
        reg(63, "ReservedPrivateUse2", 999, 'A');
        reg(70, "NetworkManagementInfoCode", 3, 'N');
        reg(90, "OriginalDataElements", 42, 'N');
        reg(95, "ReplacementAmounts", 42, 'A');
        reg(100, "ReceivingInstitutionId", 11, 'N');
        reg(102, "AccountIdentification1", 28, 'A');
        reg(103, "AccountIdentification2", 28, 'A');
    }

    public FieldSpec lookup(int field) {
        return fields.get(field);
    }

    public List<FieldSpec> all() {
        return new ArrayList<>(fields.values());
    }

    public boolean validateField(int field, String value) {
        FieldSpec spec = lookup(field);
        if (spec == null || value == null) {
            return false;
        }
        if (value.length() > spec.maxLength) {
            return false;
        }
        if (spec.format == 'N') {
            for (int i = 0; i < value.length(); i++) {
                if (!Character.isDigit(value.charAt(i))) {
                    return false;
                }
            }
        }
        return true;
    }

    public Map<Integer, String> parseBitmapMessage(byte[] data) {
        Map<Integer, String> out = new HashMap<>();
        if (data == null || data.length < 4) {
            return out;
        }
        int offset = 0;
        int mti = readAsciiInt(data, offset, 4);
        offset += 4;
        if (mti <= 0) {
            return out;
        }
        long bitmap = 0;
        for (int i = 0; i < 8 && offset < data.length; i++) {
            bitmap = (bitmap << 8) | (data[offset++] & 0xFF);
        }
        for (int bit = 1; bit <= 64; bit++) {
            if (((bitmap >> (64 - bit)) & 1) == 1) {
                FieldSpec spec = lookup(bit);
                int len = spec != null ? Math.min(spec.maxLength, 32) : 16;
                if (offset + len > data.length) {
                    break;
                }
                out.put(bit, new String(data, offset, len));
                offset += len;
            }
        }
        return out;
    }

    private int readAsciiInt(byte[] data, int offset, int digits) {
        int v = 0;
        for (int i = 0; i < digits && offset + i < data.length; i++) {
            byte b = data[offset + i];
            if (b < '0' || b > '9') {
                return -1;
            }
            v = v * 10 + (b - '0');
        }
        return v;
    }
}
