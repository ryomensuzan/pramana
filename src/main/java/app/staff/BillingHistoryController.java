package app.staff;

import app.db.DBConnection;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;

public class BillingHistoryController {

    @FXML private TableView<BillRecord> billTable;
    @FXML private TableColumn<BillRecord, Integer> billIdCol;
    @FXML private TableColumn<BillRecord, String> patientCol;
    @FXML private TableColumn<BillRecord, String> counterCol;
    @FXML private TableColumn<BillRecord, Double> amountCol;
    @FXML private TableColumn<BillRecord, String> timeCol;

    @FXML private Label totalLabel;

    private final ObservableList<BillRecord> bills = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        billIdCol.setCellValueFactory(new PropertyValueFactory<>("billId"));
        patientCol.setCellValueFactory(new PropertyValueFactory<>("patientName"));
        counterCol.setCellValueFactory(new PropertyValueFactory<>("counterNo"));
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        timeCol.setCellValueFactory(new PropertyValueFactory<>("billTime"));

        billTable.setItems(bills);

        loadFinalBills();
    }

    private void loadFinalBills() {
        bills.clear();

        String sql = """
            SELECT b.id, b.patient_code, b.counter_no, b.total_amount, b.bill_time, p.full_name
            FROM bills b
            JOIN patients p ON p.patient_code = b.patient_code
            WHERE b.status = 'FINAL'
            ORDER BY b.bill_time DESC
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

            while (rs.next()) {
                BillRecord br = new BillRecord(
                        rs.getInt("id"),
                        rs.getString("full_name"),
                        rs.getString("counter_no"),
                        rs.getDouble("total_amount"),
                        rs.getTimestamp("bill_time").toLocalDateTime().format(formatter)
                );
                bills.add(br);
            }

            totalLabel.setText("Total Bills: " + bills.size());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ==== BillRecord class for TableView ====
    public static class BillRecord {
        private final Integer billId;
        private final String patientName;
        private final String counterNo;
        private final Double amount;
        private final String billTime;

        public BillRecord(Integer billId, String patientName, String counterNo, Double amount, String billTime) {
            this.billId = billId;
            this.patientName = patientName;
            this.counterNo = counterNo;
            this.amount = amount;
            this.billTime = billTime;
        }

        public Integer getBillId() { return billId; }
        public String getPatientName() { return patientName; }
        public String getCounterNo() { return counterNo; }
        public Double getAmount() { return amount; }
        public String getBillTime() { return billTime; }
    }
}
