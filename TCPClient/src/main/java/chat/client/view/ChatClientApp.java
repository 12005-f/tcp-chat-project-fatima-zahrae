package chat.client.view;

import chat.client.model.ChatConnection;
import chat.client.model.ChatListener;
import chat.client.model.ClientConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.List;

/** JavaFX client: a login scene, then a chat scene. All network work is delegated to ChatConnection. */
public class ChatClientApp extends Application implements ChatListener {

    private Stage stage;
    private ClientConfig config;
    private ChatConnection connection;
    private boolean readOnly;

    // login scene
    private Scene loginScene;
    private final TextField usernameField = new TextField();
    private final Button connectButton = new Button("Connect");
    private final Label loginMessage = new Label();

    // chat scene
    private Scene chatScene;
    private final Label modeLabel = new Label();
    private final TextArea messagesArea = new TextArea();
    private final TextArea inputArea = new TextArea();
    private final Button sendButton = new Button("SEND");

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        try {
            config = ClientConfig.load(getParameters().getRaw());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: java TCPClient <ServerIPAddress> <PortNumber>");
            Platform.exit();
            return;
        }

        loginScene = buildLoginScene();
        chatScene = buildChatScene();

        stage.setTitle("Chat Client");
        stage.setScene(loginScene);
        stage.setOnCloseRequest(e -> {
            if (connection != null) connection.leave();
        });
        stage.show();
    }

    // ------------------------------------------------------------------ scenes

    private Scene buildLoginScene() {
        Label title = new Label("Join the chat");
        title.getStyleClass().add("title");
        Label server = new Label("Server: " + config.host() + ":" + config.port());
        server.getStyleClass().add("hint");

        usernameField.setPromptText("Username (leave empty = READ-ONLY)");
        connectButton.setDefaultButton(true);
        connectButton.setOnAction(e -> connect());
        loginMessage.getStyleClass().add("error");

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setVgap(12);
        grid.setHgap(8);
        grid.setPadding(new Insets(24));
        grid.add(title, 0, 0);
        grid.add(server, 0, 1);
        grid.add(usernameField, 0, 2);
        grid.add(connectButton, 0, 3);
        grid.add(loginMessage, 0, 4);
        GridPane.setHalignment(connectButton, HPos.RIGHT);
        usernameField.setPrefWidth(320);

        return styled(new Scene(grid, 420, 280));
    }

    private Scene buildChatScene() {
        // status bar: green circle + "Online" + mode + LEAVE
        Circle dot = new Circle(6);
        dot.getStyleClass().add("status-dot");
        Label online = new Label("Online");
        online.getStyleClass().add("online");
        modeLabel.getStyleClass().add("mode");
        Button leaveButton = new Button("LEAVE");
        leaveButton.setMinWidth(110);
        leaveButton.getStyleClass().add("leave");
        leaveButton.setOnAction(e -> leaveChat());

        HBox status = new HBox(8, dot, online, modeLabel);
        status.setMinWidth(0);
        modeLabel.setMinWidth(0);
        status.setAlignment(Pos.CENTER_LEFT);

        messagesArea.setEditable(false);
        messagesArea.setWrapText(true);
        messagesArea.getStyleClass().add("messages");

        inputArea.setPromptText("Type a message...  (\"allUsers\" lists users, \"end\"/\"bye\" leaves)");
        inputArea.setWrapText(true);
        inputArea.setPrefRowCount(2);
        // ENTER sends the message instead of adding a new line
        inputArea.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.getCode() == KeyCode.ENTER) {
                e.consume();
                submit();
            }
        });
        sendButton.setDefaultButton(false);
        sendButton.setOnAction(e -> submit());
        sendButton.setMaxHeight(Double.MAX_VALUE);
        sendButton.setMinWidth(110);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(12));
        grid.setHgap(8);
        grid.setVgap(8);
        grid.add(status, 0, 0);
        grid.add(leaveButton, 1, 0);
        grid.add(messagesArea, 0, 1, 2, 1);
        grid.add(inputArea, 0, 2);
        grid.add(sendButton, 1, 2);
        GridPane.setHalignment(leaveButton, HPos.RIGHT);

        ColumnConstraints grow = new ColumnConstraints();
        grow.setMinWidth(0);
        grow.setHgrow(Priority.ALWAYS);
        ColumnConstraints fixed = new ColumnConstraints();
        fixed.setMinWidth(110);
        fixed.setPrefWidth(110);
        grid.getColumnConstraints().addAll(grow, fixed);
        RowConstraints top = new RowConstraints();
        RowConstraints middle = new RowConstraints();
        middle.setVgrow(Priority.ALWAYS);
        RowConstraints bottom = new RowConstraints();
        grid.getRowConstraints().addAll(top, middle, bottom);

        return styled(new Scene(grid, 560, 520));
    }

    private Scene styled(Scene scene) {
        scene.getStylesheets().add(getClass().getResource("/client.css").toExternalForm());
        return scene;
    }

    // ------------------------------------------------------------------ user actions

    private void connect() {
        connectButton.setDisable(true);
        loginMessage.setText("Connecting...");
        connection = new ChatConnection(config, this);
        connection.connectAsync(usernameField.getText().strip());   // empty -> read-only
    }

    private void submit() {
        String text = inputArea.getText().strip();
        inputArea.clear();
        if (text.isEmpty()) return;

        if (text.equalsIgnoreCase("end") || text.equalsIgnoreCase("bye")) {
            leaveChat();
        } else if (text.equalsIgnoreCase("allUsers")) {
            connection.requestUsers();
        } else if (readOnly) {
            append("! READ-ONLY MODE: only allUsers, bye and end are allowed.");
        } else {
            connection.sendMessage(text);
        }
    }

    private void leaveChat() {
        if (connection != null) connection.leave();
        connection = null;
        showLogin("");
    }

    private void showLogin(String message) {
        loginMessage.setText(message);
        connectButton.setDisable(false);
        stage.setScene(loginScene);
    }

    // ------------------------------------------------------------------ ChatListener (background thread -> FX thread)

    @Override
    public void onJoined(String username, boolean readOnlyMode) {
        Platform.runLater(() -> {
            readOnly = readOnlyMode;
            messagesArea.clear();
            inputArea.clear();
            inputArea.setDisable(false);
            sendButton.setDisable(false);
            sendButton.setText(readOnlyMode ? "RUN" : "SEND");
            inputArea.setPromptText(readOnlyMode
                    ? "READ-ONLY: type allUsers, bye or end. Ordinary messages are blocked."
                    : "Type a message... (allUsers lists users; end/bye leaves)");
            modeLabel.setText(readOnlyMode ? "READ-ONLY MODE (" + username + ")" : "Logged in as " + username);
            modeLabel.getStyleClass().removeAll("readonly");
            if (readOnlyMode) modeLabel.getStyleClass().add("readonly");
            stage.setTitle("Chat Client - " + username);
            stage.setScene(chatScene);
            if (!readOnlyMode) inputArea.requestFocus();
        });
    }

    @Override
    public void onRejected(String reason) {
        Platform.runLater(() -> {
            connection = null;
            showLogin(reason);
        });
    }

    @Override
    public void onChat(String time, String sender, String text) {
        append("[" + time + "] " + sender + ": " + text);
    }

    @Override
    public void onInfo(String text) {
        append("* " + text);
    }

    @Override
    public void onUsers(List<String> users) {
        append("* Active users (" + users.size() + "): " + String.join(", ", users));
    }

    @Override
    public void onError(String text) {
        append("! " + text);
    }

    @Override
    public void onDisconnected() {
        Platform.runLater(() -> {
            connection = null;
            showLogin("Disconnected from the server.");
        });
    }

    private void append(String line) {
        Platform.runLater(() -> messagesArea.appendText(line + "\n"));
    }
}
