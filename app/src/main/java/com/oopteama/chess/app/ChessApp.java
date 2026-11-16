package com.oopteama.chess.app;

import com.oopteama.chess.io.DataPaths;
import com.oopteama.chess.ui.MainFrame;
import com.oopteama.chess.ui.Theme;

import javax.swing.SwingUtilities;
import java.io.IOException;
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

    private ChessApp() {
    }

    /**
     * 표준 사용자 폴더({@code ~/.chesslearn})로 앱을 시작한다.
     *
     * @param args 쓰지 않는다
     */
    public static void main(String[] args) {
        AppContext context;
        try {
            context = AppContext.load(DataPaths.standard());
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
}
