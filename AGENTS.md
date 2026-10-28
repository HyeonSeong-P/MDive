# MDive 작업 하네스

이 파일은 이 저장소에서 작업하는 코딩 에이전트의 기본 규칙이다. 기능과 사용자 시나리오가 확정되면 `docs/product/overview.md`를 먼저 갱신하고, 실제 구조가 달라지면 관련 `docs/engineering/` 문서와 이 파일도 함께 갱신한다.

## 현재 범위

- MDive는 Android용 Markdown 뷰어 앱이다.
- 사용자 시나리오와 첫 버전의 기능 방향은 `docs/product/overview.md` 및 연결된 기능 명세에서 관리한다. 홈·읽기 기본 시안은 승인됐으며 세부 디자인과 일부 동작은 미정이다. 열린 질문을 임의로 확정하지 않는다.
- Gradle 모듈은 제품 앱 `app`, 공통 Android 라이브러리 `designsystem`, 별도 카탈로그 앱 `designsystem/catalog`(`:designsystem:catalog`)이다. 제품은 S01의 홈·읽기 호스트와 Hilt·Navigation 3 기반이며 기존 디자인 시스템을 main/release에서 재사용한다. 카탈로그는 독립 앱으로 설치·실행한다. S02에서 시스템 파일 선택과 제목·원문 로더를 연결한다. 원문 검증은 개발 호스트에서 유지하며 S03은 CommonMark 0.31.2 기본 본문을 파싱해 기존 DS로 표시한다. 파서 AST는 data 내부에서 앱 소유 모델로 변환하고 미지원 콘텐츠는 원문으로 남긴다. GFM·코드·리소스·확장 렌더링은 각 후속 단계다. 필요가 확인되기 전에는 추가 모듈, 라이브러리, 추상화를 선행 추가하지 않는다.

## 구현 원칙

- Kotlin과 Jetpack Compose를 사용한다. 모든 화면은 `State`, `Intent`, `Effect`, 제네릭 `BaseViewModel` 구조를 따른다. 화면 구현 규칙은 `docs/engineering/mvi.md`를 따른다.
- 여러 화면의 이동에는 Navigation 3를 사용한다. 백스택과 목적지 key는 UI 계층에서 소유한다. 세부 기준은 `docs/engineering/navigation.md`를 따른다.
- 의존성 주입은 Hilt를 사용한다. 저장소 구현 바인딩은 앱의 DI 구성 코드에 두고, `domain`에 Hilt·Android 의존성을 넣지 않는다.
- 화면 단위 ViewModel이 불변 상태를 `StateFlow`로 노출하고 Intent를 처리한다. 공통 `reduce`로 상태를 갱신하고 일회성 Effect는 `Channel`로 방출한다. Composable은 상태를 그리며 Intent를 전달하고 Effect를 수집한다.
- 의존성 방향은 `ui -> domain <- data`다. `domain`은 `ui`, `data`, Android 프레임워크를 알지 못한다. 도메인 모델과 저장소 인터페이스는 `domain`에, 구현과 데이터 소스는 `data`에 둔다. UI는 구체적인 데이터 구현에 의존하지 않고, 앱 진입점에서 구현을 조립한다.
- ViewModel이 도메인 저장소 인터페이스를 직접 사용하는 것은 허용한다. 유즈케이스는 여러 단계의 비즈니스 규칙을 묶거나, 여러 저장소를 조합하거나, 여러 화면에서 재사용되는 규칙을 캡슐화할 때 만든다. 저장소 메서드마다 유즈케이스를 기계적으로 만들지 않는다.
- 재사용할 Compose 컴포넌트는 데이터와 이벤트를 인자로 받고 `Modifier`를 제공한다. 교체 가능한 영역에는 의미가 분명한 slot을 고려한다. 자식 간 공유 상태와 조합 API가 실제로 필요할 때 compound component와 scope를 고려한다. 단순한 UI에는 조건문과 일반 Composable을 사용한다.
- 화면 전용 UI와 공용 UI를 구분하고, 공용 컴포넌트에 화면 ViewModel이나 도메인 규칙을 넣지 않는다. UI 로컬 상태는 필요한 최소 공통 조상에 둔다.
- 불필요한 상태 복제와 기능이 정해지지 않은 추상화는 만들지 않는다. 공통 MVI 베이스와 화면별 네 가지 계약은 예외 없이 사용한다.

## 작업 절차

브랜치 선택, 커밋 메시지, PR 및 릴리스 절차는 `docs/engineering/git-workflow.md`를 따른다.
앱의 `versionName`과 `versionCode`는 `docs/engineering/versioning.md`를 따른다. 새 배포 빌드를 만들 때 모든 트랙의 기존 업로드 코드를 확인한다.

### 서브에이전트 활용

- `.codex/agents/*.toml`은 작업 때마다 호출할 프로젝트 서브에이전트의 역할 정의다. 각 서브에이전트는 `.agents/skills/`의 대응 스킬을 작업 절차의 원천으로 사용한다. 역할 파일과 이 문서에 절차를 복사하지 않는다.
- 호출할 때는 프롬프트에 역할 이름과 작업 범위를 직접 쓴다. 예: "`change_reviewer` 서브에이전트에게 현재 변경 사항을 검토하게 하고, 완료될 때까지 기다린 뒤 파일 위치와 근거를 요약해줘." 스킬의 `$이름`과 달리 서브에이전트 전용 접두사는 쓰지 않는다.
- 범위를 분리할 수 있는 기획, API 조사, 기능 구현, 코드 검토, Git 작업은 가능한 한 대응 서브에이전트에 위임한다. 메인은 요구사항과 의사결정, 작업 순서, 결과 통합을 맡아 긴 탐색 기록과 로그를 메인 컨텍스트에 쌓지 않는다. 짧은 단일 작업은 직접 처리한다.
- 독립적인 읽기·조사·검토 작업은 서브에이전트로 병렬 실행한다. 선행 결정이 필요한 구현은 순서대로 진행한다. 같은 파일을 수정하는 작업은 동시에 위임하지 않고, 각 서브에이전트에 범위와 파일 소유권을 명시한다.
- 커밋·푸시는 다른 서브에이전트의 파일 수정과 검증이 끝난 뒤 수행한다. PR 작성은 브랜치 푸시 이후 수행한다. Git 서브에이전트는 요청받은 변경 범위만 다룬다.
- 서브에이전트 결과는 요약, 근거가 있는 발견 사항, 변경 파일, 검증 결과, 남은 결정 사항만 받아 통합한다. 제품 요구사항이 미정이면 서브에이전트가 임의로 확정하지 않는다.

| 작업 | 대응 서브에이전트 | 절차 스킬 |
| --- | --- | --- |
| 사용자 시나리오와 수용 기준 | `product_planner` | `feature-spec` |
| 확정된 Android 기능 구현 | `feature_builder` | `android-feature` |
| 앱 화면·DS 시각 구성 검토 | `app_designer` | `app-design-review` |
| 변경 사항 독립 검토 | `change_reviewer` | `change-review` |
| Android API·의존성 확인 | `android_api_researcher` | `android-api-check` |
| 요청받은 변경 커밋·푸시 | `git_committer` | `git-commit-push` |
| 요청받은 PR/MR 작성 | `pr_creator` | `git-open-pr` |

### 제품 기획 문서

- 디자인 시스템의 공통 원본·생성물과 Compose 카탈로그 검증은 `docs/product/design-system.md`를 따른다. `design-system/`은 공통 원본 폴더이고, `designsystem/`은 Android 라이브러리 모듈이다. 생성 파일을 직접 수정하지 않는다.
- 공통 버튼·아이콘·목록·검색·탭 등은 `designsystem`의 MDive 컴포넌트를 사용한다. Material 기본 외형을 그대로 제품 디자인으로 채택하지 않으며, 새 공통 외형·상태는 카탈로그에서 함께 검토한다.
- 디자인은 기초 규칙·컴포넌트 카탈로그를 먼저 검토한 뒤 제품 화면에 적용한다. 사용자에게는 실행 가능한 HTML·Compose 카탈로그를 안내하고 캡처를 기본 검토 결과로 제시하지 않는다.

- 와이어프레임과 화면별 설명은 저장소의 HTML 화면 설계서로 작성한다. 문서 위치, 원본의 역할 분담, 작성·검증 방식은 `docs/product/planning.md`를 따른다.
- 기획을 변경할 때 `feature-spec` 스킬을 사용하고, HTML의 초안이나 예시를 확정 요구사항으로 간주하지 않는다.

### 일반 작업

여러 세션에 걸친 구현은 [세션별 구현 계획](docs/implementation/open-markdown.md)과 [실행 지시문](docs/implementation/session-prompts.md)을 따른다. 한 세션은 요청받은 ID 하나의 구현·검증·독립 검토·현황 갱신까지만 수행하고 다음 단계를 자동 시작하지 않는다. 제품 정책은 기능 명세, 시각 조합과 공통 UI 경계는 DS·화면 디자인 문서를 원본으로 삼는다. 계획의 완료 표시는 실제 검증 결과와 일치시킨다.

1. 기능 작업 전 `docs/product/overview.md`에서 확정된 요구사항과 미정 사항을 확인한다. 미정 사항이 구현 결과를 바꾸면 사용자와 결정한다.
2. 변경되는 화면의 상태, 액션, 데이터 경계, 실패·로딩 상태를 먼저 정리한다. 작은 변경에는 코드에 바로 반영해도 된다.
3. 가장 단순한 구현부터 시작하고, 재사용이나 복잡성이 실제로 드러날 때 추출한다.
4. 변경된 비즈니스 규칙은 단위 테스트로, 중요한 사용자 흐름은 필요한 범위에서 UI 테스트로 검증한다. 문서만 바꾼 경우 빌드는 선택 사항이다.
5. 구현과 문서가 어긋나면 같은 작업에서 맞춘다. 새 의존성이나 모듈은 이유를 기록한다.

## 확인 명령

- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest`
- 카탈로그: `./gradlew :designsystem:catalog:assembleDebug`
- 카탈로그 기기 검증: `./gradlew :designsystem:catalog:connectedDebugAndroidTest`
- 기기 또는 에뮬레이터가 준비된 경우: `./gradlew :app:connectedDebugAndroidTest`

## 참고

- [문서 안내](docs/README.md)
- [계층과 Compose 아키텍처](docs/engineering/architecture.md)
- [MVI 화면 구현 규칙](docs/engineering/mvi.md)
- [Navigation 3 규칙](docs/engineering/navigation.md)
- [Git 작업 규칙](docs/engineering/git-workflow.md)
- [앱 버전 전략](docs/engineering/versioning.md)
- [제품 방향과 열린 질문](docs/product/overview.md)
- [제품 기획과 HTML 화면 설계서](docs/product/planning.md)
