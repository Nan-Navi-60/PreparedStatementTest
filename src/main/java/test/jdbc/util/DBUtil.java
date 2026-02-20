package test.jdbc.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

/**
 * jdbc.properties 파일을 통해 해당 파일 내에 DB 서버 연결과 관련된 설정 정보를 불러오는 역할
 */
public class DBUtil {
  
  public static Connection getConnection(String argument) {
    // argument - 커맨드 라인을 통해 입력받은 파일명(ex. jdbc.properties)
    
    try {
      // 1. DB 설정 파일을 읽어들이는 작업 - DBConfigurer.java
      Properties props = DBConfigurer.readProperties(argument);
      
      // 2. 읽어들인 파일을 통해 설정 정보를 기반으로 Connection 객체 생성 및 반환
      final String USER_NAME = props.getProperty("user"); // root
      final String PASSWORD = props.getProperty("password");
      final String DB_URL = props.getProperty("url");
      final String DATABASE = props.getProperty("database");
      final String properties = "?useServerPrepStmts=false&cachePrepStmts=false";
      
      Connection connection 
        = DriverManager.getConnection(DB_URL + DATABASE + properties, USER_NAME, PASSWORD);
      
      return connection;
      
    } catch (Exception e) {
      e.printStackTrace();
    }
    
    return null;
  }
}
