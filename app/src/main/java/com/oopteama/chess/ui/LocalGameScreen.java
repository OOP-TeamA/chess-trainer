package com.oopteama.chess.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.GridLayout;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.game.Game;
import com.oopteama.chess.game.GameListener;
import com.oopteama.chess.nav.Route;


/**
 * 사람과 사람이 한 기기에서 번갈아 체스를 두는 화면.
 *
 * BoardScreen을 상속하여 공통 체스판 레이아웃을 사용한다.
 * Game 객체를 통해 대국 상태를 관리한다.
 */
public final class LocalGameScreen extends BoardScreen {

    // 비활성화된 조작 버튼에 사용할 글자색
    private static final Color DISABLED_BUTTON_TEXT = new Color(0xC6AD8B);

    // 체스 규칙
    private final RuleSet rules;

    // 현재 대국
    private Game game;

    // 마지막으로 이동한 수
    private Move lastMove;

    // 리스너 등록 상태
    private boolean attached;


    // =========================================
    // 오른쪽 정보 패널
    // =========================================

    private JLabel turnLabel;

    private JLabel moveCountLabel;

    private JTextArea statusLabel;

    private JTextArea resultLabel;


    // =========================================
    // 하단 조작 버튼
    // =========================================

    private JButton undoButton;

    private JButton resignButton;

    private JButton newGameButton;


    // =========================================
    // 체스판 이동 이벤트
    // =========================================

    private final Consumer<Move> moveListener =
            this::onMoveSelected;


    // =========================================
    // Game 상태 변경 이벤트
    // =========================================

    private final GameListener gameListener =
            new GameListener() {

        @Override
        public void moveMade(
                Move move,
                Position after
        ) {

            lastMove = move;

            refreshView();
        }


        @Override
        public void moveUndone(
                Move move,
                Position after
        ) {

            List<Move> moves =
                    game.history().moves();

            if (moves.isEmpty()) {

                lastMove = null;

            } else {

                lastMove = moves.get(
                        moves.size() - 1
                );
            }

            refreshView();
        }


        @Override
        public void gameEnded(GameResult result) {

            refreshView();
        }
    };


    // =========================================
    // 생성자
    // =========================================

    public LocalGameScreen(Services services) {

        Objects.requireNonNull(services);

        this.rules = Objects.requireNonNull(
                services.standardRules()
        );

        // 표준 체스의 초기 상태로 대국 생성
        this.game = new Game(
                Position.initial(),
                rules
        );

        setTitle(
                "로컬 대국",
                "두 사람이 한 기기에서 번갈아 체스를 플레이합니다."
        );

        // 오른쪽 상단 표시
        setTopRight(
                createGameBadge()
        );

        refreshView();

        // 하위 화면의 필드와 대국 객체가 초기화된 후
        // 오른쪽 정보 패널과 하단 조작 버튼을 생성한다.
        initializePanels();
    }


    // =========================================
    // 화면 이름
    // =========================================

    @Override
    public String cardName() {

        return "local-game";
    }


    // =========================================
    // 오른쪽 상단 대국 표시
    // =========================================

    private JLabel createGameBadge() {

        JLabel badge = new JLabel(
                "LOCAL GAME  ·  2 PLAYERS"
        );

        badge.setFont(
                Theme.body(Theme.FONT_12, 600)
        );

        badge.setForeground(
                Theme.ACCENT
        );

        badge.setOpaque(true);

        badge.setBackground(
                Theme.PANEL
        );

        badge.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Theme.LINE
                        ),
                        new EmptyBorder(7, 11, 7, 11)
                )
        );

        return badge;
    }


    // =========================================
    // 오른쪽 대국 정보 패널
    // =========================================

    @Override
    protected JComponent createSidePanel() {

        JPanel panel = new JPanel();

        panel.setLayout(
                new BoxLayout(
                        panel,
                        BoxLayout.Y_AXIS
                )
        );

        panel.setBackground(
                Theme.PANEL
        );

        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Theme.LINE
                        ),
                        new EmptyBorder(20, 18, 20, 18)
                )
        );


        // 제목
        JLabel heading = new JLabel("대국 정보");

        heading.setFont(
                Theme.title(Theme.FONT_21)
        );

        heading.setForeground(
                Theme.TEXT
        );

        heading.setAlignmentX(LEFT_ALIGNMENT);


        // 구분선
        JPanel divider = new JPanel();

        divider.setBackground(
                Theme.LINE
        );

        divider.setPreferredSize(
                new Dimension(1, 1)
        );

        divider.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        1
                )
        );

        divider.setAlignmentX(LEFT_ALIGNMENT);


        // 현재 차례
        JLabel turnHeading = new JLabel("현재 차례");

        turnHeading.setFont(
                Theme.body(Theme.FONT_12, 600)
        );

        turnHeading.setForeground(
                Theme.MUTED
        );

        turnHeading.setAlignmentX(LEFT_ALIGNMENT);


        turnLabel = new JLabel();

        turnLabel.setFont(
                Theme.title(Theme.FONT_28)
        );

        turnLabel.setForeground(
                Theme.ACCENT
        );

        turnLabel.setAlignmentX(LEFT_ALIGNMENT);


        // 진행한 수
        JLabel countHeading = new JLabel("대국 진행");

        countHeading.setFont(
                Theme.body(Theme.FONT_12, 600)
        );

        countHeading.setForeground(
                Theme.MUTED
        );

        countHeading.setAlignmentX(LEFT_ALIGNMENT);


        moveCountLabel = new JLabel();

        moveCountLabel.setFont(
                Theme.body(Theme.FONT_16, 600)
        );

        moveCountLabel.setForeground(
                Theme.TEXT
        );

        moveCountLabel.setAlignmentX(LEFT_ALIGNMENT);


        // 진행 상태
        JLabel statusHeading = new JLabel("상태");

        statusHeading.setFont(
                Theme.body(Theme.FONT_12, 600)
        );

        statusHeading.setForeground(
                Theme.MUTED
        );

        statusHeading.setAlignmentX(LEFT_ALIGNMENT);


        // 상태 문구: 여러 줄 표시 가능
        statusLabel = createInfoTextArea(
                Theme.FONT_14
        );

        statusLabel.setForeground(
                Theme.TEXT
        );


        // 대국 결과
        JLabel resultHeading = new JLabel("대국 결과");

        resultHeading.setFont(
                Theme.body(Theme.FONT_12, 600)
        );

        resultHeading.setForeground(
                Theme.MUTED
        );

        resultHeading.setAlignmentX(LEFT_ALIGNMENT);


        // 결과 문구: 여러 줄 표시 가능
        resultLabel = createInfoTextArea(
                Theme.FONT_16
        );

        resultLabel.setForeground(
                Theme.OK
        );


        // 안내 문구
        JLabel guideLabel = new JLabel(
                "<html>체스판에서 기물을 선택한 뒤<br>"
                        + "이동할 칸을 클릭하세요.</html>"
        );

        guideLabel.setFont(
                Theme.body(Theme.FONT_12, 400)
        );

        guideLabel.setForeground(
                Theme.MUTED
        );

        guideLabel.setAlignmentX(LEFT_ALIGNMENT);


        // 패널 배치
        panel.add(heading);

        panel.add(Box.createVerticalStrut(16));

        panel.add(divider);

        panel.add(Box.createVerticalStrut(22));

        panel.add(turnHeading);

        panel.add(Box.createVerticalStrut(8));

        panel.add(turnLabel);

        panel.add(Box.createVerticalStrut(25));

        panel.add(countHeading);

        panel.add(Box.createVerticalStrut(8));

        panel.add(moveCountLabel);

        panel.add(Box.createVerticalStrut(22));

        panel.add(statusHeading);

        panel.add(Box.createVerticalStrut(8));

        panel.add(statusLabel);

        panel.add(Box.createVerticalStrut(22));

        panel.add(resultHeading);

        panel.add(Box.createVerticalStrut(8));

        panel.add(resultLabel);

        panel.add(Box.createVerticalGlue());

        panel.add(guideLabel);


        refreshSidePanel();

        return panel;
    }


    // =========================================
    // 여러 줄 정보 영역 생성
    // =========================================

    /**
     * 상태 및 결과 문구를 표시하는 JTextArea를 생성한다.
     * 문구가 길어지면 자동으로 줄바꿈한다.
     */
    private JTextArea createInfoTextArea(int fontSize) {

        JTextArea area = new JTextArea(2, 18);

        area.setEditable(false);

        area.setFocusable(false);

        // 자동 줄바꿈
        area.setLineWrap(true);

        area.setWrapStyleWord(true);

        // 패널 배경과 자연스럽게 연결
        area.setOpaque(false);

        area.setBorder(null);

        area.setFont(
                Theme.body(fontSize, 600)
        );

        area.setAlignmentX(LEFT_ALIGNMENT);

        // 가로로는 패널 너비에 맞추고,
        // 세로로는 기본 두 줄 높이를 사용한다.
        Dimension preferred = area.getPreferredSize();

        area.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        preferred.height
                )
        );

        return area;
    }


    // =========================================
    // 하단 대국 조작 버튼
    // =========================================

    @Override
    protected JComponent createControls() {

        JPanel panel = new JPanel(
                new GridLayout(2, 1, 0, 8)
        );

        panel.setBackground(
                Theme.WINDOW
        );

        panel.setBorder(
                new EmptyBorder(5, 0, 0, 0)
        );


        // 첫 번째 줄: 무르기와 기권
        JPanel gameControls = new JPanel(
                new GridLayout(1, 2, 8, 0)
        );

        gameControls.setOpaque(false);


        undoButton = createSecondaryButton(
                "한 수 무르기"
        );

        resignButton = createDangerButton(
                "기권"
        );


        undoButton.addActionListener(
                e -> undoMove()
        );

        resignButton.addActionListener(
                e -> resign()
        );


        gameControls.add(undoButton);

        gameControls.add(resignButton);


        // 두 번째 줄: 새 대국
        newGameButton = createPrimaryButton(
                "새 대국 시작"
        );

        newGameButton.addActionListener(
                e -> startNewGame()
        );


        panel.add(gameControls);

        panel.add(newGameButton);


        refreshSidePanel();

        return panel;
    }


    // =========================================
    // 공통 버튼 스타일
    // =========================================

    private JButton createPrimaryButton(String text) {

        JButton button = new JButton(text);

        button.setFont(
                Theme.body(Theme.FONT_14, 600)
        );

        button.setForeground(
                new java.awt.Color(0x342316)
        );

        button.setBackground(
                Theme.ACCENT
        );

        button.setOpaque(true);

        button.setFocusPainted(false);

        button.setBorder(
                new EmptyBorder(10, 14, 10, 14)
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        return button;
    }


    private JButton createSecondaryButton(String text) {

        JButton button = new ReadableDisabledButton(text);

        button.setFont(
                Theme.body(Theme.FONT_12, 600)
        );

        button.setForeground(
                Theme.TEXT
        );

        button.setBackground(
                Theme.RAISED
        );

        button.setOpaque(true);

        button.setFocusPainted(false);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Theme.LINE
                        ),
                        new EmptyBorder(9, 8, 9, 8)
                )
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        return button;
    }


    private JButton createDangerButton(String text) {

        JButton button = new ReadableDisabledButton(text);

        button.setFont(
                Theme.body(Theme.FONT_12, 600)
        );

        button.setForeground(
                Theme.BAD
        );

        button.setBackground(
                Theme.PANEL
        );

        button.setOpaque(true);

        button.setFocusPainted(false);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Theme.BAD
                        ),
                        new EmptyBorder(9, 8, 9, 8)
                )
        );

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        return button;
    }


    // =========================================
    // 화면 진입 시 리스너 등록
    // =========================================

    @Override
    public void onShow(Route route) {

        attachListeners();

        refreshView();
    }


    // =========================================
    // 화면 이탈 시 리스너 해제
    // =========================================

    @Override
    public void onHide() {

        detachListeners();
    }


    // =========================================
    // 화면 이탈 경고
    // =========================================

    @Override
    public Optional<String> leaveWarning() {

        // 이미 종료된 대국은 경고하지 않음
        if (game.result().isPresent()) {

            return Optional.empty();
        }

        // 아직 한 수도 두지 않았다면 경고하지 않음
        if (game.history().moves().isEmpty()) {

            return Optional.empty();
        }

        return Optional.of(
                "아직 대국이 진행 중입니다.\n"
                        + "화면을 이동하면 현재 대국은 저장되지 않습니다.\n"
                        + "이동하시겠습니까?"
        );
    }


    // =========================================
    // 리스너 등록
    // =========================================

    private void attachListeners() {

        if (attached) {

            return;
        }

        board.addMoveListener(
                moveListener
        );

        game.addListener(
                gameListener
        );

        attached = true;
    }


    // =========================================
    // 리스너 해제
    // =========================================

    private void detachListeners() {

        if (!attached) {

            return;
        }

        board.removeMoveListener(
                moveListener
        );

        game.removeListener(
                gameListener
        );

        attached = false;
    }


    // =========================================
    // 체스판에서 이동한 경우
    // =========================================

    private void onMoveSelected(Move move) {

        // 게임이 종료된 경우 입력을 무시
        if (game.result().isPresent()) {

            return;
        }

        try {

            game.make(move);

        } catch (
                IllegalArgumentException
                        | IllegalStateException ex
        ) {

            JOptionPane.showMessageDialog(

                    this,

                    "해당 수를 둘 수 없습니다.",

                    "수 선택 오류",

                    JOptionPane.WARNING_MESSAGE
            );

            refreshView();
        }
    }


    // =========================================
    // 한 수 무르기
    // =========================================

    private void undoMove() {

        if (game.history().moves().isEmpty()) {

            return;
        }

        int answer = JOptionPane.showConfirmDialog(

                this,

                "마지막으로 둔 수를 되돌릴까요?",

                "한 수 무르기",

                JOptionPane.YES_NO_OPTION,

                JOptionPane.QUESTION_MESSAGE
        );

        if (answer != JOptionPane.YES_OPTION) {

            return;
        }

        try {

            // 마지막 한 수 취소
            game.undo(1);

        } catch (
                IllegalArgumentException
                        | IllegalStateException ex
        ) {

            JOptionPane.showMessageDialog(

                    this,

                    "수를 되돌릴 수 없습니다.",

                    "오류",

                    JOptionPane.WARNING_MESSAGE
            );
        }
    }


    // =========================================
    // 기권
    // =========================================

    private void resign() {

        if (game.result().isPresent()) {

            return;
        }

        Side loser = game.sideToMove();

        String sideName =
                loser == Side.WHITE
                        ? "백"
                        : "흑";


        int answer = JOptionPane.showConfirmDialog(

                this,

                sideName + "이(가) 기권하시겠습니까?",

                "기권 확인",

                JOptionPane.YES_NO_OPTION,

                JOptionPane.WARNING_MESSAGE
        );

        if (answer != JOptionPane.YES_OPTION) {

            return;
        }

        game.resign(loser);
    }


    // =========================================
    // 새 대국 시작
    // =========================================

    private void startNewGame() {

        // 진행 중인 대국이 있는지 확인
        boolean hasStarted =
                !game.history().moves().isEmpty()
                        && game.result().isEmpty();


        if (hasStarted) {

            int answer = JOptionPane.showConfirmDialog(

                    this,

                    "현재 진행 중인 대국을 종료하고\n"
                            + "새로운 대국을 시작하시겠습니까?",

                    "새 대국",

                    JOptionPane.YES_NO_OPTION,

                    JOptionPane.QUESTION_MESSAGE
            );

            if (answer != JOptionPane.YES_OPTION) {

                return;
            }
        }


        // 기존 리스너 등록 상태 기억
        boolean wasAttached = attached;

        detachListeners();


        // 새로운 초기 국면으로 Game 생성
        game = new Game(
                Position.initial(),
                rules
        );

        lastMove = null;


        // 화면이 표시 중이었다면 새 Game에 리스너 등록
        if (wasAttached) {

            attachListeners();
        }

        refreshView();
    }


    // =========================================
    // 체스판과 화면 갱신
    // =========================================

    private void refreshView() {

        Position position = game.snapshot();


        // 공통 BoardScreen 메서드로 체스판 반영
        showPosition(

                position,

                rules,

                Optional.ofNullable(lastMove)
        );


        // Game에서 관리하는 합법 수 전달
        board.setLegalMoves(
                game.legalMoves()
        );


        // 게임이 끝났으면 입력 잠금
        if (game.result().isPresent()) {

            board.setInputMode(
                    Optional.empty()
            );

        } else {

            board.setInputMode(
                    Optional.of(InputMode.MOVE)
            );
        }


        // 오른쪽 정보 갱신
        refreshSidePanel();
    }


    // =========================================
    // 오른쪽 정보 패널 갱신
    // =========================================

    private void refreshSidePanel() {

        // 패널이 아직 생성되지 않았다면 종료
        if (turnLabel == null) {

            return;
        }

        Position position = game.snapshot();

        Optional<GameResult> result =
                game.result();


        if (result.isPresent()) {

            // 대국 종료
            turnLabel.setText(
                    "대국 종료"
            );

            statusLabel.setText(
                    "모든 수 입력이 잠겼습니다."
            );

            resultLabel.setText(
                    resultText(result.get())
            );

            resultLabel.setForeground(
                    Theme.OK
            );

        } else {

            // 현재 차례
            Side side = game.sideToMove();

            String sideName =
                    side == Side.WHITE
                            ? "백 차례"
                            : "흑 차례";

            turnLabel.setText(sideName);


            // 체크 여부
            if (rules.inCheck(position)) {

                statusLabel.setText(
                        "체크 상태입니다!"
                );

                statusLabel.setForeground(
                        Theme.BAD
                );

            } else {

                statusLabel.setText(
                        "정상적으로 진행 중입니다."
                );

                statusLabel.setForeground(
                        Theme.TEXT
                );
            }


            resultLabel.setText(
                    "대국 진행 중"
            );

            resultLabel.setForeground(
                    Theme.MUTED
            );
        }


        // 진행한 수 표시
        moveCountLabel.setText(
                game.history().moves().size() + "수 진행"
        );


        // 버튼 활성화 상태와 글자색을 함께 갱신한다.
        if (undoButton != null) {

            updateButtonState(
                    undoButton,
                    "한 수 무르기",
                    !game.history().moves().isEmpty()
            );
        }

        if (resignButton != null) {

            updateButtonState(
                    resignButton,
                    "기권",
                    result.isEmpty()
            );
        }
    }


    /**
     * 버튼의 활성화 상태를 갱신한다.
     * 비활성화 글자색은 ReadableDisabledButton이 직접 그린다.
     */
    private void updateButtonState(
            JButton button,
            String text,
            boolean enabled
    ) {
        button.setText(text);
        button.setEnabled(enabled);
    }


    /**
     * Look and Feel이 비활성화 글자색을 덮어쓰지 않도록
     * 버튼의 글자를 직접 그리는 버튼이다.
     */
    private static final class ReadableDisabledButton extends JButton {

        private boolean hideTextDuringSuperPaint;

        private ReadableDisabledButton(String text) {
            super(text);
        }

        @Override
        public String getText() {
            if (hideTextDuringSuperPaint) {
                return "";
            }
            return super.getText();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            String text = super.getText();

            // 기본 UI는 배경과 테두리만 그리도록 글자를 잠시 숨긴다.
            hideTextDuringSuperPaint = true;
            super.paintComponent(graphics);
            hideTextDuringSuperPaint = false;

            if (text == null || text.isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) graphics.create();
            try {
                g2.setRenderingHint(
                        RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                );

                g2.setFont(getFont());
                g2.setColor(isEnabled()
                        ? getForeground()
                        : DISABLED_BUTTON_TEXT);

                FontMetrics metrics = g2.getFontMetrics();
                int textWidth = metrics.stringWidth(text);
                int x = (getWidth() - textWidth) / 2;
                int y = (getHeight() - metrics.getHeight()) / 2
                        + metrics.getAscent();

                g2.drawString(text, x, y);
            } finally {
                g2.dispose();
            }
        }
    }


    // =========================================
    // 대국 결과 문자열 변환
    // =========================================

    private String resultText(GameResult result) {

        String outcome;

        switch (result.outcome()) {

            case WHITE_WINS -> outcome = "백 승리";
            case BLACK_WINS -> outcome = "흑 승리";
            case DRAW -> outcome = "무승부";

            default -> outcome = "대국 종료";
        }


        String reason;

        switch (result.reason()) {

            case CHECKMATE -> reason = "체크메이트";
            case STALEMATE -> reason = "스테일메이트";
            case INSUFFICIENT_MATERIAL -> reason = "기물 부족";
            case FIFTY_MOVE -> reason = "50수 규칙";
            case REPETITION -> reason = "3회 반복";
            case RESIGNATION -> reason = "기권";

            default -> reason = "";
        }


        return outcome + " (" + reason + ")";
    }
}