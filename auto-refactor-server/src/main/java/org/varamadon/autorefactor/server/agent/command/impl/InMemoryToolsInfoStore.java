package org.varamadon.autorefactor.server.agent.command.impl;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;
import org.varamadon.autorefactor.server.agent.command.ToolsInfoStore;

/**
 * Implementation of {@link ToolsInfoStore} that uses an in-memory map to store the information.
 * Should be replaced with a persistent storage mechanism to enable fault tolerance.
 */
@Component
public class InMemoryToolsInfoStore implements ToolsInfoStore {
    private final ConcurrentMap<String, String> toolUrls = new ConcurrentHashMap<>();

    @Override
    public void registerToolUrl(String repositoryId, String toolUrl) {
        toolUrls.put(repositoryId, toolUrl);
    }

    @Override
    public Optional<String> getToolUrl(String repositoryId) {
        String toolUrl = toolUrls.get(repositoryId);
        return Optional.ofNullable(toolUrl);
    }

    @Override
    public void removeToolUrl(String repositoryId) {
        toolUrls.remove(repositoryId);
    }
}
