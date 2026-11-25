package com.oopteama.chess.training;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.Side;

import java.util.List;
import java.util.Objects;

/**
 * 레슨 진행 세션입니다. 단계마다 목표 수를 두면 다음 단계로 넘어갈 수 있습니다.
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

    public LessonStep currentStep() {
        return lesson.steps().get(stepIndex);
    }

    public boolean hasNextStep() {
        return stepIndex + 1 < stepCount();
    }

    public boolean hasPrevStep() {
        return stepIndex > 0;
    }

    /** 모든 단계를 풀었는지. */
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
        stepIndex = index;
        LessonStep step = lesson.steps().get(index);
        currentBoard = step.startBoard();
        solved = stepDone[index];
        wrongCount = 0;
        lastMove = null;
        hintArrow = null;
        pendingReply = null;
        highlightSquares = step.hintSquares();
        feedback = solved ? step.successText() : "";
        feedbackPositive = solved;
    }

    @Override
    public AttemptResult submitMove(Move move) {
        if (solved || pendingReply != null) {
            return new AttemptResult(Outcome.SOLVED, feedback, null);
        }
        if (!Rules.legalMoves(currentBoard).contains(move)) {
            return new AttemptResult(Outcome.WRONG, "규칙상 둘 수 없는 수입니다.", null);
        }

        LessonStep step = currentStep();
        Board after = currentBoard.apply(move);
        if (step.isGoalMet(after, move)) {
            currentBoard = after;
            lastMove = move;
            solved = true;
            stepDone[stepIndex] = true;
            hintArrow = null;
            highlightSquares = List.of();
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
        LessonStep step = currentStep();
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
