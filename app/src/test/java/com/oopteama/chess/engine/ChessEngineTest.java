package com.oopteama.chess.engine;

import com.oopteama.chess.engine.ChessAI.Difficulty;
import com.oopteama.chess.engine.Piece.Side;
import com.oopteama.chess.engine.Piece.Type;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ChessEngineTest {

    private static long perft(Board board, int depth) {
        if (depth == 0) {
            return 1;
        }
        List<Move> moves = board.legalMoves();
        if (depth == 1) {
            return moves.size();
        }
        long nodes = 0;
        for (Move move : moves) {
            nodes += perft(board.applyMove(move), depth - 1);
        }
        return nodes;
    }

    @Test
    void initialPerftMatchesStandardCounts() {
        Board initial = Board.initial();
        assertEquals(Board.INITIAL_FEN, initial.toFen());
        assertEquals(20, perft(initial, 1));
        assertEquals(400, perft(initial, 2));
        assertEquals(8_902, perft(initial, 3));
    }

    @Test
    void kiwipetePerftVerifiesCastlingEnPassantAndPromotions() {
        Board kiwipete = Board.fromFen("r3k2r/p1ppqpb1/bn2pnp1/3PN3/1p2P3/2N2Q1p/PPPBBPPP/R3K2R w KQkq - 0 1");
        assertEquals(48, perft(kiwipete, 1));
        assertEquals(2_039, perft(kiwipete, 2));
    }

    @Test
    void specialRulesCastlingEnPassantAndPromotionWork() {
        // 캐슬링: f1이 공격받으면 킹사이드 불가, 퀸사이드는 가능
        Board castle = Board.fromFen("4kr2/8/8/8/8/8/8/R3K2R w KQ - 0 1");
        List<String> ucis = castle.legalMoves().stream().map(Move::uci).toList();
        assertFalse(ucis.contains("e1g1"));
        assertTrue(ucis.contains("e1c1"));

        // 앙파상 실행 후 상대 폰 제거
        ChessGame epGame = ChessGame.fromFen("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1");
        Move.Record rec = epGame.playUci("e5d6");
        assertEquals(Move.Kind.EN_PASSANT, rec.kind());
        assertEquals(Piece.BP, rec.captured());
        assertEquals(Piece.EMPTY, epGame.board().pieceAt("d5"));

        // 승진
        ChessGame promoGame = ChessGame.fromFen("7k/1P6/8/8/8/8/8/6K1 w - - 0 1");
        Move.Record promo = promoGame.playUci("b7b8q");
        assertEquals(Piece.WQ, promoGame.board().pieceAt("b8"));
        assertEquals("b8=Q+", promo.san());
    }

    @Test
    void operaGameSanReplayEndsInCheckmateAndUndoRestores() {
        String[] moves = {
                "e4", "e5", "Nf3", "d6", "d4", "Bg4", "dxe5", "Bxf3",
                "Qxf3", "dxe5", "Bc4", "Nf6", "Qb3", "Qe7", "Nc3", "c6",
                "Bg5", "b5", "Nxb5", "cxb5", "Bxb5+", "Nbd7", "O-O-O", "Rd8",
                "Rxd7", "Rxd7", "Rd1", "Qe6", "Bxd7+", "Nxd7", "Qb8+", "Nxb8",
                "Rd8#"
        };
        ChessGame game = new ChessGame();
        for (String san : moves) {
            game.playSan(san);
        }
        assertEquals(ChessGame.Status.CHECKMATE, game.status());
        assertEquals(Side.WHITE, game.winner());
        assertEquals("Rd8#", game.lastMove().orElseThrow().san());

        assertTrue(game.undo());
        assertEquals(ChessGame.Status.PLAYING, game.status());
        assertFalse(game.isOver());
    }

    @Test
    void drawConditionsWork() {
        // 스테일메이트
        ChessGame stale = ChessGame.fromFen("7k/5Q2/6K1/8/8/8/8/8 b - - 0 1");
        assertEquals(ChessGame.Status.STALEMATE, stale.status());

        // 기물 부족
        assertTrue(Board.fromFen("4k3/8/8/8/8/8/8/4K3 w - - 0 1").isInsufficientMaterial());
        assertTrue(Board.fromFen("4k3/8/8/8/8/8/4N3/4K3 w - - 0 1").isInsufficientMaterial());
        assertTrue(Board.fromFen("4k3/8/3b4/8/8/8/5B2/4K3 w - - 0 1").isInsufficientMaterial()); // 같은 색(어두운 칸) 비숍
        assertFalse(Board.fromFen("4k3/8/2b5/8/8/8/5B2/4K3 w - - 0 1").isInsufficientMaterial()); // 다른 색 칸 비숍

        // 3회 동형 반복
        ChessGame rep = new ChessGame();
        for (int i = 0; i < 2; i++) {
            rep.playSan("Nf3");
            rep.playSan("Nf6");
            rep.playSan("Ng1");
            rep.playSan("Ng8");
        }
        assertEquals(3, rep.repetitionCount());
        assertEquals(ChessGame.Status.DRAW_REPETITION, rep.status());
    }

    @Test
    void sandboxAllowsKinglessTutorialBoards() {
        Board knightOnly = Board.fromSandboxFen("8/8/8/3N4/8/8/8/8 w - - 0 1");
        assertEquals(8, knightOnly.legalMoves().size());
        Board moved = knightOnly.applyMove(Move.fromUci("d5e7"));
        assertEquals(Piece.WN, moved.pieceAt("e7"));
    }

    @Test
    void aiFindsMateInOneAndMateInTwo() {
        ChessAI ai = new ChessAI(new Random(42));

        // 메이트인1
        Board mate1 = Board.fromFen("6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1");
        Optional<Move> m1 = ai.chooseMove(mate1, Difficulty.EASY);
        assertTrue(m1.isPresent());
        assertTrue(mate1.applyMove(m1.get()).isCheckmate());

        // 메이트인2 (깊이 3 이상, 유일 수 e5e8 -> a8e8 -> e1e8#)
        Board mate2 = Board.fromFen("r5k1/5ppp/8/4Q3/8/8/5PPP/4R1K1 w - - 0 1");
        Optional<Move> m2 = ai.chooseMove(mate2, Difficulty.MEDIUM);
        assertTrue(m2.isPresent());
        assertEquals("e5e8", m2.get().uci());

        // 취소 플래그 즉시 중단
        assertTrue(ai.chooseMove(Board.initial(), Difficulty.HARD, () -> true).isEmpty());
    }

    @Test
    void evaluateMoveExplainsGoodMovesBlundersEscapesAndExposedAllies() {
        ChessAI ai = new ChessAI(new Random(42));

        // 1. 스콜라즈 메이트: Qxf7#은 MATE, Bxf7+는 메이트 놓침(RISKY) + Ke7 탈출 응수 표시
        Board scholar = Board.fromFen("r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4");
        int scholarBase = ai.baselineScore(scholar);
        ChessAI.MoveEvaluation qMate = ai.evaluateMove(scholar, Move.fromUci("h5f7"), scholarBase);
        assertEquals(ChessAI.Verdict.MATE, qMate.verdict());
        assertTrue(qMate.checkmate());

        ChessAI.MoveEvaluation bCheck = ai.evaluateMove(scholar, Move.fromUci("c4f7"), scholarBase);
        assertEquals(ChessAI.Verdict.RISKY, bCheck.verdict());
        assertTrue(bCheck.check());
        assertFalse(bCheck.checkmate());
        assertEquals(Move.fromUci("e8e7"), bCheck.opponentReply());
        assertEquals("Ke7", bCheck.opponentReplySan());

        // 2. 퀸 희생 메이트인2: 희생 수(e5e8)를 위험(BLUNDER)이 아닌 좋은 수(GOOD)로 평가
        Board mate2 = Board.fromFen("r5k1/5ppp/8/4Q3/8/8/5PPP/4R1K1 w - - 0 1");
        int mate2Base = ai.baselineScore(mate2);
        ChessAI.MoveEvaluation sac = ai.evaluateMove(mate2, Move.fromUci("e5e8"), mate2Base);
        assertEquals(ChessAI.Verdict.GOOD, sac.verdict());
        assertEquals(Move.fromUci("a8e8"), sac.opponentReply());

        // 3. 로열 포크 (나이트가 c7로 가며 e8 킹과 a8 룩 동시 공격)
        Board forkBoard = Board.fromFen("r3k3/ppp2ppp/8/3N4/8/8/PPP2PPP/4K3 w - - 0 1");
        int forkBase = ai.baselineScore(forkBoard);
        ChessAI.MoveEvaluation fork = ai.evaluateMove(forkBoard, Move.fromUci("d5c7"), forkBase);
        assertEquals(ChessAI.Verdict.GOOD, fork.verdict());
        assertTrue(fork.check());
        assertTrue(fork.isFork());
        assertTrue(fork.attackedEnemies().contains(Board.parseSquare("a8")));
        assertTrue(fork.attackedEnemies().contains(Board.parseSquare("e8")));

        // 4. 공짜로 퀸 내주는 수 -> BLUNDER + 상대가 퀸을 잡는 응수 표시
        Board hangBoard = Board.fromFen("4k3/3p4/8/8/3Q4/8/8/4K3 w - - 0 1");
        int hangBase = ai.baselineScore(hangBoard);
        ChessAI.MoveEvaluation hang = ai.evaluateMove(hangBoard, Move.fromUci("d4c6"), hangBase);
        assertEquals(ChessAI.Verdict.BLUNDER, hang.verdict());
        assertEquals(Move.fromUci("d7c6"), hang.opponentReply());

        // 5. 방어 이탈 (d1 룩이 d4 나이트를 지키고 있다가 a1로 빠지면 d8 흑 룩에게 d4 나이트 피격)
        Board exposeBoard = Board.fromFen("3rk3/8/8/8/3N4/8/8/3RK3 w - - 0 1");
        int exposeBase = ai.baselineScore(exposeBoard);
        ChessAI.MoveEvaluation expose = ai.evaluateMove(exposeBoard, Move.fromUci("d1a1"), exposeBase);
        assertEquals(ChessAI.Verdict.BLUNDER, expose.verdict());
        assertTrue(expose.newlyExposedAllies().contains(Board.parseSquare("d4")));
        assertEquals(Move.fromUci("d8d4"), expose.opponentReply());

        // 6. 디스커버드 체크
        Board discBoard = Board.fromFen("4k3/8/8/8/8/4B3/8/4R1K1 w - - 0 1");
        int discBase = ai.baselineScore(discBoard);
        ChessAI.MoveEvaluation disc = ai.evaluateMove(discBoard, Move.fromUci("e3c5"), discBase);
        assertTrue(disc.check());
        assertEquals(List.of(Board.parseSquare("e1")), disc.checkingAttackers());
    }
}
