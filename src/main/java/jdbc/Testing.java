package jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import test.jdbc.util.DBUtil;

public class Testing {
	public static void main(String[] args) throws Exception {
		// 이 부분 속성값을 변경하며 실행

		DBUtil dbUtil = new DBUtil();
		Connection conn = dbUtil.getConnection(args[0]);
		for(int j = 0; j < 10; j++) {
			long start = System.currentTimeMillis();
			for (int i = 0; i < 20000; i++) {
				PreparedStatement stmt = conn.prepareStatement(
						"SELECT r.rental_date, c.first_name, c.last_name, f.title FROM rental r JOIN customer c ON r.customer_id = c.customer_id JOIN inventory i ON r.inventory_id = i.inventory_id JOIN film f ON i.film_id = f.film_id WHERE r.rental_id = ?"
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
		conn.close();
	}
}
