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
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class PreBillController implements DashboardAware {

    @FXML
    private VBox printArea;

    @FXML
    private Label billNoLabel;

    @FXML
    private Label billDateLabel;

    @FXML
    private Label counterLabel;

    @FXML
    private Label patientCodeLabel;

    @FXML
    private Label patientNameLabel;

    @FXML
    private Label ageGenderLabel;

    @FXML
    private TableView<BillItemDisplay> itemsTable;

    @FXML
    private TableColumn<BillItemDisplay, Integer> snoCol;

    @FXML
    private TableColumn<BillItemDisplay, String> serviceCol;

    @FXML
    private TableColumn<BillItemDisplay, Double> priceCol;

    @FXML
    private TableColumn<BillItemDisplay, Integer> qtyCol;

    @FXML
    private TableColumn<BillItemDisplay, Double> amountCol;

    @FXML
    private Label subtotalLabel;

    @FXML
    private Label discountLabel;

    @FXML
    private Label grandTotalLabel;

    private StaffDashboardController dashboardController;
    private int billId;
    private ObservableList<BillItemDisplay> billItems = FXCollections.observableArrayList();
    private double grandTotal = 0.0;

    @Override
    public void setDashboardController(StaffDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    public void initialize() {
        System.out.println("=== PreBillController Initialize ===");

        // Set up table columns
        snoCol.setCellValueFactory(new PropertyValueFactory<>("sno"));
        serviceCol.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));

        // Format price and amount columns
        priceCol.setCellFactory(col -> new TableCell<BillItemDisplay, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", price));
                }
            }
        });

        amountCol.setCellFactory(col -> new TableCell<BillItemDisplay, Double>() {
            @Override
            protected void updateItem(Double amount, boolean empty) {
                super.updateItem(amount, empty);
                if (empty || amount == null) {
                    setText(null);
                } else {
                    setText(String.format("%.2f", amount));
                }
            }
        });

        itemsTable.setItems(billItems);
        System.out.println("Table setup complete");
    }

    /**
     * Load bill data for display
     * @param billId The bill ID to load
     */
    public void loadBillData(int billId) {
        this.billId = billId;

        System.out.println("\n=== LOADING BILL DATA ===");
        System.out.println("Bill ID: " + billId);

        String billQuery = "SELECT b.id, b.patient_code, p.full_name, p.age, p.gender, " +
                "b.total_amount, b.bill_time, b.counter_no " +
                "FROM bills b " +
                "JOIN patients p ON b.patient_code = p.patient_code " +
                "WHERE b.id = ?";

        String itemsQuery = "SELECT service_name, price, quantity FROM bill_items WHERE bill_id = ? ORDER BY id";

        try (Connection conn = DBConnection.getConnection()) {
            System.out.println("✅ Database connected");

            // Load bill header
            try (PreparedStatement pstmt = conn.prepareStatement(billQuery)) {
                pstmt.setInt(1, billId);
                System.out.println("Executing bill header query...");
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    System.out.println("✅ Bill header found");

                    // Set bill number
                    String billNo = "BILL-" + String.format("%06d", rs.getInt("id"));
                    billNoLabel.setText(billNo);
                    System.out.println("Bill No: " + billNo);

                    // Set date and time
                    Timestamp timestamp = rs.getTimestamp("bill_time");
                    if (timestamp != null) {
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy hh:mm a");
                        String formattedDate = timestamp.toLocalDateTime().format(formatter);
                        billDateLabel.setText(formattedDate);
                        System.out.println("Bill Date: " + formattedDate);
                    } else {
                        billDateLabel.setText("N/A");
                        System.out.println("Bill Date: N/A");
                    }

                    // Set counter
                    String counter = rs.getString("counter_no");
                    String counterText = (counter != null && !counter.isEmpty()) ? counter : "N/A";
                    counterLabel.setText(counterText);
                    System.out.println("Counter: " + counterText);

                    // Set patient info
                    String patientCode = rs.getString("patient_code");
                    String patientName = rs.getString("full_name");
                    patientCodeLabel.setText(patientCode);
                    patientNameLabel.setText(patientName);
                    System.out.println("Patient: " + patientName + " (" + patientCode + ")");

                    int age = rs.getInt("age");
                    String gender = rs.getString("gender");
                    String ageGender = age + " / " + (gender != null ? gender : "N/A");
                    ageGenderLabel.setText(ageGender);
                    System.out.println("Age/Gender: " + ageGender);

                    grandTotal = rs.getDouble("total_amount");
                    System.out.println("Grand Total: " + grandTotal);
                } else {
                    System.out.println("❌ ERROR: Bill header not found!");
                }
            }

            // Load bill items
            System.out.println("\n--- Loading Bill Items ---");
            try (PreparedStatement pstmt = conn.prepareStatement(itemsQuery)) {
                pstmt.setInt(1, billId);
                System.out.println("Executing items query: " + itemsQuery);
                System.out.println("With bill_id = " + billId);

                ResultSet rs = pstmt.executeQuery();

                billItems.clear();
                int sno = 1;
                int itemCount = 0;

                while (rs.next()) {
                    String serviceName = rs.getString("service_name");
                    double price = rs.getDouble("price");
                    int quantity = rs.getInt("quantity");

                    System.out.println("Item " + sno + ": " + serviceName +
                            " | Price: " + price +
                            " | Qty: " + quantity +
                            " | Total: " + (price * quantity));

                    BillItemDisplay item = new BillItemDisplay(
                            sno++,
                            serviceName,
                            price,
                            quantity
                    );
                    billItems.add(item);
                    itemCount++;
                }

                System.out.println("Total items loaded: " + itemCount);
                System.out.println("Items in ObservableList: " + billItems.size());

                if (itemCount == 0) {
                    System.out.println("⚠️ WARNING: No bill items found for bill_id " + billId);
                    System.out.println("⚠️ Check if items were saved to database!");
                }
            }

            // Update totals
            subtotalLabel.setText(String.format("%.2f", grandTotal));
            grandTotalLabel.setText(String.format("%.2f", grandTotal));

            System.out.println("\n=== BILL DATA LOADED ===");
            System.out.println("Items in table: " + itemsTable.getItems().size());
            System.out.println("========================\n");

        } catch (SQLException e) {
            System.out.println("❌ DATABASE ERROR: " + e.getMessage());
            e.printStackTrace();
            showError("Error loading bill data: " + e.getMessage());
        }
    }

    @FXML
    private void handlePrint() {
        PrinterJob printerJob = PrinterJob.createPrinterJob();

        if (printerJob != null) {
            boolean showDialog = printerJob.showPrintDialog(printArea.getScene().getWindow());

            if (showDialog) {
                // Scale to fit page
                double scaleX = printerJob.getJobSettings().getPageLayout().getPrintableWidth() / printArea.getBoundsInParent().getWidth();
                double scaleY = printerJob.getJobSettings().getPageLayout().getPrintableHeight() / printArea.getBoundsInParent().getHeight();
                double scale = Math.min(scaleX, scaleY);

                printArea.getTransforms().add(new javafx.scene.transform.Scale(scale, scale));

                boolean success = printerJob.printPage(printArea);

                // Reset transform
                printArea.getTransforms().clear();

                if (success) {
                    printerJob.endJob();
                    showSuccess("Bill printed successfully!");
                } else {
                    showError("Printing failed");
                }
            }
        } else {
            showError("No printer found");
        }
    }

    @FXML
    private void handleSavePDF() {
        showInfo("PDF generation feature coming soon!\n\nFor now, please use 'Print to PDF' option from the print dialog.");
    }

    @FXML
    private void handleMarkPaid() {
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Payment");
        confirmAlert.setHeaderText("Mark Bill as Paid");
        confirmAlert.setContentText("Are you sure you want to mark this bill as PAID?\n\nBill No: " + billNoLabel.getText());

        if (confirmAlert.showAndWait().get() != ButtonType.OK) {
            return;
        }

        try {
            updateBillStatus("PAID");
            showSuccess("Bill marked as PAID successfully!");

            // Navigate back to pending bills or dashboard
            handleBack();

        } catch (SQLException e) {
            showError("Error updating bill status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBack() {
        if (dashboardController != null) {
            try {
                // Load pending bills view
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/staff/pendingBills.fxml"));
                Parent pendingBillsView = loader.load();

                PendingBillsController controller = loader.getController();
                if (controller instanceof DashboardAware) {
                    ((DashboardAware) controller).setDashboardController(dashboardController);
                }

                dashboardController.loadViewInContentArea(pendingBillsView);

            } catch (IOException e) {
                showError("Error loading pending bills view: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void updateBillStatus(String status) throws SQLException {
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

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Inner class for display in table
     */
    public static class BillItemDisplay {
        private final SimpleIntegerProperty sno;
        private final SimpleStringProperty serviceName;
        private final SimpleDoubleProperty price;
        private final SimpleIntegerProperty quantity;
        private final SimpleDoubleProperty amount;

        public BillItemDisplay(int sno, String serviceName, double price, int quantity) {
            this.sno = new SimpleIntegerProperty(sno);
            this.serviceName = new SimpleStringProperty(serviceName);
            this.price = new SimpleDoubleProperty(price);
            this.quantity = new SimpleIntegerProperty(quantity);
            this.amount = new SimpleDoubleProperty(price * quantity);
        }

        public int getSno() {
            return sno.get();
        }

        public SimpleIntegerProperty snoProperty() {
            return sno;
        }

        public String getServiceName() {
            return serviceName.get();
        }

        public SimpleStringProperty serviceNameProperty() {
            return serviceName;
        }

        public double getPrice() {
            return price.get();
        }

        public SimpleDoubleProperty priceProperty() {
            return price;
        }

        public int getQuantity() {
            return quantity.get();
        }

        public SimpleIntegerProperty quantityProperty() {
            return quantity;
        }

        public double getAmount() {
            return amount.get();
        }

        public SimpleDoubleProperty amountProperty() {
            return amount;
        }
    }
}