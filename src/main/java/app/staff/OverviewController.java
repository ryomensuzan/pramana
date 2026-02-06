package app.staff;

import app.db.DBConnection;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class OverviewController {

    @FXML private Label todayPatientsLabel;
    @FXML private Label todayBillsLabel;
    @FXML private Label todayCollectionLabel;
    @FXML private Label activeServicesLabel;

    @FXML
    public void initialize() {
        loadOverviewData();
    }

    private void loadOverviewData() {

        try (Connection con = DBConnection.getConnection()) {

            // Patients today
            todayPatientsLabel.setText(
                    querySingleValue(con,
                            "SELECT COUNT(*) FROM patients WHERE DATE(registered_at) = CURDATE()")
            );

            // Final bills today
            todayBillsLabel.setText(
                    querySingleValue(con,
                            "SELECT COUNT(*) FROM bills WHERE status='FINAL' AND DATE(bill_time)=CURDATE()")
            );

            // Collection today
            todayCollectionLabel.setText(
                    querySingleValue(con,
                            "SELECT COALESCE(SUM(total_amount),0) FROM bills WHERE status='FINAL' AND DATE(bill_time)=CURDATE()")
            );

            // Active services
            activeServicesLabel.setText(
                    querySingleValue(con,
                            "SELECT COUNT(*) FROM services WHERE status='ACTIVE'")
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String querySingleValue(Connection con, String sql) throws Exception {
        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : "0";
        }
    }
}
