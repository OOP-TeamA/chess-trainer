package com.oopteama.chess.model;

import com.oopteama.chess.engine.Board;
import com.oopteama.chess.engine.Move;
import com.oopteama.chess.engine.Piece.Side;

import java.util.Objects;

/**
 * 레슨(기본 튜토리얼 · 유명 오프닝) 단계 진행 세션입니다.
 */
public final class LessonSession extends TrainingSession {

    private final Lesson lesson;
    private final boolean[] stepDone;
    private int stepIndex;

    public LessonSession(Lesson lesson) {
        this.lesson = Objects.requireNonNull(lesson, "lesson");
        this.stepDone = new boolean[lesson.steps().size()];
        loadStep(0);
    }

    public Lesson lesson() {
        return lesson;
    }

    public int stepIndex() {
        return stepIndex;
    }

    public int stepCount() {
        return lesson.steps().size();
    }

    public Lesson.Step currentStep() {
        return lesson.steps().get(stepIndex);
    }

    public boolean hasNextStep() {
        return stepIndex + 1 < stepCount();
    }

    public boolean hasPrevStep() {
        return stepIndex > 0;
    }

    public boolean isAllCompleted() {
        for (boolean done : stepDone) {
            if (!done) {
                return false;
            }
        }
        return true;
    }

    public void nextStep() {
        if (hasNextStep()) {
            loadStep(stepIndex + 1);
        }
    }

    public void prevStep() {
        if (hasPrevStep()) {
            loadStep(stepIndex - 1);
        }
    }

    private void loadStep(int index) {
        this.stepIndex = index;
        Lesson.Step step = lesson.steps().get(index);
        this.currentBoard = step.startBoard();
        this.solved = stepDone[index];
        this.wrongCount = 0;
        this.lastMove = null;
        this.hintArrow = null;
        this.pendingReply = null;
        this.highlightSquares = step.hintSquares();
        this.feedback = solved ? step.successText() : "";
        this.feedbackPositive = solved;
    }

    @Override
    public AttemptResult submitMove(Move move) {
        if (solved || pendingReply != null) {
            return new AttemptResult(Outcome.SOLVED, feedback, null);
        }
        if (!currentBoard.legalMoves().contains(move)) {
            return new AttemptResult(Outcome.WRONG, "규칙상 둘 수 없는 수입니다.", null);
        }

        Lesson.Step step = currentStep();
        Board after = currentBoard.applyMove(move);
        if (step.matches(currentBoard, after, move)) {
            currentBoard = after;
            lastMove = move;
            solved = true;
            stepDone[stepIndex] = true;
            hintArrow = null;
            highlightSquares = java.util.List.of();
            feedback = step.successText();
            feedbackPositive = true;
            pendingReply = step.opponentReply();
            return new AttemptResult(Outcome.SOLVED, feedback, pendingReply);
        }

        applyWrongFeedback(step.primarySolution(currentBoard), step.hintSquares());
        return new AttemptResult(Outcome.WRONG, feedback, null);
    }

    @Override
    public void showHint() {
        Lesson.Step step = currentStep();
        revealHint(step.primarySolution(currentBoard), step.hintSquares());
    }

    @Override
    public void reset() {
        stepDone[stepIndex] = false;
        loadStep(stepIndex);
    }

    @Override
    public Side orientation() {
        return lesson.playerSide();
    }
}
