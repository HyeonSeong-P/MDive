# MDive 내비게이션

상태: S02 시스템 파일 선택·문서 입력 연결. Markdown 렌더링은 후속 단계다.

## UI 소유 계약

- Jetpack Navigation 3의 `NavDisplay`와 `rememberNavBackStack`을 사용한다. `ui/navigation/Destination`의 `Home`·`Reader`는 `@Serializable NavKey`이며 domain은 목적지나 Navigation 타입을 모른다.
- 제품 최초 백스택은 `[Home]`이다. 홈은 시스템 선택기의 실제 선택 결과를 받았을 때만 문서 참조 문자열을 가진 Reader key를 추가한다. 선택 취소는 홈을 유지하며 예시 파일을 제품에서 열지 않는다.
- 백스택은 UI가 소유한다. 읽기 상단 뒤로와 `ReaderRoute`의 시스템 `BackHandler`는 `ReaderIntent.Back`으로 읽기를 취소하고 `ReaderEffect.NavigateHome` → UI pop으로 전달한다. `NavDisplay.onBack`은 Reader 처리 바깥의 백스택 pop fallback이다. 홈 root에서는 Activity의 기본 시스템 뒤로 동작을 유지한다.
- 화면 콘텐츠는 콜백만 받으며 ViewModel은 백스택을 조작하지 않는다. Reader의 재시도는 현재 문서 참조를 저장소에서 다시 읽는다. 일반 실패에서 재선택을 제공하지 않는다.
- saveable-state decorator 다음 ViewModelStore decorator를 적용하고 entry 내부에서 `hiltViewModel()`을 호출한다. 각 entry의 ViewModel 수명과 저장 가능 UI 상태를 관리한다. 직렬화 가능한 key로 UI 백스택을 복원하지만 문서·권한·읽던 위치 저장을 구현한 것은 아니다.
- 상대 문서 이동 후 복귀·외부 파일/공유/딥링크 진입·동일 문서의 영속 식별은 후속 단계에서 필요한 정책을 정한다.

참고: [Android Navigation 3 문서](https://developer.android.com/guide/navigation/navigation-3), [저장 가능한 백스택과 entry 상태](https://developer.android.com/guide/navigation/navigation-3/save-state)

S02 Reader key는 `Reader(reference: String)`이며 원문을 백스택에 직렬화하지 않는다. 회전에서는 entry의 ViewModel을 유지하고, 재생성된 entry는 같은 참조로 재읽는다. 권한 소실은 일반 읽기 실패로 처리하며 영속 접근을 보장하지 않는다. 홈 선택 중 상태는 SavedStateHandle로 관리하고 Activity Result launcher는 조건 없이 등록한다.
