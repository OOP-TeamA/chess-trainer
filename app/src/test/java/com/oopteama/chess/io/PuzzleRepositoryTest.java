package com.oopteama.chess.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.TestLog;
import com.oopteama.chess.content.Puzzle;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

/** QUIZ-01·SYS-03: 퍼즐 파일 로드와 로드 시점 검증. 잘못된 퍼즐은 그 퍼즐만 건너뛴다. */
class PuzzleRepositoryTest {

    /** 기능 명세 6장 예시 그대로(줄 1~6). */
    private static final String BACK_RANK = """
            # solution은 UCI, 공백 구분, 상대 응수 포함
            @puzzle p0001
            theme=메이트인1,백랭크
            fen=6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1
            solution=d1d8
            explain=흑 킹 앞의 폰 세 개가 오히려 탈출로를 막고 있다. [[백랭크 메이트]]의 전형.
            """;

    private static final String MATE_IN_TWO = """
            @puzzle p0002
            theme=메이트인2
            fen=3rr1k1/5ppp/8/8/8/8/3Q1PPP/3R2K1 w - - 0 1
            solution=D2D8  e8d8 d1d8
            """;

    private static final String FORK = """
            @puzzle p0003
            theme=포크
            fen=r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1
            solution=b5c7 e8e7 c7a8
            """;

    private static List<Puzzle> load(String text) throws IOException {
        return new PuzzleRepository().loadAll(new StringReader(text), "puzzles.txt");
    }

    @Test
    void validPuzzlesLoadInFileOrder() throws IOException {
        List<Puzzle> puzzles;
        try (TestLog log = TestLog.capture(PuzzleRepository.class)) {
            puzzles = load(BACK_RANK + "\n" + MATE_IN_TWO + "\n" + FORK);
            assertEquals(List.of(), log.messages());
        }
        assertEquals(List.of("p0001", "p0002", "p0003"), puzzles.stream().map(Puzzle::id).toList());

        Puzzle first = puzzles.get(0);
        assertEquals(List.of("메이트인1", "백랭크"), List.copyOf(first.themes()));
        assertEquals(List.of("d1d8"), first.solutionUci());
        assertTrue(first.explain().contains("[[백랭크 메이트]]"));
        assertTrue(first.isMateTheme());

        assertEquals(List.of("d2d8", "e8d8", "d1d8"), puzzles.get(1).solutionUci(), "대문자·연속 공백 정리");
        assertEquals("", puzzles.get(2).explain());
    }

    @Test
    void duplicateIdKeepsTheFirstValidPuzzle() throws IOException {
        try (TestLog log = TestLog.capture(PuzzleRepository.class)) {
            List<Puzzle> puzzles = load(BACK_RANK + "\n" + FORK.replace("p0003", "p0001"));
            assertEquals(1, puzzles.size());
            assertEquals("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1", puzzles.get(0).fen());
            assertEquals(1, log.messages().size());
            assertTrue(log.messages().get(0).startsWith("puzzles.txt:8: 퍼즐 ID 'p0001'가 중복"), log.messages().get(0));
        }
    }

    static Stream<Arguments> brokenPuzzles() {
        return Stream.of(
                Arguments.of("정해진 주제", FORK.replace("theme=포크", "theme=포크,희생")),
                Arguments.of("theme", FORK.replace("theme=포크\n", "")),
                Arguments.of("solution", FORK.replace("solution=b5c7 e8e7 c7a8\n", "")),
                Arguments.of("2번째 수 e8e8", FORK.replace("e8e7", "e8e8")),
                Arguments.of("사용자 수로 끝나야", FORK.replace("b5c7 e8e7 c7a8", "b5c7 e8e7")),
                Arguments.of("표준 규칙", FORK.replace("4K3", "8")),
                Arguments.of("fen 형식 오류", FORK.replace("r3k3/8/8/1N6/8/8/8/4K3", "r3k3/8/1N6/8/8/8/4K3")),
                Arguments.of("체크메이트가 아닙니다", BACK_RANK.replace("solution=d1d8", "solution=d1d7")),
                Arguments.of("메이트인2인데 사용자 수가 1개", BACK_RANK.replace("메이트인1", "메이트인2")),
                Arguments.of("@term", BACK_RANK.replace("@puzzle", "@term")),
                Arguments.of("퍼즐 ID", BACK_RANK.replace("p0001", "P-1")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("brokenPuzzles")
    void brokenPuzzleIsSkippedWithItsLocation(String reason, String broken) throws IOException {
        try (TestLog log = TestLog.capture(PuzzleRepository.class)) {
            List<Puzzle> puzzles = load(MATE_IN_TWO + "\n" + broken);
            assertEquals(List.of("p0002"), puzzles.stream().map(Puzzle::id).toList(), "정상 퍼즐은 남는다");
            assertEquals(1, log.messages().size(), log.messages().toString());
            String message = log.messages().get(0);
            assertTrue(message.startsWith("puzzles.txt:"), message);
            assertTrue(message.contains(reason), message);
        }
    }
}
