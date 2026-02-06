package app;
import app.db.DBConnection;
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

        String username = usernameField.getText();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("user name and password required");
            return;
        }

        String sql = "SELECT role, status, counter_no FROM users WHERE username = ? AND password = ? ";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            System.out.println("✅ Database connected successfully!");

            ps.setString(1, username);
            ps.setString(2, password);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                String role = rs.getString("role");
                int status = rs.getInt("status");

                if (status == 0) {
                    errorLabel.setText("Account is deactivated");
                    return;
                }

                System.out.println("Login successful ➡️ " + role);

                if ("ADMIN".equalsIgnoreCase(role)) {

                    loadAdminDashboard();

                } else if ("STAFF".equalsIgnoreCase(role)) {

                    String counterNo = rs.getString("counter_no");
                    loadStaffDashboard(counterNo);   // pass counter number
                }

            } else {
                errorLabel.setText("Invalid credentials");
            }

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Database error");
        }


    }

    private void loadAdminDashboard(){
        try{
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/admin/adminDashboard.fxml")
            );

            Scene scene = new Scene(loader.load());
            Stage stage = (Stage) usernameField.getScene().getWindow();
            scene.getStylesheets().add(
                    getClass().getResource("/css/app.css").toExternalForm()
            );
            stage.setScene(scene);

        }
        catch ( Exception e){
            e.printStackTrace();
        }
    }

    private void loadStaffDashboard(String counterNo) {
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
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

}