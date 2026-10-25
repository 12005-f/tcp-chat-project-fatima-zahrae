package chat.client.model;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

/** Server address: client.properties first, overridden by command-line arguments if present. */
public record ClientConfig(String host, int port) {

    public ClientConfig {
        if (host == null || host.isBlank()) throw new IllegalArgumentException("Server host must not be blank.");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be between 1 and 65535.");
    }

    private static final String FILE_NAME = "client.properties";

    public static ClientConfig load(List<String> args) {
        if (!args.isEmpty() && args.size() != 2) {
            throw new IllegalArgumentException("Usage: TCPClient [ServerIPAddress PortNumber]");
        }
        Properties props = new Properties();
        Path external = Path.of(FILE_NAME);
        try (InputStream in = Files.exists(external)
                ? Files.newInputStream(external)
                : ClientConfig.class.getResourceAsStream("/" + FILE_NAME)) {
            if (in != null) props.load(in);
        } catch (IOException e) {
            System.err.println("Cannot read " + FILE_NAME + ", using defaults: " + e.getMessage());
        }

        String host = props.getProperty("server.host", "localhost").trim();
        String port = props.getProperty("server.port", "3000").trim();
        if (args.size() == 2) {                // <ServerIPAddress> <PortNumber>
            host = args.get(0).strip();
            port = args.get(1).strip();
        }
        try {
            return new ClientConfig(host, Integer.parseInt(port));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid port number: " + port);
        }
    }
}
