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

---

## 🧠 분석
| Client Prepared Statement 사용, Non-Caching | Client Prepared Statement 사용, Caching |
| :------------------------------------------ | :--------------------------------------- |
| * PreparedStatement를 생성하지 않는다.<br>* 매번 쿼리를 파싱해서 전달한다. | * PreparedStatement를 생성하지 않는다.<br>* QueryInfo가 캐싱된다.|

| Server Prepared Statement 사용, Non-Caching | Server Prepared Statement 사용, Caching |
| :------------------------------------------ | :--------------------------------------- |
| * PreparedStatement를 생성한다.<br>* 매 요청마다 객체가 생성 / 삭제되기를 반복한다. | * PreparedStatement를 생성한 후 <br>동일한 쿼리 요청에 대해 캐시를 활용한다.|


### 📌 속성값 변경에 대한 무의미한 차이

PreparedStatement를 생성하고 파싱하는 과정에 따른 차이는 분명 존재하나
그 차이는 미미한 수준이다.

또한, 매 요청마다 객체 생성 / 삭제를 반복하는 과정은
매 요청에 대해 새로운 객체를 생성하고 캐싱하는 과정보다 비효율적이다.


### 📌 Server Prepared Statement와 Client Prepared Statement의 성능 차이

중복된 쿼리에 대한 요청의 경우:

* 매번 쿼리 내부의 정적 부분과 동적 부분을 나누어 **2회 전송**하게 된다.

반면 Server Prepared Statement를 사용하는 경우:

* 중복된 쿼리 요청을 PreparedStatementId와 함께
* 동적인 쿼리 부분만 전송하여
* 요청 횟수가 **1회**가 된다.

---

## 📚 참고 자료

1. [https://tech.kakaopay.com/post/how-preparedstatement-works-in-our-apps/](https://tech.kakaopay.com/post/how-preparedstatement-works-in-our-apps/)

---
