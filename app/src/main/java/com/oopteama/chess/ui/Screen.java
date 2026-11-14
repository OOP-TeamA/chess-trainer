package com.oopteama.chess.ui;

import java.util.Optional;

import javax.swing.JComponent;

import com.oopteama.chess.nav.Route;

public interface Screen {

    String cardName();

    JComponent component();

    void onShow(Route route);

    void onHide();

    default Optional<String> leaveWarning() {
        return Optional.empty();
    }
}