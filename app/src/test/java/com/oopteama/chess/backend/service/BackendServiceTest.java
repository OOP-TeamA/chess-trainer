package com.oopteama.chess.backend.service;

import com.oopteama.chess.backend.engine.ChessAI;
import com.oopteama.chess.backend.engine.ChessAI.Difficulty;
import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.training.Lesson;
import com.oopteama.chess.backend.training.LessonSession;
import com.oopteama.chess.backend.training.Puzzle;
import com.oopteama.chess.backend.training.PuzzleSession;
import com.oopteama.chess.backend.training.TrainingSession;
import com.oopteama.chess.backend.training.TrainingSession.Outcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BackendServiceTest {

    @Test
    void allBundledJsonLessonsAndOpeningsSolveCleanly(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        assertFalse(backend.lessons(Lesson.Category.TUTORIAL).isEmpty());
        assertFalse(backend.lessons(Lesson.Category.OPENING).isEmpty());

        for (Lesson lesson : backend.lessons()) {
            LessonSession session = backend.startLesson(lesson);
            for (int i = 0; i < session.stepCount(); i++) {
                int stepIdx = i;
                Move sol = session.currentStep().primarySolution(session.board())
                        .orElseThrow(() -> new AssertionError("정답 없음: " + lesson.id() + " step " + stepIdx));
                TrainingSession.AttemptResult res = session.submitMove(sol);
                assertEquals(Outcome.SOLVED, res.outcome(), "레슨 단계 실패: " + lesson.id() + " step " + stepIdx);
                if (res.autoReply() != null) {
                    assertTrue(session.applyPendingReply());
                }
                if (session.hasNextStep()) {
                    session.nextStep();
                }
            }
            assertTrue(session.isAllCompleted());
        }
    }

    @Test
    void allBundledJsonPuzzlesSolveCleanly(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        ChessAI ai = new ChessAI();
        assertEquals(16, backend.puzzles().size());

        for (Puzzle puzzle : backend.puzzles()) {
            PuzzleSession session = backend.startPuzzle(puzzle);
            if (puzzle.isMatePuzzle()) {
                for (int step = 0; step < puzzle.mateIn(); step++) {
                    Move mateMove = ai.findMateIn(session.board(), session.remainingMoves())
                            .orElseThrow(() -> new AssertionError("메이트 실패: " + puzzle.id()));
                    TrainingSession.AttemptResult res = session.submitMove(mateMove);
                    if (res.outcome() == Outcome.SOLVED) break;
                    assertEquals(Outcome.CORRECT_STEP, res.outcome());
                    assertNotNull(res.autoReply());
                    assertTrue(session.applyPendingReply());
                }
                assertTrue(session.isSolved());
                assertTrue(session.board().isCheckmate());
            } else {
                List<Move> sol = puzzle.solution();
                for (int i = 0; i < sol.size(); i += 2) {
                    TrainingSession.AttemptResult res = session.submitMove(sol.get(i));
                    if (i == sol.size() - 1) {
                        assertEquals(Outcome.SOLVED, res.outcome());
                    } else {
                        assertEquals(Outcome.CORRECT_STEP, res.outcome());
                        assertEquals(sol.get(i + 1), res.autoReply());
                        assertTrue(session.applyPendingReply());
                    }
                }
                assertTrue(session.isSolved());
            }
        }
    }

    @Test
    void freeMatePuzzleAllowsAlternativePathsAndUndo(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        Puzzle ladder = backend.puzzles().stream()
                .filter(p -> p.id().equals("p05"))
                .findFirst()
                .orElseThrow();

        PuzzleSession alt = backend.startPuzzle(ladder);
        assertEquals(Outcome.CORRECT_STEP, alt.submitMove(Move.fromUci("a1a7")).outcome());
        assertTrue(alt.isFeedbackPositive());
        assertTrue(alt.applyPendingReply());
        assertEquals(Outcome.SOLVED, alt.submitMove(Move.fromUci("b2b8")).outcome());
        assertTrue(alt.board().isCheckmate());

        PuzzleSession explore = backend.startPuzzle(ladder);
        assertEquals(Outcome.CORRECT_STEP, explore.submitMove(Move.fromUci("a1a2")).outcome());
        assertFalse(explore.isFeedbackPositive());
        assertTrue(explore.applyPendingReply());
        assertTrue(explore.undo());
        assertEquals(ladder.startBoard().toFen(), explore.board().toFen());
    }

    @Test
    void jsonPersistenceRoundTripsProfilesAndSettings(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        String firstId = backend.activeProfile().id();
        backend.completeLesson("tut-pawn");
        backend.recordPuzzle("p01", true);
        backend.recordAiResult(Difficulty.MEDIUM, 1);

        Profile p2 = backend.createProfile("민준");
        backend.completeLesson("open-italian");
        backend.updateSettings(s -> {
            s.setBoardTheme(Settings.BoardTheme.FOREST);
            s.setShowMovePreview(false);
            s.setAutoFlipLocal(true);
        });

        assertTrue(Files.exists(tempDir.resolve("userdata.json")));

        ChessBackend reloaded = new ChessBackend(new JsonStorage(tempDir));
        assertEquals(2, reloaded.profiles().size());
        assertEquals("민준", reloaded.activeProfile().name());
        assertTrue(reloaded.activeProfile().isLessonCompleted("open-italian"));
        assertEquals(10, reloaded.activeProfile().totalScore());
        assertEquals(Settings.BoardTheme.FOREST, reloaded.settings().boardTheme());
        assertFalse(reloaded.settings().showMovePreview());
        assertTrue(reloaded.settings().autoFlipLocal());

        reloaded.setActiveProfile(firstId);
        assertEquals(10 + 5 + 3, reloaded.activeProfile().totalScore());
        assertTrue(reloaded.deleteProfile(p2.id()));
        assertFalse(reloaded.deleteProfile(firstId));
    }

    @Test
    void backendSupportsInMemoryStorageDip() {
        // DIP 검증: 파일 I/O 없는 가짜 Storage로도 ChessBackend가 완전히 동작
        Storage fakeStorage = new Storage() {
            private UserData saved = new UserData(null, new Settings(), List.of());

            @Override public List<Lesson> loadLessons() { return List.of(); }
            @Override public List<Puzzle> loadPuzzles() { return List.of(); }
            @Override public UserData loadUserData() { return saved; }
            @Override public void saveUserData(UserData data) { saved = data; }
        };
        ChessBackend backend = new ChessBackend(fakeStorage);
        backend.createProfile("테스터");
        assertEquals("테스터", backend.activeProfile().name());
        assertEquals(2, backend.profiles().size());
    }
}
