package app.staff;

import app.db.DBConnection;
import app.model.BillContext;
import app.model.Patient;
import app.model.PendingBillRow;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PendingBillsController implements DashboardAware {

    private StaffDashboardController dashboardController;
    @Override
    public void setDashboardController(StaffDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML private TextField searchField;
    @FXML private TableView<PendingBillRow> table;
    @FXML private TableColumn<PendingBillRow, String> codeCol;
    @FXML private TableColumn<PendingBillRow, String> nameCol;
    @FXML private TableColumn<PendingBillRow, Double> totalCol;
    @FXML private TableColumn<PendingBillRow, Object> timeCol;

    private final ObservableList<PendingBillRow> data =
            FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        codeCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getPatientCode()));
        nameCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getPatientName()));
        totalCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getTotalAmount()));
        timeCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getBillTime()));

        table.setItems(data);
    }

    @FXML
    private void handleSearch() {

        data.clear();

        String key = "%" + searchField.getText() + "%";

        String sql = """
            SELECT b.id, b.patient_code, p.full_name,
                   b.total_amount, b.bill_time
            FROM bills b
            JOIN patients p ON p.patient_code = b.patient_code
            WHERE b.status = 'DRAFT'
              AND (p.full_name LIKE ? OR p.patient_code LIKE ?)
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, key);
            ps.setString(2, key);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                data.add(new PendingBillRow(
                        rs.getInt("id"),
                        rs.getString("patient_code"),
                        rs.getString("full_name"),
                        rs.getDouble("total_amount"),
                        rs.getTimestamp("bill_time")
                ));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Patient fetchPatientByCode(String code) {

        String sql = """
        SELECT patient_code, full_name, gender, age, phone, address
        FROM patients
        WHERE patient_code = ?
    """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, code);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return new Patient(
                        rs.getString("patient_code"),
                        rs.getString("full_name"),
                        rs.getString("gender"),
                        rs.getInt("age"),
                        rs.getString("phone"),
                        rs.getString("address")
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @FXML
    private void handleEdit() {

        PendingBillRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/staff/createBill.fxml")
            );

            Parent view = loader.load();
            CreateBillController controller = loader.getController();
            controller.setDashboardController(dashboardController); // 🔥 REQUIRED
            Patient patient = fetchPatientByCode(row.getPatientCode());
            controller.init(
                    new BillContext(patient, row.getBillId())
            );
            dashboardController.loadViewWithContext(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFinal() {

        PendingBillRow row = table.getSelectionModel().getSelectedItem();
        if (row == null) return;

        String sql = """
            UPDATE bills
            SET status = 'FINAL'
            WHERE id = ?
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, row.getBillId());
            ps.executeUpdate();

            data.remove(row);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
