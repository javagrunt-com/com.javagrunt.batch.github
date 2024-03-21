package com.javagrunt.batch.github;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.NonNull;
import redis.clients.jedis.JedisPooled;

import java.util.List;

class TimeSeriesDataWriter<T> implements ItemWriter<T> {

    private final JedisPooled jedisPooled;

    TimeSeriesDataWriter(JedisPooled jedisPooled) {
        this.jedisPooled = jedisPooled;
    }

    @Override
    public void write(@NonNull Chunk<? extends T> chunk) {
        for(var data : chunk.getItems()) {
            for(SimpleTimeSeriesData tsd : (List<SimpleTimeSeriesData>)data) {
                jedisPooled.tsAdd(tsd.key(), tsd.timestamp(), tsd.value());
            }
        }
    }
    
}