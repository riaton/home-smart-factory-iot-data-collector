# IoTメッセージフォーマット定義書

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 概要

Raspberry Pi（Java Collector）が AWS IoT Core へ送信する MQTT メッセージの仕様を定義する。

------------------------------------------------------------------------

# 2. MQTTトピック

## 2.1 命名規則

```
home/devices/{device_id}/data
```

| 要素 | 説明 |
|---|---|
| `home` | プロジェクト固定プレフィックス |
| `devices` | デバイス層を示す固定セグメント |
| `{device_id}` | デバイス識別子（`devices` テーブルの `device_id` と一致する文字列） |
| `data` | センサーデータを示す固定セグメント |

**例:**
```
home/devices/room01/data
home/devices/kitchen-sensor/data
```

## 2.2 device_id の制約

| 制約 | 内容 |
|---|---|
| 使用可能文字 | 英数字・ハイフン（`-`）のみ |
| 最大長 | 100文字（`devices.device_id` カラムの定義に準拠） |
| 大文字小文字 | 区別する |

## 2.3 IoT Core 設定

| 項目 | 値 |
|---|---|
| プロトコル | MQTT over TLS（ポート 8883） |
| QoS | 1（PUBACK による到達確認） |
| 認証方式 | X.509クライアント証明書 |

> **補足:** QoS=1 はAt-least-once配信保証のため、ネットワーク障害等で同一メッセージが重複受信される場合がある。Worker側の重複INSERT対策は5.3節参照。

------------------------------------------------------------------------

# 3. ペイロード

## 3.1 JSONスキーマ

```json
{
  "device_id": "room01",
  "temperature": 25.3,
  "humidity": 60.1,
  "motion": 1,
  "power_w": 120.5,
  "recorded_at": "2026-01-15T10:00:00+09:00"
}
```

> **推定ペイロードサイズ:** 最大200バイト程度。AWS IoT Core の1メッセージあたり128KB制限に対して十分な余裕がある。

> **変更点メモ:** 要件定義書 v1.0 では `timestamp` フィールド名を使用していたが、DBカラム名（`iot_data.recorded_at`）に統一した。また要件定義書に含まれていた `user_id` はDB側で `devices` テーブルから解決するため、送信不要とした。

## 3.2 フィールド定義

| フィールド | 型 | NULL許容 | 単位 | 説明 |
|---|---|---|---|---|
| `device_id` | string | 不可 | - | デバイス識別子。`devices.device_id` と一致する値を送信する |
| `temperature` | number | 可（null） | ℃ | 温度。小数第2位まで（例: 25.30）。センサー未搭載時は null |
| `humidity` | number | 可（null） | % | 湿度。小数第2位まで（例: 60.10）。センサー未搭載時は null |
| `motion` | integer（0 or 1） | 可（null） | - | 人感センサー。`0`: 未検知、`1`: 検知。センサー未搭載時は null。boolean（`true`/`false`）は不可 |
| `power_w` | number | 可（null） | W | 消費電力。小数第2位まで（例: 120.50）。センサー未搭載時は null |
| `recorded_at` | string（ISO 8601） | 不可 | - | データ取得時刻。タイムゾーン付き（例: `+09:00`）。DBカラム `recorded_at` と同名 |

## 3.3 送信間隔

| 項目 | 値 |
|---|---|
| 通常時 | 1分（60秒）間隔 |
| リトライ時 | 指数バックオフ（1s → 2s → 4s → 8s → 16s）、最大5回。5回失敗後はそのデータの送信を断念しエラーログを出力する |

------------------------------------------------------------------------

# 4. IoT Rule

## 4.1 SQS転送ルール

| 項目 | 値 |
|---|---|
| ルール名 | `iot_to_sqs_rule` |
| SQL | `SELECT *, topic(3) AS device_id FROM 'home/devices/+/data'` |
| アクション | SQS へ SendMessage（キュー: `iot-data-queue`） |
| エラーアクション | CloudWatch Logs へ記録（`/aws/iotcore/rule-errors`） |

> **補足:** `topic(3)` はトピックの第3セグメント（`{device_id}` 部分）を抽出する。ペイロードの `device_id` と二重に持つが、ペイロード欠損時のフォールバックとして利用する。

------------------------------------------------------------------------

# 5. Worker のメッセージ処理

## 5.1 処理フロー

```
SQS受信 → JSONパース → バリデーション → iot_data INSERT → 閾値チェック → DeleteMessage
```

## 5.2 バリデーション仕様

Worker は SQS から受信したメッセージに対して以下のバリデーションを実施する。

| チェック項目 | 正常時の処理 | エラー時の処理 |
|---|---|---|
| JSONパース | 続行 | エラーログ出力 → DeleteMessage（DLQへ移動しない。不正ペイロードは再処理不可のため） |
| `device_id` 存在確認 | 続行 | エラーログ出力 → DeleteMessage（同上） |
| `recorded_at` フォーマット確認（ISO 8601） | 続行 | エラーログ出力 → DeleteMessage（同上） |
| `device_id` が `devices` テーブルに存在するか | 存在する → INSERT | 存在しない → エラーログ出力 → DeleteMessage（未登録デバイスのデータは破棄） |

> **NOTE:** バリデーションエラーは `maxReceiveCount` 前に DeleteMessage する。不正ペイロードはリトライしても解決しないため、DLQへ移動させず即廃棄する。

## 5.3 DB書き込み仕様

```sql
INSERT INTO iot_data (device_id, user_id, temperature, humidity, motion, power_w, recorded_at)
VALUES ($1, $2, $3, $4, $5, $6, $7)
ON CONFLICT (device_id, recorded_at) DO NOTHING;
```

- `user_id` は `devices` テーブルから `device_id` で引いて取得する
- `ON CONFLICT DO NOTHING` により、Workerクラッシュ後の再処理で重複INSERTを防ぐ

## 5.4 エラーケース一覧

| エラー | 挙動 | SQSメッセージ |
|---|---|---|
| JSONパースエラー | エラーログ出力して終了 | DeleteMessage（即廃棄） |
| `device_id` / `recorded_at` 欠落 | エラーログ出力して終了 | DeleteMessage（即廃棄） |
| 未登録デバイス（devices テーブルに不在） | エラーログ出力して終了 | DeleteMessage（即廃棄） |
| RDS一時障害（INSERT失敗） | エラーログ出力して終了 | DeleteMessage **しない**（VisibilityTimeout後にリトライ） |
| maxReceiveCount 超過（3回） | SQSが自動でDLQへ移動 | DLQ へ移動 |
