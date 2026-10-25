package chat.server.model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Talks to ONE client. Runs in its own virtual thread.
 *
 * Protocol (one text line per message, fields separated by '|'):
 *   client -> server : JOIN|username (empty = read-only)  MSG|text  LIST  LEAVE
 *   server -> client : WELCOME|username|readOnly  REJECTED|reason  CHAT|time|user|text
 *                      INFO|text  USERS|a,b,c  ERROR|text
 */
class ClientHandler implements Runnable {

    private final Socket socket;
    private final ChatServer server;

    private volatile PrintWriter out;
    private String username;
    private boolean readOnly;
    private volatile boolean ready;

    ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try (socket) {
            socket.setSoTimeout(10_000); // A new connection must identify itself promptly.
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            // 1) handshake
            String first = in.readLine();
            if (first == null || !first.startsWith("JOIN|")) return;
            String requested = sanitize(first.substring("JOIN|".length()));
            readOnly = requested.isEmpty();
            username = readOnly ? server.nextGuestName() : requested;

            if (!server.tryRegister(this)) {
                send("REJECTED|The username '" + username + "' is already in use.");
                return;
            }
            send("WELCOME|" + username + "|" + readOnly);
            socket.setSoTimeout(0); // Established chat users may remain idle.
            ready = true; // Chat broadcasts must not arrive before WELCOME.
            server.announceJoin(this);

            // 2) conversation
            String line;
            while ((line = in.readLine()) != null) {
                if (!handle(line)) break;
            }
        } catch (IOException e) {
            // connection dropped - handled in finally
        } finally {
            server.unregister(this);
        }
    }

    /** @return false when the client wants to leave. */
    private boolean handle(String line) {
        int i = line.indexOf('|');
        String cmd = i < 0 ? line : line.substring(0, i);
        String arg = i < 0 ? "" : line.substring(i + 1);

        switch (cmd) {
            case "MSG" -> {
                if (readOnly) send("ERROR|You are in READ-ONLY MODE: you cannot send messages.");
                else if (!arg.isBlank()) server.broadcastChat(this, arg.strip());
            }
            case "LIST" -> send("USERS|" + String.join(",", server.userNames()));
            case "LEAVE" -> {
                return false;
            }
            default -> send("ERROR|Unknown command.");
        }
        return true;
    }

    /** synchronized: several threads may broadcast to this client at the same time. */
    synchronized void send(String line) {
        PrintWriter w = out;
        if (w != null) {
            w.println(line);
            if (w.checkError()) close(); // PrintWriter reports write failures through checkError().
        }
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    String getUsername() {
        return username;
    }

    boolean isReadOnly() {
        return readOnly;
    }

    boolean isReady() { return ready; }

    String getDisplayName() {
        return readOnly ? username + " (read-only)" : username;
    }

    /** Removes the characters used by the protocol and limits the length. */
    private static String sanitize(String s) {
        String clean = s.replaceAll("[|,\\p{Cntrl}]", "").strip();
        return clean.length() > 20 ? clean.substring(0, 20) : clean;
    }
}
