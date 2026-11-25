package com.oopteama.chess.training;

import com.oopteama.chess.ai.MateSolver;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Notation;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.Side;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 퍼즐 풀이 세션입니다.
 * <ul>
 *   <li>메이트 퍼즐: N수 안에 어떤 수순이든 체크메이트하면 정답. 상대(AI)는 매번 가장 잘 버티는 수로 응수합니다.</li>
 *   <li>전술 퍼즐: 정해진 정답 수순대로 두어야 하며, 상대 응수는 자동으로 둡니다.</li>
 * </ul>
 */
public final class PuzzleSession extends TrainingSession {

    /** 무르기용 저장 상태. */
    private record Snapshot(Board board, int plyIndex, int movesUsed, Move lastMove) {
    }

    private final Puzzle puzzle;
    private final MateSolver mateSolver;
    private final Deque<Snapshot> undoStack = new ArrayDeque<>();
    private int plyIndex;   // 전술 퍼즐: 정답 수순에서 다음에 둘 차례
    private int movesUsed;  // 메이트 퍼즐: 지금까지 둔 내 수

    public PuzzleSession(Puzzle puzzle, MateSolver mateSolver) {
        this.puzzle = Objects.requireNonNull(puzzle, "puzzle");
        this.mateSolver = Objects.requireNonNull(mateSolver, "mateSolver");
        reset();
    }

    public Puzzle puzzle() {
        return puzzle;
    }

    /** 내가 두어야 하는 수의 개수. */
    public int totalUserMoves() {
        return puzzle.isMatePuzzle() ? puzzle.mateIn() : (puzzle.solution().size() + 1) / 2;
    }

    /** 지금 몇 번째 수를 둘 차례인지. */
    public int currentUserMoveNumber() {
        return puzzle.isMatePuzzle()
                ? Math.min(puzzle.mateIn(), movesUsed + 1)
                : Math.min(totalUserMoves(), plyIndex / 2 + 1);
    }

    /** 남은 내 수. */
    public int remainingMoves() {
        return puzzle.isMatePuzzle()
                ? Math.max(0, puzzle.mateIn() - movesUsed)
                : Math.max(0, totalUserMoves() - plyIndex / 2);
    }

    public boolean canUndo() {
        return !undoStack.isEmpty() && !solved;
    }

    /** 직전 내 수(와 상대 응수)를 되돌립니다. */
    public boolean undo() {
        if (!canUndo()) {
            return false;
        }
        Snapshot previous = undoStack.pop();
        currentBoard = previous.board();
        plyIndex = previous.plyIndex();
        movesUsed = previous.movesUsed();
        lastMove = previous.lastMove();
        pendingReply = null;
        hintArrow = null;
        highlightSquares = List.of();
        feedbackPositive = false;
        feedback = "한 수 되돌렸습니다. 다른 수를 시도해 보세요.";
        return true;
    }

    @Override
    public AttemptResult submitMove(Move move) {
        if (solved || pendingReply != null) {
            return new AttemptResult(Outcome.SOLVED, feedback, null);
        }
        if (!Rules.legalMoves(currentBoard).contains(move)) {
            return new AttemptResult(Outcome.WRONG, "규칙상 둘 수 없는 수입니다.", null);
        }
        return puzzle.isMatePuzzle() ? submitMateMove(move) : submitTacticalMove(move);
    }

    private AttemptResult submitMateMove(Move move) {
        Board after = currentBoard.apply(move);

        // 1. 체크메이트하면 수순과 상관없이 정답
        if (Rules.isCheckmate(after)) {
            playUserMove(move, after);
            solved = true;
            feedbackPositive = true;
            feedback = "체크메이트! (" + movesUsed + "수 만에 해결) " + puzzle.explanation();
            return new AttemptResult(Outcome.SOLVED, feedback, null);
        }

        // 2. 무승부가 되는 수는 받지 않음
        if (Rules.isStalemate(after) || Rules.isInsufficientMaterial(after)) {
            wrongCount++;
            feedbackPositive = false;
            feedback = Rules.isStalemate(after)
                    ? "스테일메이트(무승부)가 됩니다! 상대 킹에게 체크를 걸어 마무리하세요."
                    : "기물 부족으로 무승부가 됩니다. 다른 수를 찾아보세요.";
            return new AttemptResult(Outcome.WRONG, feedback, null);
        }

        // 3. 마지막 남은 수였는데 메이트가 아니면 오답 (보드는 그대로)
        if (remainingMoves() <= 1) {
            applyWrongFeedback(findHintMove(), List.of());
            if (wrongCount == 1 && Rules.isInCheck(after)) {
                List<Move> escapes = Rules.legalMoves(after);
                feedback = "체크이지만 메이트가 아닙니다! 상대가 "
                        + Notation.toSan(after, escapes.getFirst()) + " 등으로 빠져나갈 수 있습니다.";
            }
            return new AttemptResult(Outcome.WRONG, feedback, null);
        }

        // 4. 수가 남았으면 어떤 수든 두고, 상대는 가장 잘 버티는 수로 응수
        playUserMove(move, after);
        int left = remainingMoves();
        boolean stillMating = mateSolver.isForcedMate(after, left);
        pendingReply = mateSolver.bestDefense(after, left).orElse(Rules.legalMoves(after).getFirst());
        feedbackPositive = stillMating;
        feedback = stillMating
                ? "좋은 수입니다! 상대 응수 후 남은 " + left + "수 안에 체크메이트하세요."
                : "상대가 빠져나갈 수 있습니다. [무르기]로 되돌리거나 남은 " + left + "수로 계속 시도해 보세요.";
        return new AttemptResult(Outcome.CORRECT_STEP, feedback, pendingReply);
    }

    private AttemptResult submitTacticalMove(Move move) {
        Move expected = puzzle.solution().get(plyIndex);
        if (!move.equals(expected)) {
            applyWrongFeedback(Optional.of(expected), List.of());
            return new AttemptResult(Outcome.WRONG, feedback, null);
        }
        boolean lastMoveOfSolution = plyIndex == puzzle.solution().size() - 1;
        playUserMove(move, currentBoard.apply(move));
        feedbackPositive = true;
        if (lastMoveOfSolution) {
            solved = true;
            feedback = "정답입니다! " + puzzle.explanation();
            return new AttemptResult(Outcome.SOLVED, feedback, null);
        }
        pendingReply = puzzle.solution().get(plyIndex + 1);
        plyIndex += 2;
        feedback = "정확한 수입니다! 상대 응수 후 다음 수를 이어서 찾으세요.";
        return new AttemptResult(Outcome.CORRECT_STEP, feedback, pendingReply);
    }

    /** 내 수를 보드에 반영하고, 무르기용으로 직전 상태를 저장합니다. */
    private void playUserMove(Move move, Board after) {
        undoStack.push(new Snapshot(currentBoard, plyIndex, movesUsed, lastMove));
        currentBoard = after;
        lastMove = move;
        movesUsed++;
        hintArrow = null;
        highlightSquares = List.of();
    }

    /** 힌트로 보여 줄 수: 메이트 퍼즐은 지금 국면에서 메이트로 가는 수, 전술 퍼즐은 다음 정답 수. */
    private Optional<Move> findHintMove() {
        if (puzzle.isMatePuzzle()) {
            Optional<Move> mate = mateSolver.findMateIn(currentBoard, remainingMoves());
            if (mate.isPresent()) {
                return mate;
            }
            if (movesUsed == 0 && !puzzle.solution().isEmpty()
                    && Rules.legalMoves(currentBoard).contains(puzzle.solution().getFirst())) {
                return Optional.of(puzzle.solution().getFirst());
            }
            return Optional.empty();
        }
        return plyIndex < puzzle.solution().size() ? Optional.of(puzzle.solution().get(plyIndex)) : Optional.empty();
    }

    @Override
    public void showHint() {
        if (solved) {
            return;
        }
        Optional<Move> hint = findHintMove();
        if (hint.isEmpty() && puzzle.isMatePuzzle()) {
            feedbackPositive = false;
            feedback = "현재 국면에서는 남은 " + remainingMoves() + "수 안에 강제 메이트가 어렵습니다. [무르기]로 되돌려 보세요.";
            return;
        }
        hint.ifPresent(m -> revealHint(Optional.of(m), List.of(m.from())));
    }

    @Override
    public void reset() {
        currentBoard = puzzle.startBoard();
        undoStack.clear();
        plyIndex = 0;
        movesUsed = 0;
        solved = false;
        wrongCount = 0;
        lastMove = null;
        hintArrow = null;
        highlightSquares = List.of();
        pendingReply = null;
        feedback = "";
        feedbackPositive = false;
    }

    @Override
    public Side orientation() {
        return puzzle.startBoard().turn();
    }
}
