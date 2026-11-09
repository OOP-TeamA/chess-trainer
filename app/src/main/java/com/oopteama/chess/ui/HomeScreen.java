package com.oopteama.chess.ui;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.oopteama.chess.nav.Navigator;
import com.oopteama.chess.nav.Route;


public final class HomeScreen extends JPanel implements Screen {

    private final Navigator navigator;


    // 생성자
    public HomeScreen(Navigator navigator) {

        this.navigator = Objects.requireNonNull(navigator);

        setLayout(new BorderLayout(20, 20));

        setBackground(Theme.WINDOW);

        setBorder(
            BorderFactory.createEmptyBorder(40, 45, 40, 45)
        );

        add(createHeader(), BorderLayout.NORTH);

        add(createMenu(), BorderLayout.CENTER);
    }


    // 홈 화면 제목 영역
    private JPanel createHeader() {

        JPanel panel = new JPanel();

        panel.setLayout(
            new BoxLayout(panel, BoxLayout.Y_AXIS)
        );

        panel.setBackground(Theme.WINDOW);


        // 메인 제목
        JLabel title = new JLabel("CHESS TRAINER");

        title.setFont(Theme.title(Theme.FONT_36));

        title.setForeground(Theme.ACCENT);


        // 부제목
        JLabel subtitle = new JLabel(
            "체스를 배우고, 연습하고, 실력을 키워보세요."
        );

        subtitle.setFont(
            Theme.body(Theme.FONT_16, Font.PLAIN)
        );

        subtitle.setForeground(Theme.MUTED);


        panel.add(title);

        panel.add(Box.createVerticalStrut(12));

        panel.add(subtitle);

        panel.add(Box.createVerticalStrut(25));


        return panel;
    }


    // 메뉴 버튼 영역
    private JPanel createMenu() {

        JPanel panel = new JPanel(
            new GridLayout(2, 3, 15, 15)
        );

        panel.setBackground(Theme.WINDOW);


        // 로컬 대국
        panel.add(createMenuButton(
            "로컬 대국",
            "두 사람이 한 기기에서 대국하기",
            () -> navigator.go(new Route.LocalGame())
        ));


        // 룰학습
        panel.add(createMenuButton(
            "체스 레슨",
            "체스의 기본 규칙과 전략 배우기",
            () -> navigator.go(new Route.Lessons())
        ));


        // 퍼즐
        panel.add(createMenuButton(
            "퍼즐",
            "다양한 체스 문제 풀어보기",
            () -> navigator.go(new Route.Puzzles())
        ));


        // 용어 퀴즈
        panel.add(createMenuButton(
            "용어 퀴즈",
            "체스 용어 학습하기",
            () -> navigator.go(new Route.TermQuiz())
        ));


        // 용어집
        panel.add(createMenuButton(
            "용어집",
            "체스 관련 용어 찾아보기",
            () -> navigator.go(new Route.Glossary(""))
        ));


        // 학습 기록
        panel.add(createMenuButton(
            "학습 기록",
            "나의 학습 진행 상황 확인하기",
            () -> navigator.go(new Route.LearningRecord())
        ));


        return panel;
    }


    // 개별 메뉴 버튼 생성
    private JButton createMenuButton(
        String title,
        String description,
        Runnable action
    ) {

        Objects.requireNonNull(action);


        JButton button = new JButton(
            "<html>"
                + "<div style='text-align:center;'>"
                + "<h2>" + title + "</h2>"
                + "<p>" + description + "</p>"
                + "</div>"
                + "</html>"
        );


        // 버튼 글꼴과 색상
        button.setFont(
            Theme.body(Theme.FONT_16, Font.BOLD)
        );

        button.setForeground(Theme.TEXT);

        button.setBackground(Theme.PANEL);


        // 버튼 모양 설정
        button.setFocusPainted(false);

        button.setOpaque(true);

        button.setBorder(
            BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.LINE),
                BorderFactory.createEmptyBorder(
                    20, 12, 20, 12
                )
            )
        );


        // 마우스 커서 및 툴팁
        button.setCursor(
            new Cursor(Cursor.HAND_CURSOR)
        );

        button.setToolTipText(description);


        // 버튼 클릭 이벤트
        button.addActionListener(e -> action.run());


        return button;
    }


    // 화면 이름
    @Override
    public String cardName() {

        return "home";
    }


    // 현재 화면 컴포넌트 반환
    @Override
    public JPanel component() {

        return this;
    }


    // 화면 진입 시 실행
    @Override
    public void onShow(Route route) {

        // 홈 화면에 진입했을 때 실행할 작업
    }


    // 화면 이탈 시 실행
    @Override
    public void onHide() {

        // 홈 화면을 벗어날 때 정리할 작업
    }
}
