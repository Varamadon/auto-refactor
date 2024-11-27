package org.varamadon.autorefactor.server.agent.brain.chatgpt;

import static org.varamadon.autorefactor.server.agent.brain.BrainMessageType.ASSISTANT;
import static org.varamadon.autorefactor.server.agent.brain.BrainMessageType.SYSTEM;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.varamadon.autorefactor.server.agent.brain.AgentBrain;
import org.varamadon.autorefactor.server.agent.brain.BrainMessage;

/**
 * Implementation of the {@link AgentBrain} interface using ChatGPT as the backend brain.
 * <p>
 * This class handles communication with ChatGPT.
 * It defines system start messages and provides mechanisms for interacting
 * with ChatGPT to generate structured responses.
 * </p>
 */
@Component
public class ChatGPTAgentBrain implements AgentBrain {
  private final Logger log = LoggerFactory.getLogger(ChatGPTAgentBrain.class);
  private final ChatClient chatClient;

  public ChatGPTAgentBrain(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  @Override
  public BrainMessage getNextAnswer(ImmutableList<BrainMessage> messages) {
    try {
      log.debug("Calling chat gpt");
      ChatClient.CallResponseSpec responseSpec =
          chatClient.prompt().messages(transformMessages(messages)).call();
      AssistantMessage message = responseSpec.chatResponse().getResult().getOutput();
      log.debug("Got response: {}", message.getContent());
      return new BrainMessage(ASSISTANT, message.getContent());
    } catch (Exception e) {
      log.error("Call to chat gpt failed", e);
      return new BrainMessage(ASSISTANT, "finish");
    }
  }

  @Override
  public BrainMessage getSystemStartMessage() {
    String content =
        """
                        You are a great java developer.
                        You have the following commands under your disposal:
                        "nextFile" - gives you the next file to analyze,"finish" to finish the process
                        and three commands-refactorings:
                        "addComment", "renameMethod", "renameVariable".
                        You need to ask a user for files one by one using the "nextFile" command,
                        until the user input is empty, then respond with "finish" command.
                        When you get a file suggest an action plan of appropriate refactorings, format it as json.
                        Use specific line numbers for refactorings.
                        You can only respond with a json action plan, "nextFile" or "finish" command.
                        Use specific line numbers for refactorings.
                        You will receive java files with line numbers appended.
                        Omit "```json" in the beginning and "```" in the end.
                        Omit quotes for "nextFile" and "finish" commands.
                        Give "nextFile" command to start the process.
                        You should format your action plan as in this example:
                        [
                          {
                            "type": "addComment",
                            "line": 3,
                            "content": "Calculate the discriminant"
                          },
                          {
                            "type": "renameVariable",
                            "line": 4,
                            "oldName": "d",
                            "newName": "discriminant"
                          },
                          {
                            "type": "renameVariable",
                            "line": 5,
                            "oldName": "sol1",
                            "newName": "root1"
                          },
                          {
                            "type": "renameVariable",
                            "line": 6,
                            "oldName": "sol2",
                            "newName": "root2"
                          },
                          {
                            "type": "renameMethod",
                            "line": 10,
                            "oldName": "calc",
                            "newName": "calculateDiscriminant"
                          }
                        ]
                        """;
    return new BrainMessage(SYSTEM, content);
  }

  private ImmutableList<Message> transformMessages(ImmutableList<BrainMessage> messages) {
    return messages.stream().map(this::transformMessage).collect(ImmutableList.toImmutableList());
  }

  private Message transformMessage(BrainMessage message) {
    return switch (message.messageType()) {
      case SYSTEM -> new SystemMessage(message.content());
      case USER -> new UserMessage(message.content());
      case ASSISTANT -> new AssistantMessage(message.content());
    };
  }
}
