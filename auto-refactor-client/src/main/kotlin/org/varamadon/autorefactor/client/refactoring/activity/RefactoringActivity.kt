package org.varamadon.autorefactor.client.refactoring.activity

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.varamadon.autorefactor.client.Properties
import org.varamadon.autorefactor.client.refactoring.executor.RefactoringExecutor
import org.varamadon.autorefactor.client.refactoring.agent.tool.AgentToolController
import java.io.IOException

/**
 * RefactoringActivity is a [ProjectActivity] responsible for initiating and coordinating
 * the refactoring process for a given project. It collects Java source files from the project's base directory,
 * initializes a [RefactoringExecutor] and [AgentToolController]
 * and triggers an HTTP request to initiate the server
 * to start the refactoring process.
 *
 * @see ProjectActivity
 */
class RefactoringActivity : ProjectActivity {
    private val log = Logger.getInstance(javaClass)

    override suspend fun execute(project: Project) {
        val projectFileIndex = ProjectRootManager.getInstance(project).fileIndex
        val projectBasePath = project.basePath
        checkNotNull(projectBasePath) { "Can't work with default project" }
        log.info("Starting for project ${project.name}")
        val projectBaseDir = LocalFileSystem.getInstance().findFileByPath(projectBasePath)
        checkNotNull(projectBaseDir) { "Can't find projectBaseDir" }
        val files = collectFilesToProcess(projectBaseDir, projectFileIndex)
        val executor = RefactoringExecutor()
        val objectMapper = ObjectMapper()

        DumbService.getInstance(project).runWhenSmart{
            log.warn("Starting agent tool controller")
            AgentToolController(executor, objectMapper, project, ArrayDeque(files), Properties.toolsPort)

            startProcess(project.name)
        }
    }

    private fun collectFilesToProcess(
        projectBaseDir: VirtualFile,
        projectFileIndex: ProjectFileIndex
    ): Set<VirtualFile> {
        val filesToProcess = mutableSetOf<VirtualFile>()
        log.warn("Base dir: ${projectBaseDir.name}")

        ApplicationManager.getApplication().runReadAction {
            VfsUtilCore.visitChildrenRecursively(projectBaseDir, object : VirtualFileVisitor<Any>() {
                override fun visitFile(file: VirtualFile): Boolean {
                    if (projectFileIndex.isExcluded(file)) {
                        return false
                    }
                    if (!file.isDirectory && file.extension == "java") {
                        filesToProcess.add(file)
                    }
                    return true
                }
            })
        }
        log.warn("Files to process: ${filesToProcess.toList()}")
        return filesToProcess
    }

    private fun startProcess(projectName: String) {
        log.warn("Making request to start the process")
        val client = OkHttpClient()

        // localhost should be changed for something real if server is hosted elsewhere
        val body = "${Properties.toolsHost}:${Properties.toolsPort}".toRequestBody()
        val request = Request.Builder()
            .url("${Properties.serverUrl}/refactor/$projectName/start")
            .post(body)
            .build()

        client.newCall(request).execute().use { response ->
            log.debug("Response: ${response.code}")
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
        }
    }
}