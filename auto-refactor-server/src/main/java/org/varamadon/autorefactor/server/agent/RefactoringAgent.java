package org.varamadon.autorefactor.server.agent;

import static org.varamadon.autorefactor.server.agent.brain.BrainMessageType.USER;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.varamadon.autorefactor.server.agent.brain.AgentBrain;
import org.varamadon.autorefactor.server.agent.brain.BrainMessage;
import org.varamadon.autorefactor.server.agent.memory.MessageContext;
import org.varamadon.autorefactor.server.agent.memory.RefactoringAgentMessagesStore;
import org.varamadon.autorefactor.server.agent.command.RefactoringAgentCommandExecutor;
import org.varamadon.autorefactor.shared.model.ActionItem;
import org.varamadon.autorefactor.shared.model.ActionPlan;

/**
 * RefactoringAgent is the main component responsible for orchestrating the process of analyzing and
 * refactoring code repositories. It communicates with an AI brain, processes messages, executes
 * commands, and maintains the state of the refactoring process.
 *
 * <p>It calls an {@link AgentBrain} for getting the commands, uses a {@link
 * RefactoringAgentCommandExecutor} for executing them, and a {@link RefactoringAgentMessagesStore}
 * for managing message persistence and state.
 */
@Component
public class RefactoringAgent {
  private final Logger log = LoggerFactory.getLogger(RefactoringAgent.class);

  private final AgentBrain agentBrain;
  private final RefactoringAgentCommandExecutor commandExecutor;
  private final RefactoringAgentMessagesStore messagesStore;
  private final ObjectMapper objectMapper;

  /**
   * Thread pool used for listening to the message queue and scheduling the processing of assistant
   * messages.
   */
  private final ExecutorService queueListeningService = Executors.newSingleThreadExecutor();

  /** Thread pool used for processing assistant messages. */
  private final ExecutorService messagesProcessingService = Executors.newSingleThreadExecutor();

  /**
   * Creates a new instance of the RefactoringAgent.
   *
   * @param agentBrain the AI component responsible for generating refactoring actions.
   * @param commandExecutor the executor that handles command execution for refactoring tasks.
   * @param messagesStore the store managing message persistence and state.
   * @param objectMapper the JSON mapper for serializing and deserializing messages.
   */
  public RefactoringAgent(
      AgentBrain agentBrain,
      RefactoringAgentCommandExecutor commandExecutor,
      RefactoringAgentMessagesStore messagesStore,
      ObjectMapper objectMapper) {
    this.agentBrain = agentBrain;
    this.commandExecutor = commandExecutor;
    this.messagesStore = messagesStore;
    this.objectMapper = objectMapper;
    startProcessingAssistantMessages();
  }

  /**
   * Starts the refactoring process for a given repository. Initializes the message queue and
   * invokes the AI brain to process the repository.
   *
   * @param repositoryId the unique identifier of the repository to process.
   */
  public void startProcess(String repositoryId) {
    messagesProcessingService.submit(
        () -> {
          log.info("Starting process for repository {}", repositoryId);
          messagesStore.storeMessage(repositoryId, agentBrain.getSystemStartMessage());
          callBrain(repositoryId, "");
        });
  }

  /**
   * Starts the thread responsible for listening to the message queue and scheduling the processing
   * of any pending assistant messages. This method runs indefinitely until interrupted.
   */
  private void startProcessingAssistantMessages() {
    queueListeningService.submit(
        () -> {
          //noinspection InfiniteLoopStatement
          while (true) {
            processNextAssistantMessage();
          }
        });
  }

  private void processNextAssistantMessage() throws InterruptedException {
    Pair<BrainMessage, MessageContext> nextMessage =
        messagesStore.awaitNextPendingMessageWithContext();
    log.debug("Processing next assistant message");
    messagesProcessingService.submit(
        () -> {
          try {
            handleAssistantMessage(
                nextMessage.getKey(),
                nextMessage.getValue().repositoryId(),
                nextMessage.getValue().fileHash());
          } catch (Exception e) {
            log.error("Error processing assistant message", e);
            finishProcess(nextMessage.getValue().repositoryId());
          }
        });
  }

  /**
   * Handles incoming messages from the assistant and executes corresponding actions.
   *
   * @param message The BrainMessage object containing the message content.
   * @param repositoryId The identifier for the repository being processed.
   * @param currentFileHash The hash of the current file being processed. If it's blank, it means
   *     next file should be requested or the process has finished.
   */
  private void handleAssistantMessage(
      BrainMessage message, String repositoryId, String currentFileHash) {
    String messageContent = message.content();
    log.debug("Got message content {}", messageContent);
    if (Objects.equals(messageContent, "finish")) {
      finishProcess(repositoryId);
      return;
    }
    if (Objects.equals(messageContent, "nextFile")) {
      executeNextFileCommand(repositoryId);
    } else { // Must be an action plan
      if (currentFileHash.isBlank()) {
        // Either there is inconsistency between the brain and the server, or the brain sent an
        // unknown command
        throw new IllegalStateException("Got action plan for unknown file hash or unknown command");
      }
      ImmutableList<ActionItem> actionItems = deserializeActionPlan(messageContent);
      ActionPlan actionPlan = new ActionPlan(currentFileHash, actionItems);
      executeActionPlanCommand(repositoryId, actionPlan);
    }
  }

  private void executeNextFileCommand(String repositoryId) {
    log.info("Executing next file command for repository {}", repositoryId);
    String fileContent = commandExecutor.executeNextFileCommand(repositoryId);
    String fileHash = Hashing.sha256().hashString(fileContent, StandardCharsets.UTF_8).toString();
    messagesStore.storeMessage(repositoryId, getUserNextFileMessage(fileContent));
    callBrain(repositoryId, fileHash);
  }

  private void executeActionPlanCommand(String repositoryId, ActionPlan actionPlan) {
    log.info("Executing action plan for repository {}", repositoryId);
    commandExecutor.executeActionPlanCommand(repositoryId, actionPlan);
    callBrain(repositoryId, "");
  }

  private void callBrain(String repositoryId, String fileHash) {
    ImmutableList<BrainMessage> messages = messagesStore.getMessages(repositoryId);
    BrainMessage message = agentBrain.getNextAnswer(messages);
    messagesStore.storeMessage(repositoryId, message);
    messagesStore.storePendingMessageWithContext(
        Pair.of(message, new MessageContext(repositoryId, fileHash)));
  }

  private void finishProcess(String repositoryId) {
    log.info("Finishing process for repository {}", repositoryId);
    messagesStore.deleteMessages(repositoryId);
    commandExecutor.executeFinishCommand(repositoryId);
  }

  private ImmutableList<ActionItem> deserializeActionPlan(String plan) {
    try {
      List<ActionItem> actionPlan = objectMapper.readValue(plan, new TypeReference<>() {});
      return ImmutableList.copyOf(actionPlan);
    } catch (JsonProcessingException e) {
      log.error("Brain provided action plan in the wrong format, returning empty plan");
      return ImmutableList.of();
    }
  }

  private BrainMessage getUserNextFileMessage(String fileContent) {
    return new BrainMessage(USER, appendLineNumbers(fileContent));
  }

  /**
   * Adds line numbers to the provided file content. Each line will be prefixed with its line
   * number, followed by " | " for separation, making it easier to reference specific lines.
   *
   * <p>Example:
   *
   * <pre>
   * Input:
   * Hello
   * World
   *
   * Output:
   * 1 | Hello
   * 2 | World
   * </pre>
   */
  @VisibleForTesting
  static String appendLineNumbers(String fileContent) {
    if (fileContent == null || fileContent.isEmpty()) {
      return "";
    }
    String[] lines = fileContent.split("\\R");
    StringBuilder result = new StringBuilder(fileContent.length() + lines.length * 5);
    for (int i = 0; i < lines.length; i++) {
      result.append(i + 1).append(" | ").append(lines[i]).append(System.lineSeparator());
    }
    return result.toString();
  }
}
