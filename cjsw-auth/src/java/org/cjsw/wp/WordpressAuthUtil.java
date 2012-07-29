package org.cjsw.wp;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import ca.benow.java.log.LogManager;
import ca.benow.java.run.SimpleRunner;
import ca.benow.security.AuthenticationFailedException;

public class WordpressAuthUtil {

	private static Logger log = LogManager.declareLogger();

	static {
		try {
			Class.forName("com.mysql.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			System.err.println("MYSQL Driver not found on classpath.  Can't start");
			System.exit(-1);
		}
	}

	public String getUserHash(String userName) throws SQLException {
		String hash = null;
		String sql = "select user_pass from wp_users LEFT JOIN wp_usermeta ON (wp_users.ID = wp_usermeta.user_id) where user_login=?";
		Connection conn = createConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userName);
			try {
				ResultSet rs = stmt.executeQuery();
				try {
					if (rs.next())
						hash = rs.getString(1);
				} finally {
					rs.close();
				}
			} finally {
				stmt.close();
			}
		} finally {
			conn.close();
		}
		return hash;
	}

	public Connection createConnection() throws SQLException {
		String user = "cjsw_auth";
		String pass = "thecobra";
		Connection conn = DriverManager.getConnection("jdbc:mysql://cjsw.com/cjsw?user=" + user + "&password=" + pass);
		return conn;
	}

	public void checkHash(String password, String hash) throws AuthenticationFailedException, SecurityException {
		try {
			SimpleRunner runner = new SimpleRunner(new String[] { "php5", "var/wordpress/wpcheck.php", password, hash });
			runner.setQuiet(true);
			runner.setVerbose();
			int exit = runner.run();
			if (exit != 0)
				throw new AuthenticationFailedException();
		} catch (AuthenticationFailedException e) {
			throw e;
		} catch (Throwable e) {
			throw new SecurityException("Error authenticating", e);
		}
	}

	public static void main(String[] args) {
		if (args.length < 2) {
			System.err.println("Missing arguments");
			System.out.println("Usage: java " + WordpressAuthUtil.class.getName() + " [user] [pass]");
		}
		try {
			WordpressAuthUtil util = new WordpressAuthUtil();
			String wpHash = util.getUserHash(args[0]);
			// try b64(md5(pass))
			util.checkHash(args[1], wpHash);
			System.out.println("Authentication succeeded.");
		} catch (AuthenticationFailedException e) {
			System.err.println("Authentication failed.");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void authenticate(String userName, String password) throws AuthenticationFailedException, SecurityException,
			SQLException {
		String hash = getUserHash(userName);
		checkHash(password, hash);

	}

}
