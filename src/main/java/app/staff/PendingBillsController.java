package app.staff;

import app.db.DBConnection;
import app.model.Bill;
import app.model.BillItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class PendingBillsController implements DashboardAware {

    @FXML
    private TextField searchField;

    @FXML
    private TableView<Bill> table;

    @FXML
    private TableColumn<Bill, String> codeCol;

    @FXML
    private TableColumn<Bill, String> nameCol;

    @FXML
    private TableColumn<Bill, Double> totalCol;

    @FXML
    private TableColumn<Bill, String> timeCol;

    private StaffDashboardController dashboardController;
    private ObservableList<Bill> billList = FXCollections.observableArrayList();

    @Override
    public void setDashboardController(StaffDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    public void initialize() {
        // Set up table columns
        codeCol.setCellValueFactory(new PropertyValueFactory<>("patientCode"));
        nameCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("billTime"));

        // Format total column
        totalCol.setCellFactory(col -> new TableCell<Bill, Double>() {
            @Override
            protected void updateItem(Double total, boolean empty) {
                super.updateItem(total, empty);
                if (empty || total == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", total));
                }
            }
        });

        table.setItems(billList);

        // Load all pending bills on initialization
        loadPendingBills();
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().trim();

        if (searchText.isEmpty()) {
            loadPendingBills();
            return;
        }

        billList.clear();

        String query = "SELECT b.id, b.patient_code, p.full_name, b.total_amount, b.bill_time, b.counter_no " +
                "FROM bills b " +
                "JOIN patients p ON b.patient_code = p.patient_code " +
                "WHERE b.status = 'DRAFT' " +
                "AND (b.patient_code LIKE ? OR p.full_name LIKE ?) " +
                "ORDER BY b.bill_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            String searchPattern = "%" + searchText + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);

            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Bill bill = new Bill(
                        rs.getInt("id"),
                        rs.getString("patient_code"),
                        rs.getString("full_name"),
                        rs.getDouble("total_amount"),
                        rs.getTimestamp("bill_time").toLocalDateTime(),
                        rs.getString("counter_no")
                );
                billList.add(bill);
            }

        } catch (SQLException e) {
            showError("Error searching bills: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleEdit() {
        Bill selectedBill = table.getSelectionModel().getSelectedItem();

        if (selectedBill == null) {
            showError("Please select a bill to edit");
            return;
        }

        try {
            // Load the CreateBill view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/staff/createBill.fxml"));
            Parent createBillView = loader.load();

            // Get the controller and pass the bill data for editing
            CreateBillController controller = loader.getController();
            controller.loadBillForEdit(selectedBill.getId());

            // Inject dashboard reference
            if (controller instanceof DashboardAware) {
                ((DashboardAware) controller).setDashboardController(dashboardController);
            }

            // Load the view in the dashboard's content area
            if (dashboardController != null) {
                dashboardController.loadViewInContentArea(createBillView);
            }

        } catch (IOException e) {
            showError("Error loading edit view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFinal() {
        Bill selectedBill = table.getSelectionModel().getSelectedItem();

        if (selectedBill == null) {
            showError("Please select a bill to finalize");
            return;
        }

        // Confirm action
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Final Bill");
        confirmAlert.setHeaderText("Generate Final Bill");
        confirmAlert.setContentText("Are you sure you want to generate the final bill for " +
                selectedBill.getPatientName() + "?");

        if (confirmAlert.showAndWait().get() != ButtonType.OK) {
            return;
        }

        try {
            // Update bill status to FINAL
            updateBillStatus(selectedBill.getId(), "FINAL");

            // Load the PreBill view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/staff/preBill.fxml"));
            Parent preBillView = loader.load();

            // Get the controller and pass the bill data
            PreBillController controller = loader.getController();
            controller.loadBillData(selectedBill.getId());

            // Inject dashboard reference
            if (controller instanceof DashboardAware) {
                ((DashboardAware) controller).setDashboardController(dashboardController);
            }

            // Load the view in the dashboard's content area
            if (dashboardController != null) {
                dashboardController.loadViewInContentArea(preBillView);
            }

        } catch (IOException e) {
            showError("Error loading prebill view: " + e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            showError("Error updating bill status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadPendingBills() {
        billList.clear();

        String query = "SELECT b.id, b.patient_code, p.full_name, b.total_amount, b.bill_time, b.counter_no " +
                "FROM bills b " +
                "JOIN patients p ON b.patient_code = p.patient_code " +
                "WHERE b.status = 'DRAFT' " +
                "ORDER BY b.bill_time DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Bill bill = new Bill(
                        rs.getInt("id"),
                        rs.getString("patient_code"),
                        rs.getString("full_name"),
                        rs.getDouble("total_amount"),
                        rs.getTimestamp("bill_time").toLocalDateTime(),
                        rs.getString("counter_no")
                );
                billList.add(bill);
            }

        } catch (SQLException e) {
            showError("Error loading pending bills: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateBillStatus(int billId, String status) throws SQLException {
        String query = "UPDATE bills SET status = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, status);
            pstmt.setInt(2, billId);
            pstmt.executeUpdate();

        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}