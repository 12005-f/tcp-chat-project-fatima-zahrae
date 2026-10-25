package chat.client.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Owns the TCP socket: connects, sends commands, and turns server lines into ChatListener events. */
public class ChatConnection {

    private final ClientConfig config;
    private final ChatListener listener;

    private volatile Socket socket;
    private volatile PrintWriter out;
    private volatile boolean joined;
    private volatile boolean closing;

    public ChatConnection(ClientConfig config, ChatListener listener) {
        this.config = config;
        this.listener = listener;
    }

    /** Connects and reads in a virtual thread so the UI never freezes. Empty username = read-only. */
    public void connectAsync(String username) {
        Thread.ofVirtual().name("chat-reader").start(() -> run(username));
    }

    private void run(String username) {
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(config.host(), config.port()), 5000);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            send("JOIN|" + username);

            String line;
            while ((line = in.readLine()) != null) dispatch(line);   // blocks until the server writes
        } catch (IOException e) {
            if (!joined && !closing) {
                listener.onRejected("Cannot connect to " + config.host() + ":" + config.port()
                        + " (" + e.getMessage() + ")");
            }
        } finally {
            closeQuietly();
            if (joined && !closing) listener.onDisconnected();
        }
    }

    private void dispatch(String line) {
        String[] p = line.split("\\|", 4);
        String rest = line.substring(line.indexOf('|') + 1);
        switch (p[0]) {
            case "WELCOME" -> {
                joined = true;
                listener.onJoined(p[1], Boolean.parseBoolean(p[2]));
            }
            case "REJECTED" -> listener.onRejected(rest);
            case "CHAT" -> {
                if (p.length >= 4) listener.onChat(p[1], p[2], p[3]);
            }
            case "INFO" -> listener.onInfo(rest);
            case "USERS" -> listener.onUsers(rest.isBlank() ? List.of() : List.of(rest.split(",")));
            case "ERROR" -> listener.onError(rest);
            default -> { /* ignore unknown lines */ }
        }
    }

    // ---------- commands used by the UI ----------

    public void sendMessage(String text) {
        send("MSG|" + text.replace('\n', ' ').replace('\r', ' '));
    }

    public void requestUsers() {
        send("LIST");
    }

    /** Tells the server we leave, then closes the connection. */
    public void leave() {
        closing = true;
        send("LEAVE");
        closeQuietly();
    }

    private synchronized void send(String line) {
        PrintWriter w = out;
        if (w != null) w.println(line);
    }

    private void closeQuietly() {
        try {
            Socket s = socket;
            if (s != null) s.close();
        } catch (IOException ignored) {
        }
    }
}
