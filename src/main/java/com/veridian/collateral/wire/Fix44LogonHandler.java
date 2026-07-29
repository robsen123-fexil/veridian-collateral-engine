package com.veridian.collateral.wire;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Fix44LogonHandler {
    public static final class LogonRequest {
        public String senderCompId;
        public String targetCompId;
        public int heartBtInt;
        public boolean resetSeqNum;
        public String username;
        public String password;
    }

    public static final class LogonResponse {
        public boolean accepted;
        public String rejectReason;
        public byte[] encoded;
    }

    public LogonResponse handle(byte[] data) {
        LogonResponse response = new LogonResponse();
        Fix44Parser parser = new Fix44Parser();
        Fix44Parser.FixMessage msg = parser.parseWithSession(data);
        if (!msg.valid || !"A".equals(msg.msgType)) {
            response.accepted = false;
            response.rejectReason = "invalid_logon";
            return response;
        }
        LogonRequest req = new LogonRequest();
        req.senderCompId = msg.tags.getOrDefault(49, "");
        req.targetCompId = msg.tags.getOrDefault(56, "");
        req.username = msg.tags.getOrDefault(553, "");
        req.password = msg.tags.getOrDefault(554, "");
        req.resetSeqNum = "Y".equals(msg.tags.get(141));
        try {
            req.heartBtInt = Integer.parseInt(msg.tags.getOrDefault(108, "30"));
        } catch (NumberFormatException ex) {
            req.heartBtInt = 30;
        }
        if (req.senderCompId.isEmpty() || req.targetCompId.isEmpty()) {
            response.accepted = false;
            response.rejectReason = "missing_comp_ids";
            return response;
        }
        if (req.heartBtInt <= 0 || req.heartBtInt > 3600) {
            response.accepted = false;
            response.rejectReason = "bad_heartbt";
            return response;
        }
        response.accepted = true;
        response.encoded = buildAccept(req);
        return response;
    }

    private byte[] buildAccept(LogonRequest req) {
        List<String> fields = new ArrayList<>();
        fields.add("8=FIX.4.4");
        fields.add("35=A");
        fields.add("49=" + req.targetCompId);
        fields.add("56=" + req.senderCompId);
        fields.add("34=1");
        fields.add("52=20260729-12:00:00");
        fields.add("98=0");
        fields.add("108=" + req.heartBtInt);
        if (req.resetSeqNum) {
            fields.add("141=Y");
        }
        fields.add("10=000");
        StringBuilder sb = new StringBuilder();
        for (String f : fields) {
            sb.append(f).append((char) 1);
        }
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
