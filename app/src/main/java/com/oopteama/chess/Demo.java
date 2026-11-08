package com.oopteama.chess;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.piece.Piece;

import java.util.List;

/**
 * 10주차 완료 기준: "말을 놓으면 갈 수 있는 칸 목록이 나옴"을 눈으로 확인하는 임시 데모.
 * 실제 화면(프1)이 붙기 전까지 콘솔로 검증한다. RuleSet(legalMoves) 없이도
 * 기물별 pseudoLegalMoves만으로 동작을 확인할 수 있다.
 *
 * 컴파일/실행:
 *   javac -d out $(find src -name "*.java")
 *   java -cp out com.oopteama.chess.Demo
 */
public final class Demo {
    public static void main(String[] args) {
        Position pos = Position.initial();
        System.out.println("시작 FEN: " + Fen.write(pos));
        System.out.println("(참고용 상수 Fen.INITIAL: " + Fen.INITIAL + ")");

        // 나이트 b1이 갈 수 있는 칸
        printMoves(pos, "b1");
        // 폰 e2가 갈 수 있는 칸 (더블 푸시 포함)
        printMoves(pos, "e2");

        // e2-e4 둔 뒤 비숍 f1이 갈 수 있는 칸
        pos.make(new Move(Square.of("e2"), Square.of("e4"),
            com.oopteama.chess.core.MoveKind.DOUBLE_PUSH));
        System.out.println("\ne2-e4 이후: " + Fen.write(pos));
        printMoves(pos, "f1");

        pos.unmake();
        System.out.println("\nunmake 이후 (원복): " + Fen.write(pos));
    }

    private static void printMoves(Position pos, String squareName) {
        Square sq = Square.of(squareName);
        Piece piece = pos.at(sq);
        if (piece == null) {
            System.out.println(squareName + "에는 말이 없습니다.");
            return;
        }
        List<Move> moves = piece.pseudoLegalMoves(pos, sq);
        System.out.println(squareName + " (" + piece.fenChar() + ")가 갈 수 있는 칸: "
            + moves.stream().map(Move::uci).toList());
    }
}

