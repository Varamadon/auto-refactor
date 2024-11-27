package org.varamadon.autorefactor.server.web;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.varamadon.autorefactor.server.agent.RefactoringAgent;
import org.varamadon.autorefactor.server.agent.command.ToolsInfoStore;

@RestController
public class AgentController {
  private final RefactoringAgent refactoringAgent;
  private final ToolsInfoStore toolsInfoStore;

  public AgentController(
      RefactoringAgent refactoringAgent,
      ToolsInfoStore toolsInfoStore) {
    this.refactoringAgent = refactoringAgent;
    this.toolsInfoStore = toolsInfoStore;
  }

  /**
   * Triggers the refactoring process for a repository identified by the given ID.
   * Registers the tool URL and starts the process.
   *
   * @param repositoryId the ID of the repository
   * @param toolUrl the URL of the refactoring tool
   */
  @PostMapping("/refactor/{repositoryId}/start")
  public void startRefactoringProcess(
      @PathVariable String repositoryId, @RequestBody String toolUrl) {
    toolsInfoStore.registerToolUrl(repositoryId, toolUrl);
    refactoringAgent.startProcess(repositoryId);
  }
}
