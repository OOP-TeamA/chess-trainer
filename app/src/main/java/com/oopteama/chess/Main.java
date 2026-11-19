package com.oopteama.chess;

import com.oopteama.chess.model.AppStore;
import com.oopteama.chess.ui.MainWindow;
import com.oopteama.chess.ui.Theme;

import javax.swing.SwingUtilities;

/**
 * ChessMate 애플리케이션 진입점입니다.
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.appearance", "NSAppearanceNameDarkAqua");
        System.setProperty("apple.awt.application.name", "ChessMate");
        System.setProperty("flatlaf.useWindowDecorations", "true");

        AppStore store = new AppStore(AppStore.defaultDataDir());
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            new MainWindow(store).setVisible(true);
        });
    }
}
