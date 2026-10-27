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
| `.md-flow`·제목·문단·인라인·인용·목록 | `MDiveMarkdownFlow/Heading/Text/Quote/ListItem` | 준비된 내용·블록 순서 |
| `.md-code > .md-code-bar + pre` | `MDiveCodeBlock` | 원문·언어 라벨·복사 이벤트 |
| `.md-table-scroll > table.md-table` | `MDiveMarkdownTable` | 열·행·정렬 데이터 |
| `.ds-checkbox.is-readonly` | `MDiveCheckbox(onCheckedChange = null)` | 완료 상태 |
| `.md-image`, `.md-rendered`, `.md-math` | `MDiveMarkdownImage`·`MDiveRenderedBlock` | 준비된 이미지·도식·수식 slot |
| `.md-callout`·`.md-details`·참조 목록·메타데이터 | `MDiveMarkdownCallout/Details/Reference`·`MDiveReferenceList/Metadata` | 제목·내용·시연 펼침 상태 |
| `.ds-state`·`.ds-inline`·`.ds-banner`·`.ds-toast` | `MDiveStatePanel/InlineNotice/RecoveryBanner/Toast` | 표시 상태·문구·호스트 위치 |
| `.ds-banner-content` + `.ds-banner-action` | `MDiveRecoveryBanner(action)` | 한 행동 영역 안에서 버튼 두 개를 세로로 구성 |
| `.ds-sheet[data-preview]` + handle/heading/body | `MDiveBottomSheet` | 프레임 안 위치·본문 구성 |
| `.ds-dialog[data-preview]` + body/actions | `MDiveConfirmationDialog` | 확인 문구·행동·프레임 안 위치 |
| `.ds-field`, `.ds-slider` | `MDiveTextField`·`MDiveSlider` | 시연 값·입력 이벤트 |

`data-preview`는 검토 보드 안에 같은 공통 표면을 보여주는 HTML 호스트 표식이다. 실제 `<dialog>`를 닫힌 상태에서 강제로 보이게 하지 않는다. 시연 보드 자체의 모달 포커스·Android 드래그 검증을 완료한 것으로 간주하지 않는다.

## 화면 전용으로 유지하는 부분

- 기기 프레임·시스템 바·홈 인사말·본문 여백·읽기 도구의 조합과 오버레이 좌표.
- 첫 실행·로딩·빈 상태의 중앙 배치, 목차·책갈피 탭 아래의 가용 높이.
- 목차의 깊이와 책갈피 이름·본문 미리보기·편집 행동의 행 구성. 공통 외형이 필요해지면 별도 DS 검토를 거친다.
- 렌더 엔진 대신 제공하는 산·바다 SVG와 도식 내부 노드 같은 예시 그림. 바깥 콘텐츠 셸과 행동은 DS를 사용한다.
- 디자인 보드 탐색·인쇄·폰트 배율 컨트롤과 인접 명세.

화면 CSS에서 공통 컴포넌트 선택자를 사용할 때는 위치·크기·스크롤·여백 같은 배치만 허용한다. 공통 색·글꼴·내부 패딩·경계·모서리·누름 외형은 DS에서 수정한다. 사용 계약 회귀 검사는 HTML 렌더·접근성 검증을 대체하지 않는다.
