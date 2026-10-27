# 제품 화면 디자인 초안

[화면 디자인과 Compose 대응 명세 열기](index.html)

기존 `docs/product/`의 기획과 와이어프레임을 수정하지 않고 만든 별도 시각 산출물이다. 제품 동작의 원본은 기존 기획이며, 이 디자인의 미정 표시는 구현 전에 합의해야 한다. 화면 목록은 [screen-index.json](screen-index.json)에도 제공한다.

## 검토

`index.html`을 로컬 브라우저로 직접 연다. 네트워크·서버·설치가 필요 없다. 저장소 안의 생성 토큰 CSS, 카탈로그 CSS, 피드백 CSS, 글꼴과 브랜드 원본을 참조하므로 폴더를 단독으로 이동하지 않는다. 검토용 아이콘은 `design-system/icons.json` 경로의 스냅샷을 HTML에 인라인으로 포함했다. 원본 아이콘 변경 시 함께 동기화한다.

상단에서 세 색상안·라이트/다크·320/360/390 기준 폭·100/130/160% 글자를 비교한다. 왼쪽 목록이나 URL anchor로 상태를 선택한다. **모든 화면 보기 · 인쇄**를 선택하면 전체 보드를 볼 수 있다. JavaScript를 끄면 전체 화면과 인접 명세가 계속 표시된다. 인쇄 CSS는 선택 상태와 무관하게 전체 보드/명세를 출력한다.

화면 안 버튼은 디자인 보드 사이의 검토용 연결이다. 파일 선택·저장·실제 문서 검색·클립보드·확대 제스처는 구현하지 않는다. 검색 입력과 크기 슬라이더는 시연 입력이며, 검색 결과 건수는 고정 예시다. Android 시스템 파일/폴더 선택기는 앱 고유 디자인 범위에서 제외한다.

## Compose 이식 기준

- CSS 좌표만으로 Compose 화면을 구현하지 않는다. 각 보드 옆의 **구성·토큰·상태 계약**과 **구현 전 열린 결정**을 함께 읽는다.
- 공통 수치의 원본은 `design-system/tokens.json`이다. CSS의 토큰명은 Kotlin `DesignTokens`의 해당 그룹/key와 대응한다. 예: `--space-page` → `DesignTokens.Spacing.getValue("page")`, `--size-touch` → `DesignTokens.Sizes.getValue("touch")`, `--radius-panel` → `DesignTokens.Radii.getValue("panel")`. 글꼴은 테마의 의미 역할을 따른다.
- `MDiveTopBar`, `MDiveDocumentRow`, `MDiveSearchBar`, `MDiveTextTab`, `MDiveStatePanel`, `MDiveInlineNotice`, `MDiveRecoveryBanner`, `MDiveToast`, `MDiveConfirmationDialog`, `MDiveBottomSheet`, `MDiveTextField`, `MDiveSlider` 및 공통 버튼/아이콘을 재사용한다.
- `MDiveCodeSample`, `MDiveTableSample`은 고정 카탈로그 샘플이다. 실제 Markdown 렌더러로 직접 연결하지 않는다. 코드·표의 시각 역할과 가로 스크롤을 참고한다.
- 샘플 화면 높이나 상태바/제스처바 높이를 앱 상수로 옮기지 않는다. 실제 인셋, fontScale, IME, 스크롤과 접근성은 Android에서 별도로 검증한다.
- 각 화면의 MVI 계약을 만들고 UI는 Intent를 전달한다. 파일 선택·외부 링크·클립보드는 Effect 처리 책임이다. 새로운 공통 API/도메인 모델/저장 기술을 이 문서가 선행 결정하지 않는다.

## 범위와 승인

홈 7개, 읽기·부분 실패·원문 보기, 검색 3개, 목차·책갈피·이름 편집·설정, 이미지/다이어그램 확대와 저장·복사·링크 실패 안내를 포함한다. 모든 화면은 시각 초안이다. 기존 기획에서 승인된 핵심 흐름과 새 시각 제안을 구분한다. 테마 선택은 검토 도구이며 제품의 시스템 밝기 정책을 변경하지 않는다.

검증 기록은 최종 작업 결과에서 별도 보고한다. 이 파일을 열었다는 것만으로 Android 이식이나 접근성·모션·실제 렌더러 검증이 완료된 것은 아니다.

## 검증 기록 · 2026-10-27

- 브라우저에서 34개 보드를 전체 표시하고 세 색상안 × 라이트/다크 × 320/390 기준 폭의 12개 조합에서 프레임 가로 넘침이 없음을 확인했다.
- 홈·읽기 시각 배치, 320 기준 폭/160% 글자의 다크 검색 한 줄과 책갈피 패널, 390px 브라우저 창의 수식 실패 화면을 직접 확인했다. 화면 선택·테마/폭/배율 전환이 동작하며 브라우저 오류 로그가 없었다.
- 로컬 링크·고유 ID·시연 이동 대상, JavaScript 구문, 공통 디자인 시스템 생성물 최신 여부와 diff 공백 검사를 확인했다.
- 인쇄 스타일은 작성했으나 실제 인쇄 미리보기는 미검증이다. 모든 화면의 모든 글자 배율 조합, Android 이식·TalkBack·실제 확대 제스처 검증은 포함하지 않는다.
