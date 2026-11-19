package com.oopteama.chess.ui;

import com.oopteama.chess.engine.Board;
import com.oopteama.chess.engine.Move;
import com.oopteama.chess.model.AppStore;
import com.oopteama.chess.model.Settings;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UiSmokeTest {

    @Test
    void allScreensAndBoardThemesRenderWithoutError(@TempDir Path tempDir) {
        AppStore store = new AppStore(tempDir);

        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> {
            Theme.install();

            // BoardView 3개 테마 + 뒤집기 + 화살표 + 강조 + 체크 렌더링 검증
            BoardView boardView = new BoardView(store.settings());
            boardView.setSize(480, 480);
            boardView.setBoard(Board.fromFen("4k3/8/8/8/8/8/4R3/6K1 b - - 0 1")); // 흑 체크 상태
            boardView.setLastMove(Move.fromUci("e1e2"));
            boardView.setHighlightSquares(List.of(Board.parseSquare("e2"), Board.parseSquare("e8")));
            boardView.setArrow(Move.fromUci("e8d8"), Theme.ACCENT);

            for (Settings.BoardTheme theme : Settings.BoardTheme.values()) {
                store.settings().setBoardTheme(theme);
                boardView.setFlipped(false);
                paintToImage(boardView, 480, 480);
                boardView.setFlipped(true);
                paintToImage(boardView, 480, 480);
            }

            // 기물 선택 + 이동 가능 칸 호버 프리뷰(포크 + 체크) 렌더링 검증
            boardView.setFlipped(false);
            boardView.setMoveHandler(m -> {});
            boardView.setBoard(Board.fromFen("r3k3/ppp2ppp/8/3N4/8/8/PPP2PPP/4K3 w - - 0 1"));
            boardView.setSize(480, 480);
            // d5 클릭 (col=3, row=3) -> c7 호버 (col=2, row=1)
            int x0 = (480 - (460 / 8) * 8) / 2;
            int cell = 460 / 8;
            boardView.dispatchEvent(new MouseEvent(boardView, MouseEvent.MOUSE_PRESSED, 0, 0,
                    x0 + 3 * cell + cell / 2, x0 + 3 * cell + cell / 2, 1, false));
            boardView.dispatchEvent(new MouseEvent(boardView, MouseEvent.MOUSE_MOVED, 0, 0,
                    x0 + 2 * cell + cell / 2, x0 + 1 * cell + cell / 2, 0, false));
            paintToImage(boardView, 480, 480);

            // 7개 화면 생성, 라이프사이클(onShow/onSettingsChanged/onHide), 오프스크린 페인트 검증
            List<Screen> screens = List.of(
                    new HomePanel(store, id -> {}),
                    new LessonPanel(store),
                    new PuzzlePanel(store),
                    new AiGamePanel(store),
                    new LocalGamePanel(store),
                    new ProfilePanel(store),
                    new SettingsPanel(store)
            );
            for (Screen screen : screens) {
                screen.onShow();
                screen.onSettingsChanged();
                paintToImage(screen.view(), 1000, 700);
                screen.onHide();
            }
        }));
    }

    private static void paintToImage(JComponent comp, int width, int height) {
        comp.setSize(width, height);
        comp.doLayout();
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);
        comp.printAll(g2);
        g2.dispose();
    }
}
