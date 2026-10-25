package chat.server.model;

import java.util.List;

/**
 * The ONLY door between the model and the outside world.
 * The model calls these methods; whoever implements them (JavaFX, console, tests...) decides what to do.
 * Callbacks arrive from background (virtual) threads.
 */
public interface ServerListener {
    void onLog(String message);

    void onUsersChanged(List<String> users);
}
