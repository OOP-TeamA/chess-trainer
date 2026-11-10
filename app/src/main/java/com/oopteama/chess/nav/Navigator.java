package com.oopteama.chess.nav;

public interface Navigator {

    //현재 화면의 leaveWarning이 있으면 확인 대화상자 후 화면을 이동한다.//
    void go(Route route);

    // 이전 화면으로 이동
    void back();
}