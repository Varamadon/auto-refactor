package org.varamadon.autorefactor.server.agent.brain;

public record BrainMessage(BrainMessageType messageType, String content) {}
