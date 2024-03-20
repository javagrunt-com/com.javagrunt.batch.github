package com.javagrunt.batch.github;

import org.kohsuke.github.GHRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.expression.common.LiteralExpression;

import java.util.List;

public class RepositoryReader implements ItemReader<GHRepository> {
    
    private final List<GHRepository> repositories;
    
    public RepositoryReader(List<GHRepository> repositories) {
        this.repositories = repositories;
    }
    @Override
    public GHRepository read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!repositories.isEmpty()) {
            return repositories.removeFirst();
        }
        return null;
    }
}
