package com.oopteama.chess.model;

import com.oopteama.chess.engine.Board;
import com.oopteama.chess.engine.Move;
import com.oopteama.chess.engine.Piece.Side;

import java.util.List;
import java.util.Optional;

/**
 * 레슨·퍼즐 등 보드 기반 훈련 세션의 공통 상태를 관리하는 추상 기반 클래스입니다.
 */
public abstract class TrainingSession {

    public enum Outcome {
        CORRECT_STEP,
        SOLVED,
        WRONG
    }

    public record AttemptResult(Outcome outcome, String message, Move autoReply) {
    }

    protected Board currentBoard;
    protected boolean solved;
    protected int wrongCount;
    protected String feedback = "";
    protected boolean feedbackPositive;
    protected Move lastMove;
    protected Move hintArrow;
    protected List<Integer> highlightSquares = List.of();
    protected Move pendingReply;

    public abstract AttemptResult submitMove(Move move);

    public abstract void showHint();

    public abstract void reset();

    public abstract Side orientation();

    /**
     * 자동 응수가 예약되어 있으면 보드에 반영하고 true를 반환합니다.
     */
    public boolean applyPendingReply() {
        if (pendingReply == null) {
            return false;
        }
        Move reply = pendingReply;
        pendingReply = null;
        if (currentBoard.legalMoves().contains(reply)) {
            currentBoard = currentBoard.applyMove(reply);
            lastMove = reply;
            return true;
        }
        return false;
    }

    protected void applyWrongFeedback(Optional<Move> solution, List<Integer> stepHints) {
        wrongCount++;
        feedbackPositive = false;
        if (wrongCount == 1) {
            feedback = "아쉽습니다! 다른 수를 생각해 보세요.";
        } else if (wrongCount == 2) {
            if (!stepHints.isEmpty()) {
                highlightSquares = stepHints;
            } else {
                solution.ifPresent(m -> highlightSquares = List.of(m.from()));
            }
            feedback = "힌트: 강조된 칸의 기물이나 목표 칸을 살펴보세요.";
        } else {
            solution.ifPresent(m -> {
                hintArrow = m;
                highlightSquares = List.of(m.from(), m.to());
            });
            feedback = "정답 힌트: 화살표 방향대로 두어 보세요!";
        }
    }

    protected void revealHint(Optional<Move> solution, List<Integer> stepHints) {
        if (solved) {
            return;
        }
        if (hintArrow == null && highlightSquares.isEmpty() && !stepHints.isEmpty()) {
            highlightSquares = stepHints;
            feedback = "힌트: 강조된 칸을 확인해 보세요.";
            feedbackPositive = false;
        } else {
            solution.ifPresent(m -> {
                hintArrow = m;
                highlightSquares = List.of(m.from(), m.to());
                feedback = "정답 힌트: " + Board.formatSquare(m.from()) + " → " + Board.formatSquare(m.to());
                feedbackPositive = false;
            });
        }
    }

    public Board board() {
        return currentBoard;
    }

    public boolean isSolved() {
        return solved;
    }

    public int wrongCount() {
        return wrongCount;
    }

    public String feedback() {
        return feedback;
    }

    public boolean isFeedbackPositive() {
        return feedbackPositive;
    }

    public Move lastMove() {
        return lastMove;
    }

    public Move hintArrow() {
        return hintArrow;
    }

    public List<Integer> highlightSquares() {
        return highlightSquares;
    }

    public boolean hasPendingReply() {
        return pendingReply != null;
    }
}
