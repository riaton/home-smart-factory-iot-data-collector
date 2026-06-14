---
name: development-guideline-java
description: Javaでコードを実装する際に参照するコーディング規約・開発プロセスガイド。実装・レビュー・テスト設計・リリース準備時に使用する。
user-invocable: false
---

# 開発ガイドラインスキル

チーム開発に必要な2つの要素をカバーします:
1. 実装時のコーディング規約 (implementation-guide.md)
2. 開発プロセスの標準化 (process-guide.md)

## 前提条件

本ガイドラインは以下の技術スタックを前提とします:
- **言語**: Java 25
- **ビルドツール**: Gradle
- **テスト**: JUnit 5 / Mockito
- **静的解析**: Checkstyle / SpotBugs

## 参照ドキュメント

| ファイル | 内容 |
|---|---|
| `./guides/implementation-guide.md` | Javaコーディング規約・命名規則・例外処理・DI |
| `./guides/process-guide.md` | テスト戦略・CI/CD 自動化 |
| `./guides/development-workflow.md` | 開発フロー（TDD サイクル・静的解析・AIレビュー） |

## 使用シーン別ガイド

### コード実装時
`./guides/implementation-guide.md` — 命名規則・レイヤー構成・入力検証・例外処理

### 開発フロー確認時
`./guides/development-workflow.md` — TDD サイクル・静的解析・AIレビューの手順

### テスト設計時
`./guides/process-guide.md` — テストピラミッド・CI/CD 自動化
