package com.oopteama.chess.ui.controller;

import com.oopteama.chess.data.Settings.BoardTheme;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.ui.screen.SettingsScreen;

import javax.swing.JComponent;

/**
 * 설정 화면 컨트롤러입니다. 체크박스나 테마를 바꾸면 바로 저장하고 다른 화면에도 알립니다.
 */
public final class SettingsController implements ScreenController {

    private final ChessBackend backend;
    private final SettingsScreen screen = new SettingsScreen();

    public SettingsController(ChessBackend backend) {
        this.backend = backend;
        screen.coordsCheck().addActionListener(e ->
                backend.updateSettings(s -> s.setShowCoords(screen.coordsCheck().isSelected())));
        screen.legalMovesCheck().addActionListener(e ->
                backend.updateSettings(s -> s.setShowLegalMoves(screen.legalMovesCheck().isSelected())));
        screen.lastMoveCheck().addActionListener(e ->
                backend.updateSettings(s -> s.setHighlightLastMove(screen.lastMoveCheck().isSelected())));
        screen.autoFlipCheck().addActionListener(e ->
                backend.updateSettings(s -> s.setAutoFlipLocal(screen.autoFlipCheck().isSelected())));
        refresh();
    }

    private void pickTheme(BoardTheme theme) {
        backend.updateSettings(s -> s.setBoardTheme(theme));
        refresh();
    }

    private void refresh() {
        screen.showSettings(backend.settings(), this::pickTheme);
    }

    @Override
    public JComponent view() {
        return screen;
    }

    @Override
    public void onShow() {
        refresh();
    }
}
