package org.varamadon.autorefactor.client.refactoring.executor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.RenameProcessor
import org.varamadon.autorefactor.client.refactoring.ChangesCollector
import org.varamadon.autorefactor.shared.model.*
import java.util.*

/**
 * Handles the execution of refactoring actions on files.
 * Supports adding comments, renaming variables, and renaming methods.
 */
class RefactoringExecutor {
    private val log = Logger.getInstance(javaClass)

    /**
     * Executes a specified refactoring action on a given file within a project.
     *
     * @param virtualFile The file where the refactoring action should be applied.
     * @param project The IntelliJ project context.
     * @param actionItem The refactoring action to be executed.
     */
    fun execute(virtualFile: VirtualFile, project: Project, actionItem: ActionItem) {
        log.warn("Executing action plan item: $actionItem in file ${virtualFile.name}")
        val psiFile = runReadAction {
            val file = PsiManager.getInstance(project).findFile(virtualFile)
            checkNotNull(file)
        }
        val document = runReadAction {
            val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
            checkNotNull(document)
        }
        var actionExecuted = false
        @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
        when (actionItem.getType()) {
            ActionItemType.ADD_COMMENT -> {
                executeAddComment(actionItem, project, document) { actionExecuted = true }
            }

            ActionItemType.RENAME_VARIABLE -> {
                executeRenameVariable(actionItem, psiFile, project) { actionExecuted = true }
            }

            ActionItemType.RENAME_METHOD -> {
                executeRenameMethod(actionItem, psiFile, project) { actionExecuted = true }
            }
        }
        runWriteCommandAction(project) {
            FileDocumentManager.getInstance().saveAllDocuments()
            PsiDocumentManager.getInstance(project).commitDocument(document)
        }
        if (actionExecuted) {
            ChangesCollector.recordActionItem(virtualFile.name, actionItem)
        }
    }

    private fun executeAddComment(
        actionItem: ActionItem,
        project: Project,
        document: Document,
        registerActionExecuted: () -> Unit
    ) {
        val addCommentAction: AddCommentAction = actionItem as AddCommentAction
        runWriteCommandAction(
            project
        ) {
            document.insertString(
                document.getLineEndOffset(addCommentAction.line - 1),
                " // " + addCommentAction.content
            )
            registerActionExecuted()
        }
    }

    private fun executeRenameVariable(
        actionItem: ActionItem,
        psiFile: PsiFile?,
        project: Project,
        registerActionExecuted: () -> Unit
    ) {
        val renameAction = actionItem as RenameVariableAction
        renameElement(
            psiFile,
            renameAction.line,
            renameAction.oldName,
            renameAction.newName,
            PsiVariable::class.java,
            project,
            registerActionExecuted
        )
    }

    private fun executeRenameMethod(
        actionItem: ActionItem,
        psiFile: PsiFile?,
        project: Project,
        registerActionExecuted: () -> Unit
    ) {
        val renameAction = actionItem as RenameMethodAction
        renameElement(
            psiFile,
            renameAction.line,
            renameAction.oldName,
            renameAction.newName,
            PsiMethod::class.java,
            project,
            registerActionExecuted
        )
    }

    private fun renameElement(
        psiFile: PsiFile?,
        lineNumber: Int,
        oldName: String,
        newName: String,
        elementType: Class<out PsiNamedElement>,
        project: Project,
        registerActionExecuted: () -> Unit
    ) {
        val psiVariable: PsiNamedElement? = runReadAction {
            getPsiElementOnLineByTypeAndName(
                psiFile!!,
                lineNumber,
                elementType,
                oldName,
                project
            )
        }
        psiVariable?.let {
            executeRenameElement(
                it,
                project,
                newName,
                registerActionExecuted
            )
        }
    }

    private fun executeRenameElement(
        element: PsiNamedElement,
        project: Project,
        newName: String,
        registerActionExecuted: () -> Unit
    ) {
        // This produces exceptions because RenameProcessor says it should not be started in a write action,
        // but when started outside of one, it fails with an exception.
        runWriteCommandAction(project) {
            RenameProcessor(project, element, newName, false, false).run()
            registerActionExecuted()
        }
    }


    /**
     * Retrieves a PSI element of a specific type and name on a given line.
     *
     * @return The found PSI element or null if not found.
     */
    private fun getPsiElementOnLineByTypeAndName(
        psiFile: PsiFile, lineNumber: Int, elementType: Class<out PsiNamedElement>, name: String, project: Project
    ): PsiNamedElement? {
        return getPsiElementsOnLine(psiFile, lineNumber, project)
            .asSequence()
            .filterIsInstance<PsiIdentifier>()
            .map { element -> PsiTreeUtil.getParentOfType(element, elementType) }
            .filterNotNull()
            .distinct()
            .filter { psiElement -> name == psiElement.name }
            .firstOrNull()
    }

    private fun getPsiElementsOnLine(psiFile: PsiFile, lineNumber: Int, project: Project): List<PsiElement> {
        val document: Document? = PsiDocumentManager.getInstance(project).getDocument(psiFile)
        if (document == null || lineNumber < 0 || lineNumber >= document.lineCount) {
            return ArrayList<PsiElement>()
        }

        val lineStartOffset: Int = document.getLineStartOffset(lineNumber - 1)
        val lineEndOffset: Int = document.getLineEndOffset(lineNumber - 1)

        val startElement: PsiElement = psiFile.findElementAt(lineStartOffset) ?: return ArrayList<PsiElement>()

        val elements: MutableList<PsiElement> = ArrayList<PsiElement>()
        var currentElement: PsiElement? = startElement
        while (currentElement != null && currentElement.textOffset <= lineEndOffset) {
            elements.add(currentElement)
            currentElement = PsiTreeUtil.nextLeaf(currentElement)
        }
        log.debug("Elements on the line: $elements")
        return elements
    }
}
