package org.varamadon.autorefactor.client.refactoring.agent.tool

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.google.common.hash.Hashing
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.varamadon.autorefactor.client.refactoring.ChangesCollector
import org.varamadon.autorefactor.client.refactoring.executor.RefactoringExecutor
import org.varamadon.autorefactor.shared.model.ActionPlan
import java.nio.charset.StandardCharsets

/**
 * AgentToolController is responsible for managing the communication between the refactoring agent
 * and the client. It serves files to the agent, processes action plans received from
 * the agent, and handles the finalization of the refactoring process.
 */
class AgentToolController(
    private val refactoringExecutor: RefactoringExecutor,
    private val objectMapper: ObjectMapper,
    private val project: Project,
    private val fileQueue: ArrayDeque<VirtualFile>,
    port: Int
) {
    private val filesByHash = mutableMapOf<String, VirtualFile>()

    init {
        embeddedServer(Netty, port = port) {
            routing {
                /**
                 * This endpoint returns the next file in the queue as a string.
                 *
                 * If there are no more files left in the queue, an empty response will be returned.
                 */
                get("/files/next") {
                    val file = fileQueue.removeFirstOrNull()
                    if (file == null) {
                        call.respondText("", ContentType.Text.Plain)
                        return@get
                    }
                    val fileContent = file.getContent()
                    val hash = Hashing.sha256()
                        .hashString(fileContent, StandardCharsets.UTF_8).toString()
                    filesByHash[hash] = file

                    call.respondText(fileContent, ContentType.Text.Plain)
                }

                /**
                 * This endpoint receives an action plan from the agent
                 * and executes each action item on the corresponding file.
                 *
                 * The action items can be renaming variables, adding comments, etc.
                 * which are processed by [refactoringExecutor].
                 * It processes any action items it can, errors are ignored.
                 *
                 * If the specified file is not found in the map of files by hash,
                 * an error message will be logged and a BadRequest response will be sent back to the agent.
                 */
                post("/actions/execute") {
                    val requestBody = call.receiveText()
                    val actionPlan = objectMapper.readValue<ActionPlan>(requestBody)
                    val file = filesByHash[actionPlan.fileHash]
                    if (file == null) {
                        this@embeddedServer.log.error("Got execute plan command, but current file is absent")
                        call.respond(HttpStatusCode.BadRequest, "ABSENT_FILE")
                        return@post
                    }
                    actionPlan.actionItems.forEach {
                        refactoringExecutor.execute(file, project, it)
                    }
                    call.respond(HttpStatusCode.OK)
                }

                /**
                 * This endpoint signals that all changes have been made
                 * and the refactoring process should finish.
                 * It prints out executed actions and exits the application if running in headless mode.
                 */
                post("/finish") {
                    call.respond(HttpStatusCode.OK)
                    printExecutedActions()
                    exitIfHeadless()
                }
            }
        }.start(wait = false)
    }

    private fun VirtualFile.getContent(): String {
        var document: Document? = null
        runReadAction {
            document = FileDocumentManager.getInstance().getDocument(this)
        }
        return document?.text.orEmpty()
    }

    private fun printExecutedActions() {
        println("Executed actions:")
        ChangesCollector.getRecordedActionsWithFileName().forEach {
            println("In file ${it.first} ${it.second}")
        }
    }

    private fun exitIfHeadless() {
        if (ApplicationManager.getApplication().isHeadlessEnvironment) {
            invokeLater {
                ProjectManager.getInstance().closeAndDispose(project)
                ApplicationManager.getApplication().exit(false, true, false)
            }
        }
    }
}
