# ChessMate 아키텍처 개요

전체 코드는 **단방향 3계층(`ui → model → engine`)**으로 분리되어 있습니다.  
`engine`과 `model`은 AWT/Swing을 일절 참조하지 않아 JUnit만으로 규칙·AI·학습 세션·파일 저장을 독립 검증할 수 있습니다.

---

## 1. 전체 클래스 구조도 (OOP 관계)

```mermaid
classDiagram
    direction TB

    class Main {
        +main(args)
    }

    namespace engine {
        class Piece {
            <<enum>>
            +Side side()
            +Type type()
            +fromFen(char)
        }
        class Move {
            <<record>>
            +int from
            +int to
            +Type promotion
            +uci()
            +fromUci(String)
        }
        class Board {
            -Piece[64] squares
            -Side turn
            -int castling
            -int enPassant
            -boolean sandbox
            +legalMoves()
            +applyMove(Move) Board
            +attackersOf(sq, side)
            +toFen()
        }
        class ChessGame {
            -Board startBoard
            -List~Entry~ history
            +play(Move)
            +undo(int)
            +status()
            +parseSan(Board, String)
        }
        class ChessAI {
            +chooseMove(Board, Difficulty)
            +baselineScore(Board)
            +evaluateMove(Board, Move, int)
            +findMateIn(Board, int)
            +bestDefenseAgainstMate(Board, int)
        }
        class Evaluator {
            <<interface>>
            +evaluate(Board) int
        }
    }

    namespace model {
        class AppStore {
            -List~Lesson~ lessons
            -List~Puzzle~ puzzles
            -List~Profile~ profiles
            -Settings settings
            +saveAndNotify()
        }
        class TrainingSession {
            <<abstract>>
            #Board currentBoard
            #boolean solved
            #int wrongCount
            #Move pendingReply
            +submitMove(Move)*
            +showHint()*
            +reset()*
            +applyPendingReply()
        }
        class LessonSession {
            -Lesson lesson
            -int stepIndex
            +nextStep()
            +prevStep()
        }
        class PuzzleSession {
            -Puzzle puzzle
            -Deque~Snapshot~ undoStack
            -int movesUsed
            +undo()
        }
        class Profile
        class Settings
        class Lesson
        class Puzzle
    }

    namespace ui {
        class Screen {
            <<interface>>
            +view() JComponent
            +onShow()
            +onHide()
            +onSettingsChanged()
            +leaveWarning()
        }
        class MainWindow {
            -Map~ScreenId, Screen~ screens
            +navigate(ScreenId)
        }
        class BoardView {
            -Board board
            -Map~Move, MoveEvaluation~ evalCache
            +setBoard(Board)
        }
        class AbstractGamePanel {
            <<abstract>>
            #ChessGame game
            #BoardView boardView
            #initLayout()
            #refreshBoardUi()
        }
        class AiGamePanel
        class LocalGamePanel
        class HomePanel
        class LessonPanel
        class PuzzlePanel
        class ProfilePanel
        class SettingsPanel
    }

    Main --> AppStore
    Main --> MainWindow
    ChessAI ..> Evaluator : 전략 사용
    ChessGame *-- Board
    Board --> Piece
    Board --> Move

    TrainingSession <|-- LessonSession
    TrainingSession <|-- PuzzleSession
    LessonSession --> Lesson
    PuzzleSession --> Puzzle
    PuzzleSession --> ChessAI
    AppStore *-- Profile
    AppStore *-- Settings
    AppStore *-- Lesson
    AppStore *-- Puzzle

    MainWindow o-- Screen : 다형 관리
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
    BoardView --> ChessAI : 호버 수 평가
```

### OOP 설계 포인트

| 개념 | 적용 위치 | 목적 |
| --- | --- | --- |
| **불변 객체 & 캡슐화** | `Board` | 64칸 배열을 외부에 노출하지 않고 착수마다 새 `Board` 반환 → AI 탐색·호버 프리뷰·무르기에서 부작용 원천 차단 |
| **인터페이스 · 다형성** | `Screen`, `ChessAI.Evaluator` | `MainWindow`가 7개 화면을 `Screen` 계약만으로 전환 / `ChessAI`가 난이도별 평가 전략(`MATERIAL`, `MATERIAL_PST`) 교체 |
| **상속 · 템플릿 메서드** | `TrainingSession` → `LessonSession` / `PuzzleSession`<br/>`AbstractGamePanel` → `AiGamePanel` / `LocalGamePanel` | 보드 훈련 공통 상태(피드백 사다리·자동 응수)와 대국 공통 UI(보드·기보·잡힌 기물) 중복 제거 |
| **관찰자 패턴** | `AppStore.addChangeListener(Runnable)` | 프로필 변경·점수 갱신·설정 변경 시 사이드바와 모든 화면에 즉시 전파 |

---

## 2. 핵심 로직 흐름 시각화

### A. 기물 선택 → 호버 시 수 평가 & 이후 전개 시각화

```mermaid
sequenceDiagram
    actor User as 사용자
    participant BV as BoardView
    participant AI as ChessAI
    participant B as Board (불변)

    User->>BV: 기물 클릭 (mousePressed)
    BV->>BV: selectedSquare 설정 및 합법 칸 표시
    User->>BV: 합법 이동 칸 위로 마우스 이동 (mouseMoved)
    alt 같은 칸에 머무름
        BV-->>BV: 즉시 반환 (연산 없음)
    else 새 합법 칸 진입
        BV->>AI: baselineScore(board) [국면당 최초 1회 캐시]
        BV->>AI: evaluateMove(board, move, base) [수별 evalCache]
        AI->>B: applyMove(move) → after 생성
        AI->>AI: 공격·체크·포크·수비선·방어 이탈 계산
        AI->>AI: 상대 최선 응수 탐색 (1-ply + 포획 정지 탐색)
        AI-->>BV: MoveEvaluation(등급, 이유, 상대 응수, 좌표들)
        BV->>BV: 7단계 오버레이 렌더링 (고스트·공격·수비·상대응수 화살표·평가 HUD)
    end
```

### B. 자유 메이트 퍼즐 (`mate=N`) 진행 흐름

```mermaid
flowchart TD
    Start["사용자 착수 submitMove(move)"] --> Legal{"합법 수인가?"}
    Legal -- 아니오 --> Wrong["WRONG 반환"]
    Legal -- 예 --> Apply["after = board.applyMove(move)"]
    Apply --> Mate{"after.isCheckmate()?"}
    Mate -- 예 --> Solved["SOLVED (즉시 정답 · 기록 저장)"]
    Mate -- 아니오 --> Draw{"스테일메이트 / 기물 부족?"}
    Draw -- 예 --> RejectDraw["WRONG (무승부 경고 · 국면 유지)"]
    Draw -- 아니오 --> Last{"남은 수 <= 1?"}
    Last -- 예 --> FailLast["WRONG (국면 유지 · 탈출 수 안내 + 피드백 사다리)"]
    Last -- 아니오 --> Commit["스냅샷 저장(undoStack) · 착수 확정 · movesUsed++"]
    Commit --> CheckForce["ChessAI.isForcedMateAfterMove(after, left)"]
    CheckForce --> Defense["ChessAI.bestDefenseAgainstMate(after, left)<br/>(탈출 우선, 불가 시 최장 지연 응수)"]
    Defense --> Timer["400ms 후 상대 응수 자동 반영<br/>(언제든 [무르기]로 복귀 가능)"]
```

### C. AI 대전 비동기 착수 흐름

```mermaid
sequenceDiagram
    actor User as 사용자
    participant Panel as AiGamePanel (EDT)
    participant Worker as SwingWorker (백그라운드)
    participant AI as ChessAI

    User->>Panel: 보드에서 착수
    Panel->>Panel: game.play(move) · 보드 입력 잠금 · "AI 생각 중..."
    Panel->>Worker: execute() (불변 Board 스냅샷 + AtomicBoolean cancel 전달)
    Worker->>AI: chooseMove(snapshot, difficulty, cancel::get)
    Note over AI: 반복 심화 + 알파-베타 + MVV-LVA<br/>(무르기/화면 이탈 시 cancel 플래그로 즉시 중단)
    AI-->>Worker: Optional<Move>
    Worker-->>Panel: done() (EDT 복귀)
    Panel->>Panel: cancel 미설정 시 game.play(reply) · 보드 갱신 · 종료 시 전적 저장
```

---

## 3. 데이터 포맷 & 저장 구조

모든 번들 콘텐츠와 사용자 기록은 외부 라이브러리 없는 **UTF-8 평문 `.txt`** (`---` 블록 구분, `key=value`)로 통일했습니다.

| 경로 | 용도 | 포맷 요약 |
| --- | --- | --- |
| `app/src/main/resources/data/lessons.txt` | 기본 튜토리얼 8개 + 유명 오프닝 6개 | `id`, `category`, `title`, `summary`, `side`, `setup`, `step=fen\|mode\|goal\|hints\|prompt\|success[\|reply]` |
| `app/src/main/resources/data/puzzles.txt` | 자유 메이트 10개 + 전술 퍼즐 6개 | `id`, `theme`, `mate`, `title`, `fen`, `solution`, `explain` |
| `~/.chessmate/profiles.txt` | 다중 프로필 및 학습/AI 기록 | `active=<id>` + 프로필별 `id`, `name`, `lessons`, `puzzle.<id>=시도,해결`, `ai.<난이도>=승,무,패` |
| `~/.chessmate/settings.txt` | 보드 테마 및 표시 옵션 | `boardTheme`, `showCoords`, `showLegalMoves`, `highlightLastMove`, `showMovePreview`, `autoFlipLocal` |
