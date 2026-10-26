# MDive 아키텍처 초안

상태: 잠정 결정. 제품 기획과 첫 기능 구현 결과에 따라 수정한다.

## 선택

Android 권장 아키텍처의 UI·데이터 계층, 단방향 데이터 흐름, 화면 단위 ViewModel을 사용한다. 의존성 방향은 Clean Architecture 원칙을 따른다. `domain`은 앱의 모델·규칙·저장소 계약을 소유하며 다른 계층을 모른다. `ui`는 도메인 계약을 사용하고 `data`는 이를 구현한다. Android 공식 가이드의 선택적 도메인 계층 정의와는 차이가 있는 프로젝트 규칙이다.

의존성 방향:

```text
ui (Compose, ViewModel) ──> domain <── data (저장소 구현, 데이터 소스)

app의 Hilt 구성: data 구현을 domain 계약에 바인딩해 ui에 제공
```

`domain`에서는 `ui`, `data`, Android 프레임워크, Compose를 import하지 않는다. `ui`에서도 `data` 구현을 import하지 않는다. 앱 진입점 또는 DI 구성 코드만 도메인 계약과 데이터 구현을 함께 알고 연결한다.

Gradle 모듈은 `app`과 `designsystem` 두 개다. 사용자 합의에 따라 디자인 시스템만 Android 라이브러리로 분리한다. 제품의 `ui`, `domain`, `data`는 `app` 내부 패키지 경계로 유지하며, 아직 제품 기능은 템플릿 단계다. 패키지가 물리적 의존성을 강제하지 않으므로 리뷰와 테스트에서 import 방향을 확인한다. 의존성 주입은 Hilt를 사용하며, Hilt 모듈과 저장소 구현 바인딩은 앱의 DI 구성 코드에 둔다. `domain` 모델·인터페이스·유즈케이스에는 Hilt 또는 Android 어노테이션을 붙이지 않는다. 실제 의존성이 생기는 기능 구현 때 Hilt 빌드 설정을 추가한다.

`designsystem`은 테마·글꼴·stateless Compose 컴포넌트와 카탈로그 표현을 소유하며 `app`에 의존하지 않는다. 현재 `app`의 debug 카탈로그만 `designsystem`에 의존하고 카탈로그 화면 MVI는 `app/src/debug/`에 둔다. release 제품 화면은 변경하지 않는다. 공통 값·카탈로그 원본과 생성 과정은 [디자인 시스템](../product/design-system.md)에서 관리한다. 별도 카탈로그 앱 모듈은 만들지 않는다.

## 계층 책임

| 위치 | 책임 | 피할 것 |
| --- | --- | --- |
| `ui` | Compose 화면·컴포넌트, 화면 상태와 액션, ViewModel, 사용자에게 보이는 오류 표현 | 파일/DB 등 데이터 소스 직접 호출, 저장소 구현 의존 |
| `domain` | Markdown 문서 등 도메인 모델, 앱 규칙, 저장소 인터페이스, 필요한 유즈케이스 | Android UI 타입, 구체적 저장소·파서·영속화 구현 |
| `data` | 저장소 구현, 파일·DB·네트워크 등의 데이터 소스, 외부 모델과 도메인 모델 간 변환 | Compose 및 화면 상태 |

저장소가 데이터의 진입점이며 데이터 소스의 세부 사항을 숨긴다. `ViewModel -> Repository interface`는 정상 경로다. 유즈케이스는 다음 중 하나가 실재할 때 도입한다.

- 두 단계 이상의 의미 있는 비즈니스 규칙을 하나의 동작으로 묶어야 한다.
- 둘 이상의 저장소나 데이터 소스를 조합해 도메인 판단을 내려야 한다.
- 동일한 도메인 규칙을 여러 화면에서 재사용하거나, ViewModel의 비즈니스 로직이 커진다.

단순 위임만 하는 유즈케이스는 만들지 않는다. 기술적 데이터 조합만 필요하다면 저장소 구현 내부에 둘지 먼저 판단한다. 유즈케이스가 생겨도 하나의 책임을 갖고 변경 가능한 상태를 보관하지 않는다.

## 화면 MVI 계약

모든 화면은 `State`, `Intent`, `Effect`, 제네릭 `BaseViewModel`을 사용한다. 화면은 Intent를 ViewModel에 전달하고, ViewModel이 노출한 State를 Compose가 렌더링하며 Effect를 UI에서 처리한다. 세부 기준은 [MVI 화면 계약](mvi.md)을 따른다. 여러 화면의 이동은 [Navigation 3 계약](navigation.md)을 따른다. ViewModel은 `Context`, `Activity`, Compose 타입을 보관하지 않는다.

## Compose 컴포넌트 계약

공통 외형과 상태는 `designsystem`의 MDive 컴포넌트가 소유한다. 버튼·아이콘·문서 목록·검색·탭은 Foundation 기반 컨트롤로 입력·포커스·접근성 의미를 제공하며 Material 기본 외형을 그대로 제품 스타일로 사용하지 않는다. 테마·텍스트 렌더링 기반의 재사용은 허용한다. 생성 토큰·아이콘·상태 목록과 카탈로그의 원본 책임은 [디자인 시스템](../product/design-system.md)을 따른다.

- 화면 진입 Composable만 ViewModel을 가져온다. 그 아래의 화면 콘텐츠는 화면 State와 이벤트 콜백을 인자로 받아 Preview와 테스트에서 독립적으로 그릴 수 있게 한다.
- 재사용 컴포넌트는 가능한 한 stateless로 만들고 `Modifier`를 제공한다. UI 내부에서만 쓰는 펼침·스크롤·애니메이션 상태는 가장 가까운 소유자에 둔다. 외부 제어가 필요하면 상태를 끌어올린다.
- 고정된 배치에서 특정 영역의 콘텐츠만 교체해야 하면 `title`, `actions`, `content`처럼 역할이 드러나는 slot을 쓴다. 미래의 변형을 예상해 slot을 늘리지 않는다.
- 자식 컴포넌트가 부모의 상태와 동작을 함께 공유하고 조합 순서에 의미가 있을 때만 receiver scope 기반 compound component를 쓴다. scope API의 크기, 상태 변경 시 재구성, 접근성을 검토한다.
- 시각적 유사성만으로 공용 컴포넌트를 만들지 않는다. 동일한 이유로 함께 변경될 때 추출한다. 단순한 분기나 제한된 중복은 허용한다.

## Markdown 뷰어 경계

파일 선택, 홈·읽기, 로컬 이미지 폴더 연결, 링크 처리와 수식·Mermaid를 포함한 표시 범위의 제품 방향은 [제품 문서](../product/overview.md)에서 관리한다. 책갈피와 즐겨찾기는 기기 내 영속 보관한다. 파싱·렌더링 방식, 영속화 기술, 파일·폴더 권한 구현과 세부 문법 호환 범위는 아직 결정하지 않았다. Room·DataStore 등의 선택은 구현 단계에서 검토하며 이 문서에서 선행 확정하지 않는다. 화면 이동 라이브러리는 Navigation 3로 정했으며 세부 목적지·백스택 구성은 확정된 화면 흐름에 맞춰 정한다.

## 근거 자료

- [Android 아키텍처 권장사항](https://developer.android.com/topic/architecture/recommendations): UI·데이터 계층, UDF, 화면 ViewModel, 저장소와 필요 기반 유즈케이스.
- [Android 도메인 계층](https://developer.android.com/topic/architecture/domain-layer): 복잡하거나 재사용되는 비즈니스 로직에 유즈케이스 도입. 공식 가이드의 `domain` 정의와 이 프로젝트의 도메인 계약 소유 규칙의 차이를 위에 명시했다.
- [Compose 상태 끌어올리기](https://developer.android.com/develop/ui/compose/state-hoisting): 상태는 이를 읽고 쓰는 최소 공통 조상에 둔다.
- [Compose slot API](https://developer.android.com/develop/ui/compose/layouts/basics): Composable 람다로 교체 가능한 영역을 제공한다.
- [Compose UI 조합하기 심화](https://wisemuji.medium.com/jetpack-compose-ui-%EC%A1%B0%ED%95%A9-composition-%ED%95%98%EA%B8%B0-%EC%8B%AC%ED%99%94-33910e8f09df): slot과 compound component의 쓰임과 과도한 추상화의 비용.
