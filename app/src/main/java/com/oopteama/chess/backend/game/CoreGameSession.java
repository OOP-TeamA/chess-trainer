package com.oopteama.chess.backend.game;

import com.oopteama.chess.api.BoardSnapshot;
import com.oopteama.chess.api.GameOutcomeView;
import com.oopteama.chess.api.GameSession;
import com.oopteama.chess.api.GameState;
import com.oopteama.chess.api.LegalMoveView;
import com.oopteama.chess.api.MoveAttemptResult;
import com.oopteama.chess.api.MoveInput;
import com.oopteama.chess.api.PieceKind;
import com.oopteama.chess.api.PieceView;
import com.oopteama.chess.api.SideColor;
import com.oopteama.chess.backend.core.BoardPosition;
import com.oopteama.chess.backend.core.BoardPosition.Piece;
import com.oopteama.chess.backend.core.BoardPosition.Side;
import com.oopteama.chess.backend.core.BoardPosition.Status;
import com.oopteama.chess.backend.core.ChessCore;
import com.oopteama.chess.backend.core.Move;
import com.oopteama.chess.backend.core.Move.MoveRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Consumer;

/**
 * {@link ChessCore}를 UI 계약 {@link GameSession}에 연결하는 백엔드 어댑터.
 */
public final class CoreGameSession implements GameSession {

    private final String startFen;
    private final ChessCore core;
    private final List<Consumer<GameState>> listeners = new ArrayList<>();

    private Side resignedBy;
    private GameState currentState;

    public CoreGameSession() {
        this(ChessCore.INITIAL_FEN);
    }

    public CoreGameSession(String startFen) {
        this.startFen = Objects.requireNonNull(startFen, "startFen");
        this.core = new ChessCore(startFen);
        this.currentState = snapshotState();
    }

    @Override
    public GameState state() {
        return currentState;
    }

    @Override
    public MoveAttemptResult playMove(MoveInput input) {
        Objects.requireNonNull(input, "input");
        if (currentState.result().isPresent()) {
            return MoveAttemptResult.rejected("이미 종료된 대국입니다.");
        }
        try {
            Move move = new Move(input.from(), input.to(), promotionChar(input.promotion()));
            core.play(move);
        } catch (IllegalArgumentException e) {
            return MoveAttemptResult.rejected(e.getMessage());
        }
        refreshAndNotify();
        return MoveAttemptResult.ok();
    }

    @Override
    public boolean undo() {
        if (!core.undo()) {
            return false;
        }
        resignedBy = null;
        refreshAndNotify();
        return true;
    }

    @Override
    public void resign() {
        if (currentState.result().isPresent()) {
            return;
        }
        resignedBy = core.position().turn();
        refreshAndNotify();
    }

    @Override
    public void newGame() {
        core.loadFen(startFen);
        resignedBy = null;
        refreshAndNotify();
    }

    @Override
    public void addListener(Consumer<GameState> listener) {
        Objects.requireNonNull(listener, "listener");
        for (Consumer<GameState> existing : listeners) {
            if (existing == listener) {
                return;
            }
        }
        listeners.add(listener);
    }

    @Override
    public void removeListener(Consumer<GameState> listener) {
        for (int i = 0; i < listeners.size(); i++) {
            if (listeners.get(i) == listener) {
                listeners.remove(i);
                return;
            }
        }
    }

    private void refreshAndNotify() {
        currentState = snapshotState();
        for (Consumer<GameState> listener : List.copyOf(listeners)) {
            listener.accept(currentState);
        }
    }

    private GameState snapshotState() {
        BoardPosition pos = core.position();
        Optional<GameOutcomeView> outcome = computeOutcome(pos);
        boolean inCheck = pos.isCheck();

        List<PieceView> squares = new ArrayList<>(BoardSnapshot.SQUARE_COUNT);
        for (int sq = 0; sq < BoardSnapshot.SQUARE_COUNT; sq++) {
            squares.add(toPieceView(pos.pieceAt(sq)));
        }

        OptionalInt checkSquare = inCheck
                ? pos.findKingSquare(pos.turn()).map(OptionalInt::of).orElse(OptionalInt.empty())
                : OptionalInt.empty();

        List<MoveRecord> history = core.history();
        Optional<BoardSnapshot.MoveRange> lastMove = history.isEmpty()
                ? Optional.empty()
                : Optional.of(new BoardSnapshot.MoveRange(
                        history.getLast().move().from(),
                        history.getLast().move().to()
                ));

        List<LegalMoveView> legalMoves = outcome.isPresent()
                ? List.of()
                : buildLegalMoves(pos);

        BoardSnapshot board = new BoardSnapshot(
                squares,
                toSideColor(pos.turn()),
                checkSquare,
                lastMove,
                legalMoves
        );

        return new GameState(
                board,
                core.sanHistory(),
                !history.isEmpty(),
                inCheck,
                outcome
        );
    }

    private Optional<GameOutcomeView> computeOutcome(BoardPosition pos) {
        if (resignedBy != null) {
            return Optional.of(new GameOutcomeView(
                    resignedBy == Side.WHITE ? GameOutcomeView.Winner.BLACK : GameOutcomeView.Winner.WHITE,
                    GameOutcomeView.EndReason.RESIGNATION
            ));
        }
        Status status = pos.status();
        if (status == Status.CHECKMATE) {
            return Optional.of(new GameOutcomeView(
                    pos.turn() == Side.WHITE ? GameOutcomeView.Winner.BLACK : GameOutcomeView.Winner.WHITE,
                    GameOutcomeView.EndReason.CHECKMATE
            ));
        }
        if (status == Status.STALEMATE) {
            return Optional.of(new GameOutcomeView(
                    GameOutcomeView.Winner.DRAW,
                    GameOutcomeView.EndReason.STALEMATE
            ));
        }
        if (pos.isInsufficientMaterial()) {
            return Optional.of(new GameOutcomeView(
                    GameOutcomeView.Winner.DRAW,
                    GameOutcomeView.EndReason.INSUFFICIENT_MATERIAL
            ));
        }
        if (pos.halfmoveClock() >= 100) {
            return Optional.of(new GameOutcomeView(
                    GameOutcomeView.Winner.DRAW,
                    GameOutcomeView.EndReason.FIFTY_MOVE
            ));
        }
        if (core.repetitionCount() >= 3) {
            return Optional.of(new GameOutcomeView(
                    GameOutcomeView.Winner.DRAW,
                    GameOutcomeView.EndReason.REPETITION
            ));
        }
        return Optional.empty();
    }

    private static List<LegalMoveView> buildLegalMoves(BoardPosition pos) {
        Map<Integer, LegalMoveView> deduplicated = new LinkedHashMap<>();
        for (Move move : pos.legalMoves()) {
            int key = (move.from() << 6) | move.to();
            boolean promotion = move.promotion() != '-';
            boolean capture = pos.pieceAt(move.to()) != Piece.EMPTY || isEnPassantCapture(pos, move);
            deduplicated.putIfAbsent(key, new LegalMoveView(move.from(), move.to(), capture, promotion));
        }
        return List.copyOf(deduplicated.values());
    }

    private static boolean isEnPassantCapture(BoardPosition pos, Move move) {
        return pos.pieceAt(move.from()).type() == 'P'
                && move.to() == pos.enPassantSquare()
                && pos.pieceAt(move.to()) == Piece.EMPTY
                && BoardPosition.file(move.from()) != BoardPosition.file(move.to());
    }

    private static SideColor toSideColor(Side side) {
        return side == Side.WHITE ? SideColor.WHITE : SideColor.BLACK;
    }

    private static PieceView toPieceView(Piece piece) {
        if (piece == Piece.EMPTY) {
            return null;
        }
        SideColor side = piece.belongsTo(Side.WHITE) ? SideColor.WHITE : SideColor.BLACK;
        PieceKind kind = switch (piece.type()) {
            case 'K' -> PieceKind.KING;
            case 'Q' -> PieceKind.QUEEN;
            case 'R' -> PieceKind.ROOK;
            case 'B' -> PieceKind.BISHOP;
            case 'N' -> PieceKind.KNIGHT;
            case 'P' -> PieceKind.PAWN;
            default -> throw new IllegalStateException("Unexpected piece type: " + piece);
        };
        return new PieceView(side, kind);
    }

    private static char promotionChar(Optional<PieceKind> promotion) {
        if (promotion.isEmpty()) {
            return '-';
        }
        return switch (promotion.get()) {
            case QUEEN -> 'Q';
            case ROOK -> 'R';
            case BISHOP -> 'B';
            case KNIGHT -> 'N';
            default -> throw new IllegalArgumentException("Invalid promotion piece: " + promotion.get());
        };
    }
}
