package com.oopteama.chess.ui;

import com.oopteama.chess.engine.Move;
import com.oopteama.chess.engine.Piece.Side;
import com.oopteama.chess.model.AppStore;
import com.oopteama.chess.model.Puzzle;
import com.oopteama.chess.model.PuzzleSession;
import com.oopteama.chess.model.TrainingSession;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 전술 퍼즐 화면: 테마 필터, 다수 수순 퍼즐 풀이, 상대 자동 응수, 힌트 및 정답 기록을 제공합니다.
 */
public final class PuzzlePanel extends JPanel implements Screen {

    private static final String ALL_THEMES = "전체 테마";

    private final AppStore store;
    private final BoardView boardView;
    private final JComboBox<String> themeFilter = new JComboBox<>();
    private final JPanel listContainer = new JPanel();

    private final JLabel themeBadge = Theme.badge("", Theme.INFO);
    private final JLabel titleLabel = Theme.sectionTitle("");
    private final JLabel turnLabel = Theme.muted("");
    private final JTextArea explainArea = createTextArea();
    private final JLabel feedbackLabel = new JLabel();
    private final JButton hintBtn = Theme.secondaryButton("힌트");
    private final JButton undoBtn = Theme.secondaryButton("무르기");
    private final JButton resetBtn = Theme.secondaryButton("다시 풀기");
    private final JButton nextBtn = Theme.primaryButton("다음 퍼즐 →");

    private List<Puzzle> visiblePuzzles = List.of();
    private PuzzleSession session;
    private boolean recordedForAttempt;
    private Timer replyTimer;

    public PuzzlePanel(AppStore store) {
        this.store = store;
        this.boardView = new BoardView(store.settings());
        this.boardView.setMoveHandler(this::onUserMove);

        setLayout(new BorderLayout(18, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 22, 20, 22));

        add(buildLeftPanel(), BorderLayout.WEST);
        add(boardView, BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);

        themeFilter.addItem(ALL_THEMES);
        for (String t : store.puzzleThemes()) {
            themeFilter.addItem(t);
        }
        themeFilter.addActionListener(e -> applyFilter());
        applyFilter();
    }

    private JPanel buildLeftPanel() {
        JPanel side = Theme.card(14);
        side.setPreferredSize(new Dimension(250, 0));

        listContainer.setLayout(new BoxLayout(listContainer, BoxLayout.Y_AXIS));
        listContainer.setBackground(Theme.SURFACE);

        side.add(themeFilter, BorderLayout.NORTH);
        side.add(Theme.scrollPane(listContainer), BorderLayout.CENTER);
        return side;
    }

    private JPanel buildRightPanel() {
        JPanel right = Theme.card(18);
        right.setPreferredSize(new Dimension(300, 0));

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

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(14, 0, 14, 0));
        center.add(Theme.scrollPane(explainArea), BorderLayout.CENTER);

        feedbackLabel.setFont(Theme.bold(13));
        feedbackLabel.setOpaque(false);
        feedbackLabel.setBorder(new EmptyBorder(10, 12, 10, 12));
        JPanel feedbackBox = new Theme.RoundedPanel(Theme.SURFACE_ALT, Theme.BORDER, 10);
        feedbackBox.setLayout(new BorderLayout());
        feedbackBox.add(feedbackLabel, BorderLayout.CENTER);
        center.add(feedbackBox, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new GridLayout(2, 1, 0, 8));
        buttons.setOpaque(false);
        JPanel topRow = new JPanel(new GridLayout(1, 3, 6, 0));
        topRow.setOpaque(false);
        hintBtn.addActionListener(e -> {
            if (session != null) {
                session.showHint();
                syncView();
            }
        });
        undoBtn.addActionListener(e -> {
            stopTimer();
            if (session != null && session.undo()) {
                syncView();
            }
        });
        resetBtn.addActionListener(e -> {
            stopTimer();
            if (session != null) {
                session.reset();
                recordedForAttempt = false;
                syncView();
            }
        });
        nextBtn.addActionListener(e -> selectNextPuzzle());
        topRow.add(hintBtn);
        topRow.add(undoBtn);
        topRow.add(resetBtn);
        buttons.add(topRow);
        buttons.add(nextBtn);

        right.add(top, BorderLayout.NORTH);
        right.add(center, BorderLayout.CENTER);
        right.add(buttons, BorderLayout.SOUTH);
        return right;
    }

    private static JTextArea createTextArea() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(Theme.font(14));
        area.setForeground(Theme.TEXT);
        area.setBackground(Theme.SURFACE);
        return area;
    }

    private void applyFilter() {
        String selected = (String) themeFilter.getSelectedItem();
        visiblePuzzles = new ArrayList<>();
        for (Puzzle p : store.puzzles()) {
            if (ALL_THEMES.equals(selected) || p.theme().equals(selected)) {
                visiblePuzzles.add(p);
            }
        }
        if (!visiblePuzzles.isEmpty()) {
            Puzzle next = visiblePuzzles.stream()
                    .filter(p -> !store.activeProfile().isPuzzleSolved(p.id()))
                    .findFirst()
                    .orElse(visiblePuzzles.getFirst());
            selectPuzzle(next);
        } else {
            rebuildList();
        }
    }

    private void selectPuzzle(Puzzle puzzle) {
        stopTimer();
        this.session = new PuzzleSession(puzzle);
        this.recordedForAttempt = false;
        rebuildList();
        syncView();
    }

    private void selectNextPuzzle() {
        if (visiblePuzzles.isEmpty() || session == null) {
            return;
        }
        int idx = visiblePuzzles.indexOf(session.puzzle());
        int nextIdx = (idx + 1) % visiblePuzzles.size();
        selectPuzzle(visiblePuzzles.get(nextIdx));
    }

    private void rebuildList() {
        listContainer.removeAll();
        String currentId = session == null ? "" : session.puzzle().id();
        for (Puzzle puzzle : visiblePuzzles) {
            boolean solved = store.activeProfile().isPuzzleSolved(puzzle.id());
            boolean selected = puzzle.id().equals(currentId);
            String label = (solved ? "✓ " : "○ ") + puzzle.title();

            Color bg = selected ? Theme.ACCENT_SOFT : Theme.SURFACE;
            Color hover = selected ? Theme.ACCENT_SOFT : Theme.SURFACE_ALT;
            Color fg = selected ? Theme.SUCCESS : (solved ? Theme.TEXT : Theme.MUTED);

            JButton item = Theme.styledButton(label, bg, hover, fg);
            if (selected) {
                Theme.applyButtonStyle(item, bg, hover, fg, Theme.ACCENT);
            }
            item.setHorizontalAlignment(JButton.LEFT);
            item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            item.addActionListener(e -> selectPuzzle(puzzle));

            listContainer.add(item);
            listContainer.add(Box.createVerticalStrut(4));
        }
        listContainer.revalidate();
        listContainer.repaint();
    }

    private void onUserMove(Move move) {
        if (session == null) {
            return;
        }
        TrainingSession.AttemptResult result = session.submitMove(move);
        if (result.outcome() == TrainingSession.Outcome.WRONG) {
            if (!recordedForAttempt) {
                store.activeProfile().recordPuzzle(session.puzzle().id(), false);
                store.saveAndNotify();
                recordedForAttempt = true;
            }
            boardView.flashWrong(move.to());
            syncView();
            return;
        }

        if (result.outcome() == TrainingSession.Outcome.SOLVED) {
            store.activeProfile().recordPuzzle(session.puzzle().id(), true);
            store.saveAndNotify();
            recordedForAttempt = true;
            rebuildList();
        }
        syncView();

        if (result.autoReply() != null) {
            boardView.setInteractive(false);
            stopTimer();
            replyTimer = new Timer(400, e -> {
                session.applyPendingReply();
                syncView();
            });
            replyTimer.setRepeats(false);
            replyTimer.start();
        }
    }

    private void syncView() {
        if (session == null) {
            return;
        }
        Puzzle p = session.puzzle();
        themeBadge.setText(p.isMatePuzzle() ? (p.mateIn() + "수 메이트 · 자유 풀이") : p.theme());
        titleLabel.setText(p.title());
        if (p.isMatePuzzle()) {
            turnLabel.setText(p.startBoard().turn().label() + " 차례 · 남은 수 "
                    + session.remainingMoves() + " / " + p.mateIn());
        } else {
            turnLabel.setText(p.startBoard().turn().label() + " 차례 · 수순 "
                    + session.currentUserMoveNumber() + "/" + session.totalUserMoves());
        }

        if (session.isSolved()) {
            explainArea.setText("해설\n\n" + p.explanation());
        } else if (p.isMatePuzzle()) {
            explainArea.setText(p.startBoard().turn().label() + " 차례입니다.\n\n"
                    + p.mateIn() + "수 안에 어떤 수순으로든 체크메이트하면 성공합니다! "
                    + "상대(AI)는 최선으로 방어하며, 다른 아이디어를 시도하고 싶으면 [무르기]를 사용할 수 있습니다.");
        } else {
            explainArea.setText(p.startBoard().turn().label() + "의 최선의 전술 수를 찾아 보드에서 직접 두어 보세요.");
        }

        undoBtn.setEnabled(session.canUndo() && !session.hasPendingReply());

        String fb = session.feedback();
        if (fb.isEmpty()) {
            feedbackLabel.setText("<html>문제를 풀면 해설이 공개됩니다.</html>");
            feedbackLabel.setForeground(Theme.MUTED);
        } else {
            feedbackLabel.setText("<html>" + fb + "</html>");
            feedbackLabel.setForeground(session.isFeedbackPositive() ? Theme.SUCCESS : Theme.WARNING);
        }

        boardView.setFlipped(session.orientation() == Side.BLACK);
        boardView.setBoard(session.board());
        boardView.setLastMove(session.lastMove());
        boardView.setHighlightSquares(session.highlightSquares());
        boardView.setArrow(session.hintArrow(), Theme.INFO);
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
