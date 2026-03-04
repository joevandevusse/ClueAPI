package org.clueapi.resource;

import io.javalin.http.Context;
import org.clueapi.db.Database;
import org.clueapi.model.ClueDto;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ClueResource {

  private static final int BATCH_SIZE = 20;

  private final Database db;

  public ClueResource(Database db) {
    this.db = db;
  }

  public void getByTopic(Context ctx) throws Exception {
    String topic    = ctx.queryParam("topic");
    String fromDate = ctx.queryParam("fromDate"); // optional, e.g. "2013-09-09"

    boolean allTopics = (topic == null || topic.isBlank());

    List<ClueDto> clues = new ArrayList<>();
    String sql;
    if (!allTopics && fromDate != null) {
      sql = """
          SELECT c.question, c.answer, c.clue_value, c.round, c.game_date, cm.canonical_topic, c.category
          FROM clues_java c
          JOIN category_mappings cm ON c.category = cm.jeopardy_category
          WHERE cm.canonical_topic = ? AND c.game_date >= ?
          ORDER BY RANDOM() LIMIT ?
          """;
    } else if (!allTopics) {
      sql = """
          SELECT c.question, c.answer, c.clue_value, c.round, c.game_date, cm.canonical_topic, c.category
          FROM clues_java c
          JOIN category_mappings cm ON c.category = cm.jeopardy_category
          WHERE cm.canonical_topic = ?
          ORDER BY RANDOM() LIMIT ?
          """;
    } else if (fromDate != null) {
      sql = """
          SELECT c.question, c.answer, c.clue_value, c.round, c.game_date, cm.canonical_topic, c.category
          FROM clues_java c
          JOIN category_mappings cm ON c.category = cm.jeopardy_category
          WHERE c.game_date >= ?
          ORDER BY RANDOM() LIMIT ?
          """;
    } else {
      sql = """
          SELECT c.question, c.answer, c.clue_value, c.round, c.game_date, cm.canonical_topic, c.category
          FROM clues_java c
          JOIN category_mappings cm ON c.category = cm.jeopardy_category
          ORDER BY RANDOM() LIMIT ?
          """;
    }

    try (Connection conn = db.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
      int idx = 1;
      if (!allTopics) ps.setString(idx++, topic);
      if (fromDate != null) ps.setString(idx++, fromDate);
      ps.setInt(idx, BATCH_SIZE);
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        clues.add(new ClueDto(
            rs.getString("question"),
            rs.getString("answer"),
            rs.getString("clue_value"),
            rs.getString("round"),
            rs.getString("game_date"),
            rs.getString("canonical_topic"),
            rs.getString("category")
        ));
      }
    }
    ctx.json(clues);
  }
}
