# 시연용 더미 기록 (로컬 전용)

프로토타입 시연에서 홈 화면 수치·추천·프로필이 채워진 상태로 앱을 띄우기 위한 사용자 데이터입니다.
실제 사용자 폴더(`~/.chesslearn`)는 건드리지 않습니다.

```sh
./gradlew :app:runDemo     # 더미 기록으로 실행
./gradlew :app:resetDemo   # 기록을 더미 원본 상태로 되돌림 (clean도 같은 효과)
```

`runDemo`는 `demo/userdata/`를 `app/build/demo-home/`에 **처음 한 번만** 복사하고,
`-Dchesslearn.home=app/build/demo-home`으로 앱을 실행합니다. 시연 중 바뀐 기록은 다음 실행에도 남으므로
"재실행 후 기록 유지"도 보여 줄 수 있습니다. 원본 `demo/userdata/`는 바뀌지 않습니다.

## 들어 있는 데이터

| 파일 | 내용 |
| --- | --- |
| `profiles/index.txt` | 프로필 3개, 활성 `demo`(민준) · `learner-2`(서연) · `learner-3`(지호) |
| `profiles/demo.txt` | 레슨 룩·나이트 완료(캐슬링 미완료 → 홈 추천), 퍼즐 8개 시도·6개 해결, 용어 퀴즈 20회·14회 정답, AI 입문 3승 1패·초급 1승 2패 1무, 기보 맞히기 2편 · 점수 57 |
| `profiles/learner-2.txt`, `learner-3.txt` | 순위 화면용 비교 기록(점수 90, 18) |
| `prefs.txt` | 기본 설정 |
| `saves/demo/*.txt` | 민준의 AI 대국 저장 PGN 2판 |

ID는 번들 콘텐츠(`app/src/main/resources/data/`)의 레슨·퍼즐·문항·기보 ID와 맞아야 합니다.
`app/src/test/java/com/oopteama/chess/app/DemoSeedTest.java`가 경고 없이 읽히는지와 ID 일치를 확인합니다.
