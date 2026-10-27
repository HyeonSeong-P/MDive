# Markdown 콘텐츠 디자인 시스템 확장 목록

상태: **DS 확장 계획·문법 범위 갱신 / 2026-10-27**. M-01~10의 시각 컴포넌트와 HTML/Compose 카탈로그는 구현됐으며 파서·제품 화면 통합은 별도다. 아래 제안·검증 목록 전체의 완료나 시각 승인을 뜻하지 않는다. 확장 문법의 기능 채택은 제품 명세를 따르고 정확한 호환 범위는 미정으로 유지한다.

기능 원본은 [제품 방향](../product/overview.md), [문서 표시와 읽기 설정](../product/document-viewing.md), [읽기 도구](../product/reading-tools.md)다. 기능 채택 변경은 해당 제품 명세에 먼저 반영한다. [제품 화면 디자인](screens/index.html#reader)은 조합 예시이며, 이 문서의 컴포넌트나 문법 호환 범위가 승인되었다는 뜻은 아니다.

## 분석 범위와 근거

Markdown에는 모든 구현이 공유하는 단일 확장 목록이 없다. 이번 목록은 **CommonMark 핵심 문법 전체 + GFM 명세 확장 전체 + MDive가 합의한 수식·Mermaid + 기타 방언의 별도 후보**로 경계를 정한다. 문법 조사 범위를 제품 지원 약속으로 전환하지 않는다.

- [CommonMark 0.31.2](https://spec.commonmark.org/0.31.2/): 핵심 구문 분류와 구문별 공식 예제를 확인했다. 파싱 우선순위·모호성은 파서 검증 영역이다.
- [GFM 0.29-gfm](https://github.github.com/gfm/): 표, task list, 취소선, 확장 자동 링크와 disallowed raw HTML을 별도 확장으로 확인했다. GitHub 제품의 모든 기능을 GFM 명세에 포함된 것으로 보지 않는다.
- [GitHub 수식 문서](https://docs.github.com/en/get-started/writing-on-github/working-with-advanced-formatting/writing-mathematical-expressions): 인라인·블록 수식 표기 사례의 조사 근거다. MDive의 구분자·매크로·엔진 선택은 미정이다.
- [Mermaid 공식 문서](https://mermaid.js.org/intro/): 여러 다이어그램 문법이 존재함을 확인했다. MDive의 지원 종류·버전·엔진은 미정이다.

표의 문법명은 조사용 분류이고 이후의 상태·API·접근성·검증은 MDive를 위한 설계 제안이다. CommonMark와 GFM의 버전은 서로 다르므로 하나의 혼합된 호환성 기준으로 취급하지 않는다.

## 최초 분석 시점의 자산과 부족한 부분

아래 표는 M-01~10 확장 전의 분석 기록이다. 현재는 데이터 기반 콘텐츠 API와 공통 체크박스, 복구 배너를 사용하는 HTML/Compose 카탈로그가 추가됐으며, 세부 구현은 실제 코드가 기준이다.

| 자산 | 실제 상태 | 후속 작업 |
| --- | --- | --- |
| `MDiveDesignTheme`, 생성 `DesignTokens` | 의미 색상, 9개 타이포 역할, 간격·모서리·크기 존재 | 문서 역할의 부족한 값만 공통 원본에 추가 |
| `MDiveCodeSample` | `CatalogSamples.code` 고정 예시, 복사 콜백은 빈 동작, 단순 강조와 가로 스크롤 | 데이터·강조 범위·복사 이벤트를 받는 실제 블록 필요 |
| `MDiveTableSample` | `CatalogSamples.table` 고정 예시, 셀 너비 고정 | 임의 행·열·정렬·인라인 내용의 실제 표 필요 |
| `MDiveDivider` | 공용 구분선 | 문서 thematic break의 문맥 간격을 조합 |
| `MDiveInlineNotice`, `MDiveStatePanel` | 안내와 로딩·빈/실패 상태 | 문서 부분 실패의 공통 셸에 조합 |
| `MDiveButton`, `MDiveIconButton`, `MDiveToast`, `MDiveRecoveryBanner` | 공용 행동·피드백 | 복사·연결·재시도 이벤트와 호스트에서 연결 |
| 제목·본문·인라인·목록·인용·이미지·수식·Mermaid | 전용 데이터 기반 콘텐츠 API 없음 | 아래 목록의 핵심 확장 |

근거: [Components.kt](../../designsystem/src/main/java/com/parka/mdive/designsystem/Components.kt), [Controls.kt](../../designsystem/src/main/java/com/parka/mdive/designsystem/Controls.kt), [Feedback.kt](../../designsystem/src/main/java/com/parka/mdive/designsystem/Feedback.kt), [MDiveTheme.kt](../../designsystem/src/main/java/com/parka/mdive/designsystem/MDiveTheme.kt), [토큰 원본](../../design-system/tokens.json). 샘플을 이름만 바꾸어 제품 렌더러로 연결하지 않는다.

## 문법 → 표현 목록

`기본`은 기본 Markdown 검토 대상이고 정확한 호환성은 미정이다. `합의 확장`은 기능 제공 자체가 합의된 항목이다. `미정`은 조사했으나 제품 채택을 정하지 않은 항목이다.

| 문법/입력 분류 | 제품 경계 | 필요한 표현·대응 ID |
| --- | --- | --- |
| ATX H1–H6, Setext 제목 | 기본 | 같은 의미 단계는 같은 제목 표현, H1–H6 위계 `M-01` |
| 일반 문단·일반 텍스트 | 기본 | 긴 한글/영문 혼합 문단 `M-02` |
| soft break, hard break | 기본 | 문단 안 줄바꿈 규칙 `M-02`; 별도 컴포넌트 불필요 |
| emphasis, strong 및 중첩 | 기본 | 텍스트 run 스타일 조합 `M-02` |
| code span | 기본 | 본문 안 고정폭·배경·기준선 `M-02` |
| strikethrough | 합의 확장 | 인라인 장식 run `M-02` |
| thematic break | 기본 | 문서 흐름 구분 `M-03` |
| block quote·중첩 인용 | 기본 | 장식선과 중첩 블록 `M-04` |
| unordered list | 기본 | marker·내용의 내어쓰기 `M-05` |
| ordered list, 시작 번호 | 기본 | 번호 너비와 정렬 `M-05` |
| list item, tight/loose, 혼합·중첩 목록 | 기본 | 자식 문단·블록 간격과 깊이 `M-05` |
| task list checked/unchecked | 합의 확장 | 읽기 전용 상태 marker `M-05` |
| indented/fenced code, info string | 기본 | 동일 코드 블록, 언어 라벨 선택 `M-06` |
| inline/reference link, autolink | 기본 | 동일 링크 span `M-02`, 이벤트 `M-11` |
| GFM 확장 autolink | 채택·인식 범위 미정 | 인식 후 같은 링크 span; 별도 외형 없음 |
| inline/reference image, alt/title | 기본·이미지 기능 합의 | 이미지 표시·부분 상태 `M-08` |
| GFM table, header/body, column alignment | 합의 확장 | 가로 스크롤 표 `M-07` |
| backslash escape, entity/numeric reference | 기본 | 해석된 텍스트 `M-02`; 새 시각 컴포넌트 없음 |
| tabs/공백/빈 줄, 구문 우선순위 | 기본 | 블록 구조·공백 해석; 새 UI 없음 |
| link reference definition | 기본 | 참조 해석 입력; 정의 자체의 UI 없음 |
| HTML block, raw inline HTML | 채택·허용 정책 미정 | 허용/비허용 정책부터 결정; 필요시 `M-10` fallback |
| GFM disallowed raw HTML | 미정 | HTML 처리 정책과 함께 검토; 새 문서 UI 아님 |
| inline/display math | 합의 확장 | 수식 본문·블록 `M-09` |
| Mermaid 코드 fence | 합의 확장 | 다이어그램 `M-09`; 일반 코드와 분기 규칙 미정 |

### 추가 채택한 확장 — 시각 구현과 미정 방언

2026-10-27 사용자 합의로 각주·각주 목록, 정의 목록, highlight, subscript/superscript, emoji shortcode, YAML/TOML front matter, heading ID/attribute list, wiki link, 일반 callout/admonition·GitHub alerts, collapsible details, embedded HTML/media, 문법 기반 자동 목차, citation/bibliography, 주석, 삽입/밑줄, typography 치환, 확장 자동 링크 및 GitHub mention·issue·commit 참조를 **기능으로 채택**했다. 상세 범위의 원본은 [문서 표시 명세](../product/document-viewing.md#채택한-확장-문법)다.

GitHub alerts와 mention·issue·commit 참조도 이번 채택 범위다. GitHub 제품 전체 기능 지원을 뜻하지 않으며, 서비스 참조의 owner/repository 문맥·해석과 인증·네트워크·실제 연동은 별도 결정이다. 기존 GFM 표·task·취소선은 유지한다. HTML·미디어 채택은 임의 실행·접근 허용을 뜻하지 않으며, 방언·호환 버전·구분자·허용 정책은 구현 전에 정한다.

추가 채택 기능의 시각 단위는 `MarkdownExtensions.kt`와 `MarkdownContent.kt`에 데이터·slot·콜백 기반으로 추가했다. `markdown-extended`의 HTML/Compose 카탈로그에서 같은 공유 샘플과 상태를 비교한다. 이는 문법 파싱·호환성 또는 제품 이동 정책의 구현 완료를 뜻하지 않는다.

| 추가 시각 단위 | 구현 API / 카탈로그 상태 |
| --- | --- |
| highlight·sub/sup·insert·underline | `MDiveTextRun.mark/baseline/inserted/underline` / `inline`; 기존 `highlighted`는 검색 장식 입력으로 구별 |
| 각주 참조·목록·되돌아가기, citation/bibliography | `MDiveDocumentReference`, `MDiveMarkdownReference`, `MDiveReferenceList` / `references`; 해석된 대상과 역참조를 호스트가 제공 |
| 정의 목록 | `MDiveDefinitionList` / `definitions` |
| 일반 callout·GitHub alerts | `MDiveMarkdownCallout`, `MDiveCalloutKind` / `callouts`; 제목·아이콘·공통 색상 토큰과 자식 slot |
| 접고 펼침 | `MDiveMarkdownDetails` / `details`; controlled 상태·summary slot·변경 콜백, 숨긴 자식은 구성하지 않음 |
| front matter 표시용 메타데이터 | `MDiveMarkdownMetadata` / `metadata`; 필드 선택·YAML/TOML 해석은 호스트 이전 단계 |
| 문법 기반 목차 | `MDiveMarkdownToc` / `toc`; 제목·단계·해석된 대상과 이동 이벤트 |
| HTML·미디어 | `MDiveEmbeddedContent` / `embedded`; 준비된 비실행 content slot과 정상·로딩·실패, 복구 배너 내부 원문·확대 행동 |
| heading ID/attributes·wiki·확장 자동 링크·GitHub 참조 | 기존 heading/text/link API 조합 / `resolved`; ID·속성 허용·목적지 해석은 처리하지 않음 |
| emoji·typography 치환·주석 | 해석된 문자열 예시 / `resolved`; 치환과 주석 비노출은 비시각 단계이며 파서를 추가하지 않음 |

모든 추가 표현의 수치·색상은 카탈로그 검토 초안이다. 정확한 방언·허용 정책·초기 펼침 상태·네트워크·HTML 실행은 결정하지 않았다.

## 제안 컴포넌트와 최소 계약

공통 컴포넌트는 데이터·상태·콜백·`Modifier`를 받고 파일/권한/파싱/저장에 의존하지 않는다. AST 노드마다 Composable을 만드는 대신 아래 시각 단위로 묶는다. 문법별 파싱과 출처 위치 연결은 렌더러 어댑터가 담당한다. 다음 이름·타입·slot은 구현 전 API 검토용이다.

| ID / 제안 | 데이터·변형 | 상태·이벤트 | 레이아웃·접근성·카탈로그 포인트 |
| --- | --- | --- | --- |
| M-01 `MDiveMarkdownHeading` | level 1–6, 인라인 내용 | 정상, 긴 제목; 이동 ID는 호스트 | heading 의미 유지, H1–H6 각각 비교. 단계 차이를 색만으로 표현하지 않음. 줄바꿈 허용 |
| M-02 `MDiveMarkdownText` + 인라인 스타일 규칙 | 문단/run 목록, emphasis/strong/strike/code/link, 줄바꿈 | 링크 클릭·포커스, 검색 강조는 선택적 장식 입력 | 하나의 텍스트 흐름에서 중첩 스타일·기준선 유지. 인라인 코드/링크를 별도 Row로 쪼개지 않음. 본문 복사/선택 정책 미정 |
| M-03 `MDiveMarkdownDivider` 또는 구분선 조합 | 문서 구분선 | 정상 | 기존 Divider 재사용 여부 검토, 주변 블록 간격 포함; 장식선 중복 낭독 방지 |
| M-04 `MDiveMarkdownQuote` | 자식 블록 slot, 깊이 | 정상·복합 콘텐츠 | 단순 문단뿐 아니라 목록/코드 포함. 중첩 시 가용 폭, 장식선 대비. 인용 의미 전달 |
| M-05 `MDiveMarkdownListItem` + marker 규칙 | unordered/ordered/task, 번호·깊이·checked, 자식 slot | 읽기 전용 task 상태 | 여러 자리 번호/RTL/긴 줄 내어쓰기. task는 편집 체크박스로 노출하지 않음. 목록 의미·순서·완료 정보 유지 |
| M-06 `MDiveCodeBlock` | 원문, 언어 라벨, 강조 범위, 선택적 행동 | 일반/언어 미지원/강조 실패, onCopy | 원래 줄·공백 유지, 가로 스크롤; 복사는 원문. 강조 실패도 평문 코드 유지. 복사 결과 피드백은 호스트 |
| M-07 `MDiveMarkdownTable` | 헤더/행/열 정렬, 셀 인라인 내용 | 정상/긴 셀/빈 body | 한 표 안의 열 너비 일관성, 좌·중앙·우 정렬, 가로 스크롤. 행·열 헤더 관계 낭독. 셀 줄바꿈/크기 정책 미정 |
| M-08 `MDiveMarkdownImage` | 이미지 content slot, alt/title, 치수/비율 정보 | loading/ready/local-access-required/remote-failure; onExpand/onConnect/onRetry | 본문을 막지 않는 자리 표시, alt·장식 이미지 구분, 오류도 이미지 문맥 유지. 재시도·연결은 서로 다른 행동 |
| M-09 `MDiveMathInline` / `MDiveRenderedBlock` | inline math 또는 math/diagram content slot, 대체 설명 | loading/ready/render-failure; onViewSource, diagram onExpand | inline 수식 기준선·행간, 블록 긴 수식 가로 접근, Mermaid 확대 진입. 렌더 엔진과 DS 분리; 원문·대체 설명 접근 가능 |
| M-10 `MDiveContentFallback` | 문법 종류, 짧은 이유, 원문 보기 행동 | unsupported/render-failure | 기존 InlineNotice 조합. 일부 실패가 문서 전체 실패로 번지지 않음. HTML 미지원 처리 선택 전 확정 사용하지 않음 |
| M-11 콘텐츠 이벤트 계약 | link target/원문, code source, resource identity | onLink/onCopy/onExpand/onViewSource/onConnect/onRetry | 시각 컴포넌트 아님. UI 이벤트를 호스트 Intent/Effect에 연결. URI 허용·클립보드·네트워크·권한은 호스트 책임 |

M-09의 inline과 block은 배치 제약이 달라 API를 분리할 수 있다. 수식과 Mermaid는 공통 상태 셸을 공유하되 엔진 데이터나 내부 노드 모델을 디자인 시스템에 넣지 않는다. 새 UI 타입은 화면의 도메인 모델/파서 AST를 직접 받지 않는다.

### 문서 조합 규칙

- 문서 여백, 인접 블록 간 간격, 목록 안 문단 간격을 렌더러의 단일 조합 규칙으로 관리한다. 각 자식이 마진을 누적해 두 배 간격을 만들지 않는다.
- 기본 본문·H1–H6·인용·코드·표·인라인 코드의 크기/행간/굵기 관계를 함께 검토한다. 현재 `title`/`section`만으로 H1–H6를 임의 배율로 만들지 않는다. 필요 의미 역할은 토큰 원본에 추가한다.
- 문서 전체 세로 스크롤 안에서 코드·표·긴 렌더 블록만 필요한 가로 스크롤을 소유한다. 전체 문서가 가로로 밀리지 않게 한다.
- 읽기 크기 조절과 시스템 fontScale은 중복 배율이 생기지 않게 적용 지점을 하나로 정한다. 코드·표·수식에 앱 본문 배율을 적용할 범위는 미정이며 카탈로그에서 비교한다.
- 검색 강조/current match·선택·읽던 위치/책갈피 목적지는 화면 상태와 렌더러의 위치 매핑이다. DS가 검색하거나 위치를 저장하지 않는다.

## 토큰·HTML·Compose의 함께 관리할 항목

수치 원본은 [tokens.json](../../design-system/tokens.json), 상태·샘플 원본은 [catalog.json](../../design-system/catalog.json)이며 생성물 직접 수정은 금지한다. [디자인 시스템 관리 규칙](../product/design-system.md)을 따른다.

| 영역 | 검토할 공통 원본 | HTML ↔ Compose 대응 |
| --- | --- | --- |
| 문서 타이포 | H1–H6, body, inline code, quote, table의 의미 역할 | 의미 HTML 태그/스타일 ↔ text style/heading semantics |
| 인라인 | 링크·코드 배경·취소선·강조의 대비와 장식 | inline 흐름 ↔ annotated text; 동일 샘플 문자열 |
| 블록 간격 | 문단·제목 전후·목록 깊이·인용 inset | CSS margin/padding ↔ 조합 spacing; 중복 적용 금지 |
| 코드·표 | 기존 code 색상, 새 필요 역할, 셀 inset·marker | `pre`/table 가로 스크롤 ↔ 블록 내부 scroll |
| 리소스 | 비율·모서리·상태 셸·오류 행동 | 로컬 샘플/SVG·정적 수식 ↔ 준비된 content slot 샘플 |

HTML 카탈로그용 정적 수식·다이어그램은 시각 검토 샘플이며 Android 렌더 엔진 호환 검증으로 계산하지 않는다. UI 자체를 JSON에서 자동 번역하는 프레임워크를 새로 만들지 않는다.

## 다음 세션 작업 순서와 완료 기준

1. **지원 프로필 결정:** CommonMark/GFM 목표 버전, HTML 처리, 수식 구분자/기능, Mermaid 종류를 기존 미정 목록과 함께 확인한다. 결정 전에는 합의된 시각 단위의 가짜 데이터 카탈로그까지만 구현 가능하다.
2. **텍스트 기초:** M-01~05와 문서 조합·토큰을 먼저 만든다. 중첩 인라인, 긴 제목, 혼합 목록, 읽기 전용 task를 함께 검토한다.
3. **실제 코드·표:** M-06~07의 데이터 기반 API를 만들고 기존 고정 Sample은 새 API 호출 예제로 바꾼다. 복사·링크 이벤트 콜백 검증을 추가한다.
4. **리소스 셸:** M-08~10의 정상/로딩/부분 실패와 원문 보기, 확대 진입을 준비된 content slot으로 검토한다. 파일 로더·렌더 엔진 도입은 별도 결정이다.
5. **양쪽 카탈로그:** 같은 ID·샘플·상태로 HTML/Compose 구현, 생성 검사와 필요한 단위/기기 테스트. 세 색상안 × 라이트/다크, 320/360/390 폭, 큰 글자, RTL, TalkBack을 필요한 범위에서 검증한다.
6. **제품 디자인 재연결:** 승인된 새 컴포넌트 표현을 [화면 디자인](screens/index.html)에 적용한다. 실제 제품 화면·파서 구현은 별도 작업이며 이번 문서 완료와 구분한다.

완료 조건은 임의 콘텐츠를 받아 그리는 API, 공유 원본에서 나오는 토큰·상태, 양쪽 실행 카탈로그, 실패해도 주변 본문을 유지하는 상태, 콜백 동작·접근성·스크롤 검증이다. 단순 카탈로그 캡처나 고정 예시 추가만으로 콘텐츠 DS 완료로 처리하지 않는다.

## 검증용 corpus 계획

후속 구현에서 `design-system/` 안의 공유 샘플 원본 위치를 정한다. 이 문서는 실제 파서 fixture나 테스트를 추가하지 않았다. 공식 적합성 테스트와 MDive 디자인 예제를 분리한다.

| fixture ID | 포함할 입력 | 관찰할 결과 |
| --- | --- | --- |
| C-01 text | H1–H6/Setext, 한글·영문·이모지, 긴 제목·문단 | 위계·줄바꿈·fontScale, 잘림 없음 |
| C-02 inline | emphasis+strong+strike+link 중첩, code span, escapes/entities, hard/soft break | 스타일 조합·기준선·텍스트 내용 보존 |
| C-03 list | ordered 시작 번호/여러 자리, unordered, tight/loose, 혼합 중첩, task 2상태 | marker 정렬·내어쓰기·읽기 전용 의미 |
| C-04 containers | 중첩 인용 안 목록/코드, 목록 안 인용·문단·구분선 | 깊이·간격·폭, 이중 스크롤 문제 없음 |
| C-05 code | fence/indent, 언어 없음/미지원, 긴 줄·빈 줄·한글 주석 | 원문·공백·줄 보존, 복사 콜백, 강조 실패 fallback |
| C-06 table | 좌/중/우, 긴 헤더/셀, 많은 열·빈 body·인라인 내용 | 열 정렬·가로 스크롤·헤더 접근성 |
| C-07 links | inline/reference/autolink, 긴 URL, 웹/상대 Markdown/내부 anchor/기타 URI | 같은 외형·콜백, 미정 target 처리를 몰래 실행하지 않음 |
| C-08 image | 로컬/웹 정상·로딩·권한 필요·실패, 긴 alt/alt 없음 | 본문 유지·서로 다른 복구 행동·확대 콜백 |
| C-09 math | inline/블록, 분수·첨자·긴 식, 문법 오류·지원 밖 명령 | 기준선·가로 접근·오류와 원문 보기 |
| C-10 diagram | 지원 예정 유형, 긴/큰 그림, 문법 오류 | 확대·대체 설명·원문·부분 실패 |
| C-11 fallback | HTML/미채택 방언/미완성 구문 | 합의된 정책의 표시; 지원 약속 확대 없음 |
| C-12 mixed | 본문→표→이미지 실패→코드→수식→링크 | 인접 간격, 세로 읽기 지속, 개별 가로 스크롤 |

각 fixture에서 파서 결과 검증과 시각 컴포넌트 검증을 따로 기록한다. 코드 복사는 화면 표시용 변형 문자열이 아닌 원문 전달 여부를 검사한다. 큰 문서 성능, 검색 위치 매핑, 문서 수정 후 위치 복원은 렌더러/화면 통합 검증 항목이다.

## 구현 전에 남은 결정

- CommonMark/GFM 정확한 적합성 목표와 기본 문법 중 지원 예외. 채택한 확장의 방언·구분자·우선순위와 GitHub 서비스 참조의 문맥·해석 정책. HTML·미디어의 허용 태그·속성·실행·접근 정책.
- 수식 delimiter·inline/블록 범위·매크로·오류 대체, Mermaid 종류·버전·렌더 정책.
- 코드 강조 언어·강조 토큰·줄번호 여부(현재 합의 없음), 본문 선택/복사 범위와 코드 합자.
- H1–H6 구체 값, 목록 깊이의 극단 처리, 표 셀 줄바꿈·열 너비, 본문 크기 조절이 코드/표/수식에 미치는 범위.
- 링크 URI와 제목 anchor 정책, 이미지 alt/title 표시, 웹 이미지 재시도·오프라인·치수 변경, 확대 배율·닫기·탐색.
- 검색 결과의 대상/장식과 중첩 스타일 우선순위, 접근성 읽기 순서·수식 대체 설명 제공 방식, 태블릿·가로 모드·성능 기준.

이 결정을 미리 고정하는 파서·라이브러리·추가 모듈·저장소 추상화는 이번 목록에 포함하지 않는다.
