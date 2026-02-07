package app.staff;

import app.model.Patient;
import app.db.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PatientSearchController implements DashboardAware {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Patient> patientTable;

    @FXML
    private TableColumn<Patient, String> codeCol;

    @FXML
    private TableColumn<Patient, String> nameCol;

    @FXML
    private TableColumn<Patient, String> genderCol;

    @FXML
    private TableColumn<Patient, Integer> ageCol;

    @FXML
    private TableColumn<Patient, String> phoneCol;

    @FXML
    private Label statusLabel;

    private ObservableList<Patient> patientList = FXCollections.observableArrayList();
    private StaffDashboardController dashboardController;

    @Override
    public void setDashboardController(StaffDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    public void initialize() {
        // Set up table columns
        codeCol.setCellValueFactory(new PropertyValueFactory<>("patientCode"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("fullName"));
        genderCol.setCellValueFactory(new PropertyValueFactory<>("gender"));
        ageCol.setCellValueFactory(new PropertyValueFactory<>("age"));
        phoneCol.setCellValueFactory(new PropertyValueFactory<>("phone"));

        patientTable.setItems(patientList);

        // Load all patients initially
        loadAllPatients();
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim();

        if (searchText.isEmpty()) {
            loadAllPatients();
            return;
        }

        patientList.clear();

        String query = "SELECT * FROM patients WHERE " +
                "patient_code LIKE ? OR " +
                "full_name LIKE ? OR " +
                "phone LIKE ? " +
                "ORDER BY registered_at DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            String searchPattern = "%" + searchText + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setString(3, searchPattern);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Patient patient = new Patient(
                        rs.getInt("id"),
                        rs.getString("patient_code"),
                        rs.getString("full_name"),
                        rs.getString("gender"),
                        rs.getInt("age"),
                        rs.getString("phone"),
                        rs.getString("address")
                );
                patientList.add(patient);
            }

            statusLabel.setText("Found " + patientList.size() + " patient(s)");

        } catch (SQLException e) {
            statusLabel.setText("Error searching patients: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSelect(ActionEvent event) {
        Patient selectedPatient = patientTable.getSelectionModel().getSelectedItem();

        if (selectedPatient == null) {
            statusLabel.setText("Please select a patient first");
            return;
        }

        try {
            // Load the CreateBill view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/staff/createBill.fxml"));
            javafx.scene.Parent createBillView = loader.load();

            // Get the controller and pass the selected patient data
            CreateBillController controller = loader.getController();
            controller.setPatientData(selectedPatient);

            // Inject dashboard reference if the controller implements DashboardAware
            if (controller instanceof DashboardAware) {
                ((DashboardAware) controller).setDashboardController(dashboardController);
            }

            // Load the view in the dashboard's content area
            if (dashboardController != null) {
                dashboardController.loadViewInContentArea(createBillView);
            }

        } catch (IOException e) {
            statusLabel.setText("Error loading create bill view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadAllPatients() {
        patientList.clear();

        String query = "SELECT * FROM patients ORDER BY registered_at DESC LIMIT 100";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Patient patient = new Patient(
                        rs.getInt("id"),
                        rs.getString("patient_code"),
                        rs.getString("full_name"),
                        rs.getString("gender"),
                        rs.getInt("age"),
                        rs.getString("phone"),
                        rs.getString("address")
                );
                patientList.add(patient);
            }

            statusLabel.setText("Showing " + patientList.size() + " recent patients");

        } catch (SQLException e) {
            statusLabel.setText("Error loading patients: " + e.getMessage());
            e.printStackTrace();
        }
    }
}