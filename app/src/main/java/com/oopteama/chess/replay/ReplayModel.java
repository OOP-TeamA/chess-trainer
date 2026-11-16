package com.oopteama.chess.replay;

import com.oopteama.chess.core.Fen;
import com.oopteama.chess.core.FenFormatException;
import com.oopteama.chess.core.Position;
import com.oopteama.chess.core.Side;
import com.oopteama.chess.notation.PgnGame;
import com.oopteama.chess.notation.PgnMove;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Swing 없는 기보 재생 로직(GAME-02, GAME-04). 화면은 이 모델을 그리기만 한다. EDT 전용이다.
 *
 * <p>ply는 지금까지 둔 반수의 개수다. 0이면 시작 국면, {@link #length()}면 마지막 국면이다.
 * 생성할 때 모든 국면을 미리 계산하므로 임의 위치로 이동한 뒤 앞뒤로 움직여도 국면이 일관된다.
 */
public final class ReplayModel {

    private final PgnGame game;
    /** positions[p] = p수를 둔 뒤의 국면. 길이는 수순 길이 + 1. */
    private final List<Position> positions;
    private final List<Runnable> listeners = new ArrayList<>();
    private int ply;

    /**
     * 기보의 모든 국면을 미리 계산한다. 수순은 {@code Pgn.read}가 합법성을 검증한 것이어야 한다.
     *
     * @param game 재생할 기보
     * @throws IllegalArgumentException 시작 국면 FEN이 잘못됐을 때
     */
    public ReplayModel(PgnGame game) {
        this.game = Objects.requireNonNull(game, "game");
        Position pos;
        try {
            pos = Fen.parse(game.startFen());
        } catch (FenFormatException e) {
            throw new IllegalArgumentException("기보 시작 국면 FEN 오류: " + e.getMessage(), e);
        }
        List<Position> all = new ArrayList<>(game.moves().size() + 1);
        all.add(pos.copy());
        for (PgnMove m : game.moves()) {
            pos.make(m.move());
            all.add(pos.copy());
        }
        this.positions = List.copyOf(all);
    }

    /**
     * 재생 중인 기보.
     *
     * @return 생성자에 넘긴 기보
     */
    public PgnGame game() {
        return game;
    }

    /**
     * 현재 위치.
     *
     * @return 0(시작 국면) 이상 {@link #length()} 이하
     */
    public int ply() {
        return ply;
    }

    /**
     * 수순의 길이(반수).
     *
     * @return 0 이상
     */
    public int length() {
        return game.moves().size();
    }

    /**
     * 지정한 위치로 이동한다. 위치가 바뀌었을 때만 리스너를 부른다.
     *
     * @param ply 0 이상 {@link #length()} 이하
     * @throws IllegalArgumentException 범위 밖일 때
     */
    public void seek(int ply) {
        if (ply < 0 || ply > length()) {
            throw new IllegalArgumentException("재생 위치는 0~%d입니다: %d".formatted(length(), ply));
        }
        if (ply == this.ply) {
            return;
        }
        this.ply = ply;
        for (Runnable r : List.copyOf(listeners)) {
            r.run();
        }
    }

    /**
     * 현재 국면의 사본. 고쳐도 모델에는 영향이 없다.
     *
     * @return 새 Position
     */
    public Position position() {
        return positions.get(ply).copy();
    }

    /** 지정한 위치에서 둘 차례인 진영. 위치를 옮기지 않고 대기 지점을 찾을 때 쓴다. */
    Side sideToMoveAt(int ply) {
        return positions.get(ply).sideToMove();
    }

    /**
     * 방금 둔 수. 마지막 수 강조와 해설 표시에 쓴다.
     *
     * @return 시작 국면이면 empty
     */
    public Optional<PgnMove> moveBefore() {
        return ply == 0 ? Optional.empty() : Optional.of(game.moves().get(ply - 1));
    }

    /**
     * 다음에 둘 수.
     *
     * @return 마지막 국면이면 empty
     */
    public Optional<PgnMove> moveAfter() {
        return ply == length() ? Optional.empty() : Optional.of(game.moves().get(ply));
    }

    /**
     * 위치가 바뀔 때마다 부를 리스너를 등록한다.
     *
     * @param r 리스너
     */
    public void addChangeListener(Runnable r) {
        listeners.add(Objects.requireNonNull(r, "r"));
    }

    /**
     * 등록한 리스너를 해제한다. 화면을 떠날 때 부른다. 등록되지 않은 리스너면 아무것도 하지 않는다.
     *
     * @param r 리스너
     */
    public void removeChangeListener(Runnable r) {
        listeners.remove(Objects.requireNonNull(r, "r"));
    }
}
