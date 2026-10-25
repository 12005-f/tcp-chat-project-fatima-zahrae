package chat.server;

import chat.server.view.ChatServerApp;
import javafx.application.Application;

/**
 * Entry point:  java TCPServer
 * It is NOT a JavaFX Application subclass on purpose, so the executable JAR
 * also works without a module path ("JavaFX runtime components are missing").
 */
public class TCPServer {
    public static void main(String[] args) {
        Application.launch(ChatServerApp.class, args);
    }
}
