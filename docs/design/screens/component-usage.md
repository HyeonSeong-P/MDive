# 제품 시각 초안의 DS 사용 기준

제품 정책·합의 여부는 `docs/product/`가 소유한다. 이 문서는 HTML 시각 조합과 기존 Compose API의 대응을 관리하며 새 제품 행동을 확정하지 않는다.

## 공통 원본

- [components.css](../../../design-system/components.css): 카탈로그·제품이 직접 참조하는 공통 컴포넌트 표현.
- [feedback.css](../../../design-system/feedback.css): 안내·배너·상태·시트·다이얼로그·입력.
- [components.js](../../../design-system/components.js): 배너의 폭·글자 배율에 따른 배치. 화면의 `data-go`·파일 선택·저장 등은 처리하지 않는다.
- [tokens.json](../../../design-system/tokens.json), [catalog.json](../../../design-system/catalog.json), [icons.json](../../../design-system/icons.json): 값·예시 상태·아이콘 경로 원본. 생성물은 생성기로만 갱신한다.

`catalog.css`는 카탈로그 페이지 설명·목록·검토 컨트롤용이다. 제품 화면은 이 파일을 참조하지 않는다. HTML과 Compose는 같은 값·역할·상태를 공유하지만 각각의 플랫폼 표현을 작성하며 픽셀 일치를 보장하지 않는다.

## 공통 컴포넌트 대응

| 제품 HTML 계약 | 기존 Compose API | 화면에서 소유하는 부분 |
| --- | --- | --- |
| `.topbar`·`.brand-mark` | `MDiveTopBar`·`MDiveBrandMark` | 제목·앞뒤 슬롯의 조합 |
| `.button` + variant + `size-s/m/l`, `.icon-button` | `MDiveButton`·`MDiveIconButton` | 라벨·행동·크기·우선순위 선택 |
| `.document-row > div` | `MDiveDocumentRow` | 문서 데이터·즐겨찾기 여부 |
| `.search`, `.tabs > button[role=tab]` | `MDiveSearchBar`·`MDiveTextTab` | 시연 입력·검색 건수·선택 상태 |
| `.md-flow`·제목·문단·인라인·인용·목록 | `MDiveMarkdownFlow/Section/Heading/Text/Quote/ListItem` | 준비된 내용·블록 순서 |
| `.md-code > .md-code-bar + pre` | `MDiveCodeBlock` | 원문·언어 라벨·복사 이벤트 |
| `.md-table-scroll > table.md-table` | `MDiveMarkdownTable` | 열·행·정렬 데이터 |
| `.ds-checkbox.is-readonly` | `MDiveCheckbox(onCheckedChange = null)` | 완료 상태 |
| `.md-image`, `.md-rendered`, `.md-math` | `MDiveMarkdownImage`·`MDiveRenderedBlock` | 준비된 이미지·도식·수식 slot |
| `.md-callout`·`.md-details`·참조 목록·메타데이터 | `MDiveMarkdownCallout/Details/Reference`·`MDiveReferenceList/Metadata` | 제목·내용·시연 펼침 상태 |
| `.ds-skeleton-line` | `MDiveSkeleton` | 목록/본문별 비상호작용 자리표시자 폭·높이·순서 |
| `.ds-welcome-illustration` | 승인 원본의 문서 퐁당 일러스트 | primary 색의 단색 장식. HTML SVG mask·Compose 벡터가 공통 원본을 사용 |
| `.ds-state`·`.ds-inline`·`.ds-banner`·`.ds-toast` | `MDiveStatePanel/InlineNotice/RecoveryBanner/Toast` | 표시 상태·문구·호스트 위치 |
| `.ds-banner-content` + `.ds-banner-action` | `MDiveRecoveryBanner(action)` | 한 행동 영역 안에서 버튼 두 개를 세로로 구성 |
| `.ds-sheet[data-preview]` + handle/heading/body | `MDiveBottomSheet` | 프레임 안 위치·본문 구성 |
| `.ds-dialog[data-preview]` + body/actions | `MDiveConfirmationDialog` | 확인 문구·행동·프레임 안 위치 |
| `.ds-field`, `.ds-slider` | `MDiveTextField`·`MDiveSlider` | 시연 값·입력 이벤트 |

`data-preview`는 검토 보드 안에 같은 공통 표면을 보여주는 HTML 호스트 표식이다. 실제 `<dialog>`를 닫힌 상태에서 강제로 보이게 하지 않는다. 시연 보드 자체의 모달 포커스·Android 드래그 검증을 완료한 것으로 간주하지 않는다.

## 화면 전용으로 유지하는 부분

- 기기 프레임·시스템 바·홈 인사말·본문 여백·읽기 도구의 조합과 오버레이 좌표.
- 첫 실행·빈 상태의 중앙 배치, 전체 로딩의 화면별 목록/본문 골격과 목차·책갈피 탭 아래의 가용 높이.
- 목차의 깊이와 책갈피 이름·본문 미리보기·편집 행동의 행 구성. 공통 외형이 필요해지면 별도 DS 검토를 거친다.
- 렌더 엔진 대신 제공하는 산·바다 SVG와 도식 내부 노드 같은 예시 그림. 바깥 콘텐츠 셸과 행동은 DS를 사용한다.
- 디자인 보드 탐색·인쇄·폰트 배율 컨트롤과 인접 명세.

화면 CSS에서 공통 컴포넌트 선택자를 사용할 때는 위치·크기·스크롤·여백 같은 배치만 허용한다. 공통 색·글꼴·내부 패딩·경계·모서리·누름 외형은 DS에서 수정한다. 사용 계약 회귀 검사는 HTML 렌더·접근성 검증을 대체하지 않는다.

## 브랜드와 화면 기본 여백

홈 상단바는 `MDiveBrandMark`만 표시하며 앱명 글자를 반복하지 않는다. HTML 브랜드 표시는 `role="img"`와 `aria-label="MDive"`로 접근성 이름을 유지한다. 읽기의 문서 제목·뒤로·도구는 보존한다.

제품 본문은 기본 사방 `spacing.page`(16dp), HTML은 `--space-page`를 사용한다. 상태 화면의 중앙 배치는 유지한다. 상단바·터치 영역·시스템 인셋·컴포넌트 내부·목차 깊이·전체 폭 시트 같은 의미 있는 예외와 보드 바깥 검토 UI는 별도 기준이다.

## 전체 로딩과 상태 장식

홈 목록 준비와 읽기 전체 로딩은 공통 `MDiveSkeleton` / `.ds-skeleton-line`을 화면별 목록·본문 구조로 조합한다. status 호스트가 로딩 이름을 한 번 전달하고 막대는 접근성에서 숨긴다. 막대는 선택 불가이며 문서 데이터가 아니다. 첫 실행 검토안에서는 승인된 문서 퐁당 일러스트 / `.ds-welcome-illustration`을 쓰며, 빈/실패 상태는 아이콘 타일 없이 문구·행동을 표시한다. 부분 리소스의 진행 표시·복구 배너는 유지한다.

전체 로딩의 스켈레톤 방향과 문서 퐁당 일러스트 자체는 사용자 확정이다. 막대의 구체 배치·첫 실행 전체 구성·빈/실패 표현은 DS·디자인 보드 검토안이며 제품 앱에는 이번 개편을 적용하지 않는다.

## 리뷰 반영 배치 · 앱 적용 대기

- 문서가 있는 홈은 우하단 파일 열기 FAB를 사용한다. 첫 실행 L 파일 열기와 빈 상태 M 단일 복구는 내용 폭 버튼을 사용한다. `.ds-state-actions.is-compact`는 DS 계약이다. 일반 전체 실패는 재시도와 quiet 홈 복귀만 제공한다. 파일 다시 연결은 홈 기존 문서 복구 맥락이다.
- 읽기 단일 상단 행은 뒤로/제목/즐겨찾기/검색/더보기다. 즐겨찾기는 독립 토글이며 `reader-more`의 버튼 앵커 팝업 메뉴에서 목차·책갈피/위치 책갈피 추가/읽기 설정을 연결한다. 자동 숨김은 도입하지 않는다.
- `.ds-render-status`는 정상 중립 표시다. 지속 복구 배너는 `.inline-notice` 화면 호스트로 문서 흐름 안에 놓으며 일시 완료 토스트만 overlay로 둔다.
- 시트의 `.ds-state.is-flush`는 부모 본문 16dp와 중복되지 않게 한다. 검색 한 줄은 공통 DS의 48dp 터치 기준과 압축 입력 내부 여백을 사용한다.

이 조합은 디자인 검토안이며 제품 앱 적용은 후속 요청을 기다린다.

제목 리듬: HTML `.md-flow` 직접 제목은 공통 CSS가 앞 32·뒤 8(첫 제목 앞 0)을 적용한다. Compose `MDiveMarkdownFlow`는 기존 ColumnScope/일반 블록 16dp 계약을 유지하고 자동 제목 재배치를 하지 않는다. `MDiveMarkdownSection`의 heading/content slot과 `spaceBefore=16dp`를 명시 조합해 바깥 Flow 16dp와 합쳐 제목 앞 32dp, 제목 뒤 8dp, 본문 사이 16dp를 만든다. 첫 section은 `spaceBefore=0`이다.

## 문서 목록 홈의 파일 열기 FAB

`home`, `home-no-favorites`, `access-error`, `remove`, `home-partial`의 파일 열기는 공통 `.button.size-l.floating-action-button` + `file-add` + `.floating-action-label`을 사용한다. 화면 전용 `.home-file-fab`는 우하단 16dp, `.home-with-file-fab`는 글자 배율에 따른 버튼 높이와 가장자리 간격만큼 목록 끝 공간을 예약한다. 다이얼로그 배경 FAB는 `inert`·`aria-hidden`을 제공한다. `home-empty`는 중앙 CTA만 유지하며 로딩/전체 실패에 새 버튼을 추가하지 않는다.

팝업 메뉴는 `.ds-popup-menu`·`.ds-popup-menu-item` DS를 사용하며 `.reader-popup-position`은 상단바 아래/오른쪽 좌표만 소유한다. `more`는 `.icon.is-filled`로 채우고, 별표는 `aria-pressed` 및 추가/해제 이름을 시연한다. 하단시트·중복 즐겨찾기 항목은 더보기에서 제거한다.

화면 명세의 iframe embed는 이 보드의 원본 제품 마크업·공통 CSS를 재사용한다. 명세의 요소별 동작/상태 설명과 디자인의 시각 조합은 별도 원본이며 `embed=<board-id>`는 검토 chrome만 제거한다.
