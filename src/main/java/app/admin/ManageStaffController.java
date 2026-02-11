package app.admin;
import app.db.DBConnection;
import app.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.sql.*;
import java.util.Optional;

public class ManageStaffController {

    @FXML
    private TextField counterField;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, Integer> idCol;

    @FXML
    private TableColumn<User, String> roleCol;

    @FXML
    private TableColumn<User, String> counterCol;

    @FXML
    private TableColumn<User, String> usernameCol;

    @FXML
    private TableColumn<User, String> passwordCol;

    @FXML
    private TableColumn<User, String> statusCol;

    @FXML
    private TableColumn<User, Void> actionsCol;

    private ObservableList<User> userList = FXCollections.observableArrayList();
    private Integer selectedUserId = null; // For update mode

    @FXML
    public void initialize() {
        setupTableColumns();
        loadUsers();
    }

     // Setup table columns
    private void setupTableColumns() {
        userTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        roleCol.setCellValueFactory(new PropertyValueFactory<>("role"));
        counterCol.setCellValueFactory(new PropertyValueFactory<>("counterNo"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        passwordCol.setCellValueFactory(new PropertyValueFactory<>("maskedPassword"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statusText"));

        // Actions column with Update and Disable buttons
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button updateBtn = new Button("Update");
            private final Button toggleStatusBtn = new Button();
            private final HBox container = new HBox(8, updateBtn, toggleStatusBtn);

            {
                container.setAlignment(Pos.CENTER);
                updateBtn.getStyleClass().add("action-btn-update");
                toggleStatusBtn.getStyleClass().add("action-btn-disable");

                updateBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleUpdateUser(user);
                });

                toggleStatusBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleToggleStatus(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    toggleStatusBtn.setText(user.getStatus() == 1 ? "Disable" : "Enable");
                    setGraphic(container);
                }
            }
        });
    }

    //Load all users from database
    private void loadUsers() {
        userList.clear();
        String query = "SELECT id, counter_no, username, password, role, status FROM users ORDER BY id";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                User user = new User(
                        rs.getInt("id"),
                        rs.getString("counter_no"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getInt("status")
                );
                userList.add(user);
            }

            userTable.setItems(userList);

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error loading users: " + e.getMessage(), "error");
        }
    }
    // Handle Save User (Create or Update)
    @FXML
    private void handleSaveUser() {
        String counterNo = counterField.getText().trim();
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (username.isEmpty()) {
            showMessage("Username is required", "error");
            return;
        }

        if (password.isEmpty()) {
            showMessage("Password is required", "error");
            return;
        }

        // Convert empty counter_no to NULL for admin
        if (counterNo.isEmpty() || counterNo.equalsIgnoreCase("NULL")) {
            counterNo = null;
        }

        // Determine role based on counter_no
        String role = (counterNo == null) ? "ADMIN" : "STAFF";

        if (selectedUserId == null) {
            // CREATE new user
            createUser(counterNo, username, password, role);
        } else {
            // UPDATE existing user
            updateUser(selectedUserId, counterNo, username, password, role);
        }
    }

    //Create new user
    private void createUser(String counterNo, String username, String password, String role) {
        String sql = "INSERT INTO users (counter_no, username, password, role, status) VALUES (?, ?, ?, ?, 1)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, counterNo);
            pstmt.setString(2, username);
            pstmt.setString(3, password); // TODO: Hash password in production
            pstmt.setString(4, role);

            pstmt.executeUpdate();
            showMessage("User created successfully!", "success");
            handleClear();
            loadUsers();

        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getMessage().contains("username")) {
                showMessage("Username already exists!", "error");
            } else if (e.getMessage().contains("counter_no")) {
                showMessage("Counter number already exists!", "error");
            } else {
                showMessage("Duplicate entry error", "error");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error creating user: " + e.getMessage(), "error");
        }
    }

    //Update existing user
    private void updateUser(int userId, String counterNo, String username, String password, String role) {
        String sql = "UPDATE users SET counter_no = ?, username = ?, password = ?, role = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, counterNo);
            pstmt.setString(2, username);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            pstmt.setInt(5, userId);

            pstmt.executeUpdate();
            showMessage("User updated successfully!", "success");
            handleClear();
            loadUsers();

        } catch (SQLIntegrityConstraintViolationException e) {
            if (e.getMessage().contains("username")) {
                showMessage("Username already exists!", "error");
            } else if (e.getMessage().contains("counter_no")) {
                showMessage("Counter number already exists!", "error");
            } else {
                showMessage("Duplicate entry error", "error");
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error updating user: " + e.getMessage(), "error");
        }
    }

    //Handle Update button click - populate form
    private void handleUpdateUser(User user) {
        selectedUserId = user.getId();
        counterField.setText(user.getCounterNo() != null ? user.getCounterNo() : "");
        usernameField.setText(user.getUsername());
        passwordField.setText(user.getPassword());
        showMessage("Update mode - Modify and click Save", "info");
    }

    //Handle Toggle Status (Enable/Disable)
    private void handleToggleStatus(User user) {
        String action = user.getStatus() == 1 ? "disable" : "enable";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Action");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to " + action + " user: " + user.getUsername() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            int newStatus = user.getStatus() == 1 ? 0 : 1;
            String sql = "UPDATE users SET status = ? WHERE id = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setInt(1, newStatus);
                pstmt.setInt(2, user.getId());
                pstmt.executeUpdate();

                showMessage("User " + action + "d successfully!", "success");
                loadUsers();

            } catch (SQLException e) {
                e.printStackTrace();
                showMessage("Error updating status: " + e.getMessage(), "error");
            }
        }
    }

    //Clear form fields
    @FXML
    private void handleClear() {
        selectedUserId = null;
        counterField.clear();
        usernameField.clear();
        passwordField.clear();
        messageLabel.setText("");
    }

    //Show message to user
    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("success", "error", "info");
        messageLabel.getStyleClass().add(type);
    }
}