package com.oopteama.chess.ai;

import com.oopteama.chess.ai.MoveInsight.Reason;
import com.oopteama.chess.ai.MoveInsight.Verdict;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 수 설명(MoveAdvisor)이 판정과 이유를 맞게 만드는지 확인합니다.
 */
class MoveAdvisorTest {

    private final MoveAdvisor advisor = new MoveAdvisor(); // 실제 화면과 같은 설정 (잡기 정리 켬)

    @Test
    void defendingTheAttackedPawnIsExplainedAndAnEvenTradeIsNotCalledDangerous() {
        String scandinavian = "rnbqkbnr/ppp1pppp/8/3p4/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2"; // 1. e4 d5

        MoveInsight knight = insightOf(scandinavian, "b1c3");
        assertTrue(knight.verdict().isGood(), knight.verdict().toString());
        assertTrue(knight.reasons().stream().anyMatch(reason -> reason.text().equals("e4의 폰을 지킵니다")),
                knight.reasons().toString());

        MoveInsight capture = insightOf(scandinavian, "e4d5");
        assertEquals(Verdict.BEST, capture.verdict());
        assertTrue(capture.reasons().stream().allMatch(Reason::good), "폰끼리 바꾸는 것은 위험이 아닙니다: " + capture.reasons());

        MoveInsight edgePawn = insightOf(scandinavian, "a2a3");
        assertEquals(Verdict.INACCURATE, edgePawn.verdict());
        assertEquals(Move.of("d5e4"), edgePawn.reply(), "e4 폰을 지키지 않으면 상대가 잡습니다.");
    }

    @Test
    void queenMovedWhereAPawnCanTakeItIsBadAndPointsAtThePawn() {
        MoveInsight insight = insightOf("4k3/8/5p2/8/8/8/3Q4/4K3 w - - 0 1", "d2g5");

        assertEquals(Verdict.BAD, insight.verdict());
        assertEquals(Move.of("f6g5"), insight.reply(), "상대의 예상 응수는 퀸을 잡는 수여야 합니다.");
        Reason danger = firstBadReason(insight);
        assertTrue(danger.text().contains("퀸을 공짜로"), danger.text());
        assertEquals(Position.of("f6"), danger.from());
        assertEquals(List.of(Position.of("g5")), danger.targets());
    }

    @Test
    void takingAnUndefendedRookIsBestAndShowsTheGain() {
        MoveInsight insight = insightOf("4k3/8/8/3r4/8/8/3Q4/4K3 w - - 0 1", "d2d5");

        assertEquals(Verdict.BEST, insight.verdict());
        assertEquals(0, insight.loss());
        Reason capture = insight.reasons().getFirst();
        assertTrue(capture.good());
        assertTrue(capture.text().contains("룩을") && capture.text().contains("+5"), capture.text());
        assertEquals(List.of(Position.of("d5")), capture.targets());
    }

    @Test
    void backRankMateIsBestAndSaysCheckmate() {
        MoveInsight insight = insightOf("6k1/5ppp/8/8/8/8/8/R5K1 w - - 0 1", "a1a8");

        assertEquals(Verdict.BEST, insight.verdict());
        assertNull(insight.reply(), "메이트 뒤에는 상대가 둘 수가 없습니다.");
        assertTrue(insight.reasons().getFirst().text().startsWith("체크메이트"));
        assertEquals(List.of(Position.of("g8")), insight.reasons().getFirst().targets());
    }

    @Test
    void knightForkExplainsTheCheckAndTheRookItWins() {
        MoveInsight insight = insightOf("r3k3/8/8/1N6/8/8/8/4K3 w - - 0 1", "b5c7");

        assertEquals(Verdict.BEST, insight.verdict());
        List<String> texts = insight.reasons().stream().map(Reason::text).toList();
        assertTrue(texts.stream().anyMatch(text -> text.startsWith("체크")), texts.toString());
        assertTrue(texts.stream().anyMatch(text -> text.equals("상대 룩을 노립니다")), texts.toString());
    }

    @Test
    void movingAwayTheOnlyDefenderWarnsAboutTheExposedPiece() {
        // 룩이 d1을 떠나면 비숍이 노리는 d2의 나이트를 지켜 줄 기물이 없어집니다.
        MoveInsight insight = insightOf("4k3/8/8/8/1b6/8/3N4/3R2K1 w - - 0 1", "d1a1");

        Reason exposed = firstBadReason(insight);
        assertTrue(exposed.text().startsWith("d2의 나이트가"), exposed.text());
        assertEquals(Position.of("b4"), exposed.from());
        assertEquals(Move.of("b4d2"), insight.reply());
        assertTrue(!insight.verdict().isGood(), insight.verdict().toString());
    }

    @Test
    void everyLegalMoveIsExplainedAndOneIsBest() {
        Map<Move, MoveInsight> all = advisor.analyzeAll(Board.initial(), () -> false);

        assertEquals(20, all.size());
        assertTrue(all.values().stream().allMatch(insight -> !insight.reasons().isEmpty()));
        assertTrue(all.values().stream().anyMatch(insight -> insight.verdict() == Verdict.BEST));
    }

    @Test
    void cancelledAnalysisReturnsNothing() {
        assertTrue(advisor.analyzeAll(Board.initial(), () -> true).isEmpty());
    }

    private MoveInsight insightOf(String fen, String move) {
        MoveInsight insight = advisor.analyzeAll(Board.fromFen(fen), () -> false).get(Move.of(move));
        assertNotNull(insight, move + "의 설명이 있어야 합니다.");
        return insight;
    }

    private static Reason firstBadReason(MoveInsight insight) {
        return insight.reasons().stream().filter(reason -> !reason.good()).findFirst()
                .orElseThrow(() -> new AssertionError("나쁜 점이 하나는 있어야 합니다: " + insight.reasons()));
    }
}
