# MDive 내비게이션

상태: 기술 선택만 확정. 화면과 사용자 흐름이 정해지면 이 문서를 구체화한다.

## 확정 사항

- 여러 화면의 이동에는 Jetpack Navigation 3를 사용한다.
- 내비게이션은 UI 계층의 책임이다. `domain`은 Navigation 3 타입이나 화면 목적지를 알지 못한다.
- 화면 콘텐츠는 이동 의도를 콜백으로 외부에 전달한다. ViewModel에서 이동을 요청할 때는 화면 `Effect`를 방출하고 UI가 이를 수집해 처리한다. ViewModel과 재사용 컴포넌트는 백스택을 직접 조작하지 않는다.

## 화면 설계 시 결정할 사항

- 목적지 목록과 각 목적지가 받는 값
- 백스택과 뒤로 가기 동작, 상태 복원
- 외부 파일 열기·공유·딥링크가 필요한지와 진입 방식
- 화면별 ViewModel 범위와 Hilt 연결 방식

첫 다중 화면 기능을 구현할 때 Navigation 3 의존성을 추가하고 위 항목을 실제 사용자 시나리오에 맞춰 확정한다.

참고: [Android Navigation 3 문서](https://developer.android.com/guide/navigation/navigation-3)
