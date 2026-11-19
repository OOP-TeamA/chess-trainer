package com.oopteama.chess.ui;

import com.oopteama.chess.model.AppStore;
import com.oopteama.chess.model.Settings;
import com.oopteama.chess.model.Settings.BoardTheme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
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

/**
 * 설정 화면: 보드 색상 테마 미리보기 선택과 표시·대국 옵션을 즉시 반영하고 저장합니다.
 */
public final class SettingsPanel extends JPanel implements Screen {

    private final AppStore store;
    private final JPanel themeGrid = new JPanel(new GridLayout(1, 3, 14, 0));

    private final JCheckBox coordsCheck = createCheck("체스판 좌표 표시 (a-h, 1-8)");
    private final JCheckBox legalCheck = createCheck("선택한 기물의 이동 가능 칸 표시");
    private final JCheckBox lastMoveCheck = createCheck("직전에 둔 수 강조 표시");
    private final JCheckBox previewCheck = createCheck("이동 칸 호버 시 수 평가 및 이후 전개 미리보기 (응수·위험·공격·수비)");
    private final JCheckBox autoFlipCheck = createCheck("로컬 2인 대전 시 차례마다 보드 자동 뒤집기");

    public SettingsPanel(AppStore store) {
        this.store = store;
        setLayout(new BorderLayout());
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel header = Theme.title("설정");
        header.setAlignmentX(LEFT_ALIGNMENT);
        JLabel sub = Theme.muted("변경 사항은 즉시 모든 화면에 적용되며 자동 저장됩니다.");
        sub.setAlignmentX(LEFT_ALIGNMENT);

        JPanel themeCard = Theme.card(20);
        themeCard.setAlignmentX(LEFT_ALIGNMENT);
        themeCard.setMaximumSize(new Dimension(760, 190));
        themeCard.add(Theme.sectionTitle("체스판 테마"), BorderLayout.NORTH);
        themeGrid.setOpaque(false);
        themeGrid.setBorder(new EmptyBorder(12, 0, 0, 0));
        themeCard.add(themeGrid, BorderLayout.CENTER);

        JPanel optCard = Theme.card(20);
        optCard.setAlignmentX(LEFT_ALIGNMENT);
        optCard.setMaximumSize(new Dimension(760, 270));
        optCard.add(Theme.sectionTitle("표시 및 대국 옵션"), BorderLayout.NORTH);

        JPanel checks = new JPanel();
        checks.setLayout(new BoxLayout(checks, BoxLayout.Y_AXIS));
        checks.setOpaque(false);
        checks.setBorder(new EmptyBorder(12, 0, 0, 0));
        checks.add(coordsCheck);
        checks.add(Box.createVerticalStrut(10));
        checks.add(legalCheck);
        checks.add(Box.createVerticalStrut(10));
        checks.add(lastMoveCheck);
        checks.add(Box.createVerticalStrut(10));
        checks.add(previewCheck);
        checks.add(Box.createVerticalStrut(10));
        checks.add(autoFlipCheck);
        optCard.add(checks, BorderLayout.CENTER);

        coordsCheck.addActionListener(e -> {
            store.settings().setShowCoords(coordsCheck.isSelected());
            store.saveAndNotify();
        });
        legalCheck.addActionListener(e -> {
            store.settings().setShowLegalMoves(legalCheck.isSelected());
            store.saveAndNotify();
        });
        lastMoveCheck.addActionListener(e -> {
            store.settings().setHighlightLastMove(lastMoveCheck.isSelected());
            store.saveAndNotify();
        });
        previewCheck.addActionListener(e -> {
            store.settings().setShowMovePreview(previewCheck.isSelected());
            store.saveAndNotify();
        });
        autoFlipCheck.addActionListener(e -> {
            store.settings().setAutoFlipLocal(autoFlipCheck.isSelected());
            store.saveAndNotify();
        });

        content.add(header);
        content.add(Box.createVerticalStrut(4));
        content.add(sub);
        content.add(Box.createVerticalStrut(22));
        content.add(themeCard);
        content.add(Box.createVerticalStrut(16));
        content.add(optCard);

        add(content, BorderLayout.NORTH);
        refresh();
    }

    private static JCheckBox createCheck(String text) {
        JCheckBox cb = new JCheckBox(text);
        cb.setOpaque(false);
        cb.setForeground(Theme.TEXT);
        cb.setFont(Theme.font(14));
        cb.setFocusPainted(false);
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return cb;
    }

    private void refresh() {
        Settings s = store.settings();
        coordsCheck.setSelected(s.showCoords());
        legalCheck.setSelected(s.showLegalMoves());
        lastMoveCheck.setSelected(s.highlightLastMove());
        previewCheck.setSelected(s.showMovePreview());
        autoFlipCheck.setSelected(s.autoFlipLocal());

        themeGrid.removeAll();
        for (BoardTheme bt : BoardTheme.values()) {
            boolean selected = bt == s.boardTheme();
            JPanel tile = new Theme.RoundedPanel(
                    selected ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT,
                    selected ? Theme.ACCENT : Theme.BORDER,
                    12,
                    selected ? Theme.ACCENT_SOFT : Theme.SURFACE_HOVER,
                    Theme.ACCENT
            );
            tile.setLayout(new BorderLayout(0, 10));
            tile.setBorder(new EmptyBorder(12, 12, 12, 12));
            tile.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JPanel swatch = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    int w = getWidth() / 2;
                    int h = getHeight() / 2;
                    Color l = new Color(bt.lightRgb());
                    Color d = new Color(bt.darkRgb());
                    g.setColor(l); g.fillRect(0, 0, w, h);
                    g.setColor(d); g.fillRect(w, 0, getWidth() - w, h);
                    g.setColor(d); g.fillRect(0, h, w, getHeight() - h);
                    g.setColor(l); g.fillRect(w, h, getWidth() - w, getHeight() - h);
                }
            };
            swatch.setPreferredSize(new Dimension(120, 64));

            JLabel name = new JLabel((selected ? "✓ " : "") + bt.label(), JLabel.CENTER);
            name.setFont(Theme.bold(13));
            name.setForeground(selected ? Theme.SUCCESS : Theme.TEXT);

            tile.add(swatch, BorderLayout.CENTER);
            tile.add(name, BorderLayout.SOUTH);
            tile.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    store.settings().setBoardTheme(bt);
                    store.saveAndNotify();
                    refresh();
                }
            });
            themeGrid.add(tile);
        }
        themeGrid.revalidate();
        themeGrid.repaint();
    }

    @Override
    public JComponent view() {
        return this;
    }

    @Override
    public void onShow() {
        refresh();
    }
}
