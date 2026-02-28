package org.clueapi.db;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

public class Database {

  private final HikariDataSource dataSource;

  public Database() {
    String url      = System.getenv("DB_URL");
    String user     = System.getenv("DB_USER");
    String password = System.getenv("DB_PASSWORD");

    if (url == null || user == null || password == null) {
      throw new IllegalStateException(
          "Missing required environment variables: DB_URL, DB_USER, DB_PASSWORD");
    }

    HikariConfig config = new HikariConfig();
    config.setJdbcUrl(url);
    config.setUsername(user);
    config.setPassword(password);
    dataSource = new HikariDataSource(config);
  }

  public Connection getConnection() throws SQLException {
    return dataSource.getConnection();
  }
}
