# Three-minute demo of this exact code

Before recording, open IntelliJ, the server and three client windows. Connect as Fatima Zahrae, Amine and a blank-username guest. Keep the code tabs below ready. Practise once with a timer.

| Time | What to show | What to say |
|---|---|---|
| 0:00-0:15 | Two Maven folders | “Hello, I am Fatima Zahrae. This is a group chat using Java TCP sockets and JavaFX. Multiple clients communicate through a central server.” |
| 0:15-0:35 | ChatServer.acceptLoop and executor field | “The ServerSocket accepts connections. The virtual-thread executor runs a ClientHandler for each accepted socket, so clients can communicate concurrently.” |
| 0:35-0:55 | ClientHandler.run and handle | “The first line is JOIN with the username. An empty name creates a read-only guest. The handler reads text lines and processes MSG, LIST or LEAVE. Ordinary messages receive a username and time before broadcasting.” |
| 0:55-1:15 | ChatConnection.dispatch; ChatClientApp callbacks | “The model receives server lines and converts them into listener events. The view uses Platform.runLater to update JavaFX controls. The model has no JavaFX imports.” |
| 1:15-1:30 | External properties and the two UML images | “Host and port come from runtime configuration files. Client arguments can override them. These diagrams show the classes and the server-client network deployment.” |
| 1:30-1:55 | Send from Fatima with SEND; send from Amine with Enter | “All connected clients receive the messages, with the server time and sender. The server lists the connected users with different random background colors.” |
| 1:55-2:15 | Guest receives messages; try guest chat text | “This blank-username connection is in READ-ONLY MODE. It can receive messages but cannot send ordinary chat text. The server also enforces the restriction.” |
| 2:15-2:30 | Type allUsers in Fatima's window | “Only the requester receives the active-user list. The client translates allUsers into the LIST protocol command.” |
| 2:30-2:50 | Type end in Amine; reconnect and type bye; guest uses LEAVE | “end, bye and LEAVE close the connection. The server removes disconnected users from its list.” |
| 2:50-3:00 | dist, README and GitHub | “The submission includes both Maven projects, executable JARs, UML diagrams, README, repository history and this demonstration.” |

## Understand these code points before speaking

- `ServerSocket.accept()` waits for a connection and returns its Socket.
- `Executors.newVirtualThreadPerTaskExecutor()` starts a virtual thread for each submitted task.
- `BufferedReader.readLine()` waits for one incoming text line.
- `PrintWriter(..., true)` enables auto-flushing for println. `println()` terminates the protocol line.
- `JOIN|name` identifies the client; the server answers `WELCOME|name|readOnly`.
- `MSG|text` is an ordinary message. `CHAT|time|name|text` is the server's formatted response.
- The UI turns `allUsers` into `LIST`, and `bye`/`end` into `LEAVE`.
- `send` replies to one connection; `broadcast` loops over active clients.
- `readOnly` is enforced on the server even if a client bypasses the UI.
- `ChatListener` and `ServerListener` separate the model from the view.
- `Platform.runLater` schedules a short visual update on the JavaFX thread.
- `finally` unregisters a client even after an unexpected socket closure.

This version uses a newline-delimited UTF-8 text protocol (`readLine`/`println`). Explain those methods in the video; the earlier writeUTF/readUTF example is a different framing approach.
