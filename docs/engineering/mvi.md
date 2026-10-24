# MDive MVI 화면 계약

상태: 제품 기획 전 기본 규칙. 실제 화면을 만들며 필요한 부분만 구체화한다.

## 기본 흐름

```text
사용자 입력 -> Intent -> ViewModel -> 도메인 저장소/유즈케이스
                           ├─ reduce -> StateFlow<State> -> Compose 렌더링
                           └─ emitEffect -> Channel<Effect> -> UI에서 한 번 실행
```

모든 화면은 `State`, `Intent`, `Effect`, `BaseViewModel<State, Intent, Effect>` 구조를 사용한다. 공통 베이스는 [UiContract.kt](../../app/src/main/java/com/parka/mdive/ui/base/UiContract.kt)와 [BaseViewModel.kt](../../app/src/main/java/com/parka/mdive/ui/base/BaseViewModel.kt)에 있다. reducer 역할은 베이스의 `reduce { ... }`가 맡는다. 화면별 reducer 클래스는 필수가 아니다.

## 화면 구성

- 화면마다 `State : UiState`, `Intent : UiIntent`, `Effect : UiEffect`, `ViewModel : BaseViewModel<State, Intent, Effect>`를 정의한다. 값이나 종류가 당장 없어도 화면 계약의 네 부분은 유지한다. 작은 화면은 같은 파일에 둘 수 있다.
- `State`는 불변 `data class` 또는 `sealed interface`, `Intent`와 `Effect`는 보통 `sealed interface`로 만든다. 이름은 화면 이름을 붙여 충돌을 피한다.
- 화면 ViewModel은 `onIntent(intent)`를 구현한다. Hilt를 사용할 구체 ViewModel에는 `@HiltViewModel`과 주입 생성자를 적용한다. 제네릭 베이스에는 Hilt 어노테이션을 붙이지 않는다.
- 진입 Composable은 `uiState`를 `collectAsStateWithLifecycle()`로 수집하고 사용자 입력을 `onIntent`로 전달하며 `effects`를 수집한다.
- 콘텐츠 Composable은 `State`와 필요한 이벤트 콜백만 받아 UI를 그린다. ViewModel과 저장소를 직접 참조하지 않는다. Preview에서 상태를 넣어 그릴 수 있어야 한다.

## 상태 설계

- `State`는 화면을 다시 그리는 데 필요한 현재 사실을 담는 불변 값이다. 도메인 모델을 그대로 노출할지 화면 전용 모델로 바꿀지는 표현 요구에 따라 결정한다.
- 동시에 존재할 수 있는 값은 `data class`의 필드로, 로딩·성공·오류처럼 상호 배타적인 상태는 `sealed interface`로 표현하는 것을 고려한다. 모든 화면에 같은 상태 틀을 강제하지 않는다.
- 최초 로딩, 빈 결과, 실패, 재시도, 기존 내용을 유지한 갱신 중 상태를 화면 요구에 맞게 구별한다. 오류 메시지는 사용자에게 보여줄 수 있는 형태로 변환한다.
- 베이스가 비공개 `MutableStateFlow`와 읽기 전용 `StateFlow`를 소유한다. 상태 변경은 `reduce { ... }` 안에서 새 상태를 반환한다. 외부 Flow를 관찰할 때도 그 결과를 `reduce`로 반영한다. 같은 데이터를 여러 변경 가능한 상태에 중복 보관하지 않는다.
- 회전이나 프로세스 재생성 뒤에도 필요한 입력은 `SavedStateHandle` 또는 저장소에 보관할지 결정한다. 스크롤·펼침처럼 순수 UI 동작 상태는 필요한 최소 공통 조상에 둔다.

## 액션과 비동기 처리

- 사용자 입력은 `Intent`로 표현하고 `onIntent`를 단일 진입점으로 사용한다. 이름은 `Retry`, `DocumentSelected`처럼 사용자 의도가 드러나게 한다.
- 비동기 작업은 구체 ViewModel의 `viewModelScope`에서 실행한다. 중복 탭, 이전 요청의 늦은 응답, 취소가 실제로 문제가 되는 화면에서는 해당 정책을 정한다. 예외가 조용히 사라지지 않도록 상태 또는 effect로 반영한다.
- `reduce`는 현재 상태를 바탕으로 다음 상태를 원자적으로 계산한다. 외부 부작용은 `reduce` 블록 밖에서 수행한다. 복잡한 전이 규칙은 별도 순수 함수로 분리해 테스트할 수 있다.

## Effect

- `Effect`는 화면 이동 요청, Snackbar 표시, 시스템 파일 선택처럼 UI가 한 번 실행할 동작이다. 구체 ViewModel에서 `emitEffect(effect)`를 호출하고, UI가 effect를 해석해 실행한다. Navigation 3 백스택은 UI가 소유한다.
- 베이스는 `Channel.BUFFERED`와 `receiveAsFlow()`를 사용한다. 수집자가 잠시 없으면 같은 ViewModel이 살아 있는 동안 effect가 버퍼에 대기한다. effect Flow는 한 이벤트를 한 수집자에게 전달하므로 화면에서 수집자를 하나만 둔다.
- Channel effect는 프로세스 종료 후 복원되지 않는다. 반드시 복원되어야 하는 완료 결과나 사용자에게 남아 있어야 할 오류는 `State`와 저장소에 반영한다. 화면이 표시되는 동안의 일회성 동작에 effect를 쓴다.
- UI에서는 화면 생명주기에 맞춰 effect를 수집한다. effect 처리 후 상태 변경이 필요하면 새 `Intent`를 전달한다.

## Compose 컴포넌트와의 경계

MVI는 화면 단위 계약이다. 재사용 컴포넌트에는 화면 `Intent` 타입이나 ViewModel을 넘기지 않고 필요한 값과 `onClick` 같은 콜백을 전달한다. 화면 콘텐츠가 컴포넌트 이벤트를 화면 Intent에 연결한다. slot 또는 compound component 적용 기준은 [아키텍처 문서](architecture.md)를 따른다.

## 구현 검토 항목

새 화면을 추가하거나 바꿀 때 다음을 확인한다.

1. `State`, `Intent`, `Effect`, 제네릭 ViewModel을 정의하고 `onIntent`에서 입력을 처리하는가?
2. 로딩·빈 내용·오류·재시도와 중복 요청 동작이 필요한 만큼 정의됐는가?
3. 화면 콘텐츠가 ViewModel 없이 렌더링 가능한가?
4. effect가 일회성 동작이며 화면에서 한 번만 수집되는가? 복원이 필요한 결과가 effect에만 있지는 않은가?
5. 비즈니스 규칙이 ViewModel에 과도하게 쌓이거나 저장소 구현이 UI에 새지 않는가?
6. 상태 전이가 복잡하다면 중요한 전이와 실패 경로를 테스트했는가?

## 참고

- [Android 아키텍처 권장사항](https://developer.android.com/topic/architecture/recommendations): ViewModel의 UI 상태, 단방향 데이터 흐름, 수명주기 인식 수집.
- [ViewModel 이벤트 처리](https://developer.android.com/topic/architecture/ui-layer/events): UI 이벤트와 상태 갱신의 관계.
- [Compose 상태 끌어올리기](https://developer.android.com/develop/ui/compose/state-hoisting): 상태 소유 위치.
