# 환영 일러스트

`document-plunge.svg`는 사용자가 최종 승인한 `source/document-plunge-approved.png`를 기계적으로 단색 벡터 추적한 공통 원본이다. 문서가 물속으로 다이빙하는 승인 실루엣을 수동 좌표로 다시 그리지 않는다. 흰색 영역은 투명한 여백/구멍으로 유지하며 픽셀 배경을 SVG에 내장하지 않는다.

- 원본: 사용자 승인 PNG, 1254×1254.
- 추출: 밝기 128 미만을 전경으로 이진화. 전경 경계 `(66, 360)–(1204, 890)`으로 흰 주변 여백을 자른다.
- 자동 추적: potrace 1.16, spline 기본값, `--flat --turdsize 4 --opttolerance 0.2`.
- 벡터 viewport: 1138×530. 기본 표시 폭 160dp/px, 높이는 원본 비율을 유지한다.
- SVG `currentColor`, HTML의 공통 CSS mask와 Compose의 `primary` tint로 세 색상안·라이트/다크를 따른다.
- Android 생성물: `designsystem/src/main/res/drawable/mdive_document_plunge.xml`. 직접 수정하지 않는다.

재생성:

```sh
# Pillow가 있는 Python 환경과 potrace가 필요하다.
python tools/generate_welcome_illustration.py --trace
# SVG에서 Android VectorDrawable만 갱신(표준 라이브러리만 필요)
python3 tools/generate_welcome_illustration.py
python3 tools/generate_welcome_illustration.py --check
python3 tools/generate_design_system.py
```

카탈로그의 환영 상태·디자인 화면과 S01 제품 첫 실행 홈에서 사용한다. 사용자 요청으로 최신 디자인을 제품 홈에 반영했으며 파일 선택 기능은 S02 이전까지 연결하지 않는다. 직접 시각 확인은 사용자 요청에 따라 수행하지 않는다.
