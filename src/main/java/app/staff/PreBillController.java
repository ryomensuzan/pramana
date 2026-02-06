package app.staff;

import app.db.DBConnection;
import app.model.BillItem;
import app.model.Patient;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.print.*;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PreBillController implements DashboardAware {
    private StaffDashboardController dashboardController;

    @Override
    public void setDashboardController(StaffDashboardController dashboardController) {
        this.dashboardController = dashboardController;
    }

    @FXML private Label patientLabel;
    @FXML private Label billInfoLabel;
    @FXML private Label grandTotalLabel;

    @FXML private VBox billRoot;
    @FXML private GridPane itemsGrid;

    private final ObservableList<BillItem> items = FXCollections.observableArrayList();

    private Patient patient;
    private int billId;


    private void buildItemsGrid() {
        itemsGrid.getChildren().clear();

        // headers
        itemsGrid.addRow(0,
                bold("Service"),
                bold("Price"),
                bold("Qty"),
                bold("Total")
        );

        int row = 1;
        for (BillItem item : items) {
            itemsGrid.addRow(row++,
                    new Label(item.getServiceName()),
                    new Label(String.format("%.2f", item.getPrice())),
                    new Label(String.valueOf(item.getQuantity())),
                    new Label(String.format("%.2f", item.getTotal()))
            );
        }
    }

    private Label bold(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight:bold;");
        return l;
    }


    public void init(Patient patient, int billId) {
        this.patient = patient;
        this.billId = billId;

        patientLabel.setText(
                "Patient: " + patient.getFullName()
                        + " (" + patient.getPatientCode() + ")"
        );

        billInfoLabel.setText("Bill ID: " + billId);
        loadBillItems();
    }

    private void loadBillItems() {

        items.clear();
        itemsGrid.getChildren().clear();

        String sql = """
        SELECT service_name, price, quantity
        FROM bill_items
        WHERE bill_id = ?
    """;

        double total = 0;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, billId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                BillItem item = new BillItem(
                        rs.getString("service_name"),
                        rs.getDouble("price"),
                        rs.getInt("quantity")
                );
                items.add(item);
                total += item.getTotal();
            }

            buildItemsGrid();
            grandTotalLabel.setText("Grand Total: " + String.format("%.2f", total));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ================= PRINT =================
    @FXML
    private void handlePrint() {

        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) return;

        boolean proceed = job.showPrintDialog(billRoot.getScene().getWindow());
        if (!proceed) return;

        PageLayout layout = job.getPrinter().createPageLayout(
                Paper.A4,
                PageOrientation.PORTRAIT,
                Printer.MarginType.DEFAULT
        );

        billRoot.setPrefWidth(layout.getPrintableWidth());
        billRoot.setMaxWidth(layout.getPrintableWidth());

        job.printPage(layout, billRoot);
        job.endJob();

    }


    // ================= BACK =================
    @FXML
    private void handleBack() {
        // simply go back to pending bills or create bill
        billRoot.getScene().getWindow().hide();
    }
}
