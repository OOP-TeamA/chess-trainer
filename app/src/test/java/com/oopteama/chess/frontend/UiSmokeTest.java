package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.engine.Move;
import com.oopteama.chess.backend.service.ChessBackend;
import com.oopteama.chess.backend.service.JsonStorage;
import com.oopteama.chess.backend.service.Settings;
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
    void allScreensBoardThemesAndHoverPreviewRenderWithoutError(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));

        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> {
            Theme.install();

            BoardView boardView = new BoardView(backend.settings(), backend::evaluateMove);
            boardView.setSize(480, 480);
            boardView.setBoard(Board.fromFen("4k3/8/8/8/8/8/4R3/6K1 b - - 0 1"));
            boardView.setLastMove(Move.fromUci("e1e2"));
            boardView.setHighlightSquares(List.of(Board.parseSquare("e2"), Board.parseSquare("e8")));
            boardView.setArrow(Move.fromUci("e8d8"), Theme.ACCENT);

            for (Settings.BoardTheme theme : Settings.BoardTheme.values()) {
                backend.updateSettings(s -> s.setBoardTheme(theme));
                boardView.setFlipped(false);
                paintToImage(boardView, 480, 480);
                boardView.setFlipped(true);
                paintToImage(boardView, 480, 480);
            }

            // 기물 선택 + 이동 가능 칸 호버 수 평가 렌더링 검증 (백엔드 콜백 주입 동작 확인)
            boardView.setFlipped(false);
            boardView.setMoveHandler(m -> {});
            boardView.setBoard(Board.fromFen("r3k3/ppp2ppp/8/3N4/8/8/PPP2PPP/4K3 w - - 0 1"));
            boardView.setSize(480, 480);
            int x0 = (480 - (460 / 8) * 8) / 2;
            int cell = 460 / 8;
            boardView.dispatchEvent(new MouseEvent(boardView, MouseEvent.MOUSE_PRESSED, 0, 0,
                    x0 + 3 * cell + cell / 2, x0 + 3 * cell + cell / 2, 1, false));
            boardView.dispatchEvent(new MouseEvent(boardView, MouseEvent.MOUSE_MOVED, 0, 0,
                    x0 + 2 * cell + cell / 2, x0 + 1 * cell + cell / 2, 0, false));
            paintToImage(boardView, 480, 480);

            List<Screen> screens = List.of(
                    new HomePanel(backend, id -> {}),
                    new LessonPanel(backend),
                    new PuzzlePanel(backend),
                    new AiGamePanel(backend),
                    new LocalGamePanel(backend),
                    new ProfilePanel(backend),
                    new SettingsPanel(backend)
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
