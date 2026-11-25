package com.oopteama.chess.ui.component;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.Rules;
import com.oopteama.chess.model.piece.Piece;

import javax.swing.Timer;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 기물이 출발 칸에서 도착 칸으로 미끄러지는 애니메이션을 관리합니다.
 * 체스판이 바뀌었을 때 수 하나로 설명되는 변화면 그 수를 재생하고,
 * 무르기·새 대국처럼 수 하나로 설명되지 않는 변화는 바로 보여 줍니다.
 */
public final class MoveAnimator {

    private static final int DURATION_MS = 250;

    /** 미끄러지는 기물 하나 (캐슬링은 킹과 룩 두 개). */
    record Slide(Piece piece, Position from, Position to) {
    }

    /** 수 하나의 애니메이션: 수를 둔 뒤 국면, 미끄러지는 기물들, 서서히 사라지는 잡힌 기물과 그 칸. */
    record Animation(Move move, Board after, List<Slide> slides, Piece captured, Position capturedAt) {

        /** 이 칸은 미끄러지는 기물이 도착할 칸이라 애니메이션 중에는 비워 둡니다. */
        boolean hides(Position square) {
            for (Slide slide : slides) {
                if (slide.to().equals(square)) {
                    return true;
                }
            }
            return false;
        }
    }

    private final Deque<Animation> queue = new ArrayDeque<>(); // 맨 앞 = 재생 중, 그 뒤 = 대기 중
    private final Timer timer = new Timer(15, e -> nextFrame());
    private final Runnable repaint;
    private long startNanos;

    /** @param repaint 화면을 다시 그리는 동작 (애니메이션 한 프레임마다 호출) */
    MoveAnimator(Runnable repaint) {
        this.repaint = repaint;
    }

    /** 15ms마다 호출: 재생이 끝난 수는 빼고 다음 수를 이어서 재생합니다. */
    private void nextFrame() {
        if (!queue.isEmpty() && System.nanoTime() - startNanos >= DURATION_MS * 1_000_000L) {
            queue.removeFirst();
            startNanos = System.nanoTime();
        }
        if (queue.isEmpty()) {
            timer.stop();
        }
        repaint.run();
    }

    /**
     * 체스판이 previous에서 next로 바뀌었을 때 호출합니다.
     *
     * @param skip 애니메이션을 생략할 수 (드래그로 이미 도착 칸에 놓은 수)
     */
    void boardChanged(Board previous, Board next, Move skip) {
        if (previous == next || previous.toFen().equals(next.toFen())) {
            return; // 같은 국면을 다시 그리는 경우(힌트 표시 등)에는 재생 중인 애니메이션을 그대로 둡니다.
        }
        Move move = findMoveBetween(previous, next);
        if (move == null || move.equals(skip)) {
            stop();
            return;
        }
        if (queue.size() >= 2) {
            queue.clear(); // 수가 연달아 밀려 들어오면 마지막 수만 보여 줍니다.
        }
        queue.addLast(createAnimation(previous, move, next));
        if (queue.size() == 1) {
            startNanos = System.nanoTime();
            timer.start();
        }
    }

    /** 지금 재생 중인 애니메이션 (없으면 null). */
    Animation current() {
        return queue.peekFirst();
    }

    /** 재생 진행도 0~1. 처음에 빠르게 움직이다 도착할 때 천천히 멈춥니다. */
    double progress() {
        double t = Math.min(1.0, (System.nanoTime() - startNanos) / (DURATION_MS * 1_000_000.0));
        double rest = 1.0 - t;
        return 1.0 - rest * rest * rest;
    }

    /** 재생 중이거나 대기 중인 수 (테스트용). */
    List<Move> pendingMoves() {
        return queue.stream().map(Animation::move).toList();
    }

    void stop() {
        queue.clear();
        timer.stop();
    }

    /** before 국면에서 둘 수 있는 수 중 after 국면을 만드는 수를 찾습니다. 없으면 null입니다. */
    private static Move findMoveBetween(Board before, Board after) {
        String afterFen = after.toFen();
        for (Move move : Rules.legalMoves(before)) {
            Piece mover = before.pieceAt(move.from());
            Piece landed = move.isPromotion() ? Piece.create(move.promotion(), mover.side()) : mover;
            if (!landed.equals(after.pieceAt(move.to())) || !after.isEmpty(move.from())) {
                continue; // 빠른 거르기: 도착 칸의 기물이 다르면 이 수가 아닙니다.
            }
            if (before.apply(move).toFen().equals(afterFen)) {
                return move;
            }
        }
        return null;
    }

    private static Animation createAnimation(Board before, Move move, Board after) {
        List<Slide> slides = new ArrayList<>();
        slides.add(new Slide(before.pieceAt(move.from()), move.from(), move.to()));
        if (Rules.isCastling(before, move)) {
            boolean kingSide = move.to().col() > move.from().col();
            Position rookFrom = new Position(kingSide ? 7 : 0, move.from().row());
            Position rookTo = new Position(kingSide ? 5 : 3, move.from().row());
            if (!before.isEmpty(rookFrom)) {
                slides.add(new Slide(before.pieceAt(rookFrom), rookFrom, rookTo));
            }
        }
        Piece captured = Rules.capturedPiece(before, move);
        Position capturedAt = Rules.isEnPassant(before, move)
                ? new Position(move.to().col(), move.from().row())
                : move.to();
        return new Animation(move, after, List.copyOf(slides), captured, captured == null ? null : capturedAt);
    }
}
