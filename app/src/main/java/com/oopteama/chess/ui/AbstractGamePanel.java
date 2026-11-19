package com.oopteama.chess.ui;

import com.oopteama.chess.engine.ChessGame;
import com.oopteama.chess.engine.Move;
import com.oopteama.chess.engine.Piece;
import com.oopteama.chess.engine.Piece.Side;
import com.oopteama.chess.model.AppStore;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * AI 대전과 로컬 대전이 공유하는 대국 화면 기반 클래스입니다.
 * 체스판, 상·하단 플레이어/잡힌 기물 표시, 상태 배너, 기보 목록 레이아웃을 공통 제공합니다.
 */
public abstract class AbstractGamePanel extends JPanel implements Screen {

    protected final AppStore store;
    protected final ChessGame game = new ChessGame();
    protected final BoardView boardView;

    private final JLabel topNameLabel = new JLabel();
    private final JLabel topCapturedLabel = Theme.muted("");
    private final JLabel bottomNameLabel = new JLabel();
    private final JLabel bottomCapturedLabel = Theme.muted("");

    protected final JLabel statusBanner = new JLabel("", SwingConstants.CENTER);
    private final JTextArea moveListArea = new JTextArea();

    protected AbstractGamePanel(AppStore store) {
        this.store = store;
        this.boardView = new BoardView(store.settings());
        this.boardView.setMoveHandler(this::onBoardMove);

        setLayout(new BorderLayout(18, 14));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));
    }

    /**
     * 하위 클래스 생성자 끝에서 호출해 공통 레이아웃을 조립합니다.
     */
    protected final void initLayout() {
        JComponent topBar = buildTopBar();
        if (topBar != null) {
            add(topBar, BorderLayout.NORTH);
        }
        add(buildCenterColumn(), BorderLayout.CENTER);
        add(buildRightColumn(), BorderLayout.EAST);
        refreshBoardUi();
    }

    protected abstract JComponent buildTopBar();

    protected abstract JComponent buildControlBar();

    protected abstract String playerName(Side side);

    protected abstract void onBoardMove(Move move);

    private JPanel buildCenterColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setOpaque(false);
        col.add(buildPlayerStrip(topNameLabel, topCapturedLabel), BorderLayout.NORTH);
        col.add(boardView, BorderLayout.CENTER);
        col.add(buildPlayerStrip(bottomNameLabel, bottomCapturedLabel), BorderLayout.SOUTH);
        return col;
    }

    private JPanel buildPlayerStrip(JLabel nameLabel, JLabel capturedLabel) {
        JPanel bar = new Theme.RoundedPanel(Theme.SURFACE, Theme.BORDER, 10);
        bar.setLayout(new BorderLayout(10, 0));
        bar.setBorder(new EmptyBorder(8, 14, 8, 14));
        nameLabel.setFont(Theme.bold(14));
        nameLabel.setForeground(Theme.TEXT);
        capturedLabel.setFont(Theme.font(13));
        bar.add(nameLabel, BorderLayout.WEST);
        bar.add(capturedLabel, BorderLayout.EAST);
        return bar;
    }

    private JPanel buildRightColumn() {
        JPanel right = Theme.card(16);
        right.setPreferredSize(new Dimension(280, 0));

        statusBanner.setFont(Theme.bold(15));
        statusBanner.setOpaque(false);
        statusBanner.setForeground(Theme.TEXT);
        statusBanner.setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel statusBox = new Theme.RoundedPanel(Theme.SURFACE_ALT, Theme.BORDER, 10);
        statusBox.setLayout(new BorderLayout());
        statusBox.add(statusBanner, BorderLayout.CENTER);

        moveListArea.setEditable(false);
        moveListArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        moveListArea.setBackground(Theme.SURFACE);
        moveListArea.setForeground(Theme.TEXT);
        moveListArea.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel mid = new JPanel(new BorderLayout(0, 6));
        mid.setOpaque(false);
        mid.setBorder(new EmptyBorder(10, 0, 10, 0));
        mid.add(Theme.muted("기보 기록"), BorderLayout.NORTH);
        mid.add(Theme.scrollPane(moveListArea), BorderLayout.CENTER);

        right.add(statusBox, BorderLayout.NORTH);
        right.add(mid, BorderLayout.CENTER);
        right.add(buildControlBar(), BorderLayout.SOUTH);
        return right;
    }

    protected final void refreshBoardUi() {
        boardView.setBoard(game.board());
        boardView.setLastMove(game.lastMove().map(Move.Record::move).orElse(null));

        Side topSide = boardView.isFlipped() ? Side.WHITE : Side.BLACK;
        Side bottomSide = topSide.opposite();

        topNameLabel.setText(sideTag(topSide) + " " + playerName(topSide));
        bottomNameLabel.setText(sideTag(bottomSide) + " " + playerName(bottomSide));
        topCapturedLabel.setText(formatCaptured(game.capturedBy(topSide)));
        bottomCapturedLabel.setText(formatCaptured(game.capturedBy(bottomSide)));

        updateStatusBanner(game.statusText(), statusColor());
        updateMoveList();
    }

    protected final void updateStatusBanner(String text, Color fg) {
        statusBanner.setText(text);
        statusBanner.setForeground(fg);
    }

    private Color statusColor() {
        return switch (game.status()) {
            case CHECK -> Theme.WARNING;
            case CHECKMATE, RESIGNED_WHITE, RESIGNED_BLACK -> Theme.SUCCESS;
            case STALEMATE, DRAW_INSUFFICIENT, DRAW_FIFTY_MOVE, DRAW_REPETITION -> Theme.INFO;
            default -> Theme.TEXT;
        };
    }

    private void updateMoveList() {
        List<Move.Record> records = game.history();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < records.size(); i += 2) {
            int moveNum = (i / 2) + 1;
            String whiteSan = records.get(i).san();
            String blackSan = (i + 1 < records.size()) ? records.get(i + 1).san() : "";
            sb.append(String.format("%2d. %-9s %s%n", moveNum, whiteSan, blackSan));
        }
        moveListArea.setText(sb.toString());
        moveListArea.setCaretPosition(moveListArea.getDocument().getLength());
    }

    private static String sideTag(Side side) {
        return side == Side.WHITE ? "○" : "●";
    }

    private static String formatCaptured(List<Piece> pieces) {
        if (pieces.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (Piece p : pieces) {
            sb.append(p.type().symbol()).append(' ');
        }
        return sb.toString().trim();
    }

    @Override
    public JComponent view() {
        return this;
    }

    @Override
    public void onSettingsChanged() {
        boardView.repaint();
    }

    @Override
    public String leaveWarning() {
        return (!game.isOver() && !game.history().isEmpty())
                ? "진행 중인 대국이 있습니다. 화면을 이동하시겠습니까?"
                : null;
    }
}
