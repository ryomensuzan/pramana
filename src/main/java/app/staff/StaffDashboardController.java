package app.staff;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import java.io.IOException;
import javafx.scene.Node;
import javafx.event.ActionEvent;

public class StaffDashboardController {


        @FXML private StackPane contentArea;
        public Button patientBtn;
        public Button overviewBtn;
        public Button registerBtn;
        public Button pendingBtn;
        public Button historyBtn;
        public Button profileBtn;
        public Button logoutBtn;



    public void loadViewInContentArea(Parent view) {
            contentArea.getChildren().setAll(view);
        }



    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(fxmlPath)
            );

            Parent view = loader.load();

            Object controller = loader.getController();

            // inject dashboard reference if needed
            if (controller instanceof DashboardAware) {
                ((DashboardAware) controller).setDashboardController(this);
            }

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    // Overview
    @FXML
    private void openOverView(){setActive(overviewBtn);loadView("/staff/overView.fxml");}

    // Patient Management

    @FXML
    private  void openRegisterPatient(){setActive(registerBtn); loadView("/staff/registerPatient.fxml");}

    @FXML
    private  void openPatientSearch(){setActive(patientBtn); loadView("/staff/patientSearch.fxml");}

    //Bills Management

    @FXML
    private  void openPendingBills(){setActive(pendingBtn); loadView("/staff/pendingBills.fxml");}

    @FXML
    private  void openBillingHistory(){setActive(historyBtn); loadView("/staff/billingHistory.fxml");}

    // Profile

    @FXML
    private  void openProfile(){setActive(profileBtn); loadView("/staff/Profile.fxml");}

    //Logout
    @FXML
    private void logout(ActionEvent event) {
        setActive(logoutBtn);
        try {
            Parent view = FXMLLoader.load(
                    getClass().getResource("/logout.fxml")
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

