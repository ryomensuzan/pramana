package app.model;

import java.sql.Timestamp;

public class PendingBillRow {

    private int billId;
    private String patientCode;
    private String patientName;
    private double totalAmount;
    private Timestamp billTime;

    public PendingBillRow(int billId, String patientCode,
                          String patientName, double totalAmount,
                          Timestamp billTime) {
        this.billId = billId;
        this.patientCode = patientCode;
        this.patientName = patientName;
        this.totalAmount = totalAmount;
        this.billTime = billTime;
    }

    public int getBillId() { return billId; }
    public String getPatientCode() { return patientCode; }
    public String getPatientName() { return patientName; }
    public double getTotalAmount() { return totalAmount; }
    public Timestamp getBillTime() { return billTime; }
}
