# Auto-Refactor

Auto-Refactor is a project designed to automate code refactoring by leveraging an LLM agent. The project is composed of
three main modules:

- **Shared Module**: Contains models used for client-server communication.
- **Server**: Hosts the agent logic and web interfaces for interacting with the LLM and the client.
- **Client**: Integrates with IntelliJ Platform to perform refactoring tasks.

---

## Project Architecture

### Shared Module

The shared module includes the communication models used by both the server and the client to ensure seamless
interaction.

### Server

The server handles the orchestration of the refactoring process. Key components include:

- **RefactoringAgent**: The main orchestrator of the refactoring process.
    - **AgentBrain**: Generates instructions using ChatGPT (GPT-4o by default).
    - **RefactoringAgentMessagesStore**: Manages messaging history and pending messages. Current implementation uses in-memory data structures.
    - **RefactoringAgentCommandExecutor**: Executes commands by calling the client via HTTP.

The server exposes an endpoint to register clients and initiate the refactoring process.

### Client

The client is responsible for:

- Starting a headless IntelliJ instance.
- Cloning the repository if needed.
- Triggering the server to start the refactoring process.
- Exposing endpoints for executing agent commands.

The client uses the IntelliJ Platform to execute refactoring commands.

---

## Run Instructions

### Prerequisites

Ensure you have Java 21 and an OpenAI API key. Additionally, make sure you have Docker installed if you plan
to use it for running the server.

### Building the Project

Run the following command to build the project:

```bash
./gradlew clean build
```

### Running the Server

Using Docker:

```bash
docker run -p 8080:8080 -e SPRING_AI_OPENAI_API_KEY={your-token} varamadon/auto-refactor-server:latest
```

Without Docker:

1. Add your OpenAI API token to `auto-refactor-server/src/main/resources/application.properties` using the property
   `spring.ai.openai.api-key`. You also need to do that in order to run `@SpringBootTest` tests.
2. GPT-4o is set as the default but can be changed in the same properties file.
3. Update `org.varamadon.autorefactor.tools.host` in `auto-refactor-client/src/main/resources/application.properties` to
   `http://localhost`.
4. Start the server using:

```bash
./gradlew :auto-refactor-server:bootRun
```

### Running the Client

Start the client using the following command:

```bash
./gradlew runAutorefactor \
  -PprojectLocalPath={path to the project directory or target location for cloning} \
  -PrepositoryUrl={optional Git repo URL in HTTPS format} \
  -Pusername={optional Git repo username} \
  -PaccessToken={optional Git repo access token}
```

---

## Continuation Plan

### Improvements and Extensions

1. **Alternative LLMs**:
    - Implement the `AgentBrain` interface to support other LLMs as the agent brain.

2. **Database Integration**:
    - Implement `RefactoringAgentMessagesStore` to persist data in a database, enabling the server to handle restarts
      and continue processes seamlessly.
    - Implement `ToolsInfoStore` to store registered tools information in a database for fault tolerance.

3. **Multi-Client Support**:
    - Server should support multiple clients simultaneously as is, but should be tested thoroughly.

### Testing

1. Extend test coverage to include:
    - Edge cases such as LLM generating bad output.
    - Web interactions and I/O operations.
    - Retry logic for LLM communication.
    - Refactoring functionality testing on the client.


### Client Enhancements

1. **UI Improvements**:
    - Develop a proper UI to allow the client to function as a full-featured IntelliJ plugin.

2. **Packaging and Deployment**:
    - Refactor the client to compile into a JAR file for easier packaging and containerization.

---

## Known Issues

1. **Transient Client Issues**:
    - The client may occasionally encounter transient issues. Restarting the client typically resolves these problems.

2. **Logging Levels**:
    - Some logs are currently set to the `warn` level instead of `info` for better visibility during development. This
      should be adjusted for production.

3. **Testing**:
    - No client-specific tests have been implemented due to dependencies on IntelliJ platform-specific APIs. These tests
      need to be designed and added.
