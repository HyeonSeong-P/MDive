# MDive 내비게이션

상태: S01 홈·읽기 호스트 연결. 실제 파일 선택과 문서 입력은 S02에서 연결한다.

## UI 소유 계약

- Jetpack Navigation 3의 `NavDisplay`와 `rememberNavBackStack`을 사용한다. `ui/navigation/Destination`의 `Home`·`Reader`는 `@Serializable NavKey`이며 domain은 목적지나 Navigation 타입을 모른다.
- 제품 최초 백스택은 `[Home]`이다. 홈은 실제 파일 선택 전까지 Reader를 추가하지 않는다. Reader 초기 진입은 Preview/테스트에서만 검증하며 예시 파일을 제품에서 열지 않는다.
- 백스택은 UI가 소유한다. 상단 뒤로는 `ReaderIntent.Back` → `ReaderEffect.NavigateHome` → UI pop으로 전달하며 시스템 뒤로는 `NavDisplay.onBack`에서 같은 UI pop을 수행한다. 홈 root에서는 Activity의 기본 시스템 뒤로 동작을 유지한다.
- 화면 콘텐츠는 콜백만 받으며 ViewModel은 백스택을 조작하지 않는다. Reader의 재시도·재선택 요청은 Effect를 수집한 호스트 콜백으로 전달하되 S01 제품에서는 실제 로더/선택기를 연결하지 않는다.
- saveable-state decorator 다음 ViewModelStore decorator를 적용하고 entry 내부에서 `hiltViewModel()`을 호출한다. 각 entry의 ViewModel 수명과 저장 가능 UI 상태를 관리한다. 직렬화 가능한 key로 UI 백스택을 복원하지만 문서·권한·읽던 위치 저장을 구현한 것은 아니다.
- 상대 문서 이동 후 복귀·외부 파일/공유/딥링크 진입·문서 식별 입력은 후속 단계에서 필요한 정책을 정한다.

참고: [Android Navigation 3 문서](https://developer.android.com/guide/navigation/navigation-3), [저장 가능한 백스택과 entry 상태](https://developer.android.com/guide/navigation/navigation-3/save-state)
