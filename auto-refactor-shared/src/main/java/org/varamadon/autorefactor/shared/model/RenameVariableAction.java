package org.varamadon.autorefactor.shared.model;

import static org.varamadon.autorefactor.shared.model.ActionItemType.RENAME_VARIABLE;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RenameVariableAction(Integer line, String oldName, String newName)
    implements ActionItem {
  @Override
  @JsonIgnore
  public ActionItemType getType() {
    return RENAME_VARIABLE;
  }
}
