package test.jdbc.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * DB 설정 정보(파일)을 읽어서 java.util.Properites 객체로 바인딩해주는 역할
 */
public class DBConfigurer {
  /**
   * @param argument - DB 설정 파일명(ex. jdbc.properties)
   * @return properties - 설정 파일 내 설정 값을 담은 Properties 객체
   */
  public static Properties readProperties(String argument) {
    String propertiesFilePath = argument;
    Properties properties = new Properties();

    try (FileInputStream fis = new FileInputStream(propertiesFilePath)) {
      properties.load(fis);
    } catch (IOException e) {
      System.err.println(e.getMessage());
    }

    return properties;
  }
}
