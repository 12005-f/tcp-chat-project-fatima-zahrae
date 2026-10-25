import chat.server.model.*;
import chat.client.model.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;

/** Real TCP checks for this project's JOIN/MSG/LIST/LEAVE line protocol. */
public class ProjectCheck {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        System.out.println("PASS: " + message);
    }
    static void await(BooleanSupplier ok, String message) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(8);
        while (!ok.getAsBoolean() && System.nanoTime() < end) Thread.sleep(10);
        check(ok.getAsBoolean(), message);
    }
    static class Peer implements AutoCloseable {
        final Socket socket;
        final PrintWriter out;
        final List<String> lines = new CopyOnWriteArrayList<>();
        Peer(int port, String name) throws Exception {
            socket = new Socket("127.0.0.1", port);
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            Thread.ofVirtual().start(() -> {
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    String line;
                    while ((line = in.readLine()) != null) lines.add(line);
                } catch (IOException ignored) { }
            });
            send("JOIN|" + name);
        }
        void send(String line) { out.println(line); }
        boolean has(String text) { return lines.stream().anyMatch(x -> x.contains(text)); }
        public void close() { try { socket.close(); } catch (IOException ignored) { } }
    }
    public static void main(String[] args) throws Exception {
        try { new ClientConfig("localhost", 70000); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { check(true, "Invalid client port rejected"); }
        try { ClientConfig.load(List.of("localhost")); throw new AssertionError(); }
        catch (IllegalArgumentException expected) { check(true, "Incomplete client arguments rejected"); }
        check(ClientConfig.load(List.of("127.0.0.1", "3456")).port() == 3456, "Client command-line override");
        int port;
        try (ServerSocket free = new ServerSocket(0)) { port = free.getLocalPort(); }
        AtomicReference<List<String>> users = new AtomicReference<>(List.of());
        AtomicBoolean virtual = new AtomicBoolean();
        ChatServer server = new ChatServer(new ServerConfig("127.0.0.1", port), new ServerListener() {
            public void onLog(String line) {
                if (line.startsWith("Welcome")) virtual.set(Thread.currentThread().isVirtual());
            }
            public void onUsersChanged(List<String> value) { users.set(value); }
        });
        server.start();
        try (Peer a = new Peer(port,"Fatima Zahrae"); Peer b = new Peer(port,"Amine"); Peer g = new Peer(port,"")) {
            await(() -> users.get().size() == 3, "Three concurrent clients registered");
            await(virtual::get, "Server handles clients in virtual threads");
            check(a.lines.get(0).startsWith("WELCOME|"), "WELCOME is first server frame");
            check(g.has("|true"), "Blank username gets read-only mode");
            a.send("MSG|Hello مرحبا | group");
            await(() -> a.has("Hello مرحبا | group") && b.has("Hello مرحبا | group") && g.has("Hello مرحبا | group"), "Broadcast, Unicode and pipe text reach all clients");
            check(a.lines.stream().filter(x -> x.startsWith("CHAT|")).count() == 1, "One sender echo");
            check(b.lines.stream().anyMatch(x -> x.matches("CHAT\\|\\d{2}:\\d{2}:\\d{2}\\|Fatima Zahrae\\|.*")), "Server includes username and timestamp");
            a.send("LIST");
            await(() -> a.has("USERS|"), "Private user inquiry answered");
            check(!b.has("USERS|") && !g.has("USERS|"), "User list not broadcast");
            g.send("LIST");
            await(() -> g.has("USERS|"), "Read-only user can request active list");
            g.send("MSG|FORBIDDEN_GUEST_MESSAGE");
            await(() -> g.has("ERROR|You are in READ-ONLY MODE"), "Server blocks guest chat");
            check(!a.has("FORBIDDEN_GUEST_MESSAGE"), "Blocked guest text does not reach group");
            try (Peer duplicate = new Peer(port,"fatima zahrae")) {
                await(() -> duplicate.has("REJECTED|"), "Duplicate username rejected case-insensitively");
            }
            try (ExecutorService jobs = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<?> first = jobs.submit(() -> { for (int i=0;i<30;i++) a.send("MSG|BURST_A_"+i); });
                Future<?> second = jobs.submit(() -> { for (int i=0;i<30;i++) b.send("MSG|BURST_B_"+i); });
                first.get(); second.get();
            }
            await(() -> g.lines.stream().filter(x -> x.contains("BURST_")).count() == 60, "60 simultaneous messages received intact");
            b.send("LEAVE");
            await(() -> users.get().size() == 2, "LEAVE removes client");
            g.close();
            await(() -> users.get().size() == 1, "Abrupt close removes client");

            AtomicBoolean joined = new AtomicBoolean(), chat = new AtomicBoolean(), listed = new AtomicBoolean(), disconnected = new AtomicBoolean();
            ChatConnection client = new ChatConnection(new ClientConfig("127.0.0.1", port), new ChatListener() {
                public void onJoined(String name, boolean ro) { joined.set(true); }
                public void onRejected(String reason) { throw new AssertionError(reason); }
                public void onChat(String time, String sender, String text) { if (text.equals("MODEL_MESSAGE")) chat.set(true); }
                public void onInfo(String text) { }
                public void onUsers(List<String> names) { listed.set(names.contains("ModelUser")); }
                public void onError(String text) { throw new AssertionError(text); }
                public void onDisconnected() { disconnected.set(true); }
            });
            client.connectAsync("ModelUser");
            await(joined::get, "Actual client model completes handshake");
            client.sendMessage("MODEL_MESSAGE");
            await(chat::get, "Actual client model parses chat frame");
            client.requestUsers();
            await(listed::get, "Actual client model parses user list");
            server.stop();
            await(disconnected::get, "Client detects server shutdown");
            await(() -> users.get().isEmpty(), "Shutdown clears active list");
        } finally { server.stop(); }
        System.out.println("ALL PROJECT CHECKS PASSED");
    }
}
