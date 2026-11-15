package com.oopteama.chess.exercise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.content.TermQuestion;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** QUIZ-05: 용어 4지선다 세트의 진행과 채점. */
class TermQuizSessionTest {

    private static final TermQuestion Q1 = question("q1", 0);
    private static final TermQuestion Q2 = question("q2", 3);

    private static TermQuestion question(String id, int answerIndex) {
        return new TermQuestion(id, "fork", id + " 질문", List.of("가", "나", "다", "라"), answerIndex, id + " 해설");
    }

    @Test
    void gradesEachQuestionOnceAndFinishesAfterLastNext() {
        TermQuizSession session = new TermQuizSession(List.of(Q1, Q2));
        assertEquals(2, session.totalCount());
        assertEquals(Optional.of(Q1), session.current());

        assertEquals(new TermAnswer("q1", true, 0, "q1 해설"), session.submit(0));
        assertEquals(Optional.of(Q1), session.current()); // next() 전까지는 같은 문항
        assertEquals(1, session.correctCount());
        session.next();

        assertEquals(Optional.of(Q2), session.current());
        assertEquals(new TermAnswer("q2", false, 3, "q2 해설"), session.submit(1));
        assertFalse(session.finished());
        session.next();

        assertTrue(session.finished());
        assertEquals(Optional.empty(), session.current());
        assertEquals(1, session.correctCount());
    }

    @Test
    void rejectsSecondSubmitForSameQuestion() {
        TermQuizSession session = new TermQuizSession(List.of(Q1, Q2));
        session.submit(1);
        assertThrows(IllegalStateException.class, () -> session.submit(0));
        assertEquals(0, session.correctCount()); // 오답 뒤에 정답을 다시 내도 점수가 오르지 않는다
    }

    @Test
    void nextRequiresSubmittedAnswer() {
        TermQuizSession session = new TermQuizSession(List.of(Q1));
        assertThrows(IllegalStateException.class, session::next);
        session.submit(0);
        session.next();
        assertThrows(IllegalStateException.class, session::next);
        assertThrows(IllegalStateException.class, () -> session.submit(0));
    }

    @Test
    void outOfRangeChoiceIsRejectedWithoutConsumingTheQuestion() {
        TermQuizSession session = new TermQuizSession(List.of(Q1));
        assertThrows(IllegalArgumentException.class, () -> session.submit(-1));
        assertThrows(IllegalArgumentException.class, () -> session.submit(4));
        assertTrue(session.submit(0).correct());
    }

    @Test
    void needsAtLeastOneQuestionAndCopiesTheList() {
        assertThrows(IllegalArgumentException.class, () -> new TermQuizSession(List.of()));
        List<TermQuestion> source = new ArrayList<>(List.of(Q1));
        TermQuizSession session = new TermQuizSession(source);
        source.add(Q2);
        assertEquals(1, session.totalCount());
    }

    @Test
    void questionRecordChecksInvariants() {
        assertThrows(IllegalArgumentException.class, () -> question("q", 4));
        assertThrows(IllegalArgumentException.class, () -> question("q", -1));
        assertThrows(IllegalArgumentException.class,
                () -> new TermQuestion("q", "fork", "질문", List.of("가", "나", "다"), 0, "해설"));
    }
}
