package app;
import app.db.DBConnection;
import app.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class LoginController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Label errorLabel;

    @FXML
    private void handleLogin() {

        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Username and password required");
            return;
        }

        String sql = "SELECT id, username, role, status, counter_no FROM users WHERE username = ? AND password = ?";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            System.out.println("Database connected successfully!");

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                int userId = rs.getInt("id");
                String dbUsername = rs.getString("username");
                String role = rs.getString("role");
                int status = rs.getInt("status");
                String counterNo = rs.getString("counter_no");

                // Debug output
                System.out.println("=== LOGIN DEBUG ===");
                System.out.println("User ID: " + userId);
                System.out.println("Username: " + dbUsername);
                System.out.println("Role: " + role);
                System.out.println("Counter No: " + counterNo);
                System.out.println("Status: " + status);
                System.out.println("==================");

                if (status == 0) {
                    errorLabel.setText("Account is deactivated");
                    return;
                }

                // Setting session and check who is logged in. This is the critical step!
                SessionManager.setSession(userId, dbUsername, role, counterNo);

                System.out.println("Login successful: " + role);
                System.out.println("Session created for: " + dbUsername + " (ID: " + userId + ")");

                // Verify session was set correctly
                SessionManager session = SessionManager.getInstance();
                System.out.println("=== SESSION VERIFICATION ===");
                System.out.println("Session Counter No: " + session.getCounterNo());
                System.out.println("Session Username: " + session.getUsername());
                System.out.println("Session Role: " + session.getRole());
                System.out.println("Is Logged In: " + session.isLoggedIn());
                System.out.println("===========================");

                if ("ADMIN".equalsIgnoreCase(role)) {
                    loadAdminDashboard();
                } else if ("STAFF".equalsIgnoreCase(role)) {
                    loadStaffDashboard();
                }

            } else {
                errorLabel.setText("Invalid credentials");
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }

    // admin dashboard logic
    private void loadAdminDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/admin/adminDashboard.fxml")
            );

            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            scene.getStylesheets().add(
                    getClass().getResource("/css/app.css").toExternalForm()
            );
            stage.setScene(scene);
            stage.setTitle("Admin Dashboard - Pramanā Prebilling");

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Failed to load admin dashboard");
        }
    }
    // staff dashboard logic
    private void loadStaffDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/staff/staffDashboard.fxml")
            );
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/app.css").toExternalForm()
            );
            Stage stage = (Stage) usernameField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Staff Dashboard - Pramanā Prebilling");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Failed to load staff dashboard");
        }
    }
}