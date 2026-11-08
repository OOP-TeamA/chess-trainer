package com.oopteama.chess.core.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.GameResult.Outcome;
import com.oopteama.chess.core.GameResult.Reason;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.Position;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class StandardRulesTest {

    private static final StandardRules RULES = new StandardRules();
    private static final String KIWIPETE =
        "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1";

    private static Set<String> legalUci(String fen) throws Exception {
        return RULES.legalMoves(Fen.parse(fen)).stream()
            .map(Move::uci).collect(Collectors.toCollection(TreeSet::new));
    }

    private static Optional<GameResult> end(String fen) throws Exception {
        Position pos = Fen.parse(fen);
        return RULES.checkEnd(pos, new GameHistory(fen, RULES.repetitionKey(pos)));
    }

    // ---- CORE-03: 합법 수 ----

    @Test
    void pinnedPieceCannotLeaveThePinLine() throws Exception {
        // e2의 비숍은 e8 룩에 핀 되어 있어 어디로도 갈 수 없다
        for (String uci : legalUci("4r1k1/8/8/8/8/8/4B3/4K3 w - - 0 1")) {
            assertFalse(uci.startsWith("e2"), "핀 된 비숍이 움직임: " + uci);
        }
    }

    @Test
    void onlyCheckEvadingMovesWhileInCheck() throws Exception {
        String fen = "4k3/8/8/8/8/8/8/r3K3 w - - 0 1";
        assertTrue(RULES.inCheck(Fen.parse(fen)));
        // d1, f1은 룩 선 위(f1은 킹이 비켜도 룩이 통과해 공격)라 불가
        assertEquals(Set.of("e1d2", "e1e2", "e1f2"), legalUci(fen));
    }

    @Test
    void legalMovesDoNotMutateTheGivenPosition() throws Exception {
        Position pos = Fen.parse(KIWIPETE);
        String before = Fen.write(pos);
        RULES.legalMoves(pos);
        assertEquals(before, Fen.write(pos));
        assertEquals(0, pos.undoDepth());
    }

    // ---- CORE-04: 특수 규칙 ----

    @Test
    void castlingIsGeneratedWhenAllowed() throws Exception {
        Set<String> moves = legalUci("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1");
        assertTrue(moves.contains("e1g1"));
        assertTrue(moves.contains("e1c1"));
    }

    @Test
    void castlingIsForbiddenThroughOrOutOfCheck() throws Exception {
        Set<String> throughCheck = legalUci("r3kr2/8/8/8/8/8/8/R3K2R w KQ - 0 1"); // f1 공격
        assertFalse(throughCheck.contains("e1g1"));
        assertTrue(throughCheck.contains("e1c1"));

        Set<String> outOfCheck = legalUci("4r1k1/8/8/8/8/8/8/R3K2R w KQ - 0 1");   // 킹이 체크 중
        assertFalse(outOfCheck.contains("e1g1"));
        assertFalse(outOfCheck.contains("e1c1"));
    }

    @Test
    void queensideCastlingAllowedWhenOnlyB1IsAttackedButNotWhenBlocked() throws Exception {
        assertTrue(legalUci("1r4k1/8/8/8/8/8/8/R3K2R w KQ - 0 1").contains("e1c1"));
        Set<String> blocked = legalUci("r3k2r/8/8/8/8/8/8/RN2K2R w KQkq - 0 1");
        assertFalse(blocked.contains("e1c1"));
        assertTrue(blocked.contains("e1g1"));
    }

    @Test
    void castlingNeedsTheRightFlag() throws Exception {
        Set<String> moves = legalUci("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1");
        assertFalse(moves.contains("e1g1"));
        assertFalse(moves.contains("e1c1"));
    }

    @Test
    void enPassantIsLegalNormally() throws Exception {
        Position pos = Fen.parse("4k3/8/8/8/3p4/8/4P3/4K3 w - - 0 1");
        pos.make(RULES.fromUci(pos, "e2e4").orElseThrow());
        assertTrue(RULES.legalMoves(pos).stream().anyMatch(m -> m.kind() == MoveKind.EN_PASSANT));
    }

    @Test
    void enPassantIsIllegalWhenItExposesTheKingAlongTheRank() throws Exception {
        // d4xe3 e.p.는 e4, d4 두 폰을 한꺼번에 치워 b4 킹이 h4 룩에 노출된다
        assertFalse(legalUci("8/8/8/8/1k1pP2R/8/8/4K3 b - e3 0 1").contains("d4e3"));
    }

    @Test
    void promotionGeneratesFourMoves() throws Exception {
        Set<String> moves = legalUci("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        assertTrue(moves.containsAll(Set.of("a7a8q", "a7a8r", "a7a8b", "a7a8n")));
        assertFalse(moves.contains("a7a8"));
    }

    // ---- CORE-05, 06: 종료 판정 ----

    @Test
    void checkmate() throws Exception {
        String foolsMate = "rnb1kbnr/pppp1ppp/8/4p3/6Pq/5P2/PPPPP2P/RNBQKBNR w KQkq - 1 3";
        assertEquals(Optional.of(new GameResult(Outcome.BLACK_WINS, Reason.CHECKMATE)), end(foolsMate));
    }

    @Test
    void stalemate() throws Exception {
        assertEquals(Optional.of(new GameResult(Outcome.DRAW, Reason.STALEMATE)),
            end("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1"));
    }

    @Test
    void insufficientMaterialOnlyForKvKKBvKAndKNvK() throws Exception {
        GameResult draw = new GameResult(Outcome.DRAW, Reason.INSUFFICIENT_MATERIAL);
        assertEquals(Optional.of(draw), end("8/8/8/4k3/8/3K4/8/8 w - - 0 1"));
        assertEquals(Optional.of(draw), end("8/8/8/4k3/8/3K4/2B5/8 w - - 0 1"));
        assertEquals(Optional.of(draw), end("8/8/8/4k3/8/3K4/2N5/8 w - - 0 1"));
        assertEquals(Optional.empty(), end("8/8/8/4k3/8/3K4/2R5/8 w - - 0 1"));
        assertEquals(Optional.empty(), end("8/8/8/4k2b/8/3K4/2B5/8 w - - 0 1"));
    }

    @Test
    void fiftyMoveRule() throws Exception {
        assertEquals(Optional.empty(), end("8/8/8/4k3/8/3K4/2R5/8 w - - 99 80"));
        assertEquals(Optional.of(new GameResult(Outcome.DRAW, Reason.FIFTY_MOVE)),
            end("8/8/8/4k3/8/3K4/2R5/8 w - - 100 80"));
    }

    @Test
    void threefoldRepetition() throws Exception {
        Position pos = Position.initial();
        GameHistory history = new GameHistory(Fen.write(pos), RULES.repetitionKey(pos));
        String[] shuffle = {"g1f3", "g8f6", "f3g1", "f6g8"};
        for (int round = 0; round < 2; round++) {
            assertEquals(Optional.empty(), RULES.checkEnd(pos, history), "아직 반복 3회가 아님");
            for (String uci : shuffle) {
                Move m = RULES.fromUci(pos, uci).orElseThrow();
                pos.make(m);
                history.push(m, RULES.repetitionKey(pos));
            }
        }
        assertEquals(3, history.occurrences(RULES.repetitionKey(pos)));
        assertEquals(Optional.of(new GameResult(Outcome.DRAW, Reason.REPETITION)),
            RULES.checkEnd(pos, history));
    }

    @Test
    void repetitionKeyIncludesEnPassantSquareOnlyWhenCapturePossible() throws Exception {
        Position start = Position.initial();
        start.make(RULES.fromUci(start, "e2e4").orElseThrow());
        assertEquals("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq -", RULES.repetitionKey(start));

        Position capturable = Fen.parse("4k3/8/8/8/3p4/8/4P3/4K3 w - - 0 1");
        capturable.make(RULES.fromUci(capturable, "e2e4").orElseThrow());
        assertEquals("4k3/8/8/8/3pP3/8/8/4K3 b - e3", RULES.repetitionKey(capturable));
    }

    // ---- CORE-09: 국면 검증 ----

    @Test
    void validatePassesForNormalPositions() throws Exception {
        assertEquals(List.of(), RULES.validate(Position.initial()));
        assertEquals(List.of(), RULES.validate(Fen.parse(KIWIPETE)));
    }

    @Test
    void validateReportsEachProblem() throws Exception {
        assertEquals(1, RULES.validate(Fen.parse("8/8/8/8/8/8/8/4K3 w - - 0 1")).size());           // 흑 킹 없음
        assertEquals(1, RULES.validate(Fen.parse("4k3/8/8/8/8/8/8/4K2P w - - 0 1")).size());        // 1랭크 폰
        assertEquals(1, RULES.validate(Fen.parse("4k3/8/8/8/8/8/4R3/4K3 w - - 0 1")).size());       // 차례 아닌 쪽 체크
        assertEquals(2, RULES.validate(Fen.parse("4k3/8/8/8/8/8/8/4K3 w KQ - 0 1")).size());        // 룩 없이 권리만
    }

    // ---- fromUci ----

    @Test
    void fromUciParsesLegalMovesAndRejectsTheRest() throws Exception {
        Position pos = Position.initial();
        assertEquals("e2e4", RULES.fromUci(pos, "e2e4").orElseThrow().uci());
        assertEquals(Optional.empty(), RULES.fromUci(pos, "e2e5"));   // 불법 수
        assertEquals(Optional.empty(), RULES.fromUci(pos, "zz99"));
        assertEquals(Optional.empty(), RULES.fromUci(pos, "e2e4x"));
        assertEquals(Optional.empty(), RULES.fromUci(pos, "e2"));
        assertEquals(Optional.empty(), RULES.fromUci(pos, null));

        Position promo = Fen.parse("4k3/P7/8/8/8/8/8/4K3 w - - 0 1");
        assertEquals("a7a8q", RULES.fromUci(promo, "a7a8q").orElseThrow().uci());
        assertEquals(Optional.empty(), RULES.fromUci(promo, "a7a8"));  // 승진 기물 빠짐
    }

    // ---- CORE-07: make/unmake 왕복 ----

    @Test
    void everyLegalMoveRoundTripsInSeveralPositions() throws Exception {
        for (String fen : List.of(Fen.INITIAL, KIWIPETE,
                "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
                "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1")) {
            Position pos = Fen.parse(fen);
            String before = Fen.write(pos);
            for (Move m : RULES.legalMoves(pos)) {
                pos.make(m);
                pos.unmake();
                assertEquals(before, Fen.write(pos), "수 " + m.uci() + " 왕복 실패");
            }
        }
    }

    @Test
    void randomLegalPlayStaysValidAndUnmakesCleanly() throws Exception {
        Random rnd = new Random(7);
        int castles = 0;
        int enPassants = 0;
        int promotions = 0;
        for (int game = 0; game < 300; game++) {
            Position pos = Position.initial();
            Deque<String> before = new ArrayDeque<>();
            for (int ply = 0; ply < 200; ply++) {
                List<Move> moves = RULES.legalMoves(pos);
                if (moves.isEmpty()) {
                    break;
                }
                Move m = moves.get(rnd.nextInt(moves.size()));
                if (m.kind() == MoveKind.CASTLE_SHORT || m.kind() == MoveKind.CASTLE_LONG) castles++;
                if (m.kind() == MoveKind.EN_PASSANT) enPassants++;
                if (m.isPromotion()) promotions++;

                before.push(Fen.write(pos));
                pos.make(m);
                assertEquals(List.of(), RULES.validate(pos), "합법 수만 뒀는데 국면이 비정상: " + Fen.write(pos));
            }
            while (!before.isEmpty()) {
                pos.unmake();
                assertEquals(before.pop(), Fen.write(pos));
            }
        }
        assertTrue(castles > 0, "캐슬링이 한 번도 안 나와 검증 범위가 부족함");
        assertTrue(promotions > 0, "승진이 한 번도 안 나와 검증 범위가 부족함");
        assertTrue(enPassants > 0, "앙파상이 한 번도 안 나와 검증 범위가 부족함");
    }
}
