package com.oopteama.chess.training;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.Side;

import java.util.List;
import java.util.Optional;

/**
 * 레슨과 퍼즐의 공통 부모 클래스입니다.
 * 보드 상태, 피드백 문구, 힌트, 상대 자동 응수처럼 두 훈련 모드가 함께 쓰는 기능을 담습니다.
 * 수를 채점하는 방법(submitMove)은 레슨과 퍼즐이 각자 다르게 구현합니다.
 */
public abstract class TrainingSession {

    /** 사용자가 둔 수의 결과. */
    public enum Outcome {
        CORRECT_STEP, // 맞았고 다음 수가 남음
        SOLVED,       // 문제(단계)를 다 풂
        WRONG         // 틀림
    }

    /** 채점 결과: 결과 종류, 보여 줄 문구, 상대가 이어서 둘 수(없으면 null). */
    public record AttemptResult(Outcome outcome, String message, Move autoReply) {
    }

    protected Board currentBoard;
    protected boolean solved;
    protected int wrongCount;
    protected String feedback = "";
    protected boolean feedbackPositive;
    protected Move lastMove;
    protected Move hintArrow;
    protected List<Position> highlightSquares = List.of();
    protected Move pendingReply;

    /** 사용자가 둔 수를 채점합니다. */
    public abstract AttemptResult submitMove(Move move);

    /** 힌트를 한 단계 보여 줍니다. */
    public abstract void showHint();

    /** 처음 상태로 되돌립니다. */
    public abstract void reset();

    /** 보드를 어느 진영 기준으로 보여 줄지. */
    public abstract Side orientation();

    /** 예약된 상대 응수가 있으면 보드에 반영하고 true를 돌려줍니다. */
    public boolean applyPendingReply() {
        if (pendingReply == null) {
            return false;
        }
        Move reply = pendingReply;
        pendingReply = null;
        if (Rules.legalMoves(currentBoard).contains(reply)) {
            currentBoard = currentBoard.apply(reply);
            lastMove = reply;
            return true;
        }
        return false;
    }

    /** 틀린 횟수에 따라 점점 자세한 힌트를 줍니다: 1번째 격려 → 2번째 칸 강조 → 3번째부터 정답 화살표. */
    protected void applyWrongFeedback(Optional<Move> solution, List<Position> stepHints) {
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

    /** [힌트] 버튼: 처음에는 칸 강조, 다음에는 정답 화살표를 보여 줍니다. */
    protected void revealHint(Optional<Move> solution, List<Position> stepHints) {
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
                feedback = "정답 힌트: " + m.from() + " → " + m.to();
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

    public List<Position> highlightSquares() {
        return highlightSquares;
    }

    public boolean hasPendingReply() {
        return pendingReply != null;
    }
}
