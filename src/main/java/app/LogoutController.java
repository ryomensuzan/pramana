package app;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class LogoutController {

    @FXML
    private void confirmLogout() {
        try {
            Parent login = FXMLLoader.load(
                    getClass().getResource("/login.fxml")
            );

            Stage stage = Stage.getWindows()
                    .stream()
                    .filter(w -> w.isShowing())
                    .map(w -> (Stage) w)
                    .findFirst()
                    .orElse(null);

            if (stage != null) {
                stage.setScene(new Scene(login));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void cancel() {
        Stage stage = Stage.getWindows()
                .stream()
                .filter(w -> w.isShowing())
                .map(w -> (Stage) w)
                .findFirst()
                .orElse(null);

        if (stage != null) {
            stage.close();
        }
    }
}
