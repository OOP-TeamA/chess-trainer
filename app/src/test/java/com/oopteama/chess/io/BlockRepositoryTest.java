package com.oopteama.chess.io;

import com.oopteama.chess.core.MalformedDataException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockRepositoryTest {

    /** 블록 하나 = 객체 하나. ko 필수. */
    private static final class TermNames extends PerBlockRepository<String> {
        @Override
        protected String map(Block block) throws MalformedDataException {
            return block.id() + ":" + block.require("ko");
        }
    }

    /** 여러 블록 = 객체 하나. @lesson 다음의 @step 수를 센다. */
    private static final class StepCounts extends BlockRepository<String> {
        @Override
        protected List<String> assemble(List<Block> blocks) {
            List<String> out = new ArrayList<>();
            String lesson = null;
            int steps = 0;
            for (Block b : blocks) {
                if (b.type().equals("lesson")) {
                    lesson = b.id();
                } else if (b.type().equals("step")) {
                    steps++;
                }
            }
            if (lesson != null) {
                out.add(lesson + "=" + steps);
            }
            return out;
        }
    }

    /** 파일 하나 = 객체 하나. 형식 오류나 빈 파일이면 파일 전체를 건너뛴다. */
    private static final class WholeFileStepCounts extends BlockRepository<String> {
        private int assembleCalls;

        @Override
        protected List<String> assemble(List<Block> blocks) {
            assembleCalls++;
            return new StepCounts().assemble(blocks);
        }

        @Override
        protected boolean skipFileOnProblems() {
            return true;
        }
    }

    /** BlockRepository 로거의 경고 메시지를 모은다. */
    private static List<String> captureWarnings(IORunnable action) throws IOException {
        Logger logger = Logger.getLogger(BlockRepository.class.getName());
        List<String> messages = new ArrayList<>();
        Handler handler = new Handler() {
            @Override
            public void publish(LogRecord record) {
                messages.add(record.getMessage());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        logger.addHandler(handler);
        try {
            action.run();
        } finally {
            logger.removeHandler(handler);
        }
        return messages;
    }

    @FunctionalInterface
    private interface IORunnable {
        void run() throws IOException;
    }

    @Test
    void PerBlockRepository는_형식_오류와_변환_실패를_건너뛴다() throws IOException {
        String text = """
                @term a
                ko=가

                @term b
                en=no-ko

                @term c
                ko=다
                ko=중복

                @term d
                ko=라
                """;
        List<String> result = new TermNames().loadAll(new StringReader(text), "glossary.txt");
        assertEquals(List.of("a:가", "d:라"), result);
    }

    @Test
    void BlockRepository는_정상_블록만_assemble에_넘긴다() throws IOException {
        String text = "@lesson knight\n@step\nfen=x\n@step\n잘못\n@step\nfen=y\n";
        assertEquals(List.of("knight=2"), new StepCounts().loadAll(new StringReader(text), "07_knight.txt"));
    }

    @Test
    void loadAll_결과는_불변이다() throws IOException {
        List<String> result = new TermNames().loadAll(new StringReader("@term a\nko=가\n"), "g.txt");
        assertThrows(UnsupportedOperationException.class, () -> result.add("x"));
    }

    @Test
    void skipFileOnProblems면_깨진_블록이_하나라도_있을_때_파일_전체를_건너뛴다() throws IOException {
        String text = """
                @lesson knight
                @step
                fen=x
                @step
                goal=a
                goal=b
                """;
        WholeFileStepCounts repo = new WholeFileStepCounts();
        List<String> warnings = captureWarnings(() ->
                assertEquals(List.of(), repo.loadAll(new StringReader(text), "07_knight.txt")));
        assertEquals(0, repo.assembleCalls);
        assertTrue(warnings.stream().anyMatch(m -> m.startsWith("07_knight.txt:6:")), warnings::toString);
        assertTrue(warnings.stream().anyMatch(m -> m.startsWith("07_knight.txt: 형식 오류가 1건")),
                warnings::toString);
    }

    @Test
    void skipFileOnProblems면_빈_파일도_파일명과_함께_건너뛴다() throws IOException {
        WholeFileStepCounts repo = new WholeFileStepCounts();
        List<String> warnings = captureWarnings(() ->
                assertEquals(List.of(), repo.loadAll(new StringReader("# 주석만\n\n"), "empty.txt")));
        assertEquals(0, repo.assembleCalls);
        assertEquals(List.of("empty.txt: 읽을 블록이 없어 파일을 건너뜁니다"), warnings);
    }

    @Test
    void skipFileOnProblems여도_정상_파일은_그대로_읽는다() throws IOException {
        String text = """
                @lesson knight
                @step
                fen=x
                @step
                fen=y
                """;
        WholeFileStepCounts repo = new WholeFileStepCounts();
        List<String> warnings = captureWarnings(() ->
                assertEquals(List.of("knight=2"), repo.loadAll(new StringReader(text), "07_knight.txt")));
        assertEquals(1, repo.assembleCalls);
        assertTrue(warnings.isEmpty(), warnings::toString);
    }

    @Test
    void 기본값은_깨진_블록만_빼고_빈_파일도_assemble에_넘긴다() throws IOException {
        StepCounts repo = new StepCounts();
        assertFalse(repo.skipFileOnProblems());
        assertEquals(List.of(), repo.loadAll(new StringReader(""), "empty.txt"));
    }
}
