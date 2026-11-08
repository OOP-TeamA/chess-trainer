package com.oopteama.chess.core.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.GameHistory;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** RULE-05: 킹이 없거나 한쪽만 있는 교육용 국면. */
class SandboxRulesTest {

    private static final RuleSet RULES = new SandboxRules();

    private static Set<String> legalUci(String fen) throws Exception {
        return RULES.legalMoves(Fen.parse(fen)).stream()
            .map(Move::uci).collect(Collectors.toCollection(TreeSet::new));
    }

    @Test
    void loneKnightWorksWithoutAnyKing() throws Exception {
        Position pos = Fen.parse("8/8/8/4N3/8/8/8/8 w - - 0 1");
        assertEquals(8, RULES.legalMoves(pos).size());
        assertFalse(RULES.inCheck(pos));
        Move m = RULES.fromUci(pos, "e5f7").orElseThrow();
        pos.make(m);
        assertEquals("8/5N2/8/8/8/8/8/8 b - - 1 1", Fen.write(pos));
    }

    @Test
    void checkEndIsAlwaysEmpty() throws Exception {
        // 일반 규칙이라면 스테일메이트인 국면에서도 끝나지 않는다
        String fen = "7k/5Q2/6K1/8/8/8/8/8 b - - 0 1";
        Position pos = Fen.parse(fen);
        assertEquals(Optional.empty(), RULES.checkEnd(pos, new GameHistory(fen, RULES.repetitionKey(pos))));
    }

    @Test
    void ownKingIsNotLeftInCheckEvenWithoutAnEnemyKing() throws Exception {
        // e1 킹이 e8 룩에게 체크: e파일 밖으로만 나갈 수 있다
        assertEquals(Set.of("e1d1", "e1d2", "e1f1", "e1f2"), legalUci("4r3/8/8/8/8/8/8/4K3 w - - 0 1"));
    }

    @Test
    void castlingAllowedWhenKingAndRookAreInPlaceEvenWithoutRights() throws Exception {
        Set<String> moves = legalUci("8/8/8/8/8/8/8/R3K2R w - - 0 1");
        assertTrue(moves.contains("e1g1"));
        assertTrue(moves.contains("e1c1"));
    }

    @Test
    void castlingStillRespectsAttackedSquaresAndMissingRook() throws Exception {
        assertFalse(legalUci("5r2/8/8/8/8/8/8/R3K2R w - - 0 1").contains("e1g1")); // f1 공격
        assertFalse(legalUci("8/8/8/8/8/8/8/4K2R w - - 0 1").contains("e1c1"));    // a1에 룩 없음
    }
}
