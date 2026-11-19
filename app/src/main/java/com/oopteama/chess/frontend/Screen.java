package com.oopteama.chess.frontend;

import javax.swing.JComponent;

/**
 * MainWindow가 모든 화면을 다형적으로 관리하기 위한 공통 계약입니다. (ISP / OCP)
 */
public interface Screen {

    enum Id {
        HOME("홈"),
        LESSONS("레슨"),
        PUZZLES("퍼즐"),
        AI_GAME("AI 대전"),
        LOCAL_GAME("로컬 대전"),
        PROFILES("프로필"),
        SETTINGS("설정");

        private final String label;

        Id(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    JComponent view();

    default void onShow() {
    }

    default void onHide() {
    }

    default void onSettingsChanged() {
    }

    default String leaveWarning() {
        return null;
    }
}
