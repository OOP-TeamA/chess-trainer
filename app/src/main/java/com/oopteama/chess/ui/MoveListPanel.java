package com.oopteama.chess.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.oopteama.chess.core.Side;


/**
 * 체스 이동 기록을 표시하는 공통 패널.
 *
 * 기보 재생, AI 대전, 맞히기 모드에서 공용으로 사용한다.
 *
 * 주요 기능
 * 1. 백과 흑의 이동 기록 표시
 * 2. 특정 수 선택
 * 3. 현재 선택된 수 강조 및 자동 스크롤
 * 4. 정답, 오답, 실수 표시
 * 5. 맞히기 모드의 미완료 수 표시
 */
public class MoveListPanel extends JPanel {

    // =========================================
    // 수의 정답 및 오답 표시 상태
    // =========================================

    public enum Badge {
        NONE,
        CORRECT,
        WRONG,
        BLUNDER
    }


    // =========================================
    // 내부 데이터
    // =========================================

    private List<String> sanMoves = List.of();

    private int firstFullmove = 1;

    private Side firstSide = Side.WHITE;

    // 현재 선택된 수
    // 0이면 선택 없음
    // 1부터 실제 수의 순서
    private int currentPly = 0;

    // 맞히기 모드의 물음표 표시 여부
    private boolean pendingQuestion = false;

    // 각 수의 정답 및 오답 표시
    private final Map<Integer, Badge> badges =
            new HashMap<>();

    // 수를 선택했을 때 실행할 리스너
    private final List<IntConsumer> plyListeners =
            new ArrayList<>();

    // 실제 수 버튼 관리
    private final Map<Integer, JButton> moveButtons =
            new HashMap<>();

    // 이동 기록이 표시되는 영역
    private final JPanel movesPanel;

    // 이동 기록 스크롤
    private final JScrollPane scrollPane;


    // =========================================
    // 생성자
    // =========================================

    public MoveListPanel() {

        setLayout(
                new BorderLayout(0, 12)
        );

        setBackground(Theme.PANEL);

        setOpaque(true);

        setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                Theme.LINE
                        ),
                        new EmptyBorder(15, 14, 15, 14)
                )
        );

        setPreferredSize(
                new Dimension(320, 350)
        );


        // =====================================
        // 1. 상단 제목
        // =====================================

        JPanel header = new JPanel(
                new BorderLayout()
        );

        header.setOpaque(false);

        JLabel title = new JLabel("수순 목록");

        title.setFont(
                Theme.title(Theme.FONT_21)
        );

        title.setForeground(
                Theme.TEXT
        );

        header.add(
                title,
                BorderLayout.WEST
        );


        add(
                header,
                BorderLayout.NORTH
        );


        // =====================================
        // 2. 이동 기록 영역
        // =====================================

        movesPanel = new JPanel();

        movesPanel.setLayout(
                new BoxLayout(
                        movesPanel,
                        BoxLayout.Y_AXIS
                )
        );

        movesPanel.setBackground(
                Theme.PANEL
        );

        movesPanel.setOpaque(true);


        // 스크롤 영역
        scrollPane = new JScrollPane(
                movesPanel
        );

        scrollPane.setBorder(null);

        scrollPane.setBackground(
                Theme.PANEL
        );

        scrollPane.getViewport().setBackground(
                Theme.PANEL
        );

        scrollPane.setHorizontalScrollBarPolicy(
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );

        scrollPane.setVerticalScrollBarPolicy(
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        );


        add(
                scrollPane,
                BorderLayout.CENTER
        );


        // 최초 안내 문구
        rebuildRows();
    }


    // =========================================
    // 이동 기록 설정
    // =========================================

    /**
     * 체스 이동 기록을 설정한다.
     *
     * @param san SAN 방식으로 표현한 이동 기록
     * @param firstFullmove 첫 번째 이동의 대국 번호
     * @param firstSide 첫 번째 이동을 하는 플레이어
     */
    public void setMoves(
            List<String> san,
            int firstFullmove,
            Side firstSide
    ) {

        Objects.requireNonNull(san);
        Objects.requireNonNull(firstSide);

        if (firstFullmove < 1) {

            throw new IllegalArgumentException(
                    "firstFullmove must be at least 1."
            );
        }


        List<String> copiedMoves = new ArrayList<>();

        for (String move : san) {

            if (move == null || move.isBlank()) {

                throw new IllegalArgumentException(
                        "SAN move cannot be null or blank."
                );
            }

            copiedMoves.add(move.trim());
        }


        this.sanMoves = List.copyOf(copiedMoves);

        this.firstFullmove = firstFullmove;

        this.firstSide = firstSide;


        // 새로운 기보가 설정되면 이전 상태 초기화
        currentPly = 0;

        badges.clear();

        rebuildRows();
    }


    // =========================================
    // 현재 선택된 수 설정
    // =========================================

    /**
     * 현재 선택된 수를 설정한다.
     *
     * 0은 선택 없음.
     * 1부터 실제 이동 순서를 의미한다.
     *
     * 선택된 수가 화면 밖에 있으면 자동 스크롤한다.
     */
    public void setCurrentPly(int ply) {

        if (ply < 0 || ply > sanMoves.size()) {

            throw new IllegalArgumentException(
                    "Invalid ply: " + ply
            );
        }

        currentPly = ply;

        updateButtonStyles();


        // 0이면 선택된 수가 없으므로 스크롤하지 않는다.
        if (ply == 0) {

            return;
        }


        JButton button = moveButtons.get(ply);

        if (button == null) {

            return;
        }


        SwingUtilities.invokeLater(() -> {

            if (!button.isShowing()) {

                return;
            }

            Rectangle bounds =
                    SwingUtilities.convertRectangle(
                            button.getParent(),
                            button.getBounds(),
                            movesPanel
                    );

            movesPanel.scrollRectToVisible(bounds);
        });
    }


    // =========================================
    // 정답 및 오답 표시
    // =========================================

    /**
     * 특정 수에 정답 또는 오답 표시를 설정한다.
     */
    public void setBadge(
            int ply,
            Badge badge
    ) {

        Objects.requireNonNull(badge);

        if (ply < 1 || ply > sanMoves.size()) {

            throw new IllegalArgumentException(
                    "Invalid ply: " + ply
            );
        }


        if (badge == Badge.NONE) {

            badges.remove(ply);

        } else {

            badges.put(ply, badge);
        }


        JButton button = moveButtons.get(ply);

        if (button != null) {

            updateMoveButton(button, ply);
        }
    }


    // =========================================
    // 맞히기 모드의 물음표 설정
    // =========================================

    /**
     * 맞히기 모드에서 아직 답하지 않은 수를 ?로 표시한다.
     */
    public void setPendingQuestion(boolean pending) {

        this.pendingQuestion = pending;

        rebuildRows();
    }


    // =========================================
    // 수 선택 리스너 등록
    // =========================================

    /**
     * 이동 기록에서 특정 수를 선택하면 전달받은
     * IntConsumer가 해당 ply 번호와 함께 호출된다.
     */
    public void addPlyListener(
            IntConsumer onSelect
    ) {

        Objects.requireNonNull(onSelect);

        // 동일한 객체는 중복 등록하지 않는다.
        for (IntConsumer listener : plyListeners) {

            if (listener == onSelect) {

                return;
            }
        }

        plyListeners.add(onSelect);
    }


    // =========================================
    // 이동 기록 화면 다시 만들기
    // =========================================

    private void rebuildRows() {

        movesPanel.removeAll();

        moveButtons.clear();


        // 이동 기록이 없는 경우
        if (sanMoves.isEmpty() && !pendingQuestion) {

            JLabel emptyLabel = new JLabel(
                    "아직 표시할 수순이 없습니다."
            );

            emptyLabel.setFont(
                    Theme.body(Theme.FONT_12, 0)
            );

            emptyLabel.setForeground(
                    Theme.MUTED
            );

            emptyLabel.setAlignmentX(
                    CENTER_ALIGNMENT
            );

            movesPanel.add(
                    Box.createVerticalStrut(20)
            );

            movesPanel.add(emptyLabel);

            finishRebuild();

            return;
        }


        // =====================================
        // 이동 기록 제목 행
        // =====================================

        JPanel columnHeader = new JPanel(
                new GridLayout(1, 3, 6, 0)
        );

        columnHeader.setOpaque(false);

        JLabel numberHeader = createColumnLabel(
                "번호"
        );

        JLabel whiteHeader = createColumnLabel(
                "백"
        );

        JLabel blackHeader = createColumnLabel(
                "흑"
        );

        columnHeader.add(numberHeader);

        columnHeader.add(whiteHeader);

        columnHeader.add(blackHeader);

        columnHeader.setMaximumSize(
                new Dimension(
                        Integer.MAX_VALUE,
                        32
                )
        );

        movesPanel.add(columnHeader);

        movesPanel.add(
                Box.createVerticalStrut(7)
        );


        // =====================================
        // 필요한 행 개수 계산
        // =====================================

        int rowCount = 0;

        for (int i = 0; i < sanMoves.size(); i++) {

            int rowIndex = rowIndexFor(i);

            rowCount = Math.max(
                    rowCount,
                    rowIndex + 1
            );
        }


        // 물음표가 들어갈 행도 포함
        if (pendingQuestion) {

            int rowIndex = rowIndexFor(
                    sanMoves.size()
            );

            rowCount = Math.max(
                    rowCount,
                    rowIndex + 1
            );
        }


        // 행 객체 생성
        List<MoveRow> rows = new ArrayList<>();

        for (int i = 0; i < rowCount; i++) {

            MoveRow row = new MoveRow(
                    firstFullmove + i
            );

            rows.add(row);
        }


        // =====================================
        // 실제 이동 기록 배치
        // =====================================

        for (int i = 0; i < sanMoves.size(); i++) {

            int ply = i + 1;

            String san = sanMoves.get(i);

            int rowIndex = rowIndexFor(i);

            MoveRow row = rows.get(rowIndex);


            JButton moveButton = createMoveButton(
                    ply,
                    san
            );


            moveButtons.put(
                    ply,
                    moveButton
            );


            // 어느 쪽의 수인지 확인
            if (isWhitePly(i)) {

                row.setWhiteMove(moveButton);

            } else {

                row.setBlackMove(moveButton);
            }


            updateMoveButton(
                    moveButton,
                    ply
            );
        }


        // =====================================
        // 맞히기 모드의 ? 표시
        // =====================================

        if (pendingQuestion) {

            int questionIndex = sanMoves.size();

            int rowIndex = rowIndexFor(
                    questionIndex
            );

            MoveRow row = rows.get(rowIndex);

            JLabel questionLabel = new JLabel(
                    "?",
                    SwingConstants.CENTER
            );

            questionLabel.setFont(
                    Theme.title(Theme.FONT_21)
            );

            questionLabel.setForeground(
                    Theme.ACCENT
            );

            questionLabel.setOpaque(true);

            questionLabel.setBackground(
                    Theme.RAISED
            );

            questionLabel.setBorder(
                    BorderFactory.createLineBorder(
                            Theme.ACCENT
                    )
            );

            if (isWhitePly(questionIndex)) {

                row.setWhiteMove(questionLabel);

            } else {

                row.setBlackMove(questionLabel);
            }
        }


        // =====================================
        // 모든 행 화면에 추가
        // =====================================

        for (MoveRow row : rows) {

            movesPanel.add(row);

            movesPanel.add(
                    Box.createVerticalStrut(5)
            );
        }


        updateButtonStyles();

        finishRebuild();
    }


    // =========================================
    // 화면 갱신 마무리
    // =========================================

    private void finishRebuild() {

        movesPanel.revalidate();

        movesPanel.repaint();

        revalidate();

        repaint();


        // 현재 선택된 수가 있으면 자동 스크롤
        if (currentPly > 0) {

            setCurrentPly(currentPly);
        }
    }


    // =========================================
    // 이동 번호에 해당하는 행 계산
    // =========================================

    private int rowIndexFor(int zeroBasedIndex) {

        int offset = zeroBasedIndex;

        // 첫 수가 흑의 수라면 흰 칸을 건너뛰고 시작
        if (firstSide == Side.BLACK) {

            offset++;
        }

        return offset / 2;
    }


    // =========================================
    // 현재 수를 두는 플레이어 확인
    // =========================================

    private boolean isWhitePly(int zeroBasedIndex) {

        boolean isFirstPlayer = zeroBasedIndex % 2 == 0;

        return isFirstPlayer
                == (firstSide == Side.WHITE);
    }


    // =========================================
    // 열 제목 생성
    // =========================================

    private JLabel createColumnLabel(String text) {

        JLabel label = new JLabel(
                text,
                SwingConstants.CENTER
        );

        label.setFont(
                Theme.body(Theme.FONT_12, 1)
        );

        label.setForeground(
                Theme.MUTED
        );

        return label;
    }


    // =========================================
    // 이동 기록 버튼 생성
    // =========================================

    private JButton createMoveButton(
            int ply,
            String san
    ) {

        JButton button = new JButton(san);

        button.setFont(
                Theme.body(Theme.FONT_14, Font.BOLD)
        );

        button.setHorizontalAlignment(
                SwingConstants.CENTER
        );

        button.setFocusPainted(false);

        button.setOpaque(true);

        button.setCursor(
                Cursor.getPredefinedCursor(
                        Cursor.HAND_CURSOR
                )
        );

        button.setBorder(
                BorderFactory.createEmptyBorder(
                        6, 5, 6, 5
                )
        );


        button.addActionListener(e -> {

            setCurrentPly(ply);

            // 등록된 모든 선택 리스너에 ply 전달
            for (
                    IntConsumer listener :
                    List.copyOf(plyListeners)
            ) {

                listener.accept(ply);
            }
        });


        return button;
    }


    // =========================================
    // 이동 버튼 스타일 갱신
    // =========================================

    private void updateButtonStyles() {

        for (
                Map.Entry<Integer, JButton> entry :
                moveButtons.entrySet()
        ) {

            updateMoveButton(
                    entry.getValue(),
                    entry.getKey()
            );
        }
    }


    // =========================================
    // 개별 이동 버튼 스타일 갱신
    // =========================================

    private void updateMoveButton(
            JButton button,
            int ply
    ) {

        boolean selected =
                currentPly == ply;

        Badge badge = badges.getOrDefault(
                ply,
                Badge.NONE
        );


        // 기본 색상
        Color background = Theme.RAISED;

        Color foreground = Theme.TEXT;

        Color borderColor = Theme.LINE;


        // 현재 선택된 수
        if (selected) {

            background = Theme.ACCENT;

            foreground = new Color(0x342316);

            borderColor = Theme.ACCENT;
        }


        // 정답 표시
        else if (badge == Badge.CORRECT) {

            foreground = Theme.OK;

            borderColor = Theme.OK;
        }


        // 오답 표시
        else if (badge == Badge.WRONG) {

            foreground = Theme.BAD;

            borderColor = Theme.BAD;
        }


        // 실수 표시
        else if (badge == Badge.BLUNDER) {

            foreground = Theme.BAD;

            background = new Color(0x493028);

            borderColor = Theme.BAD;
        }


        // 배지 표시 문자열
        String suffix = switch (badge) {

            case NONE -> "";

            case CORRECT -> "  ✓";

            case WRONG -> "  ✕";

            case BLUNDER -> "  !";
        };


        button.setText(
                sanText(ply) + suffix
        );

        button.setBackground(background);

        button.setForeground(foreground);

        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(
                                borderColor,
                                selected ? 2 : 1
                        ),
                        BorderFactory.createEmptyBorder(
                                5, 4, 5, 4
                        )
                )
        );
    }


    // =========================================
    // SAN 문자열 반환
    // =========================================

    private String sanText(int ply) {

        return sanMoves.get(ply - 1);
    }


    // =========================================
    // 한 수 행
    // =========================================

    private static final class MoveRow extends JPanel {

        private final JPanel whiteCell;

        private final JPanel blackCell;


        MoveRow(int fullmove) {

            setLayout(
                    new BorderLayout(7, 0)
            );

            setOpaque(false);


            // 수 번호
            JLabel numberLabel = new JLabel(
                    String.valueOf(fullmove),
                    SwingConstants.CENTER
            );

            numberLabel.setFont(
                    Theme.body(Theme.FONT_12, 1)
            );

            numberLabel.setForeground(
                    Theme.MUTED
            );

            numberLabel.setPreferredSize(
                    new Dimension(38, 35)
            );


            // 백과 흑의 수를 담는 영역
            JPanel moves = new JPanel(
                    new GridLayout(1, 2, 6, 0)
            );

            moves.setOpaque(false);


            whiteCell = new JPanel(
                    new BorderLayout()
            );

            whiteCell.setOpaque(false);

            blackCell = new JPanel(
                    new BorderLayout()
            );

            blackCell.setOpaque(false);


            moves.add(whiteCell);

            moves.add(blackCell);


            add(
                    numberLabel,
                    BorderLayout.WEST
            );

            add(
                    moves,
                    BorderLayout.CENTER
            );


            setMaximumSize(
                    new Dimension(
                            Integer.MAX_VALUE,
                            39
                    )
            );

            setPreferredSize(
                    new Dimension(250, 39)
            );
        }


        void setWhiteMove(java.awt.Component component) {

            whiteCell.removeAll();

            whiteCell.add(
                    component,
                    BorderLayout.CENTER
            );
        }


        void setBlackMove(java.awt.Component component) {

            blackCell.removeAll();

            blackCell.add(
                    component,
                    BorderLayout.CENTER
            );
        }
    }
}