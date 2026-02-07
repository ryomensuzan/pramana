package app;

import app.util.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class LogoutController {

    /**
     * Handle logout confirmation
     */
    @FXML
    public void confirmLogout(ActionEvent event) {
        try {
            System.out.println("🔓 Logging out user: " + SessionManager.getInstance().getUsername());

            // Clear session
            SessionManager.clearSession();

            // Load login screen
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/login.fxml")
            );

            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(
                    getClass().getResource("/css/app.css").toExternalForm()
            );

            // Get current stage
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login - Pramanā Prebilling");

            System.out.println("✅ Logged out successfully!");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error during logout: " + e.getMessage());
        }
    }

    /**
     * Handle cancel - close the logout dialog/window
     */
    @FXML
    public void cancel(ActionEvent event) {
        // Close the current window/stage
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();

        System.out.println("ℹ️ Logout cancelled");
    }
}