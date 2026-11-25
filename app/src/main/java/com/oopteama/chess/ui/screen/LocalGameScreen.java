package com.oopteama.chess.ui.screen;

import com.oopteama.chess.data.Settings;
import com.oopteama.chess.ui.Theme;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * 로컬 2인 대전 화면(View): 공통 대국 화면에 제목·자동 뒤집기 배지와 무르기·뒤집기·기권·새 대국 버튼을 더합니다.
 */
public final class LocalGameScreen extends GameScreen {

    private final JLabel autoFlipBadge = Theme.badge("", Theme.INFO);
    private final JButton undoButton = Theme.secondaryButton("무르기");
    private final JButton flipButton = Theme.secondaryButton("보드 뒤집기");
    private final JButton resignButton = Theme.dangerButton("기권");
    private final JButton newGameButton = Theme.primaryButton("새 대국");

    public LocalGameScreen(Settings settings) {
        super(settings);
        JPanel bar = Theme.card(10);
        bar.setLayout(new BorderLayout(10, 0));
        bar.add(Theme.sectionTitle("로컬 2인 대전"), BorderLayout.WEST);
        bar.add(autoFlipBadge, BorderLayout.EAST);
        setTopBar(bar);
        addControls(undoButton, flipButton, resignButton, newGameButton);
    }

    public JButton undoButton() {
        return undoButton;
    }

    public JButton flipButton() {
        return flipButton;
    }

    public JButton resignButton() {
        return resignButton;
    }

    public JButton newGameButton() {
        return newGameButton;
    }

    /** 차례마다 보드를 자동으로 뒤집는지 배지에 표시합니다. */
    public void showAutoFlip(boolean on) {
        autoFlipBadge.setText(on ? "차례마다 자동 뒤집기 ON" : "수동 보드 회전");
    }
}
