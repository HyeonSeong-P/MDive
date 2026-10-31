# MDive 내비게이션

상태: S10 기본 링크·상대 문서 이동을 연결했다. 아래 S10 경계가 상대 문서 이동과 방문별 복귀를 정의하며 실제 검증 결과는 구현 계획에 기록한다.

## UI 소유 계약

- Jetpack Navigation 3의 `NavDisplay`와 `rememberNavBackStack`을 사용한다. `ui/navigation/Destination`의 `Home`·`Reader`는 `@Serializable NavKey`이며 domain은 목적지나 Navigation 타입을 모른다.
- 제품 최초 백스택은 `[Home]`이다. 홈은 시스템 선택기의 실제 선택 결과를 받았을 때만 문서 참조 문자열을 가진 Reader key를 추가한다. 선택 취소는 홈을 유지하며 예시 파일을 제품에서 열지 않는다.
- 백스택은 UI가 소유한다. 읽기 상단 뒤로와 `ReaderRoute`의 시스템 `BackHandler`는 `ReaderIntent.Back`으로 읽기를 취소하고 `ReaderEffect.NavigateHome` → UI pop으로 전달한다. `NavDisplay.onBack`은 Reader 처리 바깥의 백스택 pop fallback이다. 홈 root에서는 Activity의 기본 시스템 뒤로 동작을 유지한다.
- 화면 콘텐츠는 콜백만 받으며 ViewModel은 백스택을 조작하지 않는다. Reader의 재시도는 현재 문서 참조를 저장소에서 다시 읽는다. 일반 실패에서 재선택을 제공하지 않는다.
- saveable-state decorator 다음 ViewModelStore decorator를 적용하고 entry 내부에서 `hiltViewModel()`을 호출한다. 각 entry의 ViewModel 수명과 저장 가능 UI 상태를 관리한다. 직렬화 가능한 key로 UI 백스택을 복원하지만 문서·권한·읽던 위치 저장을 구현한 것은 아니다.
- 상대 문서 이동 후 복귀는 아래 S10 경계를 따른다. 외부 파일/공유/딥링크 진입·동일 문서의 영속 식별은 후속 단계에서 필요한 정책을 정한다.

참고: [Android Navigation 3 문서](https://developer.android.com/guide/navigation/navigation-3), [저장 가능한 백스택과 entry 상태](https://developer.android.com/guide/navigation/navigation-3/save-state)

S02 당시 Reader key는 `Reader(reference: String)`이었으며 S10은 아래 방문 식별자를 추가한다. 원문을 백스택에 직렬화하지 않는다. 회전에서는 entry의 ViewModel을 유지하고, 재생성된 entry는 같은 참조로 재읽는다. 권한 소실은 일반 읽기 실패로 처리하며 영속 접근을 보장하지 않는다. 홈 선택 중 상태는 SavedStateHandle로 관리하고 Activity Result launcher는 조건 없이 등록한다.


S07 Mermaid 확대는 Reader 내부의 전체 크기 Dialog 오버레이이며 새 Navigation 목적지로 이동하지 않는다. ReaderState의 원문 시트·확대 모델은 상호 배타적이고, 닫기·시스템 뒤로는 해당 오버레이만 제거한다. 같은 Reader와 LazyColumn을 유지해 본문 위치에 복귀하며 문서를 재읽거나 원문을 NavKey에 넣지 않는다. 이미지 확대는 아래 S08+S09, 상대 문서 이동은 S10 경계를 따른다.


S08+S09 이미지 확대도 같은 Reader 내부 전체 크기 Dialog이며 별도 Navigation 목적지를 만들지 않는다. Reader State/Intent가 확대 진입·닫기를 소유하고 수식/다이어그램 원문 시트·Mermaid 확대와 동시에 열지 않는다. 닫기·시스템 뒤로는 오버레이만 닫아 기존 본문 위치를 유지한다. SAF 폴더 선택은 Reader Effect에서 시스템 launcher로 연결하며 취소는 기존 연결과 Reader를 유지한다. 문서별 이미지 폴더 연결의 영속 저장은 Reader key나 홈 최근 목록 저장을 대체하지 않는다.


## S10 링크·상대 문서 이동 경계

제품의 scheme·경로·fragment·권한·복귀 정책은 [S10 링크 프로필](../product/document-viewing.md#s10-기본-링크-프로필--확정)이 소유한다. UI 소유 `Destination.Reader`는 불투명 문서 참조와 방문 식별자 `visit`를 가진다. 같은 문서를 반복 방문해도 각 entry의 ViewModel과 저장 가능한 본문 위치를 구분한다. 새 상대 문서는 위에서 시작하며 상단·시스템 뒤로는 이전 entry를 pop해 해당 본문 위치로 복귀한다. 홈에서 직접 연 첫 Reader의 뒤로는 홈이다.

Reader ViewModel은 domain 링크 저장소로 목적지를 해석한 뒤 대상 문서를 읽고 파싱한다. 실패하면 현재 문서를 유지하고, 성공하면 앱 소유 준비 결과 `PreparedDocument`와 문서 참조를 `ReaderEffect.NavigateDocument`로 전달한다. UI 호스트가 현재 entry에서 발생한 이동인지 확인하고 새 방문 key를 추가한다. 준비 결과는 UI 세션 메모리에만 두며 NavKey에 원문·AST를 직렬화하지 않는다. 세션 준비 결과가 없는 재생성은 참조로 다시 읽으며 홈 이력·영속 읽던 위치 저장을 선행 구현하지 않는다.

이동 준비 중 뒤로는 요청을 취소하며 현재 entry를 유지한다. 중복 탭·취소 후 늦은 결과가 새 entry나 본문을 덮지 않도록 Reader가 진행 상태와 요청 취소를 소유한다. 폴더 선택은 별도 Reader Effect와 Activity Result launcher로 연결하며 이미지 폴더 선택과 동시에 시작하지 않는다. 웹 열기는 Route의 브라우저 플랫폼 어댑터가 처리하고 실행 실패를 Intent로 반환한다. 브라우저 왕복도 기존 Reader entry와 본문 위치를 유지한다.
