package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Board;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 규칙상 둘 수 없는 이동에 보여 주는 안내 문구를 검증합니다.
 */
class RetryCardTest {

    @Test
    void explainsOwnPieceWrongDirectionAndBlockedPath() {
        assertEquals("a2에 아군 기물이 있어요.",
                RetryCard.illegalMoveReason(Board.initial(), sq("a1"), sq("a2"), true));

        Board rook = Board.fromSandboxFen("8/8/3P4/8/3R4/8/8/8 w - - 0 1");
        assertEquals("룩은 가로와 세로로만 움직일 수 있어요.",
                RetryCard.illegalMoveReason(rook, sq("d4"), sq("e5"), true));
        assertEquals("d6의 기물이 길을 막고 있어요.",
                RetryCard.illegalMoveReason(rook, sq("d4"), sq("d7"), true));
    }

    @Test
    void genericReasonUsesKoreanParticleAndFollowsMoveDotSetting() {
        Board start = Board.initial();
        assertEquals("b1에서 b3으로는 이 국면에서 이동할 수 없어요. 점으로 표시된 이동 가능 칸 중에서 골라 보세요.",
                RetryCard.illegalMoveReason(start, sq("b1"), sq("b3"), true));
        assertEquals("e2에서 e5로는 이 국면에서 이동할 수 없어요. 이 기물이 갈 수 있는 다른 칸을 찾아보세요.",
                RetryCard.illegalMoveReason(start, sq("e2"), sq("e5"), false));

        assertEquals("g1로", RetryCard.toward(sq("g1")));
        assertEquals("e6으로", RetryCard.toward(sq("e6")));
        assertEquals("h8로", RetryCard.toward(sq("h8")));
    }

    private static int sq(String name) {
        return Board.parseSquare(name);
    }
}
