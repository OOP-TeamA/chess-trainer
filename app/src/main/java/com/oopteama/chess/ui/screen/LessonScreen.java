package com.oopteama.chess.ui.screen;

import com.oopteama.chess.data.Settings;
import com.oopteama.chess.training.Lesson;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.BoardPanel;
import com.oopteama.chess.ui.component.CollapsibleList;
import com.oopteama.chess.ui.component.RoundedPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.text.BreakIterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 레슨 화면(View)입니다: 왼쪽 레슨 목록, 가운데 체스판, 오른쪽 안내 카드.
 * 화면을 그리기만 하고, 버튼을 눌렀을 때의 동작은 LessonController가 연결합니다.
 */
public final class LessonScreen extends JPanel {

    private final JButton tutorialTab = Theme.primaryButton(Lesson.Category.TUTORIAL.label());
    private final JButton openingTab = Theme.secondaryButton(Lesson.Category.OPENING.label());
    private final JPanel listContainer = new JPanel();
    private final CollapsibleList lessonList;
    private final BoardPanel boardPanel;

    private final JPanel guide = Theme.card(18);
    private final JLabel lessonTitle = Theme.sectionTitle("");
    private final JLabel stepCounter = Theme.muted("");
    private final JProgressBar stepBar = new JProgressBar();
    private final JTextArea promptArea = createTextArea();
    private final JTextArea feedbackArea = createTextArea();
    private final JButton hintButton = Theme.secondaryButton("힌트");
    private final JButton resetButton = Theme.secondaryButton("다시 놓기");
    private final JButton prevButton = Theme.secondaryButton("이전");
    private final JButton nextButton = Theme.primaryButton("다음 단계");
    private int guideWidth;

    public LessonScreen(Settings settings) {
        boardPanel = new BoardPanel(settings);
        setLayout(new BorderLayout(18, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 22, 20, 22));

        lessonList = new CollapsibleList(buildListCard(), "레슨 목록");
        add(lessonList, BorderLayout.WEST);
        add(boardPanel, BorderLayout.CENTER);
        add(buildGuideCard(), BorderLayout.EAST);
    }

    private JPanel buildListCard() {
        JPanel card = Theme.card(14);
        card.setPreferredSize(new Dimension(250, 0));
        JPanel tabs = new JPanel(new GridLayout(1, 2, 6, 0));
        tabs.setOpaque(false);
        tabs.add(tutorialTab);
        tabs.add(openingTab);
        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Theme.SURFACE);
        card.add(tabs, BorderLayout.NORTH);
        card.add(Theme.scrollPane(listContainer), BorderLayout.CENTER);
        return card;
    }

    private JPanel buildGuideCard() {
        guide.setPreferredSize(new Dimension(300, 0));

        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        lessonTitle.setAlignmentX(LEFT_ALIGNMENT);
        stepCounter.setAlignmentX(LEFT_ALIGNMENT);
        stepBar.setPreferredSize(new Dimension(260, 8));
        stepBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 8));
        stepBar.setAlignmentX(LEFT_ALIGNMENT);
        top.add(lessonTitle);
        top.add(Box.createVerticalStrut(6));
        top.add(stepCounter);
        top.add(Box.createVerticalStrut(8));
        top.add(stepBar);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(14, 0, 14, 0));
        center.add(Theme.scrollPane(promptArea), BorderLayout.CENTER);
        feedbackArea.setFont(Theme.font(15));
        feedbackArea.setLineWrap(true);
        feedbackArea.setWrapStyleWord(true);
        feedbackArea.setBorder(new EmptyBorder(10, 12, 10, 12));
        feedbackArea.setOpaque(false);
        JPanel feedbackBox = new RoundedPanel(Theme.SURFACE_ALT, Theme.BORDER, 10);
        feedbackBox.setLayout(new BorderLayout());
        feedbackBox.add(feedbackArea, BorderLayout.CENTER);
        center.add(feedbackBox, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);
        buttons.add(hintButton);
        buttons.add(resetButton);
        buttons.add(prevButton);
        buttons.add(nextButton);

        guide.add(top, BorderLayout.NORTH);
        guide.add(center, BorderLayout.CENTER);
        guide.add(buttons, BorderLayout.SOUTH);
        return guide;
    }

    // ---- 컨트롤러가 이벤트를 연결할 부품 ----

    public BoardPanel boardPanel() {
        return boardPanel;
    }

    public JButton tutorialTab() {
        return tutorialTab;
    }

    public JButton openingTab() {
        return openingTab;
    }

    public JButton hintButton() {
        return hintButton;
    }

    public JButton resetButton() {
        return resetButton;
    }

    public JButton prevButton() {
        return prevButton;
    }

    public JButton nextButton() {
        return nextButton;
    }

    // ---- 컨트롤러가 화면 내용을 바꿀 때 쓰는 메서드 ----

    /** 선택한 탭을 강조합니다. */
    public void showCategory(Lesson.Category active) {
        styleTab(tutorialTab, active == Lesson.Category.TUTORIAL);
        styleTab(openingTab, active == Lesson.Category.OPENING);
    }

    private static void styleTab(JButton tab, boolean active) {
        if (active) {
            Theme.applyButtonStyle(tab, Theme.ACCENT, Theme.ACCENT_HOVER, Theme.BG, null);
        } else {
            Theme.applyButtonStyle(tab, Theme.SURFACE_ALT, Theme.SURFACE_HOVER, Theme.MUTED, null);
        }
    }

    /**
     * 레슨 목록을 다시 그립니다.
     *
     * @param currentId 지금 열린 레슨 id (강조 표시)
     * @param completed 레슨 id를 받아 완료했는지 알려 주는 함수
     * @param onSelect  목록에서 레슨을 눌렀을 때 실행할 동작
     */
    public void showLessonList(List<Lesson> lessons, String currentId,
                               Predicate<String> completed, Consumer<Lesson> onSelect) {
        listContainer.removeAll();
        for (Lesson lesson : lessons) {
            boolean done = completed.test(lesson.id());
            boolean selected = lesson.id().equals(currentId);
            Color background = selected ? Theme.ACCENT_SOFT : Theme.SURFACE;
            Color hover = selected ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT;
            Color text = selected ? Theme.SUCCESS : (done ? Theme.TEXT : Theme.MUTED);
            JButton item = Theme.styledButton((done ? "✓ " : "○ ") + lesson.title(), background, hover, text);
            if (selected) {
                Theme.applyButtonStyle(item, background, hover, text, Theme.ACCENT);
            }
            item.setHorizontalAlignment(JButton.LEFT);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            item.addActionListener(e -> onSelect.accept(lesson));
            listContainer.add(item);
            listContainer.add(Box.createVerticalStrut(4));
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    /** 레슨 제목, 단계 진행도, 안내 문구를 보여 줍니다. */
    public void showStep(String title, int stepIndex, int stepCount, boolean solved, String prompt) {
        lessonTitle.setText(title);
        resizeTitle();
        stepCounter.setText("단계 " + (stepIndex + 1) + " / " + stepCount);
        stepBar.setMaximum(stepCount);
        stepBar.setValue(stepIndex + (solved ? 1 : 0));
        promptArea.setText(breakLines(prompt, promptArea.getFontMetrics(Theme.font(18))));
        promptArea.setCaretPosition(0);
    }

    /** 피드백 문구 (비어 있으면 기본 안내). positive면 초록, 아니면 노랑으로 보여 줍니다. */
    public void showFeedback(String text, boolean positive) {
        if (text.isEmpty()) {
            feedbackArea.setText("보드에서 직접 기물을 움직여 보세요.");
            feedbackArea.setForeground(Theme.MUTED);
        } else {
            feedbackArea.setText(text);
            feedbackArea.setForeground(positive ? Theme.SUCCESS : Theme.WARNING);
        }
    }

    /** [이전]·[다음] 버튼 상태. */
    public void showNavigation(boolean prevEnabled, boolean nextEnabled, String nextText) {
        prevButton.setEnabled(prevEnabled);
        nextButton.setEnabled(nextEnabled);
        nextButton.setText(nextText);
    }

    /** 접혀 있던 레슨 목록을 펼칩니다. */
    public void openLessonList() {
        lessonList.openAndFocus();
    }

    // ---- 안내 카드 크기·글꼴 ----

    /** 창 너비에 맞춰 안내 카드 너비와 글자 크기를 함께 조절합니다. */
    @Override
    public void doLayout() {
        int width = Math.max(282, Math.min(430, Math.round(getWidth() * 0.315f)));
        if (guideWidth != width) {
            guideWidth = width;
            guide.setPreferredSize(new Dimension(width, 0));
            float scale = (width - 60) / 260f;
            promptArea.setFont(Theme.font(18 * scale));
            stepCounter.setFont(Theme.font(15 * scale));
            resizeTitle();
        }
        super.doLayout();
    }

    /** 레슨 제목이 카드보다 길면 글자를 줄입니다. */
    private void resizeTitle() {
        if (guideWidth == 0) {
            return;
        }
        float size = 22 * (guideWidth - 60) / 260f;
        var font = Theme.bold(size);
        int available = guideWidth - 36;
        int textWidth = lessonTitle.getFontMetrics(font).stringWidth(lessonTitle.getText());
        if (textWidth > available) {
            font = font.deriveFont(size * available / textWidth);
        }
        lessonTitle.setFont(font);
    }

    /**
     * 안내 문구를 기준 글꼴로 245px마다 줄바꿈합니다.
     * 글자 크기가 카드 너비에 비례해 바뀌므로, 어떤 창 크기에서도 같은 위치에서 줄이 바뀝니다.
     */
    private static String breakLines(String text, FontMetrics metrics) {
        int width = 245;
        StringBuilder result = new StringBuilder();
        String[] paragraphs = text.split("\\R", -1);
        for (int i = 0; i < paragraphs.length; i++) {
            if (i > 0) {
                result.append('\n');
            }
            String paragraph = paragraphs[i];
            BreakIterator breaks = BreakIterator.getLineInstance(Locale.KOREAN);
            breaks.setText(paragraph);
            int lineStart = 0;
            int lastBreak = 0;
            for (int end = breaks.next(); end != BreakIterator.DONE; end = breaks.next()) {
                boolean tooWide = metrics.stringWidth(paragraph.substring(lineStart, end).stripTrailing()) > width;
                if (lastBreak > lineStart && tooWide) {
                    result.append(paragraph.substring(lineStart, lastBreak).strip()).append('\n');
                    lineStart = lastBreak;
                }
                lastBreak = end;
            }
            result.append(paragraph.substring(lineStart).strip());
        }
        return result.toString();
    }

    private static JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(Theme.font(18));
        area.setForeground(Theme.TEXT);
        area.setBackground(Theme.SURFACE);
        area.setBorder(new EmptyBorder(4, 2, 4, 2));
        return area;
    }
}
