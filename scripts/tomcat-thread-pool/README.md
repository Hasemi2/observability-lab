# Tomcat Thread Pool 테스트 스크립트

로컬에서 실행 중인 애플리케이션을 대상으로 Tomcat 작업 스레드 고갈을 재현하는 스크립트이다.

## 사전 조건

- 애플리케이션이 `http://localhost:8080`에서 실행 중이어야 한다.
- `server.tomcat.threads.max`가 `10`으로 설정되어 있어야 한다.
- `GET /slow?seconds=N`, `GET /normal`, Actuator metrics endpoint가 구현되어 있어야 한다.

## 사용 중인 스레드 수 관찰

느린 요청 9개를 동시에 실행하고, 남은 작업 스레드 하나로 Actuator를 조회한다. 예상되는 `tomcat.threads.busy` 값은 `10`이다.

```powershell
.\scripts\tomcat-thread-pool\observe-busy-threads.ps1
```

선택 파라미터:

```powershell
.\scripts\tomcat-thread-pool\observe-busy-threads.ps1 `
    -BaseUrl "http://localhost:8080" `
    -SlowSeconds 10 `
    -ConcurrentRequests 9
```

## `/normal` 요청에 미치는 영향 측정

느린 요청 10개를 동시에 실행하여 설정된 작업 스레드 풀을 고갈시킨다. 그다음 `/normal` 요청이 작업 스레드를 할당받을 때까지 기다리는 시간을 측정한다.

```powershell
.\scripts\tomcat-thread-pool\test-normal-delay.ps1
```

선택 파라미터:

```powershell
.\scripts\tomcat-thread-pool\test-normal-delay.ps1 `
    -BaseUrl "http://localhost:8080" `
    -SlowSeconds 10 `
    -ConcurrentRequests 10
```

작업 스레드가 10개인 상태에서 느린 요청 10개를 동시에 실행하면 `/normal`은 약 `SlowSeconds - 1`초 동안 기다리게 된다. 1초의 차이는 느린 요청을 시작한 후 `/normal`을 호출하기 전까지 대기하는 시간 때문에 발생한다.

Tomcat의 최대 작업 스레드 수를 변경했다면 사용 중인 스레드 관찰 테스트에는 최대값보다 하나 적은 요청을 사용한다. `/normal` 지연 테스트에는 최대 작업 스레드 수와 동일한 개수의 요청을 사용한다.
