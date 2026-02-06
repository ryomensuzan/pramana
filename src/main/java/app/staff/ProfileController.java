package app.staff;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
public class ProfileController {

    @FXML
    private Label usernameLabel;

    @FXML
    private Label roleLabel;

    @FXML
    private Label counterLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label createdAtLabel;


    @FXML
    public void initialize() {
        // MOCK DATA — replace with logged-in user session
        String username = "staff01";
        String role = "STAFF";
        String counterNo = "C-01";
        int status = 1;
        String createdAt = "2024-08-12 10:15:00";

        usernameLabel.setText(username);
        roleLabel.setText(role);
        counterLabel.setText(counterNo != null ? counterNo : "-");
        statusLabel.setText(status == 1 ? "Active" : "Disabled");
        createdAtLabel.setText(createdAt);
    }

}
