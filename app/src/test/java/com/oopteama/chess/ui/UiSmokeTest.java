package com.oopteama.chess.ui;

import com.oopteama.chess.ai.MoveInsight;
import com.oopteama.chess.data.JsonStorage;
import com.oopteama.chess.data.Settings;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.ui.component.BoardPanel;
import com.oopteama.chess.ui.controller.AiGameController;
import com.oopteama.chess.ui.controller.HomeController;
import com.oopteama.chess.ui.controller.LessonController;
import com.oopteama.chess.ui.controller.LocalGameController;
import com.oopteama.chess.ui.controller.ProfileController;
import com.oopteama.chess.ui.controller.PuzzleController;
import com.oopteama.chess.ui.controller.ScreenController;
import com.oopteama.chess.ui.controller.SettingsController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 모든 화면과 체스판 테마가 오류 없이 그려지는지 확인합니다 (화면 없이 실행).
 */
class UiSmokeTest {

    @Test
    void everyScreenAndBoardThemeRendersWithoutError(@TempDir Path tempDir) {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));

        assertDoesNotThrow(() -> SwingUtilities.invokeAndWait(() -> {
            Theme.install();

            BoardPanel board = new BoardPanel(backend.settings());
            board.setSize(480, 480);
            board.setBoard(Board.fromFen("4k3/8/8/8/8/8/4R3/6K1 b - - 0 1"));
            board.setLastMove(Move.of("e1e2"));
            board.setHighlights(List.of(Position.of("e2"), Position.of("e8")));
            board.setArrow(Move.of("e8d8"), Theme.BOARD_ACCENT);
            board.select(Position.of("e8"), List.of(Position.of("d8"), Position.of("f8")));
            board.showInsight(new MoveInsight(Move.of("e8d8"), "Kd8", MoveInsight.Verdict.INACCURATE, 80,
                    Move.of("e2e8"), List.of(new MoveInsight.Reason(false, "예상 응수 Re8+: 체크를 당합니다",
                    null, List.of()))));
            for (Settings.BoardTheme theme : Settings.BoardTheme.values()) {
                backend.updateSettings(s -> s.setBoardTheme(theme));
                board.setFlipped(false);
                paint(board, 480, 480);
                board.setFlipped(true);
                paint(board, 480, 480);
            }
            board.showInsight(null);

            List<ScreenController> controllers = List.of(
                    new HomeController(backend, id -> { }),
                    new LessonController(backend),
                    new PuzzleController(backend),
                    new AiGameController(backend),
                    new LocalGameController(backend),
                    new ProfileController(backend),
                    new SettingsController(backend)
            );
            for (ScreenController controller : controllers) {
                controller.onShow();
                controller.onDataChanged();
                paint(controller.view(), 1000, 700);
                controller.onHide();
            }
        }));
    }

    private static void paint(JComponent component, int width, int height) {
        component.setSize(width, height);
        component.doLayout();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, width, height);
        component.printAll(g2);
        g2.dispose();
    }
}
