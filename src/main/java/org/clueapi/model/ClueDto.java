package org.clueapi.model;

public record ClueDto(
    String question,
    String answer,
    String clueValue,
    String round,
    String gameDate,
    String canonicalTopic
) {}
