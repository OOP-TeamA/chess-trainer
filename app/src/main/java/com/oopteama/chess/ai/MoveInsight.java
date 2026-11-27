package com.oopteama.chess.ai;

import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;

import java.util.List;

/**
 * 수 하나에 대한 설명입니다: 얼마나 좋은 수인지(판정), 최선의 수보다 몇 점 손해인지, 그렇게 본 이유들.
 * 체스판 화면은 이 정보로 호버 애니메이션(화살표·칸 강조·설명 카드)을 그립니다.
 *
 * @param move    설명하는 수
 * @param san     수의 기보 표기 (예: "Nf3")
 * @param verdict 판정
 * @param loss    최선의 수보다 손해인 점수 (100 = 폰 1개). 최선의 수면 0
 * @param reply   이 수를 두면 상대가 둘 것으로 예상되는 최선의 응수 (상대가 둘 수 없으면 null)
 * @param reasons 판정 이유 (중요한 것부터)
 */
public record MoveInsight(Move move, String san, Verdict verdict, int loss, Move reply, List<Reason> reasons) {

    public MoveInsight {
        reasons = List.copyOf(reasons);
    }

    /** 판정: 최선의 수와의 점수 차이로 정합니다. */
    public enum Verdict {
        BEST("최선의 수"),
        GOOD("좋은 수"),
        INACCURATE("아쉬운 수"),
        BAD("나쁜 수");

        private final String label;

        Verdict(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }

        /** 최선의 수나 좋은 수인지. */
        public boolean isGood() {
            return this == BEST || this == GOOD;
        }

        /** 손해 점수로 판정합니다: 폰 0.1개 이하 최선, 0.5개 이하 좋음, 1.5개 이하 아쉬움, 그보다 크면 나쁨. */
        static Verdict fromLoss(int loss) {
            if (loss <= 10) {
                return BEST;
            }
            if (loss <= 50) {
                return GOOD;
            }
            return loss <= 150 ? INACCURATE : BAD;
        }
    }

    /**
     * 이유 한 줄과 그 이유를 체스판에 보여 줄 표시입니다.
     * from이 있으면 from에서 targets의 각 칸으로 화살표를 그리고, 없으면 targets 칸만 강조합니다.
     *
     * @param good 좋은 점이면 true, 나쁜 점이면 false (화면에서 초록·빨강으로 구분)
     */
    public record Reason(boolean good, String text, Position from, List<Position> targets) {

        public Reason {
            targets = List.copyOf(targets);
        }
    }
}
