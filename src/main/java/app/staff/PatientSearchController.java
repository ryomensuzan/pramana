package app.staff;

import app.db.DBConnection;
import app.model.BillContext;
import app.model.Patient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PatientSearchController implements DashboardAware {

    private StaffDashboardController dashboardController;
    @Override
    public void setDashboardController(StaffDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML private TextField searchField;
    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, String> codeCol;
    @FXML private TableColumn<Patient, String> nameCol;
    @FXML private TableColumn<Patient, String> genderCol;
    @FXML private TableColumn<Patient, Integer> ageCol;
    @FXML private TableColumn<Patient, String> phoneCol;
    @FXML private Label statusLabel;

    private final ObservableList<Patient> patientList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        patientTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

        codeCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getPatientCode()));
        nameCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getFullName()));
        genderCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getGender()));
        ageCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleObjectProperty<>(data.getValue().getAge()));
        phoneCol.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(data.getValue().getPhone()));

        patientTable.setItems(patientList);
    }

    @FXML
    private void handleSearch() {

        String keyword = searchField.getText();

        if (keyword.isEmpty()) {
            statusLabel.setText("Enter search keyword");
            return;
        }

        patientList.clear();

        String sql = """
            SELECT patient_code, full_name, gender, age, phone, address
            FROM patients
            WHERE patient_code LIKE ?
               OR full_name LIKE ?
               OR phone LIKE ?
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setString(3, like);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                patientList.add(new Patient(
                        rs.getString("patient_code"),
                        rs.getString("full_name"),
                        rs.getString("gender"),
                        rs.getInt("age"),
                        rs.getString("phone"),
                        rs.getString("address")
                ));
            }

            statusLabel.setText(patientList.size() + " result(s)");

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Database error");
        }
        if (!patientList.isEmpty()) {
            patientTable.getSelectionModel().selectFirst();
        }

    }

    @FXML
    private void handleSelect() {

        Patient selected = patientTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/staff/createBill.fxml")
            );

            Parent view = loader.load();
            CreateBillController controller = loader.getController();

            controller.init(new BillContext(selected));

            dashboardController.loadViewWithContext(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



}
