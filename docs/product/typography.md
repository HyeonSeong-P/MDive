# 타이포그래피

상태: 사용자에게 글꼴 선정을 위임받아 선택한 디자인 초안. 정적 폰트 파일을 디자인 시스템 모듈과 debug 카탈로그에 적용했다. 구체적 표현은 카탈로그에서 사용자 검토 전이며, 제품의 글꼴 선택 설정 등 새 기능을 추가하지 않는다.

## 선택과 역할

| 역할 | 글꼴 | 선택 이유 |
| --- | --- | --- |
| 문서 본문·문서 제목·인용 | Pretendard | 깔끔한 고딕체로 읽기 흐름을 유지하고, 행간·여백·부드러운 대비로 편안함을 만든다. |
| 앱 UI·파일명·검색·표 | Pretendard | 한글·영문 혼합 UI와 조밀한 정보에 명료한 고딕체. |
| 인라인 코드·코드 블록 | JetBrains Mono | 코드 구조와 비슷한 영문·숫자 기호를 구분하기 위한 고정폭 글꼴. |

‘편안한 심해’는 색과 여백으로 표현하며 글꼴에 장식을 더하지 않는다. 사용자 시각 검토에서 명조 본문의 인상이 맞지 않아 본문과 제목도 Pretendard로 변경하고 Noto Serif KR은 제거했다. 크기·굵기·행간은 아래 역할 묶음으로 초안을 정의하고 카탈로그 검토 후 확정한다. 코드 합자 사용 여부는 미정이다. 본문 크기에 비례하는 행간 정책을 따른다.

## 역할별 크기·굵기·행간

`design-system/tokens.json`의 `fontSizes`·`lineHeights`는 척도이며, `typography`는 이를 글꼴 역할·굵기와 조합한 의미 단위다. **title, section, uiTitle, body, bodySmall, ui, label, caption, code**를 정의한다. 플랫폼 코드에서 UI 크기에 임의 비율을 곱해 제목이나 보조설명을 만들지 않는다.

Compose는 생성된 역할 묶음을 `mdiveTextStyle(role)`로 변환한다. HTML도 같은 정의로 역할별 스타일과 설명을 생성한다. 실제 수치의 원본은 JSON 하나이며 Markdown에 별도 수치 표를 중복 관리하지 않는다. 카탈로그의 타이포그래피 상세에서 각 역할의 글꼴·크기·굵기·행간과 실제 예시를 함께 확인한다.

표시하는 sp 수치는 기본 글자 배율 기준이다. Compose의 시스템 글자 크기 확대를 유지하며, 향후 제품 본문 크기 조절에서도 글자 크기와 행간을 같은 비율로 조정한다. HTML의 CSS px 미리보기와 Android의 sp는 동일한 실제 픽셀 크기를 보장하지 않는다.

## 공식 출처와 배포 조건

2026-10-26 공식 배포처의 라이선스 원문을 확인했다. 두 글꼴 모두 SIL Open Font License 1.1로, 조건을 지키면 상업적 이용 및 유료·무료 앱에 포함한 배포에 별도 글꼴 사용료가 없다.

- Pretendard: [공식 저장소](https://github.com/orioncactus/pretendard), [라이선스·저작권](https://github.com/orioncactus/pretendard/blob/main/LICENSE).
- JetBrains Mono: [공식 저장소](https://github.com/JetBrains/JetBrainsMono), [라이선스·저작권](https://github.com/JetBrains/JetBrainsMono/blob/master/OFL.txt).

폰트 파일 도입 시 해당 파일의 배포 버전·원본 URL·해시를 기록하고, 그 배포본에 동봉된 저작권 고지와 라이선스 전문을 함께 보관·배포한다. 사용자가 확인 가능한 라이선스 제공 방식을 갖춘다. 폰트 자체를 단독 판매하지 않으며, 폰트는 OFL을 유지한다. 수정·서브셋·형식 변환 시 Reserved Font Name을 포함한 조건을 확인한다. 폰트 포함만으로 앱 소스 공개를 요구하는 조건은 아니다.

공식 배포 파일을 `designsystem/src/main/res/font/`에 로컬 포함하고 런타임 CDN 다운로드에 의존하지 않는다. API 24에서도 사용할 수 있도록 static Pretendard Regular·SemiBold, JetBrains Mono Regular를 사용한다. 총 원본 폰트 용량은 약 3.4 MB이며, 최적화나 서브셋 변경은 아직 하지 않았다. 버전·출처·SHA-256은 [sources.json](../../designsystem/src/main/assets/font_licenses/sources.json), 라이선스 전문과 저작권은 같은 폴더에 보관한다. Compose 카탈로그의 ‘글꼴 라이선스’에서 전문을 볼 수 있다. HTML과 Compose는 같은 원본 글꼴과 역할 이름을 대응시킨다.

## Compose 카탈로그 검증

- 라이트·다크에서 긴 한글·영문 혼합 문단의 획 선명도와 읽기 흐름을 확인한다.
- 본문 크기와 비례 행간 변경 시 제목·강조·인용 위계 및 잘림을 확인한다.
- 본문과 인라인 코드의 크기·기준선, 코드의 `0/O`, `1/l/I`, 긴 줄, 한글 주석 대체 글꼴과 정렬을 확인한다. 한글 코드가 단순 UI 고딕 대체로 충분한지 실제 확인 전에는 확정하지 않는다.
- 좁은 화면에서 표·긴 파일명·한 줄 검색바의 줄바꿈·잘림·굵기 구분을 확인한다.

원본 제품 방향: [overview.md](overview.md). 기존 HTML 시안의 시스템 글꼴은 아직 교체하지 않았으며, 새 글꼴의 시각 검증 근거로 사용하지 않는다.
