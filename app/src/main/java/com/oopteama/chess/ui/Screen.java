package com.oopteama.chess.ui;

import javax.swing.JComponent;

/**
 * MainWindow가 모든 화면을 다형적으로 관리하기 위한 공통 계약입니다.
 */
public interface Screen {

    JComponent view();

    default void onShow() {
    }

    default void onHide() {
    }

    default void onSettingsChanged() {
    }

    /**
     * 화면을 떠나기 전 확인 메시지가 필요하면 반환하고, 없으면 null을 반환합니다.
     */
    default String leaveWarning() {
        return null;
    }
}
