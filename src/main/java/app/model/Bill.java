package app.model;

import javafx.beans.property.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Bill {
    private final IntegerProperty id;
    private final StringProperty patientCode;
    private final StringProperty patientName;
    private final DoubleProperty totalAmount;
    private final StringProperty billTime;
    private final StringProperty counterNo;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Bill(int id, String patientCode, String patientName, double totalAmount,
                LocalDateTime billTime, String counterNo) {
        this.id = new SimpleIntegerProperty(id);
        this.patientCode = new SimpleStringProperty(patientCode);
        this.patientName = new SimpleStringProperty(patientName);
        this.totalAmount = new SimpleDoubleProperty(totalAmount);
        this.billTime = new SimpleStringProperty(billTime.format(FORMATTER));
        this.counterNo = new SimpleStringProperty(counterNo);
    }

    // ID
    public int getId() {
        return id.get();
    }

    public void setId(int id) {
        this.id.set(id);
    }

    public IntegerProperty idProperty() {
        return id;
    }

    // Patient Code
    public String getPatientCode() {
        return patientCode.get();
    }

    public void setPatientCode(String patientCode) {
        this.patientCode.set(patientCode);
    }

    public StringProperty patientCodeProperty() {
        return patientCode;
    }

    // Patient Name
    public String getPatientName() {
        return patientName.get();
    }

    public void setPatientName(String patientName) {
        this.patientName.set(patientName);
    }

    public StringProperty patientNameProperty() {
        return patientName;
    }

    // Total Amount
    public double getTotalAmount() {
        return totalAmount.get();
    }

    public void setTotalAmount(double totalAmount) {
        this.totalAmount.set(totalAmount);
    }

    public DoubleProperty totalAmountProperty() {
        return totalAmount;
    }

    // Bill Time
    public String getBillTime() {
        return billTime.get();
    }

    public void setBillTime(String billTime) {
        this.billTime.set(billTime);
    }

    public StringProperty billTimeProperty() {
        return billTime;
    }

    // Counter No
    public String getCounterNo() {
        return counterNo.get();
    }

    public void setCounterNo(String counterNo) {
        this.counterNo.set(counterNo);
    }

    public StringProperty counterNoProperty() {
        return counterNo;
    }

    @Override
    public String toString() {
        return String.format("Bill #%d - %s (%s) - %.2f - %s",
                getId(), getPatientName(), getPatientCode(), getTotalAmount(), getBillTime());
    }
}