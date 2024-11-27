package org.varamadon.autorefactor.shared.model;

import java.util.List;

public record ActionPlan(String fileHash, List<ActionItem> actionItems) {}
