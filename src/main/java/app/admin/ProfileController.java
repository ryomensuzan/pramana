package app.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.GridPane;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import app.db.DBConnection;
import app.util.SessionManager;

public class ProfileController {

    @FXML
    private Label avatarLabel;

    @FXML
    private Label usernameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label statusBadge;

    @FXML
    private Label usernameValue;

    @FXML
    private Label roleValue;

    @FXML
    private Label counterLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label createdAtLabel;

    @FXML
    public void initialize() {
        // Debug: Print session info
        SessionManager.printSessionInfo();

        loadUserProfile();
    }

    // Load logged-in user profile from database
    private void loadUserProfile() {
        // Get logged-in user from session
        String loggedInUsername = SessionManager.getLoggedInUser();

        if (loggedInUsername == null) {
            showError("Session Error", "No user logged in. Please login again.");
            return;
        }

        String query = "SELECT id, counter_no, username, role, status, created_at FROM users WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, loggedInUsername);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                // Extract data from ResultSet
                String username = rs.getString("username");
                String role = rs.getString("role");
                String counterNo = rs.getString("counter_no");
                int status = rs.getInt("status");
                Timestamp createdAt = rs.getTimestamp("created_at");

                // Set avatar initial (first letter of username)
                if (username != null && !username.isEmpty()) {
                    avatarLabel.setText(username.substring(0, 1).toUpperCase());
                }

                // Header section
                usernameLabel.setText(username);
                roleLabel.setText(role);

                // Status badge
                updateStatusBadge(status);

                // Details section
                usernameValue.setText(username);
                roleValue.setText(role);

                // Counter number - N/A for admin
                if (counterNo != null && !counterNo.isEmpty()) {
                    counterLabel.setText(counterNo);
                    counterLabel.getStyleClass().remove("detail-muted");
                } else {
                    counterLabel.setText("N/A (Admin Account)");
                    counterLabel.getStyleClass().add("detail-muted");
                }

                statusLabel.setText(status == 1 ? "Active" : "Disabled");
                createdAtLabel.setText(formatTimestamp(createdAt));

            } else {
                showError("Profile Error", "User profile not found in database.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database Error", "Failed to load profile: " + e.getMessage());
        }
    }


    // Update status badge styling based on status
    private void updateStatusBadge(int status) {
        if (status == 1) {
            statusBadge.setText("● Active");
            statusBadge.getStyleClass().removeAll("status-inactive");
            if (!statusBadge.getStyleClass().contains("status-active")) {
                statusBadge.getStyleClass().add("status-active");
            }
        } else {
            statusBadge.setText("● Inactive");
            statusBadge.getStyleClass().removeAll("status-active");
            if (!statusBadge.getStyleClass().contains("status-inactive")) {
                statusBadge.getStyleClass().add("status-inactive");
            }
        }
    }

    // Format Timestamp for better readability
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return "N/A";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a");
            return timestamp.toLocalDateTime().format(formatter);
        } catch (Exception e) {
            // Fallback to default format
            return timestamp.toString();
        }
    }

    //Handle Change Password
    @FXML
    private void handleChangePassword() {
        // Create custom dialog
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Change Password");
        alert.setHeaderText("Update your password");
        alert.setContentText("Please enter your current and new password:");

        // Create password fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        PasswordField currentPasswordField = new PasswordField();
        currentPasswordField.setPromptText("Current Password");
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("New Password");
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirm New Password");

        grid.add(new Label("Current Password:"), 0, 0);
        grid.add(currentPasswordField, 1, 0);
        grid.add(new Label("New Password:"), 0, 1);
        grid.add(newPasswordField, 1, 1);
        grid.add(new Label("Confirm Password:"), 0, 2);
        grid.add(confirmPasswordField, 1, 2);

        alert.getDialogPane().setContent(grid);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isPresent() && result.get() == ButtonType.OK) {
            String currentPassword = currentPasswordField.getText();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();

            // Validation
            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showError("Validation Error", "All fields are required!");
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                showError("Validation Error", "New passwords do not match!");
                return;
            }

            if (newPassword.length() < 4) {
                showError("Validation Error", "Password must be at least 4 characters long!");
                return;
            }

            // Update password in database
            updatePassword(currentPassword, newPassword);
        }
    }

    //Update password in database
    private void updatePassword(String currentPassword, String newPassword) {
        String username = SessionManager.getLoggedInUser();

        // First verify current password
        String verifyQuery = "SELECT id FROM users WHERE username = ? AND password = ?";
        String updateQuery = "UPDATE users SET password = ? WHERE username = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement verifyStmt = conn.prepareStatement(verifyQuery)) {

            verifyStmt.setString(1, username);
            verifyStmt.setString(2, currentPassword);
            ResultSet rs = verifyStmt.executeQuery();

            if (!rs.next()) {
                showError("Authentication Error", "Current password is incorrect!");
                return;
            }

            // Current password is correct, update to new password
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setString(1, newPassword);
                updateStmt.setString(2, username);
                updateStmt.executeUpdate();

                showSuccess("Password Changed", "Your password has been updated successfully!");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Database Error", "Failed to update password: " + e.getMessage());
        }
    }

    //Show error alert dialog
    private void showError(String title, String message) {
        Alert alert = new Alert(AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Show success alert dialog
    private void showSuccess(String title, String message) {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}