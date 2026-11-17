package com.oopteama.chess.backend.core;

import com.oopteama.chess.backend.core.BoardPosition.Piece;
import com.oopteama.chess.backend.core.BoardPosition.Side;
import com.oopteama.chess.backend.core.BoardPosition.Status;
import com.oopteama.chess.backend.core.Move.MoveKind;
import com.oopteama.chess.backend.core.Move.MoveRecord;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChessCoreTest {

    @Test
    void 초기국면은_표준FEN과_20개의_합법수를_가진다() {
        ChessCore core = new ChessCore();
        assertEquals(ChessCore.INITIAL_FEN, core.initialFen());
        assertEquals(Side.WHITE, core.position().turn());
        assertEquals(20, core.position().legalMoves().size());
        assertEquals(Status.PLAYING, core.position().status());
    }

    @Test
    void SAN과_UCI_좌표입력으로_대국을_진행하고_무르기할_수_있다() {
        ChessCore core = new ChessCore();
        MoveRecord r1 = core.play("e4");
        MoveRecord r2 = core.playUci("e7e5");
        MoveRecord r3 = core.play("g1", "f3");

        assertEquals("e4", r1.san());
        assertEquals("e5", r2.san());
        assertEquals("Nf3", r3.san());
        assertEquals(List.of("e4", "e5", "Nf3"), core.sanHistory());
        assertEquals(Piece.WN, core.position().pieceAt("f3"));

        assertTrue(core.undo());
        assertEquals(List.of("e4", "e5"), core.sanHistory());
        assertEquals(Piece.EMPTY, core.position().pieceAt("f3"));
        assertEquals(Piece.WN, core.position().pieceAt("g1"));

        assertTrue(core.undo());
        assertTrue(core.undo());
        assertFalse(core.undo());
    }

    @Test
    void loadGame은_중간에_불법수가_있으면_기존_대국을_보존한다() {
        ChessCore core = new ChessCore();
        core.play("e4");

        assertThrows(IllegalArgumentException.class,
                () -> core.loadGame(ChessCore.INITIAL_FEN, List.of("d4", "illegal")));
        assertEquals(List.of("e4"), core.sanHistory());
    }

    @Test
    void 캐슬링_앙파상_승진_체크메이트가_정확히_동작한다() {
        // 앙파상 테스트
        ChessCore ep = new ChessCore("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2");
        MoveRecord epMove = ep.play("exd6");
        assertEquals(MoveKind.EN_PASSANT, epMove.kind());
        assertEquals(Piece.EMPTY, ep.position().pieceAt("d5"));
        assertEquals(Piece.WP, ep.position().pieceAt("d6"));

        // 승진 테스트
        ChessCore promo = new ChessCore("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        MoveRecord promoMove = promo.play("a7", "a8", 'Q');
        assertEquals("a8=Q+", promoMove.san());
        assertTrue(promoMove.check());
        assertEquals(Piece.WQ, promo.position().pieceAt("a8"));

        // 캐슬링 테스트
        ChessCore castle = new ChessCore("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        MoveRecord shortCastle = castle.play("O-O");
        assertEquals(MoveKind.CASTLE_SHORT, shortCastle.kind());
        assertEquals(Piece.WK, castle.position().pieceAt("g1"));
        assertEquals(Piece.WR, castle.position().pieceAt("f1"));
    }

    @Test
    void 삼회_반복과_기물부족_규칙을_판정한다() {
        ChessCore rep = new ChessCore();
        assertEquals(1, rep.repetitionCount());
        for (int i = 0; i < 2; i++) {
            rep.play("Nf3");
            rep.play("Nf6");
            rep.play("Ng1");
            rep.play("Ng8");
        }
        assertEquals(3, rep.repetitionCount());

        ChessCore sameColorBishops = new ChessCore("4k3/8/8/8/8/8/3B4/2b1K3 w - - 0 1");
        assertTrue(sameColorBishops.position().isInsufficientMaterial());

        ChessCore diffColorBishops = new ChessCore("4k3/8/8/8/8/8/3B4/3bK3 w - - 0 1");
        assertFalse(diffColorBishops.position().isInsufficientMaterial());
    }

    @Test
    void 잘못된_FEN은_검증에서_거부한다() {
        // 킹 개수 오류
        assertThrows(IllegalArgumentException.class,
                () -> ChessNotation.parseFen("8/8/8/8/8/8/8/4K3 w - - 0 1"));
        // 직전에 둔 쪽의 킹이 체크인 불법 국면
        assertThrows(IllegalArgumentException.class,
                () -> ChessNotation.parseFen("4k3/8/8/8/8/8/4R3/4K3 w - - 0 1"));
        // 캐슬링 권리가 있는데 룩이 없는 경우
        assertThrows(IllegalArgumentException.class,
                () -> ChessNotation.parseFen("4k3/8/8/8/8/8/8/4K3 w K - 0 1"));
    }
}
