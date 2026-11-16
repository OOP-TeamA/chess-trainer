package com.oopteama.chess.replay;

import com.oopteama.chess.core.Move;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.notation.PgnMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 명국 기보 다음 수 맞히기(GAME-05, 인터페이스 명세 시퀀스 4). EDT 전용이다.
 *
 * <p>고른 진영의 차례(대기 지점)마다 멈춘다. 점수 측정이 목적이므로 피드백 사다리 없이 한 번 시도한 뒤
 * 바로 실제 수를 두고 해설을 공개한다(P2 예외). 이어서 {@link #continueToNext()}가 상대 수를 두고 다음 대기
 * 지점으로 간다. 세션이 {@link ReplayModel}의 위치를 움직이므로 화면은 모델 리스너로 보드를 다시 그린다.
 * 정답률은 화면이 끝난 뒤 {@code Progress.recordGuess(gameId, ratio())}로 한 번 기록한다.
 */
public final class GuessSession {

    private enum State { AWAITING, REVEALED, FINISHED }

    private final ReplayModel model;
    private final Side side;
    private final List<Integer> missed = new ArrayList<>();
    private State state;
    /** 대기 중이거나 방금 답한 수의 위치(모델 수순의 0부터 센 번호). */
    private int guessPly;
    private int attempts;
    private int correct;

    /**
     * 기보 처음부터 시작해 첫 대기 지점까지 상대 수를 둔다. 고른 진영의 수가 없으면 바로 끝난다.
     *
     * @param model 재생 모델
     * @param side  사용자가 맞힐 진영
     */
    public GuessSession(ReplayModel model, Side side) {
        this.model = Objects.requireNonNull(model, "model");
        this.side = Objects.requireNonNull(side, "side");
        advanceFrom(0);
    }

    /**
     * 사용자의 수를 기다리는지.
     *
     * @return 대기 지점에 멈춰 있고 아직 답하지 않았으면 true
     */
    public boolean awaitingGuess() {
        return state == State.AWAITING;
    }

    /**
     * 사용자 수를 실제 수와 비교하고 실제 수를 둔 뒤 멈춘다. 해설은 이 시점에 공개한다.
     *
     * @param move 대기 국면의 합법 수
     * @return 정답 여부와 실제 수
     * @throws IllegalStateException {@link #awaitingGuess()}가 아닐 때
     */
    public GuessResult guess(Move move) {
        Objects.requireNonNull(move, "move");
        PgnMove actual = requireAwaiting();
        return reveal(actual, actual.move().equals(move));
    }

    /**
     * 답하지 않고 실제 수를 본다. 오답으로 센다.
     *
     * @return 오답 결과와 실제 수
     * @throws IllegalStateException {@link #awaitingGuess()}가 아닐 때
     */
    public GuessResult giveUp() {
        return reveal(requireAwaiting(), false);
    }

    /**
     * 상대 수를 두고 다음 대기 지점으로 간다. 더 맞힐 수가 없으면 마지막 국면으로 가서 끝난다.
     *
     * @throws IllegalStateException 아직 답하지 않았을 때
     */
    public void continueToNext() {
        if (state == State.AWAITING) {
            throw new IllegalStateException("먼저 다음 수를 답하거나 포기해야 합니다");
        }
        if (state == State.FINISHED) {
            return;
        }
        advanceFrom(guessPly + 1);
    }

    /**
     * 모든 대기 지점을 지났는지.
     *
     * @return 끝났으면 true
     */
    public boolean finished() {
        return state == State.FINISHED;
    }

    /**
     * 답한 횟수(포기 포함).
     *
     * @return 0 이상
     */
    public int attempts() {
        return attempts;
    }

    /**
     * 맞힌 횟수.
     *
     * @return 0 이상 {@link #attempts()} 이하
     */
    public int correct() {
        return correct;
    }

    /**
     * 정답률.
     *
     * @return 맞힌 횟수 ÷ 답한 횟수(0~1). 답한 적이 없으면 0
     */
    public double ratio() {
        return attempts == 0 ? 0.0 : (double) correct / attempts;
    }

    /**
     * 틀리거나 포기한 수의 위치. 결과 화면의 "놓친 수" 목록에 쓴다.
     *
     * @return 기보 수순의 0부터 센 번호(답한 순서), 불변 목록
     */
    public List<Integer> missedPlies() {
        return List.copyOf(missed);
    }

    private PgnMove requireAwaiting() {
        if (state != State.AWAITING) {
            throw new IllegalStateException("다음 수를 기다리는 중이 아닙니다");
        }
        return model.game().moves().get(guessPly);
    }

    private GuessResult reveal(PgnMove actual, boolean right) {
        attempts++;
        if (right) {
            correct++;
        } else {
            missed.add(guessPly);
        }
        state = State.REVEALED;
        model.seek(guessPly + 1);
        return new GuessResult(right, actual);
    }

    /**
     * from 위치부터 고른 진영의 차례를 찾아 그 국면으로 한 번에 이동한다(화면은 한 번만 다시 그린다).
     * 없으면 마지막 국면으로 가서 끝낸다.
     */
    private void advanceFrom(int from) {
        for (int p = from; p < model.length(); p++) {
            if (model.sideToMoveAt(p) == side) {
                guessPly = p;
                state = State.AWAITING;
                model.seek(p);
                return;
            }
        }
        state = State.FINISHED;
        model.seek(model.length());
    }
}
