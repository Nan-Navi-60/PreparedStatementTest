# 📊 SQL Query Server-Side Prepared Statements

이 섹션은 `MySQL Connector/J`의 설정값(`useServerPrepStmts`, `cachePrepStmts`)에 따른 쿼리 실행 성능을 분석한 실습 기록입니다. 특히 쿼리의 복잡도와 캐시 활용 여부가 실제 응답 시간에 미치는 영향을 검증하고자 합니다.

## 1. 실습 개요
* **목적**: 복잡한 JOIN 및 집계 쿼리에서 Prepared Statement 캐싱의 유의미한 성능 차이 검증
* **기술 스택**: Java (JDBC), MySQL (Sakila DB)
* **실행 환경**: 로컬 환경 측정 (네트워크 통신 변수 배제)

## 2. 가설 설정
1. [참고 자료 1] 내용 중 테스트 결과가 '속성값이 변경되어도 쿼리문이 단순하여 결과가 큰 편차가 나타나지 않는다.' 라고 명시되어 있다.
2. 쿼리의 복잡도가 증가하면, 캐시 및 PreparedStatement 사용 여부에 따라 결과에 큰 편차가 나타날 것이다.
3. 따라서 JOIN이 포함된 복잡한 쿼리일수록 서버측 Prepared Statement 캐시가 성능 향상에 크게 기여할 것이다.

### [실험에 사용된 복잡한 쿼리]
```sql
SELECT 
    r.rental_date, c.first_name, c.last_name, f.title 
FROM rental r
JOIN customer c ON r.customer_id = c.customer_id
JOIN inventory i ON r.inventory_id = i.inventory_id
JOIN film f ON i.film_id = f.film_id
WHERE r.rental_id = ?;
```

### [`MySQL Connector/J`의 설정값(`useServerPrepStmts`, `cachePrepStmts`)]
* **Client Prepared Statement 사용, Non-Caching**
```java
final String properties = "?useServerPrepStmts=false&cachePrepStmts=false";
```

* **Client Prepared Statement 사용, Caching**
```java
final String properties = "?useServerPrepStmts=false&cachePrepStmts=true";
```

* **Server Prepared Statement 사용, Non-Caching**
```java
final String properties = "?useServerPrepStmts=true&cachePrepStmts=false";
```

* **Server Prepared Statement 사용, Caching**
```java
final String properties = "?useServerPrepStmts=true&cachePrepStmts=true";
```

### [실험을 위한 쿼리 반복문]
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

### [결과]
| useServerPrepStmts | cachePrepStmts | avg | min | max |
| :--- | :--- | :--- | :--- | :--- |
| false | false | 4043.7 | 3536 | 6267 |
| false | true | 4485.7 | 3722 | 5140 |
| true | false | 6297.5 | 5672 | 7936 |
| true | true | 3106.7 | 2473 | 3857 |

### [분석]
* **Client Prepared Statement 사용, Non-Caching**
  - PreparedStatement를 생성하지 않는다. 매번 쿼리를 파싱해서 전달한다.

* **Client Prepared Statement 사용, Caching**
  - PreparedStatement를 생성하지 않는다. QueryInfo가 캐싱된다.

* **Server Prepared Statement 사용, Non-Caching**
  - PreparedStatement를 생성한다. 매 요청마다 객체가 생성 / 삭제되기를 반복한다.

* **Server Prepared Statement 사용, Caching**
  - PreparedStatment를 생성한 후 이후 동일한 쿼리 요청에 대해 캐시를 활용한다.
 
* 속성값 변경에 대한 무의미한 차이
  - 즉, PreparedStatement를 생성하고, 파싱하는 과정에 따른 차이는 분명하게 존재하나 그 차이는 미미한 수준이다.
  또한, 매 요청 마다 객체가 생성 / 삭제를 반복하는 과정이 매 요청에 대해 새로운 객체를 생성하고 캐싱하는 과정보다 비효율적이다.
* Sever Prepared Statement와 Client Prepared Statement의 성능 차이
  - 중복된 쿼리에 대한 요청의 경우 매번 쿼리 내부의 정적이 부분과 동적인 부분을 나누어 2회 전송하게 된다.
  만약 Server Prepared Statment를 사용하였다면 중복된 쿼리 요청을 PreparedStatementId와 함께 동적인 쿼리 부분을 통해 요청하기 때문에 요청횟수가 1회가 된다.  
 
### 참고 자료
1. https://tech.kakaopay.com/post/how-preparedstatement-works-in-our-apps/

