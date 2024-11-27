package org.varamadon.autorefactor.server.agent.memory;

import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.tuple.Pair;
import org.varamadon.autorefactor.server.agent.brain.BrainMessage;

/** Interface for managing the storage and retrieval of messages. */
public interface RefactoringAgentMessagesStore {
  /** Stores a new message for the given repository. */
  void storeMessage(String repositoryId, BrainMessage message);

  /** Retrieves all stored messages for the given repository. */
  ImmutableList<BrainMessage> getMessages(String repositoryId);

  /** Deletes all stored messages for the given repository. */
  void deleteMessages(String repositoryId);

  /** Stores the next message that needs to be processed with its context. */
  void storePendingMessageWithContext(Pair<BrainMessage, MessageContext> message);

  /** Returns the next pending message along with its context or blocks until one is available. */
  Pair<BrainMessage, MessageContext> awaitNextPendingMessageWithContext() throws InterruptedException;
}
