package org.varamadon.autorefactor.server.agent.brain;

import com.google.common.collect.ImmutableList;

/**
 * Interface for an agent brain, responsible for processing messages
 * and providing responses within the system.
 * <p>
 * Implementations of this interface should typically call some LLM for generating
 * answers.
 * </p>
 */
public interface AgentBrain {
  /**
   * Retrieves the next answer based on the provided conversation history.
   *
   * @param messages an {@link ImmutableList} of {@link BrainMessage} representing the message history.
   * @return a {@link BrainMessage} containing the generated response.
   */
  BrainMessage getNextAnswer(ImmutableList<BrainMessage> messages);

  /**
   * Provides the initial system message to be used when starting a new process.
   * This message should be tweaked differently for different models,
   * so that's why it should be provided by the implementation.
   *
   * @return a {@link BrainMessage} containing the system's initial instructions.
   */
  BrainMessage getSystemStartMessage();
}
