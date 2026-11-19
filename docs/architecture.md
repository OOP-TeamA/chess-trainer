# ChessMate 아키텍처 & SOLID 설계

## 1. 엄격한 Backend / Frontend 분리 원칙

| 경계 규칙 | 보장 방식 |
| --- | --- |
| **Backend 순수성** | `backend/**` 전체에서 `java.awt.*`, `javax.swing.*`, `com.formdev.*`, `frontend.*` import **0개** |
| **Frontend 순수성** | `frontend/**` 전체에서 `com.google.gson.*`, `JsonStorage`, `java.nio.file.*`, `new ChessAI()`, `new MoveAnalyzer()`, `new LessonSession()`, `new PuzzleSession()` **0개** |
| **단일 진입점 퍼사드** | 모든 프론트엔드 화면은 **`ChessBackend`**를 통해서만 세션 생성, AI 탐색, 수 평가, 프로필/설정 변경 및 저장을 수행 |
| **단일 조립 루트** | **`Main.java`** 한 곳에서만 `new JsonStorage(...) → new ChessBackend(storage) → new MainWindow(backend)`를 조립 |

---

## 2. SOLID 원칙 준수 요약

| 원칙 | 적용 위치 | 구체적 설계 |
| --- | --- | --- |
| **S (SRP)** | `ChessAI` vs `MoveAnalyzer`<br/>`JsonStorage` vs `ChessBackend`<br/>`BoardView` | 대국 탐색·메이트 솔버(`ChessAI`)와 후보 수 평가(`MoveAnalyzer`) 분리 / JSON 직렬화(`JsonStorage`)와 앱 상태 관리(`ChessBackend`) 분리 / `BoardView`는 렌더링·입력만 담당 |
| **O (OCP)** | `Storage`, `ChessAI.Evaluator`, `TrainingSession`, `Screen` | 저장소 교체, 평가 함수 확장, 신규 훈련 모드·화면 추가 시 기존 조율 코드 수정 불필요 |
| **L (LSP)** | `LessonSession`/`PuzzleSession` ⊂ `TrainingSession`<br/>`AiGamePanel`/`LocalGamePanel` ⊂ `AbstractGamePanel` | 모든 하위 클래스가 상위 추상 계약(`submitMove`, `showHint`, `reset`, `Screen`)을 예외 없이 대체 가능 |
| **I (ISP)** | `BoardView(Settings, BiFunction<Board, Move, Evaluation>)`<br/>`Storage`, `Screen` | `BoardView`는 전체 `ChessBackend` 대신 필요한 설정과 수 평가 콜백만 의존 |
| **D (DIP)** | `ChessBackend(Storage)`<br/>`PuzzleSession(Puzzle, ChessAI)`<br/>`BoardView(..., movePreviewer)` | 상위 정책이 구체 구현(`JsonStorage`, `MoveAnalyzer`) 대신 추상 계약(`Storage`, 함수형 콜백)에 의존 |

---

## 3. 전체 클래스 다이어그램

```mermaid
classDiagram
    direction TB

    class Main {
        +main(args)
    }

    namespace backend_engine {
        class Piece { <<enum>> }
        class Move { <<record>> }
        class Board {
            -Piece[64] squares
            +legalMoves()
            +applyMove(Move) Board
            +attackersOf(sq, side)
        }
        class ChessGame {
            +play(Move)
            +undo(int)
            +status()
        }
        class ChessAI {
            +chooseMove(Board, Difficulty, BooleanSupplier)
            +hint(Board)
            +findMateIn(Board, int)
            +bestDefenseAgainstMate(Board, int)
        }
        class Evaluator {
            <<interface>>
            +evaluate(Board) int
        }
        class MoveAnalyzer {
            +baselineScore(Board) int
            +evaluate(Board, Move, int) Evaluation
        }
    }

    namespace backend_training {
        class Lesson { <<record>> }
        class Puzzle { <<record>> }
        class TrainingSession {
            <<abstract>>
            +submitMove(Move)*
            +showHint()*
            +reset()*
            +applyPendingReply()
        }
        class LessonSession
        class PuzzleSession
    }

    namespace backend_service {
        class Storage {
            <<interface>>
            +loadLessons()
            +loadPuzzles()
            +loadUserData()
            +saveUserData(UserData)
        }
        class JsonStorage {
            -Path dataDir
            -Gson GSON
        }
        class ChessBackend {
            -Storage storage
            -ChessAI ai
            -MoveAnalyzer analyzer
            +startLesson(Lesson)
            +startPuzzle(Puzzle)
            +chooseAiMove(...)
            +evaluateMove(Board, Move)
            +completeLesson(id)
            +recordPuzzle(id, solved)
            +updateSettings(Consumer)
        }
        class Profile
        class Settings
    }

    namespace frontend {
        class Screen { <<interface>> }
        class MainWindow
        class BoardView
        class AbstractGamePanel { <<abstract>> }
        class AiGamePanel
        class LocalGamePanel
        class HomePanel
        class LessonPanel
        class PuzzlePanel
        class ProfilePanel
        class SettingsPanel
    }

    Main --> JsonStorage
    Main --> ChessBackend
    Main --> MainWindow

    Storage <|.. JsonStorage
    ChessBackend --> Storage : DIP
    ChessBackend *-- ChessAI
    ChessBackend *-- MoveAnalyzer
    ChessBackend *-- Profile
    ChessBackend *-- Settings
    ChessAI ..> Evaluator : 전략
    MoveAnalyzer --> Board
    ChessGame *-- Board
    Board --> Piece
    Board --> Move

    TrainingSession <|-- LessonSession
    TrainingSession <|-- PuzzleSession
    LessonSession --> Lesson
    PuzzleSession --> Puzzle
    PuzzleSession --> ChessAI : DI

    MainWindow o-- Screen
    Screen <|.. HomePanel
    Screen <|.. LessonPanel
    Screen <|.. PuzzlePanel
    Screen <|.. AbstractGamePanel
    Screen <|.. ProfilePanel
    Screen <|.. SettingsPanel
    AbstractGamePanel <|-- AiGamePanel
    AbstractGamePanel <|-- LocalGamePanel
    AbstractGamePanel *-- BoardView
    LessonPanel *-- BoardView
    PuzzlePanel *-- BoardView
    MainWindow --> ChessBackend
    BoardView ..> ChessBackend : "evaluateMove 콜백 주입"
```

---

## 4. 핵심 실행 흐름 시각화

### A. 호버 수 평가 & 이후 전개 시각화 (DIP 콜백 경유)

```mermaid
sequenceDiagram
    actor User as 사용자
    participant BV as frontend.BoardView
    participant BE as backend.ChessBackend
    participant MA as backend.MoveAnalyzer

    User->>BV: 기물 클릭 후 합법 이동 칸 위로 마우스 이동
    BV->>BE: movePreviewer.apply(board, move)
    BE->>MA: baselineScore(board) [WeakHashMap 캐시]
    BE->>MA: evaluate(board, move, base)
    MA-->>BE: Evaluation(Verdict, 공격·수비·방어이탈·상대최선응수·이유)
    BE-->>BV: Evaluation 반환 (BoardView evalCache 저장)
    BV->>BV: 7단계 오버레이 렌더링 (고스트·공격·수비·상대응수 화살표·자동회피 HUD)
```

### B. 자유 메이트 퍼즐 (`mate=N`) 흐름

```mermaid
flowchart TD
    Move["사용자 착수"] --> Mate{"체크메이트?"}
    Mate -- 예 --> Win["SOLVED (즉시 정답 · JSON 저장)"]
    Mate -- 아니오 --> Draw{"스테일메이트 / 기물 부족?"}
    Draw -- 예 --> Reject["WRONG (무승부 경고 · 국면 유지)"]
    Draw -- 아니오 --> Last{"남은 수 <= 1?"}
    Last -- 예 --> Fail["WRONG (탈출 수 SAN 안내 + 피드백 사다리)"]
    Last -- 아니오 --> Commit["undoStack 저장 · 착수 확정"]
    Commit --> Reply["ChessAI.bestDefenseAgainstMate()<br/>(탈출 우선, 불가 시 최장 지연 응수)"]
    Reply --> Apply["400ms 후 상대 응수 자동 반영 (언제든 무르기 가능)"]
```

---

## 5. JSON 데이터 스키마 (`Gson` 직렬화)

| 파일 | 위치 | 구조 |
| --- | --- | --- |
| `lessons.json` | `app/src/main/resources/data/` | `[{ id, category, title, summary, side, setup?, steps: [{ fen, sandbox, goal, hints[], prompt, success, reply? }] }]` |
| `puzzles.json` | `app/src/main/resources/data/` | `[{ id, theme, mate, title, fen, solution[], explain }]` |
| `userdata.json` | `~/.chessmate/` (`-Dchessmate.home`) | `{ activeProfileId, settings: { boardTheme, showCoords, showLegalMoves, highlightLastMove, showMovePreview, autoFlipLocal }, profiles: [{ id, name, completedLessons[], puzzleStats{}, aiRecords{} }] }` |
