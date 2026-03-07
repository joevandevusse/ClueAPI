package org.clueapi.model;

public record BubblePointDto(
    String canonicalTopic,
    long   clueCount,
    double meanValue,
    long   attemptCount,
    Double accuracy       // null when attemptCount == 0
) {}
