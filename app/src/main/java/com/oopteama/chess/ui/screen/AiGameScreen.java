package com.oopteama.chess.ui.screen;

import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.data.Settings;
import com.oopteama.chess.ui.Theme;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import java.awt.FlowLayout;

/**
 * AI 대전 화면(View): 공통 대국 화면에 난이도·진영 선택과 힌트·무르기·뒤집기·기권 버튼을 더합니다.
 */
public final class AiGameScreen extends GameScreen {

    private final JComboBox<Difficulty> difficultyBox = new JComboBox<>(Difficulty.values());
    private final JComboBox<String> sideBox = new JComboBox<>(new String[]{"백으로 플레이", "흑으로 플레이", "무작위 진영"});
    private final JButton newGameButton = Theme.primaryButton("새 대국 시작");
    private final JButton hintButton = Theme.secondaryButton("힌트");
    private final JButton undoButton = Theme.secondaryButton("무르기");
    private final JButton flipButton = Theme.secondaryButton("보드 뒤집기");
    private final JButton resignButton = Theme.dangerButton("기권");

    public AiGameScreen(Settings settings) {
        super(settings);
        difficultyBox.setSelectedItem(Difficulty.EASY);
        JPanel bar = Theme.card(10);
        bar.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 2));
        bar.add(Theme.muted("난이도"));
        bar.add(difficultyBox);
        bar.add(Theme.muted("내 진영"));
        bar.add(sideBox);
        bar.add(newGameButton);
        setTopBar(bar);
        addControls(hintButton, undoButton, flipButton, resignButton);
    }

    public JComboBox<Difficulty> difficultyBox() {
        return difficultyBox;
    }

    /** 0 = 백, 1 = 흑, 2 = 무작위 */
    public JComboBox<String> sideBox() {
        return sideBox;
    }

    public JButton newGameButton() {
        return newGameButton;
    }

    public JButton hintButton() {
        return hintButton;
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
}
