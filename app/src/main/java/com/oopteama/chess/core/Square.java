package com.oopteama.chess.core;

import java.util.Optional;

/**
 * 좌표계: file 0 = a, rank 0 = 1. 범위 밖 좌표는 생성 시 IllegalArgumentException.
 */
public record Square(int file, int rank) {

    public Square {
        if (file < 0 || file > 7 || rank < 0 || rank > 7) {
            throw new IllegalArgumentException("좌표 범위 밖: file=" + file + ", rank=" + rank);
        }
    }

    /** "e4" 형식 문자열 파싱. */
    public static Square of(String name) {
        if (name == null || name.length() != 2) {
            throw new IllegalArgumentException("잘못된 칸 이름: " + name);
        }
        int file = name.charAt(0) - 'a';
        int rank = name.charAt(1) - '1';
        return new Square(file, rank);
    }

    /** rank * 8 + file 형식의 0~63 인덱스를 칸으로 바꾼다. 범위 밖이면 IllegalArgumentException. */
    public static Square ofIndex(int index) {
        return new Square(index % 8, index / 8);
    }

    /** rank * 8 + file. */
    public int index() {
        return rank * 8 + file;
    }

    /** "e4" 형식의 칸 이름. */
    public String name() {
        return "" + (char) ('a' + file) + (rank + 1);
    }

    /** 범위를 벗어나면 Optional.empty(). */
    public Optional<Square> offset(int dFile, int dRank) {
        int nf = file + dFile;
        int nr = rank + dRank;
        if (nf < 0 || nf > 7 || nr < 0 || nr > 7) {
            return Optional.empty();
        }
        return Optional.of(new Square(nf, nr));
    }

    /** (file + rank)가 홀수면 밝은 칸. */
    public boolean isLight() {
        return (file + rank) % 2 != 0;
    }
}

