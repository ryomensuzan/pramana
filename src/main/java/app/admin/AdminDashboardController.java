package app.admin;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.util.Objects;

public class AdminDashboardController {

    public Button manageStaffBtn;
    public Button staffListBtn;
    public Button manageServiceBtn;
    public Button serviceCategoryBtn;
    public Button hospitalProfileBtn;
    public Button invoiceSettingsBtn;
    public Button backupRestoreBtn;
    public Button profileBtn;
    public Button changePasswordBtn;
    public Button logoutBtn;

    @FXML
    private StackPane contentArea;

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath)
            );

            Parent view = loader.load();

            Object controller = loader.getController();

            // inject dashboard reference if needed
            if (controller instanceof app.admin.AdminDashboardAware) {
                ((AdminDashboardAware) controller).setDashboardController(this);
            }

            contentArea.getChildren().setAll(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //  STAFF MANAGEMENT
    @FXML
    private void openRegisterStaff() {
        setActive(staffListBtn);
        loadView("/admin/registerStaff.fxml");
    }

    //  SERVICES MANAGEMENT
    @FXML
    private void openManageService() {
        setActive(manageServiceBtn);
        loadView("/admin/manageServices.fxml");
    }

    @FXML
    private void openServiceCategory() {
        setActive(serviceCategoryBtn);
        loadView("/admin/serviceCategory.fxml");
    }

    // ================= PROFILE =================
    @FXML
    private void openProfile() {
        setActive(profileBtn);
        loadView("/admin/Profile.fxml");
    }

    //Logout
    @FXML
    private void logout(ActionEvent event) {
        try {
            Parent view = FXMLLoader.load(
                    Objects.requireNonNull(getClass().getResource("/logout.fxml"))
            );

            Stage stage = (Stage) ((Node) event.getSource())
                    .getScene()
                    .getWindow();

            stage.setScene(new Scene(view));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private VBox sidebar;
    private void setActive(Button btn) {
        sidebar.lookupAll(".sub-nav")
                .forEach(n -> n.getStyleClass().remove("active"));
        btn.getStyleClass().add("active");
    }
}
