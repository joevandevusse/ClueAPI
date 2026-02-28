package org.clueapi.resource;

import io.javalin.http.Context;
import org.clueapi.db.Database;
import org.clueapi.model.StatEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class StatsResource {

  private final Database db;

  public StatsResource(Database db) {
    this.db = db;
  }

  public void record(Context ctx) throws Exception {
    StatEntry entry = ctx.bodyAsClass(StatEntry.class);

    String sql = "INSERT INTO user_stats (canonical_topic, passed) VALUES (?, ?)";
    try (Connection conn = db.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, entry.canonicalTopic());
      ps.setBoolean(2, entry.passed());
      ps.executeUpdate();
    }
    ctx.status(201);
  }
}
