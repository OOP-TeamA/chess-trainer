# Chess Trainer

Java Swing으로 만드는 체스 학습 프로그램 팀 프로젝트입니다. 레슨, 퀴즈, 명국 기보, AI 대전, 학습 기록을 하나의 체스판과 규칙 엔진 위에서 연결하는 것을 목표로 합니다.

## 현재 상태

현재 저장소에는 Gradle 실행 설정과 빈 `JFrame`을 띄우는 `Main`만 구현되어 있습니다. 아래 명세서의 기능과 패키지 구조는 **개발 목표**이며, 구현 완료 목록이 아닙니다.

- Gradle 모듈: `app` (단일 모듈)
- 현재 진입점: `app/src/main/java/com/oopteama/chess/Main.java`
- 기준 Java 패키지: `com.oopteama.chess`
- 현재 Gradle toolchain: Java 25 (`app/build.gradle.kts`)

## 실행

Gradle을 시작할 수 있는 JDK가 필요합니다. 프로젝트의 Java toolchain은 현재 25로 설정되어 있으며, 팀의 Java 버전 결정 후 문서와 빌드 설정을 함께 맞춰야 합니다.

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

개발은 공통 계약과 실행 환경을 먼저 맞춘 뒤 체스 규칙·파일 로딩·공통 화면을 병렬로 만들고, 레슨·퀴즈·기록 저장의 작은 완성 흐름을 연결하는 순서로 진행합니다. 이후 AI 대전, 기보, 사용자별 기록과 순위를 단계별로 통합합니다. 각 단계의 구체적인 완료 기준은 주간 계획을 따릅니다.

문서에 있는 `com.oopteama.chess` 아래의 `core`, `notation`, `content`, `exercise`, `replay`, `ai`, `game`, `progress`, `nav`, `ui`, `app` 등은 예정된 하위 패키지입니다. 소스 루트는 `app/src/main/java`이며, 코드가 추가될 때 이 경로를 사용합니다.

## 문서 기준

문서 기준본은 `docs/` 아래의 세 HTML 파일입니다. 세 문서를 함께 검토하고 변경합니다. 주간 계획에서 우선순위를 정하면 기능 명세서의 요구사항과 인터페이스 명세서의 계약에도 반영해야 합니다. 아직 결정하지 않은 Java 버전, 담당자 실명, 강의 피드백 등은 결정된 것으로 간주하지 않습니다.
