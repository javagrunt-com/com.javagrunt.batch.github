package com.javagrunt.batch.github;

import org.kohsuke.github.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import redis.clients.jedis.JedisPooled;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class JobConfiguration {

    private final JobProperties jobProperties;
    private final JedisPooled jedisPooled;

    JobConfiguration(JobProperties jobProperties,
                     JedisPooled jedisPooled) {
        this.jobProperties = jobProperties;
        this.jedisPooled = jedisPooled;
    }

    @Bean
    List<GHRepository> repositories() throws IOException {
        ArrayList<GHRepository> repositories = new ArrayList<>();
        System.out.println("Tokens: " + jobProperties.getTokens().length);
        for (String token : jobProperties.getTokens()) {
            GitHub github = new GitHubBuilder().withOAuthToken(token).build();
            GHOrganization org = github.getOrganization(organization);
            for (GHRepository repo : org.listRepositories()) {
                if (!repo.isPrivate() && repo.hasPushAccess()) {
                    repositories.add(repo);
                }
            }
        }
        return repositories;
    }


    @Bean
    ItemReader<GHRepository> reader(List<GHRepository> repositories) {
        return new RepositoryReader(repositories);
    }

    @Bean
    RepositoryProcessor processor() {
        return new RepositoryProcessor(jedisPooled);
    }

    @Bean
    TimeSeriesDataWriter<List<SimpleTimeSeriesData>> writer() {
        return new TimeSeriesDataWriter<>(jedisPooled);
    }

    @Bean
    Step step1(JobRepository jobRepository,
               DataSourceTransactionManager transactionManager,
               ItemReader<GHRepository> reader,
               RepositoryProcessor processor,
               TimeSeriesDataWriter<List<SimpleTimeSeriesData>> writer) {
        return new StepBuilder("step1", jobRepository)
                .<GHRepository, List<SimpleTimeSeriesData>>chunk(3, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    Job githubBatch(JobRepository jobRepository, Step step) {
        return new JobBuilder("githubBatch", jobRepository)
                .start(step)
                .build();
    }

}