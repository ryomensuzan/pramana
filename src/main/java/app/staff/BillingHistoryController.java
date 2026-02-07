package app.staff;

import app.db.DBConnection;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.print.PrinterJob;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class BillingHistoryController implements DashboardAware {

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> statusFilter;

    @FXML
    private DatePicker fromDatePicker;

    @FXML
    private DatePicker toDatePicker;

    @FXML
    private TableView<BillRecord> billsTable;

    @FXML
    private TableColumn<BillRecord, Integer> billIdCol;

    @FXML
    private TableColumn<BillRecord, String> patientCodeCol;

    @FXML
    private TableColumn<BillRecord, String> patientNameCol;

    @FXML
    private TableColumn<BillRecord, Double> totalCol;

    @FXML
    private TableColumn<BillRecord, String> dateCol;

    @FXML
    private TableColumn<BillRecord, String> counterCol;

    @FXML
    private TableColumn<BillRecord, String> statusCol;

    @FXML
    private Label totalBillsLabel;

    @FXML
    private Label totalRevenueLabel;

    private StaffDashboardController dashboardController;
    private ObservableList<BillRecord> billRecords = FXCollections.observableArrayList();

    @Override
    public void setDashboardController(StaffDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    public void initialize() {
        // Set up table columns
        billIdCol.setCellValueFactory(new PropertyValueFactory<>("billId"));
        patientCodeCol.setCellValueFactory(new PropertyValueFactory<>("patientCode"));
        patientNameCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("totalAmount"));
        dateCol.setCellValueFactory(new PropertyValueFactory<>("billDate"));
        counterCol.setCellValueFactory(new PropertyValueFactory<>("counter"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Format total column
        totalCol.setCellFactory(col -> new TableCell<BillRecord, Double>() {
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

        // Color code status column
        statusCol.setCellFactory(col -> new TableCell<BillRecord, String>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(status);
                    switch (status) {
                        case "PAID":
                            setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                            break;
                        case "FINAL":
                            setStyle("-fx-text-fill: blue; -fx-font-weight: bold;");
                            break;
                        case "DRAFT":
                            setStyle("-fx-text-fill: orange; -fx-font-weight: bold;");
                            break;
                        case "CANCELLED":
                            setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                            break;
                    }
                }
            }
        });

        billsTable.setItems(billRecords);

        // Initialize status filter
        statusFilter.setItems(FXCollections.observableArrayList(
                "All", "DRAFT", "FINAL", "PAID", "CANCELLED"
        ));
        statusFilter.setValue("All");

        // Load all bills initially
        loadBills();
    }

    @FXML
    private void handleSearch() {
        loadBills();
    }

    @FXML
    private void handleClear() {
        searchField.clear();
        statusFilter.setValue("All");
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        loadBills();
    }

    @FXML
    private void handleFilter() {
        loadBills();
    }

    @FXML
    private void handleViewDetails() {
        BillRecord selected = billsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Please select a bill to view details");
            return;
        }

        try {
            // Load the PreBill view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/staff/preBill.fxml"));
            Parent preBillView = loader.load();

            // Get the controller and pass the bill data
            PreBillController controller = loader.getController();
            controller.loadBillData(selected.getBillId());

            // Inject dashboard reference
            if (controller instanceof DashboardAware) {
                ((DashboardAware) controller).setDashboardController(dashboardController);
            }

            // Load the view in the dashboard's content area
            if (dashboardController != null) {
                dashboardController.loadViewInContentArea(preBillView);
            }

        } catch (IOException e) {
            showError("Error loading bill details: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handlePrintBill() {
        BillRecord selected = billsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showError("Please select a bill to print");
            return;
        }

        // Navigate to PreBill view which has print functionality
        handleViewDetails();
    }

    @FXML
    private void handleExportExcel() {
        if (billRecords.isEmpty()) {
            showError("No data to export");
            return;
        }

        try {
            // Create CSV file (can be opened in Excel)
            String fileName = "BillingHistory_" + LocalDate.now() + ".csv";
            File file = new File(System.getProperty("user.home") + "/Downloads/" + fileName);
            FileWriter writer = new FileWriter(file);

            // Write header
            writer.write("Bill ID,Patient Code,Patient Name,Total Amount,Bill Date,Counter,Status\n");

            // Write data
            for (BillRecord record : billRecords) {
                writer.write(String.format("%d,%s,%s,%.2f,%s,%s,%s\n",
                        record.getBillId(),
                        record.getPatientCode(),
                        record.getPatientName(),
                        record.getTotalAmount(),
                        record.getBillDate(),
                        record.getCounter(),
                        record.getStatus()));
            }

            writer.close();

            showSuccess("Data exported successfully!\n\nFile saved to: " + file.getAbsolutePath());

        } catch (IOException e) {
            showError("Error exporting data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadBills();
    }

    private void loadBills() {
        billRecords.clear();

        StringBuilder query = new StringBuilder(
                "SELECT b.id, b.patient_code, p.full_name, b.total_amount, " +
                        "b.bill_time, b.counter_no, b.status " +
                        "FROM bills b " +
                        "JOIN patients p ON b.patient_code = p.patient_code " +
                        "WHERE 1=1 "
        );

        // Add search filter
        String searchText = searchField.getText().trim();
        boolean hasSearch = !searchText.isEmpty();

        // Add status filter
        String status = statusFilter.getValue();
        boolean hasStatusFilter = status != null && !status.equals("All");

        // Add date filters
        LocalDate fromDate = fromDatePicker.getValue();
        LocalDate toDate = toDatePicker.getValue();

        if (hasSearch) {
            query.append("AND (b.patient_code LIKE ? OR p.full_name LIKE ? OR b.id = ?) ");
        }

        if (hasStatusFilter) {
            query.append("AND b.status = ? ");
        }

        if (fromDate != null) {
            query.append("AND DATE(b.bill_time) >= ? ");
        }

        if (toDate != null) {
            query.append("AND DATE(b.bill_time) <= ? ");
        }

        query.append("ORDER BY b.bill_time DESC");

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query.toString())) {

            int paramIndex = 1;

            // Set search parameters
            if (hasSearch) {
                String searchPattern = "%" + searchText + "%";
                pstmt.setString(paramIndex++, searchPattern);
                pstmt.setString(paramIndex++, searchPattern);
                try {
                    pstmt.setInt(paramIndex++, Integer.parseInt(searchText));
                } catch (NumberFormatException e) {
                    pstmt.setInt(paramIndex++, -1); // Invalid ID
                }
            }

            // Set status filter
            if (hasStatusFilter) {
                pstmt.setString(paramIndex++, status);
            }

            // Set date filters
            if (fromDate != null) {
                pstmt.setDate(paramIndex++, Date.valueOf(fromDate));
            }

            if (toDate != null) {
                pstmt.setDate(paramIndex++, Date.valueOf(toDate));
            }

            ResultSet rs = pstmt.executeQuery();

            double totalRevenue = 0.0;

            while (rs.next()) {
                Timestamp timestamp = rs.getTimestamp("bill_time");
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
                String formattedDate = timestamp.toLocalDateTime().format(formatter);

                BillRecord record = new BillRecord(
                        rs.getInt("id"),
                        rs.getString("patient_code"),
                        rs.getString("full_name"),
                        rs.getDouble("total_amount"),
                        formattedDate,
                        rs.getString("counter_no") != null ? rs.getString("counter_no") : "N/A",
                        rs.getString("status")
                );

                billRecords.add(record);

                // Calculate total revenue (only for PAID bills)
                if ("PAID".equals(rs.getString("status"))) {
                    totalRevenue += rs.getDouble("total_amount");
                }
            }

            // Update summary labels
            totalBillsLabel.setText(String.valueOf(billRecords.size()));
            totalRevenueLabel.setText(String.format("%.2f", totalRevenue));

        } catch (SQLException e) {
            showError("Error loading billing history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class for bill record display
     */
    public static class BillRecord {
        private final SimpleIntegerProperty billId;
        private final SimpleStringProperty patientCode;
        private final SimpleStringProperty patientName;
        private final SimpleDoubleProperty totalAmount;
        private final SimpleStringProperty billDate;
        private final SimpleStringProperty counter;
        private final SimpleStringProperty status;

        public BillRecord(int billId, String patientCode, String patientName,
                          double totalAmount, String billDate, String counter, String status) {
            this.billId = new SimpleIntegerProperty(billId);
            this.patientCode = new SimpleStringProperty(patientCode);
            this.patientName = new SimpleStringProperty(patientName);
            this.totalAmount = new SimpleDoubleProperty(totalAmount);
            this.billDate = new SimpleStringProperty(billDate);
            this.counter = new SimpleStringProperty(counter);
            this.status = new SimpleStringProperty(status);
        }

        public int getBillId() {
            return billId.get();
        }

        public SimpleIntegerProperty billIdProperty() {
            return billId;
        }

        public String getPatientCode() {
            return patientCode.get();
        }

        public SimpleStringProperty patientCodeProperty() {
            return patientCode;
        }

        public String getPatientName() {
            return patientName.get();
        }

        public SimpleStringProperty patientNameProperty() {
            return patientName;
        }

        public double getTotalAmount() {
            return totalAmount.get();
        }

        public SimpleDoubleProperty totalAmountProperty() {
            return totalAmount;
        }

        public String getBillDate() {
            return billDate.get();
        }

        public SimpleStringProperty billDateProperty() {
            return billDate;
        }

        public String getCounter() {
            return counter.get();
        }

        public SimpleStringProperty counterProperty() {
            return counter;
        }

        public String getStatus() {
            return status.get();
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }
    }
}