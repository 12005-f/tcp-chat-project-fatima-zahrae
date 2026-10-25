package chat.server.view;

import chat.server.model.ChatServer;
import chat.server.model.ServerConfig;
import chat.server.model.ServerListener;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** JavaFX window of the server: activity log + live list of connected users. */
public class ChatServerApp extends Application {

    private final Random random = new Random();
    private final Map<String, Color> userColors = new HashMap<>();   // only touched by the FX thread

    private final TextArea logArea = new TextArea();
    private final ListView<String> userList = new ListView<>();
    private ChatServer server;

    @Override
    public void start(Stage stage) {
        logArea.setEditable(false);
        logArea.setWrapText(true);
        logArea.getStyleClass().add("log-area");

        // Each user gets a random (pastel) background colour, remembered for the whole session
        userList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-background-color: " + toWeb(colorFor(item)) + ";");
                }
            }
        });

        Label logTitle = new Label("Activity log");
        Label usersTitle = new Label("Connected users");
        logTitle.getStyleClass().add("section-title");
        usersTitle.getStyleClass().add("section-title");

        GridPane root = new GridPane();
        root.setHgap(12);
        root.setVgap(8);
        root.setPadding(new Insets(14));
        root.add(logTitle, 0, 0);
        root.add(usersTitle, 1, 0);
        root.add(logArea, 0, 1);
        root.add(userList, 1, 1);

        ColumnConstraints left = new ColumnConstraints();
        left.setPercentWidth(65);
        ColumnConstraints right = new ColumnConstraints();
        right.setPercentWidth(35);
        root.getColumnConstraints().addAll(left, right);
        RowConstraints header = new RowConstraints();
        RowConstraints body = new RowConstraints();
        body.setVgrow(Priority.ALWAYS);
        root.getRowConstraints().addAll(header, body);

        Scene scene = new Scene(root, 760, 480);
        scene.getStylesheets().add(getClass().getResource("/server.css").toExternalForm());
        stage.setTitle("Chat Server");
        stage.setScene(scene);
        stage.show();

        startServer();
    }

    private void startServer() {
        ServerListener listener = new ServerListener() {
            @Override
            public void onLog(String message) {
                Platform.runLater(() -> logArea.appendText(message + "\n"));
            }

            @Override
            public void onUsersChanged(List<String> users) {
                Platform.runLater(() -> {
                    userList.getItems().setAll(users);
                    userColors.keySet().retainAll(users);     // forget colours of users who left
                });
            }
        };

        try {
            server = new ChatServer(ServerConfig.load(), listener);
            server.start();
        } catch (IOException | IllegalArgumentException e) {
            logArea.appendText("Cannot start server: " + e.getMessage() + "\n");
        }
    }

    @Override
    public void stop() {
        if (server != null) server.stop();
    }

    private Color colorFor(String user) {
        return userColors.computeIfAbsent(user,
                u -> Color.hsb(random.nextDouble() * 360, 0.35, 1.0));
    }

    private static String toWeb(Color c) {
        return String.format("#%02X%02X%02X",
                (int) Math.round(c.getRed() * 255),
                (int) Math.round(c.getGreen() * 255),
                (int) Math.round(c.getBlue() * 255));
    }
}
