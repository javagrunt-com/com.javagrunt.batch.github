package com.javagrunt.batch.github;

import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.support.transaction.TransactionAwareProxyFactory;
import org.springframework.lang.NonNull;

import java.util.List;

class TimeSeriesDataWriter implements ItemWriter<List<SimpleTimeSeriesData>> {

    List<List<SimpleTimeSeriesData>> output = TransactionAwareProxyFactory.createTransactionalList();

    @Override
    public void write(@NonNull Chunk<? extends List<SimpleTimeSeriesData>> chunk) throws Exception {
        for(List<SimpleTimeSeriesData> data : chunk.getItems()) {
            for(SimpleTimeSeriesData tsd : data) {
                
            }
        }
    }
    
    public List<List<SimpleTimeSeriesData>> getOutput() {
        return output;
    }
}
