package com.oopteama.chess.exercise;

/**
 * 피드백 사다리(P2, RULE-03). 오답 횟수에 따라 도움 단계를 올린다.
 *
 * <p>1회 EXPLAIN(이유 설명), 2회 HINT(정답 기물·칸 강조), 3회 이상 REVEAL(정답 공개).
 * 정답을 공개해도 사용자가 정답을 입력해야 진행한다. 상태를 가지며 EDT 전용이다.
 */
public final class FeedbackLadder {

    /** 사다리 단계. */
    public enum Stage {
        /** 아직 오답 없음. */
        NONE,
        /** 오답 1회: 무엇이 틀렸는지 설명하고 국면을 되돌린다. */
        EXPLAIN,
        /** 오답 2회: 정답 기물이나 도착 칸을 강조한다. */
        HINT,
        /** 오답 3회 이상: 정답 수의 화살표나 정답 칸을 표시한다. */
        REVEAL
    }

    private int wrongCount;

    /**
     * 오답 하나를 기록하고 새 단계를 돌려준다. NONE → EXPLAIN → HINT → REVEAL, 이후 REVEAL 유지.
     *
     * @return 기록 후 단계
     */
    public Stage recordWrong() {
        wrongCount++;
        return stage();
    }

    /**
     * 현재 단계.
     *
     * @return 오답 0회면 NONE, 1회 EXPLAIN, 2회 HINT, 3회 이상 REVEAL
     */
    public Stage stage() {
        return switch (wrongCount) {
            case 0 -> Stage.NONE;
            case 1 -> Stage.EXPLAIN;
            case 2 -> Stage.HINT;
            default -> Stage.REVEAL;
        };
    }

    /**
     * 지금까지의 오답 횟수.
     *
     * @return 0 이상
     */
    public int wrongCount() {
        return wrongCount;
    }

    /** 오답 0회, NONE으로 되돌린다. 단계를 바꾸거나 다시 시작할 때 쓴다. */
    public void reset() {
        wrongCount = 0;
    }
}
