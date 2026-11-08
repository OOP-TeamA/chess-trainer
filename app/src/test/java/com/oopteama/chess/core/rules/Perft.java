package com.oopteama.chess.core.rules;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 테스트 전용 perft 도구. 값이 틀리면 divide로 어느 수에서 어긋나는지 좁힌다. */
final class Perft {

    private Perft() {}

    /** depth수 앞까지 전개한 말단 노드 수. */
    static long count(RuleSet rules, Position pos, int depth) {
        if (depth == 0) {
            return 1;
        }
        List<Move> moves = rules.legalMoves(pos);
        if (depth == 1) {
            return moves.size();
        }
        long nodes = 0;
        for (Move m : moves) {
            pos.make(m);
            nodes += count(rules, pos, depth - 1);
            pos.unmake();
        }
        return nodes;
    }

    /** 첫 수마다 그 아래 말단 노드 수. 합은 count(depth)와 같다. */
    static Map<String, Long> divide(RuleSet rules, Position pos, int depth) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Move m : rules.legalMoves(pos)) {
            pos.make(m);
            result.put(m.uci(), count(rules, pos, depth - 1));
            pos.unmake();
        }
        return result;
    }
}
