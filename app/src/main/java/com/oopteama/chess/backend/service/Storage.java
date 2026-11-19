package com.oopteama.chess.backend.service;

import com.oopteama.chess.backend.training.Lesson;
import com.oopteama.chess.backend.training.Puzzle;

import java.util.List;

/**
 * 콘텐츠 로드 및 사용자 데이터 영속화를 추상화한 저장소 계약입니다. (DIP / OCP)
 */
public interface Storage {

    record UserData(String activeProfileId, Settings settings, List<Profile> profiles) {
    }

    List<Lesson> loadLessons();

    List<Puzzle> loadPuzzles();

    UserData loadUserData();

    void saveUserData(UserData data);
}
