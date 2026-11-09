package com.oopteama.chess.exercise;

import com.oopteama.chess.core.Move;

import java.util.Objects;

/** 입력 한 번의 채점 결과. 닫힌 계층이라 화면은 switch로 빠짐없이 처리한다. */
public sealed interface Verdict {

    /**
     * 목표를 이뤘다. 다음 단계로 진행할 수 있다.
     *
     * @param message 정답 메시지
     */
    record Solved(String message) implements Verdict {
        /** message는 null 불가. */
        public Solved {
            Objects.requireNonNull(message, "message");
        }
    }

    /**
     * 여러 수짜리 문제의 중간 정답. 상대 응수까지 둔 국면에서 이어서 푼다(퍼즐용).
     *
     * @param reply   이어서 둔 상대 응수
     * @param message 중간 메시지
     */
    record Continue(Move reply, String message) implements Verdict {
        /** 두 필드 모두 null 불가. */
        public Continue {
            Objects.requireNonNull(reply, "reply");
            Objects.requireNonNull(message, "message");
        }
    }

    /**
     * 오답. 국면은 오답 직전으로 돌아가 있다.
     *
     * @param message 무엇이 틀렸는지 알려주는 메시지
     */
    record Wrong(String message) implements Verdict {
        /** message는 null 불가. */
        public Wrong {
            Objects.requireNonNull(message, "message");
        }
    }
}
