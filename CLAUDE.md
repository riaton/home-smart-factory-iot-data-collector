# home-smart-factory-iot-data-collector

## プロジェクト概要

Home Smart Factory の IoT データ収集クライアント。Raspberry Pi 上で常駐稼働し、IoT デバイス（温湿度センサー・スマートプラグ・人感センサー）からデータを収集して AWS IoT Core へ MQTT で送信する。

Pure Java / Gradle 構成。Spring Boot は使用しない。

---

## ドキュメント

| 種別 | パス |
|---|---|
| 機能要件 | `docs/functional-requirements.md` |
| IoT データ収集シーケンス | `docs/iot-data-collection.md` |
| IoT メッセージフォーマット | `docs/iot-message-format.md` |

---

## 技術スタック

- **言語**: Java 21
- **ビルド**: Gradle
- **実行環境**: Raspberry Pi（ローカル常駐プロセス）
- **通信プロトコル**: MQTT（AWS IoT Core へ送信）
- **CI/CD**: GitHub Actions

---

## ディレクトリ構造

```
src/
  main/java/   # プロダクションコード
  test/java/   # テストコード
docs/          # 設計ドキュメント
config/        # Checkstyle / SpotBugs 設定
```

---

## 開発ルール

- コーディング規約・テスト戦略は `Skill('development-guideline-java')` を参照すること。
- Git 運用・PR フォーマットは `Skill('git-workflow')` を参照すること。
- テストコードの実装パターンは `Skill('java-test-patterns')` を参照すること。

## 行動規範

### 基本的な行動規範
- 変更は必要な箇所のみ。影響範囲を最小化する
- 開発フローは `.claude/rules/development-workflow.md` に定義された手順のみに従うこと。独自の判断でステップを省略・変更・追加しない

### コンテキスト圧迫時の行動規範（焦ったら止まれ）
- コードを読まずに書かない
- 検証を省略しない
- Plan モードを飛ばさない
- サブエージェントを使う（コンテキスト節約）
- 中途半端に終わらせるなら止まる
- 焦りを自覚したら宣言する
