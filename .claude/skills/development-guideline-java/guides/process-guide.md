# プロセスガイド (Process Guide)

## 基本原則

### 1. 具体例を豊富に含める

抽象的なルールだけでなく、具体的なコード例を提示します。

**悪い例**:
```
変数名は分かりやすくすること
```

**良い例**:
```java
// ✅ 良い例: 役割が明確
IotDataService iotDataService = new IotDataService(repository);
AnomalyLogRepository anomalyLogRepository = new AnomalyLogRepository();

// ❌ 悪い例: 曖昧
Service svc = new Service();
Repository repo = new Repository();
```

### 2. 理由を説明する

「なぜそうするのか」を明確にします。

**例**:
```
## 例外を握り潰さない

理由: catch して何もしないと、問題の原因究明が困難になります。
予期されるエラーは適切に処理し、予期しないエラーは上位に伝播させて
ログに記録できるようにします。
```

### 3. 測定可能な基準を設定

曖昧な表現を避け、具体的な数値を示します。

**悪い例**:
```
コードカバレッジは高く保つこと
```

**良い例**:
```
コードカバレッジ目標:
- ユニットテスト: 80%以上（Service 層は 90%以上）
- 統合テスト: 60%以上
- E2Eテスト: 主要フロー100%
```

---

## テスト戦略

### テストピラミッド

```
       /\
      /E2E\       少 (遅い、高コスト)
     /------\
    / 統合   \     中  (複数クラスを組み合わせたテスト)
   /----------\
  / ユニット   \   多 (速い、低コスト) (JUnit 5 / Mockito)
 /--------------\
```

**目標比率**:
- ユニットテスト: 70%
- 統合テスト: 20%
- E2Eテスト: 10%

### テストの書き方

**Given-When-Then パターン**:

```java
class IotDataServiceTest {

    @Test
    @DisplayName("正常なペイロードでIoTデータを保存できる")
    void saveFromSqs_validPayload_savesData() {
        // Given: 準備
        IotMessagePayload payload = new IotMessagePayload("device-001", 25.5, 60.0, null, Instant.now());
        given(iotDataRepository.save(any())).willReturn(IotData.from(payload));

        // When: 実行
        iotDataService.saveFromSqs(payload);

        // Then: 検証
        then(iotDataRepository).should().save(any(IotData.class));
        then(anomalyDetector).should().check(any(IotData.class));
    }

    @Test
    @DisplayName("タイトルが空の場合ValidationExceptionをスローする")
    void saveFromSqs_blankDeviceId_throwsException() {
        // Given: 準備
        IotMessagePayload payload = new IotMessagePayload("", 25.5, 60.0, null, Instant.now());

        // When / Then: 実行と検証
        assertThatThrownBy(() -> iotDataService.saveFromSqs(payload))
                .isInstanceOf(ValidationException.class);
    }
}
```

---

## 自動化の推進

### 品質チェックの自動化

**自動化項目と採用ツール**:

1. **コードスタイルチェック**
   - **Checkstyle**
     - Google Java Style Guide をベースにチームルールを定義
     - 設定ファイル: `config/checkstyle/checkstyle.xml`
     - `./gradlew checkstyleMain` で実行

2. **静的解析**
   - **SpotBugs**
     - バグパターン（NullPointer、リソースリーク等）を自動検出
     - `./gradlew spotbugsMain` で実行
   - **PMD**（任意）
     - コードの複雑度・重複を検出

3. **テスト実行**
   - **JUnit 5 + Mockito**
     - ユニットテスト / 統合テスト
     - `./gradlew test` で実行

4. **ビルド確認**
   - `./gradlew build` で全チェック + コンパイル + テストを一括実行

**build.gradle 設定例**:

```groovy
plugins {
    id 'java'
    id 'checkstyle'
    id 'com.github.spotbugs' version '6.0.0'
}

checkstyle {
    toolVersion = '10.21.0'
    configFile = file('config/checkstyle/checkstyle.xml')
}

spotbugs {
    toolVersion = '4.9.0'
    excludeFilter = file('config/spotbugs/exclude.xml')
}

test {
    useJUnitPlatform()
}
```

**CI/CD (GitHub Actions)**:

```yaml
# .github/workflows/ci.yml
name: CI
on: [push, pull_request]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'
          cache: gradle

      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      - name: Checkstyle
        run: ./gradlew checkstyleMain checkstyleTest

      - name: SpotBugs
        run: ./gradlew spotbugsMain

      - name: Test
        run: ./gradlew test

      - name: Build
        run: ./gradlew build -x test
```

**Pre-commit フック（git hook + Gradle）**:

```bash
# .git/hooks/pre-commit
#!/bin/sh
echo "Running pre-commit checks..."
./gradlew checkstyleMain spotbugsMain test --daemon
if [ $? -ne 0 ]; then
  echo "Pre-commit checks failed. Fix errors before committing."
  exit 1
fi
```

**導入効果**:
- コミット前に自動チェックが走り、不具合コードの混入を防止
- PR作成時に自動でCI実行され、マージ前に品質を担保
- 早期発見により、修正コストを大幅削減

**この構成を選んだ理由**:
- 2025年時点での Java エコシステムにおける標準的な構成
- Checkstyle + SpotBugs は大規模 Java プロジェクトで広く採用実績あり
- Gradle との親和性が高く、既存ビルドに追加しやすい

---

## チェックリスト

- [ ] ブランチ戦略が決まっている
- [ ] コミットメッセージ規約が明確である
- [ ] PRテンプレートが用意されている
- [ ] テストの種類が定義されている（ユニット / 統合 / E2E）
- [ ] コードレビュープロセスが定義されている
- [ ] CI/CDパイプラインが構築されている（GitHub Actions + Gradle）
- [ ] Checkstyle / SpotBugs の設定ファイルがリポジトリに含まれている
