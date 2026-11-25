package com.oopteama.chess.ui.controller;

import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.data.JsonStorage;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.screen.AiGameScreen;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.SwingUtilities;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * AI 대전 컨트롤러를 검증합니다: 화면을 떠났다 돌아와도 AI 차례가 이어지는지, 기권 전적이 한 번만 기록되는지.
 */
class AiGameControllerTest {

    @Test
    void aiTurnResumesAfterLeavingWhileAiWasThinking(@TempDir Path tempDir) throws Exception {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        AiGameController[] controller = new AiGameController[1];
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            controller[0] = new AiGameController(backend);
            AiGameScreen screen = (AiGameScreen) controller[0].view();
            screen.sideBox().setSelectedIndex(1); // 흑으로 플레이 → AI(백)가 먼저 둠
            screen.newGameButton().doClick();
            // AI가 계산을 끝내기 전에 다른 화면으로 갔다가 돌아옴
            controller[0].onHide();
            controller[0].onShow();
        });

        int[] moves = {0};
        long deadline = System.currentTimeMillis() + 5_000;
        while (moves[0] == 0 && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            SwingUtilities.invokeAndWait(() -> moves[0] = controller[0].game.history().size());
        }
        assertEquals(1, moves[0], "돌아온 뒤 AI(백)가 첫 수를 두어야 합니다.");
    }

    @Test
    void resignRecordsOneLossForSelectedDifficulty(@TempDir Path tempDir) throws Exception {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            AiGameController controller = new AiGameController(backend);
            AiGameScreen screen = (AiGameScreen) controller.view();
            screen.difficultyBox().setSelectedItem(Difficulty.MEDIUM);
            screen.newGameButton().doClick();
            screen.resignButton().doClick();
            screen.resignButton().doClick(); // 이미 끝난 대국: 다시 기록하지 않음

            assertArrayEquals(new int[]{0, 0, 1}, backend.activeProfile().aiRecord(Difficulty.MEDIUM));
            assertEquals(1, backend.activeProfile().aiLosses());
        });
    }
}
