package com.javagrunt.batch.github;

import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GHRepositoryCloneTraffic;
import org.kohsuke.github.GHRepositoryViewTraffic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.NonNull;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.timeseries.DuplicatePolicy;
import redis.clients.jedis.timeseries.TSCreateParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class RepositoryProcessor implements ItemProcessor<GHRepository, List<SimpleTimeSeriesData>> {

    private static final Logger log = LoggerFactory.getLogger(RepositoryProcessor.class);
    private final JedisPooled jedisPooled;
    
    RepositoryProcessor(JedisPooled jedisPooled) {
        this.jedisPooled = jedisPooled;
    }
    @Override
    public List<SimpleTimeSeriesData> process(@NonNull GHRepository ghRepository) throws IOException {
        ArrayList<SimpleTimeSeriesData> result = new ArrayList<>();
        result.addAll(cloneTraffic(ghRepository));
        result.addAll(viewTraffic(ghRepository));
        log.info("Processed: " + ghRepository.getFullName() + " with " + result.size() + " records");
        return result;
    }

    private String createTS(String key) {
        TSCreateParams tsCreateParams = new TSCreateParams();
        tsCreateParams.duplicatePolicy(DuplicatePolicy.MAX);
        try {
            jedisPooled.tsCreate(key, tsCreateParams);
        } catch (JedisDataException jde) {
//            System.out.println("TS already exists");
        }
        return key;
    }

    private List<SimpleTimeSeriesData> cloneTraffic(GHRepository repo) throws IOException {
        ArrayList<SimpleTimeSeriesData> result = new ArrayList<>();
        String tst = createTS(String.format("%s_CLONE_T", repo.getFullName())); //total views
        String tsu = createTS(String.format("%s_CLONE_U", repo.getFullName())); //unique views
        List<GHRepositoryCloneTraffic.DailyInfo> viewInfo = repo.getCloneTraffic().getDailyInfo();
        for (GHRepositoryCloneTraffic.DailyInfo info : viewInfo) {
            result.add(new SimpleTimeSeriesData(tst, info.getTimestamp().getTime(), info.getCount()));
            result.add(new SimpleTimeSeriesData(tsu, info.getTimestamp().getTime(), info.getUniques()));
        }
        return result;
    }

    private List<SimpleTimeSeriesData> viewTraffic(GHRepository repo) throws IOException {
        ArrayList<SimpleTimeSeriesData> result = new ArrayList<>();
        String tst = createTS(String.format("%s_VIEW_T", repo.getFullName())); //total views
        String tsu = createTS(String.format("%s_VIEW_U", repo.getFullName())); //unique views
        List<GHRepositoryViewTraffic.DailyInfo> viewInfo = repo.getViewTraffic().getDailyInfo();
        for (GHRepositoryViewTraffic.DailyInfo info : viewInfo) {
            result.add(new SimpleTimeSeriesData(tst, info.getTimestamp().getTime(), info.getCount()));
            result.add(new SimpleTimeSeriesData(tsu, info.getTimestamp().getTime(), info.getUniques()));
        }
        return result;
    }
}
