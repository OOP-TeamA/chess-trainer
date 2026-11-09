package com.oopteama.chess.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;


/**
 * 체스판이 필요한 화면에서 공통으로 사용하는 기본 화면.
 * 레슨, 퍼즐, AI 대국, 기보 재생 화면 등이 상속해서 사용한다.
 */
public abstract class BoardScreen extends JPanel implements Screen {

    // 모든 BoardScreen에서 공통으로 사용하는 체스판
    protected final BoardPanel board;

    // 화면 제목과 부제목
    private final JLabel titleLabel;
    private final JLabel subtitleLabel;

    // 오른쪽 상단에 버튼 등을 배치하는 영역
    private final JPanel topRightPanel;

    // 오른쪽 영역
    private final JPanel sidePanelContainer;
    private final JPanel controlsContainer;

    // 화면의 오른쪽 영역을 한 번만 생성하기 위한 변수
    private boolean panelsInitialized = false;


    // 공통 화면 레이아웃 생성
    protected BoardScreen() {

        board = new BoardPanel();

        setLayout(new BorderLayout(20, 20));
        setBackground(Theme.WINDOW);
        setBorder(new EmptyBorder(24, 28, 24, 28));


        // 1. 상단 영역
        JPanel header = new JPanel(new BorderLayout(16, 0));
        header.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(
            new BoxLayout(titlePanel, BoxLayout.Y_AXIS)
        );
        titlePanel.setOpaque(false);

        titleLabel = new JLabel();
        titleLabel.setFont(Theme.title(Theme.FONT_28));
        titleLabel.setForeground(Theme.TEXT);

        subtitleLabel = new JLabel();
        subtitleLabel.setFont(Theme.body(Theme.FONT_14, Font.PLAIN));
        subtitleLabel.setForeground(Theme.MUTED);

        titlePanel.add(titleLabel);
        titlePanel.add(Box.createVerticalStrut(5));
        titlePanel.add(subtitleLabel);

        topRightPanel = new JPanel(
            new FlowLayout(FlowLayout.RIGHT, 0, 0)
        );
        topRightPanel.setOpaque(false);

        header.add(titlePanel, BorderLayout.CENTER);
        header.add(topRightPanel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);


        // 2. 중앙 체스판과 오른쪽 패널
        JPanel content = new JPanel(new BorderLayout(20, 0));
        content.setOpaque(false);

        board.setPreferredSize(new Dimension(600, 600));
        board.setMinimumSize(new Dimension(360, 360));

        content.add(board, BorderLayout.CENTER);


        // 3. 오른쪽 패널 영역
        JPanel rightPanel = new JPanel(new BorderLayout(0, 14));
        rightPanel.setOpaque(false);
        rightPanel.setPreferredSize(new Dimension(380, 600));

        sidePanelContainer = new JPanel(new BorderLayout());
        sidePanelContainer.setOpaque(false);

        controlsContainer = new JPanel(new BorderLayout());
        controlsContainer.setOpaque(false);

        rightPanel.add(
            sidePanelContainer,
            BorderLayout.CENTER
        );

        rightPanel.add(
            controlsContainer,
            BorderLayout.SOUTH
        );

        content.add(rightPanel, BorderLayout.EAST);

        add(content, BorderLayout.CENTER);
    }


    // 하위 화면이 오른쪽에 표시할 내용을 정의한다.
    protected abstract JComponent createSidePanel();


    // 하위 화면이 하단에 표시할 조작 버튼 등을 정의한다.
    protected abstract JComponent createControls();


    // 화면 제목과 부제목 설정
    protected void setTitle(String title, String subtitle) {

        titleLabel.setText(title == null ? "" : title);

        subtitleLabel.setText(
            subtitle == null ? "" : subtitle
        );

        subtitleLabel.setVisible(
            subtitle != null && !subtitle.isBlank()
        );
    }


    // 오른쪽 상단에 버튼이나 컴포넌트 배치
    protected void setTopRight(JComponent component) {

        topRightPanel.removeAll();

        if (component != null) {
            topRightPanel.add(component);
        }

        topRightPanel.revalidate();
        topRightPanel.repaint();
    }


    // 체스판의 현재 상태를 한 번에 갱신한다.
    // 국면, 체크 표시, 합법 수, 마지막 수 강조를 반영한다.
    protected void showPosition(
        Position position,
        RuleSet rules,
        Optional<Move> last
    ) {

        Objects.requireNonNull(position);
        Objects.requireNonNull(rules);
        Objects.requireNonNull(last);


        // 이전 국면의 표시 정보 초기화
        board.clearMarks();
        board.setArrows(List.of());


        // 현재 체스판 상태 반영
        board.setPosition(position);


        // 현재 차례인 쪽의 킹이 체크 상태인지 확인
        Optional<Square> checkSquare = rules.inCheck(position)
            ? position.kingSquare(position.sideToMove())
            : Optional.empty();

        board.setCheckSquare(checkSquare);


        // 현재 국면에서 둘 수 있는 합법 수 전달
        board.setLegalMoves(rules.legalMoves(position));


        // 마지막으로 둔 수 강조
        last.ifPresent(move ->
            board.mark(
                List.of(move.from(), move.to()),
                BoardPanel.Mark.LAST_MOVE
            )
        );
    }


    // 화면이 실제로 컨테이너에 추가될 때 하위 화면의 패널을 생성한다.
    @Override
    public void addNotify() {

        super.addNotify();

        initializePanels();
    }


    // 오른쪽 패널을 최초 한 번만 생성한다.
    private void initializePanels() {

        if (panelsInitialized) {
            return;
        }


        JComponent sidePanel = Objects.requireNonNull(
            createSidePanel(),
            "createSidePanel() must not return null"
        );

        JComponent controls = Objects.requireNonNull(
            createControls(),
            "createControls() must not return null"
        );


        // 오른쪽 패널 배치
        sidePanelContainer.add(
            sidePanel,
            BorderLayout.CENTER
        );

        controlsContainer.add(
            controls,
            BorderLayout.CENTER
        );


        panelsInitialized = true;

        revalidate();
        repaint();
    }


    // Screen 인터페이스에 현재 화면 컴포넌트를 반환한다.
    @Override
    public final JComponent component() {

        return this;
    }
}
