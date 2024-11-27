package org.varamadon.autorefactor.server.agent.command;

import java.util.Optional;

/** Interface for managing tool URLs associated with repositories. */
public interface ToolsInfoStore {
  /** Registers a tool URL for the given repository ID. */
  void registerToolUrl(String repositoryId, String toolUrl);

  /** Retrieves the tool URL associated with the given repository ID. */
  Optional<String> getToolUrl(String repositoryId);

  /** Removes the tool URL associated with the given repository ID. */
  void removeToolUrl(String repositoryId);
}
