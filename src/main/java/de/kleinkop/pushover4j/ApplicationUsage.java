package de.kleinkop.pushover4j;

import java.time.LocalDateTime;

public record ApplicationUsage(
    Integer limit,
    Integer remaining,
    LocalDateTime reset
)
{}
