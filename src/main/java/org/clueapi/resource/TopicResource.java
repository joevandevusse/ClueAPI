package org.clueapi.resource;

import io.javalin.http.Context;
import org.clueapi.db.Database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class TopicResource {

  private final Database db;

  public TopicResource(Database db) {
    this.db = db;
  }

  public void getAll(Context ctx) throws Exception {
    List<String> topics = new ArrayList<>();
    try (Connection conn = db.getConnection();
         ResultSet rs = conn.createStatement().executeQuery(
             "SELECT DISTINCT canonical_topic FROM category_mappings ORDER BY canonical_topic")) {
      while (rs.next()) {
        topics.add(rs.getString("canonical_topic"));
      }
    }
    ctx.json(topics);
  }
}
