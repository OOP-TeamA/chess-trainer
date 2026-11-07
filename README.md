# Chess Trainer

Java Swing으로 만드는 체스 학습 프로그램 팀 프로젝트입니다. 레슨, 퀴즈, 명국 기보, AI 대전, 학습 기록을 하나의 체스판과 규칙 엔진 위에서 연결하는 것을 목표로 합니다.

## 현재 상태

현재 저장소에는 Gradle 실행 설정과 빈 `JFrame`을 띄우는 `Main`만 구현되어 있습니다. 아래 명세서의 기능과 패키지 구조는 **개발 목표**이며, 구현 완료 목록이 아닙니다.

- Gradle 모듈: `app` (단일 모듈)
- 현재 진입점: `app/src/main/java/com/oopteama/chess/Main.java`
- 기준 Java 패키지: `com.oopteama.chess`
- 현재 Gradle toolchain: Java 25 (`app/build.gradle.kts`)

## 실행

개발, 검증, 배포 기준은 Java 25입니다. Gradle Wrapper는 루트에서 실행합니다.

Windows PowerShell:

```powershell
.\gradlew.bat :app:run
.\gradlew.bat :app:test
```

macOS/Linux:

```sh
./gradlew :app:run
./gradlew :app:test
```

현재 `test` 소스는 아직 없습니다.

## 문서와 개발 순서

1. [주간 계획 및 개발 순서](docs/chess-sprint-plan.html): 역할, 의존 관계, 주차별 완료 기준
2. [기능 명세서](docs/chess-learning-spec.html): 기능 범위, 수용 기준, 파일 형식, 테스트
3. [인터페이스 명세서](docs/chess-interface-spec.html): 패키지 경계, 클래스 계약, 호출 순서
4. [7인 파일 흐름도](docs/chess-development-flow.html) · 단계별 작업 파일, 병렬 작업과 선행 의존성

11/13 첫 통합은 [공용 샘플과 시연 검증 절차](docs/chess-learning-spec.html#first-demo)를 사용합니다. 나이트 레슨, 용어 검색, 10문제 응시, 재실행 후 기록 유지까지 같은 입력으로 확인할 수 있습니다.

개발은 공통 계약과 실행 환경을 먼저 맞춘 뒤 체스 규칙·파일 로딩·공통 화면을 병렬로 만들고, 레슨·퀴즈·기록 저장의 작은 완성 흐름을 연결하는 순서로 진행합니다. 이후 AI 대전, 기보, 사용자별 기록과 순위를 단계별로 통합합니다. 각 단계의 구체적인 완료 기준은 주간 계획을 따릅니다.

문서에 있는 `com.oopteama.chess` 아래의 `core`, `notation`, `content`, `exercise`, `replay`, `ai`, `game`, `progress`, `nav`, `ui`, `app` 등은 예정된 하위 패키지입니다. 소스 루트는 `app/src/main/java`이며, 코드가 추가될 때 이 경로를 사용합니다.

## 문서 기준

`docs/` 아래 기능 명세의 요구사항·파일 형식, 인터페이스 명세의 Java 계약, 주간 계획의 단계별 범위·담당을 기준으로 구현합니다. 파일 흐름도는 이 계약을 작업 순서로 보여줍니다. 설계 그림과 최신 코드 계약이 다르면 인터페이스 명세 2~4장을 우선합니다. 범위나 계약을 변경하면 관련 문서와 흐름도를 같은 PR에서 맞춥니다.

AI에 작업을 맡길 때는 대상 요구사항 ID, 흐름도의 단계와 역할, 수정할 파일, 확인할 공용 샘플을 함께 넘깁니다. 두 문서가 충돌하는 항목은 공용 계약을 먼저 맞춘 뒤 병합합니다.

## 제출용 실행 JAR 계약

제출 준비 단계에서 `:app:jar`의 출력 파일명을 `app/build/libs/chess-trainer.jar`로 설정하고 매니페스트에 `Main-Class: com.oopteama.chess.Main`을 추가합니다. 번들 콘텐츠는 JAR에 포함합니다. 배포판은 Java 25 표준 라이브러리로 실행되도록 구성합니다. 사용자 기록은 사용자 홈의 `.chesslearn`을 사용합니다.

제출 검증은 Java 25가 있는 다른 PC 또는 빈 폴더에서 `java -jar chess-trainer.jar`를 실행해 첫 시연을 확인하는 방식입니다. 현행 Gradle 골격의 `:app:jar`는 패키징 준비 단계에서 이 계약에 맞춥니다.
