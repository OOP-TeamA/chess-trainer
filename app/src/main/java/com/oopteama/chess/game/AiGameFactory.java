package com.oopteama.chess.game;

import com.oopteama.chess.ai.AlphaBetaSearch;
import com.oopteama.chess.ai.BlunderCheck;
import com.oopteama.chess.ai.Difficulty;
import com.oopteama.chess.ai.SearchLimits;
import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.FenFormatException;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.core.rules.StandardRules;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

/**
 * AiSetup 하나로 대국에 필요한 객체를 조립한다(테스트용, 계약 밖이라 패키지 전용).
 * 화면(AiGameScreen)은 명세 4.8의 생성자로 직접 조립한다. 화면에서도 쓰려면 명세에 추가한 뒤 공개한다.
 */
final class AiGameFactory {

    /** 실수 판정은 난이도와 무관하게 가장 좋은 평가 함수로. 임계값 200 = 기물 가치 2. */
    private static final int BLUNDER_THRESHOLD_CP = 200;

    record Session(Game game, GameController controller, HumanPlayer human, AiPlayer ai) { }

    private AiGameFactory() { }

    /** 시작 FEN이 잘못되었거나 표준 국면 검증에 실패하면 예외. start()는 호출하지 않는다. */
    static Session create(AiSetup setup, Async async, Random random) throws FenFormatException {
        Objects.requireNonNull(setup);
        Objects.requireNonNull(async);
        Objects.requireNonNull(random);

        StandardRules rules = new StandardRules();
        Position start = Fen.parse(setup.startFen());
        List<String> problems = rules.validate(start);
        if (!problems.isEmpty()) {
            throw new IllegalArgumentException("대국을 시작할 수 없는 국면: " + String.join(", ", problems));
        }

        Difficulty d = setup.difficulty();
        HumanPlayer human = new HumanPlayer();
        AiPlayer ai = new AiPlayer(
            new AlphaBetaSearch(rules, d.evaluator(), random), SearchLimits.of(d), async);

        Player white = setup.userSide() == Side.WHITE ? human : ai;
        Player black = setup.userSide() == Side.WHITE ? ai : human;

        Optional<BlunderCheck> blunders = setup.blunderAlert()
            ? Optional.of(new BlunderCheck(
                new AlphaBetaSearch(rules, Difficulty.ADVANCED.evaluator(), random), BLUNDER_THRESHOLD_CP))
            : Optional.empty();

        Game game = new Game(start, rules);
        GameController controller = new GameController(game, white, black, blunders, async);
        return new Session(game, controller, human, ai);
    }
}
