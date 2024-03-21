package com.javagrunt.batch.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "job")
record JobProperties(String token, String organization) { }
