package com.oopteama.chess.ui.controller;

import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.training.Puzzle;
import com.oopteama.chess.training.PuzzleSession;
import com.oopteama.chess.training.TrainingSession.AttemptResult;
import com.oopteama.chess.training.TrainingSession.Outcome;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.BoardPanel;
import com.oopteama.chess.ui.component.RetryDialog;
import com.oopteama.chess.ui.screen.PuzzleScreen;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.util.ArrayList;
import java.util.List;

/**
 * 퍼즐 화면의 컨트롤러입니다.
 * 필터·목록·버튼·체스판 이벤트를 받아 PuzzleSession을 진행하고, 결과를 PuzzleScreen에 보여 줍니다.
 */
public final class PuzzleController implements ScreenController {

    private static final String ALL = "전체 퍼즐";
    private static final String MATE = "체크메이트";
    private static final String TACTICS = "전술 문제";
    private static final int REPLY_DELAY_MS = 400;

    private final ChessBackend backend;
    private final PuzzleScreen screen;
    private final BoardController board;

    private List<Puzzle> visiblePuzzles = List.of();
    private PuzzleSession session;
    private boolean recordedThisAttempt; // 이번 풀이에서 기록을 이미 남겼는지 (실패는 한 번만 기록)
    private Timer replyTimer;

    public PuzzleController(ChessBackend backend) {
        this.backend = backend;
        this.screen = new PuzzleScreen(backend.settings(), List.of(ALL, MATE, TACTICS));
        this.board = new BoardController(screen.boardPanel());
        board.setMoveListener(this::onUserMove);
        board.setIllegalMoveListener(this::onIllegalMove);

        screen.filterBox().addActionListener(e -> applyFilter());
        screen.hintButton().addActionListener(e -> showHint());
        screen.undoButton().addActionListener(e -> undo());
        screen.resetButton().addActionListener(e -> restart());
        screen.nextButton().addActionListener(e -> selectNextPuzzle());
        applyFilter();
    }

    /**
     * 풀기 전에 답을 짐작할 수 없도록 주제를 숨긴 제목입니다. 예) "05. 2수 안에 체크메이트"
     * 홈 화면의 추천 카드에서도 씁니다.
     */
    public static String neutralTitle(List<Puzzle> allPuzzles, Puzzle puzzle) {
        int number = allPuzzles.indexOf(puzzle) + 1;
        String goal = puzzle.isMatePuzzle() ? puzzle.mateIn() + "수 안에 체크메이트" : "최선의 수 찾기";
        return "%02d. %s".formatted(number, goal);
    }

    // ---- 퍼즐 고르기 ----

    /** 필터에 맞는 퍼즐만 목록에 보여 주고, 아직 안 푼 첫 퍼즐을 엽니다. */
    private void applyFilter() {
        String filter = (String) screen.filterBox().getSelectedItem();
        visiblePuzzles = new ArrayList<>();
        for (Puzzle puzzle : backend.puzzles()) {
            if (ALL.equals(filter) || puzzle.isMatePuzzle() == MATE.equals(filter)) {
                visiblePuzzles.add(puzzle);
            }
        }
        if (visiblePuzzles.isEmpty()) {
            refreshList();
            return;
        }
        Puzzle firstUnsolved = visiblePuzzles.stream()
                .filter(puzzle -> !backend.activeProfile().isPuzzleSolved(puzzle.id()))
                .findFirst()
                .orElse(visiblePuzzles.getFirst());
        selectPuzzle(firstUnsolved);
    }

    private void selectPuzzle(Puzzle puzzle) {
        stopReplyTimer();
        session = backend.startPuzzle(puzzle);
        recordedThisAttempt = false;
        refreshList();
        refresh();
    }

    private void selectNextPuzzle() {
        if (visiblePuzzles.isEmpty() || session == null) {
            return;
        }
        int index = visiblePuzzles.indexOf(session.puzzle());
        selectPuzzle(visiblePuzzles.get((index + 1) % visiblePuzzles.size()));
    }

    private void refreshList() {
        String currentId = session == null ? "" : session.puzzle().id();
        screen.showPuzzleList(visiblePuzzles, currentId, backend.activeProfile()::isPuzzleSolved,
                puzzle -> neutralTitle(backend.puzzles(), puzzle), this::selectPuzzle);
    }

    // ---- 버튼 ----

    private void showHint() {
        if (session != null) {
            session.showHint();
            refresh();
        }
    }

    private void undo() {
        if (session != null && !session.hasPendingReply() && session.undo()) {
            stopReplyTimer();
            refresh();
        }
    }

    private void restart() {
        stopReplyTimer();
        if (session != null) {
            session.reset();
            recordedThisAttempt = false;
            refresh();
        }
    }

    // ---- 체스판 ----

    private void onIllegalMove(Position from, Position to) {
        if (session == null) {
            return;
        }
        screen.boardPanel().flashWrong(to);
        RetryDialog.show(screen.boardPanel(), "이 칸으로는 이동할 수 없어요.",
                RetryDialog.illegalMoveReason(session.board(), from, to, backend.settings().showLegalMoves()));
    }

    /** 수를 두었을 때: 채점하고, 풀었는지·틀렸는지 기록한 뒤 상대 응수를 예약합니다. */
    private void onUserMove(Move move) {
        if (session == null) {
            return;
        }
        AttemptResult result = session.submitMove(move);
        if (result.outcome() == Outcome.WRONG) {
            if (!recordedThisAttempt) {
                backend.recordPuzzle(session.puzzle().id(), false);
                recordedThisAttempt = true;
            }
            screen.boardPanel().flashWrong(move.to());
            refresh();
            RetryDialog.show(screen.boardPanel(), "퍼즐 목표와 다른 수예요.", result.message());
            return;
        }
        if (result.outcome() == Outcome.SOLVED) {
            backend.recordPuzzle(session.puzzle().id(), true);
            recordedThisAttempt = true;
            refreshList();
        }
        refresh();
        if (result.autoReply() != null) {
            scheduleReply();
        }
    }

    private void scheduleReply() {
        board.setInteractive(false);
        stopReplyTimer();
        replyTimer = new Timer(REPLY_DELAY_MS, e -> {
            session.applyPendingReply();
            refresh();
        });
        replyTimer.setRepeats(false);
        replyTimer.start();
    }

    private void stopReplyTimer() {
        if (replyTimer != null) {
            replyTimer.stop();
            replyTimer = null;
        }
    }

    /** 세션 상태를 화면에 그대로 옮깁니다. */
    private void refresh() {
        if (session == null) {
            return;
        }
        Puzzle puzzle = session.puzzle();
        String turn = puzzle.startBoard().turn().label();
        String turnText = puzzle.isMatePuzzle()
                ? turn + " 차례 · 남은 수 " + session.remainingMoves() + " / " + puzzle.mateIn()
                : turn + " 차례 · 수순 " + session.currentUserMoveNumber() + "/" + session.totalUserMoves();
        String badge = puzzle.isMatePuzzle() ? puzzle.mateIn() + "수 메이트 · 자유 풀이" : TACTICS;
        screen.showHeader(badge, neutralTitle(backend.puzzles(), puzzle), turnText);

        boolean canUndo = session.canUndo() && !session.hasPendingReply();
        if (session.isSolved()) {
            screen.showExplanation("해설 · " + puzzle.title() + "\n\n" + puzzle.explanation(), puzzle.explanation());
        } else if (puzzle.isMatePuzzle()) {
            String nextAction = session.hasPendingReply() ? "상대의 응수를 기다리는 중이에요. 응수가 끝나면 다음 수를 두세요."
                    : canUndo ? "이전 차례로 돌아가려면 [무르기]를 눌러 주세요."
                    : "오답인 수는 보드에 반영되지 않아요. 현재 국면에서 다른 수를 둘 수 있어요.";
            screen.showExplanation(turn + " 차례입니다.\n\n" + puzzle.mateIn()
                    + "수 안에 체크메이트해 보세요. 어떤 수순이든 괜찮아요.\n\n" + nextAction, null);
        } else {
            screen.showExplanation(turn + "의 최선의 전술 수를 찾아 보드에서 직접 두어 보세요.", null);
        }
        screen.showFeedback(session.feedback(), session.isFeedbackPositive());
        screen.showUndo(canUndo, canUndo ? "무르기 · 직전 차례로 돌아가요."
                : session.hasPendingReply() ? "상대의 응수를 기다리는 중이에요."
                : "현재 무를 수 있는 수가 없어요.");

        BoardPanel panel = screen.boardPanel();
        panel.setFlipped(session.orientation() == Side.BLACK);
        panel.setBoard(session.board());
        panel.setLastMove(session.lastMove());
        panel.setHighlights(session.highlightSquares());
        panel.setArrow(session.hintArrow(), Theme.BOARD_INFO);
        board.setInteractive(!session.isSolved() && !session.hasPendingReply());
    }

    // ---- ScreenController ----

    @Override
    public JComponent view() {
        return screen;
    }

    @Override
    public void onShow() {
        refreshList();
        refresh();
        if (session != null && session.hasPendingReply()) {
            scheduleReply(); // 응수를 기다리던 중에 화면을 떠났다면 멈췄던 응수를 다시 예약
        }
    }

    @Override
    public void onHide() {
        stopReplyTimer();
    }

    @Override
    public void onDataChanged() {
        screen.boardPanel().repaint();
    }
}
