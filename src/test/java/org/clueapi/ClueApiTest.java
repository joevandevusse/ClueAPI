package org.clueapi;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.clueapi.db.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class ClueApiTest {

  private HikariDataSource ds;
  private Javalin app;

  @BeforeEach
  void setUp() throws Exception {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:h2:mem:clueapi_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    config.setUsername("sa");
    config.setPassword("");
    ds = new HikariDataSource(config);

    try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
      st.execute("""
          CREATE TABLE IF NOT EXISTS clues_java (
            id           VARCHAR PRIMARY KEY,
            category     VARCHAR,
            round        VARCHAR,
            category_number INT,
            clue_value   VARCHAR,
            question     TEXT,
            answer       TEXT,
            is_daily_double BOOLEAN,
            game_id      INT,
            game_date    VARCHAR,
            date_added   VARCHAR
          )
          """);
      st.execute("""
          CREATE TABLE IF NOT EXISTS category_mappings (
            jeopardy_category VARCHAR PRIMARY KEY,
            canonical_topic   VARCHAR NOT NULL
          )
          """);
      st.execute("""
          CREATE TABLE IF NOT EXISTS user_stats (
            id              SERIAL PRIMARY KEY,
            canonical_topic VARCHAR NOT NULL,
            passed          BOOLEAN NOT NULL,
            recorded_at     TIMESTAMP NOT NULL DEFAULT now()
          )
          """);

      // Seed data
      st.execute("""
          INSERT INTO category_mappings (jeopardy_category, canonical_topic)
          VALUES ('SCIENCE', 'Science & Nature'), ('HISTORY', 'World History')
          """);
      st.execute("""
          INSERT INTO clues_java
            (id, category, round, category_number, clue_value, question, answer,
             is_daily_double, game_id, game_date, date_added)
          VALUES
            ('c1', 'SCIENCE', 'Jeopardy!', 1, '$200', 'It orbits the Sun', 'Earth',
             false, 1, '2020-01-01', '2020-01-02'),
            ('c2', 'SCIENCE', 'Jeopardy!', 1, '$400', 'Smallest planet', 'Mercury',
             false, 1, '2020-01-01', '2020-01-02'),
            ('c3', 'HISTORY', 'Double Jeopardy!', 2, '$800', 'First US president', 'Washington',
             false, 2, '2020-02-01', '2020-02-02')
          """);
    }

    app = ClueApi.createApp(new Database(ds));
  }

  @AfterEach
  void tearDown() throws Exception {
    try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
      st.execute("DROP TABLE IF EXISTS user_stats");
      st.execute("DROP TABLE IF EXISTS clues_java");
      st.execute("DROP TABLE IF EXISTS category_mappings");
    }
    ds.close();
  }

  // ── GET /api/topics ──────────────────────────────────────────────────────

  @Test
  void getTopics_returnsAllTopicsSorted() {
    JavalinTest.test(app, (server, client) -> {
      var response = client.get("/api/topics");
      assertEquals(200, response.code());
      String body = response.body().string();
      assertTrue(body.contains("Science & Nature"), "should contain 'Science & Nature'");
      assertTrue(body.contains("World History"),    "should contain 'World History'");
      // Alphabetical: Science & Nature < World History
      assertTrue(body.indexOf("Science") < body.indexOf("World"), "should be sorted");
    });
  }

  @Test
  void getTopics_emptyTable_returnsEmptyArray() throws Exception {
    try (Connection conn = ds.getConnection(); Statement st = conn.createStatement()) {
      st.execute("DELETE FROM category_mappings");
    }
    JavalinTest.test(app, (server, client) -> {
      var response = client.get("/api/topics");
      assertEquals(200, response.code());
      assertEquals("[]", response.body().string().trim());
    });
  }

  // ── GET /api/clues ───────────────────────────────────────────────────────

  @Test
  void getClues_validTopic_returnsClues() {
    JavalinTest.test(app, (server, client) -> {
      var response = client.get("/api/clues?topic=Science%20%26%20Nature");
      assertEquals(200, response.code());
      String body = response.body().string();
      assertTrue(body.contains("Earth") || body.contains("Mercury"),
          "should return science clues");
    });
  }

  @Test
  void getClues_missingTopic_returns400() {
    JavalinTest.test(app, (server, client) -> {
      var response = client.get("/api/clues");
      assertEquals(400, response.code());
    });
  }

  @Test
  void getClues_blankTopic_returns400() {
    JavalinTest.test(app, (server, client) -> {
      var response = client.get("/api/clues?topic=");
      assertEquals(400, response.code());
    });
  }

  @Test
  void getClues_unknownTopic_returnsEmptyArray() {
    JavalinTest.test(app, (server, client) -> {
      var response = client.get("/api/clues?topic=NoSuchTopic");
      assertEquals(200, response.code());
      assertEquals("[]", response.body().string().trim());
    });
  }

  @Test
  void getClues_responseContainsExpectedFields() {
    JavalinTest.test(app, (server, client) -> {
      var response = client.get("/api/clues?topic=World%20History");
      assertEquals(200, response.code());
      String body = response.body().string();
      assertTrue(body.contains("question"),      "should have 'question' field");
      assertTrue(body.contains("answer"),        "should have 'answer' field");
      assertTrue(body.contains("clueValue"),     "should have 'clueValue' field");
      assertTrue(body.contains("round"),         "should have 'round' field");
      assertTrue(body.contains("gameDate"),      "should have 'gameDate' field");
      assertTrue(body.contains("canonicalTopic"),"should have 'canonicalTopic' field");
    });
  }

  // ── POST /api/stats ──────────────────────────────────────────────────────

  @Test
  void postStats_validEntry_returns201() {
    JavalinTest.test(app, (server, client) -> {
      var response = client.post("/api/stats",
          "{\"canonicalTopic\":\"Science & Nature\",\"passed\":true}");
      assertEquals(201, response.code());
    });
  }

  @Test
  void postStats_persistsToDatabase() throws Exception {
    JavalinTest.test(app, (server, client) -> {
      client.post("/api/stats",
          "{\"canonicalTopic\":\"World History\",\"passed\":false}");
    });

    try (Connection conn = ds.getConnection();
         var rs = conn.createStatement().executeQuery(
             "SELECT canonical_topic, passed FROM user_stats")) {
      assertTrue(rs.next(), "should have one row");
      assertEquals("World History", rs.getString("canonical_topic"));
      assertFalse(rs.getBoolean("passed"));
    }
  }

  @Test
  void postStats_multipleEntries_allPersisted() throws Exception {
    JavalinTest.test(app, (server, client) -> {
      client.post("/api/stats", "{\"canonicalTopic\":\"Science & Nature\",\"passed\":true}");
      client.post("/api/stats", "{\"canonicalTopic\":\"Science & Nature\",\"passed\":false}");
      client.post("/api/stats", "{\"canonicalTopic\":\"World History\",\"passed\":true}");
    });

    try (Connection conn = ds.getConnection();
         var rs = conn.createStatement().executeQuery(
             "SELECT COUNT(*) AS cnt FROM user_stats")) {
      rs.next();
      assertEquals(3, rs.getInt("cnt"));
    }
  }
}
