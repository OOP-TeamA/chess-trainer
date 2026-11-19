package com.oopteama.chess;

import com.oopteama.chess.backend.service.ChessBackend;
import com.oopteama.chess.backend.service.JsonStorage;
import com.oopteama.chess.backend.service.Storage;
import com.oopteama.chess.frontend.MainWindow;
import com.oopteama.chess.frontend.Theme;

import javax.swing.SwingUtilities;

/**
 * 조립 루트(Composition Root): 저장소(JsonStorage) → 백엔드(ChessBackend) → 프론트엔드(MainWindow)를 연결합니다.
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

        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainWindow(backend).setVisible(true);
        });
    }
}
