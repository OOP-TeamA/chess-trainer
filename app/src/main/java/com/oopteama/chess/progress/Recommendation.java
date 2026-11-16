package com.oopteama.chess.progress;

import com.oopteama.chess.nav.Route;

import java.util.Objects;

/**
 * 홈 화면의 다음 추천 하나(PROG-02). 화면은 제목·이유를 보여 주고, 누르면 {@code navigator.go(route())}로 이동한다.
 *
 * @param route  이동할 화면
 * @param title  카드 제목(예: "레슨 7. 나이트")
 * @param reason 추천 이유 한 문장
 */
public record Recommendation(Route route, String title, String reason) {

    /** 모든 필드는 null 불가. */
    public Recommendation {
        Objects.requireNonNull(route, "route");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(reason, "reason");
    }
}
