package com.oopteama.chess.model;

import com.oopteama.chess.engine.ChessAI;
import com.oopteama.chess.engine.ChessAI.Difficulty;
import com.oopteama.chess.engine.Move;
import com.oopteama.chess.model.TrainingSession.Outcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AppModelTest {

    @Test
    void allBundledLessonsAndOpeningsSolveCleanly(@TempDir Path tempDir) {
        AppStore store = new AppStore(tempDir);
        List<Lesson> tutorials = store.lessons(Lesson.Category.TUTORIAL);
        List<Lesson> openings = store.lessons(Lesson.Category.OPENING);

        assertFalse(tutorials.isEmpty(), "기본 튜토리얼이 로드되어야 합니다");
        assertFalse(openings.isEmpty(), "유명 오프닝이 로드되어야 합니다");

        for (Lesson lesson : store.lessons()) {
            LessonSession session = new LessonSession(lesson);
            for (int i = 0; i < session.stepCount(); i++) {
                int stepIdx = i;
                Move sol = session.currentStep().primarySolution(session.board())
                        .orElseThrow(() -> new AssertionError("정답 수를 찾을 수 없음: " + lesson.id() + " step " + stepIdx));
                TrainingSession.AttemptResult res = session.submitMove(sol);
                assertEquals(Outcome.SOLVED, res.outcome(), "레슨 단계 풀이 실패: " + lesson.id() + " step " + i);
                if (res.autoReply() != null) {
                    assertTrue(session.applyPendingReply(), "오프닝 상대 응수 적용 실패: " + lesson.id() + " step " + i);
                }
                if (session.hasNextStep()) {
                    session.nextStep();
                }
            }
            assertTrue(session.isAllCompleted(), "레슨 전체 완료 실패: " + lesson.id());
        }
    }

    @Test
    void allBundledPuzzlesSolveCleanly(@TempDir Path tempDir) {
        AppStore store = new AppStore(tempDir);
        ChessAI ai = new ChessAI();
        assertFalse(store.puzzles().isEmpty(), "퍼즐이 로드되어야 합니다");

        for (Puzzle puzzle : store.puzzles()) {
            PuzzleSession session = new PuzzleSession(puzzle);
            if (puzzle.isMatePuzzle()) {
                // 자유 메이트 퍼즐: 지정 수(mateIn) 이내 강제 메이트가 실제로 존재하며 동적 AI 방어 상대로 해결 가능함을 증명
                for (int step = 0; step < puzzle.mateIn(); step++) {
                    Move mateMove = ai.findMateIn(session.board(), session.remainingMoves())
                            .orElseThrow(() -> new AssertionError(
                                    "지정 수 안에 강제 메이트를 찾을 수 없음: " + puzzle.id()
                                            + " (남은 수 " + session.remainingMoves() + ", FEN=" + session.board().toFen() + ")"
                            ));
                    TrainingSession.AttemptResult res = session.submitMove(mateMove);
                    if (res.outcome() == Outcome.SOLVED) {
                        break;
                    }
                    assertEquals(Outcome.CORRECT_STEP, res.outcome(), "자유 메이트 진행 실패: " + puzzle.id());
                    assertNotNull(res.autoReply(), "AI 방어 응수 누락: " + puzzle.id());
                    assertTrue(session.applyPendingReply());
                }
                assertTrue(session.isSolved(), "자유 메이트 퍼즐 해결 실패: " + puzzle.id());
                assertTrue(session.board().isCheckmate(), "메이트 퍼즐 종료 상태가 체크메이트가 아님: " + puzzle.id());
            } else {
                List<Move> sol = puzzle.solution();
                assertEquals(1, sol.size() % 2, "전술 퍼즐 수열은 홀수 길이여야 함: " + puzzle.id());
                for (int i = 0; i < sol.size(); i += 2) {
                    TrainingSession.AttemptResult res = session.submitMove(sol.get(i));
                    if (i == sol.size() - 1) {
                        assertEquals(Outcome.SOLVED, res.outcome(), "퍼즐 마지막 수 실패: " + puzzle.id());
                    } else {
                        assertEquals(Outcome.CORRECT_STEP, res.outcome(), "퍼즐 중간 수 실패: " + puzzle.id());
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
        AppStore store = new AppStore(tempDir);
        Puzzle ladder = store.puzzles().stream()
                .filter(p -> p.id().equals("p05"))
                .findFirst()
                .orElseThrow();

        // 1. 대표 정답(Rb7) 대신 대안 경로(Ra7 -> Kg8 -> Rb8#)로 자유 해결!
        PuzzleSession alt = new PuzzleSession(ladder);
        var r1 = alt.submitMove(Move.fromUci("a1a7"));
        assertEquals(Outcome.CORRECT_STEP, r1.outcome());
        assertTrue(alt.isFeedbackPositive());
        assertTrue(alt.applyPendingReply());
        assertEquals(1, alt.remainingMoves());

        var r2 = alt.submitMove(Move.fromUci("b2b8"));
        assertEquals(Outcome.SOLVED, r2.outcome());
        assertTrue(alt.board().isCheckmate());

        // 2. 비효율적인 수(a1a2)를 둬도 진행 후 무르기로 되돌려 다시 해결 가능
        PuzzleSession explore = new PuzzleSession(ladder);
        var bad = explore.submitMove(Move.fromUci("a1a2"));
        assertEquals(Outcome.CORRECT_STEP, bad.outcome());
        assertFalse(explore.isFeedbackPositive());
        assertTrue(explore.applyPendingReply());
        assertTrue(explore.canUndo());
        assertTrue(explore.undo());
        assertEquals(2, explore.remainingMoves());
        assertEquals(ladder.startBoard().toFen(), explore.board().toFen());
    }

    @Test
    void puzzleFeedbackLadderAndWrongRollbackWork(@TempDir Path tempDir) {
        AppStore store = new AppStore(tempDir);
        Puzzle backRank = store.puzzles().getFirst(); // p01: d1d8
        PuzzleSession session = new PuzzleSession(backRank);

        // 1회 오답: 국면 유지, 힌트 없음
        var r1 = session.submitMove(Move.fromUci("d1d2"));
        assertEquals(Outcome.WRONG, r1.outcome());
        assertEquals(backRank.startBoard().toFen(), session.board().toFen());
        assertNull(session.hintArrow());

        // 2회 오답: 강조 칸 표시
        session.submitMove(Move.fromUci("d1d3"));
        assertFalse(session.highlightSquares().isEmpty());

        // 3회 오답: 정답 화살표 표시
        session.submitMove(Move.fromUci("d1d4"));
        assertEquals(Move.fromUci("d1d8"), session.hintArrow());
    }

    @Test
    void profilesAndSettingsPersistAcrossReload(@TempDir Path tempDir) {
        AppStore store = new AppStore(tempDir);
        Profile p1 = store.activeProfile();
        p1.markLessonCompleted("tut-pawn");
        p1.recordPuzzle("p01", true);
        p1.recordAiResult(Difficulty.MEDIUM, 1);

        Profile p2 = store.createProfile("민준");
        p2.markLessonCompleted("open-italian");

        store.settings().setBoardTheme(Settings.BoardTheme.FOREST);
        store.settings().setShowMovePreview(false);
        store.settings().setAutoFlipLocal(true);
        store.save();

        // 새로 로드해 영속성 검증
        AppStore reloaded = new AppStore(tempDir);
        assertEquals(2, reloaded.profiles().size());
        assertEquals("민준", reloaded.activeProfile().name());
        assertTrue(reloaded.activeProfile().isLessonCompleted("open-italian"));
        assertEquals(10, reloaded.activeProfile().totalScore());

        assertEquals(Settings.BoardTheme.FOREST, reloaded.settings().boardTheme());
        assertFalse(reloaded.settings().showMovePreview());
        assertTrue(reloaded.settings().autoFlipLocal());

        // 프로필 전환 및 삭제
        reloaded.setActiveProfile(p1.id());
        assertEquals(10 + 5 + 3, reloaded.activeProfile().totalScore());
        assertTrue(reloaded.deleteProfile(p2.id()));
        assertFalse(reloaded.deleteProfile(p1.id()), "마지막 프로필은 삭제할 수 없어야 함");
    }
}
