---
description: ブランチ戦略・コミットメッセージ規約・PR フォーマット
---

## ブランチ戦略

```
main (本番環境)
└── develop (開発・統合環境)
    ├── feature/* (新機能開発)
    ├── hotfix/* (バグ修正)
    └── release/* (リリース準備 ※必要に応じて)
```

- `feature/*` / `hotfix/*` は develop から分岐し、完了後に PR で develop へマージ
- `develop` → `main` : リリース時に PR を出す
- **直接コミット禁止**: すべてのブランチで PR レビューを必須とする
- **マージ方針**: feature / hotfix → develop は squash merge、develop → main は merge commit

---

## コミットメッセージ規約

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type 一覧**:

| type | 用途 |
|---|---|
| feat | 新機能 |
| fix | バグ修正 |
| docs | ドキュメント |
| style | フォーマット（動作変更なし） |
| refactor | リファクタリング |
| perf | パフォーマンス改善 |
| test | テスト追加・修正 |
| build | ビルドシステム（Gradle 設定等） |
| ci | CI/CD 設定 |
| chore | その他（依存関係更新など） |

**例**:
```
feat(device): デバイス登録APIを追加

- DeviceController に POST /api/devices エンドポイント追加
- DeviceService に登録ロジック実装
- Bean Validation で入力値を検証

Closes #123
```

---

## PR フォーマット

```markdown
## 変更の種類
- [ ] 新機能 (feat)
- [ ] バグ修正 (fix)
- [ ] リファクタリング (refactor)
- [ ] ドキュメント (docs)
- [ ] その他 (chore)

## 変更内容
### 何を変更したか
[簡潔な説明]

### なぜ変更したか
[背景・理由]

### どのように変更したか
- [変更点1]
- [変更点2]

## テスト
- [ ] ユニットテスト追加
- [ ] 統合テスト追加
- [ ] 手動テスト実施

## 関連Issue
Closes #[番号]

## レビューポイント
[レビュアーに特に見てほしい点]
```
