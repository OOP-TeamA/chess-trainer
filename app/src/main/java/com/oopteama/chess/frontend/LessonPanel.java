package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.service.ChessBackend;
import com.oopteama.chess.backend.training.Lesson;
import com.oopteama.chess.backend.training.Lesson.Category;
import com.oopteama.chess.backend.training.LessonSession;
import com.oopteama.chess.backend.training.TrainingSession;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.List;
import java.util.Locale;
import java.text.BreakIterator;

/**
 * 레슨 화면: [기본 튜토리얼 | 유명 오프닝] 탭 전환, 보드 실습, 단계별 안내와 피드백을 제공합니다.
 */
public final class LessonPanel extends JPanel implements Screen {

    private final ChessBackend backend;
    private final BoardView boardView;
    private final JPanel listContainer = new JPanel();
    private final JButton tutorialTab = Theme.primaryButton("기본 튜토리얼");
    private final JButton openingTab = Theme.secondaryButton("유명 오프닝");

    private final JLabel lessonTitle = Theme.sectionTitle("");
    private final JLabel stepCounter = Theme.muted("");
    private final JProgressBar stepBar = new JProgressBar();
    private final JTextArea promptArea = createTextArea();
    private final JTextArea feedbackLabel = createTextArea();
    private final JPanel guide = Theme.card(18);
    private int guideWidth;
    private final JButton hintBtn = Theme.secondaryButton("힌트");
    private final JButton resetBtn = Theme.secondaryButton("다시 놓기");
    private final JButton prevBtn = Theme.secondaryButton("이전");
    private final JButton nextBtn = Theme.primaryButton("다음 단계");

    private final CollapsibleList lessonList;
    private boolean lessonFinished;
    private Category activeCategory = Category.TUTORIAL;
    private LessonSession session;
    private Timer replyTimer;

    public LessonPanel(ChessBackend backend) {
        this.backend = backend;
        this.boardView = new BoardView(backend.settings(), backend::evaluateMove);
        this.boardView.setMoveHandler(this::onUserMove);
        this.boardView.setIllegalMoveHandler(this::onIllegalMove);

        setLayout(new BorderLayout(18, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 22, 20, 22));

        lessonList = new CollapsibleList(buildLeftList(), "레슨 목록");
        add(lessonList, BorderLayout.WEST);
        add(boardView, BorderLayout.CENTER);
        add(buildRightGuide(), BorderLayout.EAST);

        switchCategory(Category.TUTORIAL);
    }

    private JPanel buildLeftList() {
        JPanel side = Theme.card(14);
        side.setPreferredSize(new Dimension(250, 0));

        JPanel tabs = new JPanel(new GridLayout(1, 2, 6, 0));
        tabs.setOpaque(false);
        tutorialTab.addActionListener(e -> switchCategory(Category.TUTORIAL));
        openingTab.addActionListener(e -> switchCategory(Category.OPENING));
        tabs.add(tutorialTab);
        tabs.add(openingTab);

        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Theme.SURFACE);

        side.add(tabs, BorderLayout.NORTH);
        side.add(Theme.scrollPane(listContainer), BorderLayout.CENTER);
        return side;
    }

    private JPanel buildRightGuide() {
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

        feedbackLabel.setFont(Theme.bold(13));
        feedbackLabel.setBorder(new EmptyBorder(10, 12, 10, 12));
        feedbackLabel.setOpaque(false);
        JPanel feedbackBox = new Theme.RoundedPanel(Theme.SURFACE_ALT, Theme.BORDER, 10);
        feedbackBox.setLayout(new BorderLayout());
        feedbackBox.add(feedbackLabel, BorderLayout.CENTER);
        center.add(feedbackBox, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);
        hintBtn.addActionListener(e -> {
            if (session != null) {
                session.showHint();
                syncView();
            }
        });
        resetBtn.addActionListener(e -> {
            stopTimer();
            if (session != null) {
                lessonFinished = false;
                session.reset();
                syncView();
            }
        });
        prevBtn.addActionListener(e -> {
            stopTimer();
            if (session != null && session.hasPrevStep()) {
                session.prevStep();
                syncView();
            }
        });
        nextBtn.addActionListener(e -> advanceOrNextLesson());

        buttons.add(hintBtn);
        buttons.add(resetBtn);
        buttons.add(prevBtn);
        buttons.add(nextBtn);

        guide.add(top, BorderLayout.NORTH);
        guide.add(center, BorderLayout.CENTER);
        guide.add(buttons, BorderLayout.SOUTH);
        return guide;
    }

    @Override
    public void doLayout() {
        int width = Math.max(282, Math.min(430, Math.round(getWidth() * 0.315f)));
        if (guideWidth != width) {
            guideWidth = width;
            guide.setPreferredSize(new Dimension(width, 0));
            float scale = (width - 60) / 260f;
            promptArea.setFont(Theme.font(18 * scale));
            feedbackLabel.setFont(Theme.bold(16 * scale));
            stepCounter.setFont(Theme.font(15 * scale));
            resizeGuideTitle();
        }
        super.doLayout();
    }

    private void resizeGuideTitle() {
        if (guideWidth == 0) return;
        float size = 22 * (guideWidth - 60) / 260f;
        var font = Theme.bold(size);
        int available = guideWidth - 36;
        int textWidth = lessonTitle.getFontMetrics(font).stringWidth(lessonTitle.getText());
        if (textWidth > available) font = font.deriveFont(size * available / textWidth);
        lessonTitle.setFont(font);
    }

    static String fixedGuideLines(String text, java.awt.FontMetrics metrics, int width) {
        StringBuilder result = new StringBuilder();
        String[] paragraphs = text.split("\\R", -1);
        for (int index = 0; index < paragraphs.length; index++) {
            if (index > 0) result.append('\n');
            String paragraph = paragraphs[index];
            BreakIterator lines = BreakIterator.getLineInstance(Locale.KOREAN);
            lines.setText(paragraph);
            int start = 0;
            int previous = 0;
            for (int end = lines.next(); end != BreakIterator.DONE; end = lines.next()) {
                if (previous > start && metrics.stringWidth(paragraph.substring(start, end).stripTrailing()) > width) {
                    result.append(paragraph.substring(start, previous).strip()).append('\n');
                    start = previous;
                }
                previous = end;
            }
            result.append(paragraph.substring(start).strip());
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

    private void switchCategory(Category category) {
        stopTimer();
        this.activeCategory = category;
        styleTab(tutorialTab, category == Category.TUTORIAL);
        styleTab(openingTab, category == Category.OPENING);

        List<Lesson> list = backend.lessons(category);
        if (!list.isEmpty()) {
            Lesson target = list.stream()
                    .filter(l -> !backend.activeProfile().isLessonCompleted(l.id()))
                    .findFirst()
                    .orElse(list.getFirst());
            selectLesson(target);
        }
        rebuildList();
    }

    private static void styleTab(JButton btn, boolean active) {
        if (active) {
            Theme.applyButtonStyle(btn, Theme.ACCENT, Theme.ACCENT_HOVER, Color.WHITE, null);
        } else {
            Theme.applyButtonStyle(btn, Theme.SURFACE_ALT, Theme.SURFACE_HOVER, Theme.MUTED, null);
        }
    }

    private void selectLesson(Lesson lesson) {
        lessonFinished = false;
        stopTimer();
        this.session = backend.startLesson(lesson);
        rebuildList();
        syncView();
    }

    private void rebuildList() {
        listContainer.removeAll();
        List<Lesson> list = backend.lessons(activeCategory);
        String currentId = session == null ? "" : session.lesson().id();

        for (Lesson lesson : list) {
            boolean done = backend.activeProfile().isLessonCompleted(lesson.id());
            boolean selected = lesson.id().equals(currentId);
            String prefix = done ? "✓ " : "○ ";
            Color bg = selected ? Theme.ACCENT_SOFT : Theme.SURFACE;
            Color hover = selected ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT;
            Color fg = selected ? Theme.SUCCESS : (done ? Theme.TEXT : Theme.MUTED);

            JButton item = Theme.styledButton(prefix + lesson.title(), bg, hover, fg);
            if (selected) {
                Theme.applyButtonStyle(item, bg, hover, fg, Theme.ACCENT);
            }
            item.setHorizontalAlignment(JButton.LEFT);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            item.addActionListener(e -> selectLesson(lesson));

            listContainer.add(item);
            listContainer.add(Box.createVerticalStrut(4));
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    private void onIllegalMove(int from, int to) {
        if (session == null) return;
        boardView.flashWrong(to);
        RetryCard.show(boardView, "이 칸으로는 이동할 수 없어요.", RetryCard.illegalMoveReason(session.board(), from, to));
    }

    private void onUserMove(Move move) {
        if (session == null) return;
        TrainingSession.AttemptResult result = session.submitMove(move);
        if (result.outcome() == TrainingSession.Outcome.WRONG) {
            boardView.flashWrong(move.to());
            syncView();
            RetryCard.show(boardView, "레슨 목표와 다른 수예요.", result.message());
            return;
        }

        if (session.isAllCompleted()) {
            backend.completeLesson(session.lesson().id());
            rebuildList();
        }
        syncView();

        if (result.autoReply() != null) {
            boardView.setInteractive(false);
            stopTimer();
            replyTimer = new Timer(420, e -> {
                session.applyPendingReply();
                syncView();
            });
            replyTimer.setRepeats(false);
            replyTimer.start();
        }
    }

    private void advanceOrNextLesson() {
        stopTimer();
        if (session == null) return;
        if (session.hasNextStep()) {
            session.nextStep();
            syncView();
            return;
        }
        lessonFinished = true;
        lessonList.openAndFocus();
        syncView();
    }

    private void syncView() {
        if (session == null) return;
        Lesson lesson = session.lesson();
        Lesson.Step step = session.currentStep();

        lessonTitle.setText(lesson.title());
        resizeGuideTitle();
        stepCounter.setText("단계 " + (session.stepIndex() + 1) + " / " + session.stepCount());
        stepBar.setMaximum(session.stepCount());
        stepBar.setValue(session.stepIndex() + (session.isSolved() ? 1 : 0));
        promptArea.setText(fixedGuideLines(step.prompt(), promptArea.getFontMetrics(Theme.font(18)), 245));
        promptArea.setCaretPosition(0);

        if (session.hasNextStep() || !session.isSolved()) lessonFinished = false;
        String fb = lessonFinished ? "레슨 완료! 목록에서 다른 레슨을 고를 수 있어요." : session.feedback();
        if (fb.isEmpty()) {
            feedbackLabel.setText("보드에서 직접 기물을 움직여 보세요.");
            feedbackLabel.setForeground(Theme.MUTED);
        } else {
            feedbackLabel.setText(fixedGuideLines(fb, feedbackLabel.getFontMetrics(Theme.bold(16)), 245));
            feedbackLabel.setForeground(session.isFeedbackPositive() ? Theme.SUCCESS : Theme.WARNING);
        }

        prevBtn.setEnabled(session.hasPrevStep());
        nextBtn.setEnabled(!lessonFinished && session.isSolved() && !session.hasPendingReply());
        nextBtn.setText(session.hasNextStep() ? "다음 단계 →" : lessonFinished ? "완료됨" : "레슨 완료");

        boardView.setFlipped(session.orientation() == Side.BLACK);
        boardView.setBoard(session.board());
        boardView.setLastMove(session.lastMove());
        boardView.setHighlightSquares(session.highlightSquares());
        boardView.setArrow(session.hintArrow(), Theme.ACCENT);
        boardView.setInteractive(!session.isSolved() && !session.hasPendingReply());
    }

    private void stopTimer() {
        if (replyTimer != null) {
            replyTimer.stop();
            replyTimer = null;
        }
    }

    @Override
    public JComponent view() {
        return this;
    }

    @Override
    public void onShow() {
        rebuildList();
        syncView();
    }

    @Override
    public void onHide() {
        stopTimer();
    }

    @Override
    public void onSettingsChanged() {
        boardView.repaint();
    }
}
