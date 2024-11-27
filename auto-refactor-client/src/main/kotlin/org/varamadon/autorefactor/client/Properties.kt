package org.varamadon.autorefactor.client

import java.util.*

object Properties {
    private val resourceBundle: ResourceBundle = ResourceBundle.getBundle("application")
    /**
     * Url of the server to start the process
     */
    val serverUrl: String =
        resourceBundle.getString("org.varamadon.autorefactor.server.url")
    /**
     * Server will call the client on [toolsHost]:[toolsPort] for commands execution
     */
    val toolsHost: String =
        resourceBundle.getString("org.varamadon.autorefactor.tools.host")
    val toolsPort: Int =
        resourceBundle.getString("org.varamadon.autorefactor.tools.port").toInt()
}