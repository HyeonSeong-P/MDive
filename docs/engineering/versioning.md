# 앱 버전 전략

상태: 초기 개발 규칙. 배포 채널과 첫 정식 릴리스 범위가 정해지면 갱신한다.

## 두 버전 값

| 값 | 용도 | 규칙 |
| --- | --- | --- |
| `versionName` | 사용자에게 보이는 버전 | `MAJOR.MINOR.PATCH[-alpha.N|-beta.N|-rc.N]` |
| `versionCode` | Android와 Google Play가 업데이트 순서를 판단하는 정수 | Play에 새 빌드를 업로드할 때마다 이전에 업로드한 모든 빌드보다 큰 값 |

두 값은 `app/build.gradle.kts`의 `defaultConfig`에서 관리한다. `versionCode`를 `MAJOR.MINOR.PATCH`에서 계산하지 않는다. 개발 중 로컬 빌드마다 올릴 필요는 없지만, Play의 내부·비공개·공개 테스트와 프로덕션 중 어느 트랙에 새 AAB를 올리든 이미 사용한 코드를 재사용하지 않는다. 같은 AAB를 다른 트랙으로 승격할 때는 빌드를 새로 만들지 않는다. Google Play의 `versionCode` 상한은 `2,100,000,000`이다.

## `versionName` 의미

Android 앱이므로 [Semantic Versioning](https://semver.org/spec/v2.0.0.html)을 사용자 경험에 맞게 적용한다. 여기서 호환성은 앱이 열 수 있는 문서·공유 인텐트·딥링크·저장 데이터·설정 등 사용자와 외부 시스템이 의존하는 동작을 뜻한다. 내부 클래스나 패키지 변경만으로 MAJOR를 올리지 않는다.

- 정식 공개 전 `0.x.y`: 초기 개발 단계. 기능이나 기존 동작의 의미 있는 변경은 MINOR, 같은 범위의 오류 수정은 PATCH를 올린다. `0.1.0`을 첫 개발 목표 버전으로 사용한다.
- 시험 배포: `0.1.0-alpha.1` → `0.1.0-beta.1` → `0.1.0-rc.1` → `0.1.0` 순서로 표기한다. 같은 단계에서 새 후보를 내면 마지막 숫자를 올린다. 단계는 필요할 때만 사용한다.
- `1.0.0`: 핵심 사용자 시나리오와 지원 범위가 확정되고, 데이터·파일 접근 등 이미 제공한 동작의 호환성을 유지할 준비가 된 첫 정식 버전. MVP 범위는 제품 기획에서 정한다.
- `1.x.y` 이후: 호환성을 깨는 변경은 MAJOR, 호환되는 기능 추가는 MINOR, 오류 수정은 PATCH를 올린다. 호환성에 영향이 없더라도 큰 사용자 경험 변경은 릴리스 계획에서 버전 수준을 명시한다.
- 커밋의 `feat`, `fix`, `BREAKING CHANGE`는 릴리스 검토 자료다. 커밋 타입만으로 앱 버전을 자동 결정하지 않는다.

현재 템플릿의 `1.0`은 정식 출시를 뜻하는 버전으로 사용하지 않는다. 아직 공개 배포가 없다는 전제에서 초기값을 `versionName = "0.1.0-alpha.1"`, `versionCode = 1`로 둔다. 이미 Play에 업로드한 빌드가 발견되면 다음 업로드 전에 그 이력을 확인하고 `versionCode`를 더 큰 값으로 조정한다.

## 배포 절차

1. 릴리스 후보의 범위와 `versionName`을 정한다. 새 AAB를 Play에 올릴 예정이면 모든 트랙의 업로드 이력을 보고 다음 `versionCode`를 정한다. `main`에서 시작한 `hotfix/*`도 `develop`의 시험 배포보다 큰 코드를 써야 한다.
2. 두 값을 Gradle에 반영하고 변경사항과 검증 결과를 확인한다. 버전 변경 커밋에는 `chore(release): prepare 0.1.0`처럼 Conventional Commit을 사용한다.
3. 안정 버전은 `develop`에서 `main`으로 병합하고 해당 커밋에 `vMAJOR.MINOR.PATCH` 태그를 단다. 태그의 버전과 `versionName`이 같아야 한다. 실제 배포물은 태그가 가리키는 커밋에서 빌드한다.
4. 배포 후 다음 개발 버전으로 넘어갈 때 `develop`의 `versionName`을 새 목표 버전의 사전 릴리스 표기로 갱신한다. 그 과정에서 `versionCode`가 과거 업로드 값보다 작아지지 않게 한다.

`compileSdk`, `targetSdk`, `minSdk`는 앱 호환성과 플랫폼 요구사항이며 앱 버전 번호와 별도로 결정한다.

## 근거

- [Android 앱 버전 관리](https://developer.android.com/studio/publish/versioning): `versionCode`와 `versionName`의 역할.
- [Google Play versionCode](https://support.google.com/googleplay/android-developer/answer/17367361): 업로드 코드 증가와 상한.
- [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html): `0.x`, 안정 버전, 사전 릴리스 표기.
