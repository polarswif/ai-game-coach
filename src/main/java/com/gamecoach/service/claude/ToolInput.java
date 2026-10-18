package com.gamecoach.service.claude;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads values out of a tool call's input (a Map parsed from JSON) without trusting its types.
 * A model usually follows the schema, but can still send a number as "12,345" or leave a field out,
 * so each reader accepts the reasonable variants and returns null instead of throwing.
 */
public final class ToolInput {

    private ToolInput() {
    }

    public static String string(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value == null) {
            return null;
        }
        String s = value.toString().strip();
        return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }

    /** Accepts 12345, 12345.0 and "12,345". Null when missing or not a number. */
    public static Integer integer(Map<String, Object> map, String key) {
        Double d = number(map, key);
        return d == null ? null : (int) Math.round(d);
    }

    /** Accepts 33.0, 33 and "33%". Null when missing or not a number. */
    public static Double number(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        if (value instanceof Number n) {
            return n.doubleValue();
        }
        if (value instanceof String s) {
            String cleaned = s.replace(",", "").replace("%", "").strip();
            try {
                return cleaned.isEmpty() ? null : Double.parseDouble(cleaned);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    public static boolean bool(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        return value instanceof Boolean b ? b : value instanceof String s && s.strip().equalsIgnoreCase("true");
    }

    /** A list of strings; blank and null items are dropped. Empty when missing. */
    public static List<String> strings(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        List<String> result = new ArrayList<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                if (item != null && !item.toString().isBlank()) {
                    result.add(item.toString().strip());
                }
            }
        }
        return result;
    }

    /** A list of JSON objects. Items that aren't objects are skipped. Empty when missing. */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> objects(Map<String, Object> map, String key) {
        Object value = map == null ? null : map.get(key);
        List<Map<String, Object>> result = new ArrayList<>();
        if (value instanceof List<?> items) {
            for (Object item : items) {
                if (item instanceof Map<?, ?> m) {
                    result.add((Map<String, Object>) m);
                }
            }
        }
        return result;
    }

    /** The enum constant named by the value (case and spaces ignored, "Most KOs" → MOST_KOS); null if none. */
    public static <E extends Enum<E>> E enumValue(Class<E> type, String value) {
        if (value == null) {
            return null;
        }
        String name = value.strip().toUpperCase(java.util.Locale.ROOT).replaceAll("[\\s-]+", "_");
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(name)) {
                return constant;
            }
        }
        return null;
    }
}
