package com.oopteama.chess.api;

/** 체스판이 사용자 입력을 해석하는 방식. */
public enum BoardInputMode {
    /** 입력 잠금. */
    LOCKED,
    /** 기물 선택 후 도착 칸을 클릭해 수를 둔다. */
    MOVE,
    /** 칸 하나를 선택한다. */
    SQUARE
}
