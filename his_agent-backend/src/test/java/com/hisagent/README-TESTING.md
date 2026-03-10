# 后端测试规范

## 测试框架

- **单元测试**: JUnit 5 + Mockito
- **断言库**: AssertJ (流式断言)
- **集成测试**: TestContainers + Spring Boot Test
- **测试覆盖率**: Jacoco (要求 ≥ 80%)

## 测试分类

### 1. 单元测试 (`*Test.java`)

测试单个类或方法，使用 Mock 隔离依赖。

**示例**:
```java
@ExtendWith(MockitoExtension.class)
class SoapNoteGenerationServiceTest {
    
    @Mock
    private ChatClient chatClient;
    
    @InjectMocks
    private SoapNoteGenerationService service;
    
    @Test
    void shouldGenerateSoapNote() {
        // Given
        String transcript = "患者主诉头痛...";
        when(chatClient.call(anyString())).thenReturn(mockResponse);
        
        // When
        SOAPNote result = service.generate(transcript);
        
        // Then
        assertThat(result).isNotNull();
        assertThat(result.getSubjective()).isNotNull();
    }
}
```

### 2. 集成测试 (`*IntegrationTest.java`)

测试多个组件协作，使用 TestContainers 启动真实数据库。

**示例**:
```java
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class ConsultationIntegrationTest {
    
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8")
        .withDatabaseName("testdb")
        .withUsername("test")
        .withPassword("test");
    
    @DynamicPropertySource
    static void configureTestProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void shouldCreateConsultation() throws Exception {
        mockMvc.perform(post("/api/v1/consultations")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"patientId\":\"P001\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.patientId").value("P001"));
    }
}
```

## 测试执行流程

### 串行执行要求

**重要**: 每个任务必须等待上一个任务测试通过后才能执行。

```bash
# 1. 运行单元测试
mvn clean test

# 2. 检查测试覆盖率
mvn jacoco:report

# 3. 运行集成测试
mvn verify

# 4. 查看覆盖率报告
open target/jacoco-report/index.html
```

### 任务执行检查清单

在执行下一个任务前，必须确认：

- [ ] 当前任务所有单元测试通过
- [ ] 当前任务所有集成测试通过
- [ ] 测试覆盖率 ≥ 80%
- [ ] 无编译警告
- [ ] 代码已格式化

### CI/CD 集成

```yaml
# GitHub Actions 示例
jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      
      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
      
      - name: Run Unit Tests
        run: mvn clean test
      
      - name: Check Test Coverage
        run: mvn jacoco:check
      
      - name: Run Integration Tests
        run: mvn verify
      
      - name: Upload Coverage Report
        uses: codecov/codecov-action@v4
        with:
          files: target/jacoco-report/jacoco.xml
```

## 测试最佳实践

### 1. 测试命名规范

```java
// 方法名应该描述测试场景和预期结果
@Test
void shouldThrowException_WhenPatientIdIsNull() { }

@Test
void shouldGenerateSoapNote_WhenTranscriptIsValid() { }

@Test
void shouldReturnEmptyList_WhenNoConsultationsFound() { }
```

### 2. AAA 模式 (Arrange-Act-Assert)

```java
@Test
void shouldCalculateDiagnosisScore() {
    // Arrange (准备)
    DiagnosisSuggestion suggestion = new DiagnosisSuggestion();
    suggestion.setLikelihood("high");
    
    // Act (执行)
    int score = calculator.calculate(suggestion);
    
    // Assert (断言)
    assertThat(score).isGreaterThan(80);
}
```

### 3. 使用 AssertJ 流式断言

```java
// 推荐：流式断言，可读性好
assertResult(result)
    .isNotNull()
    .extracting("primaryDiagnosis")
    .isNotNull()
    .extracting("condition")
    .isEqualTo("高血压");

// 不推荐：JUnit 原生断言
assertNotNull(result);
assertNotNull(result.getPrimaryDiagnosis());
assertEquals("高血压", result.getPrimaryDiagnosis().getCondition());
```

### 4. Mock 使用规范

```java
// 推荐：只 Mock 外部依赖
@Mock
private ChatClient chatClient;

@Mock
private ConsultationRepository repository;

@InjectMocks
private SoapNoteGenerationService service;

// 不推荐：Mock 被测试类本身
```

## 覆盖率要求

| 模块 | 行覆盖率 | 分支覆盖率 |
|------|----------|------------|
| Service 层 | ≥ 80% | ≥ 80% |
| Controller 层 | ≥ 80% | ≥ 80% |
| Repository 层 | ≥ 70% | ≥ 70% |
| 核心业务逻辑 | ≥ 90% | ≥ 85% |

## 常见问题

### Q: 如何调试失败的测试？

```bash
# 运行单个测试类
mvn test -Dtest=SoapNoteGenerationServiceTest

# 运行单个测试方法
mvn test -Dtest=SoapNoteGenerationServiceTest#shouldGenerateSoapNote

# 调试模式
mvn test -Dmaven.surefire.debug -Dtest=YourTest
```

### Q: 如何跳过测试？

```bash
# 跳过所有测试
mvn clean install -DskipTests

# 跳过单元测试，只运行集成测试
mvn clean install -DskipUnitTests
```

### Q: 测试执行顺序如何控制？

使用 `@TestMethodOrder` 注解：

```java
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrderedTests {
    
    @Test
    @Order(1)
    void shouldExecuteFirst() { }
    
    @Test
    @Order(2)
    void shouldExecuteSecond() { }
}
```

## 参考资料

- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito 文档](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [AssertJ 文档](https://assertj.github.io/doc/)
- [TestContainers 文档](https://www.testcontainers.org/)
