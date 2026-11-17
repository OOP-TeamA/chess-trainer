# Core (`com.oopteama.chess.backend.core`)

체스 규칙 엔진·국면 표현·기보/FEN 직렬화를 담당하는 독립 코어 패키지입니다.  
총 4개의 Java 파일로 구성되며, 외부 모듈은 이 패키지의 public API를 통해 불변 국면을 조회하고 합법 수를 진행합니다.

## 파일 구성 요약

| 파일 | 종류 | 핵심 책임 |
| --- | --- | --- |
| [`ChessCore.java`](./ChessCore.java) | `final class` | 대국 시작 국면과 수순 이력 보관, 수 입력(`play`/`playUci`), 무르기(`undo`), 3회 반복 판정(`repetitionCount`) |
| [`BoardPosition.java`](./BoardPosition.java) | `final class` | 한 순간의 64칸 국면 스냅샷, `Side`/`Piece`/`Status` enum, 합법 수·체크·기물 부족 규칙, 가상 착수(`simulateLegalMove`), 좌표 유틸 |
| [`ChessNotation.java`](./ChessNotation.java) | `final class` | FEN 문자열 파싱·규칙 검증·직렬화, 반복 판정 키 생성, SAN 기보 해석 및 `MoveRecord` 생성 |
| [`Move.java`](./Move.java) | `record` | 이동 좌표(`from`, `to`, `promotion`), UCI 변환, 중첩 `MoveKind`·`MoveRecord` |

```java
import com.oopteama.chess.backend.core.BoardPosition;
import com.oopteama.chess.backend.core.ChessCore;
import com.oopteama.chess.backend.core.Move;
import com.oopteama.chess.backend.core.Move.MoveRecord;
import java.util.List;

ChessCore core = new ChessCore();
BoardPosition before = core.position();
MoveRecord played = core.play("e4");
List<Move> replies = core.position().legalMoves();
core.undo();
```

### 공통 설계 원칙

- **국면 스냅샷의 불변성**: 외부에 반환하는 `BoardPosition`에는 보드 변경 메서드가 없고, 생성자(`Piece[]`)와 `currentBoard()` 모두 배열을 새로 복사해서 넘깁니다. 대국을 진행하거나 무르기를 해도 이전에 받아 둔 `BoardPosition`은 절대 바뀌지 않습니다.
- **실패 원자성**: 잘못된 수·FEN·SAN·수순을 입력하면 예외를 던지고, 기존 `start`와 `history`는 전혀 건드리지 않습니다.
- **이력과 계산의 분리**: `BoardPosition`은 수순 이력을 갖지 않아 AI 탐색 같은 가상 수 시뮬레이션에 그대로 쓸 수 있고, `ChessCore`만 대국 전체 수순을 보관합니다.

---

## 1. `ChessCore`

대국 한 판의 흐름(시작 국면, 둔 수들의 기록, 무르기, 반복 횟수)을 관리합니다.

### 상수·내부 타입·필드

- **`public static final String INITIAL_FEN`**  
  표준 체스 초기 배치 문자열 (`"rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"`). 기본 생성자나 새 대국을 시작할 때 씁니다.
- **`private record HistoryEntry(MoveRecord move, BoardPosition position)`**  
  한 수(half-move)의 기록(`MoveRecord`)과 그 수를 둔 직후 국면(`BoardPosition`)을 짝지어 저장하는 내부 엔트리입니다. 매 수의 국면을 같이 보관하므로 무르기를 할 때 처음부터 다시 재생하지 않고 마지막 엔트리만 꺼내면 되며, 3회 반복 검사도 이 국면들을 훑어 바로 셀 수 있습니다.
- **`private BoardPosition start`**  
  현재 대국의 출발 국면입니다. 기본 체스가 아니라 퍼즐이나 특정 상황부터 시작한 경우 그 시작 국면을 담습니다.
- **`private final List<HistoryEntry> history`**  
  `start` 이후 실제로 둔 수들을 순서대로 쌓아 두는 리스트입니다.

### 생성자

- **`ChessCore()`**: `INITIAL_FEN`으로 표준 새 대국을 시작합니다.
- **`ChessCore(String fen)`**: `loadFen(fen)`을 호출해 원하는 배치(퍼즐, 연습 문제 등)에서 대국을 시작합니다.
- **`ChessCore(String fen, List<String> sanSequence)`**: `loadGame(fen, sanSequence)`를 호출해 시작 배치부터 SAN 수순까지 전부 검증·재생한 상태로 대국을 시작합니다.

### 함수 상세 설명 및 예상 사용처

#### `position()` / `initialFen()` / `history()` / `sanHistory()`
- **`position()`**: `history`가 비어 있으면 `start`, 아니면 `history.getLast().position()`을 반환합니다. 현재 보드를 화면에 그리거나, 지금 누구 차례인지·체크인지 확인하거나, AI에게 현재 국면을 넘길 때 항상 여기서 조회합니다.
- **`initialFen()`**: `start.toFen()`을 반환합니다. 중간 FEN부터 시작한 대국이나 기보를 저장할 때 출발점을 남기는 데 씁니다.
- **`history()`**: `history`에서 `MoveRecord`만 뽑아 불변 리스트로 반환합니다. 직전 수 칸 강조, 잡힌 기물 목록 계산, 착수 기록 확인 등에 씁니다.
- **`sanHistory()`**: `history`에서 `"e4"`, `"Nf3"` 같은 SAN 문자열만 뽑아 불변 리스트로 반환합니다. 기보 패널에 `1. e4 e5` 형태로 수순을 출력할 때 씁니다.

#### `loadFen(String fen)` / `loadGame(String fen, List<String> sanSequence)`
- **`loadFen(String fen)`**: `ChessNotation.parseFen(fen)`으로 먼저 파싱·검증을 끝낸 뒤 `start`를 바꾸고 `history`를 비웁니다. 잘못된 FEN이면 예외가 나고 기존 대국은 유지됩니다.
- **`loadGame(String fen, List<String> sanSequence)`**: `sanSequence`가 null인지 확인한 뒤 임시 `ChessCore replay = new ChessCore(fen)`을 따로 만들어 모든 SAN을 처음부터 둬 봅니다. 중간에 잘못된 수가 있으면 몇 번째 반수에서 실패했는지 메시지에 담아 예외를 던지고 기존 대국은 보존합니다. 끝까지 정상 재생됐을 때만 `this.start`와 `this.history`를 교체합니다. 외부 기보 파일(PGN 등)을 불러올 때 씁니다.

#### `play(...)` 오버로드 5종 (`play(Move)`, `play(String san)`, `playUci(String uci)`, `play(from, to)`, `play(from, to, promotion)`)
- **핵심 실행 함수 `play(Move move)`**:
  1. 현재 `position().legalMoves()`를 구해 `move`가 합법 수 목록에 있는지 검사합니다. (불법 수면 `IllegalArgumentException("불법 수: ...")`)
  2. `next = position().simulateLegalMove(move)`로 실제로 한 수 움직인 다음 국면을 만듭니다.
  3. `ChessNotation.recordSan(position(), next, move, legal)`로 이동 유형·체크/체크메이트 여부·정규 SAN을 묶은 `MoveRecord`를 생성합니다.
  4. `(record, next)`를 `history`에 추가하고 `record`를 반환합니다.
- **나머지 오버로드**: 입력 형식만 다를 뿐 내부에서 `Move`로 변환해 모두 `play(Move)`를 호출합니다.
  - `play(String san)`: `ChessNotation.parseSan(position(), san)`으로 해석 후 실행 (기보 재생, 텍스트 수 입력)
  - `playUci(String uci)`: `Move.fromUci(uci)`로 해석 후 실행 (`"e2e4"` 같은 UCI 입력)
  - `play(String from, String to)` / `play(String from, String to, char promotion)`: 칸 이름(`"e2"`, `"e4"`)과 선택적 승진 기물(`'Q'`, `'R'`, `'B'`, `'N'`)로 즉시 수 입력

#### `undo()`
- `history`가 비어 있으면 `false`를 반환하고, 있으면 마지막 `HistoryEntry`를 제거해 직전 국면으로 되돌린 뒤 `true`를 반환합니다.
- **예상 사용처**: 플레이어가 무르기 버튼을 누르거나, 퀴즈에서 오답 수를 잠시 뒀다가 즉시 원래 국면으로 복구할 때 씁니다.

#### `repetitionCount()`
- **동작**: `ChessNotation.positionKey(position(), true)`로 현재 국면의 비교 키를 만든 뒤, `start` 및 `history`에 쌓인 모든 국면의 키와 비교해 현재 국면이 이번 대국에서 총 몇 번 등장했는지 셉니다.
- **왜 `BoardPosition`이 아닌 `ChessCore`에 있고 `positionKey(..., true)`를 쓰는가**:
  - `BoardPosition`은 현재 한 판의 상태만 알아서 과거에 무슨 국면이 있었는지 모릅니다. 수순 타임라인(`start` + `history`)을 가진 `ChessCore`만 반복 횟수를 셀 수 있습니다.
  - 전체 FEN(`toFen()`)을 비교하면 매 수 바뀌는 `halfmoveClock`과 `fullmoveNumber` 때문에 절대 같아질 수 없습니다. 또 체스 규칙상 3회 동형 반복 무승부는 **기물 배치·차례·캐슬링 권리·실제로 가능한 앙파상 권리**가 같아야 성립하므로 `effectiveEpOnly = true`로 정규화한 앞 4필드 키를 비교합니다.
- **예상 사용처**: 한 수를 둔 직후 `repetitionCount() >= 3`인지 확인해 3회 동형 반복 무승부 종료를 판정할 때 씁니다.

---

## 2. `BoardPosition`

체스판 한 국면의 상태와 체스 이동 규칙을 담는 클래스입니다.  
칸 인덱스는 **`0 = a1`, `7 = h1`, `56 = a8`, `63 = h8`**(`rank * 8 + file`) 순서를 사용합니다.

### 상수·중첩 enum·필드

- **캐슬링 권리 비트마스크 (`public static final int`)**  
  `WK = 1` (백 킹사이드), `WQ = 2` (백 퀸사이드), `BK = 4` (흑 킹사이드), `BQ = 8` (흑 퀸사이드). 네 권리를 정수 1개(`0~15`)의 비트로 합쳐 표현합니다.
- **방향 벡터 테이블 (`private static final int[][]`)**  
  `KNIGHT_STEPS`(나이트 L자 8방향), `DIAGONALS`(대각선 4방향), `STRAIGHTS`(직선 4방향), `KING_STEPS`(상하좌우+대각선 8방향). 이동 생성과 공격 판정(`isSquareAttacked`)에서 같은 방향 정의를 씁니다.
- **`public enum Side`**: `WHITE`, `BLACK`
  - `opposite()`: 상대 진영(`WHITE <-> BLACK`)을 반환합니다. 차례 전환, 상대 기물 식별, 승자 계산에 씁니다.
- **`public enum Status`**: `PLAYING`, `CHECK`, `CHECKMATE`, `STALEMATE`
  - 현재 국면의 규칙상 진행 상태를 나타냅니다. 대국 종료 여부 및 승패/무승부 화면 표시에 씁니다.
- **`public enum Piece`**: `EMPTY('.')`, `WP('P')`, `WN('N')`, `WB('B')`, `WR('R')`, `WQ('Q')`, `WK('K')`, `BP('p')`, `BN('n')`, `BB('b')`, `BR('r')`, `BQ('q')`, `BK('k')`
  - `fen()`: 대응하는 FEN 문자 반환
  - `type()`: 진영을 뺀 대문자 기물 문자(`'P'`, `'N'`, `'B'`, `'R'`, `'Q'`, `'K'`, 빈칸 `'.'`) 반환
  - `belongsTo(Side side)`: 빈칸이 아니고 대문자=백, 소문자=흑 여부가 `side`와 맞는지 판별
  - `is(Side side, char type)`: 특정 진영의 특정 기물인지 한 번에 확인 (`board[s].is(turn, 'R')` 등)
  - `of(Side side, char type)`: 진영과 기물 종류(예: 승진 기물 `'Q'`)로부터 해당 색 `Piece` 반환
  - `fromFen(char fen)`: FEN 문자(빈칸 `.` 제외)를 `Piece`로 변환
- **인스턴스 필드 (`private`)**:
  - `board` (`Piece[64]`): 64칸 기물 배치
  - `turn` (`Side`): 현재 차례 진영
  - `castlingRights` (`int`): 현재 남아 있는 캐슬링 권리 비트 조합 (`0~15`)
  - `enPassantSquare` (`int`): 직전에 상대 폰이 두 칸 전진해 열린 앙파상 타깃 칸 인덱스 (`-1`이면 없음)
  - `halfmoveClock` (`int`): 마지막 폰 전진 또는 기물 포획 이후 흐른 반수 수 (100 이상이면 50수 무승부 조건)
  - `fullmoveNumber` (`int`): 전체 수 번호 (1부터 시작해 흑 차례 착수 후 1 증가)

### 생성자

- **`public BoardPosition(Piece[] board, Side turn, int castlingRights, int enPassantSquare, int halfmoveClock, int fullmoveNumber)`**  
  배열 길이(64), 원소 null 여부, 상태값 범위를 검사하고 `board`를 방어 복사합니다. (`ChessNotation.parseFen`이 국면을 만들 때 호출)
- **`private BoardPosition(BoardPosition source)`**  
  `simulateLegalMove` 전용 내부 복사 생성자입니다. 기존 국면의 배열과 상태 필드를 그대로 복제합니다.

### 칸 좌표·유틸리티 함수 (`static`)

- **`parseSquare(String name)`**: `"e4"` 같은 2글자 칸 이름을 검증해 `0~63` 인덱스로 변환합니다. (`'a'..'h'`, `'1'..'8'` 외의 입력이면 `IllegalArgumentException`) 칸 선택 입력, UCI 파싱, SAN 파싱에 씁니다.
- **`formatSquare(int square)`**: `checkSquare` 검증 후 `0~63` 인덱스를 `"e4"` 문자열로 바꿉니다. 힌트 칸/선택 칸을 문자열로 바꾸거나 UCI/SAN 출력 시 씁니다.
- **`file(int square)` / `rank(int square)`**: 칸 인덱스에서 열(`0=a ~ 7=h`, `square % 8`)과 행(`0=1행 ~ 7=8행`, `square / 8`)을 꺼냅니다. 이동 생성, 비숍 칸 색 판별, 모호성 해소에 씁니다.
- **`squareOf(int file, int rank)`**: 열과 행을 1차원 칸 인덱스(`rank * 8 + file`)로 합칩니다.
- **`insideSquare(int file, int rank)`**: 좌표가 `0..7` 보드 안쪽인지 확인합니다. 방향 벡터로 칸을 뻗어나갈 때 보드 밖으로 나가지 않게 막습니다.
- **`checkSquare(int square)`**: 칸 인덱스가 `0..63` 범위인지 확인해 벗어나면 예외를 던집니다. `Move`, `pieceAt`, `formatSquare`의 방어 검사에 씁니다.
- **`cornerRight(int square)`**: **체스판 네 귀퉁이 칸(룩의 시작 자리)에 해당하는 캐슬링 권리 비트를 반환하는 매핑 함수**입니다.
  - 반환값: `0`(`a1`) → `WQ`, `7`(`h1`) → `WK`, `56`(`a8`) → `BQ`, `63`(`h8`) → `BK`, 나머지 60칸 → `0`
  - **쓰는 이유 2곳**:
    1. `applyUnchecked`에서 `castlingRights &= ~(cornerRight(from) | cornerRight(to));` 한 줄로, 코너 룩이 움직였거나(`from`) 상대가 코너에 들어가 룩을 잡았을 때(`to`) `if`문 4개 없이 해당 방향 캐슬링 권리를 끕니다.
    2. `ChessNotation.validatePosition`에서 `{0, 7, 56, 63}`을 순회하며 캐슬링 권리가 살아 있는데 해당 코너 룩/킹이 제자리에 없는 잘못된 FEN을 걸러냅니다.

### 국면 조회·규칙·시뮬레이션 함수 상세 설명

#### 상태 조회 (`turn`, `castlingRights`, `enPassantSquare`, `halfmoveClock`, `fullmoveNumber`, `pieceAt`, `findKingSquare`, `kingSquare`, `currentBoard`, `toFen`)
- **`turn()` / `castlingRights()` / `enPassantSquare()` / `halfmoveClock()` / `fullmoveNumber()`**: 각 국면 메타데이터를 반환합니다. UI 차례 표시, 50수 규칙(`halfmoveClock >= 100`) 판정, FEN 직렬화·검증에 씁니다.
- **`pieceAt(int square)` / `pieceAt(String square)`**: 지정 칸의 `Piece`를 반환합니다. 보드 렌더링, 클릭한 기물 확인, AI 말 점수 평가에 씁니다.
- **`findKingSquare(Side side)` / `kingSquare(Side side)`**: 보드에서 `side` 진영 킹(`'K'`) 위치를 찾습니다. `findKingSquare`는 `Optional`을 반환해 체크된 킹 칸을 화면에 강조할 때 쓰이고, `kingSquare`는 킹이 없으면 예외를 던지며 내부 체크 판정에 씁니다.
- **`currentBoard()`**: `Piece[8][8]` 2차원 배열을 새로 만들어 8행씩 복사해 돌려줍니다. 호출자가 반환 배열을 수정해도 원본 `BoardPosition`은 바뀌지 않습니다.
- **`toFen()`**: `ChessNotation.formatFen(this)`에 위임해 현재 국면의 6필드 FEN 문자열을 생성합니다.

#### 합법 수·체크·종료 판정 (`legalMoves`, `isCheck`, `isCheckmate`, `status`, `isInsufficientMaterial`)
- **`legalMoves()`**:
  1. `pseudoLegalMoves()`로 현재 차례 기물들의 이동 후보를 모두 수집합니다.
  2. 각 후보 수를 `trial = simulateLegalMove(move)`로 임시 국면에 두어 본 뒤, `trial.isSquareAttacked(trial.kingSquare(turn), turn.opposite())`가 `false`인(내 킹이 상대 공격에 노출되지 않는) 수만 `List.copyOf`에 담습니다.
  - 보드에서 이동 가능한 칸 하이라이트, 불법 수 차단, AI 탐색 후보 생성에 씁니다.
- **`isCheck()`**: `isSquareAttacked(kingSquare(turn), turn.opposite())`를 호출해 현재 차례 킹이 공격받고 있는지 판별합니다.
- **`isCheckmate()`**: `isCheck() && legalMoves().isEmpty()`. 체크 상태에서 피할 수가 하나도 없는지 간편하게 확인할 때(예: 체크메이트 퍼즐) 씁니다.
- **`status()`**: 체크 여부(`isCheck()`)와 합법 수 유무(`legalMoves().isEmpty()`) 조합으로 `CHECKMATE`, `STALEMATE`, `CHECK`, `PLAYING`을 반환합니다. 한 수 둔 뒤 대국이 끝났는지 판정할 때 씁니다.
- **`isInsufficientMaterial()`**: 기물 부족(Dead Position) 무승부 판정 함수입니다.
  - 보드를 훑어 킹 외에 폰·룩·퀸이 하나라도 있으면 체크메이트 가능성이 있으므로 즉시 `false`를 반환합니다.
  - 남은 나이트·비숍 합이 `1`개 이하(K vs K, K+N vs K, K+B vs K)이면 `true`입니다.
  - `2`개일 때는 백 비숍 1개 + 흑 비숍 1개 조합이면서 두 비숍의 칸 색 `(file + rank) & 1`이 같을 때(같은 색 칸 비숍끼리는 메이트 불가)만 `true`이고, 나이트 2개나 서로 다른 색 비숍은 `false`입니다.

#### 가상 착수 및 내부 규칙 판정 (`simulateLegalMove`, `applyUnchecked`, `isSquareAttacked`, `hasLegalEnPassant`, `pseudoLegalMoves`류)
- **`simulateLegalMove(Move move)`**: `new BoardPosition(this)`로 복사본을 만들고 `next.applyUnchecked(move)`를 실행해 새 국면을 반환합니다. 원본 `BoardPosition`을 건드리지 않으므로 AI 탐색 트리 확장, 합법 수 검증, SAN 검증에 씁니다.
- **`applyUnchecked(Move move)` (`private`)**: 합법성이 검증된 수를 복사본 보드에 실제로 반영합니다.
  1. 앙파상 여부 판별: 폰이 비어 있는 `enPassantSquare`로 대각 이동(`file(from) != file(to)`)하면 앙파상 포획이므로 한 칸 뒤에 서 있던 상대 폰(`to - dir`)을 지우고 `captured`에 기록합니다.
  2. 도착 칸에 기물을 놓고(승진이면 `Piece.of(turn, move.promotion())`), 킹 이동 시 해당 진영 캐슬링 권리를 모두 지우며, 가로 2칸 이동(`|to - from| == 2`)이면 캐슬링이므로 룩(`+3 -> +1` 또는 `-4 -> -1`)도 같이 옮깁니다.
  3. `cornerRight(from) | cornerRight(to)` 비트를 꺼 룩 출발·피포획 시 캐슬링 권리를 제거하고, 폰 2칸 전진 시 건너뛴 칸(`from + dir`)을 `enPassantSquare`로 기록합니다.
  4. 폰 이동/잡기면 `halfmoveClock = 0`, 아니면 `+1` 증가, 흑 착수 후 `fullmoveNumber + 1`, `turn` 반전 순으로 상태를 마저 갱신합니다.
- **`isSquareAttacked(int square, Side by)` (`package-private`)**: `square`가 `by` 진영에게 공격받는지 확인합니다.
  - 보드 전체 기물을 돌지 않고 **`square` 기준 역방향**으로 검사합니다: ① `by`의 폰이 대각 공격할 수 있는 한 행 뒤(`r + (by == WHITE ? -1 : 1)`, `f ± 1`)에 상대 `P`가 있는지 ② `KNIGHT_STEPS` 8칸에 `N`이 있는지 ③ `KING_STEPS` 8방향으로 한 칸씩 뻗어 처음 부딪힌 기물이 `by`의 `Q`, 대각 `B`, 직선 `R`, 거리 1의 `K`인지 확인하고 어떤 기물이든 만나면 광선을 멈춥니다.
- **`hasLegalEnPassant()` (`package-private`)**: FEN에 `enPassantSquare`가 설정되어 있더라도 실제로 내 폰이 그 칸을 대각선으로 잡을 수 있는 합법 수가 존재하는지 확인합니다. `ChessNotation.positionKey`가 3회 동형 반복 키를 만들 때 씁니다.
- **`pseudoLegalMoves()` / `addPawnMoves()` / `addPawnMove()` / `addDirectionalMoves()` / `addCastlingMoves()` / `capturable()` (`private`)**:
  - `legalMoves()`에서 호출되어 내 기물별 이동 규칙에 따른 후보 수를 생성합니다.
  - **폰**: 1칸 전진, 시작 행(백 2랭크/흑 7랭크) 2칸 전진, 좌우 대각 잡기·앙파상을 생성하며, 끝 행 도달 시 `addPawnMove`가 `Q/R/B/N` 4종 승진 수를 추가합니다.
  - **N/B/R/Q/K**: `addDirectionalMoves`가 방향 배열(`sliding=false`면 1스텝, `true`면 기물 만날 때까지)로 빈 칸과 적 기물(`capturable`: 상대 소속이며 킹이 아님)까지 이동을 추가합니다.
  - **캐슬링**: `addCastlingMoves`는 킹이 초기 칸(`4`/`60`)에 있고 현재 체크가 아닐 때, 캐슬링 권리·코너 룩·사이 빈칸·킹이 통과하는 인접 칸(`from + dir`) 비공격을 확인해 2칸 캐슬링 후보를 만듭니다(도착 칸 피공격 여부는 `legalMoves()`의 자기 킹 안전 검사에서 걸러집니다).

---

## 3. `ChessNotation`

FEN 문자열과 표준 대수 표기(SAN)의 파싱, 정합성 검증, 직렬화를 담당하는 무상태 정적 유틸리티 클래스입니다.

### 상수

- `CASTLE_FEN` (`"K?Q?k?q?"`): FEN 3번째 필드의 캐슬링 문자 순서와 형식 검증
- `DIGITS` (`"[0-9]+"`): FEN 반수 시계·수 번호가 음이 아닌 정수인지 검증
- `SAN_PATTERN` (`"^([KQRBN])?([a-h])?([1-8])?(x)?([a-h][1-8])(?:=([QRBN]))?([+#])?$"`): 일반 SAN 수 구문 분해
- `CASTLE_SAN` (`"^(O-O(?:-O)?)([+#]?)$"`): 캐슬링 SAN(`O-O`, `O-O-O`) 구문 분해

### 함수 상세 설명 및 예상 사용처

- **`parseFen(String fen)`** & **`validatePosition(BoardPosition pos)` (`private`)**  
  FEN 문자열 6개 필드를 순서대로 파싱(8행 `/` 배치, 차례 `w`/`b`, 캐슬링 권리 `KQkq`, 앙파상 칸, 반수 시계 `>= 0`, 수 번호 `>= 1`)해 `BoardPosition`을 만든 뒤, `validatePosition`으로 **체스 규칙상 실제로 존재할 수 있는 국면인지** 4단계 검증을 수행합니다.
  1. 백 킹과 흑 킹이 정확히 1개씩 있고, 폰이 1행이나 8행에 놓여 있지 않은지
  2. **직전에 수를 둔 쪽(`turn.opposite()`)의 킹이 이미 체크 상태가 아닌지** (체크 상태를 방치한 불법 FEN 거부)
  3. 캐슬링 권리 비트가 켜진 쪽의 킹(`e1`/`e8`)과 코너 룩(`a1`/`h1`/`a8`/`h8`)이 실제로 제자리에 서 있는지 (`cornerRight` 활용)
  4. 앙파상 칸이 지정된 경우 예상 행(백 차례 6행/흑 차례 3행), 앙파상 칸 빈칸, 앞 칸(`ep - dir`) 상대 폰 존재, 출발 칸(`ep + dir`) 빈칸, `halfmoveClock == 0`을 모두 만족하는지
  - **예상 사용처**: `ChessCore.loadFen()`, 외부 FEN 입력 검증 및 국면 로드
- **`positionKey(BoardPosition pos, boolean effectiveEpOnly)`**  
  FEN 앞 4개 필드(기물 배치·차례·캐슬링·앙파상)만 조립합니다. `effectiveEpOnly`가 `true`이면 `pos.hasLegalEnPassant()`가 거짓일 때(앙파상 칸은 열렸지만 인접 아군 폰이 없거나 핀 때문에 잡을 수 없을 때) EP 칸을 `"-"`로 바꿔 FIDE 3회 동형 반복 비교용 정규 키로 씁니다.
  - **예상 사용처**: `ChessCore.repetitionCount()`(3회 반복 비교), `formatFen()`
- **`formatFen(BoardPosition pos)`**  
  `positionKey(pos, false) + " " + pos.halfmoveClock() + " " + pos.fullmoveNumber()`로 표준 6필드 FEN을 조립합니다. (`BoardPosition.toFen()`에서 호출)
- **`parseSan(BoardPosition position, String san)`** & **`findMoveForSan(...)` (`private`)**  
  `findMoveForSan`이 `CASTLE_SAN` / `SAN_PATTERN`으로 기물 종류, 출발 열·행 힌트, 잡기 여부(`x`), 목적지, 승진(`=Q` 등)을 뽑아 `position.legalMoves()`와 대조해 후보가 0개면 둘 수 없는 수, 2개 이상이면 출발 기물 구분 필요 예외를 던지고 유일한 후보 `Move`를 찾습니다. 그다음 `parseSan`이 그 수를 `recordSan`으로 다시 SAN 문자열로 만들어 입력과 `input.equals(record.san())`으로 대조합니다. 이렇게 하면 `+`/`#` 기호가 틀렸거나 불필요한 모호성 해소 문자(예: `Ng1f3`)가 들어간 비표준 입력까지 정확히 거부합니다.
  - **예상 사용처**: `ChessCore.play(String san)`, 수를 두지 않고 SAN 문자열을 `Move`로 해석할 때
- **`recordSan(BoardPosition pos, BoardPosition next, Move move, List<Move> legal)`** & **`buildSanPrefix(...)` (`private`)**  
  `pos`와 이동 직후 `next`를 읽어 `MoveKind`(캐슬링·앙파상·일반 잡기·일반 이동)를 판정하고, `buildSanPrefix`로 SAN 본체(`O-O`, `O-O-O`, 또는 기물·모호성 해소 문자·`x`·도착 칸·`=승진`)를 만든 뒤, `next.isCheck()`와 `next.legalMoves().isEmpty()`를 확인해 `#`(체크메이트) 또는 `+`(체크)를 붙여 `MoveRecord`를 생성합니다.
  - **모호성 해소 우선순위**: 같은 종류의 다른 아군 기물이 같은 칸에 갈 수 있으면 ① 출발 열이 다르면 열 문자(`Nbd2`), ② 열이 같고 행이 다르면 행 문자(`R1e2`), ③ 둘 다 겹치면 전체 칸 이름(`Qa1a4`)을 붙입니다.
  - **예상 사용처**: `ChessCore.play(Move)`, `ChessNotation.parseSan()`

---

## 4. `Move`

이동 좌표를 나타내는 `Move` 레코드와, 실행 완료된 수의 결과 타입인 `MoveKind`·`MoveRecord`를 담는 파일입니다.

### `Move` 레코드

- **컴포넌트**: `int from` (`0~63`), `int to` (`0~63`), `char promotion` (`'-'` 또는 `'Q'`, `'R'`, `'B'`, `'N'`)
- **상수**: `private static final Pattern UCI_PATTERN = Pattern.compile("[a-h][1-8][a-h][1-8][qrbnQRBN]?")`
- **생성자**:
  - 컴팩트 생성자 `public Move`: `BoardPosition.checkSquare`로 `from`/`to` 범위를 검사하고 `promotion`을 대문자로 정규화한 뒤 `from == to`이거나 `"-QRBN"` 외의 문자가 들어오면 `IllegalArgumentException`을 던집니다.
  - 보조 생성자 `public Move(int from, int to)`: 승진 없는 일반 이동(`promotion = '-'`)용 생성자입니다.
- **메서드 및 예상 사용처**:
  - **`fromUci(String uci)`**: `"e2e4"` 또는 승진 포함 `"a7a8q"`를 파싱해 `Move`를 만듭니다. (`ChessCore.playUci()`, 외부 UCI 수열/수 입력 파싱)
  - **`uci()`**: 소문자 UCI 문자열(`"e2e4"`, `"a7a8q"`)로 변환합니다. (불법 수 오류 메시지, 로그 출력, UCI 직렬화)

### 중첩 `MoveKind` enum

- 상수: `NORMAL` (일반 이동), `CAPTURE` (일반 잡기), `EN_PASSANT` (앙파상 잡기), `CASTLE_SHORT` (킹사이드 캐슬링), `CASTLE_LONG` (퀸사이드 캐슬링)
- **`isCapture()`**: `CAPTURE` 또는 `EN_PASSANT`일 때 `true`를 반환합니다. (`MoveRecord.isCapture()`, 이동 유형별 효과·분류)

### 중첩 `MoveRecord` record

- **컴포넌트**: `Move move`, `MoveKind kind`, `boolean check`, `boolean checkmate`, `String san`
- **`isCapture()`**: `kind.isCapture()`에 위임합니다. (잡힌 기물 표시, 효과음·UI 피드백 분기 등)
- **예상 사용처**: 한 수 착수 후 결과 반환, 대국 기보 목록 보관, 직전 수 하이라이트(`move()`), SAN 기보 표시(`san()`), 체크/메이트 상태 확인
