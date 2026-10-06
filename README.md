# Observability Lab

Spring Boot 애플리케이션에서 발생할 수 있는 성능 문제를 직접 재현하고, Actuator·Prometheus·Grafana를 이용해 원인과 영향을 관찰하는 학습용 프로젝트입니다.

단순히 장애 API를 만드는 것이 아니라 다음 흐름을 반복해서 익히는 것을 목표로 합니다.

```text
실험 조건 설정 → 부하 발생 → 메트릭 수집 → Grafana 시각화 → 원인 분석
```

## 현재 구현된 실험

| 시나리오 | 엔드포인트 | 관찰 대상 | 상태 |
|---|---|---|---|
| 정상 요청 | `GET /normal` | 기준 응답시간과 요청량 | 구현 완료 |
| 느린 요청 | `GET /slow?seconds=10` | Tomcat 작업 스레드 점유와 응답 지연 | 구현 완료 |
| DB 커넥션 점유 | `GET /db/hold?seconds=5` | HikariCP Active, Idle, Pending | 구현 완료 |
| DB 행 잠금 | `POST /db/lock/hold?seconds=10` | 트랜잭션 대기와 잠금 전파 | 구현 완료 |
| Heap 메모리 보유 | `POST /memory/allocate?mebibytes=10` | Heap 사용량과 장수 객체 | 구현 완료 |
| Heap 메모리 해제 | `DELETE /memory` | 참조 해제 후 GC에 따른 Heap 변화 | 구현 완료 |
| GC Churn | `POST /gc/churn` | 객체 할당률, Young GC, GC Pause | 구현 완료 |

다음 시나리오는 향후 학습 대상으로 남겨두었습니다.

- Slow Query
- 외부 API 응답 지연과 Timeout
- CPU 부하
- Thread `BLOCKED` / `WAITING`
- 메모리 누수 및 Full GC 비교

## 기술 구성

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- H2 Database
- HikariCP
- Spring Boot Actuator
- Micrometer Prometheus Registry
- Prometheus
- Grafana
- Docker Compose

## 관측 구조

```text
사용자 또는 부하 스크립트
          │ HTTP 요청
          ▼
Spring Boot 애플리케이션 :8080
          │ /actuator/prometheus
          ▼
Prometheus :9090
          │ PromQL
          ▼
Grafana :3000
```

Spring Boot Actuator와 Micrometer가 애플리케이션 메트릭을 제공합니다. Prometheus가 1초 간격으로 메트릭을 수집하고, Grafana가 이를 대시보드로 시각화합니다.

## 프로젝트 구조

```text
observability-lab/
├─ src/main/java/com/practice/observability_lab/
│  ├─ NormalController.java
│  ├─ SlowController.java
│  ├─ connectionpool/
│  │  ├─ ConnectionPoolController.java
│  │  └─ ConnectionPoolService.java
│  ├─ dblock/
│  │  ├─ DbLockController.java
│  │  ├─ DbLockService.java
│  │  └─ DbLockInitializer.java
│  ├─ memory/
│  │  ├─ MemoryController.java
│  │  ├─ MemoryService.java
│  │  ├─ MemoryProperties.java
│  │  ├─ dto/
│  │  └─ exception/
│  └─ gc/
│     ├─ GcController.java
│     ├─ GcService.java
│     ├─ GcProperties.java
│     ├─ dto/
│     └─ exception/
├─ src/main/resources/
│  ├─ application.yaml
│  ├─ application-thread-pool.yaml
│  ├─ application-connection-pool.yaml
│  ├─ application-db-lock.yaml
│  ├─ application-memory.yaml
│  └─ application-gc.yaml
├─ src/test/java/com/practice/observability_lab/
│  ├─ memory/MemoryServiceTests.java
│  └─ gc/GcServiceTests.java
├─ scripts/tomcat-thread-pool/
│  ├─ observe-busy-threads.ps1
│  └─ test-normal-delay.ps1
└─ monitoring/
   ├─ docker-compose.yaml
   ├─ prometheus/prometheus.yml
   └─ grafana/
      ├─ dashboards/observability-lab.json
      └─ provisioning/
```

## 애플리케이션 실행

### 기본 실행

PowerShell에서 다음 명령을 실행합니다.

```powershell
.\gradlew.bat bootRun
```

기본 주소는 `http://localhost:8080`입니다.

```powershell
Invoke-RestMethod "http://localhost:8080/normal"
Invoke-RestMethod "http://localhost:8080/actuator/health"
```


### 프로파일 실행

실험별 설정은 Spring Profile로 분리되어 있습니다.

```powershell
# Tomcat 최대 작업 스레드 10개
.\gradlew.bat bootRun --args="--spring.profiles.active=thread-pool"

# HikariCP 최대 커넥션 5개
.\gradlew.bat bootRun --args="--spring.profiles.active=connection-pool"

# H2 행 잠금 실험
.\gradlew.bat bootRun --args="--spring.profiles.active=db-lock"

# Heap 보유 실험
.\gradlew.bat bootRun --args="--spring.profiles.active=memory"

# GC Churn 실험
.\gradlew.bat bootRun --args="--spring.profiles.active=gc"

# 여러 실험을 함께 활성화
.\gradlew.bat bootRun --args="--spring.profiles.active=connection-pool,db-lock,memory,gc"
```

GC 실험에서 Heap 크기와 수집기를 명시하려면 실행 전에 다음 환경변수를 지정합니다.

```powershell
$env:JAVA_TOOL_OPTIONS = "-Xms256m -Xmx256m -XX:+UseG1GC -Xlog:gc*"
.\gradlew.bat bootRun --args="--spring.profiles.active=memory,gc"
```

실험을 마친 뒤 현재 PowerShell 세션의 JVM 옵션을 제거할 수 있습니다.

```powershell
Remove-Item Env:JAVA_TOOL_OPTIONS
```

## Prometheus와 Grafana 실행

Docker Engine이 실행 중인 WSL에서 프로젝트의 `monitoring` 디렉터리로 이동합니다.

```bash
cd /mnt/d/personal/observability-lab/monitoring
docker compose up -d
docker compose ps
```

접속 주소:

| 서비스 | 주소 | 계정 |
|---|---|---|
| Spring Boot | `http://localhost:8080` | - |
| Actuator Prometheus | `http://localhost:8080/actuator/prometheus` | - |
| Prometheus | `http://localhost:9090` | - |
| Grafana | `http://localhost:3000` | `admin` / `admin` |

Grafana의 Prometheus 데이터소스와 `Observability Lab` 대시보드는 시작할 때 자동으로 프로비저닝됩니다.

### Prometheus Target 확인

Prometheus의 **Status → Target health**에서 `observability-lab`이 `UP`인지 확인합니다.

현재 `monitoring/prometheus/prometheus.yml`에는 WSL2에서 Windows 호스트로 접근하기 위한 IP가 설정되어 있습니다.

```yaml
targets:
  - 172.23.32.1:8080
```

WSL 재시작 후 네트워크 주소가 바뀌어 Target이 `DOWN`이 되면 다음 명령으로 Windows 호스트 주소를 확인합니다.

```bash
ip route show default
```

출력된 기본 게이트웨이 IP로 `prometheus.yml`의 Target을 수정한 다음 Prometheus를 재시작합니다.

```bash
docker compose restart prometheus
```

### 컨테이너 종료와 데이터 보존

```bash
docker compose down
```

위 명령은 컨테이너만 제거하고 named volume은 유지합니다. 다음 명령은 Prometheus와 Grafana의 저장 데이터까지 삭제하므로 주의해야 합니다.

```bash
docker compose down -v
```

대시보드 정의는 `monitoring/grafana/dashboards/observability-lab.json`으로 관리되므로 볼륨이 삭제되어도 프로비저닝을 통해 다시 생성할 수 있습니다.

## 실험 1: Tomcat Thread Pool 고갈

`thread-pool` 프로파일은 다음 설정을 사용합니다.

```yaml
server:
  tomcat:
    threads:
      max: 10
      min-spare: 2
    max-connections: 100
    accept-count: 20
```

느린 요청 하나는 지정한 시간 동안 Tomcat 작업 스레드 하나를 점유합니다.

```powershell
Invoke-RestMethod "http://localhost:8080/slow?seconds=10"
```

준비된 스크립트로 사용 중인 스레드를 관찰할 수 있습니다.

```powershell
.\scripts\tomcat-thread-pool\observe-busy-threads.ps1
.\scripts\tomcat-thread-pool\test-normal-delay.ps1
```

주요 메트릭:

```promql
tomcat_threads_busy_threads
tomcat_threads_current_threads
tomcat_threads_config_max_threads
```

학습 포인트:

- 느린 작업이 Tomcat 스레드를 계속 점유한다.
- 최대 스레드가 모두 사용되면 정상 요청도 대기한다.
- 애플리케이션 내부 작업 하나의 지연이 전체 HTTP 응답 지연으로 전파될 수 있다.

## 실험 2: HikariCP Connection Pool 고갈

`connection-pool` 프로파일은 최대 커넥션을 5개로 제한합니다.

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
      connection-timeout: 3000
```

`/db/hold`는 실제 DB 커넥션을 획득한 다음 트랜잭션 안에서 지정한 시간 동안 유지합니다.

```powershell
Invoke-RestMethod "http://localhost:8080/db/hold?seconds=10"
```

동시 요청으로 커넥션 풀을 점유합니다.

```powershell
1..10 | ForEach-Object {
    Start-Job {
        Invoke-RestMethod "http://localhost:8080/db/hold?seconds=10"
    }
}
```

주요 메트릭:

```promql
hikaricp_connections_active
hikaricp_connections_idle
hikaricp_connections_pending
hikaricp_connections_max
```

학습 포인트:

- `Active`가 최대치에 도달하면 이후 요청은 `Pending` 상태가 된다.
- `connection-timeout` 안에 커넥션을 얻지 못하면 요청이 실패한다.
- Tomcat 스레드가 남아 있어도 DB 커넥션이 부족하면 서비스는 지연될 수 있다.

## 실험 3: DB Lock 및 트랜잭션 대기

`db-lock` 프로파일은 H2 인메모리 데이터베이스에 실험용 행 하나를 생성합니다. 잠금 획득 제한시간은 15초이며, 잠금 유지 API는 최대 30초까지 허용합니다.

첫 번째 PowerShell에서 행 잠금을 10초 동안 유지합니다.

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/db/lock/hold?seconds=10"
```

첫 번째 요청이 실행 중일 때 두 번째 PowerShell에서 같은 행을 수정합니다.

```powershell
Measure-Command {
    Invoke-RestMethod -Method Post `
      -Uri "http://localhost:8080/db/lock/update"
}
```

두 번째 요청은 첫 번째 트랜잭션이 커밋되고 행 잠금을 반환할 때까지 기다립니다. 현재 값과 애플리케이션이 측정한 보유·대기 요청 수는 다음 API로 확인합니다.

```powershell
Invoke-RestMethod "http://localhost:8080/db/lock/status"
```

사용자 정의 메트릭:

```promql
lab_db_lock_holders
lab_db_lock_waiting
lab_db_lock_acquire_seconds_count
lab_db_lock_acquire_seconds_sum
```

업데이트 요청의 평균 잠금 획득 대기시간:

```promql
rate(lab_db_lock_acquire_seconds_sum{operation="update"}[5m])
/
rate(lab_db_lock_acquire_seconds_count{operation="update"}[5m])
```

학습 포인트:

- 트랜잭션이 끝나기 전까지 행 잠금은 반환되지 않는다.
- SQL 실행 자체가 단순해도 Lock 획득 대기 때문에 응답이 느려질 수 있다.
- 대기 요청도 Tomcat 스레드와 HikariCP 커넥션을 점유한다.
- 잠금 대기가 누적되면 DB 문제가 커넥션 풀과 HTTP 계층으로 전파될 수 있다.

## 실험 4: Heap 메모리 보유

메모리 API는 `memory` 프로파일에서만 활성화됩니다.

```powershell
# 10MiB를 할당하고 참조를 유지
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/memory/allocate?mebibytes=10"

# 실험 상태와 JVM Heap 상태 확인
Invoke-RestMethod "http://localhost:8080/memory/status"

# 유지 중인 참조 해제
Invoke-RestMethod -Method Delete "http://localhost:8080/memory"
```

기본 안전 제한:

- 요청 한 번당 최대 10MiB
- 총 보유량 최대 80MiB
- 1MiB 단위 청크 사용

사용자 정의 메트릭:

```promql
lab_memory_retained_bytes
lab_memory_chunks
```

JVM Heap 메트릭:

```promql
sum(jvm_memory_used_bytes{area="heap"})
sum(jvm_memory_max_bytes{area="heap"} > 0)
```

학습 포인트:

- 사용자 정의 보유량은 실험 코드가 참조 중인 배열만 의미한다.
- JVM Heap 사용량은 Spring 객체 등 모든 Heap 객체를 포함한다.
- 참조를 해제해도 실제 Heap 사용량은 GC가 실행된 후 감소한다.
- 최대 Heap은 JVM의 상한이므로 실행 중 일반적으로 변하지 않는다.

## 실험 5: GC Churn

GC Churn API는 `gc` 프로파일에서만 활성화됩니다.

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/gc/churn?totalMebibytes=500&chunkKibibytes=256"
```

동작 방식:

1. 256KiB 크기의 임시 배열을 반복 생성합니다.
2. 누적 생성량이 500MiB에 도달할 때까지 반복합니다.
3. 이전 배열의 참조를 유지하지 않아 대부분 Young GC 대상이 됩니다.
4. 동시에 두 실험이 실행되지 않도록 중복 요청을 차단합니다.

`500MiB 할당`은 500MiB를 동시에 보유한다는 의미가 아닙니다. 작은 객체를 생성하고 버리는 동작의 누적량입니다.

사용자 정의 메트릭:

```promql
lab_gc_churn_executions_total
lab_gc_churn_allocated_bytes_total
lab_gc_churn_duration_seconds_count
lab_gc_churn_duration_seconds_sum
```

모니터링 Grafana 쿼리:

```promql
# JVM 전체 객체 할당 속도(MiB/s)
rate(jvm_gc_memory_allocated_bytes_total[1m]) / 1024 / 1024

# 최근 1분 동안 발생한 GC 횟수
sum by (action, cause) (
  increase(jvm_gc_pause_seconds_count[1m])
)

# 최근 1분 동안 GC로 정지한 총 시간
sum by (action, cause) (
  increase(jvm_gc_pause_seconds_sum[1m])
)

# Heap 사용률
100 *
sum(jvm_memory_used_bytes{area="heap"})
/
sum(jvm_memory_max_bytes{area="heap"} > 0)
```

학습 포인트:

- 짧은 수명의 객체가 많으면 Young GC가 증가할 수 있다.
- 누적 할당량과 동시에 살아 있는 Heap 사용량은 서로 다르다.
- GC 발생 자체보다 GC 빈도, Pause 시간, HTTP 응답 지연을 함께 봐야 한다.
- GC 후 Heap이 다시 내려오면 단명 객체 Churn에 가깝고, 최저점이 계속 상승하면 장수 객체나 메모리 누수를 의심할 수 있다.

## 주요 HTTP 메트릭

URI별 초당 요청 수:

```promql
sum by (uri) (
  rate(http_server_request_duration_seconds_count[1m])
)
```

프로젝트 환경에서 메트릭 이름이 `http_server_requests_seconds_*`로 노출된다면 동일한 쿼리에서 이름만 바꿉니다. 실제 이름은 Prometheus에서 다음과 같이 확인할 수 있습니다.

```promql
{__name__=~"http_server_.*"}
```

URI별 평균 응답시간:

```promql
sum by (uri) (
  rate(http_server_request_duration_seconds_sum[1m])
)
/
sum by (uri) (
  rate(http_server_request_duration_seconds_count[1m])
)
```

HTTP p95 응답시간:

```promql
histogram_quantile(
  0.95,
  sum by (le) (
    rate(http_server_request_duration_seconds_bucket[5m])
  )
)
```

## 테스트

```powershell
.\gradlew.bat test
```

현재 테스트는 DB 행 잠금 대기, 메모리 할당 제한과 상태 변경, GC Churn 기본값과 요청 제한, 사용자 정의 메트릭 기록을 검증합니다.
