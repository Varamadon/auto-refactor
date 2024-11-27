package org.varamadon.autorefactor.server.agent;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;
import static org.varamadon.autorefactor.server.agent.brain.BrainMessageType.ASSISTANT;
import static org.varamadon.autorefactor.server.agent.brain.BrainMessageType.SYSTEM;
import static org.varamadon.autorefactor.server.agent.brain.BrainMessageType.USER;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Hashing;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.varamadon.autorefactor.server.agent.brain.AgentBrain;
import org.varamadon.autorefactor.server.agent.brain.BrainMessage;
import org.varamadon.autorefactor.server.agent.memory.RefactoringAgentMessagesStore;
import org.varamadon.autorefactor.server.agent.command.RefactoringAgentCommandExecutor;
import org.varamadon.autorefactor.server.agent.memory.impl.InMemoryMessagesStore;
import org.varamadon.autorefactor.shared.model.ActionItem;
import org.varamadon.autorefactor.shared.model.ActionPlan;

import org.varamadon.autorefactor.shared.model.AddCommentAction;

class RefactoringAgentTest {

  @Test
  void test() {
    AgentBrain agentBrain = mock();
    RefactoringAgentCommandExecutor commandExecutor = mock();
    RefactoringAgentMessagesStore messagesStore = new InMemoryMessagesStore();

    String fileContent = "fileContent";
    String repositoryId = "repositoryId";
    String fileContentWithLines = RefactoringAgent.appendLineNumbers(fileContent);
    when(commandExecutor.executeNextFileCommand(repositoryId))
        .thenReturn(fileContent)
        .thenReturn("");

    ActionItem actionItem = new AddCommentAction(5, "comment");
    ActionPlan actionPlan =
        new ActionPlan(
            Hashing.sha256().hashString(fileContent, UTF_8).toString(),
            ImmutableList.of(actionItem));
    ImmutableList<ActionItem> actionItems = ImmutableList.of(actionItem);
    String serializedActionItems = "items";

    ObjectMapper objectMapper = new MockObjectMapper(actionItems);

    BrainMessage systemMessage = new BrainMessage(SYSTEM, "start"); // initial system prompt
    BrainMessage nextFileMessage = new BrainMessage(ASSISTANT, "nextFile"); // next file command
    BrainMessage userFileMessage = new BrainMessage(USER, fileContentWithLines); // user sends file
    BrainMessage actionPlanMessage =
        new BrainMessage(ASSISTANT, serializedActionItems); // action plan
    BrainMessage userEmptyFileMessage =
        new BrainMessage(USER, ""); // user sends empty file
    BrainMessage finishMessage = new BrainMessage(ASSISTANT, "finish"); // finish command

    // Simulate LLM behaviour
    when(agentBrain.getSystemStartMessage()).thenReturn(systemMessage);
    when(agentBrain.getNextAnswer(ImmutableList.of(systemMessage)))
        .thenReturn(nextFileMessage); // request next file
    when(agentBrain.getNextAnswer(
            ImmutableList.of(systemMessage, nextFileMessage, userFileMessage)))
        .thenReturn(actionPlanMessage); // return action plan
    when(agentBrain.getNextAnswer(
            ImmutableList.of(systemMessage, nextFileMessage, userFileMessage, actionPlanMessage)))
        .thenReturn(nextFileMessage); // request next file
    when(agentBrain.getNextAnswer(
            ImmutableList.of(
                systemMessage,
                nextFileMessage,
                userFileMessage,
                actionPlanMessage,
                nextFileMessage,
                userEmptyFileMessage)))
        .thenReturn(finishMessage); // got empty file, finish

    RefactoringAgent agent =
        new RefactoringAgent(agentBrain, commandExecutor, messagesStore, objectMapper);

    agent.startProcess(repositoryId);

    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              verify(commandExecutor, times(2)).executeNextFileCommand(repositoryId);
              verify(commandExecutor).executeActionPlanCommand(repositoryId, actionPlan);
              verify(commandExecutor).executeFinishCommand(repositoryId);
            });
  }

  static class MockObjectMapper extends ObjectMapper {
    private final ImmutableList<ActionItem> actionItems;

    public MockObjectMapper(ImmutableList<ActionItem> actionItems) {
      this.actionItems = actionItems;
    }

    @Override
    public <T> T readValue(String content, TypeReference<T> valueTypeRef) {
      _assertNotNull("content", content);
      //noinspection unchecked
      return (T) actionItems;
    }
  }
}
