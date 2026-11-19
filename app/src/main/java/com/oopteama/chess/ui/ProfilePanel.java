package com.oopteama.chess.ui;

import com.formdev.flatlaf.FlatClientProperties;
import com.oopteama.chess.engine.ChessAI.Difficulty;
import com.oopteama.chess.model.AppStore;
import com.oopteama.chess.model.Lesson;
import com.oopteama.chess.model.Profile;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;

/**
 * 프로필 관리 및 학습 통계 화면: 프로필 생성·전환·삭제, 레슨·퍼즐·AI 전적 요약을 제공합니다.
 */
public final class ProfilePanel extends JPanel implements Screen {

    private final AppStore store;
    private final JPanel listContainer = new JPanel();
    private final JTextField nameField = new JTextField();

    private final JLabel activeNameLabel = Theme.title("");
    private final JLabel scoreBadge = Theme.badge("", Theme.ACCENT);
    private final JLabel tutorialStat = new JLabel();
    private final JLabel openingStat = new JLabel();
    private final JLabel puzzleStat = new JLabel();
    private final JPanel aiTable = new JPanel(new GridLayout(5, 4, 8, 8));

    public ProfilePanel(AppStore store) {
        this.store = store;
        setLayout(new BorderLayout(20, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        add(buildLeftProfiles(), BorderLayout.WEST);
        add(buildRightStats(), BorderLayout.CENTER);
        refresh();
    }

    private JPanel buildLeftProfiles() {
        JPanel left = Theme.card(16);
        left.setPreferredSize(new Dimension(280, 0));

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(Theme.sectionTitle("프로필 목록"), BorderLayout.NORTH);

        JPanel addRow = new JPanel(new BorderLayout(6, 0));
        addRow.setOpaque(false);
        nameField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "새 프로필 이름...");
        nameField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
        nameField.putClientProperty(FlatClientProperties.STYLE, "arc: 10; margin: 6,10,6,10");
        JButton addBtn = Theme.primaryButton("추가");
        addBtn.addActionListener(e -> createProfile());
        nameField.addActionListener(e -> createProfile());
        addRow.add(nameField, BorderLayout.CENTER);
        addRow.add(addBtn, BorderLayout.EAST);
        top.add(addRow, BorderLayout.SOUTH);

        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Theme.SURFACE);

        left.add(top, BorderLayout.NORTH);
        left.add(Theme.scrollPane(listContainer), BorderLayout.CENTER);
        return left;
    }

    private JPanel buildRightStats() {
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);

        // 헤더 카드
        JPanel header = Theme.card(20);
        header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        JPanel nameCol = new JPanel();
        nameCol.setLayout(new BoxLayout(nameCol, BoxLayout.Y_AXIS));
        nameCol.setOpaque(false);
        nameCol.add(activeNameLabel);
        nameCol.add(Box.createVerticalStrut(4));
        nameCol.add(Theme.muted("점수 기준: 레슨 완료 ×10 + 퍼즐 해결 ×5 + AI 승리 ×3"));

        JPanel rightBadge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        rightBadge.setOpaque(false);
        rightBadge.add(scoreBadge);
        header.add(nameCol, BorderLayout.CENTER);
        header.add(rightBadge, BorderLayout.EAST);

        // 학습 지표 카드
        JPanel metrics = new JPanel(new GridLayout(1, 3, 12, 0));
        metrics.setOpaque(false);
        metrics.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));
        metrics.add(metricBox("기본 튜토리얼", tutorialStat, Theme.ACCENT));
        metrics.add(metricBox("유명 오프닝", openingStat, Theme.SUCCESS));
        metrics.add(metricBox("전술 퍼즐", puzzleStat, Theme.INFO));

        // AI 전적표 카드
        JPanel aiCard = Theme.card(18);
        aiCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 240));
        aiCard.add(Theme.sectionTitle("난이도별 AI 대전 전적"), BorderLayout.NORTH);
        aiTable.setOpaque(false);
        aiTable.setBorder(new EmptyBorder(10, 0, 0, 0));
        aiCard.add(aiTable, BorderLayout.CENTER);

        // 하단 관리 바
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setOpaque(false);
        JButton clearBtn = Theme.dangerButton("현재 프로필 기록 초기화");
        clearBtn.addActionListener(e -> confirmClearProgress());
        footer.add(clearBtn);

        right.add(header);
        right.add(Box.createVerticalStrut(14));
        right.add(metrics);
        right.add(Box.createVerticalStrut(14));
        right.add(aiCard);
        right.add(Box.createVerticalStrut(14));
        right.add(footer);
        right.add(Box.createVerticalGlue());
        return right;
    }

    private JPanel metricBox(String label, JLabel value, Color accent) {
        JPanel card = Theme.card(14);
        value.setFont(Theme.bold(20));
        value.setForeground(accent);
        card.add(Theme.muted(label), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private void createProfile() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "프로필 이름을 입력해 주세요.", "안내", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        store.createProfile(name);
        nameField.setText("");
        refresh();
    }

    private void deleteProfile(Profile profile) {
        if (store.profiles().size() <= 1) {
            JOptionPane.showMessageDialog(this, "마지막 프로필은 삭제할 수 없습니다.", "안내", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "'" + profile.name() + "' 프로필과 학습 기록을 삭제할까요?",
                "프로필 삭제",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.OK_OPTION) {
            store.deleteProfile(profile.id());
            refresh();
        }
    }

    private void confirmClearProgress() {
        Profile active = store.activeProfile();
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "'" + active.name() + "'의 모든 학습·퍼즐·AI 전적을 초기화할까요?",
                "기록 초기화",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE
        );
        if (confirm == JOptionPane.OK_OPTION) {
            active.clearProgress();
            store.saveAndNotify();
            refresh();
        }
    }

    public void refresh() {
        Profile active = store.activeProfile();

        // 좌측 목록
        listContainer.removeAll();
        for (Profile p : store.profiles()) {
            boolean isActive = p.id().equals(active.id());
            JPanel row = new Theme.RoundedPanel(
                    isActive ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT,
                    isActive ? Theme.ACCENT : Theme.BORDER,
                    10
            );
            row.setLayout(new BorderLayout(6, 0));
            row.setBorder(new EmptyBorder(8, 12, 8, 8));
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

            JButton selectBtn = Theme.styledButton(
                    (isActive ? "● " : "") + p.name() + " (" + p.totalScore() + "점)",
                    isActive ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT,
                    Theme.BORDER,
                    isActive ? Theme.SUCCESS : Theme.TEXT
            );
            selectBtn.setHorizontalAlignment(JButton.LEFT);
            selectBtn.setBorder(new EmptyBorder(4, 4, 4, 4));
            selectBtn.addActionListener(e -> {
                store.setActiveProfile(p.id());
                refresh();
            });
            row.add(selectBtn, BorderLayout.CENTER);

            if (store.profiles().size() > 1) {
                JButton delBtn = Theme.dangerButton("삭제");
                delBtn.setBorder(new EmptyBorder(4, 10, 4, 10));
                delBtn.addActionListener(e -> deleteProfile(p));
                row.add(delBtn, BorderLayout.EAST);
            }
            listContainer.add(row);
            listContainer.add(Box.createVerticalStrut(6));
        }
        listContainer.revalidate();
        listContainer.repaint();

        // 우측 통계
        activeNameLabel.setText(active.name());
        scoreBadge.setText("총점 " + active.totalScore() + "점");

        var tutorials = store.lessons(Lesson.Category.TUTORIAL);
        long tutDone = tutorials.stream().filter(l -> active.isLessonCompleted(l.id())).count();
        tutorialStat.setText(tutDone + " / " + tutorials.size());

        var openings = store.lessons(Lesson.Category.OPENING);
        long openDone = openings.stream().filter(l -> active.isLessonCompleted(l.id())).count();
        openingStat.setText(openDone + " / " + openings.size());

        puzzleStat.setText(active.solvedPuzzleCount() + " / " + store.puzzles().size()
                + " (" + active.puzzleAccuracyPercent() + "%)");

        aiTable.removeAll();
        aiTable.add(Theme.muted("난이도"));
        aiTable.add(Theme.muted("승"));
        aiTable.add(Theme.muted("무"));
        aiTable.add(Theme.muted("패"));
        for (Difficulty d : Difficulty.values()) {
            int[] rec = active.aiRecord(d);
            JLabel diffLbl = new JLabel(d.label());
            diffLbl.setFont(Theme.bold(14));
            JLabel wLbl = new JLabel(String.valueOf(rec[0]));
            wLbl.setForeground(Theme.SUCCESS);
            JLabel dLbl = new JLabel(String.valueOf(rec[1]));
            dLbl.setForeground(Theme.INFO);
            JLabel lLbl = new JLabel(String.valueOf(rec[2]));
            lLbl.setForeground(Theme.DANGER);
            aiTable.add(diffLbl);
            aiTable.add(wLbl);
            aiTable.add(dLbl);
            aiTable.add(lLbl);
        }
        aiTable.revalidate();
        aiTable.repaint();
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
