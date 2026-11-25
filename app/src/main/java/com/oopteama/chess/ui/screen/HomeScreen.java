package com.oopteama.chess.ui.screen;

import com.oopteama.chess.ui.ScreenId;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.RoundedPanel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 홈 화면(View): 인사말과 총점, 학습 현황 3칸, 다음 추천 학습, 4가지 모드 바로가기.
 */
public final class HomeScreen extends JPanel {

    private final JLabel greetingLabel = Theme.title("안녕하세요!");
    private final JLabel scoreBadge = Theme.badge("0점", Theme.ACCENT);
    private final JLabel lessonStat = new JLabel();
    private final JLabel puzzleStat = new JLabel();
    private final JLabel aiStat = new JLabel();
    private final JLabel recommendTitle = Theme.sectionTitle("다음 추천 학습");
    private final JLabel recommendDescription = Theme.muted("");
    private final JButton recommendButton = Theme.primaryButton("바로 시작");
    private final List<Consumer<ScreenId>> launchListeners = new ArrayList<>();

    public HomeScreen() {
        setLayout(new BorderLayout(0, 20));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.add(buildGreeting());
        body.add(Box.createVerticalStrut(18));
        body.add(buildStats());
        body.add(Box.createVerticalStrut(18));
        body.add(buildRecommendation());
        body.add(Box.createVerticalStrut(22));
        body.add(buildLaunchCards());
        add(body, BorderLayout.NORTH);
    }

    private JPanel buildGreeting() {
        JPanel greeting = new JPanel(new BorderLayout(12, 6));
        greeting.setOpaque(false);
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        text.add(greetingLabel);
        text.add(Box.createVerticalStrut(4));
        text.add(Theme.muted("체스 튜토리얼, 유명 오프닝, 전술 퍼즐과 AI 대전을 한 곳에서 연습하세요."));
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        badge.setOpaque(false);
        badge.add(scoreBadge);
        greeting.add(text, BorderLayout.CENTER);
        greeting.add(badge, BorderLayout.EAST);
        return greeting;
    }

    private JPanel buildStats() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.add(statCard("레슨 완료", lessonStat, Theme.ACCENT));
        row.add(statCard("퍼즐 해결", puzzleStat, Theme.INFO));
        row.add(statCard("AI 대전 전적", aiStat, Theme.WARNING));
        return row;
    }

    private static JPanel statCard(String caption, JLabel value, Color accent) {
        JPanel card = Theme.card(16);
        value.setFont(Theme.bold(22));
        value.setForeground(accent);
        card.add(Theme.muted(caption), BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecommendation() {
        JPanel card = new RoundedPanel(Theme.ACCENT_SOFT, Theme.ACCENT, 14);
        card.setLayout(new BorderLayout(16, 6));
        card.setBorder(new EmptyBorder(18, 22, 18, 22));
        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel tag = Theme.badge("추천 활동", Theme.SUCCESS);
        tag.setAlignmentX(LEFT_ALIGNMENT);
        recommendTitle.setAlignmentX(LEFT_ALIGNMENT);
        recommendDescription.setAlignmentX(LEFT_ALIGNMENT);
        text.add(tag);
        text.add(Box.createVerticalStrut(8));
        text.add(recommendTitle);
        text.add(Box.createVerticalStrut(4));
        text.add(recommendDescription);
        JPanel buttonWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 14));
        buttonWrap.setOpaque(false);
        buttonWrap.add(recommendButton);
        card.add(text, BorderLayout.CENTER);
        card.add(buttonWrap, BorderLayout.EAST);
        return card;
    }

    private JPanel buildLaunchCards() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);
        grid.add(launchCard("레슨 · 튜토리얼 & 오프닝", "기물 이동 기초부터 대표 오프닝까지 직접 두며 익힙니다.", ScreenId.LESSONS, Theme.ACCENT));
        grid.add(launchCard("전술 & 자유 메이트 퍼즐", "N수 안에 자유롭게 체크메이트하거나 전술 수를 풀이합니다.", ScreenId.PUZZLES, Theme.INFO));
        grid.add(launchCard("AI 대전", "입문부터 상급까지 4단계 AI와 실전 대국을 펼칩니다.", ScreenId.AI_GAME, Theme.WARNING));
        grid.add(launchCard("로컬 2인 대전", "한 화면에서 친구와 번갈아 체스 대국을 즐깁니다.", ScreenId.LOCAL_GAME, Theme.SUCCESS));
        return grid;
    }

    private JPanel launchCard(String title, String description, ScreenId target, Color accent) {
        JPanel card = Theme.hoverCard(20, accent);
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel arrow = new JLabel("→");
        arrow.setFont(Theme.bold(18));
        arrow.setForeground(accent);
        header.add(Theme.sectionTitle(title), BorderLayout.WEST);
        header.add(arrow, BorderLayout.EAST);
        JLabel descriptionLabel = new JLabel("<html><body style='width:280px'>" + description + "</body></html>");
        descriptionLabel.setFont(Theme.font(13));
        descriptionLabel.setForeground(Theme.MUTED);
        card.add(header, BorderLayout.NORTH);
        card.add(descriptionLabel, BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                launchListeners.forEach(listener -> listener.accept(target));
            }
        });
        return card;
    }

    // ---- 컨트롤러가 쓰는 메서드 ----

    public JButton recommendButton() {
        return recommendButton;
    }

    /** 바로가기 카드를 눌렀을 때 실행할 동작을 등록합니다. */
    public void addLaunchListener(Consumer<ScreenId> listener) {
        launchListeners.add(listener);
    }

    public void showProfile(String name, int totalScore) {
        greetingLabel.setText(name + "님, 오늘도 한 수 배워볼까요?");
        scoreBadge.setText("총점 " + totalScore + "점");
    }

    public void showStats(String lessons, String puzzles, String aiRecord) {
        lessonStat.setText(lessons);
        puzzleStat.setText(puzzles);
        aiStat.setText(aiRecord);
    }

    public void showRecommendation(String title, String description, String buttonText) {
        recommendTitle.setText(title);
        recommendDescription.setText(description);
        recommendButton.setText(buttonText);
    }
}
