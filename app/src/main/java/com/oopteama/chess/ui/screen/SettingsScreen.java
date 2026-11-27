package com.oopteama.chess.ui.screen;

import com.oopteama.chess.data.Settings;
import com.oopteama.chess.data.Settings.BoardTheme;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.RoundedPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

/**
 * 설정 화면(View): 체스판 색 테마 3가지와 표시 옵션 체크박스.
 */
public final class SettingsScreen extends JPanel {

    private final JPanel themeGrid = new JPanel(new GridLayout(1, 3, 14, 0));
    private final JCheckBox coordsCheck = createCheck("체스판 좌표 표시 (a-h, 1-8)");
    private final JCheckBox legalMovesCheck = createCheck("선택한 기물의 이동 가능 칸 표시");
    private final JCheckBox moveInsightCheck =
            createCheck("수 설명: 갈 수 있는 칸에 마우스를 올리면 좋은 수·나쁜 수인 이유를 애니메이션으로 보여 주기");
    private final JLabel moveInsightNote =
            Theme.muted("퍼즐 · AI 대전 · 로컬 대전에서 적용 (레슨은 단계별 안내를 그대로 씁니다)");
    private final JCheckBox lastMoveCheck = createCheck("직전에 둔 수 강조 표시");
    private final JCheckBox autoFlipCheck = createCheck("로컬 2인 대전 시 차례마다 보드 자동 뒤집기");

    public SettingsScreen() {
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JLabel header = Theme.title("설정");
        header.setAlignmentX(LEFT_ALIGNMENT);
        JLabel subtitle = Theme.muted("변경 사항은 즉시 모든 화면에 적용되며 JSON으로 자동 저장됩니다.");
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        JPanel themeCard = Theme.card(20);
        themeCard.setAlignmentX(LEFT_ALIGNMENT);
        themeCard.setMaximumSize(new Dimension(760, 190));
        themeCard.add(Theme.sectionTitle("체스판 테마"), BorderLayout.NORTH);
        themeGrid.setOpaque(false);
        themeGrid.setBorder(new EmptyBorder(12, 0, 0, 0));
        themeCard.add(themeGrid, BorderLayout.CENTER);

        JPanel optionCard = Theme.card(20);
        optionCard.setAlignmentX(LEFT_ALIGNMENT);
        optionCard.setMaximumSize(new Dimension(760, 300));
        optionCard.add(Theme.sectionTitle("표시 및 대국 옵션"), BorderLayout.NORTH);
        JPanel checks = new JPanel();
        checks.setLayout(new BoxLayout(checks, BoxLayout.Y_AXIS));
        checks.setOpaque(false);
        checks.setBorder(new EmptyBorder(12, 0, 0, 0));
        for (JCheckBox check : new JCheckBox[]{coordsCheck, legalMovesCheck, moveInsightCheck, lastMoveCheck, autoFlipCheck}) {
            checks.add(check);
            if (check == moveInsightCheck) {
                moveInsightNote.setBorder(new EmptyBorder(2, 28, 0, 0)); // 체크박스 글자와 줄을 맞춤
                checks.add(moveInsightNote);
            }
            checks.add(Box.createVerticalStrut(10));
        }
        optionCard.add(checks, BorderLayout.CENTER);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.add(header);
        content.add(Box.createVerticalStrut(4));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(22));
        content.add(themeCard);
        content.add(Box.createVerticalStrut(16));
        content.add(optionCard);
        add(content, BorderLayout.NORTH);
    }

    private static JCheckBox createCheck(String text) {
        JCheckBox check = new JCheckBox(text);
        check.setOpaque(false);
        check.setForeground(Theme.TEXT);
        check.setFont(Theme.font(14));
        check.setFocusPainted(false);
        check.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return check;
    }

    // ---- 컨트롤러가 쓰는 메서드 ----

    public JCheckBox coordsCheck() {
        return coordsCheck;
    }

    public JCheckBox legalMovesCheck() {
        return legalMovesCheck;
    }

    public JCheckBox moveInsightCheck() {
        return moveInsightCheck;
    }

    public JCheckBox lastMoveCheck() {
        return lastMoveCheck;
    }

    public JCheckBox autoFlipCheck() {
        return autoFlipCheck;
    }

    /**
     * 현재 설정을 화면에 보여 줍니다.
     *
     * @param onPickTheme 테마 칸을 눌렀을 때 실행할 동작
     */
    public void showSettings(Settings settings, Consumer<BoardTheme> onPickTheme) {
        coordsCheck.setSelected(settings.showCoords());
        legalMovesCheck.setSelected(settings.showLegalMoves());
        moveInsightCheck.setSelected(settings.showMoveInsight());
        lastMoveCheck.setSelected(settings.highlightLastMove());
        autoFlipCheck.setSelected(settings.autoFlipLocal());

        themeGrid.removeAll();
        for (BoardTheme theme : BoardTheme.values()) {
            themeGrid.add(themeTile(theme, theme == settings.boardTheme(), onPickTheme));
        }
        themeGrid.revalidate();
        themeGrid.repaint();
    }

    /** 테마 하나를 보여 주는 칸: 2×2 체스판 견본과 이름. */
    private static JPanel themeTile(BoardTheme theme, boolean selected, Consumer<BoardTheme> onPick) {
        JPanel tile = new RoundedPanel(selected ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT, selected ? Theme.ACCENT : Theme.BORDER,
                12, selected ? Theme.ACCENT_SOFT : Theme.SURFACE_HOVER, Theme.ACCENT);
        tile.setLayout(new BorderLayout(0, 10));
        tile.setBorder(new EmptyBorder(12, 12, 12, 12));
        tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel swatch = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                int halfWidth = getWidth() / 2;
                int halfHeight = getHeight() / 2;
                Color light = new Color(theme.lightRgb());
                Color dark = new Color(theme.darkRgb());
                g.setColor(light);
                g.fillRect(0, 0, halfWidth, halfHeight);
                g.fillRect(halfWidth, halfHeight, getWidth() - halfWidth, getHeight() - halfHeight);
                g.setColor(dark);
                g.fillRect(halfWidth, 0, getWidth() - halfWidth, halfHeight);
                g.fillRect(0, halfHeight, halfWidth, getHeight() - halfHeight);
            }
        };
        swatch.setPreferredSize(new Dimension(120, 64));

        JLabel name = new JLabel((selected ? "✓ " : "") + theme.label(), JLabel.CENTER);
        name.setFont(Theme.bold(13));
        name.setForeground(selected ? Theme.SUCCESS : Theme.TEXT);

        tile.add(swatch, BorderLayout.CENTER);
        tile.add(name, BorderLayout.SOUTH);
        tile.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                onPick.accept(theme);
            }
        });
        return tile;
    }
}
