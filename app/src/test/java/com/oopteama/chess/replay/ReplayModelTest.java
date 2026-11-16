package com.oopteama.chess.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.notation.Pgn;
import com.oopteama.chess.notation.PgnGame;
import com.oopteama.chess.notation.PgnMove;
import java.io.Reader;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** GAME-02·04: 번들 오페라 게임으로 임의 위치 이동과 앞뒤 이동의 일관성을 확인한다. */
class ReplayModelTest {

    static final String OPERA_FINAL_FEN = "1n1Rkb1r/p4ppp/4q3/4p1B1/4P3/8/PPP2PPP/2K5 b k - 1 17";

    private static PgnGame opera;

    @BeforeAll
    static void readOpera() throws Exception {
        opera = readBundledOpera();
    }

    static PgnGame readBundledOpera() throws Exception {
        try (Reader in = new DataPaths(Path.of("unused")).openResource("data/games/opera-1858.txt")) {
            return Pgn.read(in, "opera-1858.txt");
        }
    }

    @Test
    void startsAtTheInitialPositionAndEndsAtTheReferencePosition() {
        ReplayModel model = new ReplayModel(opera);
        assertEquals(0, model.ply());
        assertEquals(33, model.length());
        assertEquals(Fen.INITIAL, Fen.write(model.position()));
        assertEquals(Optional.empty(), model.moveBefore());
        assertEquals("e4", model.moveAfter().orElseThrow().san());

        model.seek(model.length());
        assertEquals(OPERA_FINAL_FEN, Fen.write(model.position()));
        assertEquals("Rd8#", model.moveBefore().orElseThrow().san());
        assertEquals(Optional.empty(), model.moveAfter());
    }

    @Test
    void seekingBackAndForthMatchesPlayingFromTheStart() {
        ReplayModel model = new ReplayModel(opera);
        Position pos = Position.initial();
        for (int ply = 0; ply <= opera.moves().size(); ply++) {
            ReplayModel fresh = new ReplayModel(opera);
            model.seek(opera.moves().size() - ply);   // 끝에서 거꾸로
            fresh.seek(ply);                          // 처음에서 앞으로
            assertEquals(Fen.write(pos), Fen.write(fresh.position()), "ply " + ply);
            if (ply < opera.moves().size()) {
                pos.make(opera.moves().get(ply).move());
            }
        }
        model.seek(19);
        model.seek(18);
        ReplayModel direct = new ReplayModel(opera);
        direct.seek(18);
        assertEquals(Fen.write(direct.position()), Fen.write(model.position()));
    }

    @Test
    void commentsAndSuffixesComeWithTheMoveJustPlayed() {
        ReplayModel model = new ReplayModel(opera);
        model.seek(6);
        PgnMove bg4 = model.moveBefore().orElseThrow();
        assertEquals("Bg4", bg4.san());
        assertTrue(bg4.comment().contains("[[핀]]"), bg4.comment());

        model.seek(19);
        PgnMove nxb5 = model.moveBefore().orElseThrow();
        assertEquals("Nxb5", nxb5.san());
        assertEquals("!", nxb5.suffix());
    }

    @Test
    void positionIsACopy() {
        ReplayModel model = new ReplayModel(opera);
        Position copy = model.position();
        copy.make(opera.moves().get(0).move());
        assertEquals(Fen.INITIAL, Fen.write(model.position()));
        assertEquals(Square.of("e4"), opera.moves().get(0).move().to());
    }

    @Test
    void seekOutsideTheGameIsRejected() {
        ReplayModel model = new ReplayModel(opera);
        assertThrows(IllegalArgumentException.class, () -> model.seek(-1));
        assertThrows(IllegalArgumentException.class, () -> model.seek(34));
        assertEquals(0, model.ply());
    }

    @Test
    void listenersRunOnlyWhenThePositionChangesAndCanBeRemoved() {
        ReplayModel model = new ReplayModel(opera);
        AtomicInteger calls = new AtomicInteger();
        Runnable listener = calls::incrementAndGet;
        model.addChangeListener(listener);

        model.seek(0);
        assertEquals(0, calls.get());
        model.seek(5);
        model.seek(4);
        assertEquals(2, calls.get());

        model.removeChangeListener(listener);
        model.seek(10);
        assertEquals(2, calls.get());
    }

    @Test
    void customStartAndEmptyGamesWork() {
        String fen = "4k3/8/8/8/8/8/8/4K2R w K - 0 1";
        PgnGame empty = new PgnGame(Map.of(), fen, List.of(), "*");
        ReplayModel model = new ReplayModel(empty);
        assertEquals(0, model.length());
        assertEquals(fen, Fen.write(model.position()));
        assertEquals(Optional.empty(), model.moveAfter());

        PgnGame broken = new PgnGame(Map.of(), "not a fen", List.of(), "*");
        assertThrows(IllegalArgumentException.class, () -> new ReplayModel(broken));
    }
}
