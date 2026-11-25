package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.service.ChessBackend;
import com.oopteama.chess.backend.service.Profile;
import com.oopteama.chess.backend.training.Lesson;
import com.oopteama.chess.backend.training.Puzzle;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 홈 대시보드 화면: 현재 프로필 요약, 추천 다음 학습, 4개 핵심 기능 바로가기를 제공합니다.
 */
public final class HomePanel extends JPanel implements Screen {

    private final ChessBackend backend;
    private final Consumer<Screen.Id> navigator;

    private final JLabel greetingLabel = Theme.title("안녕하세요!");
    private final JLabel scoreBadge = Theme.badge("0점", Theme.ACCENT);
    private final JLabel lessonStat = new JLabel();
    private final JLabel puzzleStat = new JLabel();
    private final JLabel aiStat = new JLabel();

    private final JLabel recTitle = Theme.sectionTitle("다음 추천 학습");
    private final JLabel recDesc = Theme.muted("");
    private final JButton recButton = Theme.primaryButton("바로 시작");
    private Screen.Id recTarget = Screen.Id.LESSONS;

    public HomePanel(ChessBackend backend, Consumer<Screen.Id> navigator) {
        this.backend = backend;
        this.navigator = navigator;
        setLayout(new BorderLayout(0, 20));
        setBackground(Theme.BG);
        setBorder(new EmptyBorder(28, 32, 28, 32));

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        body.add(buildHero());
        body.add(Box.createVerticalStrut(18));
        body.add(buildStatsRow());
        body.add(Box.createVerticalStrut(18));
        body.add(buildRecommendationCard());
        body.add(Box.createVerticalStrut(22));
        body.add(buildQuickLaunchGrid());

        add(body, BorderLayout.NORTH);
        refresh();
    }

    private JPanel buildHero() {
        JPanel hero = new JPanel(new BorderLayout(12, 6));
        hero.setOpaque(false);
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(greetingLabel);
        left.add(Box.createVerticalStrut(4));
        left.add(Theme.muted("체스 튜토리얼, 유명 오프닝, 전술 퍼즐과 AI 대전을 한 곳에서 연습하세요."));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        right.setOpaque(false);
        right.add(scoreBadge);

        hero.add(left, BorderLayout.CENTER);
        hero.add(right, BorderLayout.EAST);
        return hero;
    }

    private JPanel buildStatsRow() {
        JPanel row = new JPanel(new GridLayout(1, 3, 14, 0));
        row.setOpaque(false);
        row.add(statCard("레슨 완료", lessonStat, Theme.ACCENT));
        row.add(statCard("퍼즐 해결", puzzleStat, Theme.INFO));
        row.add(statCard("AI 대전 전적", aiStat, Theme.WARNING));
        return row;
    }

    private JPanel statCard(String caption, JLabel valueLabel, Color accent) {
        JPanel card = Theme.card(16);
        valueLabel.setFont(Theme.bold(22));
        valueLabel.setForeground(accent);
        card.add(Theme.muted(caption), BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildRecommendationCard() {
        JPanel card = new Theme.RoundedPanel(Theme.ACCENT_SOFT, Theme.ACCENT, 14);
        card.setLayout(new BorderLayout(16, 6));
        card.setBorder(new EmptyBorder(18, 22, 18, 22));

        JPanel text = new JPanel();
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));
        text.setOpaque(false);
        JLabel tag = Theme.badge("추천 활동", Theme.SUCCESS);
        tag.setAlignmentX(LEFT_ALIGNMENT);
        recTitle.setAlignmentX(LEFT_ALIGNMENT);
        recDesc.setAlignmentX(LEFT_ALIGNMENT);

        text.add(tag);
        text.add(Box.createVerticalStrut(8));
        text.add(recTitle);
        text.add(Box.createVerticalStrut(4));
        text.add(recDesc);

        recButton.addActionListener(e -> navigator.accept(recTarget));
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 14));
        btnWrap.setOpaque(false);
        btnWrap.add(recButton);

        card.add(text, BorderLayout.CENTER);
        card.add(btnWrap, BorderLayout.EAST);
        return card;
    }

    private JPanel buildQuickLaunchGrid() {
        JPanel grid = new JPanel(new GridLayout(2, 2, 14, 14));
        grid.setOpaque(false);
        grid.add(launchCard("레슨 · 튜토리얼 & 오프닝", "기물 이동 기초부터 대표 오프닝까지 직접 두며 익힙니다.", Screen.Id.LESSONS, Theme.ACCENT));
        grid.add(launchCard("전술 & 자유 메이트 퍼즐", "N수 안에 자유롭게 체크메이트하거나 전술 수를 풀이합니다.", Screen.Id.PUZZLES, Theme.INFO));
        grid.add(launchCard("AI 대전", "입문부터 상급까지 4단계 AI와 실전 대국을 펼칩니다.", Screen.Id.AI_GAME, Theme.WARNING));
        grid.add(launchCard("로컬 2인 대전", "한 화면에서 친구와 번갈아 체스 대국을 즐깁니다.", Screen.Id.LOCAL_GAME, Theme.SUCCESS));
        return grid;
    }

    private JPanel launchCard(String title, String desc, Screen.Id target, Color accent) {
        JPanel card = Theme.hoverCard(20, accent);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel arrow = new JLabel("→");
        arrow.setFont(Theme.bold(18));
        arrow.setForeground(accent);
        header.add(Theme.sectionTitle(title), BorderLayout.WEST);
        header.add(arrow, BorderLayout.EAST);

        JLabel descLabel = new JLabel("<html><body style='width:280px'>" + desc + "</body></html>");
        descLabel.setFont(Theme.font(13));
        descLabel.setForeground(Theme.MUTED);

        card.add(header, BorderLayout.NORTH);
        card.add(descLabel, BorderLayout.CENTER);
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                navigator.accept(target);
            }
        });
        return card;
    }

    public void refresh() {
        Profile p = backend.activeProfile();
        greetingLabel.setText(p.name() + "님, 오늘도 한 수 배워볼까요?");
        scoreBadge.setText("총점 " + p.totalScore() + "점");

        int totalLessons = backend.lessons().size();
        int doneLessons = (int) backend.lessons().stream().filter(l -> p.isLessonCompleted(l.id())).count();
        lessonStat.setText(doneLessons + " / " + totalLessons);

        int totalPuzzles = backend.puzzles().size();
        puzzleStat.setText(p.solvedPuzzleCount() + " / " + totalPuzzles + " (" + p.puzzleAccuracyPercent() + "%)");
        aiStat.setText(p.aiWins() + "승 " + p.aiDraws() + "무 " + p.aiLosses() + "패");

        Optional<Lesson> nextLesson = backend.lessons().stream()
                .filter(l -> !p.isLessonCompleted(l.id()))
                .findFirst();
        if (nextLesson.isPresent()) {
            Lesson l = nextLesson.get();
            recTitle.setText("[" + l.category().label() + "] " + l.title());
            recDesc.setText(l.summary());
            recButton.setText("레슨 이어하기");
            recTarget = Screen.Id.LESSONS;
            return;
        }
        Optional<Puzzle> nextPuzzle = backend.puzzles().stream()
                .filter(pz -> !p.isPuzzleSolved(pz.id()))
                .findFirst();
        if (nextPuzzle.isPresent()) {
            Puzzle pz = nextPuzzle.get();
            recTitle.setText("[퍼즐 · " + pz.theme() + "] " + pz.title());
            recDesc.setText("아직 해결하지 않은 퍼즐에 도전해 보세요.");
            recButton.setText("퍼즐 풀러 가기");
            recTarget = Screen.Id.PUZZLES;
            return;
        }
        recTitle.setText("모든 학습 콘텐츠를 완료했습니다!");
        recDesc.setText("이제 AI 대전에서 실전 감각을 끌어올려 보세요.");
        recButton.setText("AI 대전 시작");
        recTarget = Screen.Id.AI_GAME;
    }

    @Override
    public JComponent view() {
        return this;
    }

    @Override
    public void onShow() {
        refresh();
    }
}
