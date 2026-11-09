package com.oopteama.chess.ui;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ButtonGroup;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import com.oopteama.chess.nav.Navigator;
import com.oopteama.chess.nav.Route;


public final class NavRail extends JPanel {

    private final Navigator navigator;

    // 메뉴 버튼 관리
    private final Map<Class<?>, JToggleButton> buttons = new HashMap<>();

    private final ButtonGroup buttonGroup = new ButtonGroup();

    private Route activeRoute;


    // 생성자
    public NavRail(Navigator navigator) {

        this.navigator = Objects.requireNonNull(navigator);

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setBackground(Theme.WINDOW);

        setBorder(new EmptyBorder(24, 12, 24, 12));

        setPreferredSize(new Dimension(76, 0));


        // 프로필 영역
        addProfile();

        add(Box.createVerticalStrut(24));


        // 주요 메뉴
        addMenuButton("⌂", "홈", new Route.Home());

        addMenuButton("♘", "룰학습", new Route.Lessons());

        addMenuButton("✦", "체스 퀴즈", new Route.Puzzles());

        addMenuButton("♛", "명국 기보", new Route.Library());

        addMenuButton("♞", "AI 대전", new Route.NewGame());

        addMenuButton("♧", "용어집", new Route.Glossary(""));


        // 아래쪽 메뉴를 하단으로 밀어내기
        add(Box.createVerticalGlue());

        addMenuButton("▥", "학습 기록", new Route.LearningRecord());

        addMenuButton("⚙", "설정", new Route.Prefs());


        // 처음 선택된 메뉴는 홈
        setActiveRoute(new Route.Home());
    }


    // 프로필 영역
    private void addProfile() {

        JLabel mark = new JLabel("♞", SwingConstants.CENTER);
        mark.setFont(Theme.title(Theme.FONT_21));
        mark.setForeground(Theme.ACCENT);
        mark.setAlignmentX(Component.CENTER_ALIGNMENT);
        mark.setToolTipText("Chess Trainer");
        add(mark);
    }


    // 메뉴 버튼 생성
    private void addMenuButton(String icon, String label, Route route) {

        JToggleButton button = new JToggleButton(icon);

        button.setFont(Theme.body(Theme.FONT_21, Font.PLAIN));

        button.setForeground(Theme.TEXT);

        button.setBackground(Theme.WINDOW);

        button.setFocusPainted(false);

        button.setBorderPainted(false);

        button.setContentAreaFilled(true);

        button.setOpaque(true);

        button.setHorizontalAlignment(SwingConstants.CENTER);

        button.setBorder(new EmptyBorder(10, 8, 10, 8));

        button.setPreferredSize(new Dimension(48, 48));
        button.setMaximumSize(new Dimension(48, 48));

        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        button.setCursor(
            new Cursor(Cursor.HAND_CURSOR)
        );

        button.addItemListener(e ->
            button.setBackground(button.isSelected() ? Theme.RAISED : Theme.WINDOW)
        );

        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);


        // 버튼 클릭 시 화면 이동
        button.addActionListener(e -> {

            // 실제 화면 이동과 선택 상태 변경은 MainFrame에서 처리
            navigator.go(route);

            // 이동이 확인 대화상자에서 취소되면 현재 선택을 복원
            setActiveRoute(activeRoute);

        });


        // 메뉴 버튼 등록
        buttonGroup.add(button);
        buttons.put(route.getClass(), button);

        add(button);
    }


    // 선택된 메뉴 표시
    private void setActive(JToggleButton button) {
        if (button == null) {
            buttonGroup.clearSelection();
            return;
        }

        button.setSelected(true);
    }


    // 현재 화면에 맞는 메뉴 선택
    public void setActiveRoute(Route route) {

        Objects.requireNonNull(route);

        activeRoute = route;

        Class<?> menuType = route.getClass();


        // 하위 화면에서도 상위 메뉴가 선택되도록 처리
        if (route instanceof Route.Lesson) {

            menuType = Route.Lessons.class;

        } else if (route instanceof Route.Puzzle) {

            menuType = Route.Puzzles.class;

        } else if (route instanceof Route.AiGame) {

            menuType = Route.NewGame.class;

        } else if (route instanceof Route.Replay) {

            menuType = Route.Library.class;

        }


        // 해당 메뉴 버튼 찾기
        JToggleButton button = buttons.get(menuType);

        // 해당하는 메뉴가 없으면 선택 해제
        setActive(button);
    }
}
