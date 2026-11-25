package com.oopteama.chess.ui.controller;

import com.oopteama.chess.data.JsonStorage;
import com.oopteama.chess.model.Position;
import com.oopteama.chess.service.ChessBackend;
import com.oopteama.chess.ui.Theme;
import com.oopteama.chess.ui.component.BoardPanel;
import com.oopteama.chess.ui.component.CollapsibleList;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 레슨·퍼즐 컨트롤러의 응수 대기 처리와 금색 강조색 위 글자 대비를 검증합니다.
 */
class TrainingControllerTest {

    /** 응수 타이머(400~420ms)가 충분히 지나도록 기다리는 시간. */
    private static final long REPLY_WAIT_MS = 1200;

    @Test
    void puzzleReplyIsAppliedAfterLeavingAndReturning(@TempDir Path tempDir) throws Exception {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        PuzzleController[] puzzle = new PuzzleController[1];
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            puzzle[0] = new PuzzleController(backend);
            Container view = (Container) puzzle[0].view();
            layout(view);
            find(view, JButton.class, b -> b.getText().contains("05.")).doClick(); // 2수 메이트
            BoardPanel board = find(view, BoardPanel.class, b -> true);
            click(board, "b2");
            click(board, "b7");

            String explanation = find(view, JTextArea.class, a -> true).getText();
            assertTrue(explanation.contains("응수를 기다리는 중"), explanation);
            assertFalse(explanation.contains("다른 수를 둘 수 있어요"), explanation);

            // 응수 타이머가 돌기 전에 다른 화면으로 갔다가 돌아옴
            puzzle[0].onHide();
            puzzle[0].onShow();
        });
        Thread.sleep(REPLY_WAIT_MS);
        SwingUtilities.invokeAndWait(() -> assertTrue(
                find((Container) puzzle[0].view(), JButton.class, b -> b.getText().equals("무르기")).isEnabled(),
                "돌아온 뒤 응수가 반영되어 무르기를 쓸 수 있어야 합니다."));
    }

    @Test
    void lessonReplyIsAppliedAfterLeavingAndReturning(@TempDir Path tempDir) throws Exception {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        LessonController[] lesson = new LessonController[1];
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            lesson[0] = new LessonController(backend);
            Container view = (Container) lesson[0].view();
            layout(view);
            find(view, JButton.class, b -> b.getText().equals("유명 오프닝")).doClick();
            BoardPanel board = find(view, BoardPanel.class, b -> true);
            click(board, "e2");
            click(board, "e4");
            lesson[0].onHide();
            lesson[0].onShow();
        });
        Thread.sleep(REPLY_WAIT_MS);
        SwingUtilities.invokeAndWait(() -> assertTrue(
                find((Container) lesson[0].view(), JButton.class, b -> b.getText().equals("다음 단계 →")).isEnabled(),
                "돌아온 뒤 흑의 응수가 반영되어 다음 단계로 갈 수 있어야 합니다."));
    }

    @Test
    void textOnGoldAccentStaysReadable(@TempDir Path tempDir) throws Exception {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            Container lessonView = (Container) new LessonController(backend).view();
            JButton activeTab = find(lessonView, JButton.class, b -> b.getText().equals("기본 튜토리얼"));
            assertTrue(contrast(activeTab.getForeground(), Theme.ACCENT) >= 4.5, "활성 레슨 탭 글자 대비");
            assertTrue(contrast(UIManager.getColor("CheckBox.icon.checkmarkColor"),
                    UIManager.getColor("CheckBox.icon.selectedBackground")) >= 4.5, "체크박스 체크 표시 대비");
        });
    }

    @Test
    void collapsibleListToggleKeepsSecondaryButtonStyle() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            JButton reference = Theme.secondaryButton("<");
            JButton toggle = find(new CollapsibleList(new JPanel(), "목록"), JButton.class, b -> true);
            assertEquals(reference.getBackground(), toggle.getBackground());
        });
    }

    private static void layout(Container root) {
        root.setSize(1100, 700);
        layoutTree(root);
        layoutTree(root);
    }

    private static void layoutTree(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container inner) {
                layoutTree(inner);
            }
        }
    }

    private static <T extends Component> T find(Container root, Class<T> type, Predicate<T> filter) {
        T found = search(root, type, filter);
        if (found == null) {
            fail(type.getSimpleName() + "를 찾지 못했습니다.");
        }
        return found;
    }

    private static <T extends Component> T search(Container root, Class<T> type, Predicate<T> filter) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && filter.test(type.cast(child))) {
                return type.cast(child);
            }
            if (child instanceof Container inner) {
                T found = search(inner, type, filter);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 뒤집지 않은 체스판에서 칸 가운데를 클릭 (BoardPanel의 여백 10px 배치와 같은 계산). */
    private static void click(BoardPanel board, String square) {
        int size = Math.max(64, Math.min(board.getWidth(), board.getHeight()) - 20);
        int cell = size / 8;
        int left = (board.getWidth() - cell * 8) / 2;
        int top = (board.getHeight() - cell * 8) / 2;
        Position position = Position.of(square);
        Point p = new Point(left + position.col() * cell + cell / 2, top + (7 - position.row()) * cell + cell / 2);
        board.dispatchEvent(new MouseEvent(board, MouseEvent.MOUSE_PRESSED, 0, 0, p.x, p.y, 1, false));
        board.dispatchEvent(new MouseEvent(board, MouseEvent.MOUSE_RELEASED, 0, 0, p.x, p.y, 1, false));
    }

    /** WCAG 상대 휘도 대비. */
    private static double contrast(Color a, Color b) {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color c) {
        double[] rgb = {c.getRed() / 255.0, c.getGreen() / 255.0, c.getBlue() / 255.0};
        for (int i = 0; i < 3; i++) {
            rgb[i] = rgb[i] <= 0.03928 ? rgb[i] / 12.92 : Math.pow((rgb[i] + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
    }
}
