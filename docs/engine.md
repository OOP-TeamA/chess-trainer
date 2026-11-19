# Engine 패키지 (`com.oopteama.chess.engine`)

체스 규칙, 국면 표현, 기보 변환(FEN/UCI/SAN), AI 탐색 및 후보 수 평가를 담당하는 순수 도메인 패키지입니다.  
총 **5개 파일**로 구성되며 Swing/AWT에 전혀 의존하지 않습니다.

| 파일 | 종류 | 핵심 책임 |
| --- | --- | --- |
| [`Piece.java`](../app/src/main/java/com/oopteama/chess/engine/Piece.java) | `enum` | 진영(`Side`), 기물 종류·가치(`Type`), 64칸 기물(`Piece`), FEN 문자 변환 |
| [`Move.java`](../app/src/main/java/com/oopteama/chess/engine/Move.java) | `record` | 이동 좌표(`from`, `to`, `promotion`), UCI 변환, 착수 결과(`Kind`, `Record`) |
| [`Board.java`](../app/src/main/java/com/oopteama/chess/engine/Board.java) | `final class` | 불변 64칸 국면, FEN 파싱·검증(표준/샌드박스), 합법 수·특수 규칙·공격자 탐색 |
| [`ChessGame.java`](../app/src/main/java/com/oopteama/chess/engine/ChessGame.java) | `final class` | 대국 세션(수순 이력, 무르기, 3회 반복·50수·기권, SAN 파싱/생성, 잡힌 기물) |
| [`ChessAI.java`](../app/src/main/java/com/oopteama/chess/engine/ChessAI.java) | `final class` | 알파-베타·PST AI, 반복 심화, 호버 수 평가(`evaluateMove`), N수 강제 메이트 탐색 |

---

## 1. `Piece`

기물과 진영의 기본 어휘를 한 파일에 모았습니다.

- **중첩 enum `Side`**: `WHITE('w', "백")`, `BLACK('b', "흑")` — `opposite()`, `fenChar()`, `label()`
- **중첩 enum `Type`**: `PAWN('P', 100)`, `KNIGHT('N', 320)`, `BISHOP('B', 330)`, `ROOK('R', 500)`, `QUEEN('Q', 900)`, `KING('K', 20000)` — `symbol()`, `value()`, `label()`, `isPromotable()`, `fromSymbol(char)`
- **enum `Piece`**: `EMPTY`, `WP..WK`, `BP..BK` — `fenChar()`, `side()`, `type()`, `isEmpty()`, `is(Side)`, `is(Side, Type)`, `of(Side, Type)`, `fromFen(char)`

---

## 2. `Move`

- **컴포넌트**: `int from` (`0..63`), `int to` (`0..63`), `Piece.Type promotion` (`null` 또는 `Q/R/B/N`)
- **검증**: 컴팩트 생성자에서 칸 범위, `from != to`, 승진 가능 기물 여부를 검증합니다.
- **중첩 타입**:
  - `Move.Kind`: `NORMAL`, `CAPTURE`, `EN_PASSANT`, `CASTLE_SHORT`, `CASTLE_LONG` (`isCapture()`)
  - `Move.Record(Move move, Kind kind, Piece moved, Piece captured, boolean check, boolean checkmate, String san)`: 착수 직후의 전체 메타데이터를 불변 레코드로 보관
- **메서드**: `fromUci(String)`, `uci()`

---

## 3. `Board`

### 좌표 체계 & 주요 필드

- **칸 인덱스**: `0 = a1`, `7 = h1`, `56 = a8`, `63 = h8` (`rank * 8 + file`)
- **캐슬링 비트마스크**: `CASTLE_WK = 1`, `CASTLE_WQ = 2`, `CASTLE_BK = 4`, `CASTLE_BQ = 8`
- **불변 필드 (`private final`)**: `Piece[] squares`(64칸), `Side turn`, `int castling`, `int enPassant`(`-1`이면 없음), `int halfmoveClock`, `int fullmoveNumber`, `boolean sandbox`

### 주요 메서드 & 내부 로직

| 구분 | 메서드 | 설명 |
| --- | --- | --- |
| 생성 | `initial()`, `fromFen(fen)`, `fromSandboxFen(fen)` | 표준 FEN은 양쪽 킹 1개, 1/8랭크 폰 금지, 비차례 킹 체크 금지, 캐슬링·앙파상 정합성 검증. 샌드박스는 킹 없는 튜토리얼 국면 허용 |
| 좌표 | `file(sq)`, `rank(sq)`, `square(f, r)`, `inside(f, r)`, `parseSquare(name)`, `formatSquare(sq)` | 1차원 인덱스 ↔ `(file, rank)` ↔ `"e4"` 상호 변환 |
| 합법 수 | `legalMoves()`, `legalMovesFrom(from)` | 의사합법 수 생성 후 `applyMove`로 임시 적용해 아군 킹이 공격받지 않는 수만 필터링 (샌드박스 무킹 국면은 의사합법 수 그대로 허용) |
| 착수 | `applyMove(move)`, `classifyMove(move)`, `capturedPiece(move)` | `squares.clone()` 위에 이동·승진·앙파상 포획·캐슬링 룩 이동·`cornerRight` 비트 제거·시계 갱신을 수행해 새 `Board` 반환 |
| 공격/종료 | `isSquareAttacked(sq, by)`, `attackersOf(sq, by)`, `attackedEnemySquaresFrom(sq)` | `sq` 기준 역방향(폰·나이트·8방향 광선)으로 공격 여부 및 공격자 칸 목록을 O(1) 상한으로 조회 |
| 판정/직렬화 | `isInCheck()`, `isCheckmate()`, `isStalemate()`, `isInsufficientMaterial()`, `hasLegalEnPassant()`, `positionKey(effectiveEpOnly)`, `toFen()` | K 대 K, K+마이너 대 K, 같은 색 비숍 쌍 기물 부족 판정 및 FIDE 3회 반복용 정규 4필드 키 생성 |

---

## 4. `ChessGame`

한 판의 대국 상태와 기보 타임라인을 관리합니다.

- **내부 상태**: `Board startBoard`, `List<Entry> history`(`Entry(Move.Record, Board)`), `Side resignedSide`
- **종료 상태 `Status`**: `PLAYING`, `CHECK`, `CHECKMATE`, `STALEMATE`, `DRAW_INSUFFICIENT`, `DRAW_FIFTY_MOVE`(`halfmoveClock >= 100`), `DRAW_REPETITION`(`repetitionCount() >= 3`), `RESIGNED_WHITE`, `RESIGNED_BLACK`
- **주요 메서드**:
  - 착수: `play(Move)`, `playUci(String)`, `playSan(String)` — 합법성 검증 후 SAN·포획·체크 정보를 포함한 `Move.Record` 생성 및 이력 추가
  - 제어: `undo()`, `undo(int count)`, `resign(Side)`, `reset()`, `reset(Board)`
  - 조회: `board()`, `history()`, `lastMove()`, `status()`, `winner()`, `statusText()`, `repetitionCount()`, `capturedBy(Side)` (가치 내림차순 정렬)
  - SAN 변환: `formatSan(...)` (열→행→전체칸 순 모호성 해소, `O-O`/`O-O-O`, `=Q`, `+`/`#`), `parseSan(Board, String)`

---

## 5. `ChessAI`

단일 클래스 안에 **대국 AI**, **호버 수 평가기**, **자유 메이트 전용 솔버** 세 가지 엔진 기능을 응집했습니다.

### 난이도(`Difficulty`) 및 평가 전략(`Evaluator`)

| 난이도 | 탐색 깊이 | 평가 함수 | 시간 제한 | 특징 |
| --- | --- | --- | --- | --- |
| `BEGINNER` (입문) | 1 ply | `MATERIAL` | 1.0초 | 상위 3개 후보(180cp 이내) 중 무작위 선택 |
| `EASY` (초급) | 2 ply | `MATERIAL` | 1.5초 | 공짜 기물 포획, 얕은 수읽기 |
| `MEDIUM` (중급) | 3 ply | `MATERIAL_PST` | 2.0초 | 기물 가치 + 6종 위치 점수표(PST) |
| `HARD` (상급) | 4 ply | `MATERIAL_PST` | 2.5초 | 반복 심화 + MVV-LVA 수 정렬 가지치기 |

### 핵심 로직 3가지

1. **대국 탐색 (`chooseMove`, `hint`)**
   - `negamax` + 알파-베타 가지치기 + `orderedMoves`(MVV-LVA 포획·승진 우선 정렬) + 반복 심화(직전 깊이 최선 수를 맨 앞으로 이동).
   - 메이트 점수는 `-(MATE_SCORE - ply)`로 계산해 **더 빠른 메이트를 우선** 선택합니다.
   - `BooleanSupplier cancelled`와 `deadline`으로 UI 무르기·화면 이동 시 `AbortedException`으로 즉시 중단합니다.

2. **호버 후보 수 평가 (`baselineScore`, `evaluateMove`)**
   - `MoveEvaluation` 레코드에 `Verdict`(`MATE`/`GOOD`/`NEUTRAL`/`RISKY`/`BLUNDER`), `attackedEnemies`, `checkingAttackers`, `defendersOfDest`, `attackersOfDest`, `newlyExposedAllies`, `opponentReply`, `opponentReplySan`, `reason`을 담아 반환합니다.
   - `negamaxPreview` + **`quiescence` (포획 정지 탐색 + 체크 회피 전개)**를 사용해 퀸 희생 메이트인2 같은 전술 수도 손실로 오판하지 않고, `baselineScore` 대비 손실(`lossCp`)과 방어 이탈(`findNewlyExposedAllies`)을 정확히 판별합니다.

3. **자유 메이트 퍼즐 솔버 (`findMateIn`, `isForcedMateAfterMove`, `bestDefenseAgainstMate`)**
   - AND-OR 트리(`canAttackerForceMate` ↔ `isForcedMateForOpponentToMove`)로 1수→2수→…→N수 순으로 강제 체크메이트 존재 여부를 증명합니다.
   - 체크 수 우선 정렬(`orderChecksFirst`)과 리프 즉시 체크메이트 검사(`hasImmediateMate`)로 3수 메이트도 수 밀리초 내에 판별합니다.
   - `bestDefenseAgainstMate`는 메이트 탈출 수가 있으면 가장 유리한 탈출 수를, 모든 응수가 메이트면 1수 즉사를 피하고 가장 오래 버티는 방어 수를 동적으로 고릅니다.
