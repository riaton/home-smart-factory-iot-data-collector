# 実装ガイド (Implementation Guide)

## Java / Spring Boot 規約

### 命名規則

**クラス・インターフェース・Enum**:
```java
// クラス: PascalCase、名詞
public class IotDataService { }
public class AnomalyLogRepository { }

// インターフェース: PascalCase（I プレフィックス不要）
public interface DeviceRepository { }
public interface ReportGenerator { }

// Enum: PascalCase、値は UPPER_SNAKE_CASE
public enum AnomalyType {
    TEMPERATURE_HIGH,
    HUMIDITY_LOW,
    POWER_EXCEEDED
}
```

**メソッド・変数**:
```java
// メソッド: camelCase、動詞で始める
public IotData findLatestByDeviceId(String deviceId) { }
public void processIncomingMessage(SqsMessage message) { }
public boolean isThresholdExceeded(double value) { }

// 変数: camelCase、名詞
String deviceId = "device-001";
List<IotData> iotDataList = new ArrayList<>();
boolean isAnomaly = false;
```

**定数**:
```java
// UPPER_SNAKE_CASE
private static final int MAX_RETRY_COUNT = 3;
private static final long RETRY_DELAY_MS = 1000L;
private static final String DEFAULT_REGION = "ap-northeast-1";
```

**パッケージ・ファイル名**:
```
// パッケージ: すべて小文字、ドット区切り
com.example.smartfactory.domain.iotdata
com.example.smartfactory.infrastructure.repository
com.example.smartfactory.application.service
com.example.smartfactory.presentation.controller

// ファイル名: クラス名と一致（PascalCase）
IotDataService.java
AnomalyLogController.java
DeviceRepository.java
```

---

### レイヤー構成と責務

```
presentation/   ← @RestController（HTTPの受け口のみ）
application/    ← @Service（ユースケース・ビジネスロジック）
domain/         ← Entity、VO、Repository インターフェース
infrastructure/ ← @Repository 実装、外部サービス連携
```

**Controller（presentation 層）**:
```java
// ✅ 良い例: HTTP の受け口に徹する
@RestController
@RequestMapping("/api/iot-data")
@RequiredArgsConstructor
public class IotDataController {

    private final IotDataService iotDataService;

    @GetMapping
    public ResponseEntity<List<IotDataResponse>> getIotData(
            @AuthenticationPrincipal UserDetails user,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(iotDataService.findByDateRange(user.getUsername(), from, to));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        iotDataService.delete(id);
        return ResponseEntity.noContent().build();
    }
}

// ❌ 悪い例: ビジネスロジックをコントローラに書く
@GetMapping
public ResponseEntity<?> getIotData() {
    List<IotData> list = repository.findAll();
    list = list.stream()
        .filter(d -> d.getRecordedAt().isAfter(LocalDateTime.now().minusDays(7)))
        .collect(Collectors.toList());
    return ResponseEntity.ok(list);
}
```

**Service（application 層）**:
```java
// ✅ 良い例: ユースケース単位でメソッドを分ける
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IotDataService {

    private final IotDataRepository iotDataRepository;
    private final AnomalyDetector anomalyDetector;

    public List<IotDataResponse> findByDateRange(String userId, LocalDate from, LocalDate to) {
        return iotDataRepository.findByUserIdAndDateRange(userId, from, to)
                .stream()
                .map(IotDataResponse::from)
                .toList();
    }

    @Transactional
    public void saveFromSqs(IotMessagePayload payload) {
        IotData data = IotData.from(payload);
        iotDataRepository.save(data);
        anomalyDetector.check(data);
    }
}
```

**Repository（infrastructure 層）**:
```java
// Spring Data JPA を使用
@Repository
public interface IotDataRepository extends JpaRepository<IotData, Long> {

    @Query("SELECT d FROM IotData d WHERE d.userId = :userId " +
           "AND DATE(d.recordedAt) BETWEEN :from AND :to " +
           "ORDER BY d.recordedAt DESC")
    List<IotData> findByUserIdAndDateRange(
            @Param("userId") String userId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
```

---

### 入力検証（Bean Validation）

```java
// リクエストDTO にアノテーションを付与
public record CreateDeviceRequest(
        @NotBlank(message = "デバイス名は必須です")
        @Size(max = 100, message = "デバイス名は100文字以内で入力してください")
        String name,

        @NotBlank(message = "デバイスIDは必須です")
        @Pattern(regexp = "^[a-zA-Z0-9\\-]+$", message = "デバイスIDは英数字とハイフンのみ使用可能です")
        String deviceId
) {}

// Controller で @Valid を付与
@PostMapping("/api/devices")
public ResponseEntity<DeviceResponse> create(
        @Valid @RequestBody CreateDeviceRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(deviceService.create(request));
}
```

---

### 例外処理

**カスタム例外クラス**:
```java
// 404 系
public class DeviceNotFoundException extends RuntimeException {
    public DeviceNotFoundException(String deviceId) {
        super("デバイスが見つかりません: " + deviceId);
    }
}

// 400 系
public class ValidationException extends RuntimeException {
    private final String field;
    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }
    public String getField() { return field; }
}

// 409 系
public class DuplicateDeviceException extends RuntimeException {
    public DuplicateDeviceException(String deviceId) {
        super("デバイスIDが既に存在します: " + deviceId);
    }
}
```

**グローバル例外ハンドラ**:
```java
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(DeviceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DeviceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(404, ex.getMessage()));
    }

    @ExceptionHandler(DuplicateDeviceException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DuplicateDeviceException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(409, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(400, message));
    }
}

// エラーレスポンス DTO
public record ErrorResponse(int status, String message) {}
```

---

### DI（依存性注入）

```java
// ✅ 良い例: コンストラクタインジェクション（@RequiredArgsConstructor 活用）
@Service
@RequiredArgsConstructor
public class AnomalyDetector {
    private final AnomalyThresholdRepository thresholdRepository;
    private final SnsNotifier snsNotifier;
}

// ❌ 悪い例: フィールドインジェクション（テスト困難、循環依存検出しにくい）
@Service
public class AnomalyDetector {
    @Autowired
    private AnomalyThresholdRepository thresholdRepository;
}
```

---

### セキュリティ

**機密情報の管理**:
```java
// ✅ 良い例: @ConfigurationProperties で環境変数から読み込む
@ConfigurationProperties(prefix = "aws")
public record AwsProperties(
        String region,
        String sqsQueueUrl,
        String snsTopicArn
) {}

// application.yml
// aws:
//   region: ${AWS_REGION}
//   sqs-queue-url: ${SQS_QUEUE_URL}
//   sns-topic-arn: ${SNS_TOPIC_ARN}

// ❌ 悪い例: ハードコード
private static final String SNS_TOPIC_ARN = "arn:aws:sns:ap-northeast-1:123456789:alert";
```

**SQLインジェクション対策**:
```java
// ✅ 良い例: Spring Data JPA / パラメータバインディング
@Query("SELECT d FROM IotData d WHERE d.deviceId = :deviceId")
List<IotData> findByDeviceId(@Param("deviceId") String deviceId);

// ❌ 悪い例: 文字列連結でクエリ構築
String sql = "SELECT * FROM iot_data WHERE device_id = '" + deviceId + "'";
```

---

### 非同期・並列処理

```java
// @Async でバックグラウンド処理
@Async
public CompletableFuture<Void> sendAnomalyNotification(AnomalyLog log) {
    snsClient.publish(buildPublishRequest(log));
    return CompletableFuture.completedFuture(null);
}

// 並列処理
public List<DailyReport> generateReportsInParallel(List<String> deviceIds) {
    return deviceIds.parallelStream()
            .map(this::generateReport)
            .toList();
}
```

---

## コメント規約

### Javadoc

```java
/**
 * SQSメッセージからIoTデータを保存し、異常検知を実行する。
 *
 * @param payload SQSから受信したIoTメッセージペイロード
 * @throws DeviceNotFoundException device_id がDBに存在しない場合
 */
@Transactional
public void saveFromSqs(IotMessagePayload payload) {
    // 実装
}
```

### インラインコメント

```java
// ✅ 理由・制約を説明する
// QoS=1 の再送対策: (device_id, recorded_at) の UNIQUE 制約で冪等性を確保
iotDataRepository.saveWithOnConflictDoNothing(data);

// ✅ 非自明なロジックを説明する
// power_w の閾値は上限のみ有効。min_value は無視する（設計上の制約）
if (threshold.getMaxValue() != null && value > threshold.getMaxValue()) {
    flagAnomaly(data);
}

// ❌ コードの内容をそのまま書くだけ
// リストを返す
return list;
```

---

## パフォーマンス

```java
// ✅ N+1 問題を避ける: fetch join を使用
@Query("SELECT d FROM Device d JOIN FETCH d.thresholds WHERE d.userId = :userId")
List<Device> findByUserIdWithThresholds(@Param("userId") String userId);

// ✅ 大量データは Pageable で分割取得
Page<IotData> findByDeviceId(String deviceId, Pageable pageable);

// ✅ 読み取り専用トランザクション（Hibernate の最適化が効く）
@Transactional(readOnly = true)
public List<IotDataResponse> findRecent(String deviceId) { }
```

---

## テストコード

### 単体テスト（Service）

```java
// Mockito を使ったサービス層の単体テスト
class IotDataServiceTest {

    @Mock
    private IotDataRepository iotDataRepository;

    @Mock
    private AnomalyDetector anomalyDetector;

    @InjectMocks
    private IotDataService iotDataService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("正常なペイロードでIoTデータを保存できる")
    void saveFromSqs_validPayload_savesData() {
        // Given
        IotMessagePayload payload = new IotMessagePayload("device-001", 25.5, 60.0, null, Instant.now());
        IotData expected = IotData.from(payload);
        given(iotDataRepository.save(any())).willReturn(expected);

        // When
        iotDataService.saveFromSqs(payload);

        // Then
        then(iotDataRepository).should().save(any(IotData.class));
        then(anomalyDetector).should().check(any(IotData.class));
    }

    @Test
    @DisplayName("存在しないdeviceIdの場合DeviceNotFoundExceptionをスローする")
    void saveFromSqs_unknownDeviceId_throwsException() {
        // Given
        IotMessagePayload payload = new IotMessagePayload("unknown-device", 25.5, 60.0, null, Instant.now());
        given(iotDataRepository.save(any())).willThrow(new DeviceNotFoundException("unknown-device"));

        // When / Then
        assertThatThrownBy(() -> iotDataService.saveFromSqs(payload))
                .isInstanceOf(DeviceNotFoundException.class)
                .hasMessageContaining("unknown-device");
    }
}
```

### 結合テスト（Controller）

```java
// @WebMvcTest を使ったコントローラ層のスライステスト（Spring Boot 3.5）
@WebMvcTest(IotDataController.class)
class IotDataControllerTest {

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private IotDataService iotDataService;

    @Test
    @DisplayName("GET /api/iot-data は200と一覧を返す")
    void getIotData_authenticated_returns200() {
        given(iotDataService.findByDateRange(any(), any(), any()))
                .willReturn(List.of(new IotDataResponse(/* ... */)));

        assertThat(mvc.get().uri("/api/iot-data")
                .param("from", "2025-01-01")
                .param("to", "2025-01-31"))
                .hasStatusOk();
    }

    @Test
    @DisplayName("バリデーションエラーは400を返す")
    void createDevice_blankName_returns400() {
        String body = """
                {"name": "", "deviceId": "dev-001"}
                """;
        assertThat(mvc.post().uri("/api/devices")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
```

---

## チェックリスト

実装完了前に確認:

### コード品質
- [ ] 命名が Java 規約に従っている（PascalCase / camelCase / UPPER_SNAKE_CASE）
- [ ] レイヤーの責務が分離されている（Controller / Service / Repository）
- [ ] メソッドが単一責務を持っている
- [ ] マジックナンバーが定数化されている
- [ ] コンストラクタインジェクションを使用している

### セキュリティ
- [ ] Bean Validation（@Valid）で入力検証されている
- [ ] 機密情報が環境変数経由で注入されている
- [ ] SQL は JPA / パラメータバインディングを使用している
- [ ] 認証必須エンドポイントに @AuthenticationPrincipal が適用されている

### パフォーマンス
- [ ] 読み取り専用メソッドに @Transactional(readOnly = true) が付いている
- [ ] N+1 クエリが発生していない（fetch join または @EntityGraph）
- [ ] 大量データ取得に Pageable を使用している

### テスト
- [ ] Service 層に Mockito 単体テストがある
- [ ] Controller 層に @WebMvcTest スライステストがある
- [ ] 正常系・異常系・エッジケースがカバーされている
- [ ] @DisplayName で日本語テスト名が付いている

### ドキュメント
- [ ] public メソッドに Javadoc がある（非自明な場合）
- [ ] 非自明なロジックにインラインコメントがある
- [ ] TODO / FIXME が記載されている（該当する場合）

### ツール
- [ ] `./gradlew build` が成功する
- [ ] CheckStyle / SpotBugs のエラーがない
- [ ] フォーマット（google-java-format 等）が統一されている
