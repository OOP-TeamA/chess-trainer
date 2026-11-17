package com.oopteama.chess.app;

import com.oopteama.chess.api.AppServices;
import com.oopteama.chess.api.GameSession;
import com.oopteama.chess.backend.game.CoreGameSession;

/**
 * 객체 조립의 유일한 장소(Composition Root).
 * UI가 요구하는 {@link AppServices} 계약을 백엔드 구현체와 연결해 주입한다.
 */
public final class AppContext implements AppServices {

    @Override
    public GameSession createLocalGame() {
        return new CoreGameSession();
    }
}
