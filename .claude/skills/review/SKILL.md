---
name: review
description: 3エージェント構成でCheckstyle/SpotBugs静的解析・設計書整合性・品質検証を実施する
argument-hint: "<path/to/.claude/plans/*.plan.md>"
allowed-tools: Read, Glob, Grep, Agent, Bash
user-invocable: true
---

# レビューコマンド

development-workflow のステップ 3〜5 に対応する3エージェントレビューを実施する。
エージェントA（静的解析）が完了後、エージェントB・C を**並列**で起動する。

**入力**: `$ARGUMENTS`（プランファイルパス。省略可）

---

## フェーズ 0 — findings ファイルの準備

プランファイルが指定されている場合はそこから機能名を取得する。
指定がない場合は現在のブランチ名（`git branch --show-current`）を機能名とする。

- findings ファイルパス: `.claude/review/{機能名}-findings.md`
- `.claude/review/` が存在しない場合は `mkdir -p` で作成する
- findings ファイルが既存の場合は**削除して新規作成**する

findings ファイルの初期テンプレート（`{機能名}` を実際の名前に置き換えること）:

```markdown
# レビュー指摘: {機能名}

## B: 設計書整合性・コーディング規約（reviewer-implementation-validator）

（エージェントの完了報告をここに書き出す）

## C: セキュリティ・品質・パフォーマンス（reviewer-quality-checker）

（エージェントの完了報告をここに書き出す）

## B再実行: 追加指摘（修正後）

（再実行後の完了報告をここに書き出す）
```

---

## フェーズ 1 — エージェントA: 静的解析（直列）

`reviewer-static-analyst` サブエージェントを起動する。

エージェントAが完了するまでフェーズ2に進まないこと。

---

## フェーズ 2 — エージェントB・C: 整合性チェック・品質検証（直列）

同一ファイルへの同時書き込み競合を避けるため、B → C の順に直列で起動する。
**各エージェントへの指示には findings ファイルのパス（`{findingsPath}`）と書き出し先セクション名のみを伝える。**
エージェントの検証内容はそれぞれのエージェント定義に記載されているため、再指示しない。

### エージェントB: `reviewer-implementation-validator`

追加指示: 完了証拠として出力した内容全体を **Edit ツール**を使って
findings ファイル `{findingsPath}` の「B: 設計書整合性・コーディング規約」セクションに書き出すこと。

エージェントBの完了を確認してから、エージェントCを起動すること。

### エージェントC: `reviewer-quality-checker`

追加指示: 完了証拠として出力した内容全体を **Edit ツール**を使って
findings ファイル `{findingsPath}` の「C: セキュリティ・品質・パフォーマンス」セクションに書き出すこと。

---

## フェーズ 3 — 指摘修正

findings ファイルを Read して全指摘を確認し、1件ずつ処理する:

1. **修正する場合** — 最小限の変更でコードを修正し、findings ファイルの当該行の状態列を `✅ 修正済み` に更新する
2. **対応不要の場合** — 理由を添えて状態列を `➖ 対応不要（理由）` に更新する

全指摘の処理後に `./gradlew test` を実行して全緑を確認すること。

---

## フェーズ 3.5 — エージェントB 再実行（修正後チェック）

`reviewer-implementation-validator` サブエージェントを再起動する。

追加指示: 完了証拠として出力した内容全体を **Edit ツール**を使って
findings ファイル `{findingsPath}` の「B再実行: 追加指摘（修正後）」セクションに書き出すこと。

追加指摘がある場合はフェーズ3に戻って修正し、再度フェーズ3.5を実行すること（最大2回まで）。

---

## フェーズ 4 — 完了報告

全エージェントの結果と findings ファイルのパスを集約してユーザーに提示する:

```
## レビュー完了: {機能名}

findings ファイル: {findingsPath}

| エージェント | 内容 | 判定 |
|---|---|---|
| A: 静的解析 | Checkstyle/SpotBugs 違反X件修正 | ✅ / ❌ |
| B: 設計書整合性・規約 | 指摘X件修正 | ✅ / ❌ |
| C: セキュリティ・品質 | 指摘X件修正 | ✅ / ❌ |
| B再実行 | 追加指摘X件 | ✅ / ❌ |

次のステップ:
  - git commit → PR を develop へ作成
```
