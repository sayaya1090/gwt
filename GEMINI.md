# Gemini CLI Foundation Mandates

이 프로젝트는 `sayaya-gwt` Gradle 플러그인 개발 프로젝트입니다. 모든 개발 작업은 다음 문서에 명시된 아키텍처 및 설계 원칙을 반드시 준수해야 합니다.

## 핵심 참조 문서
- **기본 아키텍처**: `docs/architecture.md` (3계층 구조 및 Shared Build Service 원칙)
- **클래스 구조**: `docs/class-diagram.md`
- **실행 흐름**: `docs/sequence-diagram.md`
- **사용자 요구사항**: `docs/use-cases.md`, `docs/requirements.md`

## 개발 원칙 (Mandates)

### 1. 작업 착수 전 체크포인트 (Self-Enforced Checkpoints)
- **코드 수정 전 평가**: 모든 작업 착수 전, 아래 조건을 평가하고 필요 시 서브에이전트를 호출한다.
    - **Gradle 태스크/로직 수정**: `test-specialist`에게 검증 계획 자문 필수.
    - **인프라/아키텍처 변경**: `codebase_investigator`를 통한 영향도 분석 필수.
    - **문서 수정 및 구조 변경**: `docs-keeper`를 통한 링크 무결성 및 계약 정합성 감사 필수.
    - **패턴 이식**: 기존 유사 모듈의 에이전트 노트를 조회하여 베스트 프랙티스 확인.

### 2. GWT 프로젝트 커밋 및 품질 규칙
- **금지 파일**: GWT 캐시 파일(`*.cache.js`, `*.nocache.js`, `*.devmode.js`, `compilation-mappings.txt`, `clear.cache.gif`)은 절대 커밋하지 않는다.
- **로컬 검증 필수**: 커밋 전 반드시 `./gradlew test`를 실행하여 'Green' 상태를 확인한다. 테스트 없이 커밋 가능한 예외는 순수 문서(`*.md`) 수정뿐이다.
- **메시지**: 한국어를 사용하며 Conventional Commits 규격을 준수한다.
- **푸시 금지**: 원격 저장소로의 `push`는 절대 수행하지 않는다. 모든 변경사항은 로컬 커밋까지만 완료한다.

### 3. 지식의 결정화 및 승격 (Crystallization & Promotion)
- **노트 활용**: 에이전트 노트(`*.note.md`)에 기록된 반복 패턴, 실수, 해결 방법론을 주기적으로 검토한다.
- **문서 승격**: 검증된 지식은 에이전트 노트에서 삭제하고, 이 파일(`GEMINI.md`)이나 `.gemini/skills/`의 관련 가이드로 이동(승격)시켜 프로젝트 공식 원칙으로 삼는다.

### 4. 클래스 문서화 (KDoc 필수)
- **모든 클래스**: 신규 생성되거나 수정되는 모든 클래스에는 반드시 KDoc을 작성한다.
- **포함 내용**: 클래스의 역할, 책임, 주요 의존관계, 사용 시 주의사항을 명시한다.

### 5. 에이전트 위임 맥락 전달 (One-shot Context)
- **명확한 위임**: 서브에이전트 호출 시 아래 정보를 반드시 포함한다.
    - **작업 배경**: 왜 이 조사가 필요한지 (1-2문장)
    - **범위**: 최근 커밋, 특정 파일, 특정 모듈 등 경계 설정
    - **제약/형식**: 출력 형식(표, 요약 등) 및 비목적(수정 금지 등) 명시

### 6. 기존 아키텍처 및 리소스 관리
- **계층 구분**: `Layer 1(Gradle)`, `Layer 2(Infrastructure)`, `Layer 3(Testing)` 구조를 유지한다.
- **Build Service**: 웹 서버 리소스는 반드시 `WebServerService`를 통해 싱글톤으로 관리한다.

## 기술 스택 가이드
- 상세한 GWT 및 플러그인 개발 패턴은 `.gemini/skills/gwt-guide/SKILL.md`를 참고할 것.
