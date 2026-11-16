package com.oopteama.chess.app;

import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.ui.MainFrame;
import com.oopteama.chess.ui.Theme;

import javax.swing.SwingUtilities;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 앱 시작 순서(인터페이스 명세 시퀀스 5). 콘텐츠와 기록은 호출한 스레드(EDT가 아닌 main 스레드)에서 읽고,
 * 다 읽은 뒤 EDT에서 테마를 설치하고 메인 창을 만든다. 창을 닫을 때 학습 기록을 한 번 더 저장한다.
 */
public final class ChessApp {

    private static final Logger LOG = Logger.getLogger(ChessApp.class.getName());

    /** 시작에 실패했을 때의 종료 코드. */
    private static final int STARTUP_FAILURE = 1;

    /**
     * 사용자 폴더를 바꾸는 시스템 속성. 시연용 {@code :app:runDemo}가 저장소 안의 더미 기록 폴더를 넘길 때 쓴다.
     * 없으면 표준 폴더({@code ~/.chesslearn})를 쓴다.
     */
    static final String USER_DIR_PROPERTY = "chesslearn.home";

    private ChessApp() {
    }

    /**
     * 표준 사용자 폴더({@code ~/.chesslearn}), 또는 {@value #USER_DIR_PROPERTY} 속성의 폴더로 앱을 시작한다.
     *
     * @param args 쓰지 않는다
     */
    public static void main(String[] args) {
        AppContext context;
        try {
            context = AppContext.load(userPaths());
        } catch (IOException | RuntimeException e) {
            LOG.log(Level.SEVERE, "콘텐츠나 학습 기록을 읽지 못해 시작할 수 없습니다", e);
            System.err.println("Chess Trainer를 시작하지 못했습니다: " + e.getMessage());
            System.exit(STARTUP_FAILURE);
            return;
        }
        SwingUtilities.invokeLater(() -> {
            Theme.install();
            MainFrame frame = new MainFrame(context, context::saveProgress);
            frame.setVisible(true);
        });
    }

    private static DataPaths userPaths() {
        String dir = System.getProperty(USER_DIR_PROPERTY, "").strip();
        if (dir.isEmpty()) {
            return DataPaths.standard();
        }
        LOG.info("사용자 폴더를 %s 속성으로 바꿉니다: %s".formatted(USER_DIR_PROPERTY, dir));
        return new DataPaths(Path.of(dir));
    }
}
