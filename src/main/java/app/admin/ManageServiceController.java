package app.admin;

import app.db.DBConnection;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.*;
import java.util.Optional;

public class ManageServiceController {

    @FXML
    private ComboBox<ServiceCategory> categoryComboBox;

    @FXML
    private TextField serviceNameField;

    @FXML
    private TextField priceField;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<Service> serviceTable;

    @FXML
    private TableColumn<Service, Integer> idColumn;

    @FXML
    private TableColumn<Service, String> categoryColumn;

    @FXML
    private TableColumn<Service, String> nameColumn;

    @FXML
    private TableColumn<Service, Double> priceColumn;

    @FXML
    private TableColumn<Service, String> statusColumn;

    @FXML
    private TableColumn<Service, Void> actionsCol;

    private ObservableList<Service> serviceList = FXCollections.observableArrayList();
    private ObservableList<ServiceCategory> categoryList = FXCollections.observableArrayList();
    private Integer selectedServiceId = null; // For update mode

    @FXML
    public void initialize() {
        setupTableColumns();
        loadCategories();
        loadServices();
    }

    /**
     * Setup table columns
     */
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Format price column with Rs.
        priceColumn.setCellFactory(column -> new TableCell<Service, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText("Rs. " + String.format("%.2f", price));
                }
            }
        });

        // Actions column with Update and Toggle Status buttons
        actionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button updateBtn = new Button("Update");
            private final Button toggleStatusBtn = new Button();
            private final HBox container = new HBox(8, updateBtn, toggleStatusBtn);

            {
                container.setAlignment(Pos.CENTER);
                updateBtn.getStyleClass().add("action-btn-update");
                toggleStatusBtn.getStyleClass().add("action-btn-toggle");

                updateBtn.setOnAction(event -> {
                    Service service = getTableView().getItems().get(getIndex());
                    handleUpdateService(service);
                });

                toggleStatusBtn.setOnAction(event -> {
                    Service service = getTableView().getItems().get(getIndex());
                    handleToggleStatus(service);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Service service = getTableView().getItems().get(getIndex());
                    String status = service.getStatus();

                    if ("ACTIVE".equalsIgnoreCase(status)) {
                        toggleStatusBtn.setText("Deactivate");
                        toggleStatusBtn.getStyleClass().removeAll("action-btn-activate");
                        toggleStatusBtn.getStyleClass().add("action-btn-deactivate");
                    } else {
                        toggleStatusBtn.setText("Activate");
                        toggleStatusBtn.getStyleClass().removeAll("action-btn-deactivate");
                        toggleStatusBtn.getStyleClass().add("action-btn-activate");
                    }

                    setGraphic(container);
                }
            }
        });
    }

    /**
     * Load active categories into ComboBox
     */
    private void loadCategories() {
        categoryList.clear();
        String query = "SELECT id, category_name FROM service_category WHERE status = 'ACTIVE' ORDER BY category_name";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                ServiceCategory category = new ServiceCategory(
                        rs.getInt("id"),
                        rs.getString("category_name")
                );
                categoryList.add(category);
            }

            categoryComboBox.setItems(categoryList);

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error loading categories: " + e.getMessage(), "error");
        }
    }

    /**
     * Load all services from database
     */
    private void loadServices() {
        serviceList.clear();
        String query = """
            SELECT s.id, s.service_name, s.price, s.status, 
                   sc.category_name, s.category_id
            FROM services s
            JOIN service_category sc ON s.category_id = sc.id
            ORDER BY s.id DESC
        """;

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                Service service = new Service(
                        rs.getInt("id"),
                        rs.getInt("category_id"),
                        rs.getString("category_name"),
                        rs.getString("service_name"),
                        rs.getDouble("price"),
                        rs.getString("status")
                );
                serviceList.add(service);
            }

            serviceTable.setItems(serviceList);

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error loading services: " + e.getMessage(), "error");
        }
    }

    /**
     * Handle Save Service (Create or Update)
     */
    @FXML
    private void handleSaveService() {
        ServiceCategory selectedCategory = categoryComboBox.getValue();
        String serviceName = serviceNameField.getText().trim();
        String priceText = priceField.getText().trim();

        // Validation
        if (selectedCategory == null) {
            showMessage("Please select a category!", "error");
            return;
        }

        if (serviceName.isEmpty()) {
            showMessage("Service name is required!", "error");
            return;
        }

        if (priceText.isEmpty()) {
            showMessage("Price is required!", "error");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
            if (price < 0) {
                showMessage("Price cannot be negative!", "error");
                return;
            }
        } catch (NumberFormatException e) {
            showMessage("Invalid price format! Please enter a valid number.", "error");
            return;
        }

        if (selectedServiceId == null) {
            // CREATE new service
            createService(selectedCategory.getId(), serviceName, price);
        } else {
            // UPDATE existing service
            updateService(selectedServiceId, selectedCategory.getId(), serviceName, price);
        }
    }

    /**
     * Create new service
     */
    private void createService(int categoryId, String serviceName, double price) {
        String sql = "INSERT INTO services (category_id, service_name, price, status) VALUES (?, ?, ?, 'ACTIVE')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, categoryId);
            pstmt.setString(2, serviceName);
            pstmt.setDouble(3, price);

            pstmt.executeUpdate();
            showMessage("Service created successfully!", "success");
            handleClear();
            loadServices();

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error creating service: " + e.getMessage(), "error");
        }
    }

    /**
     * Update existing service
     */
    private void updateService(int serviceId, int categoryId, String serviceName, double price) {
        String sql = "UPDATE services SET category_id = ?, service_name = ?, price = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, categoryId);
            pstmt.setString(2, serviceName);
            pstmt.setDouble(3, price);
            pstmt.setInt(4, serviceId);

            pstmt.executeUpdate();
            showMessage("Service updated successfully!", "success");
            handleClear();
            loadServices();

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error updating service: " + e.getMessage(), "error");
        }
    }

    /**
     * Handle Update button click - populate form
     */
    private void handleUpdateService(Service service) {
        selectedServiceId = service.getId();

        // Find and select the category in ComboBox
        for (ServiceCategory category : categoryList) {
            if (category.getId() == service.getCategoryId()) {
                categoryComboBox.setValue(category);
                break;
            }
        }

        serviceNameField.setText(service.getServiceName());
        priceField.setText(String.valueOf(service.getPrice()));
        showMessage("Update mode - Modify and click Save", "info");
    }

    /**
     * Handle Toggle Status (Activate/Deactivate)
     */
    private void handleToggleStatus(Service service) {
        String currentStatus = service.getStatus();
        String newStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "INACTIVE" : "ACTIVE";
        String action = "ACTIVE".equalsIgnoreCase(currentStatus) ? "deactivate" : "activate";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Action");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to " + action + " service: " + service.getServiceName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "UPDATE services SET status = ? WHERE id = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, newStatus);
                pstmt.setInt(2, service.getId());
                pstmt.executeUpdate();

                showMessage("Service " + action + "d successfully!", "success");
                loadServices();

            } catch (SQLException e) {
                e.printStackTrace();
                showMessage("Error updating status: " + e.getMessage(), "error");
            }
        }
    }

    /**
     * Clear form fields
     */
    @FXML
    private void handleClear() {
        selectedServiceId = null;
        categoryComboBox.setValue(null);
        serviceNameField.clear();
        priceField.clear();
        messageLabel.setText("");
    }

    /**
     * Show message to user
     */
    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("success", "error", "info");
        messageLabel.getStyleClass().add(type);
    }

    // ========================================
    // SERVICE CATEGORY MODEL (for ComboBox)
    // ========================================
    public static class ServiceCategory {
        private final int id;
        private final String categoryName;

        public ServiceCategory(int id, String categoryName) {
            this.id = id;
            this.categoryName = categoryName;
        }

        public int getId() { return id; }
        public String getCategoryName() { return categoryName; }

        @Override
        public String toString() {
            return categoryName; // Display in ComboBox
        }
    }

    // ========================================
    // SERVICE MODEL CLASS
    // ========================================
    public static class Service {
        private final SimpleIntegerProperty id;
        private final SimpleIntegerProperty categoryId;
        private final SimpleStringProperty categoryName;
        private final SimpleStringProperty serviceName;
        private final SimpleDoubleProperty price;
        private final SimpleStringProperty status;

        public Service(int id, int categoryId, String categoryName, String serviceName, double price, String status) {
            this.id = new SimpleIntegerProperty(id);
            this.categoryId = new SimpleIntegerProperty(categoryId);
            this.categoryName = new SimpleStringProperty(categoryName);
            this.serviceName = new SimpleStringProperty(serviceName);
            this.price = new SimpleDoubleProperty(price);
            this.status = new SimpleStringProperty(status);
        }

        // Getters
        public int getId() { return id.get(); }
        public int getCategoryId() { return categoryId.get(); }
        public String getCategoryName() { return categoryName.get(); }
        public String getServiceName() { return serviceName.get(); }
        public double getPrice() { return price.get(); }
        public String getStatus() { return status.get(); }

        // Property getters (for TableView binding)
        public SimpleIntegerProperty idProperty() { return id; }
        public SimpleStringProperty categoryNameProperty() { return categoryName; }
        public SimpleStringProperty serviceNameProperty() { return serviceName; }
        public SimpleDoubleProperty priceProperty() { return price; }
        public SimpleStringProperty statusProperty() { return status; }
    }
}