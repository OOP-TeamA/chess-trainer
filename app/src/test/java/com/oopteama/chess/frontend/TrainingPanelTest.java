package com.oopteama.chess.frontend;

import com.oopteama.chess.backend.engine.Board;
import com.oopteama.chess.backend.service.ChessBackend;
import com.oopteama.chess.backend.service.JsonStorage;
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
 * 레슨·퍼즐 화면의 응수 대기 처리와 강조색 위 글자 대비를 검증합니다.
 */
class TrainingPanelTest {

    /** 응수 타이머(400~420ms)가 충분히 지나도록 기다리는 시간. */
    private static final long REPLY_WAIT_MS = 1200;

    @Test
    void puzzleReplyIsAppliedAfterLeavingAndReturning(@TempDir Path tempDir) throws Exception {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        PuzzlePanel[] panel = new PuzzlePanel[1];
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            panel[0] = new PuzzlePanel(backend);
            layout(panel[0]);
            find(panel[0], JButton.class, b -> b.getText().contains("05.")).doClick(); // 2수 메이트
            BoardView board = find(panel[0], BoardView.class, b -> true);
            click(board, "b2");
            click(board, "b7");

            String explain = find(panel[0], JTextArea.class, a -> true).getText();
            assertTrue(explain.contains("응수를 기다리는 중"), explain);
            assertFalse(explain.contains("다른 수를 둘 수 있어요"), explain);

            // 응수 타이머가 돌기 전에 다른 화면으로 갔다가 돌아옴
            panel[0].onHide();
            panel[0].onShow();
        });
        Thread.sleep(REPLY_WAIT_MS);
        SwingUtilities.invokeAndWait(() -> assertTrue(
                find(panel[0], JButton.class, b -> b.getText().equals("무르기")).isEnabled(),
                "돌아온 뒤 응수가 반영되어 무르기를 쓸 수 있어야 합니다."));
    }

    @Test
    void lessonReplyIsAppliedAfterLeavingAndReturning(@TempDir Path tempDir) throws Exception {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        LessonPanel[] panel = new LessonPanel[1];
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            panel[0] = new LessonPanel(backend);
            layout(panel[0]);
            find(panel[0], JButton.class, b -> b.getText().equals("유명 오프닝")).doClick();
            BoardView board = find(panel[0], BoardView.class, b -> true);
            click(board, "e2");
            click(board, "e4");
            panel[0].onHide();
            panel[0].onShow();
        });
        Thread.sleep(REPLY_WAIT_MS);
        SwingUtilities.invokeAndWait(() -> assertTrue(
                find(panel[0], JButton.class, b -> b.getText().equals("다음 단계 →")).isEnabled(),
                "돌아온 뒤 흑의 응수가 반영되어 다음 단계로 갈 수 있어야 합니다."));
    }

    @Test
    void textOnGoldAccentStaysReadable(@TempDir Path tempDir) throws Exception {
        ChessBackend backend = new ChessBackend(new JsonStorage(tempDir));
        SwingUtilities.invokeAndWait(() -> {
            Theme.install();
            JButton activeTab = find(new LessonPanel(backend), JButton.class, b -> b.getText().equals("기본 튜토리얼"));
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

    private static void layoutTree(Container c) {
        c.doLayout();
        for (Component child : c.getComponents()) {
            if (child instanceof Container container) {
                layoutTree(container);
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
        for (Component c : root.getComponents()) {
            if (type.isInstance(c) && filter.test(type.cast(c))) {
                return type.cast(c);
            }
            if (c instanceof Container container) {
                T found = search(container, type, filter);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /** 뒤집지 않은 보드에서 칸 중앙을 클릭 (BoardView의 여백 10px 배치와 같은 계산). */
    private static void click(BoardView view, String square) {
        int size = Math.max(64, Math.min(view.getWidth(), view.getHeight()) - 20);
        int cell = size / 8;
        int x0 = (view.getWidth() - cell * 8) / 2;
        int y0 = (view.getHeight() - cell * 8) / 2;
        int sq = Board.parseSquare(square);
        Point p = new Point(x0 + Board.file(sq) * cell + cell / 2, y0 + (7 - Board.rank(sq)) * cell + cell / 2);
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_PRESSED, 0, 0, p.x, p.y, 1, false));
        view.dispatchEvent(new MouseEvent(view, MouseEvent.MOUSE_RELEASED, 0, 0, p.x, p.y, 1, false));
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
