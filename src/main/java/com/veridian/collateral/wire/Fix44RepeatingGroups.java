package com.veridian.collateral.wire;

import com.veridian.collateral.util.BoundedAscii;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class Fix44RepeatingGroups {
    public static final class GroupInstance {
        public final List<TagValue> tags = new ArrayList<>();
    }

    public static final class TagValue {
        public int tag;
        public String value;
    }

    public static final class ParsedGroups {
        public final List<GroupInstance> partyGroups = new ArrayList<>();
        public final List<GroupInstance> legGroups = new ArrayList<>();
        public final List<GroupInstance> collateralGroups = new ArrayList<>();
    }

    public ParsedGroups parseCollateralGroups(byte[] data) {
        ParsedGroups groups = new ParsedGroups();
        if (data == null) {
            return groups;
        }
        List<TagValue> flat = tokenize(data);
        walkPartyGroups(flat, groups);
        walkLegGroups(flat, groups);
        walkCollateralGroups(flat, groups);
        return groups;
    }

    private List<TagValue> tokenize(byte[] data) {
        List<TagValue> out = new ArrayList<>();
        int offset = 0;
        while (offset < data.length) {
            int eq = indexOf(data, offset, (byte) '=');
            if (eq < 0) {
                break;
            }
            int tag = BoundedAscii.parseAsciiInt(data, offset, 6);
            if (tag < 0) {
                break;
            }
            int soh = indexOf(data, eq + 1, (byte) 1);
            if (soh < 0) {
                soh = data.length;
            }
            TagValue tv = new TagValue();
            tv.tag = tag;
            tv.value = BoundedAscii.readString(data, eq + 1, soh - eq - 1);
            out.add(tv);
            offset = soh + 1;
        }
        return out;
    }

    private void walkPartyGroups(List<TagValue> flat, ParsedGroups groups) {
        int count = readCount(flat, 453);
        if (count <= 0) {
            return;
        }
        GroupInstance current = null;
        int seen = 0;
        for (TagValue tv : flat) {
            if (tv.tag == 453) {
                continue;
            }
            if (tv.tag == 448 || tv.tag == 447 || tv.tag == 452) {
                if (current == null || current.tags.isEmpty() || tv.tag == 448) {
                    if (current != null) {
                        groups.partyGroups.add(current);
                        seen++;
                    }
                    if (seen >= count) {
                        break;
                    }
                    current = new GroupInstance();
                }
                current.tags.add(tv);
            }
        }
        if (current != null && seen < count) {
            groups.partyGroups.add(current);
        }
    }

    private void walkLegGroups(List<TagValue> flat, ParsedGroups groups) {
        int count = readCount(flat, 555);
        if (count <= 0) {
            return;
        }
        GroupInstance current = null;
        int seen = 0;
        for (TagValue tv : flat) {
            if (tv.tag == 555) {
                continue;
            }
            if (tv.tag == 600 || tv.tag == 602 || tv.tag == 603 || tv.tag == 921) {
                if (current == null || (tv.tag == 600 && !current.tags.isEmpty())) {
                    if (current != null) {
                        groups.legGroups.add(current);
                        seen++;
                    }
                    if (seen >= count) {
                        break;
                    }
                    current = new GroupInstance();
                }
                if (current != null) {
                    current.tags.add(tv);
                }
            }
        }
        if (current != null && seen < count) {
            groups.legGroups.add(current);
        }
    }

    private void walkCollateralGroups(List<TagValue> flat, ParsedGroups groups) {
        int count = readCount(flat, 909);
        if (count <= 0) {
            count = inferCollateralGroupCount(flat);
        }
        GroupInstance current = null;
        int seen = 0;
        for (TagValue tv : flat) {
            if (isCollateralTag(tv.tag)) {
                if (current == null || tv.tag == 909) {
                    if (current != null) {
                        groups.collateralGroups.add(current);
                        seen++;
                    }
                    if (count > 0 && seen >= count) {
                        break;
                    }
                    current = new GroupInstance();
                }
                current.tags.add(tv);
            }
        }
        if (current != null) {
            groups.collateralGroups.add(current);
        }
    }

    private int inferCollateralGroupCount(List<TagValue> flat) {
        int n = 0;
        for (TagValue tv : flat) {
            if (tv.tag == 920) {
                n++;
            }
        }
        return n;
    }

    private boolean isCollateralTag(int tag) {
        return tag >= 909 && tag <= 930;
    }

    private int readCount(List<TagValue> flat, int countTag) {
        for (TagValue tv : flat) {
            if (tv.tag == countTag) {
                try {
                    return Integer.parseInt(tv.value);
                } catch (NumberFormatException ex) {
                    return -1;
                }
            }
        }
        return -1;
    }

    private int indexOf(byte[] data, int start, byte needle) {
        for (int i = start; i < data.length; i++) {
            if (data[i] == needle) {
                return i;
            }
        }
        return -1;
    }

    public String extractLegReference(GroupInstance leg) {
        for (TagValue tv : leg.tags) {
            if (tv.tag == 921) {
                return tv.value;
            }
        }
        return "";
    }

    public double extractHaircut(GroupInstance collateral) {
        for (TagValue tv : collateral.tags) {
            if (tv.tag == 909) {
                try {
                    return Double.parseDouble(tv.value);
                } catch (NumberFormatException ex) {
                    return 0;
                }
            }
        }
        return 0;
    }

    public byte[] serializePartyGroup(List<GroupInstance> groups) {
        StringBuilder sb = new StringBuilder();
        sb.append("453=").append(groups.size()).append((char) 1);
        for (GroupInstance g : groups) {
            for (TagValue tv : g.tags) {
                sb.append(tv.tag).append('=').append(tv.value).append((char) 1);
            }
        }
        return sb.toString().getBytes(StandardCharsets.US_ASCII);
    }
}
