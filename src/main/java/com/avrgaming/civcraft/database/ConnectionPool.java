package com.avrgaming.civcraft.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionPool {

	private  HikariDataSource connectionPool;

	public ConnectionPool(String dbcUrl, String user, String pass) throws ClassNotFoundException, SQLException {
		/* Initialize our connection pool.
		 * 
		 * We'll use a connection pool and reuse connections on a per-thread basis. */

		/* setup the connection pool */
		HikariConfig config = new HikariConfig();
//		config.setDataSourceClassName("com.mysql.jdbc.jdbc2.optional.MysqlDataSource");
		config.setJdbcUrl(dbcUrl);
		config.setUsername(user);
		config.setPassword(pass);
		config.setMaximumPoolSize(30);
		
		connectionPool = new HikariDataSource(config);
	}

	public Connection getConnection() throws SQLException {
		return connectionPool.getConnection();
	}

}
