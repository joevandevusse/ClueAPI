package org.clueapi;

import io.javalin.Javalin;
import org.clueapi.db.Database;
import org.clueapi.resource.ClueResource;
import org.clueapi.resource.StatsResource;
import org.clueapi.resource.TopicResource;

public class ClueApi {

  public static void main(String[] args) {
    Database db = new Database();

    TopicResource topics = new TopicResource(db);
    ClueResource  clues  = new ClueResource(db);
    StatsResource stats  = new StatsResource(db);

    Javalin app = Javalin.create(config -> {
      // Allow any origin so the React dev server can call the API
      config.bundledPlugins.enableCors(cors ->
          cors.addRule(it -> it.anyHost()));
    }).start(7070);

    app.get("/api/topics",        topics::getAll);
    app.get("/api/clues",         clues::getByTopic);
    app.post("/api/stats",        stats::record);
  }
}
