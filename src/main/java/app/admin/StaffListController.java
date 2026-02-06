package app.admin;

import app.model.Staff;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class StaffListController implements Initializable {

    @FXML private TextField counterField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label messageLabel;

    @FXML private TableView<Staff> staffTable;
    @FXML private TableColumn<Staff, Integer> idCol;
    @FXML private TableColumn<Staff, String> counterCol;
    @FXML private TableColumn<Staff, String> usernameCol;

    private ObservableList<Staff> staffList = FXCollections.observableArrayList();

    private Connection con;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        connectDB();

        idCol.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        counterCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getCounterNo()));
        usernameCol.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(data.getValue().getUsername()));

        loadStaff();
    }

    private void connectDB() {
        try {
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/pramana",
                    "root",
                    ""
            );
            System.out.println("✅ Database connected");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddStaff() {

        String counter = counterField.getText();
        String username = usernameField.getText();
        String password = passwordField.getText();
        String role = "STAFF";

        if (counter.isEmpty() || username.isEmpty() || password.isEmpty()) {
            messageLabel.setText("All fields are required");
            return;
        }

        String sql = "INSERT INTO users(counter_no, username, password, role) VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, counter);
            ps.setString(2, username);
            ps.setString(3, password);
            ps.setString(4,role);
            ps.executeUpdate();

            messageLabel.setText("Staff added successfully ✔");
            clearFields();
            loadStaff();

        } catch (SQLException e) {
            messageLabel.setText("Error: Username or Counter already exists");
        }
    }

    private void loadStaff() {
        staffList.clear();

        String sql = "SELECT * FROM users WHERE role='STAFF' ";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                staffList.add(
                        new Staff(
                                rs.getInt("id"),
                                rs.getString("counter_no"),
                                rs.getString("username")

                        )
                );
            }
            staffTable.setItems(staffList);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        counterField.clear();
        usernameField.clear();
        passwordField.clear();
    }
}
