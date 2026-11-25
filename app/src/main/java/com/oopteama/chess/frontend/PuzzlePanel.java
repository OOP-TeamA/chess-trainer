package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.service.ChessBackend;
import com.oopteama.chess.backend.training.Puzzle;
import com.oopteama.chess.backend.training.PuzzleSession;
import com.oopteama.chess.backend.training.TrainingSession;

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
 * 퍼즐 화면: 테마 필터, 자유 메이트(N수 이내 자율 풀이 + 무르기)와 전술 퍼즐 풀이를 제공합니다.
 */
public final class PuzzlePanel extends JPanel implements Screen {

    private static final String ALL_THEMES = "전체 퍼즐";
    private static final String MATE_PUZZLES = "체크메이트";
    private static final String TACTICAL_PUZZLES = "전술 문제";

    private final ChessBackend backend;
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

    public PuzzlePanel(ChessBackend backend) {
        this.backend = backend;
        this.boardView = new BoardView(backend.settings(), backend::evaluateMove);
        this.boardView.setMoveHandler(this::onUserMove);
        this.boardView.setIllegalMoveHandler(this::onIllegalMove);

        setLayout(new BorderLayout(18, 0));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(20, 22, 20, 22));

        add(new CollapsibleList(buildLeftPanel(), "퍼즐 목록"), BorderLayout.WEST);
        add(boardView, BorderLayout.CENTER);
        add(buildRightPanel(), BorderLayout.EAST);

        themeFilter.addItem(ALL_THEMES);
        themeFilter.addItem(MATE_PUZZLES);
        themeFilter.addItem(TACTICAL_PUZZLES);
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
        right.setPreferredSize(new Dimension(320, 0));

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

        feedbackLabel.setFont(Theme.bold(15));
        feedbackLabel.setOpaque(false);
        feedbackLabel.setBorder(new EmptyBorder(10, 12, 10, 12));
        JPanel feedbackBox = new Theme.RoundedPanel(Theme.SURFACE_ALT, Theme.BORDER, 10);
        feedbackBox.setLayout(new BorderLayout());
        feedbackBox.add(feedbackLabel, BorderLayout.CENTER);
        center.add(feedbackBox, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
        buttons.setOpaque(false);
        hintBtn.setToolTipText("힌트 · 다음 수를 둘 칸을 확인해요.");
        resetBtn.setToolTipText("다시 풀기 · 이 퍼즐의 시작 국면으로 돌아가요.");
        nextBtn.setToolTipText("다음 퍼즐 · 목록의 다음 문제를 열어요.");
        hintBtn.addActionListener(e -> {
            if (session != null) {
                session.showHint();
                syncView();
            }
        });
        undoBtn.addActionListener(e -> {
            if (session != null && !session.hasPendingReply() && session.undo()) {
                stopTimer();
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
        buttons.add(hintBtn);
        buttons.add(undoBtn);
        buttons.add(resetBtn);
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
        area.setFont(Theme.font(20));
        area.setForeground(Theme.TEXT);
        area.setBackground(Theme.SURFACE);
        area.setBorder(new EmptyBorder(15, 16, 15, 16));
        return area;
    }

    static String neutralTitle(List<Puzzle> puzzles, Puzzle puzzle) {
        int number = puzzles.indexOf(puzzle) + 1;
        String goal = puzzle.isMatePuzzle() ? puzzle.mateIn() + "수 안에 체크메이트" : "최선의 수 찾기";
        return "%02d. %s".formatted(number, goal);
    }

    private void applyFilter() {
        String selected = (String) themeFilter.getSelectedItem();
        visiblePuzzles = new ArrayList<>();
        for (Puzzle p : backend.puzzles()) {
            if (ALL_THEMES.equals(selected) || p.isMatePuzzle() == MATE_PUZZLES.equals(selected)) {
                visiblePuzzles.add(p);
            }
        }
        if (!visiblePuzzles.isEmpty()) {
            Puzzle next = visiblePuzzles.stream()
                    .filter(p -> !backend.activeProfile().isPuzzleSolved(p.id()))
                    .findFirst()
                    .orElse(visiblePuzzles.getFirst());
            selectPuzzle(next);
        } else {
            rebuildList();
        }
    }

    private void selectPuzzle(Puzzle puzzle) {
        stopTimer();
        this.session = backend.startPuzzle(puzzle);
        this.recordedForAttempt = false;
        rebuildList();
        syncView();
    }

    private void selectNextPuzzle() {
        if (visiblePuzzles.isEmpty() || session == null) return;
        int idx = visiblePuzzles.indexOf(session.puzzle());
        selectPuzzle(visiblePuzzles.get((idx + 1) % visiblePuzzles.size()));
    }

    private void rebuildList() {
        listContainer.removeAll();
        String currentId = session == null ? "" : session.puzzle().id();
        for (Puzzle puzzle : visiblePuzzles) {
            boolean solved = backend.activeProfile().isPuzzleSolved(puzzle.id());
            boolean selected = puzzle.id().equals(currentId);
            String label = (solved ? "✓ " : "○ ") + neutralTitle(backend.puzzles(), puzzle);

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

    private void onIllegalMove(int from, int to) {
        if (session == null) return;
        boardView.flashWrong(to);
        RetryCard.show(boardView, "이 칸으로는 이동할 수 없어요.",
                RetryCard.illegalMoveReason(session.board(), from, to, backend.settings().showLegalMoves()));
    }

    private void onUserMove(Move move) {
        if (session == null) return;
        TrainingSession.AttemptResult result = session.submitMove(move);
        if (result.outcome() == TrainingSession.Outcome.WRONG) {
            if (!recordedForAttempt) {
                backend.recordPuzzle(session.puzzle().id(), false);
                recordedForAttempt = true;
            }
            boardView.flashWrong(move.to());
            syncView();
            RetryCard.show(boardView, "퍼즐 목표와 다른 수예요.", result.message());
            return;
        }

        if (result.outcome() == TrainingSession.Outcome.SOLVED) {
            backend.recordPuzzle(session.puzzle().id(), true);
            recordedForAttempt = true;
            rebuildList();
        }
        syncView();

        if (result.autoReply() != null) {
            scheduleReply();
        }
    }

    /** 예약된 상대 응수를 잠시 뒤 보드에 반영합니다. */
    private void scheduleReply() {
        boardView.setInteractive(false);
        stopTimer();
        replyTimer = new Timer(400, e -> {
            session.applyPendingReply();
            syncView();
        });
        replyTimer.setRepeats(false);
        replyTimer.start();
    }

    private void syncView() {
        if (session == null) return;
        Puzzle p = session.puzzle();
        themeBadge.setText(p.isMatePuzzle() ? (p.mateIn() + "수 메이트 · 자유 풀이") : TACTICAL_PUZZLES);
        titleLabel.setText(neutralTitle(backend.puzzles(), p));
        explainArea.setToolTipText(null);
        if (p.isMatePuzzle()) {
            turnLabel.setText(p.startBoard().turn().label() + " 차례 · 남은 수 "
                    + session.remainingMoves() + " / " + p.mateIn());
        } else {
            turnLabel.setText(p.startBoard().turn().label() + " 차례 · 수순 "
                    + session.currentUserMoveNumber() + "/" + session.totalUserMoves());
        }

        boolean canUndo = session.canUndo() && !session.hasPendingReply();
        if (session.isSolved()) {
            explainArea.setText("해설 · " + p.title() + "\n\n" + p.explanation());
            explainArea.setToolTipText("<html><div style='width: 240px'>" +
                    p.explanation().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                    .replace("\n", "<br>") + "</div></html>");
        } else if (p.isMatePuzzle()) {
            String nextAction = session.hasPendingReply() ? "\n\n상대의 응수를 기다리는 중이에요. 응수가 끝나면 다음 수를 두세요."
                    : canUndo ? "\n\n이전 차례로 돌아가려면 [무르기]를 눌러 주세요."
                    : "\n\n오답인 수는 보드에 반영되지 않아요. 현재 국면에서 다른 수를 둘 수 있어요.";
            explainArea.setText(p.startBoard().turn().label() + " 차례입니다.\n\n"
                    + p.mateIn() + "수 안에 체크메이트해 보세요. 어떤 수순이든 괜찮아요." + nextAction);
        } else {
            explainArea.setText(p.startBoard().turn().label() + "의 최선의 전술 수를 찾아 보드에서 직접 두어 보세요.");
        }

        explainArea.setCaretPosition(0);
        String fb = session.feedback();
        if (fb.isEmpty()) {
            feedbackLabel.setText("<html>문제를 풀면 해설이 공개됩니다.</html>");
            feedbackLabel.setForeground(Theme.MUTED);
        } else {
            feedbackLabel.setText("<html>" + fb + "</html>");
            feedbackLabel.setForeground(session.isFeedbackPositive() ? Theme.SUCCESS : Theme.WARNING);
        }

        undoBtn.setEnabled(canUndo);
        undoBtn.setToolTipText(canUndo ? "무르기 · 직전 차례로 돌아가요."
                : session.hasPendingReply() ? "상대의 응수를 기다리는 중이에요."
                : "현재 무를 수 있는 수가 없어요.");
        boardView.setFlipped(session.orientation() == Side.BLACK);
        boardView.setBoard(session.board());
        boardView.setLastMove(session.lastMove());
        boardView.setHighlightSquares(session.highlightSquares());
        boardView.setArrow(session.hintArrow(), Theme.BOARD_INFO);
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
        // 응수를 기다리는 중에 화면을 떠났다면 onHide에서 멈춘 응수를 다시 예약
        if (session != null && session.hasPendingReply()) {
            scheduleReply();
        }
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
