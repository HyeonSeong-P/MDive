# S11+S12 재현 예제

`extensions/extensions.md`와 `target.md`, `target.png`를 함께 기기 Downloads/MDive-S11-S12에 복사하고 파일 선택기로 extensions.md를 연다. 본문은 CommonMark/GitHub 기본과 추가 인라인·정의 목록·일반 callout·GitHub alerts·details를 섞는다.

- emoji는 고정 gemoji Unicode 사전 alias를 시스템 문자로 표시한다. :shipit: 같은 이미지 전용 custom은 사용자 승인에 따라 원문이다. .../--/따옴표 typography 자동 치환은 보류하고 원문을 유지한다.
- `펼칠 내용`은 처음 접혀 있다. 펼치기 전 숨겨진 본문·이미지·Mermaid가 구성/요청되지 않아야 한다. 펼쳐 이미지 폴더를 연결하고 정상 그림·확대/복귀를 확인한다.
- `중첩 내용`은 open 표기를 따른다. 펼침 상태를 변경하고 부모 접기/다시 펴기·스크롤·회전·상대 문서 뒤로에서 같은 방문 상태를 확인한다.
- `상대 문서 열기`는 target.md다. 문서 폴더 연결이 없으면 연결 안내를 사용한다. 뒤로는 기존 본문 위치와 펼침 상태를 유지한다.
- 코드·수식·URI의 문자 원문을 유지하고 잘못된 문법과 지원 밖 HTML도 읽을 수 있게 남긴다. 실제 검증 범위는 구현 계획의 인계를 따른다.
