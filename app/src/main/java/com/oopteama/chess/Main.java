package com.oopteama.chess;

import com.oopteama.chess.data.JsonStorage;
import com.oopteama.chess.data.Storage;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.ui.MainFrame;
import com.oopteama.chess.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * 프로그램 시작점입니다.
 * 저장소(JsonStorage) → 서비스(ChessBackend) → 메인 창(MainFrame) 순서로 만들어 연결합니다.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        System.setProperty("apple.awt.application.name", "ChessMate");
        System.setProperty("flatlaf.useWindowDecorations", "true");

        Storage storage = new JsonStorage(JsonStorage.defaultDataDir());
        ChessBackend backend = new ChessBackend(storage);

        SwingUtilities.invokeLater(() -> { // Swing 화면은 화면 전용 스레드에서 만들어야 합니다.
            Theme.install();
            new MainFrame(backend).setVisible(true);
        });
    }
}
