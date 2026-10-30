# S08+S09 재현 파일

`samples/images.md`와 `samples/images/`를 함께 기기의 `Download/MDive-S08/` 아래에 복사한다. Android 11+에서는 Download 루트 자체를 폴더로 선택할 수 없으므로 `MDive-S08` 하위 폴더를 선택한다. 폴더 선택기에 Downloads 바로가기가 보이지 않으면 기기 저장소 → `Download` → `MDive-S08`로 들어간다. 제품에서 파일 열기로 `images.md`를 연 다음 첫 로컬 이미지 위치의 ‘이미지 폴더 연결’에서 `MDive-S08`을 선택한다.

PNG·JPEG·WebP는 같은 산·바다·노란 해 그림, GIF는 빨간 첫 프레임(다음 초록 프레임으로 재생하지 않음), SVG는 정적 산·바다와 `SVG & Markdown` 문자열을 표시한다. HTTPS 예제는 네트워크 상태에 따라 실패·재시도를 제공한다. 파일 없음·범위 밖·지원 밖·스크립트 SVG·손상 이미지·HTTP 주소는 해당 위치만 실패하며 본문은 유지한다.

연결 취소/잘못된 폴더/권한 상실을 확인한 뒤 정상 폴더로 복구한다. 재실행 후 같은 Markdown을 다시 선택하면 연결을 재사용한다. 스크롤·확대·테마 전환은 준비된 원본을 재사용한다. 로컬 외부 변경은 자동 감시하지 않으며 폴더를 다시 연결하면 갱신한다. 웹 ‘다시 시도’는 메모리·디스크 캐시를 우회한다.

`images/그림 폴더`의 이미지는 저장소용 결정적 테스트 도형이며 외부 이미지의 복사본이 아니다.

## API 37 에뮬레이터 실기 하네스

`ImageSystemPickerTest`는 실제 Downloads 파일·시스템 선택기·프로세스 종료를 검증하는 명시적 하네스다. 일반 `connectedDebugAndroidTest`에서는 네 단계 모두 건너뛴다. 아래 명령은 `emulator-5554`와 `sdk_gphone64_arm64` 저장소 이름을 사용하는 현재 에뮬레이터 기준이며, 다른 기기는 선택기의 저장소 이름을 맞춰야 한다. HTTPS 단계는 실제 인터넷 연결도 필요하다.

먼저 debug 앱과 테스트 APK를 설치하고 샘플을 준비한다. `adb`가 PATH에 없으면 Android SDK의 `platform-tools/adb`를 사용한다.

```sh
./gradlew :app:assembleDebug :app:assembleDebugAndroidTest
adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
adb -s emulator-5554 install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb -s emulator-5554 shell mkdir -p /sdcard/Download/MDive-S08
adb -s emulator-5554 push samples/images.md /sdcard/Download/MDive-S08/images.md
adb -s emulator-5554 push samples/images /sdcard/Download/MDive-S08/
adb -s emulator-5554 shell am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file:///sdcard/Download/MDive-S08/images.md
```

`setup` → force-stop → `persisted` → force-stop → `revoked` 순서로 실행한다. 중간에 앱 데이터를 지우거나 Gradle의 기기 테스트로 앱을 제거하면 영속 연결 검증의 선행 상태가 사라진다. `https`는 복구 후 별도로 실행한다.

```sh
run_image_phase() {
  adb -s emulator-5554 shell am instrument -w \
    -e class "com.parka.mdive.ui.ImageSystemPickerTest#$1" \
    -e imageProcessPhase "$2" \
    com.parka.mdive.test/androidx.test.runner.AndroidJUnitRunner
}
run_image_phase actualDownloadedImagesConnectCancelWrongFolderAndPersist setup
adb -s emulator-5554 shell am force-stop com.parka.mdive
run_image_phase persistedConnectionAfterProcessRestartAndRevokedGrantRecovers persisted
adb -s emulator-5554 shell am force-stop com.parka.mdive
run_image_phase revokedGrantShowsReconnectAndCanRecover revoked
run_image_phase actualHttpsImageDisplaysPixelsAndExpands https
```

정상 이미지의 fetch/decode 횟수로 캐시 재사용을 확인한다. 웹 실패·파일 없음 등 실패 항목은 성공 이미지 캐시 적중과 구분한다. 원본 파일의 외부 변경 자동 감시는 하지 않으며, 폴더 재연결·명시적 웹 재시도로 갱신한다.
