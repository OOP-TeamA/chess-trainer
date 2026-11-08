package com.oopteama.chess.io;

import com.oopteama.chess.core.MalformedDataException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockReaderTest {

    private static BlockReader.Result read(String text) throws IOException {
        return BlockReader.read(new StringReader(text), "test.txt");
    }

    @Test
    void 블록_머리_종류_아이디와_키값을_읽는다() throws IOException {
        BlockReader.Result r = read("""
                # 주석
                @term fork
                ko = 포크
                def=a=b=c

                @step
                fen=8/8/8/8/8/8/8/8 w - - 0 1
                """);
        assertTrue(r.problems().isEmpty());
        assertEquals(2, r.blocks().size());

        Block fork = r.blocks().get(0);
        assertEquals("term", fork.type());
        assertEquals("fork", fork.id());
        assertEquals(2, fork.line());
        assertEquals("test.txt", fork.source());
        assertEquals(Map.of("ko", "포크", "def", "a=b=c"), fork.values());

        Block step = r.blocks().get(1);
        assertEquals("step", step.type());
        assertEquals("", step.id());
        assertEquals(6, step.line());
    }

    @Test
    void 첫_머리_이전의_키는_이름_없는_기본_블록() throws IOException {
        BlockReader.Result r = read("""
                active=default

                @profile default
                name=학습자
                """);
        assertEquals(2, r.blocks().size());
        Block head = r.blocks().get(0);
        assertEquals("", head.type());
        assertEquals("", head.id());
        assertEquals(1, head.line());
        assertEquals("default", head.get("active", ""));
        assertEquals("학습자", r.blocks().get(1).get("name", ""));
    }

    @Test
    void 주석과_빈_줄만_있으면_블록이_없다() throws IOException {
        BlockReader.Result r = read("# a\n\n   \n  # b\n");
        assertTrue(r.blocks().isEmpty());
        assertTrue(r.problems().isEmpty());
    }

    @Test
    void 키가_없는_머리도_빈_블록으로_남긴다() throws IOException {
        BlockReader.Result r = read("@a\n@b x\n");
        assertEquals(2, r.blocks().size());
        assertTrue(r.blocks().get(0).values().isEmpty());
    }

    @Test
    void 이스케이프를_푼다() throws IOException {
        Block b = read("@x\ntext=첫 줄\\n둘째 줄\npath=C:\\\\data\\\\a\nother=\\t그대로\\\n").blocks().get(0);
        assertEquals("첫 줄\n둘째 줄", b.get("text", ""));
        assertEquals("C:\\data\\a", b.get("path", ""));
        assertEquals("\\t그대로\\", b.get("other", ""));
    }

    @Test
    void 깨진_블록만_건너뛰고_파일명과_행번호를_남긴다() throws IOException {
        BlockReader.Result r = read("""
                @term a
                ko=가

                @term b
                ko=나
                ko=다

                @term c
                이건 키값이 아님

                @
                ko=라

                @term d e f
                ko=마

                @term g
                =값만

                @term ok
                ko=바
                """);
        assertEquals(List.of("a", "ok"), r.blocks().stream().map(Block::id).toList());
        assertEquals(5, r.problems().size());
        List<Integer> lines = r.problems().stream().map(MalformedDataException::line).toList();
        assertEquals(List.of(6, 9, 11, 14, 18), lines);
        for (MalformedDataException p : r.problems()) {
            assertEquals("test.txt", p.source());
            assertTrue(p.getMessage().startsWith("test.txt:" + p.line() + ":"), p.getMessage());
        }
    }

    @Test
    void 첫_줄의_BOM은_제거한다() throws IOException {
        BlockReader.Result r = read("\uFEFF@term fork\nko=포크\n");
        assertEquals("term", r.blocks().get(0).type());
    }

    @Test
    void 결과_목록은_불변이다() throws IOException {
        BlockReader.Result r = read("@a\n");
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> r.blocks().add(r.blocks().get(0)));
    }

    @Test
    void 공용_샘플_용어집과_퀴즈를_모두_읽는다() throws IOException {
        BlockReader.Result glossary = readFixture("glossary.txt");
        assertTrue(glossary.problems().isEmpty(), glossary.problems().toString());
        assertEquals(10, glossary.blocks().size());
        assertTrue(glossary.blocks().stream().allMatch(b -> b.type().equals("term")));
        assertTrue(glossary.blocks().stream().anyMatch(b -> b.id().equals("fork")));

        BlockReader.Result quiz = readFixture("term-quiz.txt");
        assertTrue(quiz.problems().isEmpty(), quiz.problems().toString());
        assertEquals(10, quiz.blocks().size());
        Block q1 = quiz.blocks().get(0);
        assertEquals("q0001", q1.id());
        assertEquals("fork", q1.get("term", ""));
        assertEquals("1", q1.get("answer", ""));
    }

    @Test
    void 공용_샘플_나이트_레슨은_lesson_하나와_step_셋() throws IOException {
        BlockReader.Result r = readFixture("lessons/07_knight.txt");
        assertTrue(r.problems().isEmpty(), r.problems().toString());
        assertEquals(List.of("lesson", "step", "step", "step"), r.blocks().stream().map(Block::type).toList());
        assertEquals("knight", r.blocks().get(0).id());
        assertEquals(List.of("move:d4f5"), r.blocks().get(2).alternatives("goal"));
    }

    @Test
    void 공용_샘플에_깨진_블록을_섞어도_정상_블록은_모두_읽는다() throws IOException {
        String original = Files.readString(TestFixtures.firstDemo().resolve("glossary.txt"), StandardCharsets.UTF_8);
        String broken = "@term broken1\nko=하나\nko=둘\n\n" + original + "\n@term broken2\n잘못된 줄\n";
        BlockReader.Result r = BlockReader.read(new StringReader(broken), "glossary.txt");
        assertEquals(10, r.blocks().size());
        assertEquals(2, r.problems().size());
        assertEquals(3, r.problems().get(0).line());
    }

    private static BlockReader.Result readFixture(String relative) throws IOException {
        Path file = TestFixtures.firstDemo().resolve(relative);
        try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return BlockReader.read(in, relative);
        }
    }
}
