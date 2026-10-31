# S10 기본 링크 재현 파일

`samples/links/`의 내용을 기기의 `Download/MDive-S10/`에 복사하고 파일 열기에서 `links.md`를 선택한다. 첫 `하위 문서` 링크에서 `문서 폴더 연결`을 누르고 `MDive-S10`을 선택한다. `하위`를 잘못 연결하거나 선택을 취소해도 현재 본문과 이전 연결은 유지한다. 한글·공백·대문자 확장자, 하위/상위/자기 자신 연속 이동, 파일 없음·범위 밖·fragment·기타 URI와 HTTP/HTTPS 브라우저를 확인할 수 있다.

대상 문서의 `target.png`는 S08의 저장소 소유 결정적 도형을 복사했다. 대상의 이미지가 대상 문서 디렉토리를 기준으로 열리는지, 확대/복귀와 스크롤 재사용을 확인한다. 이미지를 감싼 링크는 그림 탭 확대와 아래 목적지 링크를 별도로 제공한다.

## API37 에뮬레이터 하네스

아래는 `emulator-5554`와 `sdk_gphone64_arm64` 저장소 이름을 사용하는 선택기 기준이다. 다른 기기에서는 저장소 이름을 맞춰야 한다. 브라우저 테스트에는 설치된 브라우저가 필요하며 첫 실행/웹 페이지의 내용은 MDive 검증 범위가 아니다. `LinkSystemPickerTest`는 일반 실행에서 세 테스트를 건너뛰고 명시적 단계 인자가 있을 때만 실제 Downloads/SAF를 사용한다.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell mkdir -p /sdcard/Download/MDive-S10
adb -s emulator-5554 push samples/links/. /sdcard/Download/MDive-S10/
adb -s emulator-5554 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/MDive-S10/links.md
adb -s emulator-5554 shell am instrument -w \
  -e class 'com.parka.mdive.ui.LinkSystemPickerTest#actualFileFolderRecoveryContinuousNavigationBrowserAndReturn' \
  -e linkProcessPhase setup \
  com.parka.mdive.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am force-stop com.parka.mdive
adb -s emulator-5554 shell am instrument -w \
  -e class 'com.parka.mdive.ui.LinkSystemPickerTest#persistedTreeSurvivesProcessRestartThenRevoke' \
  -e linkProcessPhase persisted \
  com.parka.mdive.test/androidx.test.runner.AndroidJUnitRunner
adb -s emulator-5554 shell am force-stop com.parka.mdive
adb -s emulator-5554 shell am instrument -w \
  -e class 'com.parka.mdive.ui.LinkSystemPickerTest#revokedTreeMaintainsOriginalBodyAndReconnects' \
  -e linkProcessPhase revoked \
  com.parka.mdive.test/androidx.test.runner.AndroidJUnitRunner
```

`setup`은 S10 fixture에 해당하는 연결만 초기화한다. S08 이미지 연결·Coil 캐시는 지우지 않는다. `persisted` 단계는 연결 재사용 확인 뒤 S10 tree의 영속 읽기 grant를 해제한다. 다음 force-stop 후 `revoked`에서 실제 권한 상실과 재연결을 확인한다. 중간에 앱 데이터를 지우거나 앱을 제거하면 선행 상태가 사라진다. 이전 문서의 위치 복귀는 세션 내 동작이며 홈 이력이나 영속 읽던 위치 저장을 뜻하지 않는다.

상대 경로의 UTF-8/percent 한 번 해석·범위 경계, 파서 중첩/참조 링크, 비동기 중복/취소/늦은 picker 결과는 `DocumentLinkPolicyTest`·`LinkStateTest`에서 검사한다. `ReaderLinkTest`는 좁은 폭/큰 글자 링크·표·이미지 아래 목적지와 실패 배너, 브라우저 없음/실행 예외의 결정적 어댑터 fixture를 검사한다. 실제 통과·미검증은 구현 계획의 현재 인계를 따른다.
