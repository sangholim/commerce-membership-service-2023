# membership-service
- MSA 기반 회원 혜택 서비스
- 개발 언어: Kotlin
- 통합 프레임워크: Spring Boot2
  - Spring Security Reactive
  - Spring Webflux
  - Spring Data Mongo Reactive
- Kotlin 의 Coroutine 도입 함으로서, 동시성 처리 지원
- 데이터 베이스: Mongodb
## API 명세
## 이벤트
- 회원 가입 이후 이벤트 처리
  - 회원 등급, 적립금 계좌 생성
  - 회원 서비스 -> 회원 혜택 서비스
## 마이크로 서비스 구조
![image](https://user-images.githubusercontent.com/55565835/215302711-9f1c370c-ff1c-4026-a556-fd8153c1b1e8.png)
---
