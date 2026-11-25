package com.oopteama.chess.ui.controller;

import javax.swing.JComponent;

/**
 * 화면 하나를 맡는 컨트롤러가 지켜야 하는 약속입니다.
 * MainFrame은 이 인터페이스만 보고 화면을 보여 주거나 숨깁니다.
 * (컨트롤러는 화면(View)을 만들고, 버튼 같은 이벤트를 받아 처리한 뒤 화면을 갱신합니다.)
 */
public interface ScreenController {

    /** 이 컨트롤러가 관리하는 화면. */
    JComponent view();

    /** 화면이 보이기 직전에 호출됩니다. */
    default void onShow() {
    }

    /** 다른 화면으로 넘어가기 직전에 호출됩니다. */
    default void onHide() {
    }

    /** 설정이나 프로필 같은 데이터가 바뀌었을 때 호출됩니다. */
    default void onDataChanged() {
    }

    /** 화면을 떠나기 전에 확인받을 문구. 확인이 필요 없으면 null입니다. */
    default String leaveWarning() {
        return null;
    }
}
