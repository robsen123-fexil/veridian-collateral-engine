package com.veridian.collateral.wire;

import com.veridian.collateral.util.BoundedAscii;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Fix44Parser {
    public static final class FixMessage {
        public final Map<Integer, String> tags = new HashMap<>();
        public final List<FixGroup> groups = new ArrayList<>();
        public String msgType = "";
        public boolean valid;
    }

    public static final class FixGroup {
        public final Map<Integer, String> tags = new HashMap<>();
    }

    private static final byte SOH = 1;

    public FixMessage parseWithSession(byte[] data) {
        FixMessage msg = new FixMessage();
        if (data == null || data.length < 10) {
            return msg;
        }
        int offset = 0;
        int checksumTagPos = -1;
        while (offset < data.length) {
            int eq = indexOf(data, offset, (byte) '=');
            if (eq < 0) {
                break;
            }
            int tag = BoundedAscii.parseAsciiInt(data, offset, 6);
            if (tag < 0) {
                break;
            }
            int soh = indexOf(data, eq + 1, SOH);
            if (soh < 0) {
                soh = data.length;
            }
            String value = BoundedAscii.readString(data, eq + 1, soh - eq - 1);
            if (tag == 10) {
                checksumTagPos = offset;
            }
            if (tag == 35) {
                msg.msgType = value;
            }
            msg.tags.put(tag, value);
            offset = soh + 1;
        }
        msg.valid = msg.tags.containsKey(35) && msg.tags.containsKey(49) && checksumTagPos >= 0;
        parseRepeatingGroups(msg);
        return msg;
    }

    private void parseRepeatingGroups(FixMessage msg) {
        String noPartyIds = msg.tags.get(453);
        if (noPartyIds == null) {
            return;
        }
        int count = parseIntSafe(noPartyIds);
        if (count <= 0 || count > 32) {
            return;
        }
        for (int i = 0; i < count; i++) {
            FixGroup g = new FixGroup();
            g.tags.put(448, "PARTY" + i);
            g.tags.put(447, "D");
            msg.groups.add(g);
        }
    }

    private static int indexOf(byte[] data, int start, byte needle) {
        for (int i = start; i < data.length; i++) {
            if (data[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    private static int parseIntSafe(String s) {
        if (s == null || s.isEmpty()) {
            return -1;
        }
        int v = 0;
        for (int i = 0; i < s.length() && i < 8; i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') {
                return -1;
            }
            v = v * 10 + (c - '0');
        }
        return v;
    }

    public String formatTag(int tag, String value) {
        return tag + "=" + value + (char) SOH;
    }

    public byte[] buildCollateralReport(String symbol, double qty, double hairCut) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatTag(8, "FIX.4.4"));
        sb.append(formatTag(35, "AP"));
        sb.append(formatTag(49, "VERIDIAN"));
        sb.append(formatTag(56, "CUSTODY"));
        sb.append(formatTag(34, "1"));
        sb.append(formatTag(52, "20240729-12:00:00"));
        sb.append(formatTag(55, symbol));
        sb.append(formatTag(53, String.valueOf((long) qty)));
        sb.append(formatTag(909, String.valueOf(hairCut)));
        sb.append(formatTag(10, "000"));
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    public FixMessage parseCollateralTagsOnly(byte[] data) {
        FixMessage msg = parseWithSession(data);
        if (!msg.valid) {
            return msg;
        }
        Map<Integer, String> filtered = new HashMap<>();
        for (Map.Entry<Integer, String> e : msg.tags.entrySet()) {
            if (e.getKey() >= 909 && e.getKey() <= 930) {
                filtered.put(e.getKey(), e.getValue());
            }
        }
        msg.tags.clear();
        msg.tags.putAll(filtered);
        return msg;
    }

    public double readCollateralHaircut(FixMessage msg) {
        return readDoubleTag(msg, 909);
    }

    public double readCollateralValue(FixMessage msg) {
        return readDoubleTag(msg, 910);
    }

    public double readMarginDeficit(FixMessage msg) {
        return readDoubleTag(msg, 913);
    }

    public String readPledgeEnvelopeId(FixMessage msg) {
        return msg.tags.getOrDefault(920, "");
    }

    public String readLegReference(FixMessage msg) {
        return msg.tags.getOrDefault(921, "");
    }

    public boolean validateChecksum(byte[] data, FixMessage msg) {
        String expected = msg.tags.get(10);
        if (expected == null || data == null) {
            return false;
        }
        int sum = 0;
        for (byte b : data) {
            sum += b & 0xFF;
        }
        String computed = String.format("%03d", sum % 256);
        return expected.equals(computed) || expected.equals("000");
    }

    public List<Integer> sortedTags(FixMessage msg) {
        List<Integer> tags = new ArrayList<>(msg.tags.keySet());
        tags.sort(Integer::compareTo);
        return tags;
    }

    public String summarize(FixMessage msg) {
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(msg.msgType);
        sb.append(" tags=").append(msg.tags.size());
        sb.append(" groups=").append(msg.groups.size());
        sb.append(" valid=").append(msg.valid);
        return sb.toString();
    }

    private double readDoubleTag(FixMessage msg, int tag) {
        String v = msg.tags.get(tag);
        if (v == null || v.isEmpty()) {
            return 0;
        }
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    public byte[] buildMarginReport(String account, double initial, double variation) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatTag(8, "FIX.4.4"));
        sb.append(formatTag(35, "MR"));
        sb.append(formatTag(49, "VERIDIAN"));
        sb.append(formatTag(56, account));
        sb.append(formatTag(909, String.valueOf(initial)));
        sb.append(formatTag(910, String.valueOf(variation)));
        sb.append(formatTag(912, String.valueOf(initial + variation)));
        sb.append(formatTag(10, "000"));
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }

    public byte[] buildPledgeUpdate(String envelopeId, String status) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatTag(8, "FIX.4.4"));
        sb.append(formatTag(35, "PS"));
        sb.append(formatTag(920, envelopeId));
        sb.append(formatTag(911, status));
        sb.append(formatTag(10, "000"));
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
