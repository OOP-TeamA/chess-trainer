package com.oopteama.chess.progress;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressTest {

    @Test
    void 빈_기록은_모두_0() {
        Progress p = new Progress();
        assertTrue(p.lessonsDone().isEmpty());
        assertTrue(p.termQuizCorrectIds().isEmpty());
        assertEquals(new AttemptStats(0, 0), p.termQuizAttempts());
        assertEquals(0, p.score());
    }

    @Test
    void 레슨_완료는_ID별로_한_번만_점수에_들어간다() {
        Progress p = new Progress();
        p.markLessonDone("knight");
        p.markLessonDone("knight");
        p.markLessonDone("rook");
        assertEquals(List.of("knight", "rook"), List.copyOf(p.lessonsDone()));
        assertEquals(20, p.score());
    }

    @Test
    void 용어_퀴즈는_호출마다_시도가_늘고_같은_문제_재정답은_점수에_한_번() {
        Progress p = new Progress();
        p.recordTermQuiz("q0001", true);
        p.recordTermQuiz("q0002", false);
        p.recordTermQuiz("q0001", true);
        assertEquals(new AttemptStats(3, 2), p.termQuizAttempts());
        assertEquals(Set.of("q0001"), p.termQuizCorrectIds());
        assertEquals(1, p.score());
    }

    @Test
    void 퍼즐은_종료마다_시도가_늘고_해결_상태와_5점은_유지된다() {
        Progress p = new Progress();
        AtomicInteger calls = new AtomicInteger();
        p.addChangeListener(calls::incrementAndGet);
        assertEquals(PuzzleState.UNSEEN, p.puzzleState("p0001"));
        assertEquals(new AttemptStats(0, 0), p.puzzleAttempts("p0001"));

        p.recordPuzzle("p0001", true);   // HINT까지만 보고 완주
        assertEquals(new AttemptStats(1, 1), p.puzzleAttempts("p0001"));
        p.recordPuzzle("p0001", false);  // REVEAL 후 완주
        assertEquals(new AttemptStats(2, 1), p.puzzleAttempts("p0001"));
        assertEquals(PuzzleState.SOLVED, p.puzzleState("p0001"));
        p.recordPuzzle("p0001", true);   // 공개 없이 재완주
        assertEquals(new AttemptStats(3, 2), p.puzzleAttempts("p0001"));
        assertEquals(PuzzleState.SOLVED, p.puzzleState("p0001"));
        assertEquals(5, p.score());
        assertEquals(3, calls.get());

        p.recordPuzzle("p0002", false);
        assertEquals(PuzzleState.FAILED, p.puzzleState("p0002"));
        assertEquals(5, p.score());
        p.recordPuzzle("p0002", true);
        assertEquals(PuzzleState.SOLVED, p.puzzleState("p0002"));
        assertEquals(10, p.score());
    }

    @Test
    void 퍼즐_ID를_검사한다() {
        Progress p = new Progress();
        assertThrows(IllegalArgumentException.class, () -> p.recordPuzzle("p 1", true));
        assertThrows(IllegalArgumentException.class, () -> p.recordPuzzle("", true));
        assertThrows(NullPointerException.class, () -> p.recordPuzzle(null, true));
        assertThrows(NullPointerException.class, () -> p.puzzleState(null));
        assertEquals(PuzzleState.UNSEEN, p.puzzleState("p 1"));
    }

    @Test
    void 첫_시연_흐름의_점수는_17() {
        Progress p = new Progress();
        p.markLessonDone("knight");
        for (int i = 1; i <= 10; i++) {
            p.recordTermQuiz("q%04d".formatted(i), i <= 7);
        }
        assertEquals(new AttemptStats(10, 7), p.termQuizAttempts());
        assertEquals(17, p.score());
    }

    @Test
    void 값이_바뀔_때만_리스너를_부르고_해제하면_부르지_않는다() {
        Progress p = new Progress();
        AtomicInteger calls = new AtomicInteger();
        Runnable listener = calls::incrementAndGet;
        p.addChangeListener(listener);
        p.markLessonDone("knight");
        p.markLessonDone("knight");      // 변화 없음
        p.recordTermQuiz("q0001", false); // 시도 +1은 변화
        assertEquals(2, calls.get());
        p.removeChangeListener(listener);
        p.markLessonDone("rook");
        assertEquals(2, calls.get());
    }

    @Test
    void 반환_집합은_불변_사본() {
        Progress p = new Progress();
        p.markLessonDone("knight");
        Set<String> lessons = p.lessonsDone();
        assertThrows(UnsupportedOperationException.class, () -> lessons.add("rook"));
        p.markLessonDone("rook");
        assertEquals(1, lessons.size());
        assertThrows(UnsupportedOperationException.class, () -> p.termQuizCorrectIds().add("q"));
    }

    @Test
    void 파일에_쓸_수_없는_ID와_null을_거부한다() {
        Progress p = new Progress();
        for (String bad : List.of("", "a,b", "a b", "a\nb", "\t")) {
            assertThrows(IllegalArgumentException.class, () -> p.markLessonDone(bad), bad);
            assertThrows(IllegalArgumentException.class, () -> p.recordTermQuiz(bad, true), bad);
        }
        assertThrows(NullPointerException.class, () -> p.markLessonDone(null));
        assertThrows(NullPointerException.class, () -> p.addChangeListener(null));
        assertEquals(new AttemptStats(0, 0), p.termQuizAttempts());
    }

    @Test
    void AttemptStats는_범위를_검사한다() {
        assertThrows(IllegalArgumentException.class, () -> new AttemptStats(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new AttemptStats(1, 2));
        assertThrows(IllegalArgumentException.class, () -> new AttemptStats(0, -1));
        assertEquals(3, new AttemptStats(3, 3).solved());
    }
}
