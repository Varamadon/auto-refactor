package org.varamadon.autorefactor.shared.model;

import static org.varamadon.autorefactor.shared.model.ActionItemType.ADD_COMMENT;

import com.fasterxml.jackson.annotation.JsonIgnore;

public record AddCommentAction(Integer line, String content) implements ActionItem {
  @Override
  @JsonIgnore
  public ActionItemType getType() {
    return ADD_COMMENT;
  }
}
