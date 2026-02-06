package app.admin;
import app.db.DBConnection;
import app.model.CategoryItem;
import app.model.ServiceModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ManageServiceController {

    @FXML
    private ComboBox<CategoryItem> categoryComboBox;

    @FXML
    private TextField serviceNameField;

    @FXML
    private TextField priceField;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<ServiceModel> serviceTable;

    @FXML
    private TableColumn<ServiceModel, Integer> idColumn;

    @FXML
    private TableColumn<ServiceModel, String> categoryColumn;

    @FXML
    private TableColumn<ServiceModel, String> nameColumn;

    @FXML
    private TableColumn<ServiceModel, Double> priceColumn;

    @FXML
    private TableColumn<ServiceModel, String> statusColumn;

    private final ObservableList<ServiceModel> serviceList =
            FXCollections.observableArrayList();

    // ================= INITIALIZE =================
    @FXML
    private void initialize() {
        loadCategories();

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        categoryColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        priceColumn.setCellValueFactory(new PropertyValueFactory<>("price"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadServices();
    }

    // ================= LOAD CATEGORIES =================
    private void loadCategories() {
        String sql = "SELECT id, category_name FROM service_category WHERE status='ACTIVE'";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            ObservableList<CategoryItem> categories =
                    FXCollections.observableArrayList();

            while (rs.next()) {
                categories.add(
                        new CategoryItem(
                                rs.getInt("id"),
                                rs.getString("category_name")
                        )
                );
            }

            categoryComboBox.setItems(categories);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= SAVE SERVICE =================
    @FXML
    private void handleSaveService() {

        CategoryItem category = categoryComboBox.getValue();
        String name = serviceNameField.getText();
        String priceText = priceField.getText();

        if (category == null || name.isEmpty() || priceText.isEmpty()) {
            messageLabel.setText("All fields are required");
            messageLabel.setStyle("-fx-text-fill:red;");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            messageLabel.setText("Invalid price");
            messageLabel.setStyle("-fx-text-fill:red;");
            return;
        }

        String sql = """
            INSERT INTO services (category_id, service_name, price)
            VALUES (?, ?, ?)
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, category.getId());
            ps.setString(2, name);
            ps.setDouble(3, price);

            ps.executeUpdate();

            messageLabel.setText("Service added successfully");
            messageLabel.setStyle("-fx-text-fill:green;");

            clearFields();
            loadServices();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Service already exists");
            messageLabel.setStyle("-fx-text-fill:red;");
        }
    }

    // ================= LOAD SERVICES =================
    private void loadServices() {
        serviceList.clear();

        String sql = """
            SELECT s.id, sc.category_name, s.service_name, s.price, s.status
            FROM services s
            JOIN service_category sc ON s.category_id = sc.id
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                serviceList.add(
                        new ServiceModel(
                                rs.getInt("id"),
                                rs.getString("category_name"),
                                rs.getString("service_name"),
                                rs.getDouble("price"),
                                rs.getString("status")
                        )
                );
            }

            serviceTable.setItems(serviceList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= CLEAR =================
    @FXML
    private void handleClear() {
        clearFields();
    }

    private void clearFields() {
        serviceNameField.clear();
        priceField.clear();
        categoryComboBox.getSelectionModel().clearSelection();
    }
}

