package com.oopteama.chess.backend.game;

import com.oopteama.chess.api.GameOutcomeView;
import com.oopteama.chess.api.GameState;
import com.oopteama.chess.api.LegalMoveView;
import com.oopteama.chess.api.MoveAttemptResult;
import com.oopteama.chess.api.MoveInput;
import com.oopteama.chess.api.PieceKind;
import com.oopteama.chess.api.PieceView;
import com.oopteama.chess.api.SideColor;
import com.oopteama.chess.backend.core.BoardPosition;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreGameSessionTest {

    private static int sq(String name) {
        return BoardPosition.parseSquare(name);
    }

    @Test
    void 초기상태와_합법수_착수_무르기를_지원한다() {
        CoreGameSession session = new CoreGameSession();
        List<GameState> events = new ArrayList<>();
        session.addListener(events::add);

        GameState initial = session.state();
        assertEquals(SideColor.WHITE, initial.board().turn());
        assertEquals(20, initial.board().legalMoves().size());
        assertFalse(initial.canUndo());
        assertTrue(initial.result().isEmpty());
        assertEquals(new PieceView(SideColor.WHITE, PieceKind.KING),
                initial.board().pieceAt(sq("e1")).orElseThrow());

        MoveAttemptResult ok = session.playMove(MoveInput.of(sq("e2"), sq("e4")));
        assertTrue(ok.accepted());
        assertEquals(1, events.size());
        assertEquals(List.of("e4"), session.state().sanHistory());
        assertTrue(session.state().canUndo());
        assertEquals(sq("e2"), session.state().board().lastMove().orElseThrow().from());
        assertEquals(sq("e4"), session.state().board().lastMove().orElseThrow().to());

        MoveAttemptResult bad = session.playMove(MoveInput.of(sq("e4"), sq("e5")));
        assertFalse(bad.accepted());
        assertEquals(1, events.size());

        assertTrue(session.undo());
        assertEquals(2, events.size());
        assertTrue(session.state().sanHistory().isEmpty());
        assertFalse(session.state().canUndo());
    }

    @Test
    void 체크메이트와_기권_종료후_처리를_검증한다() {
        CoreGameSession session = new CoreGameSession();
        assertTrue(session.playMove(MoveInput.of(sq("f2"), sq("f3"))).accepted());
        assertTrue(session.playMove(MoveInput.of(sq("e7"), sq("e5"))).accepted());
        assertTrue(session.playMove(MoveInput.of(sq("g2"), sq("g4"))).accepted());
        assertTrue(session.playMove(MoveInput.of(sq("d8"), sq("h4"))).accepted());

        GameState mated = session.state();
        assertTrue(mated.inCheck());
        assertEquals(sq("e1"), mated.board().checkSquare().orElseThrow());
        assertTrue(mated.board().legalMoves().isEmpty());
        assertEquals(
                new GameOutcomeView(GameOutcomeView.Winner.BLACK, GameOutcomeView.EndReason.CHECKMATE),
                mated.result().orElseThrow()
        );
        assertFalse(session.playMove(MoveInput.of(sq("a2"), sq("a3"))).accepted());

        // 무르기 후 흑 차례에서 기권 -> 백 승리
        assertTrue(session.undo());
        assertTrue(session.state().result().isEmpty());
        session.resign();
        assertEquals(
                new GameOutcomeView(GameOutcomeView.Winner.WHITE, GameOutcomeView.EndReason.RESIGNATION),
                session.state().result().orElseThrow()
        );

        session.newGame();
        assertTrue(session.state().result().isEmpty());
        assertTrue(session.state().sanHistory().isEmpty());
    }

    @Test
    void 승진칸은_UI용_합법수에서_중복없이_한번만_내려준다() {
        CoreGameSession promo = new CoreGameSession("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        List<LegalMoveView> pawnMoves = promo.state().board().legalMoves().stream()
                .filter(m -> m.from() == sq("a7") && m.to() == sq("a8"))
                .toList();
        assertEquals(1, pawnMoves.size());
        assertTrue(pawnMoves.getFirst().promotion());

        MoveAttemptResult res = promo.playMove(MoveInput.promote(sq("a7"), sq("a8"), PieceKind.KNIGHT));
        assertTrue(res.accepted());
        assertEquals(List.of("a8=N"), promo.state().sanHistory());
    }

    @Test
    void 앙파상은_LegalMoveView에_capture로_표시된다() {
        CoreGameSession ep = new CoreGameSession("rnbqkbnr/ppp1pppp/8/3pP3/8/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2");
        LegalMoveView epMove = ep.state().board().legalMoves().stream()
                .filter(m -> m.from() == sq("e5") && m.to() == sq("d6"))
                .findFirst()
                .orElseThrow();
        assertTrue(epMove.capture());
    }

    @Test
    void 스테일메이트_50수_삼회반복을_판정한다() {
        CoreGameSession stalemate = new CoreGameSession("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        assertEquals(
                new GameOutcomeView(GameOutcomeView.Winner.DRAW, GameOutcomeView.EndReason.STALEMATE),
                stalemate.state().result().orElseThrow()
        );

        CoreGameSession fifty = new CoreGameSession("4k3/8/8/8/8/8/R7/4K3 w - - 100 60");
        assertEquals(
                new GameOutcomeView(GameOutcomeView.Winner.DRAW, GameOutcomeView.EndReason.FIFTY_MOVE),
                fifty.state().result().orElseThrow()
        );

        CoreGameSession rep = new CoreGameSession();
        for (int i = 0; i < 2; i++) {
            rep.playMove(MoveInput.of(sq("g1"), sq("f3")));
            rep.playMove(MoveInput.of(sq("g8"), sq("f6")));
            rep.playMove(MoveInput.of(sq("f3"), sq("g1")));
            rep.playMove(MoveInput.of(sq("f6"), sq("g8")));
        }
        assertEquals(
                new GameOutcomeView(GameOutcomeView.Winner.DRAW, GameOutcomeView.EndReason.REPETITION),
                rep.state().result().orElseThrow()
        );
    }
}
