package liquibasewizard.wizards;

import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class JDBCConnection {

	// JDBC driver name and database URL
	private Properties prop;

	public String getCatalogVersion(String nmView) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String catalog = "0";
		try {
			Class.forName(getConnectionProperty("driver"));

			System.out.println("Connecting to database...");
			conn = DriverManager.getConnection(getConnectionProperty("url"), getConnectionProperty("username"), getConnectionProperty("password"));

			String sql = "SELECT NVL(MAX(VERSIONEXECUTED), 0) + 1 NR_CATALOG FROM DATABASECHANGECUSTOM WHERE OBJECTNAME = ?";
			ps = conn.prepareStatement(sql);
			ps.setString(1, nmView); 
			rs = ps.executeQuery();
			if (rs.next()) {
				catalog = rs.getString("NR_CATALOG");
			}
		} catch (SQLException se) {
			se.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (ps != null)
					ps.close();
			} catch (SQLException se2) {
			}
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			}
		}

		return catalog;
	}

	public String getConnectionProperty(String nmProperty) throws Exception {
		if (prop == null) {
			prop = new Properties();
			InputStream input = null;
			try {
				input = new FileInputStream("U:\\Softwares\\Windows\\Eclipse\\plugin\\connection\\rdahom_datacenter.properties");
				prop.load(input);
			} finally {
				if (input != null) {
					input.close();
				}
			}
		}
		return prop.getProperty(nmProperty);
	}

}
