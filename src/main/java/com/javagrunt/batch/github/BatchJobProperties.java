package com.javagrunt.batch.github;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "github-batch")
record BatchJobProperties(String token, String organization, String redis_host, int redis_port) { }
