package com.oopteama.chess.io;

import com.oopteama.chess.core.MalformedDataException;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockTest {

    private static Block term() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("ko", "포크");
        values.put("related", " pin , skewer,, ");
        values.put("goal", "move:d4f5|d4f3");
        values.put("try.fen", "8/8/8/8/8/8/8/8 w - - 0 1");
        values.put("try.goal", "check");
        values.put("empty", "");
        return new Block("term", "fork", values, "glossary.txt", 12);
    }

    @Test
    void require는_값을_돌려주고_없거나_비면_파일명과_행번호를_담아_실패한다() throws MalformedDataException {
        Block b = term();
        assertEquals("포크", b.require("ko"));

        MalformedDataException missing = assertThrows(MalformedDataException.class, () -> b.require("def"));
        assertEquals("glossary.txt", missing.source());
        assertEquals(12, missing.line());
        assertTrue(missing.getMessage().contains("glossary.txt:12"));
        assertTrue(missing.getMessage().contains("def"));

        MalformedDataException empty = assertThrows(MalformedDataException.class, () -> b.require("empty"));
        assertTrue(empty.getMessage().contains("없거나 비어 있습니다"), empty.getMessage());
    }

    @Test
    void get은_없을_때_기본값() {
        Block b = term();
        assertEquals("포크", b.get("ko", "x"));
        assertEquals("standard", b.get("rules", "standard"));
        assertEquals("", b.get("empty", "x"));
    }

    @Test
    void list는_쉼표로_나누고_공백과_빈_항목을_버린다() {
        Block b = term();
        assertEquals(List.of("pin", "skewer"), b.list("related"));
        assertEquals(List.of(), b.list("none"));
        assertEquals(List.of(), b.list("empty"));
    }

    @Test
    void alternatives는_세로막대로_나눈다() {
        assertEquals(List.of("move:d4f5", "d4f3"), term().alternatives("goal"));
        assertEquals(List.of(), term().alternatives("none"));
    }

    @Test
    void sub는_접두어를_뗀_블록을_만든다() {
        Block sub = term().sub("try.");
        assertEquals(Map.of("fen", "8/8/8/8/8/8/8/8 w - - 0 1", "goal", "check"), sub.values());
        assertEquals("term", sub.type());
        assertEquals("fork", sub.id());
        assertEquals("glossary.txt", sub.source());
        assertEquals(12, sub.line());
        assertTrue(term().sub("nothing.").values().isEmpty());
    }

    @Test
    void 값_맵은_불변_사본이고_순서를_유지한다() {
        Map<String, String> src = new LinkedHashMap<>();
        src.put("b", "1");
        src.put("a", "2");
        Block b = new Block("x", "", src, "s", 1);
        src.put("c", "3");
        assertEquals(List.of("b", "a"), List.copyOf(b.values().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> b.values().put("d", "4"));
    }

    @Test
    void null_인자는_거부한다() {
        assertThrows(NullPointerException.class, () -> new Block(null, "", Map.of(), "s", 1));
        assertThrows(NullPointerException.class, () -> term().get("ko", null));
    }
}
