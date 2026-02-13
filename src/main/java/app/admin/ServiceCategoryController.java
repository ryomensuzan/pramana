package app.admin;
import app.db.DBConnection;
import app.model.ServiceCategory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import java.sql.*;
import java.util.Optional;

public class ServiceCategoryController {

    @FXML
    private TextField categoryNameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<ServiceCategory> categoryTable;

    @FXML
    private TableColumn<ServiceCategory, Integer> idColumn;

    @FXML
    private TableColumn<ServiceCategory, String> nameColumn;

    @FXML
    private TableColumn<ServiceCategory, String> descColumn;

    @FXML
    private TableColumn<ServiceCategory, String> statusColumn;

    @FXML
    private TableColumn<ServiceCategory, Void> actionsCol;

    private ObservableList<ServiceCategory> categoryList = FXCollections.observableArrayList();
    private Integer selectedCategoryId = null; // For update mode

    @FXML
    public void initialize() {
        setupTableColumns();
        loadCategories();
    }
    // Setup table columns
    private void setupTableColumns() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        descColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

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
                    ServiceCategory category = getTableView().getItems().get(getIndex());
                    handleUpdateCategory(category);
                });

                toggleStatusBtn.setOnAction(event -> {
                    ServiceCategory category = getTableView().getItems().get(getIndex());
                    handleToggleStatus(category);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ServiceCategory category = getTableView().getItems().get(getIndex());
                    String status = category.getStatus();

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

    //Load all categories from database
    private void loadCategories() {
        categoryList.clear();
        String query = "SELECT id, category_name, description, status FROM service_category ORDER BY id";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                ServiceCategory category = new ServiceCategory(
                        rs.getInt("id"),
                        rs.getString("category_name"),
                        rs.getString("description"),
                        rs.getString("status")
                );
                categoryList.add(category);
            }

            categoryTable.setItems(categoryList);

        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error loading categories: " + e.getMessage(), "error");
        }
    }

    //Handle Save Category (Create or Update)
    @FXML
    private void handleSaveCategory() {
        String categoryName = categoryNameField.getText().trim();
        String description = descriptionField.getText().trim();

        // Validation
        if (categoryName.isEmpty()) {
            showMessage("Category name is required!", "error");
            return;
        }

        if (selectedCategoryId == null) {
            // CREATE new category
            createCategory(categoryName, description);
        } else {
            // UPDATE existing category
            updateCategory(selectedCategoryId, categoryName, description);
        }
    }

    //Create new category
    private void createCategory(String categoryName, String description) {
        String sql = "INSERT INTO service_category (category_name, description, status) VALUES (?, ?, 'ACTIVE')";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, categoryName);
            pstmt.setString(2, description.isEmpty() ? null : description);

            pstmt.executeUpdate();
            showMessage("Category created successfully!", "success");
            handleClear();
            loadCategories();

        } catch (SQLIntegrityConstraintViolationException e) {
            showMessage("Category name already exists!", "error");
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error creating category: " + e.getMessage(), "error");
        }
    }

    //Update existing category
    private void updateCategory(int categoryId, String categoryName, String description) {
        String sql = "UPDATE service_category SET category_name = ?, description = ? WHERE id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, categoryName);
            pstmt.setString(2, description.isEmpty() ? null : description);
            pstmt.setInt(3, categoryId);

            pstmt.executeUpdate();
            showMessage("Category updated successfully!", "success");
            handleClear();
            loadCategories();

        } catch (SQLIntegrityConstraintViolationException e) {
            showMessage("Category name already exists!", "error");
        } catch (SQLException e) {
            e.printStackTrace();
            showMessage("Error updating category: " + e.getMessage(), "error");
        }
    }

    //Handle Update button click - populate form
    private void handleUpdateCategory(ServiceCategory category) {
        selectedCategoryId = category.getId();
        categoryNameField.setText(category.getCategoryName());
        descriptionField.setText(category.getDescription() != null ? category.getDescription() : "");
        showMessage("Update mode - Modify and click Save", "info");
    }

    //Handle Toggle Status (Activate/Deactivate)
    private void handleToggleStatus(ServiceCategory category) {
        String currentStatus = category.getStatus();
        String newStatus = "ACTIVE".equalsIgnoreCase(currentStatus) ? "INACTIVE" : "ACTIVE";
        String action = "ACTIVE".equalsIgnoreCase(currentStatus) ? "deactivate" : "activate";

        // Check if category has active services before deactivating
        if ("INACTIVE".equals(newStatus)) {
            if (hasActiveServices(category.getId())) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Cannot Deactivate");
                alert.setHeaderText(null);
                alert.setContentText("This category has active services. Please deactivate all services first.");
                alert.showAndWait();
                return;
            }
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Action");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to " + action + " category: " + category.getCategoryName() + "?");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String sql = "UPDATE service_category SET status = ? WHERE id = ?";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, newStatus);
                pstmt.setInt(2, category.getId());
                pstmt.executeUpdate();

                showMessage("Category " + action + "d successfully!", "success");
                loadCategories();

            } catch (SQLException e) {
                e.printStackTrace();
                showMessage("Error updating status: " + e.getMessage(), "error");
            }
        }
    }

    // Check if category has active services
    private boolean hasActiveServices(int categoryId) {
        String query = "SELECT COUNT(*) FROM services WHERE category_id = ? AND status = 'ACTIVE'";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, categoryId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    // Clear form fields
    @FXML
    private void handleClear() {
        selectedCategoryId = null;
        categoryNameField.clear();
        descriptionField.clear();
        messageLabel.setText("");
    }

    // Show message to user
    private void showMessage(String message, String type) {
        messageLabel.setText(message);
        messageLabel.getStyleClass().removeAll("success", "error", "info");
        messageLabel.getStyleClass().add(type);
    }
}