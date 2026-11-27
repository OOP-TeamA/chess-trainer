package com.oopteama.chess.service;

import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.ai.MateSolver;
import com.oopteama.chess.data.JsonStorage;
import com.oopteama.chess.data.Profile;
import com.oopteama.chess.data.Settings;
import com.oopteama.chess.data.Storage;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.training.Lesson;
import com.oopteama.chess.training.LessonSession;
import com.oopteama.chess.training.Puzzle;
import com.oopteama.chess.training.PuzzleSession;
import com.oopteama.chess.training.TrainingSession.AttemptResult;
import com.oopteama.chess.training.TrainingSession.Outcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 레슨·퍼즐 진행과 프로필·설정 저장을 ChessBackend 창구를 통해 검증합니다.
 */
class ChessBackendTest {

    @Test
    void everyBundledLessonCanBeCompleted(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        assertFalse(backend.lessons(Lesson.Category.TUTORIAL).isEmpty());
        assertFalse(backend.lessons(Lesson.Category.OPENING).isEmpty());

        for (Lesson lesson : backend.lessons()) {
            LessonSession session = backend.startLesson(lesson);
            for (int i = 0; i < session.stepCount(); i++) {
                String where = lesson.id() + " " + (i + 1) + "단계";
                Move solution = session.currentStep().primarySolution(session.board())
                        .orElseThrow(() -> new AssertionError("정답 없음: " + where));
                AttemptResult result = session.submitMove(solution);
                assertEquals(Outcome.SOLVED, result.outcome(), where);
                if (result.autoReply() != null) {
                    assertTrue(session.applyPendingReply(), where);
                }
                session.nextStep();
            }
            assertTrue(session.isAllCompleted(), lesson.id());
        }
    }

    @Test
    void everyBundledPuzzleCanBeSolved(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        MateSolver solver = new MateSolver();
        assertEquals(16, backend.puzzles().size());

        for (Puzzle puzzle : backend.puzzles()) {
            PuzzleSession session = backend.startPuzzle(puzzle);
            if (puzzle.isMatePuzzle()) {
                for (int step = 0; step < puzzle.mateIn() && !session.isSolved(); step++) {
                    Move mateMove = solver.findMateIn(session.board(), session.remainingMoves())
                            .orElseThrow(() -> new AssertionError("메이트를 찾지 못함: " + puzzle.id()));
                    AttemptResult result = session.submitMove(mateMove);
                    if (result.outcome() == Outcome.CORRECT_STEP) {
                        assertNotNull(result.autoReply(), puzzle.id());
                        assertTrue(session.applyPendingReply(), puzzle.id());
                    }
                }
                assertTrue(session.isSolved(), puzzle.id());
                assertTrue(Rules.isCheckmate(session.board()), puzzle.id());
            } else {
                List<Move> solution = puzzle.solution();
                for (int i = 0; i < solution.size(); i += 2) {
                    AttemptResult result = session.submitMove(solution.get(i));
                    if (i == solution.size() - 1) {
                        assertEquals(Outcome.SOLVED, result.outcome(), puzzle.id());
                    } else {
                        assertEquals(Outcome.CORRECT_STEP, result.outcome(), puzzle.id());
                        assertEquals(solution.get(i + 1), result.autoReply(), puzzle.id());
                        assertTrue(session.applyPendingReply(), puzzle.id());
                    }
                }
                assertTrue(session.isSolved(), puzzle.id());
            }
        }
    }

    @Test
    void matePuzzleAcceptsOtherMatingLinesAndUndo(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        Puzzle ladder = backend.puzzles().stream().filter(p -> p.id().equals("p05")).findFirst().orElseThrow();

        // 정답 수순(b2b7)과 다른 a1a7로도 2수 안에 메이트할 수 있습니다.
        PuzzleSession other = backend.startPuzzle(ladder);
        assertEquals(Outcome.CORRECT_STEP, other.submitMove(Move.of("a1a7")).outcome());
        assertTrue(other.isFeedbackPositive());
        assertTrue(other.applyPendingReply());
        assertEquals(Outcome.SOLVED, other.submitMove(Move.of("b2b8")).outcome());
        assertTrue(Rules.isCheckmate(other.board()));

        // 메이트로 이어지지 않는 수도 둘 수 있고, 무르기로 되돌릴 수 있습니다.
        PuzzleSession explore = backend.startPuzzle(ladder);
        assertEquals(Outcome.CORRECT_STEP, explore.submitMove(Move.of("a1a2")).outcome());
        assertFalse(explore.isFeedbackPositive());
        assertTrue(explore.applyPendingReply());
        assertTrue(explore.undo());
        assertEquals(ladder.startBoard().toFen(), explore.board().toFen());
    }

    @Test
    void profilesAndSettingsSurviveRestart(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        String firstId = backend.activeProfile().id();
        backend.completeLesson("tut-pawn");
        backend.recordPuzzle("p01", true);
        backend.recordAiResult(Difficulty.MEDIUM, 1);

        Profile second = backend.createProfile("민준");
        backend.completeLesson("open-italian");
        backend.updateSettings(s -> {
            s.setBoardTheme(Settings.BoardTheme.FOREST);
            s.setShowCoords(false);
            s.setShowMoveInsight(false);
            s.setAutoFlipLocal(true);
        });
        assertTrue(Files.exists(tempDir.resolve("userdata.json")));

        ChessBackend reloaded = new ChessBackend(new JsonStorage(tempDir));
        assertEquals(2, reloaded.profiles().size());
        assertEquals("민준", reloaded.activeProfile().name());
        assertTrue(reloaded.activeProfile().isLessonCompleted("open-italian"));
        assertEquals(10, reloaded.activeProfile().totalScore());
        assertEquals(Settings.BoardTheme.FOREST, reloaded.settings().boardTheme());
        assertFalse(reloaded.settings().showCoords());
        assertFalse(reloaded.settings().showMoveInsight());
        assertTrue(reloaded.settings().autoFlipLocal());

        reloaded.setActiveProfile(firstId);
        assertEquals(10 + 5 + 3, reloaded.activeProfile().totalScore());
        assertTrue(reloaded.deleteProfile(second.id()));
        assertFalse(reloaded.deleteProfile(firstId)); // 마지막 프로필은 지울 수 없음
    }

    @Test
    void backendWorksWithAnyStorageImplementation() {
        // Storage 인터페이스만 지키면 파일 없이 메모리에 저장하는 구현으로도 동작합니다.
        Storage memoryStorage = new Storage() {
            private UserData saved = new UserData(null, new Settings(), List.of());

            @Override
            public List<Lesson> loadLessons() {
                return List.of();
            }

            @Override
            public List<Puzzle> loadPuzzles() {
                return List.of();
            }

            @Override
            public UserData loadUserData() {
                return saved;
            }

            @Override
            public void saveUserData(UserData data) {
                saved = data;
            }
        };
        ChessBackend backend = new ChessBackend(memoryStorage);
        backend.createProfile("테스터");
        assertEquals("테스터", backend.activeProfile().name());
        assertEquals(2, backend.profiles().size());
    }
}
