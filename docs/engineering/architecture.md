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

Gradle 모듈은 제품 앱 `app`, 공통 Android 라이브러리 `designsystem`, 별도 카탈로그 앱 `:designsystem:catalog` 세 개다. 카탈로그 앱은 `designsystem/catalog` 아래에 두며 사용자 요청으로 제품 실행과 분리했다. 제품의 `ui`, `domain`, `data`는 `app` 내부 패키지 경계로 유지하며, S01에서 `ui/home`, `ui/reader`, `ui/navigation`의 제품 호스트를 추가했다. S02는 `domain/document`의 문서 참조·원문·실패 결과·저장소 계약과 `data/document`의 ContentResolver 로더·인코딩 판독을 추가한다. `di/DocumentModule`이 구현을 바인딩한다. 문서 참조는 Android 타입을 포함하지 않는 불투명 문자열이며 URI 해석·스트림·권한은 플랫폼 경계가 소유한다. 파서·영속 저장의 빈 계약은 선행 추가하지 않는다. 패키지가 물리적 의존성을 강제하지 않으므로 리뷰와 테스트에서 import 방향을 확인한다. 의존성 주입은 Hilt를 사용하며, Hilt 모듈과 저장소 구현 바인딩은 앱의 DI 구성 코드에 둔다. `domain` 모델·인터페이스·유즈케이스에는 Hilt 또는 Android 어노테이션을 붙이지 않는다. `MDiveApplication`의 `@HiltAndroidApp`, `MainActivity`의 `@AndroidEntryPoint`, 화면별 `@HiltViewModel` 및 KSP로 Hilt를 연결한다.

`designsystem`은 테마·글꼴·stateless Compose 컴포넌트와 카탈로그 표현을 소유하며 `app`에 의존하지 않는다. `app`의 main/release 제품 호스트와 독립 `:designsystem:catalog` 앱이 `designsystem`을 사용한다. 카탈로그 화면 MVI와 카탈로그 테스트는 `designsystem/catalog`에 둔다. 카탈로그는 제품 앱을 참조하지 않으며 자체 MVI 베이스를 소유한다. 제품 테마는 시스템 라이트·다크를 따르는 `MDiveDesignTheme`이며 카탈로그의 테마 비교 상태와 분리한다. 공통 값·카탈로그 원본과 생성 과정은 [디자인 시스템](../product/design-system.md)에서 관리한다. 제품 `app`은 `MainActivity` 하나를 런처로 제공하고, 카탈로그는 별도 applicationId `com.parka.mdive.catalog`로 설치한다.

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

공통 외형과 상태는 `designsystem`의 MDive 컴포넌트가 소유한다. 바텀시트는 Material3 `ModalBottomSheet`의 기본 모션·제스처·접근성을 기반으로 하고 MDive 토큰으로 외형을 지정한다. 공통 외형을 정의한다는 이유로 표준 동작을 정적 패널로 대체하지 않는다. 버튼·아이콘·문서 목록·검색·탭은 Foundation 기반 컨트롤로 입력·포커스·접근성 의미를 제공하며 Material 기본 외형을 그대로 제품 스타일로 사용하지 않는다. 테마·텍스트 렌더링 기반의 재사용은 허용한다. 생성 토큰·아이콘·상태 목록과 카탈로그의 원본 책임은 [디자인 시스템](../product/design-system.md)을 따른다.

- 화면 진입 Composable만 ViewModel을 가져온다. 그 아래의 화면 콘텐츠는 화면 State와 이벤트 콜백을 인자로 받아 Preview와 테스트에서 독립적으로 그릴 수 있게 한다.
- 재사용 컴포넌트는 가능한 한 stateless로 만들고 `Modifier`를 제공한다. UI 내부에서만 쓰는 펼침·스크롤·애니메이션 상태는 가장 가까운 소유자에 둔다. 외부 제어가 필요하면 상태를 끌어올린다.
- 고정된 배치에서 특정 영역의 콘텐츠만 교체해야 하면 `title`, `actions`, `content`처럼 역할이 드러나는 slot을 쓴다. 미래의 변형을 예상해 slot을 늘리지 않는다.
- 자식 컴포넌트가 부모의 상태와 동작을 함께 공유하고 조합 순서에 의미가 있을 때만 receiver scope 기반 compound component를 쓴다. scope API의 크기, 상태 변경 시 재구성, 접근성을 검토한다.
- 시각적 유사성만으로 공용 컴포넌트를 만들지 않는다. 동일한 이유로 함께 변경될 때 추출한다. 단순한 분기나 제한된 중복은 허용한다.

## Markdown 뷰어 경계

파일 선택, 홈·읽기, 로컬 이미지 폴더 연결, 링크 처리와 수식·Mermaid를 포함한 표시 범위의 제품 방향은 [제품 문서](../product/overview.md)에서 관리한다. 책갈피와 즐겨찾기는 기기 내 영속 보관한다. 기본 파싱·DS 렌더링은 아래 S03 경계를 따른다. 수식은 아래 S06, Mermaid는 S07, 이미지는 S08+S09 경계를 따른다. 후속 렌더 엔진·홈/도구 영속화 기술과 확장 문법의 세부 호환 범위는 해당 단계에서 결정한다. Room·DataStore 등의 선택은 구현 단계에서 검토하며 이 문서에서 선행 확정하지 않는다. 화면 이동 라이브러리는 Navigation 3로 정했으며 세부 목적지·백스택 구성은 확정된 화면 흐름에 맞춰 정한다.

## 근거 자료

- [Android 아키텍처 권장사항](https://developer.android.com/topic/architecture/recommendations): UI·데이터 계층, UDF, 화면 ViewModel, 저장소와 필요 기반 유즈케이스.
- [Android 도메인 계층](https://developer.android.com/topic/architecture/domain-layer): 복잡하거나 재사용되는 비즈니스 로직에 유즈케이스 도입. 공식 가이드의 `domain` 정의와 이 프로젝트의 도메인 계약 소유 규칙의 차이를 위에 명시했다.
- [Compose 상태 끌어올리기](https://developer.android.com/develop/ui/compose/state-hoisting): 상태는 이를 읽고 쓰는 최소 공통 조상에 둔다.
- [Compose slot API](https://developer.android.com/develop/ui/compose/layouts/basics): Composable 람다로 교체 가능한 영역을 제공한다.
- [Compose UI 조합하기 심화](https://wisemuji.medium.com/jetpack-compose-ui-%EC%A1%B0%ED%95%A9-composition-%ED%95%98%EA%B8%B0-%EC%8B%AC%ED%99%94-33910e8f09df): slot과 compound component의 쓰임과 과도한 추상화의 비용.

## S01 제품 호스트 경계 · 당시 구현 기록

아래는 S01 당시 경계이며 S02 연결은 다음 절을 따른다. 홈은 DS 첫 실행 상태와 비활성 파일 열기를 표시한다. 실제 선택기를 연결하기 전까지 읽기 화면으로 이동하거나 예시 파일을 제품 데이터로 표시하지 않는다. 읽기 로딩·빈 내용·전체 실패는 독립 콘텐츠와 Preview/테스트에서 검증한다. 실패 복구 콜백은 파일 처리 없이 호스트로 전달하며 실제 재시도·재선택은 S02에서 연결한다. 상태 수집은 `collectAsStateWithLifecycle`, Effect는 화면별 `repeatOnLifecycle(STARTED)` 단일 수집자를 사용한다. 시스템 인셋과 상태 패널의 중앙/넘침 스크롤 배치는 화면 호스트가 소유한다.

Navigation 3·Lifecycle·AndroidX Hilt·Dagger Hilt/KSP·Serialization 의존성은 main/release 제품 조립에 필요해 추가했다. 테스트 의존성의 Espresso 3.7.0·AndroidX Test JUnit 1.3.0은 기존 API 37 reflection 오류를 피하는 기기 검증용 갱신이다. minSdk와 제품 앱 배포 버전은 유지한다. 이후 사용자 요청으로 제품 실행과 분리한 카탈로그 앱 모듈을 추가했다.

## S02 파일 로더 경계

홈의 Activity Result `OpenDocument` 선택 결과를 UI 소유 Navigation 3 Reader key의 문자열 입력으로 전달한다. Reader ViewModel은 domain 저장소를 호출하고 로딩·빈 원문·실패·원문 준비 상태를 노출한다. 원문 준비 시 제품은 중간 안내만 표시하고 debug 개발 호스트에서 실제 원문을 검증한다. 파일 확장자·인코딩·크기와 취소 정책은 [제품 로더 명세](../product/document-viewing.md#s02-로더-경계--확정)를 따른다.

ContentResolver 메타데이터·스트림은 IO에서 처리하며 실제 읽기 바이트 상한을 적용하고 스트림을 닫는다. 코루틴 취소는 실패로 변환하지 않고 늦은 응답의 상태 반영을 차단한다. 외부 provider의 blocking IO가 즉시 중단됨을 보장하지는 않는다. 영속 URI 권한이나 저장 목록은 추가하지 않는다.

## S03 기본 파서·DS 어댑터 경계

S03의 해석 기준과 중간 원문 대체 정책은 [제품 프로필](../product/document-viewing.md#s03-기본-본문-프로필--확정)이 소유한다. `org.commonmark:commonmark:0.31.0` core로 CommonMark 0.31.2를 해석하고 data 내부에서 앱 소유 블록·인라인 모델로 변환한다. 특정 라이브러리 AST를 domain·UI·DS에 노출하지 않는다. UI는 준비된 모델을 기존 `MDiveMarkdownHeading/Text/Quote/ListItem/Divider`로 조합하며 DS는 파일이나 파서를 알지 못한다. 파싱은 메인 스레드 밖에서 수행하고 읽기 취소·늦은 결과 차단을 유지한다.

선택 근거는 작은 core와 원문 위치(`BLOCKS_AND_INLINES`, UTF-16 `inputIndex`·`length`) 제공이다. 외부 리소스를 요청하거나 HTML을 실행하는 렌더러는 도입하지 않는다. 미지원 영역은 source span에 대응하는 원문을 보존하며 GFM 플러그인·엔진·새 모듈을 선행 추가하지 않는다. Java 11·minSdk 24 환경을 유지하고 core의 `List.of()` 등 API 호환을 위해 앱에 `desugar_jdk_libs:2.1.5` core-library desugaring을 설정한다. 실제 API 24 기기 실행 여부는 구현 계획의 검증 기록과 구분한다.

근거: [유지관리자 README](https://raw.githubusercontent.com/commonmark/commonmark-java/commonmark-parent-0.31.0/README.md), [태그의 CommonMark 명세](https://raw.githubusercontent.com/commonmark/commonmark-java/commonmark-parent-0.31.0/commonmark-test-util/src/main/resources/spec.txt), [SourceSpan](https://raw.githubusercontent.com/commonmark/commonmark-java/commonmark-parent-0.31.0/commonmark/src/main/java/org/commonmark/node/SourceSpan.java), [Android Java API desugaring](https://developer.android.com/studio/write/java8-support), [Java 11 지원 API 목록](https://developer.android.com/studio/write/java11-default-support-table).

파서 기본 중첩 한도(열린 블록·인라인 각각 100)를 유지한다. 어댑터도 컨테이너·강조 재귀 깊이 128에서 해당 부분을 원문으로 대체하는 안전장치를 둔다. 최상위 깊이 0에서 인용/목록 컨테이너 진입, 인라인 강조/굵게 진입마다 1씩 센다. 미인식 확장 구분자는 텍스트로 남지만 그 안의 기본 CommonMark는 해석될 수 있다(예: `~~**굵게**~~`는 취소선 구분자를 남기고 굵게를 적용). 이를 확장 문법 지원으로 간주하지 않는다.

좁은 폭·큰 글자의 깊은 중첩에서는 UI가 가용 폭을 확인한다. 인용·목록의 다음 들여쓰기 후 폭이 `128dp × fontScale`보다 작아지면 추가 들여쓰기를 멈춘다. 최상위에서 폭을 얻어 하위에 전달하고 인용의 inset 및 실측한 목록 마커 폭을 차감한다. 인용의 intrinsic 측정 안에는 `BoxWithConstraints`를 중첩하지 않는다. 인용은 본문을 현재 폭에 이어 표시하고 목록은 번호/불릿을 별도 줄에 유지한다. 이는 화면 조합의 폭 보호이며 파서 의미나 원문을 변경하지 않는다.

## S04 GFM 파서·DS 연결 경계

제품 인식 규칙과 GFM 예외는 [S04 프로필](../product/document-viewing.md#s04-gfm-본문-프로필--확정)이 소유한다. 기존 core와 같은 0.31.0의 `commonmark-ext-gfm-tables`·`commonmark-ext-gfm-strikethrough`를 추가한다. AST는 data 내부에 유지하고 domain의 표 열/정렬·행, 목록 항목별 task 상태, 인라인 취소선 속성으로 변환한다. UI는 기존 `MDiveMarkdownTable`, 읽기 전용 `MDiveListMarker.Task`/`MDiveCheckbox`, `MDiveTextRun.strike`를 사용한다. 새 모듈·렌더러·네트워크 기능은 추가하지 않는다.

표 확장의 기본 factory가 헤더보다 구분 행 열 수가 많은 입력도 허용하므로, data의 `GfmTableFactory`가 정확한 동수를 먼저 검사한 뒤 고정 버전 factory에 위임한다. 라이브러리의 `internal` 패키지 Java API를 사용하는 작은 호환 경계이므로 버전 갱신 시 표 fixture를 함께 확인한다. 표 확장이 escaped pipe를 제거한 뒤 인라인을 파싱하므로 미지원 링크·이미지 등의 source offset을 원래 셀 위치로 역변환한다. 표 셀 안전 한도 초과 시에는 표 확장을 끄고 기본 본문으로 다시 파싱해 읽기를 유지한다.

task는 해석된 Text를 정규식으로 바꾸는 확장 대신 첫 문단의 원문 시작을 확인해 escape·문자 참조의 오인식을 방지한다. 취소선은 공식 `requireTwoTildes(true)` 옵션으로 제한한다. 기존 중첩 폭 보호를 task에도 적용하며 표의 가로 스크롤은 DS 블록 내부가 소유한다.

근거: [0.31.0 표 factory](https://github.com/commonmark/commonmark-java/blob/commonmark-parent-0.31.0/commonmark-ext-gfm-tables/src/main/java/org/commonmark/ext/gfm/tables/internal/TableBlockParser.java), [0.31.0 취소선 API](https://github.com/commonmark/commonmark-java/blob/commonmark-parent-0.31.0/commonmark-ext-gfm-strikethrough/src/main/java/org/commonmark/ext/gfm/strikethrough/StrikethroughExtension.java).


## S05 코드·강조·복사 경계

제품 언어·별칭·대체 표시·복사 정책은 [S05 프로필](../product/document-viewing.md#s05-코드-프로필--확정)이 소유한다. `dev.snipme:highlights:1.1.0`의 동기 `getCodeStructure()`를 기존 파서의 `Dispatchers.Default` 안에서 블록별 새 인스턴스로 호출한다. 17개 언어의 기본 어휘 강조를 선택한 이유는 순수 Kotlin/JVM으로 별도 WebView·JavaScript 실행 엔진 없이 기존 DS의 Keyword/String/Plain 범위에 연결할 수 있기 때문이다. 최신 문법 전체 호환을 보장하지 않는다. 엔진 타입과 언어 매핑은 data 내부에 두고 UTF-16 범위·의미 tone만 domain 코드 모델로 전달한다. 잘못된 범위나 분석 예외는 강조만 제거하며 코루틴 취소는 다시 던진다. 동기 CPU 분석의 즉시 취소를 보장하지 않는다.

CommonMark literal의 정규화된 본문을 그대로 복사하지 않는다. data의 `CodeSourceReader`는 literal의 구조 제거 후 선행 공백(부분 소비된 탭 잔여열 포함)과 source span의 원래 내용·CR/LF·EOF를 조합한다. fence·info·인용/목록/들여쓰기 코드 구조만 제거한다. S05 당시 `mermaid` fence는 전체 원문으로 남겼으며 S07에서 별도 다이어그램 모델로 연결한다.

UI는 `MDiveCodeBlock`에 원문·언어 라벨·강조 범위·복사 callback을 전달한다. 복사는 Reader Intent → Channel Effect → Route의 새 Compose `LocalClipboard`/`setClipEntry`로 실행한다. ViewModel에 Android 클립보드 타입을 넣지 않는다. 플랫폼 쓰기 직후 클립보드를 다시 읽지 않는다. API33 이상은 시스템 성공 안내, 이전 버전은 공통 `MDiveToast`, 실패는 같은 공통 피드백과 기존 복사 버튼으로 재시도한다. 일시 안내의 표시·3초 유지와 반복 표시 시간 갱신은 Route의 UI 로컬 상태가 소유한다. 새 공통 UI·모듈은 추가하지 않는다.

근거: [Highlights 1.1.0 소스](https://github.com/SnipMeDev/Highlights/tree/1.1.0), [Compose Clipboard](https://developer.android.com/reference/kotlin/androidx/compose/ui/platform/Clipboard), [Android 복사 피드백](https://developer.android.com/develop/ui/compose/touch-input/copy-and-paste#feedback).


## S06 수식 경계

제품 입력·실패·원문 시트 정책은 [S06 프로필](../product/document-viewing.md#s06-수식-프로필--확정)이 소유한다. GitHub 문서의 수식 표기법을 지향하되 GitHub의 MathJax 전체와 동일한 프로필은 아니다. `ru.noties:jlatexmath-android:0.2.0`을 오프라인 네이티브 엔진으로 사용한다. WebView 없이 기존 본문·DS slot에 연결할 수 있고 minSdk24를 유지하기 때문에 선택했다. 엔진은 보관 처리된 라이브러리이므로 지원 명령을 앱 프로필로 한정하며, 라이선스·폰트 고지를 함께 보존한다.

엔진/파서 타입과 Android 그래픽 구현은 data 내부에서만 사용한다. domain에는 TeX·구분자 포함 원문과 앱 소유 픽셀·크기·기준선 정보를 전달하며 Android/Compose 타입을 넣지 않는다. 인라인은 text 스타일, 블록은 display 스타일로 렌더링하고 UI에서 본문 크기·fontScale·테마에 맞게 표시한다. 자연어 수학 낭독 대신 TeX 대체 설명을 제공한다. 긴 인라인은 별도 행으로 옮기고 수식 영역만 가로 스크롤한다.

렌더 실패는 문서 열기 실패와 구분해 해당 수식에 오류·원문 보기만 제공한다. 원문 시트의 상태·Intent·닫기 동작은 Reader가 소유하고 DS 시트는 데이터와 이벤트만 받는다. 정상 수식에 실패 배너나 임의 상호작용을 붙이지 않는다. 일반 코드·링크/HTML 보존 경계는 유지한다. Mermaid는 아래 S07에서 별도 처리한다.

엔진은 전역 레지스트리의 동시 변경을 피하도록 직렬 호출한다. 입력 길이·중괄호 깊이·명령 수·행렬 구조·개별 및 문서 전체 픽셀 예산을 적용하고 초과 입력은 해당 수식만 실패 처리한다. 엔진의 StackOverflowError는 해당 수식의 실패로 격리한다. 동기 엔진 호출의 즉시 취소는 보장하지 않으며 취소 뒤 늦은 화면 반영은 기존 읽기 작업 경계에서 차단한다. 크기 예산은 64px/em 기준의 앱 렌더 결과에 적용하며 제품의 구체 한계는 S06 프로필에 기록한다.

근거: [엔진 소스·보관 상태](https://github.com/noties/jlatexmath-android), [엔진 라이선스](https://github.com/noties/jlatexmath-android/blob/android/LICENSE), [GitHub 수식 작성 안내](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/writing-mathematical-expressions).


## S07 Mermaid 플랫폼 경계

제품의 5종·입력/연결선/시간 한계·원문·확대 정책은 [S07 프로필](../product/document-viewing.md#s07-mermaid-프로필--확정)이 소유한다. Mermaid 11.15.0 공식 npm 배포의 self-contained IIFE를 APK assets에 그대로 고정한다. JS와 라이선스·종속성 고지를 보존하며 런타임 CDN이나 추가 Gradle 라이브러리는 사용하지 않는다. 버전·해시는 assets/mermaid/README.md에 기록한다.

`data/document/MermaidProfile`은 지원 종류와 실행 가능한 설정·HTML·링크·CSS·리소스를 제한한다. CommonMark AST에서 코드 본문과 fence 포함 원문을 앱 소유 `MarkdownMermaid`로 변환하며 미완성/미지원 입력도 이 모델의 실패 상태로 보존한다. domain에는 Android·WebView·SVG·Mermaid 파서 타입을 넣지 않는다.

WebView는 뷰 수명주기를 필요로 하므로 `ui/reader/MermaidWebView`의 플랫폼 어댑터 내부에 격리한다. 이는 데이터 저장소가 아니라 Compose slot의 렌더링 구현이다. 앱 소유 HTML/JS 세 파일만 가상 HTTPS origin에서 제공하고 모든 나머지 요청은 명시적으로 차단해 네트워크 fallback을 방지한다. 파일/content 접근·DOM 저장소·새 창을 끄고 JS native bridge를 노출하지 않는다. trusted host는 JSON 데이터만 받아 strict 프로필로 파싱하고 종류별 실제 연결 수를 확인한 뒤 SVG를 렌더링한다. 출력의 실행 요소·링크·외부 참조를 다시 검사하고 SVG viewBox를 전체 맞춤으로 표시한다. `minSdk24`와 오래된 WebView의 JavaScript 호환은 별개이며 엔진 구문 오류/준비 실패도 해당 영역의 원문 대체로 처리한다.

로딩/정상/실패는 어댑터의 UI 로컬 상태이며 DS는 엔진을 모르는 기존 `MDiveRenderedBlock`/`MDiveContentFallback` slot과 callback을 받는다. main thread의 10초 타이머와 evaluateJavascript polling에는 dispose/완료 guard를 두고, timeout·실패·준비 후 renderer 종료에서 즉시 부모에서 제거하고 WebView를 destroy한다. 늦은 callback은 무시하며 중복 해제는 안전하다.

원문·확대는 Reader의 State/Intent가 소유한다. 확대는 같은 Reader의 전체 크기 Dialog 오버레이이며 별도 문서 목적지/백스택 key나 원문 직렬화를 추가하지 않는다. 기존 문서를 재읽지 않고 같은 앱 모델을 사용하므로 권한 상실·원문 변경에 따라 다른 다이어그램으로 바뀌지 않는다. 본문 LazyColumn을 유지한 채 닫기·시스템 뒤로로 돌아오고 원문/수식/확대를 동시에 열지 않는다. 핀치/팬/배율은 오버레이의 최소 UI 로컬 상태이며 1–8배·경계 내 이동·맞춤 복원을 적용하고 재진입하면 초기화한다. 접근성에는 종류와 Mermaid 원문을 설명하고 핀치 대체 확대/축소/맞춤 버튼을 제공한다. 기존 DS 아이콘 버튼과 스켈레톤을 재사용한다. 확대 조작은 하단의 단일 표면 그룹으로 조합하고 공통 아이콘 원본에 minus/fit을 추가해 양쪽 카탈로그에 생성한다. 준비 중은 그림과 같은 영역의 비상호작용 스켈레톤으로 표시한다.

근거: [고정 Mermaid 릴리스](https://github.com/mermaid-js/mermaid/releases/tag/mermaid@11.15.0), [Mermaid API](https://mermaid.js.org/config/usage.html), [Android 로컬 콘텐츠](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content), [WebView 프로세스 수명주기](https://developer.android.com/develop/ui/views/layout/webapps/managing-webview).


### S07 읽기 세션 SVG 캐시

`ReaderScreen`은 현재 문서에 종속된 `MermaidSessionCache`를 기억하고 본문·확대에 같은 인스턴스를 전달한다. 완료된 안전 SVG와 엔진 실패만 보관하며 WebView·Context·진행 중 작업은 보관하지 않는다. LazyColumn에서 뷰가 dispose돼도 완료 결과는 남으므로 스크롤 복귀·확대에서 엔진을 다시 실행하지 않는다. 준비 도중 dispose된 작업에는 pending 항목을 남기지 않는다.

키는 코드 본문·테마·엔진/프로필·1024px 고정 해석 폭이다. SVG의 viewBox와 표시 영역의 맞춤은 분리하므로 본문·확대의 표시 폭이 달라도 같은 해석 폭의 SVG를 사용할 수 있다. 소스·테마·프로필·해석 폭이 달라지면 다른 항목이고, 다른 문서를 열면 별도 세션 캐시를 만든다. LRU는 최대 64개·키와 SVG의 UTF-16 크기를 포함한 4MiB 예산이며 초과한 항목은 제거하거나 보관하지 않는다. 제거된 결과는 다시 나타날 때 재렌더링할 수 있다. 앱 전역·영속 저장·문서 간 공유는 하지 않는다.

캐시 hit는 새 WebView에 이미 검사한 SVG만 제공한다. 이 페이지에는 Mermaid JS가 없고 JavaScript도 끄며 CSP script-src none과 기존 외부 요청 차단을 유지한다. 확대 품질을 위해 래스터가 아닌 SVG를 재사용한다. cache miss에서만 기존 strict 엔진·출력 검사를 거치며, 사용자 입력을 캐시 페이지 HTML에 직접 넣지 않는다. 실제 renderer 호출 횟수와 JavaScript 설정·픽셀 회귀로 스크롤/확대 재사용을 검증한다.

실제 샘플 검증에서 `height:100%` 부모 체인이 0으로 계산돼 SVG viewBox가 정상이어도 그림 높이가 0인 결함을 발견했다. 호스트와 캐시 페이지의 그림 컨테이너는 fixed/inset 0으로 viewport에 맞추고, Ready 상태·외곽 크기만으로 표시 검증을 대체하지 않는다. 실제 화면의 그림 영역 PixelCopy 검사와 SAF 샘플을 함께 검증한다.


## S08+S09 이미지·폴더 연결 경계

지원 형식·경로·복구·요청 한계·캐시·확대는 [이미지 제품 프로필](../product/document-viewing.md#s08s09-이미지-프로필--확정)이 소유한다. CommonMark 이미지 AST는 data에서 앱 소유 `MarkdownImage`로 변환하고, 문단·목록·인용·표 안의 이미지도 `MarkdownRun.image`로 전달한다. 후속 링크/HTML의 실행 정책을 바꾸지 않는다.

`domain/document/ImageRepository`는 문서 참조·이미지 목적지 문자열·폴더 연결 문자열과 앱 소유 픽셀/실패 결과만 노출한다. Android URI·ContentResolver·Bitmap·SVG·HTTP 엔진 타입은 data 내부에 둔다. `di/DocumentModule`에서 구현을 바인딩하며 UI는 구체 저장소를 알지 못한다. 화면 ViewModel이 로딩·실패·준비 상태와 복구 Intent를 소유하고, 폴더 선택은 Effect로 Route의 Activity Result launcher에 전달한다. 폴더 선택 취소는 기존 연결과 본문을 유지한다.

`ContentImageRepository`는 IO에서 SAF tree의 실제 child 항목을 이름으로 조회한다. percent 인코딩을 한 번 해석한 상대 경로를 정규화하고, 기준 폴더 밖 이동과 절대/금지 URI를 차단한다. document ID를 파일 경로처럼 이어 붙이지 않는다. 읽기 grant를 영속화한 뒤 문서 참조별 tree 연결을 SharedPreferences에 저장한다. 이는 이미지 연결의 최소 영속 저장이며 H01 최근/즐겨찾기 저장소를 선행 구현하지 않는다. 실제 읽기 시 파일 없음·권한 상실을 다시 판정한다.

사용자 선호에 따라 실제 디코딩은 `io.coil-kt.coil3:coil-core:3.3.0`과 `coil-svg:3.3.0`의 `ImageLoader.execute`가 담당한다. 현재 Kotlin 2.2.10·minSdk24와 호환되는 고정 버전이며 SVG 디코더는 AndroidSVG를 사용한다. 로더는 data에서 재사용하고 Coil/Android 타입은 외부로 노출하지 않는다. SVG의 XML을 사전 검사해 DTD·스크립트·외부 리소스·지원 밖 요소를 거부하고 내부 entity 확장을 끈다. SVGZ는 지원하지 않는다. 요소 10,000개·깊이 128·내부 참조 10,000개·확장 작업량 50,000개의 예산과 참조 그래프 순환 검사를 적용하고 렌더러 StackOverflowError도 해당 그림의 실패로 격리한다. 스타일 텍스트는 XML 토큰 경계를 합쳐 검사하며 stylesheet의 모든 `url(...)`을 거부한다. 도형 속성의 안전한 내부 `url(#id)` 참조는 허용한다. Coil 호출 전에 실제 입력 바이트와 원본 출력 치수를 검사하고 GIF는 animated decoder 없이 첫 프레임만 표시한다. Android/엔진 타입을 숨기면서 기존 DS slot에 연결하기 위해 앱 소유 ARGB 픽셀을 반환한다.

웹에는 `com.squareup.okhttp3:okhttp:4.12.0`을 사용한다. 쿠키/인증·프록시 인증과 자동 리다이렉트를 사용하지 않으며, 수동 리다이렉트의 각 HTTPS 목적지를 재검사한다. 모든 hop은 하나의 전체 deadline을 공유한다. 요청은 앱 이름·설치된 versionName·공개 프로젝트 연락 URL·OkHttp 버전으로 정직하게 식별한다. 문서·사용자·기기 식별자는 User-Agent에 넣지 않는다. 일반 라이브러리 User-Agent를 거절하는 서버에서도 앱을 정확히 식별하기 위한 헤더이며 인증이나 브라우저 위장을 추가하지 않는다. 이 의존성은 인증 없는 요청과 호출 제한을 명시하고 재현 가능한 HTTPS 경계 테스트를 수행하기 위해 추가했다. Coil·AndroidSVG·OkHttp는 Apache 2.0이며 새 모듈은 만들지 않는다.

재사용할 디코딩 결과는 Coil의 제한된 메모리 캐시로 보관한다. 화면에 표시 중인 결과와 확대 결과는 캐시 회수와 분리해 유지하므로 동시에 보이는 그림의 합이 캐시 예산을 넘더라도 스켈레톤/재요청이 반복되지 않는다. 캐시 예산은 활성 화면의 픽셀·Compose 표시 Bitmap까지 포함한 앱 전체 메모리 상한이 아니다. 보이는 이미지의 요청과 확대는 같은 결과를 사용한다. 웹 원본은 Coil의 디스크 캐시를 사용하며 앱 cache 디렉터리에 둔다. Coil `MemoryCache` 64MiB·`DiskCache` 100MiB와 요청별 캐시 정책으로 예산과 읽기/쓰기를 관리하고 별도의 앱 LRU·디스크 회수 구현을 두지 않는다. 로컬 원본은 웹 디스크 캐시에 기록하지 않는다. 명시적 재시도는 캐시 읽기를 우회한다. 테마는 이미지 원본을 변경하지 않으며 문서·폴더 연결 변경 후 늦은 결과가 새 상태를 덮지 않도록 요청 세대를 구분한다. 영속 웹 캐시는 OS가 회수할 수 있으며 외부 파일 변경을 자동 감시하지 않는다.

이미지 확대는 Reader 내부 Dialog 오버레이다. Navigation key나 문서 재읽기를 추가하지 않고 본문 LazyColumn을 유지한다. 핀치·팬·배율은 오버레이의 UI 로컬 상태이며 그림/실패/스켈레톤의 공통 외형과 접근성 액션은 DS가 소유한다. DS는 파일·권한·네트워크·도메인 규칙을 알지 못한다.

근거: [Android SAF와 tree 제한](https://developer.android.com/training/data-storage/shared/documents-files), [BitmapFactory](https://developer.android.com/reference/android/graphics/BitmapFactory), [Coil 3.3.0](https://github.com/coil-kt/coil/tree/3.3.0), [AndroidSVG 공식 지원 범위](https://bigbadaboom.github.io/androidsvg/), [OkHttp 4.12 요구사항·라이선스](https://github.com/square/okhttp/tree/parent-4.12.0), [OkHttp TLS 테스트](https://github.com/square/okhttp/blob/parent-4.12.0/okhttp-tls/README.md). 실제 검증·미검증은 구현 계획의 현재 인계를 따른다.
