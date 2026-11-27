# 화면: ui 패키지

Swing과 FlatLaf로 만든 화면입니다. **MVC** 구조로 역할을 나눴습니다. 전체 그림은 [전체 구조](architecture.md)에 있습니다.

| 역할 | 패키지 | 하는 일 | 예 |
| --- | --- | --- | --- |
| View (화면) | `ui.screen` | 컴포넌트를 배치하고, 받은 값을 보여 줌. 버튼은 getter로 내줌 | `lessonScreen.showStep(...)`, `lessonScreen.hintButton()` |
| Controller | `ui.controller` | 버튼·마우스 이벤트를 받아 `ChessBackend`와 세션을 부르고, 결과를 화면에 넘김 | `LessonController.onUserMove(move)` |
| Model | `model`, `training`, `service` | 규칙, 채점, 저장 | `LessonSession.submitMove(move)` |
| 부품 | `ui.component` | 여러 화면이 같이 쓰는 그리기 부품 | `BoardPanel`, `RetryDialog` |

화면(`ui.screen`)과 부품(`ui.component`)은 `ChessBackend`를 모릅니다. 판단은 모두 컨트롤러가 합니다.

## 1. 가장 짧은 예: 설정 화면

- [`SettingsScreen`](../app/src/main/java/com/oopteama/chess/ui/screen/SettingsScreen.java) (View): 테마 칸 3개와 체크박스 5개를 배치하고, `showSettings(settings, onPickTheme)`로 현재 값을 보여 줍니다.
- [`SettingsController`](../app/src/main/java/com/oopteama/chess/ui/controller/SettingsController.java) (Controller): 화면을 만들고 체크박스에 리스너를 겁니다.

```java
public final class SettingsController implements ScreenController {

    private final ChessBackend backend;
    private final SettingsScreen screen = new SettingsScreen();

    public SettingsController(ChessBackend backend) {
        this.backend = backend;
        screen.coordsCheck().addActionListener(e ->
                backend.updateSettings(s -> s.setShowCoords(screen.coordsCheck().isSelected())));
        // ... 나머지 체크박스 4개도 같은 방식
        refresh();
    }

    private void refresh() {
        screen.showSettings(backend.settings(), this::pickTheme);
    }
    ...
}
```

체크박스를 누르면: 컨트롤러가 `backend.updateSettings(...)` 호출 → 백엔드가 저장하고 변경을 알림 → `MainFrame`이 모든 컨트롤러의 `onDataChanged()`를 부름 → 체스판이 있는 화면이 바뀐 설정으로 다시 그려집니다.

## 2. 클래스 다이어그램

### 컨트롤러와 화면

```mermaid
classDiagram
    direction LR
    class MainFrame {
        -Map controllers
        -CardLayout cards
        +navigate(ScreenId)
    }
    class ScreenController {
        <<interface>>
        +view() JComponent
        +onShow()
        +onHide()
        +onDataChanged()
        +leaveWarning() String
    }
    class GameController {
        <<abstract>>
        #Game game
        #BoardController board
        #onBoardMove(Move)*
        #playerName(Side)* String
        #humanToMove()* boolean
        #refreshScreen()
    }
    class GameScreen {
        +boardPanel() BoardPanel
        +showPlayers(String, String, String, String)
        +showStatus(String, Color)
        +showMoveList(String)
    }
    MainFrame o-- ScreenController : 화면 7개
    ScreenController <|.. HomeController
    ScreenController <|.. LessonController
    ScreenController <|.. PuzzleController
    ScreenController <|.. GameController
    ScreenController <|.. ProfileController
    ScreenController <|.. SettingsController
    GameController <|-- AiGameController
    GameController <|-- LocalGameController
    GameScreen <|-- AiGameScreen
    GameScreen <|-- LocalGameScreen
    HomeController *-- HomeScreen
    LessonController *-- LessonScreen
    PuzzleController *-- PuzzleScreen
    GameController --> GameScreen : 공통 부분
    AiGameController *-- AiGameScreen
    LocalGameController *-- LocalGameScreen
    ProfileController *-- ProfileScreen
    SettingsController *-- SettingsScreen
```

### 체스판 부품

```mermaid
classDiagram
    direction LR
    class BoardController {
        -BoardPanel panel
        -boolean interactive
        -Side allowedSide
        +setMoveListener(MoveListener)
        +setIllegalMoveListener(IllegalMoveListener)
        +setInteractive(boolean)
        +setAllowedSide(Side)
        +setInsights(Board, Map)
    }
    class InsightLoader {
        +update(Board, boolean)
        +clear()
    }
    class MoveListener {
        <<interface>>
        +onMove(Move)
    }
    class IllegalMoveListener {
        <<interface>>
        +onIllegalMove(Position, Position)
    }
    class BoardPanel {
        -Board board
        -boolean flipped
        -Move lastMove
        -Position selected
        +setBoard(Board)
        +setFlipped(boolean)
        +setLastMove(Move)
        +setArrow(Move, Color)
        +setHighlights(List~Position~)
        +select(Position, List~Position~)
        +showInsight(MoveInsight)
        +positionAt(Point) Position
    }
    class MoveAnimator {
        -Deque~Animation~ queue
        -Timer timer
    }
    class PiecePainter {
        <<utility>>
        +draw(Graphics2D, Piece, int, int, int, float)$
    }
    class InsightPainter {
        <<utility>>
        +paint(Graphics2D, BoardGeometry, Board, MoveInsight, double, boolean)$
    }
    class ArrowPainter {
        <<utility>>
        +draw(Graphics2D, BoardGeometry, Position, Position, Color, float, double, boolean)$
    }
    class BoardGeometry {
        <<record>>
        +x(Position) int
        +y(Position) int
        +positionAt(Point) Position
    }
    class RetryDialog {
        <<utility>>
        +illegalMoveReason(Board, Position, Position, boolean)$ String
        +show(JComponent, String, String)$
    }
    BoardController --> BoardPanel : 마우스 리스너 등록
    BoardController --> MoveListener : 수를 알림
    BoardController --> IllegalMoveListener : 못 가는 칸 알림
    BoardPanel *-- MoveAnimator
    BoardPanel ..> PiecePainter
    BoardPanel ..> InsightPainter : 수 설명 그리기
    BoardPanel ..> ArrowPainter : 힌트 화살표
    InsightPainter ..> ArrowPainter
    BoardPanel ..> BoardGeometry : 칸 ↔ 화면 좌표
    InsightLoader --> BoardController : 수 설명 전달
    LessonScreen *-- BoardPanel
    PuzzleScreen *-- BoardPanel
    GameScreen *-- BoardPanel
```

## 3. 클래스 목록

### `ui` (3개)

| 클래스 | 하는 일 |
| --- | --- |
| `MainFrame` | 메인 창. 왼쪽 메뉴와 프로필 카드, 오른쪽은 `CardLayout`으로 화면 7개 중 하나를 보여 줌. 창을 닫을 때 저장 |
| `ScreenId` | 화면 목록 enum (홈, 레슨, 퍼즐, AI 대전, 로컬 대전, 프로필, 설정)과 메뉴 이름 |
| `Theme` | FlatLaf 어두운 테마 설치, Pretendard 글꼴, 색 상수, 카드·배지·버튼을 만드는 메서드(`card()`, `badge()`, `primaryButton()` …) |

### `ui.screen` (8개, View)

| 클래스 | 보여 주는 것 |
| --- | --- |
| `HomeScreen` | 인사말, 점수, 레슨·퍼즐·AI 전적 요약, 추천 카드, 모드 바로가기 4개 |
| `LessonScreen` | 튜토리얼·오프닝 탭, 레슨 목록(접기 가능), 체스판, 단계 안내·진행 막대·피드백, 힌트·다시 놓기·이전·다음 버튼 |
| `PuzzleScreen` | 필터, 퍼즐 목록, 체스판, 퍼즐 제목·차례·남은 수, 해설, 힌트·무르기·다시 풀기·다음 퍼즐 버튼 |
| `GameScreen` | 대국 화면의 부모: 위아래 선수 띠(잡은 기물 포함), 체스판, 상태 배너, 기보 |
| `AiGameScreen` | `GameScreen` + 난이도·진영 선택, 새 대국·힌트·무르기·보드 뒤집기·기권 버튼 |
| `LocalGameScreen` | `GameScreen` + 자동 뒤집기 표시, 무르기·보드 뒤집기·기권·새 대국 버튼 |
| `ProfileScreen` | 프로필 목록과 추가 입력칸, 현재 프로필 점수, 레슨·퍼즐 진도, 난이도별 AI 전적표, 기록 초기화 버튼 |
| `SettingsScreen` | 체스판 테마 3칸, 표시 옵션 체크박스 5개 |

### `ui.controller` (11개, Controller)

| 클래스 | 하는 일 |
| --- | --- |
| `ScreenController` | 모든 화면 컨트롤러의 인터페이스: `view()`, `onShow()`, `onHide()`, `onDataChanged()`, `leaveWarning()` |
| `BoardController` | 체스판 마우스 입력 처리, 갈 수 있는 칸에 마우스를 올리면 수 설명 보여 주기 (아래 4·5절) |
| `InsightLoader` | 수 설명을 `SwingWorker`로 백그라운드에서 계산해 `BoardController`에 넘김. 퍼즐·AI·로컬 대전 컨트롤러가 화면을 새로 그릴 때마다 부름 (아래 5절) |
| `HomeController` | 점수·진도 요약, 추천(안 끝낸 레슨 → 안 푼 퍼즐 → AI 대전 순서), 바로가기 버튼으로 화면 이동 |
| `LessonController` | 탭·레슨 선택, 단계 이동, 힌트, 오답 안내, 상대 응수(0.42초 뒤), 레슨 완료 기록 |
| `PuzzleController` | 필터, 퍼즐 선택, 힌트·무르기·다시 풀기·다음 퍼즐, 상대 응수(0.4초 뒤), 풀이 기록. 목록 제목은 답을 드러내지 않게 "05. 2수 안에 체크메이트"처럼 표시 |
| `GameController` | 대국 공통: `Game` 객체, 체스판 연결, 선수 띠·상태·기보 다시 그리기(`refreshScreen`), 보드 뒤집기, 진행 중 대국 이탈 경고 |
| `AiGameController` | 난이도·진영, `SwingWorker`로 AI 계산, 힌트 화살표, 무르기, 기권, 전적 기록. 화면을 떠나면 계산을 멈추고 돌아오면 이어서 둠 |
| `LocalGameController` | 두 사람 대국: 무르기, 기권, 새 대국, 차례마다 자동 뒤집기 |
| `ProfileController` | 프로필 추가·선택·삭제(마지막 하나는 삭제 불가), 기록 초기화 |
| `SettingsController` | 테마·체크박스 변경을 `backend.updateSettings(...)`로 저장 |

### `ui.component` (9개, 부품)

| 클래스 | 하는 일 |
| --- | --- |
| `BoardPanel` | 체스판 그리기 (아래 4절). 입력 처리는 하지 않음 |
| `BoardGeometry` | 체스판이 그려지는 위치·칸 크기·뒤집힘을 담고 칸 ↔ 화면 좌표를 바꿈 |
| `PiecePainter` | 기물을 이미지 파일 없이 도형(`Path2D`)으로 그림 |
| `ArrowPainter` | 화살표 그리기 (힌트 화살표, 수 설명 화살표). 뻗어 나가는 애니메이션과 점선 지원 |
| `InsightPainter` | 수 설명 애니메이션: 도착 칸 빛남, 기물 미리보기, 이유 화살표, 상대 응수 재생, 설명 카드 (아래 5절) |
| `MoveAnimator` | 기물이 미끄러지는 이동 애니메이션 (아래 4절) |
| `RetryDialog` | 못 가는 칸으로 옮기려 할 때 이유를 알려 주는 안내 카드 |
| `CollapsibleList` | 접고 펼 수 있는 목록 (레슨·퍼즐 목록) |
| `RoundedPanel` | 모서리가 둥근 카드 패널 (마우스를 올리면 색이 바뀌는 버전 포함) |

## 4. 체스판: `BoardPanel` + `BoardController` + `MoveAnimator`

체스판은 **그리기(`BoardPanel`)** 와 **입력(`BoardController`)** 을 나눴습니다. 레슨·퍼즐·AI 대전·로컬 대전이 같은 두 클래스를 씁니다.

### 그리는 순서 (`BoardPanel.paintComponent`)

1. 테두리
2. 64칸: 테마 색 → 직전 수 강조 → 힌트 칸 강조 → 선택한 칸 → 체크당한 킹(빨강) → 오답 칸(잠깐 빨강)
3. 좌표 a~h, 1~8 (설정에서 켰을 때)
4. 이동 가능 칸: 빈칸은 점, 잡을 수 있는 칸은 고리 (설정에서 켰을 때)
5. 기물 (`PiecePainter`)
6. 움직이는 중인 기물 (`MoveAnimator`)
7. 힌트 화살표 (`ArrowPainter`)
8. 수 설명 애니메이션 (`InsightPainter`, 갈 수 있는 칸에 마우스를 올렸을 때)
9. 드래그 중인 기물 (마우스 위치)

### 마우스 입력 규칙 (`BoardController`)

| 사용자 동작 | 결과 |
| --- | --- |
| 내 기물 클릭 | 선택하고 갈 수 있는 칸을 표시 |
| 갈 수 있는 칸 클릭 | 수를 둠 → `MoveListener.onMove(move)` |
| 다른 내 기물 클릭 | 그 기물로 선택을 바꿈 (움직일 수 없는 기물이면 선택만 풀림) |
| 선택한 기물을 다시 클릭 | 선택 해제 |
| 갈 수 없는 칸 클릭 | 선택은 그대로 두고 `IllegalMoveListener`에 알림 → 레슨·퍼즐은 `RetryDialog`로 이유 안내 |
| 기물을 끌어서 놓기 | 수를 둠 (놓은 칸이 갈 수 없는 칸이면 위와 같이 안내) |
| 킹을 고른 뒤 같은 편 룩 클릭 | 캐슬링 |
| 폰이 끝 줄에 도착 | 퀸·룩·비숍·나이트 중 고르는 창 |
| 기물을 고르거나 끄는 중에 갈 수 있는 칸 위로 마우스 이동 | 그 수의 설명을 애니메이션으로 보여 줌 (설정에서 켰을 때, 아래 5절) |

컨트롤러는 `setInteractive(false)`로 상대 응수를 기다리는 동안 입력을 막고, AI 대전에서는 `setAllowedSide(사람 진영)`으로 내 기물만 움직이게 합니다.

`RetryDialog.illegalMoveReason(...)`이 알려 주는 이유: 도착 칸에 아군 기물이 있음, 룩·비숍·퀸이 갈 수 없는 방향, 가는 길을 막은 기물, 그 밖에는 "e2에서 e5로는 이 국면에서 이동할 수 없어요."처럼 일반 안내(칸 이름에 맞춰 '로'·'으로'를 붙임).

### 이동 애니메이션 (`MoveAnimator`)

- `setBoard(새 국면)`을 받으면, 이전 국면에서 **합법 수 하나로** 올 수 있는 국면인지 찾습니다. 맞으면 그 기물을 0.25초 동안 미끄러지게 그립니다.
- 캐슬링은 킹과 룩이 함께 움직이고, 잡힌 기물(앙파상 포함)은 제자리에서 서서히 사라집니다.
- 드래그로 놓은 수는 이미 도착 칸에 있으므로 다시 움직이지 않습니다.
- 내 수가 끝나기 전에 상대 응수가 오면 이어서 재생합니다. 밀린 수가 더 쌓이면 마지막 수만 재생합니다.
- 무르기·새 대국처럼 한 수로 설명되지 않는 변화는 바로 표시합니다.
- 컨트롤러는 애니메이션을 신경 쓰지 않고 `setBoard()`만 부르면 됩니다.

## 5. 수 설명: 갈 수 있는 칸에 마우스를 올렸을 때

퍼즐·AI 대전·로컬 대전에서 기물을 고른 뒤(또는 끄는 중에) 갈 수 있는 칸에 마우스를 올리면, 그 수가 왜 좋은지·나쁜지를 체스판 위에 애니메이션으로 보여 줍니다. 설정의 "수 설명" 체크박스로 끄고 켭니다(기본 켜짐). 레슨은 단계별 안내가 따로 있어 쓰지 않습니다.

```mermaid
sequenceDiagram
    actor U as 사용자
    participant C as 컨트롤러 (퍼즐·AI·로컬)
    participant IL as InsightLoader
    participant BE as ChessBackend
    participant BC as BoardController
    participant BP as BoardPanel

    C->>IL: update(국면, 사람 차례인지) [화면을 새로 그릴 때마다]
    IL->>BE: analyzeMoves(국면, 취소 신호) [SwingWorker 백그라운드 스레드]
    BE-->>IL: 수마다 MoveInsight (판정·이유·예상 응수)
    IL->>BC: setInsights(국면, 설명들)
    U->>BC: 기물 선택 → 갈 수 있는 칸 위로 마우스 이동
    BC->>BP: showInsight(그 수의 설명)
    loop 설명이 보이는 동안 20ms마다
        BP->>BP: InsightPainter.paint(지난 시간)
    end
    U->>BC: 다른 칸으로 이동하거나 수를 둠
    BC->>BP: showInsight(null)
```

- **미리 계산**: 국면이 바뀌면 그 국면의 모든 수 설명을 백그라운드에서 한 번에 계산해 둡니다(보통 0.1~0.6초). 마우스를 올릴 때는 계산하지 않고 찾아서 보여 주기만 하므로 바로 나타납니다. 같은 국면이면 다시 계산하지 않고, 국면이 바뀌면 진행 중인 계산을 취소합니다.
- **사람 차례일 때만**: AI가 생각 중이거나 퍼즐의 상대 응수를 기다리는 중, 대국이 끝난 뒤에는 설명을 지웁니다. 컨트롤러마다 `humanToMove()`(퍼즐은 `canUserMove()`)로 알려 줍니다.
- **늦게 도착한 설명 무시**: `BoardController`는 설명이 어느 국면(`Board`) 것인지 함께 받아, 지금 체스판의 국면과 같을 때만 씁니다.

시간에 따라 그리는 것 (`InsightPainter`):

| 시각 | 그리는 것 |
| --- | --- |
| 0초~ | 도착 칸이 판정 색(최선 초록·좋음 파랑·아쉬움 주황·나쁨 빨강)으로 숨 쉬듯 빛나고 물결 고리가 퍼짐. 움직일 기물의 반투명 미리보기가 도착 칸으로 미끄러짐 |
| 0.3초 | 잡는 수면 도착 칸에서 고리가 터지며 "+점수"가 떠오름. 승진이면 미리보기가 승진한 기물로 바뀜 |
| 0.3초~ (0.22초 간격) | 이유마다 화살표가 뻗어 나가거나(초록: 좋은 점, 빨강: 나쁜 점) 칸에 고리가 생기고, 설명 카드에 그 이유 글이 함께 나타남 |
| 0.9초~ (2.2초마다 반복) | 좋지 않은 수면 상대의 예상 응수: 점선 화살표 → 상대 기물 미리보기가 이동 → 그 칸이 빨갛게 번쩍이고, 내 기물을 잡으면 "−점수"가 떠오름 |

설명 카드는 체스판 위쪽과 아래쪽 중 중요한 칸(도착 칸, 이유·응수 칸, 출발 칸 순으로 무겁게 셈)을 덜 가리는 쪽에 나타납니다.

## 6. 화면 전환 (`MainFrame`)

- 생성자에서 컨트롤러 7개를 만들어 `Map<ScreenId, ScreenController>`에 넣고, 각 컨트롤러의 `view()`를 `CardLayout`에 붙입니다.
- `navigate(ScreenId)`: 지금 화면의 `leaveWarning()`이 문구를 돌려주면(진행 중인 대국) 확인 창을 띄우고, 확인하면 지금 화면 `onHide()` → 새 화면 표시 → 새 화면 `onShow()` 순서로 부릅니다. ([흐름도](architecture.md#d-화면-전환))
- `ChessBackend`에 변경 리스너를 등록해, 데이터가 바뀌면 왼쪽 프로필 카드와 모든 화면의 `onDataChanged()`를 부릅니다.
- 창을 닫으면 진행 중인 대국을 확인한 뒤 `backend.save()`를 부르고 닫습니다.

## 7. 새 화면을 추가하는 방법

예: "통계" 화면을 추가한다면

1. `ScreenId`에 항목 추가: `STATS("통계")`
2. `ui/screen/StatsScreen.java`: `JPanel`을 상속하고, 보여 줄 값을 받는 `showXxx(...)` 메서드를 만듭니다.
3. `ui/controller/StatsController.java`: `ScreenController`를 구현합니다. 생성자에서 화면을 만들고 버튼에 리스너를 겁니다. `onShow()`에서 `backend`의 값을 읽어 화면에 넘깁니다.
4. `MainFrame` 생성자에 `register(ScreenId.STATS, new StatsController(backend));` 한 줄을 넣고, `buildSidebar()`의 메뉴 배열에 `ScreenId.STATS`를 추가합니다.

`MainFrame`은 `ScreenController` 인터페이스로만 화면을 다루므로, 새 화면을 위해 화면 전환 코드를 고칠 필요가 없습니다.

## 8. 화면 테스트

화면 테스트도 창을 띄우지 않고 실행됩니다.

| 테스트 | 확인하는 것 |
| --- | --- |
| `UiSmokeTest` | 7개 화면과 체스판 테마 3종이 오류 없이 그려지는지 |
| `BoardControllerTest` | 클릭 선택·해제, 아군 기물 클릭, 갈 수 없는 칸 클릭 후 선택 유지, 드래그, 수 설명 호버(고른 뒤·끄는 중, 다른 국면의 설명은 무시) |
| `InsightPainterTest` | 수 설명 애니메이션이 시간대마다 오류 없이 그려지고, 나쁜 수의 도착 칸이 빨갛게 빛나는지 |
| `BoardPanelAnimationTest` | 클릭한 수는 미끄러지고 드래그한 수는 바로 놓임, 응수 대기열, 캐슬링·앙파상·승진 애니메이션 |
| `RetryDialogTest` | 못 가는 이유 문구 (아군 기물, 방향, 막힌 길, '로'·'으로') |
| `TrainingControllerTest` | 레슨·퍼즐에서 응수를 기다리는 중 화면을 떠났다 와도 응수가 반영되는지, 금색 위 글자 대비(4.5:1 이상), 목록 접기 버튼 모양 |
| `AiGameControllerTest` | AI가 생각하는 중 화면을 떠났다 와도 AI가 이어서 두는지, 기권 전적이 한 번만 기록되는지 |
