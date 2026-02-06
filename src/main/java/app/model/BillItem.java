package app.model;

public class BillItem {

    private String serviceName;
    private double price;
    private int quantity;

    public BillItem(String serviceName, double price, int quantity) {
        this.serviceName = serviceName;
        this.price = price;
        this.quantity = quantity;
    }

    public String getServiceName() { return serviceName; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }

    public double getTotal() {
        return price * quantity;
    }
}
