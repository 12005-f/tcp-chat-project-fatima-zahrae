package chat.server.model;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** Accepts clients, keeps the list of active connections and distributes messages. */
public class ChatServer {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ServerConfig config;
    private final ServerListener listener;
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();
    private final AtomicInteger guestCounter = new AtomicInteger();

    // One cheap virtual thread per client (Java 21)
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    private volatile ServerSocket serverSocket;
    private volatile boolean running;

    public ChatServer(ServerConfig config, ServerListener listener) {
        this.config = config;
        this.listener = listener;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket();
        try {
            serverSocket.bind(new InetSocketAddress(config.host(), config.port()));
        } catch (IOException | RuntimeException e) {
            serverSocket.close();
            throw e;
        }
        running = true;
        listener.onLog("Server Started on " + config.host() + ":" + config.port());
        executor.submit(this::acceptLoop);
    }

    private void acceptLoop() {
        while (running) {
            try {
                listener.onLog("Waiting for Client...");
                Socket socket = serverSocket.accept();            // blocks until someone connects
                executor.submit(new ClientHandler(socket, this)); // one virtual thread per client
            } catch (IOException e) {
                if (running) listener.onLog("Accept error: " + e.getMessage());
            }
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
        clients.forEach(ClientHandler::close);
        executor.shutdownNow();
    }

    // ---------- called by ClientHandler ----------

    /** Adds the client unless a (non read-only) user already uses that name. */
    synchronized boolean tryRegister(ClientHandler handler) {
        boolean taken = clients.stream().anyMatch(c ->
                !c.isReadOnly() && c.getUsername().equalsIgnoreCase(handler.getUsername()));
        if (taken) return false;
        clients.add(handler);
        return true;
    }

    String nextGuestName() {
        return "Guest-" + guestCounter.incrementAndGet();
    }

    void announceJoin(ClientHandler handler) {
        listener.onLog("Welcome " + handler.getDisplayName());
        broadcast("INFO|" + handler.getDisplayName() + " joined the chat");
        notifyUsersChanged();
    }

    void unregister(ClientHandler handler) {
        if (clients.remove(handler)) {                 // false if it never finished joining
            listener.onLog("Goodbye " + handler.getDisplayName());
            broadcast("INFO|" + handler.getDisplayName() + " left the chat");
            notifyUsersChanged();
        }
    }

    /** Formats "[time] username: text" data and sends it to every active connection. */
    void broadcastChat(ClientHandler sender, String text) {
        String time = LocalTime.now().format(TIME);
        broadcast("CHAT|" + time + "|" + sender.getUsername() + "|" + text);
        listener.onLog("[" + time + "] " + sender.getUsername() + ": " + text);
    }

    private void broadcast(String line) {
        for (ClientHandler c : clients) if (c.isReady()) c.send(line);
    }

    List<String> userNames() {
        return clients.stream().filter(ClientHandler::isReady).map(ClientHandler::getDisplayName).toList();
    }

    private synchronized void notifyUsersChanged() {
        listener.onUsersChanged(userNames());
    }
}
