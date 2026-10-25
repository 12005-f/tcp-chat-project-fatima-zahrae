package chat.server.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Server IP/port loaded from server.properties (external file first, then the bundled default). */
public record ServerConfig(String host, int port) {

    public ServerConfig {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Server host must not be blank.");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be between 1 and 65535.");
    }

    private static final String FILE_NAME = "server.properties";

    public static ServerConfig load() {
        Properties props = new Properties();
        Path external = Path.of(FILE_NAME);                 // current working directory
        try (InputStream in = Files.exists(external)
                ? Files.newInputStream(external)
                : ServerConfig.class.getResourceAsStream("/" + FILE_NAME)) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            System.err.println("Cannot read " + FILE_NAME + ", using defaults: " + e.getMessage());
        }
        String host = props.getProperty("server.host", "0.0.0.0").trim();
        int port = Integer.parseInt(props.getProperty("server.port", "3000").trim());
        return new ServerConfig(host, port);
    }
}
