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
        String s = clean(value.toString());
        return s.isEmpty() || s.equalsIgnoreCase("null") ? null : s;
    }

    /**
     * Trims whitespace, a stray backslash at either end, an unmatched quote mark at either end, and
     * quotes wrapped around the whole text: formatting slips a model sometimes makes (for example a
     * sentence ending in \"). A sentence that legitimately ends with a quotation keeps its quotes.
     */
    static String clean(String text) {
        String s = text.strip();
        boolean changed = true;
        while (changed && !s.isEmpty()) {
            changed = false;
            long quotes = s.chars().filter(c -> c == '"').count();
            if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"") && s.indexOf('"', 1) == s.length() - 1) {
                s = s.substring(1, s.length() - 1).strip();          // "whole text in quotes"
                changed = true;
            } else if (s.endsWith("\\") || (s.endsWith("\"") && quotes % 2 == 1)) {
                s = s.substring(0, s.length() - 1).stripTrailing();  // trailing \ or unmatched "
                changed = true;
            } else if (s.startsWith("\\") || (s.startsWith("\"") && quotes % 2 == 1)) {
                s = s.substring(1).stripLeading();                   // leading \ or unmatched "
                changed = true;
            }
        }
        return s;
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
        if (value instanceof String text) {
            // A model sometimes sends a list as text: a JSON array, or one plain sentence.
            String t = text.strip();
            if (t.startsWith("[") && t.endsWith("]")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"").matcher(t);
                while (m.find()) {
                    String item = clean(m.group(1).replace("\\\"", "\"").replace("\\n", " "));
                    if (!item.isEmpty()) {
                        result.add(item);
                    }
                }
            } else if (!clean(t).isEmpty() && !t.equalsIgnoreCase("null")) {
                result.add(clean(t));
            }
            return result;
        }
        if (value instanceof List<?> items) {
            for (Object item : items) {
                String text = item == null ? "" : clean(item.toString());
                if (!text.isEmpty()) {
                    result.add(text);
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
