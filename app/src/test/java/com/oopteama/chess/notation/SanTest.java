package com.oopteama.chess.notation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.MoveKind;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.rules.StandardRules;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class SanTest {

    private static final StandardRules RULES = new StandardRules();

    private static Move parse(String fen, String san) throws Exception {
        return San.parse(Fen.parse(fen), RULES, san);
    }

    private static String write(String fen, String uci) throws Exception {
        Position pos = Fen.parse(fen);
        Move move = RULES.fromUci(pos, uci).orElseThrow(() -> new AssertionError("불법 수: " + uci));
        return San.write(pos, RULES, move);
    }

    private static SanException.Reason reasonOf(String fen, String san) throws Exception {
        Position pos = Fen.parse(fen);
        return assertThrows(SanException.class, () -> San.parse(pos, RULES, san)).reason();
    }

    @Test
    void parsesPawnAndPieceMovesFromTheStartPosition() throws Exception {
        assertEquals("e2e4", parse(Fen.INITIAL, "e4").uci());
        assertEquals(MoveKind.DOUBLE_PUSH, parse(Fen.INITIAL, "e4").kind());
        assertEquals("e2e3", parse(Fen.INITIAL, "e3").uci());
        assertEquals("g1f3", parse(Fen.INITIAL, "Nf3").uci());
    }

    @Test
    void parsesCapturesAndEnPassant() throws Exception {
        Move capture = parse("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2", "exd5");
        assertEquals("e4d5", capture.uci());
        assertEquals(MoveKind.CAPTURE, capture.kind());

        Move enPassant = parse("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "exd6");
        assertEquals("e5d6", enPassant.uci());
        assertEquals(MoveKind.EN_PASSANT, enPassant.kind());
    }

    @Test
    void parsesCastlingForBothSides() throws Exception {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        assertEquals(MoveKind.CASTLE_SHORT, parse(fen, "O-O").kind());
        assertEquals("e1c1", parse(fen, "O-O-O").uci());
        assertEquals("e1g1", parse(fen, "0-0").uci());
        assertEquals("e8g8", parse("r3k2r/8/8/8/8/8/8/R3K2R b KQkq - 0 1", "O-O+").uci());
        assertEquals(SanException.Reason.ILLEGAL, reasonOf("r3k2r/8/8/8/8/8/8/R3K2R w - - 0 1", "O-O"));
    }

    @Test
    void parsesPromotions() throws Exception {
        String fen = "8/P6k/8/8/8/8/8/K7 w - - 0 1";
        assertEquals("a7a8q", parse(fen, "a8=Q").uci());
        assertEquals("a7a8n", parse(fen, "a8=N+").uci());
        assertEquals("a7a8r", parse(fen, "a8R").uci());
        assertEquals(SanException.Reason.ILLEGAL, reasonOf(fen, "a8")); // 승진 기물이 없음
    }

    @Test
    void checkMarksAndEvaluationSymbolsAreIgnored() throws Exception {
        assertEquals("e2e4", parse(Fen.INITIAL, "e4!").uci());
        assertEquals("g1f3", parse(Fen.INITIAL, "Nf3!?").uci());
        assertEquals("g1f3", parse(Fen.INITIAL, "Nf3+").uci());     // 체크 표시는 검사하지 않는다
        assertEquals("g1f3", parse(Fen.INITIAL, "  Nf3??  ").uci());
    }

    @Test
    void disambiguationByFileRankOrBoth() throws Exception {
        String knights = "4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1";
        assertEquals("b1d2", parse(knights, "Nbd2").uci());
        assertEquals("f1d2", parse(knights, "Nfd2").uci());
        assertEquals(SanException.Reason.AMBIGUOUS, reasonOf(knights, "Nd2"));

        String rooks = "4k3/8/8/8/8/R7/8/R3K3 w - - 0 1";
        assertEquals("a1a2", parse(rooks, "R1a2").uci());
        assertEquals("a3a2", parse(rooks, "R3a2").uci());
        assertEquals(SanException.Reason.AMBIGUOUS, reasonOf(rooks, "Ra2"));

        String queens = "8/7k/8/8/Q7/8/8/Q2QK3 w - - 0 1";
        assertEquals("a1d4", parse(queens, "Qa1d4").uci());
        assertEquals("a4d4", parse(queens, "Q4d4").uci());
        assertEquals("d1d4", parse(queens, "Qdd4").uci());
    }

    @Test
    void reportsSyntaxIllegalAndAmbiguousSeparately() throws Exception {
        assertEquals(SanException.Reason.SYNTAX, reasonOf(Fen.INITIAL, ""));
        assertEquals(SanException.Reason.SYNTAX, reasonOf(Fen.INITIAL, "Zz9"));
        assertEquals(SanException.Reason.SYNTAX, reasonOf(Fen.INITIAL, "e9"));
        assertEquals(SanException.Reason.SYNTAX, reasonOf(Fen.INITIAL, "e4!!!"));
        assertEquals(SanException.Reason.SYNTAX, reasonOf(Fen.INITIAL, "e4!!+"));   // 평가 기호는 +/# 뒤에
        assertEquals(SanException.Reason.SYNTAX, reasonOf(Fen.INITIAL, "Ne8=Q"));
        assertEquals(SanException.Reason.ILLEGAL, reasonOf(Fen.INITIAL, "e5"));
        assertEquals(SanException.Reason.ILLEGAL, reasonOf(Fen.INITIAL, "Ke2"));
        assertEquals(SanException.Reason.ILLEGAL, reasonOf(Fen.INITIAL, "Nxf3"));   // 잡는 수가 아닌데 x
        SanException e = assertThrows(SanException.class, () -> parse(Fen.INITIAL, "Qd4"));
        assertEquals("Qd4", e.san());
    }

    @Test
    void pawnCaptureNeedsTheOriginFileAndTheCaptureMark() throws Exception {
        String fen = "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2";
        assertThrows(SanException.class, () -> parse(fen, "xd5"));
        assertThrows(SanException.class, () -> parse(fen, "ed5"));
        assertEquals(SanException.Reason.ILLEGAL, reasonOf(fen, "d5"));  // 폰은 직진으로 d5에 갈 수 없음
    }

    @Test
    void writesPlainMovesAndCaptures() throws Exception {
        assertEquals("e4", write(Fen.INITIAL, "e2e4"));
        assertEquals("Nf3", write(Fen.INITIAL, "g1f3"));
        assertEquals("exd5", write("rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq d6 0 2", "e4d5"));
        assertEquals("exd6", write("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1", "e5d6"));
    }

    @Test
    void writesCastlingAndPromotion() throws Exception {
        String fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1";
        assertEquals("O-O", write(fen, "e1g1"));
        assertEquals("O-O-O", write(fen, "e1c1"));
        assertEquals("a8=Q", write("8/P6k/8/8/8/8/8/K7 w - - 0 1", "a7a8q"));
        assertEquals("a8=N", write("8/P6k/8/8/8/8/8/K7 w - - 0 1", "a7a8n"));
    }

    @Test
    void writesMinimalDisambiguation() throws Exception {
        assertEquals("Nbd2", write("4k3/8/8/8/8/8/8/1N2KN2 w - - 0 1", "b1d2"));
        assertEquals("R1a2", write("4k3/8/8/8/8/R7/8/R3K3 w - - 0 1", "a1a2"));
        assertEquals("R3a2", write("4k3/8/8/8/8/R7/8/R3K3 w - - 0 1", "a3a2"));
        String queens = "8/7k/8/8/Q7/8/8/Q2QK3 w - - 0 1";
        assertEquals("Qa1d4", write(queens, "a1d4"));
        assertEquals("Q4d4", write(queens, "a4d4"));
        assertEquals("Qdd4", write(queens, "d1d4"));
    }

    @Test
    void writesCheckAndCheckmateMarks() throws Exception {
        assertEquals("Rf8+", write("4k3/8/8/8/8/8/8/4KR2 w - - 0 1", "f1f8"));
        assertEquals("Rd8#", write("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1", "d1d8"));
    }

    @Test
    void writeRejectsMovesThatAreNotLegal() throws Exception {
        Position pos = Fen.parse(Fen.INITIAL);
        Move illegal = new Move(com.oopteama.chess.core.Square.of("e2"), com.oopteama.chess.core.Square.of("e5"), MoveKind.QUIET);
        assertThrows(IllegalArgumentException.class, () -> San.write(pos, RULES, illegal));
    }

    @Test
    void suffixOfFindsTheEvaluationSymbol() {
        assertEquals("!!", San.suffixOf("Qb8+!!"));
        assertEquals("!", San.suffixOf("Nxb5!"));
        assertEquals("?", San.suffixOf("e4?"));
        assertEquals("??", San.suffixOf("e4??"));
        assertEquals("!?", San.suffixOf("e4!?"));
        assertEquals("?!", San.suffixOf("e4?!"));
        assertEquals("", San.suffixOf("Rd8#"));
        assertEquals("", San.suffixOf("e4"));
    }

    @Test
    void everyLegalMoveRoundTripsThroughSan() throws Exception {
        List<String> fens = List.of(
            Fen.INITIAL,
            "r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1",   // Kiwipete
            "8/2p5/3p4/KP5r/1R3p1k/8/4P1P1/8 w - - 0 1",
            "r3k2r/Pppp1ppp/1b3nbN/nP6/BBP1P3/q4N2/Pp1P2PP/R2Q1RK1 w kq - 0 1",
            "rnbq1k1r/pp1Pbppp/2p5/8/2B5/8/PPP1NnPP/RNBQK2R w KQ - 1 8",
            "4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1",
            "8/7k/8/8/Q7/8/8/Q2QK3 w - - 0 1");
        for (String fen : fens) {
            assertRoundTrips(Fen.parse(fen), fen);
        }
    }

    @Test
    void randomGamesRoundTripThroughSan() throws Exception {
        Random random = new Random(20261108);
        for (int game = 0; game < 6; game++) {
            Position pos = Position.initial();
            for (int ply = 0; ply < 100; ply++) {
                List<Move> legal = RULES.legalMoves(pos);
                if (legal.isEmpty()) {
                    break;
                }
                assertRoundTrips(pos, Fen.write(pos));
                pos.make(legal.get(random.nextInt(legal.size())));
            }
        }
    }

    private static void assertRoundTrips(Position pos, String label) throws Exception {
        for (Move m : RULES.legalMoves(pos)) {
            String san = San.write(pos, RULES, m);
            assertEquals(m, San.parse(pos, RULES, san), label + " " + san);
        }
    }
}
