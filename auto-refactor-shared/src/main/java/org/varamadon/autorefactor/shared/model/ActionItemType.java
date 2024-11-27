package org.varamadon.autorefactor.shared.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ActionItemType {
    @JsonProperty("addComment")
    ADD_COMMENT,
    @JsonProperty("renameMethod")
    RENAME_METHOD,
    @JsonProperty("renameVariable")
    RENAME_VARIABLE
}
