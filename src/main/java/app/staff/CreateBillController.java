package app.staff;

import app.model.Patient;
import app.model.Service;
import app.model.BillItem;
import app.db.DBConnection;
import app.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class CreateBillController implements DashboardAware {

    @FXML
    private Label patientLabel;

    @FXML
    private Label counterLabel;

    @FXML
    private TableView<BillItem> billTable;

    @FXML
    private TableColumn<BillItem, String> serviceCol;

    @FXML
    private TableColumn<BillItem, Double> priceCol;

    @FXML
    private TableColumn<BillItem, Integer> qtyCol;

    @FXML
    private TableColumn<BillItem, Double> totalCol;

    @FXML
    private ComboBox<String> categoryBox;

    @FXML
    private ComboBox<String> serviceBox;

    @FXML
    private TextField qtyField;

    @FXML
    private Label grandTotalLabel;

    private Patient currentPatient;
    private String counterNo;
    private StaffDashboardController dashboardController;
    private ObservableList<BillItem> billItems = FXCollections.observableArrayList();
    private Map<String, ObservableList<Service>> categoryServicesMap = new HashMap<>();
    private double grandTotal = 0.0;

    // Track if we're editing an existing bill
    private Integer editingBillId = null;

    @Override
    public void setDashboardController(StaffDashboardController controller) {
        this.dashboardController = controller;
    }

    @FXML
    public void initialize() {
        // Set up table columns
        serviceCol.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        priceCol.setCellValueFactory(new PropertyValueFactory<>("price"));
        qtyCol.setCellValueFactory(new PropertyValueFactory<>("quantity"));
        totalCol.setCellValueFactory(new PropertyValueFactory<>("total"));

        // Format price and total columns
        priceCol.setCellFactory(col -> new TableCell<BillItem, Double>() {
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

        totalCol.setCellFactory(col -> new TableCell<BillItem, Double>() {
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

        billTable.setItems(billItems);

        // Set default quantity
        qtyField.setText("1");

        // Get counter number from session manager
        SessionManager session = SessionManager.getInstance();
        if (session.isLoggedIn()) {
            this.counterNo = session.getCounterNo();
            System.out.println("DEBUG: Counter from session: " + this.counterNo);
            if (this.counterNo != null && !this.counterNo.isEmpty()) {
                counterLabel.setText("Counter: " + this.counterNo);
            } else {
                counterLabel.setText("Counter: N/A");
            }
        } else {
            System.out.println("WARNING: No active session!");
            counterLabel.setText("Counter: N/A");
        }

        // Load categories
        loadCategories();

        // Add listener to category selection
        categoryBox.setOnAction(event -> {
            String selectedCategory = categoryBox.getValue();
            if (selectedCategory != null) {
                loadServicesForCategory(selectedCategory);
            }
        });

        // Add context menu for removing items
        addContextMenu();
    }

    /**
     * Set the patient data when navigating from patient search
     */
    public void setPatientData(Patient patient) {
        this.currentPatient = patient;
        this.editingBillId = null; // New bill

        // Update patient label
        if (patient != null) {
            patientLabel.setText(String.format("Patient: %s (%s) - Age: %d, Gender: %s",
                    patient.getFullName(),
                    patient.getPatientCode(),
                    patient.getAge(),
                    patient.getGender()));
        }
    }

    /**
     * Set counter number (call this from dashboard after login)
     */
    public void setCounterNo(String counterNo) {
        this.counterNo = counterNo;
        if (counterNo != null) {
            counterLabel.setText("Counter: " + counterNo);
        }
    }

    /**
     * Load existing bill for editing
     * @param billId The bill ID to load
     */
    public void loadBillForEdit(int billId) {
        this.editingBillId = billId;

        String billQuery = "SELECT b.patient_code, p.full_name, p.gender, p.age, b.counter_no " +
                "FROM bills b " +
                "JOIN patients p ON b.patient_code = p.patient_code " +
                "WHERE b.id = ?";

        String itemsQuery = "SELECT service_name, price, quantity FROM bill_items WHERE bill_id = ?";

        try (Connection conn = DBConnection.getConnection()) {

            // Load bill header
            try (PreparedStatement pstmt = conn.prepareStatement(billQuery)) {
                pstmt.setInt(1, billId);
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    // Create patient object
                    Patient patient = new Patient(
                            0, // id not needed
                            rs.getString("patient_code"),
                            rs.getString("full_name"),
                            rs.getString("gender"),
                            rs.getInt("age"),
                            null, // phone not needed
                            null  // address not needed
                    );
                    this.currentPatient = patient;

                    patientLabel.setText(String.format("Patient: %s (%s) - Age: %d, Gender: %s (EDITING)",
                            patient.getFullName(),
                            patient.getPatientCode(),
                            patient.getAge(),
                            patient.getGender()));

                    setCounterNo(rs.getString("counter_no"));
                }
            }

            // Load bill items
            try (PreparedStatement pstmt = conn.prepareStatement(itemsQuery)) {
                pstmt.setInt(1, billId);
                ResultSet rs = pstmt.executeQuery();

                billItems.clear();
                while (rs.next()) {
                    BillItem item = new BillItem(
                            rs.getString("service_name"),
                            rs.getDouble("price"),
                            rs.getInt("quantity")
                    );
                    billItems.add(item);
                }
                updateGrandTotal();
            }

        } catch (SQLException e) {
            showError("Error loading bill for edit: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadCategories() {
        String query = "SELECT category_name FROM service_category WHERE status = 'ACTIVE' ORDER BY category_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query);
             ResultSet rs = pstmt.executeQuery()) {

            ObservableList<String> categories = FXCollections.observableArrayList();

            while (rs.next()) {
                String categoryName = rs.getString("category_name");
                categories.add(categoryName);
                categoryServicesMap.put(categoryName, FXCollections.observableArrayList());
            }

            categoryBox.setItems(categories);

        } catch (SQLException e) {
            showError("Error loading categories: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadServicesForCategory(String categoryName) {
        // Check if already loaded
        if (!categoryServicesMap.get(categoryName).isEmpty()) {
            updateServiceBox(categoryName);
            return;
        }

        String query = "SELECT s.id, s.service_name, s.price " +
                "FROM services s " +
                "JOIN service_category sc ON s.category_id = sc.id " +
                "WHERE sc.category_name = ? AND s.status = 'ACTIVE' " +
                "ORDER BY s.service_name";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, categoryName);
            ResultSet rs = pstmt.executeQuery();

            ObservableList<Service> services = categoryServicesMap.get(categoryName);
            services.clear();

            while (rs.next()) {
                Service service = new Service(
                        rs.getInt("id"),
                        rs.getString("service_name"),
                        rs.getDouble("price")
                );
                services.add(service);
            }

            updateServiceBox(categoryName);

        } catch (SQLException e) {
            showError("Error loading services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void updateServiceBox(String categoryName) {
        ObservableList<Service> services = categoryServicesMap.get(categoryName);
        ObservableList<String> serviceNames = FXCollections.observableArrayList();

        for (Service service : services) {
            serviceNames.add(service.getServiceName());
        }

        serviceBox.setItems(serviceNames);
        serviceBox.setValue(null);
    }

    @FXML
    private void handleAddService() {
        String category = categoryBox.getValue();
        String serviceName = serviceBox.getValue();
        String qtyText = qtyField.getText().trim();

        // Validation
        if (category == null || serviceName == null || qtyText.isEmpty()) {
            showError("Please select category, service, and enter quantity");
            return;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(qtyText);
            if (quantity <= 0) {
                showError("Quantity must be greater than 0");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Invalid quantity");
            return;
        }

        // Find the service to get price
        Service selectedService = null;
        for (Service service : categoryServicesMap.get(category)) {
            if (service.getServiceName().equals(serviceName)) {
                selectedService = service;
                break;
            }
        }

        if (selectedService == null) {
            showError("Service not found");
            return;
        }

        // Check if service already exists in bill
        for (BillItem item : billItems) {
            if (item.getServiceName().equals(serviceName)) {
                // Update quantity
                item.setQuantity(item.getQuantity() + quantity);
                billTable.refresh();
                updateGrandTotal();
                resetServiceFields();
                return;
            }
        }

        // Add new bill item
        BillItem billItem = new BillItem(
                serviceName,
                selectedService.getPrice(),
                quantity
        );

        billItems.add(billItem);
        updateGrandTotal();
        resetServiceFields();
    }

    private void resetServiceFields() {
        categoryBox.setValue(null);
        serviceBox.setValue(null);
        serviceBox.setItems(FXCollections.observableArrayList());
        qtyField.setText("1");
    }

    private void updateGrandTotal() {
        grandTotal = 0.0;
        for (BillItem item : billItems) {
            grandTotal += item.getTotal();
        }
        grandTotalLabel.setText(String.format("Grand Total: %.2f", grandTotal));
    }

    private void addContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        MenuItem removeItem = new MenuItem("Remove");

        removeItem.setOnAction(event -> {
            BillItem selected = billTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                billItems.remove(selected);
                updateGrandTotal();
            }
        });

        contextMenu.getItems().add(removeItem);
        billTable.setContextMenu(contextMenu);

        // Also add a delete key handler
        billTable.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.DELETE) {
                BillItem selected = billTable.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    billItems.remove(selected);
                    updateGrandTotal();
                }
            }
        });
    }

    @FXML
    private void handleSaveDraft() {
        if (currentPatient == null) {
            showError("No patient selected");
            return;
        }

        if (billItems.isEmpty()) {
            showError("Bill is empty. Add at least one service.");
            return;
        }

        if (editingBillId != null) {
            // Update existing draft
            updateBill("DRAFT");
        } else {
            // Create new draft
            saveBill("DRAFT");
        }
    }

    @FXML
    private void handleGeneratePrebill() {
        if (currentPatient == null) {
            showError("No patient selected");
            return;
        }

        if (billItems.isEmpty()) {
            showError("Bill is empty. Add at least one service.");
            return;
        }

        if (editingBillId != null) {
            // Update existing bill and change status to FINAL
            updateBill("FINAL");
        } else {
            // Create new bill as FINAL
            saveBill("FINAL");
        }
    }

    private void saveBill(String status) {
        Connection conn = null;
        PreparedStatement billStmt = null;
        PreparedStatement itemStmt = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Insert bill
            String billQuery = "INSERT INTO bills (patient_code, counter_no, total_amount, status) " +
                    "VALUES (?, ?, ?, ?)";
            billStmt = conn.prepareStatement(billQuery, Statement.RETURN_GENERATED_KEYS);
            billStmt.setString(1, currentPatient.getPatientCode());
            billStmt.setString(2, counterNo);
            billStmt.setDouble(3, grandTotal);
            billStmt.setString(4, status);
            billStmt.executeUpdate();

            // Get generated bill ID
            ResultSet rs = billStmt.getGeneratedKeys();
            int billId = 0;
            if (rs.next()) {
                billId = rs.getInt(1);
            }

            // Insert bill items
            String itemQuery = "INSERT INTO bill_items (bill_id, service_name, price, quantity) " +
                    "VALUES (?, ?, ?, ?)";
            itemStmt = conn.prepareStatement(itemQuery);

            for (BillItem item : billItems) {
                itemStmt.setInt(1, billId);
                itemStmt.setString(2, item.getServiceName());
                itemStmt.setDouble(3, item.getPrice());
                itemStmt.setInt(4, item.getQuantity());
                itemStmt.addBatch();
            }

            itemStmt.executeBatch();
            conn.commit();

            String message = status.equals("DRAFT") ?
                    "Draft saved successfully! Bill ID: " + billId :
                    "Prebill generated successfully! Bill ID: " + billId;

            showSuccess(message);

            // Clear the form
            clearBill();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            showError("Error saving bill: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (itemStmt != null) itemStmt.close();
                if (billStmt != null) billStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateBill(String status) {
        Connection conn = null;
        PreparedStatement billStmt = null;
        PreparedStatement deleteItemsStmt = null;
        PreparedStatement itemStmt = null;

        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Update bill
            String billQuery = "UPDATE bills SET total_amount = ?, status = ?, bill_time = CURRENT_TIMESTAMP " +
                    "WHERE id = ?";
            billStmt = conn.prepareStatement(billQuery);
            billStmt.setDouble(1, grandTotal);
            billStmt.setString(2, status);
            billStmt.setInt(3, editingBillId);
            billStmt.executeUpdate();

            // Delete old bill items
            String deleteQuery = "DELETE FROM bill_items WHERE bill_id = ?";
            deleteItemsStmt = conn.prepareStatement(deleteQuery);
            deleteItemsStmt.setInt(1, editingBillId);
            deleteItemsStmt.executeUpdate();

            // Insert updated bill items
            String itemQuery = "INSERT INTO bill_items (bill_id, service_name, price, quantity) " +
                    "VALUES (?, ?, ?, ?)";
            itemStmt = conn.prepareStatement(itemQuery);

            for (BillItem item : billItems) {
                itemStmt.setInt(1, editingBillId);
                itemStmt.setString(2, item.getServiceName());
                itemStmt.setDouble(3, item.getPrice());
                itemStmt.setInt(4, item.getQuantity());
                itemStmt.addBatch();
            }

            itemStmt.executeBatch();
            conn.commit();

            String message = status.equals("DRAFT") ?
                    "Draft updated successfully! Bill ID: " + editingBillId :
                    "Bill finalized successfully! Bill ID: " + editingBillId;

            showSuccess(message);

            // Clear the form
            clearBill();

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            showError("Error updating bill: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (itemStmt != null) itemStmt.close();
                if (deleteItemsStmt != null) deleteItemsStmt.close();
                if (billStmt != null) billStmt.close();
                if (conn != null) {
                    conn.setAutoCommit(true);
                    conn.close();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    private void clearBill() {
        billItems.clear();
        updateGrandTotal();
        resetServiceFields();
        editingBillId = null;
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
}