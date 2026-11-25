package com.oopteama.chess.ui.screen;

import com.formdev.flatlaf.FlatClientProperties;
import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.data.Profile;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.RoundedPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;

/**
 * 프로필 화면(View): 왼쪽 프로필 목록·추가, 오른쪽 선택한 프로필의 점수와 학습·AI 대전 기록.
 */
public final class ProfileScreen extends JPanel {

    private final JPanel listContainer = new JPanel();
    private final JTextField nameField = new JTextField();
    private final JButton addButton = Theme.primaryButton("추가");
    private final JLabel activeNameLabel = Theme.title("");
    private final JLabel scoreBadge = Theme.badge("", Theme.ACCENT);
    private final JLabel tutorialStat = new JLabel();
    private final JLabel openingStat = new JLabel();
    private final JLabel puzzleStat = new JLabel();
    private final JPanel aiTable = new JPanel(new GridLayout(5, 4, 8, 8));
    private final JButton clearButton = Theme.dangerButton("현재 프로필 기록 초기화");

    public ProfileScreen() {
        setLayout(new BorderLayout(20, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        add(buildProfileList(), BorderLayout.WEST);
        add(buildRecords(), BorderLayout.CENTER);
    }

    private JPanel buildProfileList() {
        JPanel card = Theme.card(16);
        card.setPreferredSize(new Dimension(280, 0));
        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(Theme.sectionTitle("프로필 목록"), BorderLayout.NORTH);
        nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "새 프로필 이름...");
        nameField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        nameField.putClientProperty(FlatClientProperties.STYLE, "arc: 10; margin: 6,10,6,10");
        JPanel addRow = new JPanel(new BorderLayout(6, 0));
        addRow.setOpaque(false);
        addRow.add(nameField, BorderLayout.CENTER);
        addRow.add(addButton, BorderLayout.EAST);
        top.add(addRow, BorderLayout.SOUTH);
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Theme.SURFACE);
        card.add(top, BorderLayout.NORTH);
        card.add(Theme.scrollPane(listContainer), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecords() {
        JPanel column = new JPanel();
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));
        column.setOpaque(false);

        JPanel header = Theme.card(20);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        JPanel nameColumn = new JPanel();
        nameColumn.setLayout(new BoxLayout(nameColumn, BoxLayout.Y_AXIS));
        nameColumn.setOpaque(false);
        nameColumn.add(activeNameLabel);
        nameColumn.add(Box.createVerticalStrut(4));
        nameColumn.add(Theme.muted("점수 기준: 레슨 완료 ×10 + 퍼즐 해결 ×5 + AI 승리 ×3"));
        JPanel badgeWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        badgeWrap.setOpaque(false);
        badgeWrap.add(scoreBadge);
        header.add(nameColumn, BorderLayout.CENTER);
        header.add(badgeWrap, BorderLayout.EAST);

        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        metrics.add(metricCard("기본 튜토리얼", tutorialStat, Theme.ACCENT));
        metrics.add(metricCard("유명 오프닝", openingStat, Theme.SUCCESS));
        metrics.add(metricCard("전술 · 메이트 퍼즐", puzzleStat, Theme.INFO));

        JPanel aiCard = Theme.card(18);
        aiCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        aiCard.add(Theme.sectionTitle("난이도별 AI 대전 전적"), BorderLayout.NORTH);
        aiTable.setOpaque(false);
        aiTable.setBorder(new EmptyBorder(10, 0, 0, 0));
        aiCard.add(aiTable, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        footer.add(clearButton);

        column.add(header);
        column.add(Box.createVerticalStrut(14));
        column.add(metrics);
        column.add(Box.createVerticalStrut(14));
        column.add(aiCard);
        column.add(Box.createVerticalStrut(14));
        column.add(footer);
        column.add(Box.createVerticalGlue());
        return column;
    }

    private static JPanel metricCard(String caption, JLabel value, Color accent) {
        JPanel card = Theme.card(14);
        value.setFont(Theme.bold(20));
        value.setForeground(accent);
        card.add(Theme.muted(caption), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    // ---- 컨트롤러가 쓰는 메서드 ----

    public JTextField nameField() {
        return nameField;
    }

    public JButton addButton() {
        return addButton;
    }

    public JButton clearButton() {
        return clearButton;
    }

    /**
     * 프로필 목록을 다시 그립니다. 프로필이 하나뿐이면 [삭제] 버튼을 숨깁니다.
     *
     * @param onSelect 프로필을 눌렀을 때 실행할 동작
     * @param onDelete [삭제]를 눌렀을 때 실행할 동작
     */
    public void showProfiles(List<Profile> profiles, String activeId,
                             Consumer<Profile> onSelect, Consumer<Profile> onDelete) {
        listContainer.removeAll();
        for (Profile profile : profiles) {
            boolean active = profile.id().equals(activeId);
            JPanel row = new RoundedPanel(active ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT, active ? Theme.ACCENT : Theme.BORDER, 10);
            row.setLayout(new BorderLayout(6, 0));
            row.setBorder(new EmptyBorder(8, 12, 8, 8));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

            JButton selectButton = Theme.styledButton(
                    (active ? "● " : "") + profile.name() + " (" + profile.totalScore() + "점)",
                    active ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT, Theme.BORDER, active ? Theme.SUCCESS : Theme.TEXT);
            selectButton.setHorizontalAlignment(JButton.LEFT);
            selectButton.setBorder(new EmptyBorder(4, 4, 4, 4));
            selectButton.addActionListener(e -> onSelect.accept(profile));
            row.add(selectButton, BorderLayout.CENTER);

            if (profiles.size() > 1) {
                JButton deleteButton = Theme.dangerButton("삭제");
                deleteButton.setBorder(new EmptyBorder(4, 10, 4, 10));
                deleteButton.addActionListener(e -> onDelete.accept(profile));
                row.add(deleteButton, BorderLayout.EAST);
            }
            listContainer.add(row);
            listContainer.add(Box.createVerticalStrut(6));
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    /** 선택한 프로필의 이름·총점과 학습 현황. */
    public void showActiveProfile(String name, int totalScore, String tutorials, String openings, String puzzles) {
        activeNameLabel.setText(name);
        scoreBadge.setText("총점 " + totalScore + "점");
        tutorialStat.setText(tutorials);
        openingStat.setText(openings);
        puzzleStat.setText(puzzles);
    }

    /** 난이도별 AI 대전 승·무·패 표. */
    public void showAiRecords(Profile profile) {
        aiTable.removeAll();
        for (String heading : new String[]{"난이도", "승", "무", "패"}) {
            aiTable.add(Theme.muted(heading));
        }
        for (Difficulty difficulty : Difficulty.values()) {
            int[] record = profile.aiRecord(difficulty);
            JLabel name = new JLabel(difficulty.label());
            name.setFont(Theme.bold(14));
            aiTable.add(name);
            aiTable.add(coloredNumber(record[0], Theme.SUCCESS));
            aiTable.add(coloredNumber(record[1], Theme.INFO));
            aiTable.add(coloredNumber(record[2], Theme.DANGER));
        }
        aiTable.revalidate();
        aiTable.repaint();
    }

    private static JLabel coloredNumber(int value, Color color) {
        JLabel label = new JLabel(String.valueOf(value));
        label.setForeground(color);
        return label;
    }
}
