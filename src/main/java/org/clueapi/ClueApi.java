package org.clueapi;

import io.javalin.Javalin;
import org.clueapi.db.Database;
import org.clueapi.resource.ClueResource;
import org.clueapi.resource.StatsResource;
import org.clueapi.resource.TopicResource;

public class ClueApi {

  public static void main(String[] args) {
    createApp(new Database()).start(7070);
  }

  static Javalin createApp(Database db) {
    TopicResource topics = new TopicResource(db);
    ClueResource  clues  = new ClueResource(db);
    StatsResource stats  = new StatsResource(db);

    return Javalin.create(config -> {
      // Allow any origin so the React dev server can call the API
      config.bundledPlugins.enableCors(cors ->
          cors.addRule(it -> it.anyHost()));
    })
    .get("/api/topics", topics::getAll)
    .get("/api/clues",  clues::getByTopic)
    .post("/api/stats", stats::record);
  }
}
