package app.admin;
import app.db.DBConnection;
import app.model.ServiceCategoryModel;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ServiceCategoryController {

    @FXML
    private TextField categoryNameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private Label messageLabel;

    @FXML
    private TableView<ServiceCategoryModel> categoryTable;

    @FXML
    private TableColumn<ServiceCategoryModel, Integer> idColumn;

    @FXML
    private TableColumn<ServiceCategoryModel, String> nameColumn;

    @FXML
    private TableColumn<ServiceCategoryModel, String> descColumn;

    @FXML
    private TableColumn<ServiceCategoryModel, String> statusColumn;

    private final ObservableList<ServiceCategoryModel> categoryList =
            FXCollections.observableArrayList();

    // ================= INITIALIZE =================
    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("categoryName"));
        descColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));

        loadCategories();
    }

    // ================= SAVE =================
    @FXML
    private void handleSaveCategory() {

        String name = categoryNameField.getText();
        String desc = descriptionField.getText();

        if (name.isEmpty()) {
            messageLabel.setText("Category name is required");
            messageLabel.setStyle("-fx-text-fill:red;");
            return;
        }

        String sql = """
            INSERT INTO service_category (category_name, description)
            VALUES (?, ?)
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, desc);

            ps.executeUpdate();

            messageLabel.setText("Category added successfully");
            messageLabel.setStyle("-fx-text-fill:green;");

            clearFields();
            loadCategories();

        } catch (Exception e) {
            e.printStackTrace();
            messageLabel.setText("Category already exists");
            messageLabel.setStyle("-fx-text-fill:red;");
        }
    }

    // ================= LOAD =================
    private void loadCategories() {
        categoryList.clear();

        String sql = "SELECT * FROM service_category";

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                categoryList.add(
                        new ServiceCategoryModel(
                                rs.getInt("id"),
                                rs.getString("category_name"),
                                rs.getString("description"),
                                rs.getString("status")
                        )
                );
            }

            categoryTable.setItems(categoryList);

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
        categoryNameField.clear();
        descriptionField.clear();
    }
}

