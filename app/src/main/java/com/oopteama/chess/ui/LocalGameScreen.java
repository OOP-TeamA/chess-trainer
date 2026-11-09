package com.oopteama.chess.ui;

import com.oopteama.chess.content.goal.InputMode;
import com.oopteama.chess.core.GameResult;
import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.rules.RuleSet;
import com.oopteama.chess.game.Game;
import com.oopteama.chess.game.GameListener;
import com.oopteama.chess.nav.Route;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import java.awt.FlowLayout;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;


public final class LocalGameScreen extends BoardScreen {

    private final RuleSet rules;

    private Game game;

    private Move lastMove;

    private boolean attached;


    // 오른쪽 정보 패널에서 사용하는 라벨
    private JLabel turnLabel;
    private JLabel moveCountLabel;
    private JLabel statusLabel;
    private JLabel resultLabel;


    // 하단 조작 버튼
    private JButton undoButton;
    private JButton resignButton;
    private JButton newGameButton;


    // 보드에서 수를 선택했을 때 실행할 리스너
    private final Consumer<Move> moveListener =
        this::onMoveSelected;


    // Game에서 상태가 변경되었을 때 실행할 리스너
    private final GameListener gameListener = new GameListener() {

        @Override
        public void moveMade(Move move, Position after) {

            lastMove = move;

            refreshView();
        }


        @Override
        public void moveUndone(Move move, Position after) {

            List<Move> moves = game.history().moves();

            if (moves.isEmpty()) {
                lastMove = null;
            } else {
                lastMove = moves.get(moves.size() - 1);
            }

            refreshView();
        }


        @Override
        public void gameEnded(GameResult result) {

            refreshView();
        }
    };


    // 생성자
    public LocalGameScreen(Services services) {

        Objects.requireNonNull(services);

        this.rules = Objects.requireNonNull(
            services.standardRules()
        );

        this.game = new Game(Position.initial(), rules);


        setTitle(
            "로컬 대국",
            "두 사람이 한 기기에서 번갈아 체스를 플레이합니다."
        );

        JLabel badge = new JLabel("STANDARD CHESS");
        badge.setFont(Theme.body(Theme.FONT_12, 1));
        badge.setForeground(Theme.ACCENT);

        setTopRight(badge);

        refreshView();
    }


    // 화면 이름
    @Override
    public String cardName() {

        return "local-game";
    }


    // 오른쪽 정보 패널
    @Override
    protected JComponent createSidePanel() {

        JPanel panel = new JPanel();

        panel.setLayout(
            new BoxLayout(panel, BoxLayout.Y_AXIS)
        );

        panel.setBackground(Theme.PANEL);

        panel.setBorder(
            BorderFactory.createEmptyBorder(
                20, 18, 20, 18
            )
        );


        JLabel heading = new JLabel("대국 정보");

        heading.setFont(Theme.title(Theme.FONT_21));
        heading.setForeground(Theme.TEXT);


        turnLabel = new JLabel();
        moveCountLabel = new JLabel();
        statusLabel = new JLabel();
        resultLabel = new JLabel();


        turnLabel.setFont(Theme.body(Theme.FONT_16, 1));
        moveCountLabel.setFont(Theme.body(Theme.FONT_14, 0));
        statusLabel.setFont(Theme.body(Theme.FONT_14, 0));
        resultLabel.setFont(Theme.body(Theme.FONT_14, 1));


        turnLabel.setForeground(Theme.ACCENT);
        moveCountLabel.setForeground(Theme.MUTED);
        statusLabel.setForeground(Theme.TEXT);
        resultLabel.setForeground(Theme.OK);


        panel.add(heading);
        panel.add(Box.createVerticalStrut(25));

        panel.add(turnLabel);
        panel.add(Box.createVerticalStrut(15));

        panel.add(moveCountLabel);
        panel.add(Box.createVerticalStrut(15));

        panel.add(statusLabel);
        panel.add(Box.createVerticalStrut(25));

        panel.add(resultLabel);

        panel.add(Box.createVerticalGlue());


        refreshSidePanel();

        return panel;
    }


    // 하단 조작 버튼
    @Override
    protected JComponent createControls() {

        JPanel panel = new JPanel(
            new FlowLayout(
                FlowLayout.CENTER,
                10,
                10
            )
        );

        panel.setBackground(Theme.WINDOW);


        undoButton = new JButton("한 수 무르기");
        resignButton = new JButton("기권");
        newGameButton = new JButton("새 대국");


        undoButton.setFont(Theme.body(Theme.FONT_14, 1));
        resignButton.setFont(Theme.body(Theme.FONT_14, 1));
        newGameButton.setFont(Theme.body(Theme.FONT_14, 1));


        undoButton.addActionListener(
            e -> undoMove()
        );

        resignButton.addActionListener(
            e -> resign()
        );

        newGameButton.addActionListener(
            e -> startNewGame()
        );


        panel.add(undoButton);
        panel.add(resignButton);
        panel.add(newGameButton);


        refreshSidePanel();

        return panel;
    }


    // 화면에 들어왔을 때 리스너 등록
    @Override
    public void onShow(Route route) {

        attachListeners();

        refreshView();
    }


    // 화면에서 나갈 때 리스너 해제
    @Override
    public void onHide() {

        detachListeners();
    }


    // 대국 중 화면을 나가려고 할 때 경고
    @Override
    public Optional<String> leaveWarning() {

        // 이미 종료된 대국이면 경고하지 않음
        if (game.result().isPresent()) {
            return Optional.empty();
        }

        return Optional.of(
            "아직 대국이 진행 중입니다.\n"
                + "화면을 나가면 현재 대국을 중단하게 됩니다."
        );
    }


    // 리스너 등록
    private void attachListeners() {

        if (attached) {
            return;
        }

        board.addMoveListener(moveListener);

        game.addListener(gameListener);

        attached = true;
    }


    // 리스너 해제
    private void detachListeners() {

        if (!attached) {
            return;
        }

        board.removeMoveListener(moveListener);

        game.removeListener(gameListener);

        attached = false;
    }


    // 보드에서 수를 선택했을 때 호출
    private void onMoveSelected(Move move) {

        if (game.result().isPresent()) {
            return;
        }

        try {

            game.make(move);

        } catch (
            IllegalArgumentException | IllegalStateException ex
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


    // 한 수 무르기
    private void undoMove() {

        if (game.history().moves().isEmpty()) {
            return;
        }


        int answer = JOptionPane.showConfirmDialog(
            this,
            "마지막으로 둔 수를 되돌릴까요?",
            "한 수 무르기",
            JOptionPane.YES_NO_OPTION
        );


        if (answer != JOptionPane.YES_OPTION) {
            return;
        }


        try {

            game.undo(1);

        } catch (
            IllegalArgumentException | IllegalStateException ex
        ) {

            JOptionPane.showMessageDialog(
                this,
                "수를 되돌릴 수 없습니다.",
                "무르기 오류",
                JOptionPane.WARNING_MESSAGE
            );

            refreshView();
        }
    }


    // 기권
    private void resign() {

        if (game.result().isPresent()) {
            return;
        }


        Side loser = game.sideToMove();

        String sideName =
            loser == Side.WHITE ? "백" : "흑";


        int answer = JOptionPane.showConfirmDialog(
            this,
            sideName + "이(가) 기권하시겠습니까?",
            "기권 확인",
            JOptionPane.YES_NO_OPTION
        );


        if (answer != JOptionPane.YES_OPTION) {
            return;
        }


        game.resign(loser);
    }


    // 새 대국 시작 전 확인
    private void startNewGame() {

        boolean hasMoves =
            !game.history().moves().isEmpty();

        boolean isPlaying =
            game.result().isEmpty();


        // 진행 중인 대국에 수가 있다면 확인
        if (hasMoves && isPlaying) {

            int answer = JOptionPane.showConfirmDialog(
                this,
                "현재 대국을 종료하고 새 대국을 시작할까요?",
                "새 대국",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
            );

            if (answer != JOptionPane.YES_OPTION) {
                return;
            }
        }


        restartGame();
    }


    // 새로운 대국 시작
    private void restartGame() {

        // 기존 리스너 해제
        detachListeners();


        // 새로운 대국 생성
        game = new Game(
            Position.initial(),
            rules
        );

        lastMove = null;


        // 화면에 표시된 경우 리스너 다시 등록
        attachListeners();


        refreshView();
    }


    // 보드와 화면 정보 갱신
    private void refreshView() {

        Position position = game.snapshot();


        showPosition(
            position,
            rules,
            Optional.ofNullable(lastMove)
        );


        // 실제 게임의 합법 수 갱신
        // 종료된 대국이면 빈 목록이 전달됨
        board.setLegalMoves(game.legalMoves());


        // 대국 종료 여부에 따라 입력 잠금
        if (game.result().isPresent()) {

            board.setInputMode(Optional.empty());

        } else {

            board.setInputMode(
                Optional.of(InputMode.MOVE)
            );
        }


        refreshSidePanel();
    }


    // 오른쪽 정보 패널 갱신
    private void refreshSidePanel() {

        // 패널이 아직 생성되지 않았다면 종료
        if (
            turnLabel == null ||
            moveCountLabel == null ||
            statusLabel == null ||
            resultLabel == null
        ) {
            return;
        }


        Position position = game.snapshot();

        Optional<GameResult> result = game.result();


        // 대국 종료 상태
        if (result.isPresent()) {

            turnLabel.setText("대국 종료");

            statusLabel.setText(
                "모든 수 입력이 잠겼습니다."
            );

            resultLabel.setText(
                resultText(result.get())
            );

        } else {

            // 현재 차례
            Side side = game.sideToMove();

            String sideName =
                side == Side.WHITE ? "백 차례" : "흑 차례";

            turnLabel.setText(sideName);


            // 체크 상태 확인
            if (rules.inCheck(position)) {

                statusLabel.setText("체크 상태입니다!");

            } else {

                statusLabel.setText(
                    "정상적으로 진행 중입니다."
                );
            }

            resultLabel.setText("");
        }


        // 진행한 수 표시
        moveCountLabel.setText(
            "진행한 수: "
                + game.history().moves().size()
                + "수"
        );


        // 무르기 버튼 활성화
        if (undoButton != null) {

            undoButton.setEnabled(
                !game.history().moves().isEmpty()
            );
        }


        // 기권 버튼 활성화
        if (resignButton != null) {

            resignButton.setEnabled(
                game.result().isEmpty()
            );
        }


        // 새 대국 버튼은 항상 활성화
        if (newGameButton != null) {

            newGameButton.setEnabled(true);
        }
    }


    // 경기 결과를 문자열로 변환
    private String resultText(GameResult result) {

        String outcome = switch (result.outcome()) {

            case WHITE_WINS -> "백 승리";

            case BLACK_WINS -> "흑 승리";

            case DRAW -> "무승부";
        };


        String reason = switch (result.reason()) {

            case CHECKMATE -> "체크메이트";

            case STALEMATE -> "스테일메이트";

            case INSUFFICIENT_MATERIAL -> "기물 부족";

            case FIFTY_MOVE -> "50수 규칙";

            case REPETITION -> "3회 반복";

            case RESIGNATION -> "기권";
        };


        return outcome + " (" + reason + ")";
    }
}
