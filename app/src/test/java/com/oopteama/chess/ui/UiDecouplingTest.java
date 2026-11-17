package com.oopteama.chess.ui;

import com.oopteama.chess.api.AppServices;
import com.oopteama.chess.api.BoardInputMode;
import com.oopteama.chess.api.BoardSnapshot;
import com.oopteama.chess.api.GameOutcomeView;
import com.oopteama.chess.api.GameSession;
import com.oopteama.chess.api.GameState;
import com.oopteama.chess.api.HomeSummary;
import com.oopteama.chess.api.HomeSummaryProvider;
import com.oopteama.chess.api.MoveAttemptResult;
import com.oopteama.chess.api.MoveInput;
import com.oopteama.chess.api.SideColor;
import com.oopteama.chess.app.AppContext;
import com.oopteama.chess.nav.Navigator;
import com.oopteama.chess.nav.Route;
import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class UiDecouplingTest {

    @Test
    void UI는_백엔드_구현없이_스텁_AppServices만으로도_동작한다() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());

        AtomicBoolean closed = new AtomicBoolean(false);
        StubGameSession stubSession = new StubGameSession();
        AppServices stubServices = new AppServices() {
            @Override
            public GameSession createLocalGame() {
                return stubSession;
            }

            @Override
            public Optional<HomeSummaryProvider> homeSummary() {
                return Optional.of(() -> new HomeSummary(
                        120,
                        "나이트 배우기",
                        "추천 레슨",
                        50,
                        "1 / 2 레슨 · 50%",
                        HomeSummary.ContinueTarget.LESSONS,
                        "",
                        new HomeSummary.CardMetric("1 / 2", 50),
                        new HomeSummary.CardMetric("0문제", 0),
                        new HomeSummary.CardMetric("대기", 0),
                        new HomeSummary.CardMetric("대기", 0)
                ));
            }

            @Override
            public void saveOnClose() {
                closed.set(true);
            }
        };

        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            MainFrame frame = new MainFrame(stubServices);
            try {
                assertTrue(hasLabelText(frame, "나이트 배우기"));
                assertTrue(hasLabelText(frame, "점수 120"));

                frame.go(new Route.LocalGame());
                assertTrue(hasLabelText(frame, "로컬 대국"));

                frame.go(new Route.Lessons());
                assertTrue(hasLabelText(frame, "룰학습 화면"));

                frame.back();
                assertTrue(hasLabelText(frame, "로컬 대국"));

                BoardPanel panel = new BoardPanel();
                panel.setSnapshot(BoardSnapshot.empty());
                panel.setInputMode(BoardInputMode.SQUARE);
                panel.setFlipped(true);
                panel.mark(List.of(0, 63), BoardPanel.Mark.HINT);
                panel.setArrows(List.of(new BoardPanel.Arrow(0, 63, BoardPanel.ArrowKind.SOLUTION)));

                MoveListPanel moveList = new MoveListPanel();
                moveList.setMoves(List.of("e4", "e5"), 1, SideColor.WHITE);
                moveList.setCurrentPly(2);
                moveList.setBadge(1, MoveListPanel.Badge.CORRECT);

                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            } finally {
                frame.dispose();
            }
        });
        assertTrue(closed.get());
    }

    @Test
    void homeSummary가_비어있으면_HomeScreen이_기본_대기상태를_표시한다() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());

        AppContext context = new AppContext();
        AtomicReference<Route> navigated = new AtomicReference<>();
        Navigator navigator = new Navigator() {
            @Override
            public void go(Route route) {
                navigated.set(route);
            }

            @Override
            public void back() {
            }
        };

        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            HomeScreen home = new HomeScreen(navigator, context);
            home.onShow(new Route.Home());
            assertTrue(hasLabelText(home, "학습 기능 준비 중"));
            assertTrue(hasLabelText(home, "백엔드 연동 대기"));
            home.onHide();
        });
        assertEquals(null, navigated.get());
    }

    @Test
    void AppContext와_LocalGameScreen이_세션상태_변경을_반영한다() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());

        AppContext context = new AppContext();
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            LocalGameScreen screen = new LocalGameScreen(context);
            screen.onShow(new Route.LocalGame());
            assertTrue(screen.leaveWarning().isEmpty());
            assertTrue(hasLabelText(screen, "백 차례"));
            assertTrue(hasLabelText(screen, "0수 진행"));
            screen.onHide();

            // 스텁 세션으로 착수/종료 상태 반영 확인
            StubGameSession stub = new StubGameSession();
            LocalGameScreen stubScreen = new LocalGameScreen(new AppServices() {
                @Override
                public GameSession createLocalGame() {
                    return stub;
                }
            });
            stubScreen.onShow(new Route.LocalGame());
            stub.pushEndedState();
            assertTrue(hasLabelText(stubScreen, "대국 종료"));
            assertTrue(hasLabelText(stubScreen, "1수 진행"));
            assertFalse(stubScreen.leaveWarning().isPresent());
            stubScreen.onHide();
        });
    }

    private static boolean hasLabelText(Container root, String expected) {
        for (Component comp : root.getComponents()) {
            if (comp instanceof JLabel label && expected.equals(label.getText())) {
                return true;
            }
            if (comp instanceof Container nested && hasLabelText(nested, expected)) {
                return true;
            }
        }
        return false;
    }

    private static final class StubGameSession implements GameSession {
        private final List<Consumer<GameState>> listeners = new ArrayList<>();
        private GameState current = new GameState(
                BoardSnapshot.empty(), List.of(), false, false, Optional.empty()
        );

        void pushEndedState() {
            current = new GameState(
                    BoardSnapshot.empty(),
                    List.of("e4"),
                    true,
                    false,
                    Optional.of(new GameOutcomeView(
                            GameOutcomeView.Winner.WHITE,
                            GameOutcomeView.EndReason.RESIGNATION
                    ))
            );
            for (Consumer<GameState> l : List.copyOf(listeners)) {
                l.accept(current);
            }
        }

        @Override
        public GameState state() {
            return current;
        }

        @Override
        public MoveAttemptResult playMove(MoveInput move) {
            return MoveAttemptResult.ok();
        }

        @Override
        public boolean undo() {
            return false;
        }

        @Override
        public void resign() {
        }

        @Override
        public void newGame() {
        }

        @Override
        public void addListener(Consumer<GameState> listener) {
            listeners.add(listener);
        }

        @Override
        public void removeListener(Consumer<GameState> listener) {
            listeners.remove(listener);
        }
    }
}
