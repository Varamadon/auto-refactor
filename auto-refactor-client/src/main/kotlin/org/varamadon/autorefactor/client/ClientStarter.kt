package org.varamadon.autorefactor.client

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil.getOpenProjects
import com.intellij.ide.impl.ProjectUtil.openOrImport
import com.intellij.openapi.application.ApplicationStarter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.util.*

/**
 * ClientStarter is an application starter that handles the initialization and setup of a client project.
 * It checks for necessary arguments, clones a repository if required, and opens or imports the project.
 * If the client is run as a full plugin this class will not get called.
 */
class ClientStarter : ApplicationStarter {
    private val log: Logger = Logger.getInstance(ClientStarter::class.java)

    override val isHeadless: Boolean
        get() = true

    override fun main(args: List<String>) {
        if (args.size < 2) {
            log.error("ProjectLocalPath must be provided")
            return
        }
        val projectLocalPath = args[1]
        if (args.size > 2) {
            if (args.size < 5) {
                log.error("Username and accessToken must be provided")
                return
            }
            val repositoryUrl = args[2]
            val username = args[3]
            val accessToken = args[4]
            cloneRepository(
                projectLocalPath,
                repositoryUrl,
                username,
                accessToken
            )
        }
        val projectRoot = File(projectLocalPath)
        Arrays.stream(getOpenProjects())
            .forEach { project: Project -> ProjectManager.getInstance().closeAndDispose(project) }
        val task = OpenProjectTask.build()
        val project = openOrImport(projectRoot.toPath(), task)
        checkNotNull(project) { "Failed to open the project." }
    }

    private fun cloneRepository(targetDir: String, repoUrl: String, username: String, token: String) {
        try {
            Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(File(targetDir))
                .setCredentialsProvider(UsernamePasswordCredentialsProvider(username, token))
                .call()

            log.warn("Repository cloned successfully into $targetDir")
        } catch (e: Exception) {
            log.error("${e.message}")
        }
    }
}
