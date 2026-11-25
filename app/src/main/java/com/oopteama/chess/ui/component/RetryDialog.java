package com.oopteama.chess.ui.component;

import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.model.piece.Bishop;
import com.oopteama.chess.model.piece.Piece;
import com.oopteama.chess.model.piece.Queen;
import com.oopteama.chess.model.piece.Rook;
import com.oopteama.chess.model.piece.SlidingPiece;
import com.oopteama.chess.ui.Theme;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

/**
 * 잘못 둔 수를 알려 주는 "다시 두기" 카드입니다.
 * 규칙상 갈 수 없는 칸이면 이유를 설명하고, 목표와 다른 수면 세션의 피드백 문구를 보여 줍니다.
 */
public final class RetryDialog {

    private RetryDialog() {
    }

    /**
     * from 칸의 기물이 to 칸으로 갈 수 없는 이유를 설명합니다.
     *
     * @param legalMovesShown 보드에 이동 가능 칸 점이 표시되는지 (설정에서 끌 수 있음)
     */
    public static String illegalMoveReason(Board board, Position from, Position to, boolean legalMovesShown) {
        Piece target = board.pieceAt(to);
        if (target != null && target.side() == board.turn()) {
            return to + "에 아군 기물이 있어요.";
        }
        Piece piece = board.pieceAt(from);
        int dCol = to.col() - from.col();
        int dRow = to.row() - from.row();
        boolean straight = dCol == 0 || dRow == 0;
        boolean diagonal = Math.abs(dCol) == Math.abs(dRow);
        if (piece instanceof Rook && !straight) {
            return "룩은 가로와 세로로만 움직일 수 있어요.";
        }
        if (piece instanceof Bishop && !diagonal) {
            return "비숍은 대각선으로만 움직일 수 있어요.";
        }
        if (piece instanceof Queen && !straight && !diagonal) {
            return "퀸은 가로, 세로, 대각선으로 움직여요.";
        }
        if (piece instanceof SlidingPiece) {
            // 가는 길 중간에 다른 기물이 있으면 막힌 것입니다.
            int steps = Math.max(Math.abs(dCol), Math.abs(dRow));
            for (int step = 1; step < steps; step++) {
                Position square = from.offset(step * Integer.signum(dCol), step * Integer.signum(dRow));
                if (!board.isEmpty(square)) {
                    return square + "의 기물이 길을 막고 있어요.";
                }
            }
        }
        return from + "에서 " + toward(to) + "는 이 국면에서 이동할 수 없어요. "
                + (legalMovesShown ? "점으로 표시된 이동 가능 칸 중에서 골라 보세요." : "이 기물이 갈 수 있는 다른 칸을 찾아보세요.");
    }

    /** 칸 이름에 '(으)로'를 붙입니다. 숫자를 한국어로 읽으면 3(삼)·6(육)만 받침이 있어 '으로'가 됩니다. */
    static String toward(Position square) {
        int rank = square.row() + 1;
        return square + (rank == 3 || rank == 6 ? "으로" : "로");
    }

    /** 체스판 위에 카드를 띄웁니다. 닫을 때까지 다른 입력을 막습니다(모달). */
    public static void show(JComponent boardView, String title, String reason) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(boardView), title,
                Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel card = new JPanel(new BorderLayout(0, 16));
        card.setBackground(Theme.SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.ACCENT, 1),
                new EmptyBorder(22, 24, 18, 24)));
        JLabel heading = Theme.sectionTitle(title);
        heading.setFont(Theme.bold(19));
        heading.setForeground(Theme.WARNING);
        card.add(heading, BorderLayout.NORTH);

        JTextArea message = new JTextArea(reason, 0, 26);
        message.setEditable(false);
        message.setOpaque(false);
        message.setLineWrap(true);
        message.setWrapStyleWord(true);
        message.setFont(Theme.font(15));
        message.setForeground(Theme.TEXT);
        message.getAccessibleContext().setAccessibleName(reason);
        message.setSize(message.getPreferredSize().width, Short.MAX_VALUE);
        card.add(message, BorderLayout.CENTER);

        JButton retry = Theme.primaryButton("다시 두기");
        retry.addActionListener(event -> dialog.dispose());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.TRAILING, 0, 0));
        actions.setOpaque(false);
        actions.add(retry);
        card.add(actions, BorderLayout.SOUTH);

        dialog.setContentPane(card);
        dialog.getRootPane().setDefaultButton(retry);
        dialog.getRootPane().registerKeyboardAction(event -> dialog.dispose(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(boardView);
        dialog.setVisible(true);
    }
}
