package org.varamadon.autorefactor.server.agent.memory;

import org.varamadon.autorefactor.server.agent.brain.BrainMessage;

/** This record provides necessary information to process a {@link BrainMessage}. */
public record MessageContext(String repositoryId, String fileHash) {}
