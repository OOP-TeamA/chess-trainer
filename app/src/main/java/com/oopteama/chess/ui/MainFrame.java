package com.oopteama.chess.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.oopteama.chess.nav.Navigator;
import com.oopteama.chess.nav.Route;


public final class MainFrame extends JFrame implements Navigator {

    // 공통 서비스
    private final Services services;

    // 화면 전환 관리
    private final CardLayout cardLayout;
    private final JPanel cardPanel;

    // 화면 목록
    // Route 종류별로 화면 객체를 한 번만 생성하여 재사용
    private final Map<Class<?>, Screen> screens = new HashMap<>();

    // 이전 화면 기록
    private final Deque<Route> history = new ArrayDeque<>();

    // 왼쪽 메뉴
    private final NavRail navRail;

    // 현재 화면 정보
    private Screen currentScreen;
    private Route currentRoute;


    // 생성자
    public MainFrame(Services services) {

        this.services = Objects.requireNonNull(services);

        setTitle("Chess Learner");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        setMinimumSize(new Dimension(1024, 700));

        setSize(1280, 800);

        setLocationRelativeTo(null);

        setLayout(new BorderLayout());


        // 화면 전환 영역
        cardLayout = new CardLayout();

        cardPanel = new JPanel(cardLayout);

        cardPanel.setBackground(Theme.WINDOW);


        // 왼쪽 메뉴 생성
        navRail = new NavRail(this);


        // 전체 화면 배치
        add(navRail, BorderLayout.WEST);

        add(cardPanel, BorderLayout.CENTER);


        // 첫 화면은 홈
        go(new Route.Home());
    }


    // 화면 이동
    @Override
    public void go(Route route) {

        Objects.requireNonNull(route);


        // 현재 화면과 동일하면 이동하지 않음
        if (route.equals(currentRoute)) {
            return;
        }


        // 현재 화면을 나갈 수 있는지 확인
        if (!confirmLeave()) {
            return;
        }


        // 현재 화면을 이전 화면 기록에 저장
        if (currentRoute != null) {

            history.push(currentRoute);
        }


        showRoute(route);
    }


    // 이전 화면으로 이동
    @Override
    public void back() {

        if (history.isEmpty()) {
            return;
        }


        // 현재 화면을 나갈 수 있는지 확인
        if (!confirmLeave()) {
            return;
        }


        // 이전 화면 가져오기
        Route previousRoute = history.pop();


        showRoute(previousRoute);
    }


    // 실제 화면 표시
    private void showRoute(Route route) {

        // 기존 화면 정리
        if (currentScreen != null) {

            currentScreen.onHide();
        }


        // Route 종류를 기준으로 화면 구분
        Class<?> routeType = route.getClass();


        // 이미 만들어진 화면인지 확인
        Screen nextScreen = screens.get(routeType);


        // 아직 만들어지지 않은 화면이면 생성
        if (nextScreen == null) {

            nextScreen = createScreen(route);

            screens.put(routeType, nextScreen);


            // 화면 등록
            cardPanel.add(
                nextScreen.component(),
                nextScreen.cardName()
            );
        }


        // 현재 화면 정보 갱신
        currentScreen = nextScreen;

        currentRoute = route;


        // 화면 표시
        cardLayout.show(
            cardPanel,
            nextScreen.cardName()
        );


        // 화면 진입 처리
        currentScreen.onShow(route);


        // 왼쪽 메뉴 선택 상태 변경
        navRail.setActiveRoute(route);
    }


    // Route에 맞는 화면 생성
    private Screen createScreen(Route route) {

        // 홈 화면
        if (route instanceof Route.Home) {

            return new HomeScreen(this);
        }


        // 로컬 대국 화면
        if (route instanceof Route.LocalGame) {

            return new LocalGameScreen(services);
        }


        // 아직 구현하지 않은 화면은 임시 화면으로 생성
        String key = route.getClass().getSimpleName();

        return new PlaceholderScreen(
            key,
            getRouteTitle(route)
        );
    }


    // 화면을 나갈 때 확인
    private boolean confirmLeave() {

        if (currentScreen == null) {
            return true;
        }


        Optional<String> warning = currentScreen.leaveWarning();

        if (warning.isEmpty()) {
            return true;
        }


        int result = JOptionPane.showConfirmDialog(
            this,
            warning.get(),
            "화면 이동 확인",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );


        return result == JOptionPane.OK_OPTION;
    }


    // 화면 이름 반환
    private String getRouteTitle(Route route) {

        if (route instanceof Route.Home) {

            return "홈";
        }

        if (route instanceof Route.LocalGame) {

            return "로컬 대국";
        }

        if (route instanceof Route.Lessons) {

            return "룰학습";
        }

        if (route instanceof Route.Lesson) {

            return "레슨 진행";
        }

        if (route instanceof Route.Puzzles) {

            return "체스 퀴즈";
        }

        if (route instanceof Route.Puzzle) {

            return "퍼즐 풀이";
        }

        if (route instanceof Route.TermQuiz) {

            return "용어 퀴즈";
        }

        if (route instanceof Route.Library) {

            return "명국 기보";
        }

        if (route instanceof Route.NewGame) {

            return "AI 대전 설정";
        }

        if (route instanceof Route.AiGame) {

            return "AI 대국";
        }

        if (route instanceof Route.Replay) {

            return "기보 재생";
        }

        if (route instanceof Route.Glossary) {

            return "용어집";
        }

        if (route instanceof Route.Favorites) {

            return "즐겨찾기";
        }

        if (route instanceof Route.WrongNotes) {

            return "오답 노트";
        }

        if (route instanceof Route.Profiles) {

            return "프로필";
        }

        if (route instanceof Route.Ranking) {

            return "랭킹";
        }

        if (route instanceof Route.LearningRecord) {

            return "학습 기록";
        }

        if (route instanceof Route.Prefs) {

            return "설정";
        }


        return route.getClass().getSimpleName();
    }


    // 아직 구현되지 않은 화면을 위한 임시 클래스
    private static class PlaceholderScreen implements Screen {

        private final String cardName;

        private final JPanel panel;


        private PlaceholderScreen(String cardName, String title) {

            this.cardName = cardName;

            panel = new JPanel(new BorderLayout());

            panel.setBackground(Theme.WINDOW);


            JLabel label = new JLabel(
                title + " 화면",
                SwingConstants.CENTER
            );

            label.setFont(Theme.title(28));

            label.setForeground(Theme.TEXT);


            panel.add(label, BorderLayout.CENTER);
        }


        @Override
        public String cardName() {

            return cardName;
        }


        @Override
        public JComponent component() {

            return panel;
        }


        @Override
        public void onShow(Route route) {

            // 화면이 표시될 때 실행할 작업
        }


        @Override
        public void onHide() {

            // 화면을 나갈 때 정리할 작업
        }
    }
}
