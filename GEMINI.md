# Gemini CLI Foundation Mandates

이 프로젝트는 `sayaya-gwt` Gradle 플러그인 개발 프로젝트입니다. 모든 개발 작업은 다음 문서에 명시된 아키텍처 및 설계 원칙을 반드시 준수해야 합니다.

## 핵심 참조 문서
- **기본 아키텍처**: `docs/architecture.md` (3계층 구조 및 Shared Build Service 원칙)
- **클래스 구조**: `docs/class-diagram.md`
- **실행 흐름**: `docs/sequence-diagram.md`
- **사용자 요구사항**: `docs/use-cases.md`, `docs/requirements.md`

## 개발 원칙 (Mandates)
1. **아키텍처 준수**: 새로운 기능을 추가하거나 수정할 때 `Layer 1(Gradle)`, `Layer 2(Infrastructure)`, `Layer 3(Testing)` 중 어디에 해당하는지 명확히 구분하고 기존 구조를 깨뜨리지 말 것.
2. **리소스 관리**: 웹 서버 관련 변경 시 반드시 `WebServerService`(Gradle Build Service)를 통해 싱글톤 인스턴스 및 생명주기가 관리되도록 할 것.
3. **테스트 우선**: 플러그인 기능 변경 시 `test/` 모듈의 통합 테스트를 통해 Playwright 및 Kotest 기반의 검증을 반드시 수행할 것.
4. **문서 동기화**: 아키텍처나 클래스 구조에 중요한 변경이 생길 경우, `docs/` 하위의 관련 문서들도 즉시 업데이트할 것.

## 기술 스택 가이드
- 상세한 GWT 및 플러그인 개발 패턴은 `.gemini/skills/gwt-guide/SKILL.md`를 참고할 것.
