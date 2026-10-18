package com.gamecoach.service.claude;

import com.gamecoach.model.Medal;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolInputTest {

    @Test
    void readsLooselyTypedValues() {
        Map<String, Object> m = new HashMap<>();
        m.put("a", 12);
        m.put("b", "1,234");
        m.put("c", 33.6);
        m.put("d", "true");
        m.put("e", "  ");
        m.put("f", List.of("x", " ", "y"));
        assertEquals(12, ToolInput.integer(m, "a"));
        assertEquals(1234, ToolInput.integer(m, "b"));
        assertEquals(34, ToolInput.integer(m, "c"));
        assertTrue(ToolInput.bool(m, "d"));
        assertFalse(ToolInput.bool(m, "missing"));
        assertNull(ToolInput.string(m, "e"));
        assertEquals(List.of("x", "y"), ToolInput.strings(m, "f"));
        assertNull(ToolInput.integer(m, "missing"));
    }

    @Test
    void matchesEnumNamesLoosely() {
        assertEquals(Medal.MOST_KOS, ToolInput.enumValue(Medal.class, "Most KOs"));
        assertEquals(Medal.MOST_HEALING, ToolInput.enumValue(Medal.class, "most-healing"));
        assertNull(ToolInput.enumValue(Medal.class, "best hair"));
    }
}
