# 구현 세션 지시문

사용자는 한 세션에 한 작업만 지시한다. ID와 범위는 [구현 계획](open-markdown.md)이 소유하며, 이 문서는 다음 채팅에 붙여 넣을 실행 지시문이다. 이 PR을 병합한 후 사용한다. 아직 병합 전이면 먼저 사용할 기준 브랜치를 명시하며 에이전트가 PR을 자동 병합하지 않게 한다.

<a id="session-s01"></a>

## 첫 구현 세션 · S01

```text
AGENTS.md와 docs/implementation/open-markdown.md를 읽고 S01만 구현해줘.
이 디자인/계획 PR이 병합된 최신 develop에서 저장소 Git 규칙에 맞는 작업 브랜치를 사용해.
PR이 아직 병합되지 않았다면 임의로 병합하거나 기존 변경을 덮지 말고 기준 브랜치를 확인해.

docs/product/overview.md, docs/product/home.md, docs/product/document-viewing.md와
관련 docs/engineering/{architecture,mvi,navigation}.md를 확인하고,
docs/design/screens/README.md·component-usage.md 및 실제 designsystem Compose API를 기준으로 진행해.

범위는 제품 홈·읽기 호스트, 화면별 State/Intent/Effect/BaseViewModel,
UI 소유 Navigation 3 백스택과 필요한 Hilt 설정,
DS·lifecycle Compose의 제품 main/release 연결이야.
기존 DS 컴포넌트를 사용하고 화면에서 공통 외형을 새로 만들지 마.
두 모듈과 minSdk 24를 유지하고, 아직 필요 없는 저장소·유즈케이스·모듈은 추가하지 마.

실제 파일 선택·파일 로더·파서·수식/Mermaid 엔진·영속 저장·읽기 도구는 이번 범위가 아니야.
준비된 데이터는 Preview/테스트 호스트에서만 써서 상태·이동을 검증하고,
제품에서 가짜 파일을 실제로 연 것처럼 보이게 만들지 마.
이번 중간 단계가 파일 선택부터 읽기까지 완성한 것으로 표시되면 안 돼.

필요한 Android API 조사는 대응 서브에이전트에 맡기고,
구현과 관련 테스트, debug/release 빌드, 가능한 에뮬레이터 확인,
change_reviewer 독립 검토와 발견 사항 수정까지 마쳐줘.
실행하지 못한 검증은 원인과 남은 확인을 구분해서 적어줘.
마지막으로 구현 계획의 S01 상태·검증 결과·다음 시작점을 갱신하고 멈춰.
S02는 자동 시작하지 말고, 커밋·푸시·PR은 별도 요청 전에는 하지 마.
```

## 후속 세션 템플릿

```text
AGENTS.md와 docs/implementation/open-markdown.md를 읽고 [요청할 ID]만 진행해줘.
시작 전에 현재 브랜치·로컬 변경·선행 단계의 완료/검증 상태를 확인하고,
해당 ID의 범위·결정 게이트·완료 기준을 짧게 안내해.

관련 제품 명세와 DS 가이드·컴포넌트 사용 기준·실제 Compose API를 우선해.
미정인 제품 정책은 해당 단계에 필요한 것만 질문하고 임의로 확정하지 마.
아직 구현하지 않은 문법·리소스를 조용히 지우거나 지원 완료로 표시하지 마.
공통 UI는 DS를 재사용하고, 부족하면 DS 원본과 양쪽 카탈로그를 함께 보완해.

해당 ID의 구현·필요한 검증·독립 검토와 수정·계획 현황 갱신까지 수행해.
끝에 완료/미완료·실제 검증/미검증·열린 결정·다음 ID를 남기고 멈춰.
다음 단계나 커밋·푸시·PR은 별도 요청 없이 시작하지 마.
```

예: S01 완료 후 다음 세션에서는 `[요청할 ID]`를 `S02`로 바꾼다. 선행 단계가 미완료면 같은 ID의 남은 범위를 지시한다. 요청한 단계가 커지면 계획에 하위 ID와 남은 범위를 기록하고 이어서 지시한다.
