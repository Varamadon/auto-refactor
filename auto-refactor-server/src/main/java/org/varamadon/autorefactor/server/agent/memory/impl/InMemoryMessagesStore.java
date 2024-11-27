package org.varamadon.autorefactor.server.agent.memory.impl;

import com.google.common.collect.ImmutableList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiFunction;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;
import org.varamadon.autorefactor.server.agent.brain.BrainMessage;
import org.varamadon.autorefactor.server.agent.memory.MessageContext;
import org.varamadon.autorefactor.server.agent.memory.RefactoringAgentMessagesStore;

/**
 * Implementation of {@link RefactoringAgentMessagesStore} that stores data in memory. Should be
 * replaced with a persistent storage mechanism to enable fault tolerance.
 */
@Component
public class InMemoryMessagesStore implements RefactoringAgentMessagesStore {
  private final ConcurrentMap<String, ImmutableList<BrainMessage>> messages =
      new ConcurrentHashMap<>();
  private final BlockingQueue<Pair<BrainMessage, MessageContext>> pendingMessagesWithContext =
      new LinkedBlockingQueue<>();

  @Override
  public void storeMessage(String repositoryId, BrainMessage message) {
    messages.compute(repositoryId, addToStoredMessagesFunction(message));
  }

  @Override
  public ImmutableList<BrainMessage> getMessages(String repositoryId) {
    return messages.get(repositoryId);
  }

  @Override
  public void deleteMessages(String repositoryId) {
    messages.remove(repositoryId);
  }

  @Override
  public void storePendingMessageWithContext(Pair<BrainMessage, MessageContext> message) {
    pendingMessagesWithContext.add(message);
  }

  @Override
  public Pair<BrainMessage, MessageContext> awaitNextPendingMessageWithContext()
      throws InterruptedException {
    return pendingMessagesWithContext.take();
  }

  private BiFunction<String, ImmutableList<BrainMessage>, ImmutableList<BrainMessage>>
      addToStoredMessagesFunction(BrainMessage message) {
    return (ignored, storedMessages) -> copyMessagesWith(storedMessages, message);
  }

  private ImmutableList<BrainMessage> copyMessagesWith(
      ImmutableList<BrainMessage> messages, BrainMessage message) {
    if (messages == null) return ImmutableList.of(message);
    return new ImmutableList.Builder<BrainMessage>().addAll(messages).add(message).build();
  }
}
