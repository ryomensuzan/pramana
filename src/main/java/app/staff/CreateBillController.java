package app.staff;

import app.db.DBConnection;
import app.model.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class CreateBillController implements DashboardAware {
    private StaffDashboardController dashboardController;
    @Override
    public void setDashboardController(StaffDashboardController controller) {
        this.dashboardController = controller;
    }
    // ===== Patient Info =====
    @FXML private Label patientLabel;
    @FXML private Label counterLabel;

    // ===== Bill Table =====
    @FXML private TableView<BillItem> billTable;
    @FXML private TableColumn<BillItem, String> serviceCol;
    @FXML private TableColumn<BillItem, Double> priceCol;
    @FXML private TableColumn<BillItem, Integer> qtyCol;
    @FXML private TableColumn<BillItem, Double> totalCol;

    // ===== Service Selection =====
    @FXML private ComboBox<ServiceCategoryModel> categoryBox;
    @FXML private ComboBox<ServiceModel> serviceBox;
    @FXML private TextField qtyField;
    @FXML private Label grandTotalLabel;

    private final ObservableList<BillItem> billItems = FXCollections.observableArrayList();

    private Patient patient;
    private String counterNo;
    private double grandTotal = 0.0;
    private Integer billId = null; // null = new draft


    // ================= INIT =================
    @FXML
    public void initialize() {

        // Category display
        categoryBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(ServiceCategoryModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getCategoryName());
            }
        });
        categoryBox.setButtonCell(categoryBox.getCellFactory().call(null));

        // Service display
        serviceBox.setCellFactory(cb -> new ListCell<>() {
            @Override
            protected void updateItem(ServiceModel item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null
                        ? null
                        : item.getServiceName() + " (" + item.getPrice() + ")");
            }
        });
        serviceBox.setButtonCell(serviceBox.getCellFactory().call(null));

        // Table bindings
        serviceCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(d.getValue().getServiceName()));
        priceCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getPrice()));
        qtyCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getQuantity()));
        totalCol.setCellValueFactory(d ->
                new javafx.beans.property.SimpleObjectProperty<>(d.getValue().getTotal()));

        billTable.setItems(billItems);

        // Load categories
        loadCategories();

        // Category → service binding
        categoryBox.setOnAction(e -> loadServicesByCategory());
    }

    // ================= PATIENT DATA =================
    public void initData(Patient patient, String counterNo) {
        this.patient = patient;
        this.counterNo = counterNo;

        patientLabel.setText("Patient: " + patient.getFullName()
                + " (" + patient.getPatientCode() + ")");
        counterLabel.setText("Counter: " + counterNo);
    }

    // ================= LOAD CATEGORIES =================
    private void loadCategories() {

        categoryBox.getItems().clear();

        String sql = """
            SELECT id, category_name, description, status
            FROM service_category
            WHERE status = 'ACTIVE'
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                categoryBox.getItems().add(
                        new ServiceCategoryModel(
                                rs.getInt("id"),
                                rs.getString("category_name"),
                                rs.getString("description"),
                                rs.getString("status")
                        )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= LOAD SERVICES BY CATEGORY =================
    private void loadServicesByCategory() {

        serviceBox.getItems().clear();

        ServiceCategoryModel category = categoryBox.getValue();
        if (category == null) return;

        String sql = """
            SELECT s.id, sc.category_name, s.service_name, s.price, s.status
            FROM services s
            JOIN service_category sc ON s.category_id = sc.id
            WHERE s.category_id = ?
              AND s.status = 'ACTIVE'
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, category.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                serviceBox.getItems().add(
                        new ServiceModel(
                                rs.getInt("id"),
                                rs.getString("category_name"),
                                rs.getString("service_name"),
                                rs.getDouble("price"),
                                rs.getString("status")
                        )
                );
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= ADD SERVICE TO BILL =================
    @FXML
    private void handleAddService() {

        ServiceModel service = serviceBox.getValue();
        if (service == null || qtyField.getText().isEmpty()) return;

        int qty;
        try {
            qty = Integer.parseInt(qtyField.getText());
        } catch (NumberFormatException e) {
            return;
        }

        BillItem item = new BillItem(
                service.getServiceName(),
                service.getPrice(),
                qty
        );

        billItems.add(item);
        grandTotal += item.getTotal();
        grandTotalLabel.setText("Grand Total: " + grandTotal);

        qtyField.clear();
    }
    @FXML
    private void handleGenerateBill() {

        if (billItems.isEmpty()) {
            return;
        }

        String insertBillSql = """
        INSERT INTO bills (patient_id, counter_no, total_amount)
        VALUES (?, ?, ?)
    """;

        String insertItemSql = """
        INSERT INTO bill_items (bill_id, service_name, price, quantity, total)
        VALUES (?, ?, ?, ?, ?)
    """;

        try (Connection con = DBConnection.getConnection()) {

            // ================= INSERT BILL =================
            con.setAutoCommit(false);

            int billId;

            try (PreparedStatement billPs =
                         con.prepareStatement(insertBillSql, PreparedStatement.RETURN_GENERATED_KEYS)) {

                billPs.setString(1, patient.getPatientCode());
                billPs.setString(2, counterNo);
                billPs.setDouble(3, grandTotal);

                billPs.executeUpdate();

                ResultSet rs = billPs.getGeneratedKeys();
                if (!rs.next()) {
                    con.rollback();
                    return;
                }

                billId = rs.getInt(1);
            }

            // ================= INSERT BILL ITEMS =================
            try (PreparedStatement itemPs = con.prepareStatement(insertItemSql)) {

                for (BillItem item : billItems) {
                    itemPs.setInt(1, billId);
                    itemPs.setString(2, item.getServiceName());
                    itemPs.setDouble(3, item.getPrice());
                    itemPs.setInt(4, item.getQuantity());
                    itemPs.setDouble(5, item.getTotal());
                    itemPs.addBatch();
                }

                itemPs.executeBatch();
            }

            con.commit();

            // ================= RESET UI =================
            billItems.clear();
            grandTotal = 0;
            grandTotalLabel.setText("Grand Total: 0");

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Bill generated successfully!");
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void init(BillContext context) {

        this.patient = context.getPatient();

        patientLabel.setText(
                "Patient: " + patient.getFullName()
                        + " (" + patient.getPatientCode() + ")"
        );

        if (context.getBillId() != null) {
            loadExistingBill(context.getBillId());
        }

        loadCategories();
    }
    private void loadExistingBill(int billId) {

        billItems.clear();
        grandTotal = 0;
        this.billId = billId;

        String billSql = """
        SELECT patient_code, counter_no, total_amount
        FROM bills
        WHERE id = ? AND status = 'DRAFT'
    """;

        String itemsSql = """
        SELECT service_name, price, quantity
        FROM bill_items
        WHERE bill_id = ?
    """;

        try (Connection con = DBConnection.getConnection()) {

            // 1️⃣ Load bill header
            try (PreparedStatement ps = con.prepareStatement(billSql)) {
                ps.setInt(1, billId);
                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    System.out.println("Draft bill not found: " + billId);
                    return;
                }

                counterNo = rs.getString("counter_no");
                grandTotal = rs.getDouble("total_amount");
            }

            // 2️⃣ Load bill items
            try (PreparedStatement ps = con.prepareStatement(itemsSql)) {
                ps.setInt(1, billId);
                ResultSet rs = ps.executeQuery();

                while (rs.next()) {

                    BillItem item = new BillItem(
                            rs.getString("service_name"),
                            rs.getDouble("price"),
                            rs.getInt("quantity")
                    );

                    billItems.add(item);
                }
            }

            // 3️⃣ Update UI
            billTable.setItems(billItems);
            recalculateGrandTotal();

            System.out.println("Loaded draft bill ID: " + billId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recalculateGrandTotal() {

        grandTotal = billItems.stream()
                .mapToDouble(BillItem::getTotal)
                .sum();

        grandTotalLabel.setText("Grand Total: " + grandTotal);
    }



    @FXML
    private void handleSaveDraft() {

        if (patient == null || billItems.isEmpty()) {
            return;
        }

        try (Connection con = DBConnection.getConnection()) {

            con.setAutoCommit(false);

            if (billId == null) {
                billId = insertBill(con);
            } else {
                updateBill(con);
                deleteBillItems(con);
            }

            insertBillItems(con);

            con.commit();

            System.out.println("Draft saved. Bill ID = " + billId);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int insertBill(Connection con) throws Exception {

        String sql = """
        INSERT INTO bills (patient_code, counter_no, total_amount, status)
        VALUES (?, ?, ?, 'DRAFT')
    """;

        try (PreparedStatement ps =
                     con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, patient.getPatientCode());
            ps.setString(2, counterNo);
            ps.setDouble(3, grandTotal);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            return rs.getInt(1);
        }
    }

    private void updateBill(Connection con) throws Exception {

        String sql = """
        UPDATE bills
        SET total_amount = ?
        WHERE id = ? AND status = 'DRAFT'
    """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, grandTotal);
            ps.setInt(2, billId);
            ps.executeUpdate();
        }
    }

    private void deleteBillItems(Connection con) throws Exception {

        String sql = "DELETE FROM bill_items WHERE bill_id = ?";

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, billId);
            ps.executeUpdate();
        }
    }

    private void insertBillItems(Connection con) throws Exception {

        String sql = """
        INSERT INTO bill_items (bill_id, service_name, price, quantity)
        VALUES (?, ?, ?, ?)
    """;

        try (PreparedStatement ps = con.prepareStatement(sql)) {

            for (BillItem item : billItems) {
                ps.setInt(1, billId);
                ps.setString(2, item.getServiceName());
                ps.setDouble(3, item.getPrice());
                ps.setInt(4, item.getQuantity());
                ps.addBatch();
            }

            ps.executeBatch();
        }
    }

    @FXML
    private void handleGeneratePrebill() {

        if (patient == null) return;

        handleSaveDraft(); // must assign billId

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/staff/preBill.fxml")
            );

            Parent view = loader.load();
            PreBillController controller = loader.getController();

            controller.setDashboardController(dashboardController);
            controller.init(patient, billId);

            dashboardController.loadViewWithContext(view);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
