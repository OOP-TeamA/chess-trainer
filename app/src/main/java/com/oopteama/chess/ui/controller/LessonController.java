package com.oopteama.chess.ui.controller;

import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Side;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.training.Lesson;
import com.oopteama.chess.training.LessonSession;
import com.oopteama.chess.training.TrainingSession.AttemptResult;
import com.oopteama.chess.training.TrainingSession.Outcome;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.BoardPanel;
import com.oopteama.chess.ui.component.RetryDialog;
import com.oopteama.chess.ui.screen.LessonScreen;

import javax.swing.JComponent;
import javax.swing.Timer;
import java.util.List;

/**
 * 레슨 화면의 컨트롤러입니다.
 * 탭·레슨 목록·버튼·체스판 이벤트를 받아 LessonSession을 진행하고, 결과를 LessonScreen에 보여 줍니다.
 */
public final class LessonController implements ScreenController {

    private static final int REPLY_DELAY_MS = 420;

    private final ChessBackend backend;
    private final LessonScreen screen;
    private final BoardController board;

    private Lesson.Category category = Lesson.Category.TUTORIAL;
    private LessonSession session;
    private Timer replyTimer;
    private boolean lessonFinished;

    public LessonController(ChessBackend backend) {
        this.backend = backend;
        this.screen = new LessonScreen(backend.settings());
        this.board = new BoardController(screen.boardPanel());
        board.setMoveListener(this::onUserMove);
        board.setIllegalMoveListener(this::onIllegalMove);

        screen.tutorialTab().addActionListener(e -> switchCategory(Lesson.Category.TUTORIAL));
        screen.openingTab().addActionListener(e -> switchCategory(Lesson.Category.OPENING));
        screen.hintButton().addActionListener(e -> showHint());
        screen.resetButton().addActionListener(e -> resetStep());
        screen.prevButton().addActionListener(e -> previousStep());
        screen.nextButton().addActionListener(e -> nextStepOrFinish());

        switchCategory(Lesson.Category.TUTORIAL);
    }

    // ---- 레슨 고르기 ----

    /** 탭을 바꾸고, 그 탭에서 아직 안 끝낸 첫 레슨을 엽니다. */
    private void switchCategory(Lesson.Category newCategory) {
        stopReplyTimer();
        category = newCategory;
        screen.showCategory(newCategory);
        List<Lesson> lessons = backend.lessons(newCategory);
        if (!lessons.isEmpty()) {
            Lesson firstUnfinished = lessons.stream()
                    .filter(lesson -> !backend.activeProfile().isLessonCompleted(lesson.id()))
                    .findFirst()
                    .orElse(lessons.getFirst());
            selectLesson(firstUnfinished);
        }
        refreshList();
    }

    private void selectLesson(Lesson lesson) {
        stopReplyTimer();
        lessonFinished = false;
        session = backend.startLesson(lesson);
        refreshList();
        refresh();
    }

    private void refreshList() {
        String currentId = session == null ? "" : session.lesson().id();
        screen.showLessonList(backend.lessons(category), currentId,
                backend.activeProfile()::isLessonCompleted, this::selectLesson);
    }

    // ---- 버튼 ----

    private void showHint() {
        if (session != null) {
            session.showHint();
            refresh();
        }
    }

    private void resetStep() {
        stopReplyTimer();
        if (session != null) {
            lessonFinished = false;
            session.reset();
            refresh();
        }
    }

    private void previousStep() {
        stopReplyTimer();
        if (session != null && session.hasPrevStep()) {
            session.prevStep();
            refresh();
        }
    }

    /** 다음 단계로 가거나, 마지막 단계면 레슨을 마치고 목록을 펼쳐 다른 레슨을 고르게 합니다. */
    private void nextStepOrFinish() {
        stopReplyTimer();
        if (session == null) {
            return;
        }
        if (session.hasNextStep()) {
            session.nextStep();
        } else {
            lessonFinished = true;
            screen.openLessonList();
        }
        refresh();
    }

    // ---- 체스판 ----

    /** 규칙상 갈 수 없는 칸을 골랐을 때: 이유를 카드로 알려 줍니다. */
    private void onIllegalMove(Position from, Position to) {
        if (session == null) {
            return;
        }
        screen.boardPanel().flashWrong(to);
        RetryDialog.show(screen.boardPanel(), "이 칸으로는 이동할 수 없어요.",
                RetryDialog.illegalMoveReason(session.board(), from, to, backend.settings().showLegalMoves()));
    }

    /** 수를 두었을 때: 채점하고, 정답이면 기록을 저장하고 상대 응수를 예약합니다. */
    private void onUserMove(Move move) {
        if (session == null) {
            return;
        }
        AttemptResult result = session.submitMove(move);
        if (result.outcome() == Outcome.WRONG) {
            screen.boardPanel().flashWrong(move.to());
            refresh();
            RetryDialog.show(screen.boardPanel(), "레슨 목표와 다른 수예요.", result.message());
            return;
        }
        if (session.isAllCompleted()) {
            backend.completeLesson(session.lesson().id());
            refreshList();
        }
        refresh();
        if (result.autoReply() != null) {
            scheduleReply();
        }
    }

    /** 잠시 뒤 상대의 응수를 보드에 둡니다. */
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
        if (session.hasNextStep() || !session.isSolved()) {
            lessonFinished = false;
        }
        screen.showStep(session.lesson().title(), session.stepIndex(), session.stepCount(),
                session.isSolved(), session.currentStep().prompt());
        String feedback = lessonFinished ? "레슨 완료! 목록에서 다른 레슨을 고를 수 있어요." : session.feedback();
        screen.showFeedback(feedback, session.isFeedbackPositive());
        String nextText = session.hasNextStep() ? "다음 단계 →" : lessonFinished ? "완료됨" : "레슨 완료";
        screen.showNavigation(session.hasPrevStep(),
                !lessonFinished && session.isSolved() && !session.hasPendingReply(), nextText);

        BoardPanel panel = screen.boardPanel();
        panel.setFlipped(session.orientation() == Side.BLACK);
        panel.setBoard(session.board());
        panel.setLastMove(session.lastMove());
        panel.setHighlights(session.highlightSquares());
        panel.setArrow(session.hintArrow(), Theme.BOARD_ACCENT);
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
