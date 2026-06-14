---
name: java-test-patterns
description: このプロジェクト（Pure Java / JUnit 5 / Mockito BDD）固有のテスト実装パターン。ユニットテストの雛形、BDD スタイルの Mock 記法、テストメソッド命名規則を網羅する。テストクラスを新規作成するとき、既存テストを修正するとき、またはテストの書き方を確認するときに必ず参照する。
user-invocable: false
---

# Java テストパターン（このプロジェクト専用）

## 基本方針

- Spring DI コンテキストは存在しない。依存はコンストラクタで直接注入する。
- モックは Mockito のみ。`@ExtendWith(MockitoExtension.class)` + `@Mock` + `@BeforeEach` でセットアップする。
- アサーションは AssertJ を使う。

---

## ユニットテストの雛形

```java
@ExtendWith(MockitoExtension.class)
class XxxProcessorTest {

    @Mock
    private XxxRepository xxxRepository;

    @Mock
    private AnotherService anotherService;

    private XxxProcessor processor;

    @BeforeEach
    void setup() {
        processor = new XxxProcessor(xxxRepository, anotherService);
    }
}
```

**ポイント:**
- `@InjectMocks` は使わない。`@BeforeEach` でコンストラクタを直接呼び出す。
- UUID 定数はメソッドに渡す型に合わせて `UUID` で定義する。

```java
private static final UUID DEVICE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
```

---

## Mock 記法（BDD スタイル）

全て `org.mockito.BDDMockito.*` の静的インポートを使う。

```java
// 戻り値あり
given(xxxRepository.findById(any())).willReturn(Optional.of(entity));
given(anotherService.process(eq(DEVICE_ID), any())).willReturn(result);

// void メソッド
willDoNothing().given(anotherService).notify(any());

// 例外スロー（戻り値あり）
given(xxxRepository.findById(any())).willThrow(new DataNotFoundException("..."));

// 例外スロー（void メソッド）
willThrow(new DataNotFoundException("...")).given(anotherService).notify(any());

// 呼び出し検証
then(xxxRepository).should().save(any());
then(anotherService).should(never()).notify(any());
then(xxxRepository).shouldHaveNoInteractions();
```

`Mockito.verify()` は使わず、必ず `BDDMockito.then()` を使う。

---

## テストメソッド命名規則

```
メソッド名_シナリオ_期待結果

例:
process_validPayload_savesData
process_temperatureExceedsMax_throwsAnomalyException
collect_deviceNotFound_throwsNotFoundException
aggregate_emptyInput_returnsEmptyList
```

`@DisplayName` は日本語で「何をするか・何が起きるか」を記述する:

```java
@Test
@DisplayName("温度が閾値を超えた場合は AnomalyException をスローすること")
void process_temperatureExceedsMax_throwsAnomalyException() { ... }
```

---

## ArgumentCaptor（保存オブジェクトの検証）

保存されたオブジェクトのフィールドを検証したい場合に使う:

```java
ArgumentCaptor<IoTData> captor = ArgumentCaptor.forClass(IoTData.class);
then(xxxRepository).should().save(captor.capture());
IoTData saved = captor.getValue();
assertThat(saved.getDeviceId()).isEqualTo(DEVICE_ID);
assertThat(saved.getValue()).isEqualByComparingTo("38.2");
```

---

## 例外テスト

```java
assertThatThrownBy(() -> processor.process(invalidPayload))
        .isInstanceOf(AnomalyException.class)
        .hasMessageContaining("temperature");
```

---

## エンティティのモック（lenient）

フィールドが複数あり、テストによって参照するフィールドが変わる場合:

```java
IoTData data = mock(IoTData.class);
lenient().when(data.getDeviceId()).thenReturn(DEVICE_ID);
lenient().when(data.getMetricType()).thenReturn("temperature");
lenient().when(data.getValue()).thenReturn(new BigDecimal("38.2"));
```

`lenient()` を使う理由: 使われなかったスタブが `UnnecessaryStubbingException` になるのを防ぐため。

---

## よく使う static import

```java
// JUnit 5
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

// Mockito BDD
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;

// AssertJ
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
```
