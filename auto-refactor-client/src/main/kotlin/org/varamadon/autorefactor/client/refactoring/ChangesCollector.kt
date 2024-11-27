package org.varamadon.autorefactor.client.refactoring

import org.varamadon.autorefactor.shared.model.ActionItem

/**
 * Collects all the changes made by refactoring actions.
 */
object ChangesCollector {
    private val actionsWithFileName: MutableSet<Pair<String, ActionItem>> = HashSet()

    /**
     * Records an action item executed for a given file.
     */
    fun recordActionItem(fileName: String, action: ActionItem) {
        actionsWithFileName.add(fileName to action);
    }

    /**
     * Returns all recorded actions with their corresponding files.
     */
    fun getRecordedActionsWithFileName(): Set<Pair<String, ActionItem>> {
        return actionsWithFileName.toSet()
    }
}