package com.oopteama.chess.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TextFilesTest {

    @TempDir
    Path tmp;

    @Test
    void 없는_파일은_빈_목록() throws IOException {
        assertEquals(List.of(), TextFiles.readLines(tmp.resolve("none.txt")));
    }

    @Test
    void 상위_폴더를_만들고_UTF8로_쓴_뒤_다시_읽는다() throws IOException {
        Path file = tmp.resolve("profiles").resolve("default.txt");
        TextFiles.writeAtomic(file, "lesson.done=knight\nname=학습자\n");
        assertEquals(List.of("lesson.done=knight", "name=학습자"), TextFiles.readLines(file));
        assertEquals("lesson.done=knight\nname=학습자\n", Files.readString(file, StandardCharsets.UTF_8));
    }

    @Test
    void 기존_파일을_바꾸고_임시_파일을_남기지_않는다() throws IOException {
        Path file = tmp.resolve("prefs.txt");
        TextFiles.writeAtomic(file, "a=1\n");
        TextFiles.writeAtomic(file, "a=2\n");
        assertEquals(List.of("a=2"), TextFiles.readLines(file));
        try (Stream<Path> files = Files.list(tmp)) {
            assertEquals(List.of(file), files.toList());
        }
    }

    @Test
    void 첫_줄_BOM을_제거한다() throws IOException {
        Path file = tmp.resolve("bom.txt");
        Files.writeString(file, "﻿a=1\nb=2", StandardCharsets.UTF_8);
        assertEquals(List.of("a=1", "b=2"), TextFiles.readLines(file));
    }

    @Test
    void 쓰기에_실패하면_기존_파일과_폴더가_그대로다() throws IOException {
        // 대상이 비어 있지 않은 폴더라 교체가 실패한다.
        Path target = tmp.resolve("dir.txt");
        Files.createDirectories(target);
        Files.writeString(target.resolve("keep.txt"), "x", StandardCharsets.UTF_8);
        assertThrows(IOException.class, () -> TextFiles.writeAtomic(target, "new"));
        try (Stream<Path> files = Files.list(tmp)) {
            assertEquals(List.of(target), files.toList());
        }
        assertEquals("x", Files.readString(target.resolve("keep.txt"), StandardCharsets.UTF_8));
    }

    @Test
    void 결과_목록은_불변이고_null을_거부한다() throws IOException {
        Path file = tmp.resolve("a.txt");
        TextFiles.writeAtomic(file, "a\n");
        assertThrows(UnsupportedOperationException.class, () -> TextFiles.readLines(file).add("b"));
        assertThrows(NullPointerException.class, () -> TextFiles.readLines(null));
        assertThrows(NullPointerException.class, () -> TextFiles.writeAtomic(file, null));
    }
}
