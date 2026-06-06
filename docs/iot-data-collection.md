# シーケンス図: IoTデータ収集

## Home Smart Factory -- IoT設備監視基盤

------------------------------------------------------------------------

# 1. 正常系

```mermaid
sequenceDiagram
    participant RPi as Raspberry Pi<br/>(Java Collector)
    participant IoTCore as AWS IoT Core
    participant SQS as Amazon SQS
    participant Worker as ECS(Worker)
    participant RDS as iot_data (table)

    RPi->>IoTCore: MQTT Publish<br/>topic: home/devices/{device_id}/data<br/>payload: JSON
    IoTCore-->>RPi: PUBACK
    Note over RPi,IoTCore: PUBACKはIoT Coreがメッセージを<br/>確かに受け取ったことを示す（QoS 1）<br/>SQS転送成否はRPiに通知されない

    Note over IoTCore,SQS: IoT Rule により自動転送
    IoTCore->>SQS: SendMessage
    SQS-->>IoTCore: 200 OK

    loop ロングポーリング (WaitTimeSeconds=20)
        Worker->>SQS: ReceiveMessage
        SQS-->>Worker: Message + ReceiptHandle<br/>(VisibilityTimeout=30s)
    end

    Worker->>RDS: INSERT INTO iot_data
    RDS-->>Worker: 成功

    Worker->>SQS: DeleteMessage (ReceiptHandle)
    SQS-->>Worker: 成功
```

------------------------------------------------------------------------

# 2. エラー系

## 2.1 MQTT接続失敗（認証エラー / ネットワーク断）

**発生箇所:** Raspberry Pi → AWS IoT Core

**原因:**
- IoT Core に登録された証明書の失効・不一致
- ネットワーク断

```mermaid
sequenceDiagram
    participant RPi as Raspberry Pi<br/>(Java Collector)
    participant IoTCore as AWS IoT Core

    RPi->>IoTCore: MQTT Connect<br/>(クライアント証明書付き)

    alt 認証エラー
        IoTCore-->>RPi: CONNACK (returnCode=5: 認証拒否)
        Note over RPi: 証明書の問題のため<br/>リトライしても解決しない<br/>→ ローカルにエラーログを出力して停止
    else ネットワーク断
        IoTCore--xRPi: タイムアウト（応答なし）
        Note over RPi: 指数バックオフでリトライ<br/>（1s → 2s → 4s → 最大60s）
        RPi->>IoTCore: MQTT Connect (リトライ)
        IoTCore-->>RPi: CONNACK (returnCode=0: 接続成功)
    end
```

---

## 2.2 IoT Core → SQS 転送失敗

**発生箇所:** AWS IoT Core → Amazon SQS

**原因:**
- SQS 一時障害
- IoT Rule の設定ミス

```mermaid
sequenceDiagram
    participant RPi as Raspberry Pi<br/>(Java Collector)
    participant IoTCore as AWS IoT Core
    participant SQS as Amazon SQS

    RPi->>IoTCore: MQTT Publish
    IoTCore-->>RPi: PUBACK
    Note over RPi: PUBACKはIoT Core受信の確認<br/>SQS転送成否はRPiには通知されない

    IoTCore->>SQS: SendMessage (IoT Rule)

    alt SQS 一時障害
        SQS-->>IoTCore: 500 ServiceUnavailable
        Note over IoTCore: IoT Rule の組み込みリトライで再送<br/>（最大で数回リトライ）
        IoTCore->>SQS: SendMessage (リトライ)
        SQS-->>IoTCore: 200 OK（成功）
    else リトライ上限超過
        Note over IoTCore: メッセージがロストする<br/>（このシステムでは許容する）
    end
```

> **設計メモ:** IoT Core の Error Action に別 SQS（DLQ）を設定することで転送失敗を補足できるが、本システムでは以下の理由によりシンプルな構成とする:
> - IoTデータは1分間隔で送信されるため、1-2件のロストは監視・分析への影響が軽微
> - ロスト率の想定値は月間0.1%未満（AWS IoT Core SLA 99.9%を前提）
> - 異常検知の精度への影響は限定的（閾値判定は個別データポイントで行うため）

---

## 2.3 RDS 書き込み失敗 → SQS リトライ → DLQ

**発生箇所:** ECS Worker → Amazon RDS

**原因:**
- RDS 一時障害 / 接続タイムアウト
- データバリデーションエラー（不正な JSON など）

```mermaid
sequenceDiagram
    participant SQS as Amazon SQS
    participant DLQ as Dead Letter Queue
    participant Worker as ECS(Worker)
    participant RDS as iot_data (table)
    participant CW as CloudWatch
    participant SNS as Amazon SNS
    participant Admin as 管理者

    Worker->>SQS: ReceiveMessage
    SQS-->>Worker: Message (VisibilityTimeout=30s)

    Worker->>RDS: INSERT INTO iot_data

    alt RDS 一時障害
        RDS-->>Worker: 500 エラー / タイムアウト
        Note over Worker: DeleteMessage を呼ばずに処理終了
        Note over SQS: VisibilityTimeout (30s) 経過後<br/>メッセージが再びキューに戻る

        Worker->>SQS: ReceiveMessage (リトライ)
        SQS-->>Worker: Message (ReceiveCount+1)
        Worker->>RDS: INSERT INTO iot_data
        RDS-->>Worker: 成功
        Worker->>SQS: DeleteMessage
        SQS-->>Worker: 成功

    else リトライ上限超過 (maxReceiveCount=3)
        Note over SQS: ReceiveCount が maxReceiveCount を超過
        SQS->>DLQ: メッセージを Dead Letter Queue へ移動
        Note over DLQ: 調査・手動リドライブ用に保管
        SQS->>CW: ApproximateNumberOfMessagesVisible (DLQ) > 0
        Note over CW: Alarm条件成立<br/>DLQ内メッセージ数 ≥ 1
        CW->>SNS: アラート発報（ALARM）
        SNS-->>Admin: メール通知「DLQにメッセージあり」
        Note over Admin: 手動調査 → AWS CLIでリドライブ
    end
```

---

## 2.4 ECS Worker クラッシュ（処理途中）

**発生箇所:** ECS Worker がメッセージ処理中にクラッシュ

**原因:**
- OOM / プロセス異常終了

```mermaid
sequenceDiagram
    participant SQS as Amazon SQS
    participant Worker as ECS(Worker)
    participant RDS as iot_data (table)
    participant ECS as Amazon ECS<br/>（タスク管理）
    participant CW as CloudWatch
    participant SNS as Amazon SNS
    participant Admin as 管理者

    Worker->>SQS: ReceiveMessage
    SQS-->>Worker: Message (VisibilityTimeout=30s)

    Worker->>RDS: INSERT INTO iot_data
    Note over Worker: クラッシュ（OOM等）<br/>DeleteMessage は実行されない

    Note over SQS: VisibilityTimeout (30s) 経過後<br/>メッセージがキューに戻る

    ECS->>CW: RunningTaskCount = 0（1分毎に自動送信）
    Note over CW: Alarm条件成立<br/>RunningTaskCount < 1
    CW->>SNS: アラート発報（ALARM）
    SNS-->>Admin: メール通知「ECS Workerクラッシュ検知」

    ECS->>Worker: タスク再起動（ECSの自動復旧）
    ECS->>CW: RunningTaskCount = 1
    Note over CW: OK状態へ復帰
    CW->>SNS: 復旧通知（OK）
    SNS-->>Admin: 復旧メール

    Worker->>SQS: ReceiveMessage (リトライ)
    SQS-->>Worker: Message (ReceiveCount+1)
    Worker->>RDS: INSERT INTO iot_data
    Note over RDS: UNIQUE(device_id, recorded_at)制約により<br/>INSERT成功済みの場合は重複を弾く
    RDS-->>Worker: 成功 or 重複エラー（無視）
    Worker->>SQS: DeleteMessage
    SQS-->>Worker: 成功
```

---

## 2.5 IoT Core → SQS 連続転送失敗アラート

**発生箇所:** AWS IoT Core → CloudWatch → Amazon SNS

**条件:** 10分以内に転送失敗が3回以上発生した場合

**メカニズム:** IoT Rule の実行失敗は CloudWatch の `RuleExecutionError` メトリクスにカウントされる。単発ロストは許容するが、連続失敗は SQS 障害や IoT Rule 設定ミスの可能性があるため通知する。

```mermaid
sequenceDiagram
    participant IoTCore as AWS IoT Core
    participant CW as CloudWatch
    participant SNS as Amazon SNS
    participant Admin as 管理者

    loop 転送失敗が繰り返し発生（SQS障害等）
        IoTCore->>IoTCore: ルール実行失敗（リトライ上限超過）
        IoTCore->>CW: RuleExecutionError += 1
    end

    Note over CW: Alarm条件成立<br/>RuleExecutionError ≥ 3（直近10分のSum）
    CW->>SNS: アラート発報（ALARM）
    SNS-->>Admin: メール通知「IoT転送失敗多発」
    Note over Admin: SQS障害・IoT Rule設定を確認
```

> **設計メモ:** CloudWatch Alarm の閾値（3回/10分）は運用中に調整する。複数台の RPi が同時にデータ送信するため、単一障害でも複数カウントが積みあがる点に注意。

---

## 2.6 ECS Worker → SQS ReceiveMessage 連続失敗アラート

**発生箇所:** ECS Worker → Amazon SQS

**原因:**
- SQS 一時障害
- ECS ↔ SQS 間のネットワーク断
- IAM 権限エラー

**条件:** 5分以内に ReceiveMessage が5回以上失敗した場合

**メカニズム:** SQS の組み込みメトリクスには ReceiveMessage 失敗数がないため、Worker アプリが catch したエラーをカスタムメトリクスとして CloudWatch に送信する。

```mermaid
sequenceDiagram
    participant SQS as Amazon SQS
    participant Worker as ECS(Worker)
    participant CW as CloudWatch
    participant SNS as Amazon SNS
    participant Admin as 管理者

    loop ReceiveMessage が繰り返し失敗
        Worker->>SQS: ReceiveMessage
        SQS--xWorker: 接続エラー / タイムアウト
        Worker->>CW: カスタムメトリクス送信<br/>SQSReceiveError += 1
    end

    Note over CW: Alarm条件成立<br/>SQSReceiveError ≥ 5（直近5分のSum）
    CW->>SNS: アラート発報（ALARM）
    SNS-->>Admin: メール通知「SQS ReceiveMessage 連続失敗」
    Note over Admin: SQS障害・ネットワーク・IAM権限を確認
```

> **設計メモ:** `SQSReceiveError` は Worker アプリが送信するカスタムメトリクス。ロングポーリングの正常タイムアウト（WaitTimeSeconds=20 の空応答）はエラーに含めない。

------------------------------------------------------------------------

# 3. エラー対応まとめ

> **補足:** RPi は PUBACK（QoS 1）により IoT Core へのメッセージ到達を確認できる。ただし SQS への転送成否は RPi には通知されない。

| エラー箇所 | エラー内容 | 挙動 | データロスト |
|---|---|---|---|
| RPi → IoT Core | 認証エラー | ローカルログ出力・停止 | あり（停止中のデータ） |
| RPi → IoT Core | ネットワーク断 | 指数バックオフでリトライ | なし |
| IoT Core → SQS | SQS一時障害 | 組み込みリトライ | なし（通常） |
| IoT Core → SQS | リトライ上限超過 | メッセージロスト | あり（許容）、連続ロスト時は CloudWatch Alarm で通知 |
| ECS(Worker) → RDS | RDS一時障害 | VisibilityTimeout後にSQSリトライ | なし |
| ECS(Worker) → RDS | リトライ上限超過 (maxReceiveCount=3) | DLQへ移動 | なし（DLQに保管、CloudWatch Alarm で通知） |
| ECS(Worker) クラッシュ | OOM等 | ECS自動復旧 + SQSリトライ | なし（CloudWatch Alarm で通知） |
| ECS(Worker) → SQS | ReceiveMessage 連続失敗 | CloudWatch Alarm 発報 | なし（メッセージはSQSに残存） |
