# Git 작업 규칙

## 커밋 메시지

[Conventional Commits 1.0.0](https://www.conventionalcommits.org/en/v1.0.0/) 형식을 사용한다.

```text
<type>(<scope>): <summary>

<body>

<footer>
```

- `scope`, 본문, footer는 필요할 때만 쓴다. 제목은 `type: summary`만 있어도 된다.
- `summary`는 변경을 설명하는 짧은 문장으로 작성한다. 한 커밋에는 하나의 논리적 변경을 담는다.
- 기능 추가는 `feat`, 오류 수정은 `fix`를 쓴다. 그 외 `refactor`, `docs`, `test`, `chore`, `build`, `ci`, `perf`, `style`을 사용한다. 새 유형은 의미가 겹치지 않을 때 추가할 수 있다.
- 호환성을 깨는 변경은 `feat!:`처럼 `!`를 붙이거나 본문 뒤에 `BREAKING CHANGE: 설명` footer를 적는다.
- scope는 변경 범위를 드러낼 때만 쓴다. 예: `viewer`, `parser`, `navigation`, `mvi`, `build`.
- PR을 squash merge할 때 최종 커밋 메시지에도 이 형식을 적용한다.

예시:

```text
feat(viewer): render fenced code blocks
fix(files): handle unreadable document URI
refactor(mvi): simplify effect collection
docs(workflow): define branch rules
feat(parser)!: change document identifier format
```

## 브랜치 전략

버전이 있는 Android 앱에 맞춰 [Gitflow의 장기 브랜치와 단기 작업 브랜치](https://nvie.com/posts/a-successful-git-branching-model/)를 간소화해 사용한다. 작업 브랜치를 짧게 유지하고 PR로 통합하는 절차는 [GitHub flow](https://docs.github.com/en/get-started/using-github/github-flow/)를 참고한다.

| 브랜치 | 출발점 | 병합 대상 | 용도 |
| --- | --- | --- | --- |
| `main` | 장기 유지 | — | 릴리스 가능한 버전과 태그 |
| `develop` | 장기 유지 | 릴리스 시 `main` | 다음 릴리스의 통합 |
| `feat/<slug>` | `develop` | `develop` | 기능 추가 |
| `fix/<slug>` | `develop` | `develop` | 다음 릴리스에 포함할 버그 수정 |
| `refactor/<slug>` | `develop` | `develop` | 동작을 유지하는 구조 개선 |
| `docs/<slug>`, `test/<slug>`, `chore/<slug>` | `develop` | `develop` | 문서, 테스트, 유지보수 |
| `hotfix/<slug>` | `main` | `main`과 `develop` | 배포 버전의 긴급 수정 |

- `<slug>`는 작업 내용을 나타내는 짧은 소문자 kebab-case 이름을 쓴다. 이슈 번호가 있으면 앞에 넣어도 된다. 예: `feat/42-document-search`.
- 새 작업은 최신 `develop`에서 브랜치를 만든다. 하나의 브랜치에는 하나의 목적을 담는다. 변경을 검증한 뒤 대상 브랜치로 PR을 열고, 병합 후 작업 브랜치를 삭제한다.
- `main`에는 일반 기능 작업을 직접 넣지 않는다. 릴리스할 때 검증된 `develop`을 `main`으로 병합하고 `vMAJOR.MINOR.PATCH` 태그를 단다. 버전 번호와 `versionCode` 규칙은 [앱 버전 전략](versioning.md)을 따른다.
- 배포 버전의 긴급 수정은 `main`에서 `hotfix/*`를 만들고 `main`에 반영한 후 같은 수정이 `develop`에도 들어가도록 병합한다.
- 릴리스 안정화 기간이나 여러 유지 버전이 생기기 전에는 `release/*` 브랜치를 만들지 않는다. 필요해지면 이 문서에 출발점·병합 대상을 정한다.
- 이 문서는 작업 규칙이다. GitHub 브랜치 보호 및 필수 리뷰 설정은 별도 저장소 설정으로 적용할 수 있다.
