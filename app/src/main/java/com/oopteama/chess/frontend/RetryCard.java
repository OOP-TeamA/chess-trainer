package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.engine.Piece.Type;
import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

final class RetryCard {
    private RetryCard() {}

    static String illegalMoveReason(Board board, int from, int to) {
        if (board.pieceAt(to).side() == board.turn()) {
            return Board.formatSquare(to) + "에 아군 기물이 있어요.";
        }
        Type type = board.pieceAt(from).type();
        int dx = Board.file(to) - Board.file(from);
        int dy = Board.rank(to) - Board.rank(from);
        boolean straight = dx == 0 || dy == 0;
        boolean diagonal = Math.abs(dx) == Math.abs(dy);
        if (type == Type.ROOK && !straight) return "룩은 가로와 세로로만 움직일 수 있어요.";
        if (type == Type.BISHOP && !diagonal) return "비숍은 대각선으로만 움직일 수 있어요.";
        if (type == Type.QUEEN && !straight && !diagonal) return "퀸은 가로, 세로, 대각선으로 움직여요.";
        if (type == Type.ROOK || type == Type.BISHOP || type == Type.QUEEN) {
            for (int step = 1; step < Math.max(Math.abs(dx), Math.abs(dy)); step++) {
                int square = Board.square(Board.file(from) + step * Integer.signum(dx),
                        Board.rank(from) + step * Integer.signum(dy));
                if (!board.pieceAt(square).isEmpty()) {
                    return Board.formatSquare(square) + "의 기물이 길을 막고 있어요.";
                }
            }
        }
        return Board.formatSquare(from) + "에서 " + Board.formatSquare(to)
                + "로는 이 국면에서 이동할 수 없어요. 표시된 이동 가능 칸을 확인해 보세요.";
    }

    static void show(JComponent boardView, String title, String reason) {
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
        heading.setForeground(Theme.SUCCESS);
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
