package org.varamadon.autorefactor.server.agent.command.impl;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.varamadon.autorefactor.server.agent.command.RefactoringAgentCommandExecutor;
import org.varamadon.autorefactor.server.agent.command.ToolsInfoStore;
import org.varamadon.autorefactor.shared.model.ActionPlan;

@Component
public class CommandExecutorImpl implements RefactoringAgentCommandExecutor {
  private final Logger log = LoggerFactory.getLogger(CommandExecutorImpl.class);
  private final RestTemplate restTemplate;
  private final ToolsInfoStore toolsInfoStore;

  public CommandExecutorImpl(RestTemplate restTemplate, ToolsInfoStore toolsInfoStore) {
    this.restTemplate = restTemplate;
    this.toolsInfoStore = toolsInfoStore;
  }

  @Override
  public String executeNextFileCommand(String repositoryId) {
    log.debug("Executing next file command");
    ResponseEntity<String> response =
        restTemplate.getForEntity(getToolUrl(repositoryId) + "/files/next", String.class);
    String body = response.getBody();

    return body == null ? "" : body;
  }

  @Override
  public void executeActionPlanCommand(String repositoryId, ActionPlan actionPlan) {
    log.debug("Executing action plan command");
    restTemplate.postForEntity(
            getToolUrl(repositoryId) + "/actions/execute", actionPlan, String.class);
  }

  @Override
  public void executeFinishCommand(String repositoryId) {
    log.debug("Executing finish command");
    Optional<String> url = toolsInfoStore.getToolUrl(repositoryId);
    toolsInfoStore.removeToolUrl(repositoryId);
    url.ifPresent(it -> restTemplate.postForEntity(it + "/finish", "", String.class));
  }

  private String getToolUrl(String repositoryId) {
    return toolsInfoStore
        .getToolUrl(repositoryId)
        .orElseThrow(
            () -> new IllegalStateException("Tool url not found for repository " + repositoryId));
  }
}
