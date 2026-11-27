package com.oopteama.chess.ui.component;

import com.oopteama.chess.model.Position;

import java.awt.Point;

/**
 * 체스판이 화면 어디에 그려지는지(왼쪽 위 좌표, 칸 크기, 뒤집힘)를 담고 칸 ↔ 화면 좌표를 바꿉니다.
 * BoardPanel과 체스판 위에 덧그리는 부품(화살표, 수 설명)이 같은 계산을 씁니다.
 *
 * @param left    체스판 왼쪽 끝 x 좌표
 * @param top     체스판 위쪽 끝 y 좌표
 * @param cell    칸 한 변의 길이
 * @param flipped true면 흑이 아래쪽에 오도록 뒤집어 그림
 */
record BoardGeometry(int left, int top, int cell, boolean flipped) {

    /** 체스판 한 변의 길이. */
    int size() {
        return cell * 8;
    }

    /** 칸의 화면상 x 좌표 (왼쪽 위 모서리). */
    int x(Position square) {
        int screenCol = flipped ? 7 - square.col() : square.col();
        return left + screenCol * cell;
    }

    /** 칸의 화면상 y 좌표 (왼쪽 위 모서리). 흰 진영 기준으로 8랭크가 위쪽입니다. */
    int y(Position square) {
        int screenRow = flipped ? square.row() : 7 - square.row();
        return top + screenRow * cell;
    }

    double centerX(Position square) {
        return x(square) + cell / 2.0;
    }

    double centerY(Position square) {
        return y(square) + cell / 2.0;
    }

    /** 화면 좌표에 있는 칸 (체스판 밖이면 null). */
    Position positionAt(Point point) {
        if (point.x < left || point.y < top) {
            return null;
        }
        int screenCol = (point.x - left) / cell;
        int screenRow = (point.y - top) / cell;
        if (screenCol >= 8 || screenRow >= 8) {
            return null;
        }
        int col = flipped ? 7 - screenCol : screenCol;
        int row = flipped ? screenRow : 7 - screenRow;
        return new Position(col, row);
    }
}
