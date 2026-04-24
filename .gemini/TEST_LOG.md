# Test Log - GWT Test URL Generation Bug Verification

## Status Checklist
- [x] Research: Understand current URL generation logic and web server implementation.
- [x] Hypothesis: Identify the root cause of `file://` fallback.
- [x] Unit Test: Create `GwtUrlValidationTest.kt` to reproduce the bug.
- [x] Green Phase: Verify `GwtUrlValidationTest.kt` passing.
- [x] Integration Test: Design scenario for HTTP accessibility verification.
- [x] Red Phase: Confirm failures on v2.2.9.1.
- [x] Report: Final summary of findings.
- [x] Multiple Roots: Design and implement support for multiple content roots.

## Activity Log

### [Initial State]
- Project: sayaya-gwt
- Version: v2.2.9.1
- Issue: GwtTestSpec.kt always uses `file://` instead of HTTP with random port.
- Mandate: Enforce HTTP connection based on random port.
- [Confirmed] GwtTestSpec.kt의 115행 부근에서 'file://' 프로토콜만 지원하며 HTTP 연동이 누락된 버그를 확인했습니다.

### [2025-05-22] TDD Red Phase: URL Validation Test
- `test/src/test/kotlin/dev/sayaya/gwt/test/GwtUrlValidationTest.kt` 작성 완료.
- Kotest의 BehaviorSpec을 사용하여 Scenario A, B, C-1, C-2 검증.
- `gradlew :test:test` 실행 결과: **4 tests completed, 4 failed**.

### [2026-04-24 12:50:33] - Green Phase: GwtUrlValidationTest 통과
- `dev.sayaya.gwt.test.GwtUrlValidationTest` 실행 결과 모든 시나리오 통과 확인.

### [2026-04-24 12:54:38] - Integration Test: HTTP Accessibility Verification
- **결론**: WebServerService가 HTTP 프로토콜을 통해 리소스를 정상적으로 제공함을 확인하였습니다.

### [2026-04-24 13:22:16] - 정책 완화(기본값 제공)에 따른 테스트 코드 정렬 완료
- GwtTestSpecUrlTest.kt: Case 1 수정 (IllegalStateException -> 기본값 확인)
- GwtUrlValidationTest.kt: Scenario B 수정 (IllegalStateException -> 기본값 확인)

### [2026-04-24 14:15:00] - 결함 분석: 번들 파일(*.cache.js) 접근 불가 현상
- **원인**: 웹 서버가 단일 경로(devMode.war)만 서빙하여 GWT 컴파일 출력물과 소스 리소스 간의 괴리 발생.
- **해결책**: WebServerService가 다중 컨텐츠 루트(Multiple Content Roots)를 지원하도록 리팩토링 결정.
