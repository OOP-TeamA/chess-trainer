package com.oopteama.chess.ui.screen;

import com.oopteama.chess.data.Settings;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.BoardPanel;
import com.oopteama.chess.ui.component.RoundedPanel;

import javax.swing.JButton;
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
import java.awt.GridLayout;

/**
 * 대국 화면(View)의 공통 부모 클래스입니다.
 * 가운데에 체스판과 두 선수 정보, 오른쪽에 대국 상태와 기보를 보여 줍니다.
 * 위쪽 막대와 아래쪽 버튼은 AiGameScreen, LocalGameScreen이 각자 채웁니다.
 */
public class GameScreen extends JPanel {

    private final BoardPanel boardPanel;
    private final JLabel topNameLabel = new JLabel();
    private final JLabel topCapturedLabel = Theme.muted("");
    private final JLabel bottomNameLabel = new JLabel();
    private final JLabel bottomCapturedLabel = Theme.muted("");
    private final JLabel statusBanner = new JLabel("", SwingConstants.CENTER);
    private final JTextArea moveListArea = new JTextArea();
    private final JPanel controls = new JPanel(new GridLayout(2, 2, 8, 8));

    protected GameScreen(Settings settings) {
        boardPanel = new BoardPanel(settings);
        setLayout(new BorderLayout(18, 14));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(18, 22, 18, 22));
        add(buildBoardColumn(), BorderLayout.CENTER);
        add(buildInfoColumn(), BorderLayout.EAST);
    }

    private JPanel buildBoardColumn() {
        JPanel column = new JPanel(new BorderLayout(0, 8));
        column.setOpaque(false);
        column.add(playerStrip(topNameLabel, topCapturedLabel), BorderLayout.NORTH);
        column.add(boardPanel, BorderLayout.CENTER);
        column.add(playerStrip(bottomNameLabel, bottomCapturedLabel), BorderLayout.SOUTH);
        return column;
    }

    /** 선수 이름과 잡은 기물을 보여 주는 띠. */
    private static JPanel playerStrip(JLabel nameLabel, JLabel capturedLabel) {
        JPanel strip = new RoundedPanel(Theme.SURFACE, Theme.BORDER, 10);
        strip.setLayout(new BorderLayout(10, 0));
        strip.setBorder(new EmptyBorder(8, 14, 8, 14));
        nameLabel.setFont(Theme.bold(14));
        nameLabel.setForeground(Theme.TEXT);
        capturedLabel.setFont(Theme.font(13));
        strip.add(nameLabel, BorderLayout.WEST);
        strip.add(capturedLabel, BorderLayout.EAST);
        return strip;
    }

    private JPanel buildInfoColumn() {
        JPanel card = Theme.card(16);
        card.setPreferredSize(new Dimension(280, 0));

        statusBanner.setFont(Theme.bold(15));
        statusBanner.setForeground(Theme.TEXT);
        statusBanner.setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel statusBox = new RoundedPanel(Theme.SURFACE_ALT, Theme.BORDER, 10);
        statusBox.setLayout(new BorderLayout());
        statusBox.add(statusBanner, BorderLayout.CENTER);

        moveListArea.setEditable(false);
        moveListArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        moveListArea.setBackground(Theme.SURFACE);
        moveListArea.setForeground(Theme.TEXT);
        moveListArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        JPanel moveList = new JPanel(new BorderLayout(0, 6));
        moveList.setOpaque(false);
        moveList.setBorder(new EmptyBorder(10, 0, 10, 0));
        moveList.add(Theme.muted("기보 기록"), BorderLayout.NORTH);
        moveList.add(Theme.scrollPane(moveListArea), BorderLayout.CENTER);

        controls.setOpaque(false);
        card.add(statusBox, BorderLayout.NORTH);
        card.add(moveList, BorderLayout.CENTER);
        card.add(controls, BorderLayout.SOUTH);
        return card;
    }

    /** 하위 클래스가 화면 위쪽 막대를 넣습니다. */
    protected final void setTopBar(JComponent bar) {
        add(bar, BorderLayout.NORTH);
    }

    /** 하위 클래스가 오른쪽 아래 버튼 4개를 넣습니다. */
    protected final void addControls(JButton... buttons) {
        for (JButton button : buttons) {
            controls.add(button);
        }
    }

    // ---- 컨트롤러가 쓰는 메서드 ----

    public BoardPanel boardPanel() {
        return boardPanel;
    }

    /** 위·아래 선수 띠: 이름과 잡은 기물. */
    public void showPlayers(String topName, String topCaptured, String bottomName, String bottomCaptured) {
        topNameLabel.setText(topName);
        topCapturedLabel.setText(topCaptured);
        bottomNameLabel.setText(bottomName);
        bottomCapturedLabel.setText(bottomCaptured);
    }

    /** 대국 상태 문구 (예: "백 차례", "체크메이트! 흑 승리"). */
    public void showStatus(String text, Color color) {
        statusBanner.setText(text);
        statusBanner.setForeground(color);
    }

    /** 기보 기록. 항상 마지막 줄이 보이게 내립니다. */
    public void showMoveList(String text) {
        moveListArea.setText(text);
        moveListArea.setCaretPosition(moveListArea.getDocument().getLength());
    }
}
