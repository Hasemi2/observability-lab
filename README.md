# Observability Lab

## 1. 프로젝트 개요

Observability Lab은 Spring Boot 기반 애플리케이션에서 발생할 수 있는
성능 저하와 장애 상황을 직접 재현하고 분석하기 위한  프로젝트이다.

일반적인 비즈니스 기능 구현보다는 실제 운영 환경에서 발생할 수 있는
Thread Pool 고갈, DB Connection Pool 고갈, Slow Query, GC 문제,
외부 API 지연 등의 상황을 의도적으로 발생시키고 원인을 분석하는 것을 목표로 한다.

단순히 장애 원인을 이론적으로 학습하는 것이 아니라,

**장애 발생 → 증상 확인 → 데이터 수집 → 원인 분석 → 개선 → 재검증**

과정을 직접 반복하면서 JVM, Tomcat, DB 및 애플리케이션의 동작을 이해한다.


## 2. 프로젝트 목표

### 장애 상황 직접 재현

운영 환경에서 발생할 수 있는 대표적인 장애 상황을 의도적으로 만든다.

- Tomcat Thread Pool 고갈
- HikariCP Connection Pool 고갈
- Slow Query
- DB Lock / Connection 대기
- 외부 API 응답 지연 및 Timeout
- Heap 사용량 증가
- GC 및 Full GC
- CPU 부하
- Thread BLOCKED / WAITING 상태


### 장애 분석 도구 학습

장애 발생 시 다음 도구를 이용하여 JVM과 애플리케이션 상태를 분석한다.

- Access Log
- Application Log
- Actuator
- jstack
- jcmd
- jstat
- Heap Dump
- Thread Dump
- DB Execution Plan


### 관찰 가능한 서비스 구성

최종적으로 애플리케이션 상태를 Metric으로 확인할 수 있도록 구성한다.

Spring Boot Actuator와 Micrometer를 기반으로 Metric을 수집하고,
추후 Prometheus와 Grafana를 연동하여 시각화한다.


## 3. 기술 스택

### Application

- Java 21
- Spring Boot 4.1.1
- Spring Web MVC
- Spring Data JPA
- Spring Boot Actuator
- HikariCP
- Lombok

### Database

초기:

- H2



### Observability

초기:

- Spring Boot Actuator
- JVM Tools
    - jstack
    - jcmd
    - jstat

추후:

- Micrometer
- Prometheus
- Grafana


## 4. 기본 구조

애플리케이션에는 장애 상황을 재현하기 위한 API를 구성한다.

```text
Client
  │
  ▼
Embedded Tomcat
  │
  ├── /normal
  │     └── 정상 요청
  │
  ├── /slow
  │     └── 느린 요청
  │
  ├── /db
  │     └── DB Connection 사용
  │
  ├── /memory
  │     └── Heap 사용량 증가
  │
  ├── /cpu
  │     └── CPU 부하 발생
  │
  └── /external
        └── 외부 API 지연 / Timeout