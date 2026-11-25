package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.engine.Piece.Side;
import com.oopteama.chess.backend.service.ChessBackend;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

/**
 * 로컬 2인 대전 화면: 한 보드에서 백·흑이 번갈아 두며 설정에 따른 자동 보드 회전을 지원합니다.
 */
public final class LocalGamePanel extends AbstractGamePanel {

    private final JLabel autoFlipBadge = Theme.badge("", Theme.INFO);

    public LocalGamePanel(ChessBackend backend) {
        super(backend);
        initLayout();
        updateAutoFlipBadge();
    }

    @Override
    protected JComponent buildTopBar() {
        JPanel bar = Theme.card(10);
        bar.setLayout(new BorderLayout(10, 0));
        bar.add(Theme.sectionTitle("로컬 2인 대전"), BorderLayout.WEST);
        bar.add(autoFlipBadge, BorderLayout.EAST);
        return bar;
    }

    @Override
    protected JComponent buildControlBar() {
        JPanel controls = new JPanel(new GridLayout(2, 2, 8, 8));
        controls.setOpaque(false);

        JButton undoBtn = Theme.secondaryButton("무르기");
        JButton flipBtn = Theme.secondaryButton("보드 뒤집기");
        JButton resignBtn = Theme.dangerButton("기권");
        JButton newBtn = Theme.primaryButton("새 대국");

        undoBtn.addActionListener(e -> {
            if (game.undo()) {
                boardView.setInteractive(true);
                syncFlip();
                refreshBoardUi();
            }
        });
        flipBtn.addActionListener(e -> {
            boardView.setFlipped(!boardView.isFlipped());
            refreshBoardUi();
        });
        resignBtn.addActionListener(e -> {
            if (!game.isOver()) {
                game.resign(game.turn());
                boardView.setInteractive(false);
                refreshBoardUi();
            }
        });
        newBtn.addActionListener(e -> {
            game.reset();
            boardView.setFlipped(false);
            boardView.setInteractive(true);
            refreshBoardUi();
        });

        controls.add(undoBtn);
        controls.add(flipBtn);
        controls.add(resignBtn);
        controls.add(newBtn);
        return controls;
    }

    @Override
    protected String playerName(Side side) {
        return side == Side.WHITE ? "플레이어 1 (백)" : "플레이어 2 (흑)";
    }

    @Override
    protected void onBoardMove(Move move) {
        if (game.isOver()) return;
        game.play(move);
        if (game.isOver()) {
            boardView.setInteractive(false);
        } else {
            syncFlip();
        }
        refreshBoardUi();
    }

    private void syncFlip() {
        if (backend.settings().autoFlipLocal()) {
            boardView.setFlipped(game.turn() == Side.BLACK);
        }
    }

    private void updateAutoFlipBadge() {
        boolean on = backend.settings().autoFlipLocal();
        autoFlipBadge.setText(on ? "차례마다 자동 뒤집기 ON" : "수동 보드 회전");
    }

    @Override
    public void onSettingsChanged() {
        updateAutoFlipBadge();
        super.onSettingsChanged();
    }
}
