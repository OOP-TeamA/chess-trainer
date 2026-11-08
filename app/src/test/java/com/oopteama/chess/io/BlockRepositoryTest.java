package com.oopteama.chess.io;

import com.oopteama.chess.core.MalformedDataException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
