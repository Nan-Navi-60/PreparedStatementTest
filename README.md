# 📊 SQL Query Server-Side Prepared Statements Performance Test

> `MySQL Connector/J`의 `useServerPrepStmts`, `cachePrepStmts` 설정에 따른  
> 쿼리 실행 성능 비교 실험 기록

---

## 📌 1. 실습 개요

### 🎯 목적
복잡한 `JOIN` 및 집계 쿼리에서 **Prepared Statement 캐싱 여부가 성능에 유의미한 차이를 만드는지 검증**

### 🛠 기술 스택
- **Java (JDBC)**
- **MySQL (Sakila DB)**

### 🖥 실행 환경
- 로컬 환경 측정
- 네트워크 통신 변수 배제

---

## 🔍 2. 가설 설정

1. 참고 자료에 따르면,  
   > "속성값이 변경되어도 쿼리문이 단순하여 결과가 큰 편차가 나타나지 않는다."

2. 쿼리의 복잡도가 증가하면, 캐시 및 PreparedStatement 사용 여부에 따라 결과에 큰 편차가 나타날 것이다.

3. 따라서 JOIN이 포함된 복잡한 쿼리일수록 서버측 Prepared Statement 캐시가 성능 향상에 크게 기여할 것이다.

---

## 🧪 실험 쿼리

### 📎 복잡한 JOIN 쿼리

```sql
SELECT 
    r.rental_date, c.first_name, c.last_name, f.title 
FROM rental r
JOIN customer c ON r.customer_id = c.customer_id
JOIN inventory i ON r.inventory_id = i.inventory_id
JOIN film f ON i.film_id = f.film_id
WHERE r.rental_id = ?;
````


## ⚙️ MySQL Connector/J 설정값

### 1️⃣ Client Prepared Statement 사용, Non-Caching

```java
final String properties = "?useServerPrepStmts=false&cachePrepStmts=false";
```


### 2️⃣ Client Prepared Statement 사용, Caching

```java
final String properties = "?useServerPrepStmts=false&cachePrepStmts=true";
```


### 3️⃣ Server Prepared Statement 사용, Non-Caching

```java
final String properties = "?useServerPrepStmts=true&cachePrepStmts=false";
```


### 4️⃣ Server Prepared Statement 사용, Caching

```java
final String properties = "?useServerPrepStmts=true&cachePrepStmts=true";
```


## 🔁 실험용 반복 코드

```java
for(int j = 0; j < 10; j++) {

    long start = System.currentTimeMillis();

    for (int i = 0; i < 20000; i++) {

        PreparedStatement stmt = conn.prepareStatement(
            "{Qurey}"
        );

        int targetId = (i % 16000) + 1;

        stmt.setInt(1, targetId);
        ResultSet rs = stmt.executeQuery();

        rs.close();
        stmt.close();
    }

    long end = System.currentTimeMillis();

    System.out.println((j+1)+"번 실행시간: " + (end - start) + "ms");
}
```

---

## 📊 실험 결과

| useServerPrepStmts | cachePrepStmts | avg (ms) | min (ms) | max (ms) |
| :----------------- | :------------- | :------- | :------- | :------- |
| false              | false          | 4043.7   | 3536     | 6267     |
| false              | true           | 4485.7   | 3722     | 5140     |
| true               | false          | 6297.5   | 5672     | 7936     |
| true               | true           | 3106.7   | 2473     | 3857     |
<br>

## 🧠 분석
| Client Prepared Statement 사용, Non-Caching | Client Prepared Statement 사용, Caching |
| :------------------------------------------ | :--------------------------------------- |
| * PreparedStatement를 생성하지 않는다.<br>* 매번 쿼리를 파싱해서 전달한다. | * PreparedStatement를 생성하지 않는다.<br>* QueryInfo가 캐싱된다.|

| Server Prepared Statement 사용, Non-Caching | Server Prepared Statement 사용, Caching |
| :------------------------------------------ | :--------------------------------------- |
| * PreparedStatement를 생성한다.<br>* 매 요청마다 객체가 생성 / 삭제되기를 반복한다. | * PreparedStatement를 생성한 후 <br>동일한 쿼리 요청에 대해 캐시를 활용한다.|




---


## 📝 실험 결과 요약 및 가설 검증

### :white_check_mark: SQL 파싱 주체 및 캐싱 여부에 따른 성능 분석
본 실험에서는 Prepared Statement의 생성 위치(**Server vs Client**)와 캐싱 전략에 따라 수행 시간이 각기 다른 양상으로 나타남을 확인하였습니다.

* **Server-side Prepared Statement**
    * **캐싱 활성 시**: SQL 서버 측에 Statement가 생성 및 캐싱되어, 동일 쿼리 요청 시 재사용되므로 수행 시간이 가장 짧고 효율적입니다.
    * **캐싱 비활성 시**: 클라이언트의 요청마다 Statement 생성과 삭제를 반복하는 과정이 추가되어 수행 시간이 급격히 길어지는 현상을 보였습니다.
* **Client-side Prepared Statement**
    * 실험 결과, 오히려 캐싱을 적용했을 때 수행 시간이 더 길게 측정되었습니다. 이를 통해 **CPU가 직접 SQL문을 파싱하여 서버에 전달하는 비용보다, 캐싱 과정에서 발생하는 메모리 참조 오버헤드가 더 크다**는 결론을 도출하였습니다.

<br>

### :white_check_mark: 가설 검증 결과

#### **가설 1: "쿼리의 복잡도가 증가하면 캐시 및 PreparedStatement 사용 여부에 따라 결과 편차가 크게 나타날 것이다."**
* **검증 결과:**
  Server와 Client 양측 모두 캐시 사용 여부에 따른 유의미한 편차를 확인하였습니다. 다만, Server 측에서는 캐싱이 성능을 향상시키는 반면, Client 측에서는 오히려 오버헤드로 작용하여 성능 차이가 반대 양상으로 나타남을 확인하였습니다.

#### **가설 2: "JOIN이 포함된 복잡한 쿼리일수록 Server-side Prepared Statement 캐시가 성능 향상에 크게 기여할 것이다."**
* **검증 결과:**
  선행 연구의 단순 쿼리 테스트 결과와 비교했을 때, 복잡한 JOIN 쿼리를 사용한 본 실험에서 각 케이스별 성능 편차가 훨씬 두드러지게 나타났습니다.



> **최종 결론**: 쿼리문이 복잡해질수록 서버의 실행 계획 수립 및 구문 분석 비용이 증가하며, 이를 최적화하는 **서버 측 캐시가 전체 성능에 미치는 영향력 또한 정비례하여 증가함**을 입증하였습니다.

---

## 📚 참고 자료

1. [https://tech.kakaopay.com/post/how-preparedstatement-works-in-our-apps/](https://tech.kakaopay.com/post/how-preparedstatement-works-in-our-apps/)

---

