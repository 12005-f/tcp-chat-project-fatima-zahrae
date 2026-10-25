package chat.client.model;

import java.util.List;

/** Events sent by the model to whoever displays the chat. Called from a background thread. */
public interface ChatListener {
    void onJoined(String username, boolean readOnly);

    void onRejected(String reason);          // connection failed or username refused

    void onChat(String time, String sender, String text);

    void onInfo(String text);

    void onUsers(List<String> users);

    void onError(String text);

    void onDisconnected();                   // connection lost (not triggered by our own leave())
}
