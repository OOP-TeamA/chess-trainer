package com.oopteama.chess.core.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.piece.Piece;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 앙파상은 잡힐 상대 폰이 실제로 있을 때만 만들어진다(리뷰 P2). */
class EnPassantTest {

    private static final StandardRules RULES = new StandardRules();

    private static boolean hasEnPassant(String fen) throws Exception {
        return RULES.legalMoves(Fen.parse(fen)).stream().anyMatch(m -> m.kind() == MoveKind.EN_PASSANT);
    }

    @Test
    void generatedWhenAnEnemyPawnIsReallyThere() throws Exception {
        assertTrue(hasEnPassant("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1"));
        assertTrue(hasEnPassant("4k3/8/8/8/3pP3/8/8/4K3 b - e3 0 1"));
    }

    @Test
    void notGeneratedWhenTheCapturedSquareHoldsAnotherPiece() throws Exception {
        // 리뷰어가 보고한 국면: d5에 백 룩. 예전에는 e5d6이 만들어져 룩이 사라졌다.
        String fen = "4k3/8/8/3RP3/8/8/8/4K3 w - d6 0 1";
        assertFalse(hasEnPassant(fen));
        assertTrue(RULES.validate(Fen.parse(fen)).isEmpty()); // validate는 문법/기본 규칙만 본다
        // 흑 쪽도 마찬가지(e4에 백 룩).
        assertFalse(hasEnPassant("4k3/8/8/8/3pR3/8/8/4K3 b - e3 0 1"));
        // 잡힐 칸에 상대 폰이 아니라 같은 편 폰이 있는 경우.
        assertFalse(hasEnPassant("4k3/8/8/3PP3/8/8/8/4K3 w - d6 0 1"));
    }

    @Test
    void notGeneratedWhenTheCapturedSquareIsEmpty() throws Exception {
        assertFalse(hasEnPassant("4k3/8/8/4P3/8/8/8/4K3 w - d6 0 1"));
        assertFalse(hasEnPassant("4k3/8/8/8/3p4/8/8/4K3 b - e3 0 1"));
    }

    @Test
    void pseudoLegalMovesAlsoSkipItAndNoPieceDisappears() throws Exception {
        Position pos = Fen.parse("4k3/8/8/3RP3/8/8/8/4K3 w - d6 0 1");
        Square pawn = Square.of("e5");
        Piece piece = pos.at(pawn);
        List<Move> moves = piece.pseudoLegalMoves(pos, pawn);
        assertTrue(moves.stream().noneMatch(m -> m.kind() == MoveKind.EN_PASSANT));
        // 가능한 수를 전부 두었다 되돌려도 룩은 d5에 남아 있다.
        for (Move m : RULES.legalMoves(pos)) {
            Position copy = pos.copy();
            copy.make(m);
            if (m.from().equals(pawn)) {
                assertEquals("R", String.valueOf(copy.at(Square.of("d5")).fenChar()), m.uci());
            }
        }
    }
}
