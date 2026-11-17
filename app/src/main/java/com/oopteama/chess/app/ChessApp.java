package com.oopteama.chess.app;

import com.oopteama.chess.ui.MainFrame;
import com.oopteama.chess.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * 앱 시작 진입점. 조립 컨텍스트를 만들고 EDT에서 테마 설치 후 메인 창을 띄운다.
 */
public final class ChessApp {

    private ChessApp() {
    }

    public static void main(String[] args) {
        AppContext context = new AppContext();
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            MainFrame frame = new MainFrame(context);
            frame.setVisible(true);
        });
    }
}
