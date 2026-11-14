package com.oopteama.chess.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.oopteama.chess.content.Lesson;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.exercise.Exercise;
import com.oopteama.chess.exercise.FeedbackLadder;
import com.oopteama.chess.exercise.LessonStepExercise;
import com.oopteama.chess.exercise.Verdict;
import com.oopteama.chess.io.LessonRepository;
import java.awt.Component;
import java.awt.Container;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

/** 실제 나이트 샘플·채점기로 단계 이동 조건을 확인한다. JFrame 없이 EDT에서 실행한다. */
class LessonStepPanelTest {
    @Test
    void revealDoesNotCompleteLessonUntilTheLearnerAnswers() throws Exception {
        Lesson lesson = knight();
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger completions = new AtomicInteger();
            LessonStepPanel panel = new LessonStepPanel(completions::incrementAndGet);
            panel.showStep(lesson, 2);
            JButton next = named(panel, "lesson.next", JButton.class);
            Exercise exercise = new LessonStepExercise(lesson.steps().get(2));
            FeedbackLadder ladder = new FeedbackLadder();
            assertFalse(next.isEnabled());
            for (String square : new String[] {"a1", "a2", "a3"}) {
                Verdict verdict = exercise.submitSquare(Square.of(square));
                assertInstanceOf(Verdict.Wrong.class, verdict);
                ladder.recordWrong();
                panel.showFeedback(verdict, ladder);
                next.doClick(0);
                assertEquals(0, completions.get());
                assertFalse(next.isEnabled());
            }
            assertEquals("오답 3회", named(panel, "lesson.attempts", JLabel.class).getText());
            assertTrue(named(panel, "lesson.help", JTextArea.class).getText().contains("직접 정답"));
            panel.showFeedback(exercise.submitSquare(Square.of("c7")), ladder);
            assertTrue(next.isEnabled());
            assertEquals("레슨 완료", next.getText());
            next.doClick(0);
            next.doClick(0);
            assertEquals(1, completions.get());
        });
    }

    @Test
    void nextStepClearsPreviousSuccessAndWrongCount() throws Exception {
        Lesson lesson = knight();
        SwingUtilities.invokeAndWait(() -> {
            LessonStepPanel panel = new LessonStepPanel(() -> { });
            panel.showStep(lesson, 0);
            Exercise exercise = new LessonStepExercise(lesson.steps().get(0));
            FeedbackLadder ladder = new FeedbackLadder();
            panel.showFeedback(exercise.submit(exercise.legalMoves().stream()
                    .filter(move -> move.uci().equals("d5f6")).findFirst().orElseThrow()), ladder);
            assertTrue(named(panel, "lesson.next", JButton.class).isEnabled());
            panel.showStep(lesson, 1);
            assertFalse(named(panel, "lesson.next", JButton.class).isEnabled());
            assertEquals("오답 0회", named(panel, "lesson.attempts", JLabel.class).getText());
            assertEquals(lesson.steps().get(1).text(), named(panel, "lesson.prompt", JTextArea.class).getText());
        });
    }

    @Test
    void repeatedSolvedNotificationCannotAdvanceTwice() throws Exception {
        Lesson lesson = knight();
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger advances = new AtomicInteger();
            LessonStepPanel panel = new LessonStepPanel(advances::incrementAndGet);
            panel.showStep(lesson, 0);
            Exercise exercise = new LessonStepExercise(lesson.steps().get(0));
            Verdict solved = exercise.submit(exercise.legalMoves().get(0));
            FeedbackLadder ladder = new FeedbackLadder();
            panel.showFeedback(solved, ladder);
            named(panel, "lesson.next", JButton.class).doClick(0);
            panel.showFeedback(solved, ladder);
            named(panel, "lesson.next", JButton.class).doClick(0);
            assertEquals(1, advances.get());
        });
    }

    private static Lesson knight() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        Path relative = Path.of("docs/fixtures/first-demo/data/lessons/07_knight.txt");
        while (root != null && !Files.isRegularFile(root.resolve(relative))) {
            root = root.getParent();
        }
        if (root == null) {
            throw new IllegalStateException("공용 나이트 샘플을 찾지 못했습니다");
        }
        try (Reader in = Files.newBufferedReader(root.resolve(relative), StandardCharsets.UTF_8)) {
            return new LessonRepository().loadAll(in, "07_knight.txt").get(0);
        }
    }

    private static <T extends Component> T named(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = find(container, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        throw new AssertionError("컴포넌트를 찾지 못했습니다: " + name);
    }

    private static <T extends Component> T find(Container parent, String name, Class<T> type) {
        for (Component child : parent.getComponents()) {
            if (name.equals(child.getName())) {
                return type.cast(child);
            }
            if (child instanceof Container container) {
                T result = find(container, name, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
