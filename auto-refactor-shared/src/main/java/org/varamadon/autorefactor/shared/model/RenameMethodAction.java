package org.varamadon.autorefactor.shared.model;

import static org.varamadon.autorefactor.shared.model.ActionItemType.RENAME_METHOD;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record RenameMethodAction(Integer line, String oldName, String newName)
    implements ActionItem {
  @Override
  @JsonIgnore
  public ActionItemType getType() {
    return RENAME_METHOD;
  }
}
