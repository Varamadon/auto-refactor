package org.varamadon.autorefactor.shared.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(property = "type", use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
  @JsonSubTypes.Type(name = "addComment", value = AddCommentAction.class),
  @JsonSubTypes.Type(name = "renameMethod", value = RenameMethodAction.class),
  @JsonSubTypes.Type(name = "renameVariable", value = RenameVariableAction.class)
})
public sealed interface ActionItem
    permits AddCommentAction, RenameMethodAction, RenameVariableAction {
  ActionItemType getType();
}
