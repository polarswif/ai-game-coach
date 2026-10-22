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
    void strayQuotesAtTheEndsAreRemoved() {
        Map<String, Object> m = new HashMap<>();
        m.put("ctx", "This was a close match.\\\"");          // ends in \"
        m.put("list", List.of("\"Quoted item\"", "Fine"));
        assertEquals("This was a close match.", ToolInput.string(m, "ctx"));
        assertEquals(List.of("Quoted item", "Fine"), ToolInput.strings(m, "list"));
        assertEquals("Say \"hi\" now.", ToolInput.clean("Say \"hi\" now."));   // inner quotes stay
        assertEquals("Call it \"push.\"", ToolInput.clean("Call it \"push.\""));  // a real closing quote stays
    }

    @Test
    void matchesEnumNamesLoosely() {
        assertEquals(Medal.MOST_KOS, ToolInput.enumValue(Medal.class, "Most KOs"));
        assertEquals(Medal.MOST_HEALING, ToolInput.enumValue(Medal.class, "most-healing"));
        assertNull(ToolInput.enumValue(Medal.class, "best hair"));
    }

    @Test
    void aListSentAsTextIsStillRead() {
        java.util.Map<String, Object> m = new java.util.HashMap<>();
        m.put("a", "[\"First point.\", \"Second, with a comma.\"]");
        m.put("b", "One plain sentence.");
        m.put("c", "[]");
        assertEquals(java.util.List.of("First point.", "Second, with a comma."), ToolInput.strings(m, "a"));
        assertEquals(java.util.List.of("One plain sentence."), ToolInput.strings(m, "b"));
        assertTrue(ToolInput.strings(m, "c").isEmpty());
    }
}
