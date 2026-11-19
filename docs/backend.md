# Backend 패키지 상세 (`com.oopteama.chess.backend`)

백엔드는 AWT/Swing에 완전히 독립된 **16개 Java 클래스**로 구성되며 3개 하위 패키지로 역할을 엄격히 나눕니다.

---

## 1. `backend.engine` — 체스 규칙 · 탐색 · 수 평가 (6개)

| 파일 | 종류 | 핵심 책임 |
| --- | --- | --- |
| [`Piece.java`](../app/src/main/java/com/oopteama/chess/backend/engine/Piece.java) | `enum` | `Side`(WHITE/BLACK), `Type`(PAWN~KING 가치·기호), `Piece`(EMPTY, WP..BK) |
| [`Move.java`](../app/src/main/java/com/oopteama/chess/backend/engine/Move.java) | `record` | `Move(from, to, promotion)`, `Kind`, `Record(move, kind, moved, captured, check, checkmate, san)`, UCI 변환 |
| [`Board.java`](../app/src/main/java/com/oopteama/chess/backend/engine/Board.java) | `final class` | 불변 64칸 국면(`0=a1..63=h8`), FEN 표준/샌드박스 파싱·직렬화, 합법 수, 특수 규칙, `attackersOf`, `attackedEnemySquaresFrom` |
| [`ChessGame.java`](../app/src/main/java/com/oopteama/chess/backend/engine/ChessGame.java) | `final class` | 대국 세션(`startBoard`, `history`, `resignedSide`), `play`/`playUci`/`playSan`, `undo(n)`, 3회 반복·50수·기물 부족 판정, SAN 생성/파싱 |
| [`ChessAI.java`](../app/src/main/java/com/oopteama/chess/backend/engine/ChessAI.java) | `final class` | `Difficulty`(4단계), `Evaluator` 전략(`MATERIAL`/`MATERIAL_PST`), 알파-베타·반복 심화 탐색(`chooseMove`/`hint`), N수 강제 메이트 솔버(`findMateIn`/`isForcedMateAfterMove`/`bestDefenseAgainstMate`) |
| [`MoveAnalyzer.java`](../app/src/main/java/com/oopteama/chess/backend/engine/MoveAnalyzer.java) | `final class` | 후보 수 평가 전용 (`Verdict`, `Evaluation`, `baselineScore`, `evaluate`, 포획 정지 탐색 `quiescence`, `findNewlyExposedAllies` 방어 이탈 탐지, 한국어 이유 생성) |

### 핵심 알고리즘 요약
- **`Board` 불변성 & 공격 탐색**: `applyMove(move)`는 `squares.clone()` 위에 캐슬링·앙파상·승진·`cornerRight` 비트 제거를 적용해 새 인스턴스를 반환합니다. `isSquareAttacked` / `attackersOf`는 대상 칸 기준 역방향 탐색으로 O(1) 상한 내에 동작합니다.
- **`ChessAI` 대국 탐색 & 메이트 솔버**:
  - 대국 탐색: Negamax + Alpha-Beta + MVV-LVA 수 정렬 + 반복 심화 + `BooleanSupplier cancelled` 즉시 중단.
  - 메이트 솔버: AND-OR 트리(`canAttackerForceMate` ↔ `isForcedMateForOpponentToMove`) + 체크 우선 정렬 + 리프 `hasImmediateMate`로 3수 메이트를 밀리초 단위로 증명합니다.
- **`MoveAnalyzer` 수 평가**:
  - `baselineScore(before)`와 동일한 깊이(1-ply + 체크 회피 포함 3-ply 포획 정지 탐색)로 `evaluate(before, move, base)`를 계산해, 퀸 희생 메이트인2는 `GOOD`으로, 메이트 놓침(`Bxf7+`)은 `RISKY + Ke7 탈출`로, 공짜 기물 헌납·방어 이탈은 `BLUNDER`로 정확히 분류합니다.

---

## 2. `backend.training` — 레슨 · 퍼즐 · 훈련 세션 (5개)

| 파일 | 종류 | 핵심 책임 |
| --- | --- | --- |
| [`Lesson.java`](../app/src/main/java/com/oopteama/chess/backend/training/Lesson.java) | `record` | `Category`(TUTORIAL/OPENING), `Step(startBoard, goalSpec, hintSquares, prompt, successText, opponentReply)` 및 목표 매칭(`matches`/`primarySolution`) |
| [`Puzzle.java`](../app/src/main/java/com/oopteama/chess/backend/training/Puzzle.java) | `record` | `Puzzle(id, theme, title, startBoard, solution, mateIn, explanation)`, `isMatePuzzle()` |
| [`TrainingSession.java`](../app/src/main/java/com/oopteama/chess/backend/training/TrainingSession.java) | `abstract class` | 공통 상태(`currentBoard`, `solved`, `wrongCount`, `feedback`, `hintArrow`, `pendingReply`), 3단계 피드백 사다리, `applyPendingReply()` |
| [`LessonSession.java`](../app/src/main/java/com/oopteama/chess/backend/training/LessonSession.java) | `final class` | 단계 이동(`nextStep`/`prevStep`), `stepDone[]` 완료 추적, 오프닝 자동 응수 예약 |
| [`PuzzleSession.java`](../app/src/main/java/com/oopteama/chess/backend/training/PuzzleSession.java) | `final class` | DI로 `ChessAI` 주입. 자유 메이트(`mateIn > 0`) 자율 풀이 + AI 동적 방어 + `undoStack` 무르기 + 실시간 메이트 힌트 |

---

## 3. `backend.service` — 상태 · JSON 영속화 · 단일 퍼사드 (5개)

| 파일 | 종류 | 핵심 책임 |
| --- | --- | --- |
| [`Profile.java`](../app/src/main/java/com/oopteama/chess/backend/service/Profile.java) | `final class` | 완료 레슨 Set, 퍼즐 시도/정답 Map, 난이도별 AI 전적 Map, `totalScore()` (`레슨×10 + 퍼즐×5 + AI승×3`) |
| [`Settings.java`](../app/src/main/java/com/oopteama/chess/backend/service/Settings.java) | `final class` | `BoardTheme`(WALNUT/FOREST/SLATE RGB) 및 5개 옵션(`showCoords`, `showLegalMoves`, `highlightLastMove`, `showMovePreview`, `autoFlipLocal`) |
| [`Storage.java`](../app/src/main/java/com/oopteama/chess/backend/service/Storage.java) | `interface` | `UserData` 레코드와 `loadLessons`, `loadPuzzles`, `loadUserData`, `saveUserData` 영속 계약 (DIP) |
| [`JsonStorage.java`](../app/src/main/java/com/oopteama/chess/backend/service/JsonStorage.java) | `final class` | **Gson** 기반 `lessons.json`·`puzzles.json` 역직렬화 및 `~/.chessmate/userdata.json` 원자적(`.tmp` → `ATOMIC_MOVE`) 직렬화 |
| [`ChessBackend.java`](../app/src/main/java/com/oopteama/chess/backend/service/ChessBackend.java) | `final class` | 프론트엔드 전용 단일 퍼사드 — 콘텐츠 조회, 세션 생성, AI/수 평가(`baselineCache`), 프로필·설정 변경 + 자동 저장 + 관찰자 알림 |
