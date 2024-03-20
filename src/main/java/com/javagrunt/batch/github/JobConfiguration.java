package com.javagrunt.batch.github;

import org.kohsuke.github.*;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.timeseries.DuplicatePolicy;
import redis.clients.jedis.timeseries.TSCreateParams;

import java.io.IOException;
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

    Step getRepositories(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("getRepositories", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    System.out.println("Tokens: " + jobProperties.getTokens().length);
                    for (String token : jobProperties.getTokens()) {
                        GitHub github = new GitHubBuilder().withOAuthToken(token).build();
                        System.out.println("Orgs: " + github.getMyOrganizations().keySet());
                        for (String organization : github.getMyOrganizations().keySet()) {
                            GHOrganization org = github.getOrganization(organization);
                            for (GHRepository repo : org.listRepositories()) {
                                if (!repo.isPrivate() && repo.hasPushAccess()) {
                                    viewTraffic(repo);
                                    cloneTraffic(repo);
                                }
                            }
                        }
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    Job githubBatch(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new JobBuilder("githubBatch", jobRepository)
                .start(getRepositories(jobRepository, transactionManager))
                .build();
    }

    private void cloneTraffic(GHRepository repo) throws IOException {
        String tst = repo.getFullName() + "_CLONE_T"; //total views
        String tsu = repo.getFullName() + "_CLONE_U"; //unique views
        createTS(tst);
        createTS(tsu);
        List<GHRepositoryCloneTraffic.DailyInfo> viewInfo;
        try {
            viewInfo = repo.getCloneTraffic().getDailyInfo();
            for (GHRepositoryCloneTraffic.DailyInfo info : viewInfo) {
                jedisPooled.tsAdd(tst, info.getTimestamp().getTime(), info.getCount());
                jedisPooled.tsAdd(tsu, info.getTimestamp().getTime(), info.getUniques());
            }
            System.out.println("Clones T: " + repo.getFullName() + " " + jedisPooled.tsRange(tst, 0, System.currentTimeMillis()));
            System.out.println("Clones U: " + repo.getFullName() + " " + jedisPooled.tsRange(tsu, 0, System.currentTimeMillis()));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void viewTraffic(GHRepository repo) throws IOException {

        String tst = repo.getFullName() + "_VIEW_T"; //total views
        String tsu = repo.getFullName() + "_VIEW_U"; //unique views
        createTS(tst);
        createTS(tsu);

        List<GHRepositoryViewTraffic.DailyInfo> viewInfo;
        try {
            viewInfo = repo.getViewTraffic().getDailyInfo();
            for (GHRepositoryViewTraffic.DailyInfo info : viewInfo) {
                jedisPooled.tsAdd(tst, info.getTimestamp().getTime(), info.getCount());
                jedisPooled.tsAdd(tsu, info.getTimestamp().getTime(), info.getUniques());
            }
            System.out.println("Views T: " + repo.getFullName() + " " + jedisPooled.tsRange(tst, 0, System.currentTimeMillis()));
            System.out.println("Views U: " + repo.getFullName() + " " + jedisPooled.tsRange(tsu, 0, System.currentTimeMillis()));
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private void createTS(String key) {
        TSCreateParams tsCreateParams = new TSCreateParams();
        tsCreateParams.duplicatePolicy(DuplicatePolicy.MAX);
        try {
            jedisPooled.tsCreate(key, tsCreateParams);
        } catch (JedisDataException jde) {
//            System.out.println("TS already exists");
        }
    }
}