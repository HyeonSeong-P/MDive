# MDive 작업 하네스

이 파일은 이 저장소에서 작업하는 코딩 에이전트의 기본 규칙이다. 기능과 사용자 시나리오가 확정되면 `docs/product/overview.md`를 먼저 갱신하고, 실제 구조가 달라지면 관련 `docs/engineering/` 문서와 이 파일도 함께 갱신한다.

## 현재 범위

- MDive는 Android용 Markdown 뷰어 앱이다.
- 사용자 시나리오, 기능 범위, 파일 획득 방식, Markdown 지원 범위는 아직 미정이다. `docs/product/overview.md`의 열린 질문을 임의로 확정하지 않는다.
- 지금은 단일 `app` 모듈의 Compose 템플릿이다. 필요가 확인되기 전에는 모듈, 라이브러리, 추상화를 선행 추가하지 않는다.

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

1. 기능 작업 전 `docs/product/overview.md`에서 확정된 요구사항과 미정 사항을 확인한다. 미정 사항이 구현 결과를 바꾸면 사용자와 결정한다.
2. 변경되는 화면의 상태, 액션, 데이터 경계, 실패·로딩 상태를 먼저 정리한다. 작은 변경에는 코드에 바로 반영해도 된다.
3. 가장 단순한 구현부터 시작하고, 재사용이나 복잡성이 실제로 드러날 때 추출한다.
4. 변경된 비즈니스 규칙은 단위 테스트로, 중요한 사용자 흐름은 필요한 범위에서 UI 테스트로 검증한다. 문서만 바꾼 경우 빌드는 선택 사항이다.
5. 구현과 문서가 어긋나면 같은 작업에서 맞춘다. 새 의존성이나 모듈은 이유를 기록한다.

## 확인 명령

- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest`
- 기기 또는 에뮬레이터가 준비된 경우: `./gradlew :app:connectedDebugAndroidTest`

## 참고

- [문서 안내](docs/README.md)
- [계층과 Compose 아키텍처](docs/engineering/architecture.md)
- [MVI 화면 구현 규칙](docs/engineering/mvi.md)
- [Navigation 3 규칙](docs/engineering/navigation.md)
- [Git 작업 규칙](docs/engineering/git-workflow.md)
- [제품 방향과 열린 질문](docs/product/overview.md)
