# MDive 브랜드 마크

2026-10-26 사용자가 [시안 10](mdive-icon-concept-v10.png)을 앱 아이콘으로 선택했다. `md`가 사선으로 연결되고 d의 안쪽과 바깥쪽에 물결이 이어지는 형태다. 생성 도구와 전체 프롬프트는 [시안 기록](mdive-icon-concept-v10.md)에 있다.

## 원본과 파생물

- `mdive-icon-concept-v10.png`: 승인한 생성 이미지. 디자인 참조와 이력으로 보관한다.
- `mdive-mark.svg`: 승인 이미지의 윤곽을 추출한 배경 없는 벡터. 원본의 글자·내부 공간·안팎 물결을 유지하며, 두 전경색을 실제 심해 테마의 `onBackground`·`primary`에 맞춰 정리했다.
- Android 런처와 UI용 벡터는 `tools/generate_launcher_icons.py`로 생성한다. 배경색은 심해 다크 테마의 `background`를 사용한다. 파생 XML을 직접 수정하지 않는다.

윤곽 추출에는 임시 도구 환경의 `vtracer==0.6.15`를 사용했다. 프로젝트 런타임이나 빌드 의존성은 아니다. 변환 옵션은 `colormode=color, hierarchical=cutout, color_precision=6, filter_speckle=20, layer_difference=16, path_precision=2`다. 배경색 경로를 제외하고 전경색만 정규화했다. 재생성에는 이미 보관된 SVG와 Python 표준 라이브러리만 필요하다.

```sh
python3 tools/generate_launcher_icons.py
python3 tools/generate_launcher_icons.py --check
```

## 사용 구분

- **앱 아이콘:** 배경·전경·단색 레이어와 구형 Android용 일반·원형 벡터를 제공한다. 전경은 108dp 안의 중앙 안전 영역에 배치하며 원형 마스크의 잘림과 내부 공간의 투명도를 기기 테스트로 확인한다. [Android 공식 규격](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive)
- **UI 대표 심볼:** `MDiveBrandMark`로 사용한다. 기본은 현재 테마의 단색이며, `tint = Color.Unspecified`로 원래 두 색을 사용할 수 있다. 원색 버전은 짙은 배경에 배치한다. 버튼용 동작 아이콘과 구분한다.
- **상단바:** `MDiveTopBar.leading` 슬롯에 대표 심볼을 넣을 수 있다. 제목과 같이 표시하면 `contentDescription = null`로 중복 낭독을 피한다. 뒤로 가기 아이콘이 있으면 탐색 아이콘을 우선한다.

아이콘 카탈로그에서 테마색·원색을 함께 검토할 수 있다. 런처의 배경이나 안전 영역 여백을 UI 대표 심볼에 그대로 붙이지 않는다. 선택하지 않은 탐색안과 초기 화면 캡처는 로컬 검토 파일로 남기며 PR에는 승인본과 재현 가능한 원본·생성물을 보관한다.
