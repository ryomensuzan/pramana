package app.model;
import javafx.beans.property.*;

public class BillItem {
    private final StringProperty serviceName;
    private final DoubleProperty price;
    private final IntegerProperty quantity;
    private final DoubleProperty total;

    public BillItem(String serviceName, double price, int quantity) {
        this.serviceName = new SimpleStringProperty(serviceName);
        this.price = new SimpleDoubleProperty(price);
        this.quantity = new SimpleIntegerProperty(quantity);
        this.total = new SimpleDoubleProperty(price * quantity);
    }

    // Service Name
    public String getServiceName() {
        return serviceName.get();
    }

    public void setServiceName(String serviceName) {
        this.serviceName.set(serviceName);
    }

    public StringProperty serviceNameProperty() {
        return serviceName;
    }

    // Price
    public double getPrice() {
        return price.get();
    }

    public void setPrice(double price) {
        this.price.set(price);
        updateTotal();
    }

    public DoubleProperty priceProperty() {
        return price;
    }

    // Quantity
    public int getQuantity() {
        return quantity.get();
    }

    public void setQuantity(int quantity) {
        this.quantity.set(quantity);
        updateTotal();
    }

    public IntegerProperty quantityProperty() {
        return quantity;
    }

    // Total
    public double getTotal() {
        return total.get();
    }

    private void updateTotal() {
        this.total.set(price.get() * quantity.get());
    }

    public DoubleProperty totalProperty() {
        return total;
    }

    @Override
    public String toString() {
        return String.format("%s - %.2f x %d = %.2f",
                getServiceName(), getPrice(), getQuantity(), getTotal());
    }
}