package com.oopteama.chess.model;

import com.oopteama.chess.engine.Board;
import com.oopteama.chess.engine.ChessAI;
import com.oopteama.chess.engine.ChessGame;
import com.oopteama.chess.engine.Move;
import com.oopteama.chess.engine.Piece.Side;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 퍼즐 풀이 세션입니다.
 * - 자유 메이트 퍼즐(mateIn > 0): 지정 수 이내에 어떤 합법 수순으로든 체크메이트하면 정답이며,
 *   AI가 매 수 동적으로 최선 방어 응수를 둡니다.
 * - 전술 퍼즐(mateIn == 0): 정답 수열에 따라 상대 자동 응수와 함께 풀이합니다.
 */
public final class PuzzleSession extends TrainingSession {

    private record Snapshot(Board board, int plyIndex, int movesUsed, Move lastMove) {
    }

    private final Puzzle puzzle;
    private final ChessAI ai = new ChessAI();
    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private int plyIndex;
    private int movesUsed;
    private boolean attempted;

    public PuzzleSession(Puzzle puzzle) {
        this.puzzle = Objects.requireNonNull(puzzle, "puzzle");
        reset();
    }

    public Puzzle puzzle() {
        return puzzle;
    }

    public boolean isMatePuzzle() {
        return puzzle.isMatePuzzle();
    }

    public boolean wasAttempted() {
        return attempted;
    }

    public int totalUserMoves() {
        return puzzle.isMatePuzzle()
                ? puzzle.mateIn()
                : (puzzle.solution().size() + 1) / 2;
    }

    public int currentUserMoveNumber() {
        return puzzle.isMatePuzzle()
                ? Math.min(puzzle.mateIn(), movesUsed + 1)
                : Math.min(totalUserMoves(), plyIndex / 2 + 1);
    }

    public int remainingMoves() {
        return puzzle.isMatePuzzle()
                ? Math.max(0, puzzle.mateIn() - movesUsed)
                : Math.max(0, totalUserMoves() - plyIndex / 2);
    }

    public boolean canUndo() {
        return !undoStack.isEmpty() && !solved;
    }

    public boolean undo() {
        if (!canUndo()) {
            return false;
        }
        Snapshot prev = undoStack.pop();
        this.currentBoard = prev.board();
        this.plyIndex = prev.plyIndex();
        this.movesUsed = prev.movesUsed();
        this.lastMove = prev.lastMove();
        this.pendingReply = null;
        this.hintArrow = null;
        this.highlightSquares = List.of();
        this.feedbackPositive = false;
        this.feedback = "한 수 되돌렸습니다. 다른 수를 시도해 보세요.";
        return true;
    }

    @Override
    public AttemptResult submitMove(Move move) {
        if (solved || pendingReply != null) {
            return new AttemptResult(Outcome.SOLVED, feedback, null);
        }
        if (!currentBoard.legalMoves().contains(move)) {
            return new AttemptResult(Outcome.WRONG, "규칙상 둘 수 없는 수입니다.", null);
        }

        attempted = true;
        return puzzle.isMatePuzzle() ? submitFreeMate(move) : submitTactical(move);
    }

    private AttemptResult submitFreeMate(Move move) {
        Board after = currentBoard.applyMove(move);

        // 1. 체크메이트 달성 -> 수순에 상관없이 즉시 정답!
        if (after.isCheckmate()) {
            undoStack.push(new Snapshot(currentBoard, plyIndex, movesUsed, lastMove));
            currentBoard = after;
            lastMove = move;
            movesUsed++;
            solved = true;
            hintArrow = null;
            highlightSquares = List.of();
            feedbackPositive = true;
            feedback = "체크메이트! (" + movesUsed + "수 만에 해결) " + puzzle.explanation();
            return new AttemptResult(Outcome.SOLVED, feedback, null);
        }

        // 2. 무승부(스테일메이트·기물 부족) 방지
        if (after.isStalemate() || after.isInsufficientMaterial()) {
            wrongCount++;
            feedbackPositive = false;
            feedback = after.isStalemate()
                    ? "스테일메이트(무승부)가 됩니다! 상대 킹에게 체크를 걸어 마무리하세요."
                    : "기물 부족으로 무승부가 됩니다. 다른 수를 찾아보세요.";
            return new AttemptResult(Outcome.WRONG, feedback, null);
        }

        // 3. 마지막 남은 수였는데 메이트 실패 -> 직전 국면 유지 + 이유 안내
        int leftBefore = remainingMoves();
        if (leftBefore <= 1) {
            Optional<Move> dynamicHint = findBestHintMove();
            applyWrongFeedback(dynamicHint, List.of());
            if (wrongCount == 1 && after.isInCheck()) {
                List<Move> escapes = after.legalMoves();
                if (!escapes.isEmpty()) {
                    Move esc = escapes.getFirst();
                    String escSan = ChessGame.formatSan(after, esc, after.classifyMove(esc), escapes, false, false);
                    feedback = "체크이지만 메이트가 아닙니다! 상대가 " + escSan + " 등으로 빠져나갈 수 있습니다.";
                }
            }
            return new AttemptResult(Outcome.WRONG, feedback, null);
        }

        // 4. 남은 수가 있으면 어떤 합법 수든 자유롭게 진행 + AI가 최선 방어 응수 선택
        undoStack.push(new Snapshot(currentBoard, plyIndex, movesUsed, lastMove));
        currentBoard = after;
        lastMove = move;
        movesUsed++;
        hintArrow = null;
        highlightSquares = List.of();

        int leftAfter = remainingMoves();
        boolean stillForcedMate = ai.isForcedMateAfterMove(after, leftAfter);
        Move reply = ai.bestDefenseAgainstMate(after, leftAfter)
                .orElse(after.legalMoves().getFirst());
        pendingReply = reply;

        if (stillForcedMate) {
            feedbackPositive = true;
            feedback = "좋은 수입니다! 상대 응수 후 남은 " + leftAfter + "수 안에 체크메이트하세요.";
        } else {
            feedbackPositive = false;
            feedback = "상대가 빠져나갈 수 있습니다. [무르기]로 되돌리거나 남은 " + leftAfter + "수로 계속 시도해 보세요.";
        }
        return new AttemptResult(Outcome.CORRECT_STEP, feedback, pendingReply);
    }

    private AttemptResult submitTactical(Move move) {
        Move expected = puzzle.solution().get(plyIndex);
        boolean isLast = plyIndex == puzzle.solution().size() - 1;
        Board after = currentBoard.applyMove(move);

        if (move.equals(expected)) {
            undoStack.push(new Snapshot(currentBoard, plyIndex, movesUsed, lastMove));
            currentBoard = after;
            lastMove = move;
            hintArrow = null;
            highlightSquares = List.of();
            feedbackPositive = true;

            if (isLast) {
                solved = true;
                feedback = "정답입니다! " + puzzle.explanation();
                return new AttemptResult(Outcome.SOLVED, feedback, null);
            }

            pendingReply = puzzle.solution().get(plyIndex + 1);
            plyIndex += 2;
            feedback = "정확한 수입니다! 상대 응수 후 다음 수를 이어서 찾으세요.";
            return new AttemptResult(Outcome.CORRECT_STEP, feedback, pendingReply);
        }

        applyWrongFeedback(Optional.of(expected), List.of());
        return new AttemptResult(Outcome.WRONG, feedback, null);
    }

    private Optional<Move> findBestHintMove() {
        if (puzzle.isMatePuzzle()) {
            Optional<Move> computed = ai.findMateIn(currentBoard, remainingMoves());
            if (computed.isPresent()) {
                return computed;
            }
            if (movesUsed == 0 && !puzzle.solution().isEmpty()) {
                Move fallback = puzzle.solution().getFirst();
                if (currentBoard.legalMoves().contains(fallback)) {
                    return Optional.of(fallback);
                }
            }
            return Optional.empty();
        }
        if (plyIndex < puzzle.solution().size()) {
            return Optional.of(puzzle.solution().get(plyIndex));
        }
        return Optional.empty();
    }

    @Override
    public void showHint() {
        if (solved) {
            return;
        }
        Optional<Move> hint = findBestHintMove();
        if (hint.isEmpty() && puzzle.isMatePuzzle()) {
            feedbackPositive = false;
            feedback = "현재 국면에서는 남은 " + remainingMoves() + "수 안에 강제 메이트가 어렵습니다. [무르기]로 되돌려 보세요.";
            return;
        }
        hint.ifPresent(m -> revealHint(Optional.of(m), List.of(m.from())));
    }

    @Override
    public void reset() {
        this.currentBoard = puzzle.startBoard();
        this.undoStack.clear();
        this.plyIndex = 0;
        this.movesUsed = 0;
        this.solved = false;
        this.wrongCount = 0;
        this.lastMove = null;
        this.hintArrow = null;
        this.highlightSquares = List.of();
        this.pendingReply = null;
        this.feedback = "";
        this.feedbackPositive = false;
    }

    @Override
    public Side orientation() {
        return puzzle.startBoard().turn();
    }
}
