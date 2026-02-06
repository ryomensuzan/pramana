package app.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class AddStaffController {
    @FXML
    private TextField staffUsername;

    @FXML
    private PasswordField staffPassword;

    @FXML
    private Label errorStaff;


    @FXML
    private void handleAddStaff() {
        String username = staffUsername.getText();
        String password = staffPassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorStaff.setText("All fields are required!");
            return;
        }

    }
}
