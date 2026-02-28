package org.clueapi.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record StatEntry(
    @JsonProperty("canonicalTopic") String canonicalTopic,
    @JsonProperty("passed") boolean passed
) {}
