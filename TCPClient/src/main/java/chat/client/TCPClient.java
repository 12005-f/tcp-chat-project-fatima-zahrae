package chat.client;

import chat.client.view.ChatClientApp;
import javafx.application.Application;

/**
 * Entry point:  java TCPClient <ServerIPAddress> <PortNumber>   (e.g. localhost 3000)
 * Plain main class (not an Application subclass) so the executable JAR works.
 */
public class TCPClient {
    public static void main(String[] args) {
        Application.launch(ChatClientApp.class, args);
    }
}
