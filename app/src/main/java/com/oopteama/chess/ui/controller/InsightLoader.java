package com.oopteama.chess.ui.controller;

import com.oopteama.chess.ai.MoveInsight;
import com.oopteama.chess.model.Board;
import com.oopteama.chess.model.Move;
import com.oopteama.chess.service.ChessBackend;

import javax.swing.SwingWorker;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 수 설명을 백그라운드 스레드에서 계산해 체스판 컨트롤러(BoardController)에 넘깁니다.
 * 퍼즐·AI 대전·로컬 대전 컨트롤러가 화면을 새로 그릴 때마다 update()를 부릅니다.
 * (계산에 시간이 걸려도 화면이 멈추지 않도록 SwingWorker를 씁니다.)
 */
final class InsightLoader {

    private final ChessBackend backend;
    private final BoardController board;
    private Board requested;                                     // 설명을 계산 중이거나 계산을 마친 국면
    private AtomicBoolean cancelFlag = new AtomicBoolean(false); // 진행 중인 계산을 멈추는 신호

    InsightLoader(ChessBackend backend, BoardController board) {
        this.backend = backend;
        this.board = board;
    }

    /**
     * position의 수 설명을 준비합니다. 설정이 꺼져 있거나 사람이 둘 차례가 아니면 설명을 지웁니다.
     *
     * @param humanToMove 사람이 지금 체스판에서 수를 둘 수 있으면 true
     */
    void update(Board position, boolean humanToMove) {
        if (!backend.settings().showMoveInsight() || !humanToMove) {
            clear();
            return;
        }
        if (position == requested) {
            return; // 같은 국면은 다시 계산하지 않습니다.
        }
        clear();
        requested = position;
        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancelFlag = cancelled;
        new SwingWorker<Map<Move, MoveInsight>, Void>() {
            @Override
            protected Map<Move, MoveInsight> doInBackground() {
                return backend.analyzeMoves(position, cancelled::get); // 백그라운드 스레드
            }

            @Override
            protected void done() { // 화면 스레드
                if (cancelled.get()) {
                    return;
                }
                try {
                    board.setInsights(position, get());
                } catch (Exception ignored) {
                    // 계산에 실패하면 설명 없이 둡니다.
                }
            }
        }.execute();
    }

    /** 진행 중인 계산을 멈추고 보여 주던 설명을 지웁니다. */
    void clear() {
        cancelFlag.set(true);
        requested = null;
        board.setInsights(null, Map.of());
    }
}
