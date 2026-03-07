package org.clueapi.resource;

import io.javalin.http.Context;
import org.clueapi.db.Database;
import org.clueapi.model.BubblePointDto;
import org.clueapi.model.StatEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

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

  public void getBubble(Context ctx) throws Exception {
    String sql = """
        SELECT
          t.canonical_topic,
          t.clue_count,
          t.mean_value,
          COALESCE(s.attempt_count, 0) AS attempt_count,
          s.accuracy
        FROM (
          SELECT
            cm.canonical_topic,
            COUNT(*) AS clue_count,
            AVG(
              CASE
                WHEN c.clue_value ~ '^\\$[0-9,]+$'
                THEN CAST(REPLACE(REPLACE(c.clue_value, '$', ''), ',', '') AS INTEGER)
                ELSE NULL
              END
            ) AS mean_value
          FROM category_mappings cm
          JOIN clues_java c ON c.category = cm.jeopardy_category
          GROUP BY cm.canonical_topic
        ) t
        LEFT JOIN (
          SELECT canonical_topic,
                 COUNT(*) AS attempt_count,
                 AVG(CASE WHEN passed THEN 1.0 ELSE 0.0 END) AS accuracy
          FROM user_stats
          GROUP BY canonical_topic
        ) s ON s.canonical_topic = t.canonical_topic
        ORDER BY t.canonical_topic
        """;

    List<BubblePointDto> points = new ArrayList<>();
    try (Connection conn = db.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql);
         ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        long attempts = rs.getLong("attempt_count");
        Double accuracy = attempts > 0 ? rs.getDouble("accuracy") : null;
        points.add(new BubblePointDto(
            rs.getString("canonical_topic"),
            rs.getLong("clue_count"),
            rs.getDouble("mean_value"),
            attempts,
            accuracy
        ));
      }
    }
    ctx.json(points);
  }
}
