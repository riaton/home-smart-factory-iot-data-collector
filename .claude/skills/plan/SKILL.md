---
name: plan
description: 設計書から Java/Gradle プロジェクト向けの実装プランを生成する
allowed-tools: Read, Write, Bash, Glob, Grep
user-invocable: true
---

# プラン生成コマンド

設計書から実装プランを作成する。コードは一切書かない。
ユーザーの確認を受けてから `/implement` へ渡す。

## フェーズ 1 — 要件の把握

docs/  配下の設計書を読み、実装対象の機能に関する要件を把握する。

以下を把握する:

- **何を作るか**（具体的な成果物）
- **なぜ必要か**（ユーザー価値）
- **どこに影響するか**（コードベースの範囲）

情報が不足・曖昧な場合は**ここで停止して確認する**。推測でプランを作らない。

---

## フェーズ 2 — 過去の決定事項を参照

既存プランファイルの決定事項ログを確認し、現在の設計に活かす:

```bash
ls .claude/plans/*.plan.md 2>/dev/null
```

各ファイルの `## 決定事項ログ` セクションを読む。
現在の機能に関連する判断（技術選択・避けるべきパターン・既知の落とし穴）があれば、
プランの GOTCHA や MIRROR に反映する。

---

## フェーズ 3 — 開発フロー・ルール確認

以下を**全文読む**（スキップ禁止）。プランのタスク構成はこれらのフローに沿って組み立てる。

```
.claude/skills/development-guideline-java/guides/development-workflow.md  # 開発フロー
.claude/skills/git-workflow/git-workflow.md                               # Git 運用・PR フォーマット
```

---

## フェーズ 4 — プランファイル生成

```bash
mkdir -p .claude/plans
```

出力先: `.claude/plans/{kebab-case-機能名}.plan.md`

### プランファイルのテンプレート

```
# プラン: {機能名}

**設計書**: {パスまたは "なし"}
**複雑度**: {Small | Medium | Large}
**作成日**: {日付}

## サマリー
{2-3文で何を作るかを説明}

## 変更ファイル

| ファイル | アクション | 理由 |
|---------|----------|------|
| `src/main/java/...` | CREATE | {理由} |
| `src/test/java/...` | CREATE | {理由} |

## タスクリスト

- [ ] Task 1: {タスク名}
- [ ] Task 2: {タスク名}
- [ ] Task 3: {タスク名}
- [ ] Task N: {タスク名}

## タスク詳細

### Task 1: {タスク名}
- **IMPLEMENT**: {実装すべきロジック・設計書の該当箇所}
- **TEST**: {テストケース（正常系・異常系・境界値）}
- **GOTCHA**: {既知の落とし穴・注意点（あれば）}
- **RED**: `./gradlew test --tests "{テストクラス}.{テストメソッド}"` → 失敗を確認
- **GREEN**: 最小実装 → `./gradlew test --tests "{テストクラス}.{テストメソッド}"` → 全緑確認
- **REFACTOR**: コード整理 → `./gradlew test --tests "{テストクラス}.{テストメソッド}"` → 緑確認

### Task 2: {タスク名}
- **IMPLEMENT**: ...
- **TEST**: ...
- **GOTCHA**: ...
- **RED**: `./gradlew test --tests "..."` → 失敗を確認
- **GREEN**: 最小実装 → `./gradlew test --tests "..."` → 全緑確認
- **REFACTOR**: コード整理 → `./gradlew test --tests "..."` → 緑確認

## バリデーションコマンド

\`\`\`bash
./gradlew test
./gradlew build -x test
\`\`\`

## 受け入れ条件

- [ ] 全タスク完了
- [ ] `./gradlew test` がパス（全テスト緑）
- [ ] `./gradlew build -x test` が成功

## 実装時メモ

| タイミング | 内容 | 理由 |
|-----------|------|------|
| （実装完了後に追記） | | |
```

---

## フェーズ 5 — implement へ進む

プランを提示し、そのまま `/implement .claude/plans/{name}.plan.md` を実行する。

```
プランを生成しました: .claude/plans/{name}.plan.md

タスク数: {N}
複雑度: {Small|Medium|Large}
変更ファイル数: {N}

実装を開始するには: /implement {機能名}
```
