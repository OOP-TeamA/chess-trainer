package com.oopteama.chess.core.rules;

import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.GameResult.Outcome;
import com.oopteama.chess.core.GameResult.Reason;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.PieceType;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.piece.Piece;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** 표준 체스 규칙. 상태가 없으므로 인스턴스를 공유해도 된다. */
public final class StandardRules implements RuleSet {

    @Override
    public List<Move> legalMoves(Position pos) {
        Objects.requireNonNull(pos, "pos");
        return MoveGenerator.legalMoves(pos, true);
    }

    @Override
    public boolean inCheck(Position pos) {
        Objects.requireNonNull(pos, "pos");
        return MoveGenerator.inCheck(pos);
    }

    @Override
    public boolean isAttacked(Position pos, Square square, Side by) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(square, "square");
        Objects.requireNonNull(by, "by");
        return MoveGenerator.isAttacked(pos, square, by);
    }

    @Override
    public String repetitionKey(Position pos) {
        Objects.requireNonNull(pos, "pos");
        return MoveGenerator.repetitionKey(pos, true);
    }

    /**
     * 체크메이트, 스테일메이트, 기물 부족(K 대 K, K+B 대 K, K+N 대 K), 50수, 3회 반복 순으로 본다.
     * 반복은 history에 이 국면이 이미 기록돼 있다고 보고 세므로 수를 둔 뒤에 호출해야 한다.
     */
    @Override
    public Optional<GameResult> checkEnd(Position pos, GameHistory history) {
        Objects.requireNonNull(pos, "pos");
        Objects.requireNonNull(history, "history");

        if (legalMoves(pos).isEmpty()) {
            if (inCheck(pos)) {
                Outcome winner = pos.sideToMove() == Side.WHITE ? Outcome.BLACK_WINS : Outcome.WHITE_WINS;
                return Optional.of(new GameResult(winner, Reason.CHECKMATE));
            }
            return Optional.of(new GameResult(Outcome.DRAW, Reason.STALEMATE));
        }
        if (insufficientMaterial(pos)) {
            return Optional.of(new GameResult(Outcome.DRAW, Reason.INSUFFICIENT_MATERIAL));
        }
        if (pos.halfmoveClock() >= 100) {
            return Optional.of(new GameResult(Outcome.DRAW, Reason.FIFTY_MOVE));
        }
        if (history.occurrences(repetitionKey(pos)) >= 3) {
            return Optional.of(new GameResult(Outcome.DRAW, Reason.REPETITION));
        }
        return Optional.empty();
    }

    /** 킹 외에 기물이 없거나, 비숍 또는 나이트가 정확히 하나뿐인 경우. */
    private static boolean insufficientMaterial(Position pos) {
        int others = 0;
        PieceType last = null;
        for (int i = 0; i < 64; i++) {
            Piece p = pos.at(Square.ofIndex(i));
            if (p == null || p.type() == PieceType.KING) {
                continue;
            }
            others++;
            last = p.type();
        }
        return others == 0 || (others == 1 && (last == PieceType.BISHOP || last == PieceType.KNIGHT));
    }

    /**
     * 표준 국면 검증: 양쪽 킹 각 1개, 1·8랭크에 폰 없음, 차례가 아닌 쪽이 체크 상태 아님,
     * 캐슬링 권리와 기물 위치 일치.
     *
     * @return 문제 설명 목록. 정상이면 빈 목록
     */
    public List<String> validate(Position pos) {
        Objects.requireNonNull(pos, "pos");
        List<String> problems = new ArrayList<>();

        int[] kings = new int[2];
        for (int i = 0; i < 64; i++) {
            Square sq = Square.ofIndex(i);
            Piece p = pos.at(sq);
            if (p == null) {
                continue;
            }
            if (p.type() == PieceType.KING) {
                kings[p.side().ordinal()]++;
            }
            if (p.type() == PieceType.PAWN && (sq.rank() == 0 || sq.rank() == 7)) {
                problems.add(name(p.side()) + " 폰이 " + sq.name() + "에 있습니다(1·8랭크에는 폰이 있을 수 없음).");
            }
        }
        for (Side side : Side.values()) {
            int n = kings[side.ordinal()];
            if (n != 1) {
                problems.add(name(side) + " 킹이 " + n + "개입니다(정확히 1개여야 함).");
            }
        }

        Side mover = pos.sideToMove();
        Optional<Square> waitingKing = pos.kingSquare(mover.opposite());
        if (waitingKing.isPresent() && MoveGenerator.isAttacked(pos, waitingKing.get(), mover)) {
            problems.add("차례가 아닌 " + name(mover.opposite()) + " 킹이 체크 상태입니다.");
        }

        for (Side side : Side.values()) {
            checkCastlingRight(pos, side, true, problems);
            checkCastlingRight(pos, side, false, problems);
        }
        return List.copyOf(problems);
    }

    private static void checkCastlingRight(Position pos, Side side, boolean shortSide, List<String> problems) {
        if (!pos.canCastle(side, shortSide)) {
            return;
        }
        int rank = side == Side.WHITE ? 0 : 7;
        String label = name(side) + " " + (shortSide ? "킹사이드" : "퀸사이드") + " 캐슬링 권리가 있지만 ";
        Square king = new Square(4, rank);
        if (!MoveGenerator.isPiece(pos.at(king), side, PieceType.KING)) {
            problems.add(label + "킹이 " + king.name() + "에 없습니다.");
        }
        Square rook = new Square(shortSide ? 7 : 0, rank);
        if (!MoveGenerator.isPiece(pos.at(rook), side, PieceType.ROOK)) {
            problems.add(label + "룩이 " + rook.name() + "에 없습니다.");
        }
    }

    private static String name(Side side) {
        return side == Side.WHITE ? "백" : "흑";
    }
}
