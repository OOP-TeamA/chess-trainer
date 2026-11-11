package com.oopteama.chess.game;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.GameResult.Outcome;
import com.oopteama.chess.core.GameResult.Reason;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.StandardRules;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 대국 상태 Game: 수 두기, 되돌리기, 종료 판정, 기권, 리스너 통지. */
class GameTest {

    private static final StandardRules RULES = new StandardRules();

    /** 리스너 호출을 문자열로 기록. */
    private static final class Recorder implements GameListener {
        final List<String> events = new ArrayList<>();

        @Override
        public void moveMade(Move move, Position after) { events.add("made " + move.uci()); }

        @Override
        public void moveUndone(Move move, Position after) { events.add("undone " + move.uci()); }

        @Override
        public void gameEnded(GameResult result) { events.add("ended " + result.outcome()); }
    }

    private static Game newGame() {
        return new Game(Position.initial(), RULES);
    }

    private static void play(Game game, String... ucis) {
        for (String uci : ucis) {
            game.make(RULES.fromUci(game.snapshot(), uci).orElseThrow());
        }
    }

    @Test
    void 수를_두면_국면과_기록이_함께_바뀌고_리스너에_알린다() {
        Game game = newGame();
        Recorder rec = new Recorder();
        game.addListener(rec);

        play(game, "e2e4", "e7e5");

        assertEquals(Side.WHITE, game.sideToMove());
        assertEquals(2, game.history().moves().size());
        assertEquals(List.of("made e2e4", "made e7e5"), rec.events);
        assertEquals(Fen.INITIAL, game.history().startFen());
    }

    @Test
    void 불법_수는_거부한다() {
        Game game = newGame();
        Move illegal = new Move(Square.of("e2"), Square.of("e5"), MoveKind.QUIET);
        assertThrows(IllegalArgumentException.class, () -> game.make(illegal));
        assertEquals(0, game.history().moves().size());
    }

    @Test
    void 되돌리기는_시작_국면까지만_가능하다() {
        Game game = newGame();
        Recorder rec = new Recorder();
        game.addListener(rec);
        play(game, "e2e4", "e7e5");

        game.undo(2);

        assertEquals(Fen.INITIAL, Fen.write(game.snapshot()));
        assertEquals(0, game.history().moves().size());
        assertEquals(List.of("made e2e4", "made e7e5", "undone e7e5", "undone e2e4"), rec.events);
        assertThrows(IllegalArgumentException.class, () -> game.undo(1));
    }

    @Test
    void 체크메이트로_끝나면_결과를_알리고_더_둘_수_없다() {
        Game game = newGame();
        Recorder rec = new Recorder();
        game.addListener(rec);

        play(game, "f2f3", "e7e5", "g2g4", "d8h4");   // 바보의 메이트

        GameResult r = game.result().orElseThrow();
        assertEquals(Outcome.BLACK_WINS, r.outcome());
        assertEquals(Reason.CHECKMATE, r.reason());
        assertTrue(game.legalMoves().isEmpty());
        assertEquals("ended BLACK_WINS", rec.events.get(rec.events.size() - 1));
        Move any = new Move(Square.of("a2"), Square.of("a3"), MoveKind.QUIET);
        assertThrows(IllegalStateException.class, () -> game.make(any));
    }

    @Test
    void 되돌리면_종료_상태가_풀린다() {
        Game game = newGame();
        play(game, "f2f3", "e7e5", "g2g4", "d8h4");
        game.undo(1);
        assertFalse(game.result().isPresent());
        assertFalse(game.legalMoves().isEmpty());
    }

    @Test
    void 기권하면_상대가_이긴다() {
        Game game = newGame();
        Recorder rec = new Recorder();
        game.addListener(rec);

        game.resign(Side.WHITE);

        assertEquals(Outcome.BLACK_WINS, game.result().orElseThrow().outcome());
        assertEquals(List.of("ended BLACK_WINS"), rec.events);
        assertThrows(IllegalStateException.class, () -> game.resign(Side.BLACK));
    }

    @Test
    void 스냅샷을_바꿔도_대국에는_영향이_없다() {
        Game game = newGame();
        Position snap = game.snapshot();
        snap.make(RULES.fromUci(snap, "e2e4").orElseThrow());
        assertEquals(Fen.INITIAL, Fen.write(game.snapshot()));
    }

    @Test
    void 영_수_되돌리기는_아무것도_바꾸지_않는다() throws Exception {
        String mated = "7k/5QQ1/8/8/8/8/8/K7 b - - 0 1";
        Game game = new Game(Fen.parse(mated), RULES);
        Recorder rec = new Recorder();
        game.addListener(rec);

        game.undo(0);

        assertEquals(Reason.CHECKMATE, game.result().orElseThrow().reason());
        assertTrue(game.legalMoves().isEmpty());
        assertEquals(mated, Fen.write(game.snapshot()));
        assertEquals(List.of(), rec.events);
    }

    @Test
    void 리스너마다_따로_만든_사본을_받는다() {
        Game game = newGame();
        List<String> seen = new ArrayList<>();
        GameListener mutating = new GameListener() {
            @Override public void gameEnded(GameResult result) { }
            @Override public void moveMade(Move move, Position after) {
                after.make(RULES.legalMoves(after).get(0));   // 받은 국면을 바꿔 본다
            }
            @Override public void moveUndone(Move move, Position after) {
                after.make(RULES.legalMoves(after).get(0));
            }
        };
        GameListener observing = new GameListener() {
            @Override public void gameEnded(GameResult result) { }
            @Override public void moveMade(Move move, Position after) { seen.add(Fen.write(after)); }
            @Override public void moveUndone(Move move, Position after) { seen.add(Fen.write(after)); }
        };
        game.addListener(mutating);
        game.addListener(observing);

        play(game, "e2e4");
        String afterE4 = Fen.write(game.snapshot());
        game.undo(1);

        assertEquals(List.of(afterE4, Fen.INITIAL), seen);   // 앞 리스너가 바꾼 국면이 아니다
    }

    @Test
    void 기록_사본을_바꿔도_국면과_기록이_어긋나지_않는다() {
        Game game = newGame();
        play(game, "e2e4", "e7e5");

        game.history().push(RULES.fromUci(game.snapshot(), "g1f3").orElseThrow(), "fake");
        game.history().pop();

        assertEquals(2, game.history().moves().size());
        game.undo(2);
        assertEquals(Fen.INITIAL, Fen.write(game.snapshot()));
        assertEquals(0, game.history().moves().size());
    }

    @Test
    void 기록_사본의_반복_횟수는_원본과_같다() {
        Game game = newGame();
        play(game, "g1f3", "g8f6", "f3g1", "f6g8");   // 시작 국면 2회째
        assertEquals(2, game.history().occurrences(RULES.repetitionKey(Position.initial())));
    }

    @Test
    void 시작부터_끝난_국면이면_결과가_있다() throws Exception {
        Game game = new Game(Fen.parse("7k/5QQ1/8/8/8/8/8/K7 b - - 0 1"), RULES);
        assertTrue(game.result().isPresent());
        assertTrue(game.legalMoves().isEmpty());
    }
}
