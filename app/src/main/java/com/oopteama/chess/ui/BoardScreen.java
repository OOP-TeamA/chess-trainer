package com.oopteama.chess.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Square;
import com.oopteama.chess.core.rules.RuleSet;


/**
 * 체스판이 필요한 화면에서 공통으로 사용하는 기본 화면.
 *
 * 레슨, 퍼즐, AI 대국, 기보 재생 화면 등이
 * 이 클래스를 상속하여 공통 레이아웃을 사용한다.
 *
 * 화면 구성:
 * 1. 상단 제목 및 부제목
 * 2. 왼쪽 체스판
 * 3. 오른쪽 정보 패널
 * 4. 오른쪽 하단 조작 버튼
 */
public abstract class BoardScreen extends JPanel implements Screen {

    // =========================================
    // 공통 체스판
    // =========================================

    protected final BoardPanel board;


    // =========================================
    // 상단 제목 영역
    // =========================================

    private final JLabel titleLabel;

    private final JLabel subtitleLabel;

    private final JPanel topRightPanel;


    // =========================================
    // 오른쪽 패널 영역
    // =========================================

    private final JPanel sidePanelContainer;

    private final JPanel controlsContainer;


    // 오른쪽 패널 최초 생성 여부
    private boolean panelsInitialized = false;


    // =========================================
    // 생성자
    // =========================================

    protected BoardScreen() {

        board = new BoardPanel();

        setLayout(
                new BorderLayout(18, 18)
        );

        setBackground(Theme.WINDOW);

        setOpaque(true);

        setBorder(
                new EmptyBorder(22, 25, 22, 25)
        );


        // =====================================
        // 1. 상단 제목 영역
        // =====================================

        JPanel header = new JPanel(
                new BorderLayout(15, 0)
        );

        header.setOpaque(false);

        header.setBorder(
                BorderFactory.createCompoundBorder(
                        new MatteBorder(
                                0, 0, 1, 0,
                                Theme.LINE
                        ),
                        new EmptyBorder(0, 0, 14, 0)
                )
        );


        // 제목과 부제목
        JPanel titlePanel = new JPanel();

        titlePanel.setLayout(
                new BoxLayout(
                        titlePanel,
                        BoxLayout.Y_AXIS
                )
        );

        titlePanel.setOpaque(false);


        titleLabel = new JLabel();

        titleLabel.setFont(
                Theme.title(Theme.FONT_28)
        );

        titleLabel.setForeground(
                Theme.TEXT
        );


        subtitleLabel = new JLabel();

        subtitleLabel.setFont(
                Theme.body(Theme.FONT_14, Font.PLAIN)
        );

        subtitleLabel.setForeground(
                Theme.MUTED
        );


        titlePanel.add(titleLabel);

        titlePanel.add(
                Box.createVerticalStrut(5)
        );

        titlePanel.add(subtitleLabel);


        // 오른쪽 상단 버튼 영역
        topRightPanel = new JPanel(
                new FlowLayout(
                        FlowLayout.RIGHT,
                        8,
                        2
                )
        );

        topRightPanel.setOpaque(false);


        header.add(
                titlePanel,
                BorderLayout.CENTER
        );

        header.add(
                topRightPanel,
                BorderLayout.EAST
        );


        add(
                header,
                BorderLayout.NORTH
        );


        // =====================================
        // 2. 중앙 콘텐츠 영역
        // =====================================

        JPanel content = new JPanel(
                new BorderLayout(18, 0)
        );

        content.setOpaque(false);


        // =====================================
        // 3. 왼쪽 체스판 영역
        // =====================================

        JPanel boardFrame = new JPanel(
                new BorderLayout()
        );

        boardFrame.setBackground(
                Theme.PANEL
        );

        boardFrame.setOpaque(true);

        boardFrame.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Theme.LINE
                        ),
                        new EmptyBorder(8, 8, 8, 8)
                )
        );


        board.setPreferredSize(
                new Dimension(560, 560)
        );

        board.setMinimumSize(
                new Dimension(320, 320)
        );


        boardFrame.add(
                board,
                BorderLayout.CENTER
        );


        content.add(
                boardFrame,
                BorderLayout.CENTER
        );


        // =====================================
        // 4. 오른쪽 정보 및 버튼 영역
        // =====================================

        JPanel rightPanel = new JPanel(
                new BorderLayout(0, 14)
        );

        rightPanel.setOpaque(false);

        rightPanel.setPreferredSize(
                new Dimension(330, 560)
        );

        rightPanel.setMinimumSize(
                new Dimension(270, 300)
        );


        // 오른쪽 정보 패널
        sidePanelContainer = new JPanel(
                new BorderLayout()
        );

        sidePanelContainer.setOpaque(false);


        // 하단 조작 버튼
        controlsContainer = new JPanel(
                new BorderLayout()
        );

        controlsContainer.setOpaque(false);


        rightPanel.add(
                sidePanelContainer,
                BorderLayout.CENTER
        );

        rightPanel.add(
                controlsContainer,
                BorderLayout.SOUTH
        );


        content.add(
                rightPanel,
                BorderLayout.EAST
        );


        add(
                content,
                BorderLayout.CENTER
        );
    }


    // =========================================
    // 하위 화면에서 구현할 메서드
    // =========================================

    /**
     * 오른쪽 정보 패널을 생성한다.
     */
    protected abstract JComponent createSidePanel();


    /**
     * 하단 조작 버튼을 생성한다.
     */
    protected abstract JComponent createControls();


    // =========================================
    // 제목 및 부제목 설정
    // =========================================

    protected void setTitle(
            String title,
            String subtitle
    ) {

        titleLabel.setText(
                title == null ? "" : title
        );

        subtitleLabel.setText(
                subtitle == null ? "" : subtitle
        );

        subtitleLabel.setVisible(
                subtitle != null && !subtitle.isBlank()
        );
    }


    // =========================================
    // 오른쪽 상단 컴포넌트 설정
    // =========================================

    protected void setTopRight(
            JComponent component
    ) {

        topRightPanel.removeAll();

        if (component != null) {

            topRightPanel.add(component);
        }

        topRightPanel.revalidate();

        topRightPanel.repaint();
    }


    // =========================================
    // 체스판 상태 갱신
    // =========================================

    /**
     * 현재 국면을 체스판에 반영한다.
     *
     * 1. 이전 표시 정보 초기화
     * 2. 체스판 국면 갱신
     * 3. 체크 상태 확인
     * 4. 합법 수 전달
     * 5. 마지막 수 강조
     */
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


        // 현재 국면 설정
        board.setPosition(position);


        // 체크 상태 확인
        Optional<Square> checkSquare =
                rules.inCheck(position)
                        ? position.kingSquare(
                                position.sideToMove()
                        )
                        : Optional.empty();


        board.setCheckSquare(checkSquare);


        // 현재 국면의 합법 수 전달
        board.setLegalMoves(
                rules.legalMoves(position)
        );


        // 마지막 수 강조
        last.ifPresent(move ->

                board.mark(
                        List.of(
                                move.from(),
                                move.to()
                        ),
                        BoardPanel.Mark.LAST_MOVE
                )
        );
    }


    // =========================================
    // 화면 추가 시 패널 초기화
    // =========================================

    @Override
    public void addNotify() {

        super.addNotify();

        initializePanels();
    }


    // =========================================
    // 오른쪽 패널 최초 한 번만 생성
    // =========================================

    private void initializePanels() {

        if (panelsInitialized) {

            return;
        }


        // 하위 클래스에서 정보 패널 생성
        JComponent sidePanel = Objects.requireNonNull(
                createSidePanel(),
                "createSidePanel() must not return null"
        );


        // 하위 클래스에서 조작 버튼 생성
        JComponent controls = Objects.requireNonNull(
                createControls(),
                "createControls() must not return null"
        );


        // 오른쪽 정보 패널 배치
        sidePanelContainer.add(
                sidePanel,
                BorderLayout.CENTER
        );


        // 하단 조작 버튼 배치
        controlsContainer.add(
                controls,
                BorderLayout.CENTER
        );


        panelsInitialized = true;

        revalidate();

        repaint();
    }


    // =========================================
    // Screen 인터페이스 구현
    // =========================================

    @Override
    public final JComponent component() {

        return this;
    }
}