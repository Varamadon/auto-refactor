package org.varamadon.autorefactor.server.agent.command;

import org.varamadon.autorefactor.shared.model.ActionPlan;

/** Component that executes commands from the refactoring agent. */
public interface RefactoringAgentCommandExecutor {
  /**
   * Executes a command for the next file in the given repository. Returns empty string if there are
   * no more files left to process.
   */
  String executeNextFileCommand(String repositoryId);

  /** Executes a command for applying the given action plan on the given repository. */
  void executeActionPlanCommand(String repositoryId, ActionPlan actionPlan);

  /** Executes a finish command for the given repository. */
  void executeFinishCommand(String repositoryId);
}
