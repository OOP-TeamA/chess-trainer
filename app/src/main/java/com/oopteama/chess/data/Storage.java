package com.oopteama.chess.data;

import com.oopteama.chess.training.Lesson;
import com.oopteama.chess.training.Puzzle;

import java.util.List;

/**
 * 데이터를 읽고 쓰는 방법을 정한 인터페이스입니다.
 * 실제 구현은 JsonStorage(JSON 파일)이고, 테스트에서는 메모리에만 저장하는 가짜 구현으로 바꿔 쓸 수 있습니다.
 */
public interface Storage {

    /** 저장되는 사용자 데이터 묶음. */
    record UserData(String activeProfileId, Settings settings, List<Profile> profiles) {
    }

    List<Lesson> loadLessons();

    List<Puzzle> loadPuzzles();

    UserData loadUserData();

    void saveUserData(UserData data);
}
