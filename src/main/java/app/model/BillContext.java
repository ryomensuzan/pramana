package app.model;

public class BillContext {

    private Patient patient;
    private Integer billId; // null = new bill (draft)

    public BillContext(Patient patient) {
        this.patient = patient;
        this.billId = null;
    }

    public BillContext(Patient patient, Integer billId) {
        this.patient = patient;
        this.billId = billId;
    }

    public Patient getPatient() {
        return patient;
    }

    public Integer getBillId() {
        return billId;
    }
}
