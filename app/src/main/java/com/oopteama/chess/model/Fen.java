package com.oopteama.chess.model;

import com.oopteama.chess.model.piece.King;
import com.oopteama.chess.model.piece.Pawn;
import com.oopteama.chess.model.piece.Piece;
import com.oopteama.chess.model.piece.Rook;

/**
 * FEN(국면 하나를 한 줄 문자열로 적는 표준 표기)을 읽고 씁니다.
 * 예) "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
 * 필드: 1) 기물 배치(8랭크부터) 2) 둘 차례 3) 캐슬링 권리 4) 앙파상 칸 5) 50수 규칙 반수 6) 수 번호
 */
public final class Fen {

    /** 표준 시작 국면. */
    public static final String START = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

    private Fen() {
    }

    /** 표준 국면을 읽습니다. 킹이 없거나 규칙에 맞지 않으면 예외가 납니다. */
    public static Board parse(String fen) {
        return parse(fen, false);
    }

    /** 킹이 없어도 되는 연습용(sandbox) 국면을 읽습니다. */
    public static Board parseSandbox(String fen) {
        return parse(fen, true);
    }

    /** 체스판을 FEN 문자열로 씁니다. */
    public static String format(Board board) {
        StringBuilder sb = new StringBuilder();
        for (int row = 7; row >= 0; row--) {
            int empty = 0;
            for (int col = 0; col < 8; col++) {
                Piece piece = board.pieceAt(new Position(col, row));
                if (piece == null) {
                    empty++;
                    continue;
                }
                if (empty > 0) {
                    sb.append(empty);
                    empty = 0;
                }
                sb.append(piece.fenChar());
            }
            if (empty > 0) {
                sb.append(empty);
            }
            if (row > 0) {
                sb.append('/');
            }
        }
        sb.append(board.turn() == Side.WHITE ? " w " : " b ");
        sb.append(castlingText(board.castlingRights()));
        sb.append(' ').append(board.enPassant() == null ? "-" : board.enPassant().toString());
        sb.append(' ').append(board.halfmoveClock());
        sb.append(' ').append(board.fullmoveNumber());
        return sb.toString();
    }

    private static Board parse(String fen, boolean sandbox) {
        if (fen == null || fen.isBlank()) {
            throw new IllegalArgumentException("FEN 문자열이 비어 있습니다.");
        }
        String[] fields = fen.trim().split("\\s+");
        if (fields.length != 6) {
            throw new IllegalArgumentException("FEN은 6개 필드가 필요합니다: " + fen);
        }
        Piece[][] grid = parsePlacement(fields[0]);
        Side turn = switch (fields[1]) {
            case "w" -> Side.WHITE;
            case "b" -> Side.BLACK;
            default -> throw new IllegalArgumentException("잘못된 FEN 차례 필드: " + fields[1]);
        };
        CastlingRights castling = parseCastling(fields[2]);
        Position enPassant = "-".equals(fields[3]) ? null : Position.of(fields[3]);
        int halfmove;
        int fullmove;
        try {
            halfmove = Integer.parseInt(fields[4]);
            fullmove = Integer.parseInt(fields[5]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("FEN 숫자 필드가 올바르지 않습니다: " + fen, e);
        }
        if (halfmove < 0 || fullmove < 1) {
            throw new IllegalArgumentException("FEN 반수·수 번호 범위 오류: " + fen);
        }
        Board board = new Board(grid, turn, castling, enPassant, halfmove, fullmove, sandbox);
        if (!sandbox) {
            validate(board);
        }
        return board;
    }

    private static Piece[][] parsePlacement(String placement) {
        String[] rows = placement.split("/", -1);
        if (rows.length != 8) {
            throw new IllegalArgumentException("FEN 배치에는 8개 행이 필요합니다: " + placement);
        }
        Piece[][] grid = new Piece[8][8];
        for (int i = 0; i < 8; i++) {
            int row = 7 - i; // FEN은 8랭크부터 적습니다.
            int col = 0;
            for (char c : rows[i].toCharArray()) {
                if (c >= '1' && c <= '8') {
                    col += c - '0';
                    continue;
                }
                if (col >= 8) {
                    throw new IllegalArgumentException("FEN 행 길이가 8을 넘습니다: " + rows[i]);
                }
                grid[row][col++] = Piece.fromFenChar(c);
            }
            if (col != 8) {
                throw new IllegalArgumentException("FEN 행 길이가 8이 아닙니다: " + rows[i]);
            }
        }
        return grid;
    }

    private static CastlingRights parseCastling(String field) {
        if ("-".equals(field)) {
            return CastlingRights.NONE;
        }
        for (int i = 0; i < field.length(); i++) {
            char c = field.charAt(i);
            if ("KQkq".indexOf(c) < 0 || field.indexOf(c) != i) {
                throw new IllegalArgumentException("잘못된 캐슬링 필드: " + field);
            }
        }
        return new CastlingRights(field.contains("K"), field.contains("Q"), field.contains("k"), field.contains("q"));
    }

    private static String castlingText(CastlingRights rights) {
        String text = (rights.whiteKingSide() ? "K" : "") + (rights.whiteQueenSide() ? "Q" : "")
                + (rights.blackKingSide() ? "k" : "") + (rights.blackQueenSide() ? "q" : "");
        return text.isEmpty() ? "-" : text;
    }

    /** 표준 국면이 체스 규칙에 맞는지 확인합니다. */
    private static void validate(Board board) {
        int whiteKings = 0;
        int blackKings = 0;
        for (Side side : Side.values()) {
            for (Position position : board.positionsOf(side)) {
                Piece piece = board.pieceAt(position);
                if (piece instanceof King) {
                    if (side == Side.WHITE) {
                        whiteKings++;
                    } else {
                        blackKings++;
                    }
                }
                if (piece instanceof Pawn && (position.row() == 0 || position.row() == 7)) {
                    throw new IllegalArgumentException("1랭크나 8랭크에는 폰을 둘 수 없습니다.");
                }
            }
        }
        if (whiteKings != 1 || blackKings != 1) {
            throw new IllegalArgumentException("표준 국면에는 백·흑 킹이 하나씩 있어야 합니다.");
        }
        if (Rules.isInCheck(board, board.turn().opposite())) {
            throw new IllegalArgumentException("상대 킹이 이미 체크 상태인 불법 국면입니다.");
        }

        // 캐슬링 권리가 있으면 킹과 룩이 처음 자리에 있어야 합니다.
        CastlingRights rights = board.castlingRights();
        for (Side side : Side.values()) {
            int row = side.homeRow();
            boolean kingHome = isPiece(board, new Position(4, row), King.class, side);
            if (rights.kingSide(side) && !(kingHome && isPiece(board, new Position(7, row), Rook.class, side))
                    || rights.queenSide(side) && !(kingHome && isPiece(board, new Position(0, row), Rook.class, side))) {
                throw new IllegalArgumentException("캐슬링 권리와 킹·룩 위치가 맞지 않습니다.");
            }
        }

        // 앙파상 칸은 상대 폰이 방금 두 칸 전진하며 건너뛴 칸이어야 합니다.
        Position ep = board.enPassant();
        if (ep != null) {
            Side mover = board.turn().opposite();
            int expectedRow = board.turn() == Side.WHITE ? 5 : 2;
            Position pawnSquare = ep.offset(0, mover.forward());
            Position startSquare = ep.offset(0, -mover.forward());
            boolean valid = ep.row() == expectedRow
                    && board.isEmpty(ep)
                    && pawnSquare != null && isPiece(board, pawnSquare, Pawn.class, mover)
                    && startSquare != null && board.isEmpty(startSquare);
            if (!valid) {
                throw new IllegalArgumentException("잘못된 앙파상 칸입니다: " + ep);
            }
        }
    }

    private static boolean isPiece(Board board, Position position, Class<? extends Piece> type, Side side) {
        Piece piece = board.pieceAt(position);
        return type.isInstance(piece) && piece.side() == side;
    }
}
