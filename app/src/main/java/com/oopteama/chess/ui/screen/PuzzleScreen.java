package com.oopteama.chess.ui.screen;

import com.oopteama.chess.data.Settings;
import com.oopteama.chess.training.Puzzle;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.BoardPanel;
import com.oopteama.chess.ui.component.CollapsibleList;
import com.oopteama.chess.ui.component.RoundedPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 퍼즐 화면(View)입니다: 왼쪽 퍼즐 목록(종류 필터), 가운데 체스판, 오른쪽 문제 설명 카드.
 * 화면을 그리기만 하고, 동작은 PuzzleController가 연결합니다.
 */
public final class PuzzleScreen extends JPanel {

    private final JComboBox<String> filterBox = new JComboBox<>();
    private final JPanel listContainer = new JPanel();
    private final BoardPanel boardPanel;

    private final JLabel themeBadge = Theme.badge("", Theme.INFO);
    private final JLabel titleLabel = Theme.sectionTitle("");
    private final JLabel turnLabel = Theme.muted("");
    private final JTextArea explainArea = new JTextArea();
    private final JLabel feedbackLabel = new JLabel();
    private final JButton hintButton = Theme.secondaryButton("힌트");
    private final JButton undoButton = Theme.secondaryButton("무르기");
    private final JButton resetButton = Theme.secondaryButton("다시 풀기");
    private final JButton nextButton = Theme.primaryButton("다음 퍼즐 →");

    /** @param filters 목록 위 필터 상자에 넣을 항목 (예: 전체 퍼즐, 체크메이트, 전술 문제) */
    public PuzzleScreen(Settings settings, List<String> filters) {
        boardPanel = new BoardPanel(settings);
        filters.forEach(filterBox::addItem);
        setLayout(new BorderLayout(18, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 22, 20, 22));
        add(new CollapsibleList(buildListCard(), "퍼즐 목록"), BorderLayout.WEST);
        add(boardPanel, BorderLayout.CENTER);
        add(buildInfoCard(), BorderLayout.EAST);
    }

    private JPanel buildListCard() {
        JPanel card = Theme.card(14);
        card.setPreferredSize(new Dimension(250, 0));
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Theme.SURFACE);
        card.add(filterBox, BorderLayout.NORTH);
        card.add(Theme.scrollPane(listContainer), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildInfoCard() {
        JPanel card = Theme.card(18);
        card.setPreferredSize(new Dimension(320, 0));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        themeBadge.setAlignmentX(LEFT_ALIGNMENT);
        titleLabel.setAlignmentX(LEFT_ALIGNMENT);
        turnLabel.setAlignmentX(LEFT_ALIGNMENT);
        top.add(themeBadge);
        top.add(Box.createVerticalStrut(10));
        top.add(titleLabel);
        top.add(Box.createVerticalStrut(4));
        top.add(turnLabel);

        explainArea.setEditable(false);
        explainArea.setLineWrap(true);
        explainArea.setWrapStyleWord(true);
        explainArea.setFont(Theme.font(20));
        explainArea.setForeground(Theme.TEXT);
        explainArea.setBackground(Theme.SURFACE);
        explainArea.setBorder(new EmptyBorder(15, 16, 15, 16));

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(14, 0, 14, 0));
        center.add(Theme.scrollPane(explainArea), BorderLayout.CENTER);
        feedbackLabel.setFont(Theme.bold(15));
        feedbackLabel.setOpaque(false);
        feedbackLabel.setBorder(new EmptyBorder(10, 12, 10, 12));
        JPanel feedbackBox = new RoundedPanel(Theme.SURFACE_ALT, Theme.BORDER, 10);
        feedbackBox.setLayout(new BorderLayout());
        feedbackBox.add(feedbackLabel, BorderLayout.CENTER);
        center.add(feedbackBox, BorderLayout.SOUTH);

        hintButton.setToolTipText("힌트 · 다음 수를 둘 칸을 확인해요.");
        resetButton.setToolTipText("다시 풀기 · 이 퍼즐의 시작 국면으로 돌아가요.");
        nextButton.setToolTipText("다음 퍼즐 · 목록의 다음 문제를 열어요.");
        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);
        buttons.add(hintButton);
        buttons.add(undoButton);
        buttons.add(resetButton);
        buttons.add(nextButton);

        card.add(top, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(buttons, BorderLayout.SOUTH);
        return card;
    }

    // ---- 컨트롤러가 이벤트를 연결할 부품 ----

    public BoardPanel boardPanel() {
        return boardPanel;
    }

    public JComboBox<String> filterBox() {
        return filterBox;
    }

    public JButton hintButton() {
        return hintButton;
    }

    public JButton undoButton() {
        return undoButton;
    }

    public JButton resetButton() {
        return resetButton;
    }

    public JButton nextButton() {
        return nextButton;
    }

    // ---- 컨트롤러가 화면 내용을 바꿀 때 쓰는 메서드 ----

    /**
     * 퍼즐 목록을 다시 그립니다.
     *
     * @param solved   퍼즐 id를 받아 풀었는지 알려 주는 함수
     * @param titleOf  목록에 보여 줄 제목을 만드는 함수
     * @param onSelect 목록에서 퍼즐을 눌렀을 때 실행할 동작
     */
    public void showPuzzleList(List<Puzzle> puzzles, String currentId, Predicate<String> solved,
                               Function<Puzzle, String> titleOf, Consumer<Puzzle> onSelect) {
        listContainer.removeAll();
        for (Puzzle puzzle : puzzles) {
            boolean done = solved.test(puzzle.id());
            boolean selected = puzzle.id().equals(currentId);
            Color background = selected ? Theme.ACCENT_SOFT : Theme.SURFACE;
            Color hover = selected ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT;
            Color text = selected ? Theme.SUCCESS : (done ? Theme.TEXT : Theme.MUTED);
            JButton item = Theme.styledButton((done ? "✓ " : "○ ") + titleOf.apply(puzzle), background, hover, text);
            if (selected) {
                Theme.applyButtonStyle(item, background, hover, text, Theme.ACCENT);
            }
            item.setHorizontalAlignment(JButton.LEFT);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            item.addActionListener(e -> onSelect.accept(puzzle));
            listContainer.add(item);
            listContainer.add(Box.createVerticalStrut(4));
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    /** 위쪽의 종류 배지, 제목, 차례·남은 수. */
    public void showHeader(String badge, String title, String turnText) {
        themeBadge.setText(badge);
        titleLabel.setText(title);
        turnLabel.setText(turnText);
    }

    /** 문제 설명이나 해설. tooltip은 마우스를 올렸을 때 보여 줄 글 (없으면 null). */
    public void showExplanation(String text, String tooltip) {
        explainArea.setText(text);
        explainArea.setCaretPosition(0);
        explainArea.setToolTipText(tooltip == null ? null : "<html><div style='width: 240px'>"
                + escapeHtml(tooltip).replace("\n", "<br>") + "</div></html>");
    }

    /** 피드백 문구 (비어 있으면 기본 안내). positive면 초록, 아니면 노랑으로 보여 줍니다. */
    public void showFeedback(String text, boolean positive) {
        if (text.isEmpty()) {
            feedbackLabel.setText("<html>문제를 풀면 해설이 공개됩니다.</html>");
            feedbackLabel.setForeground(Theme.MUTED);
        } else {
            feedbackLabel.setText("<html>" + escapeHtml(text) + "</html>");
            feedbackLabel.setForeground(positive ? Theme.SUCCESS : Theme.WARNING);
        }
    }

    /** [무르기] 버튼 상태와 설명. */
    public void showUndo(boolean enabled, String tooltip) {
        undoButton.setEnabled(enabled);
        undoButton.setToolTipText(tooltip);
    }

    /** 문장 속 &, <, > 기호가 HTML 태그로 잘못 해석되지 않게 바꿉니다. */
    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
