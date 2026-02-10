package app.model;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Service {

    private final SimpleIntegerProperty id;
    private final SimpleIntegerProperty categoryId;
    private final SimpleStringProperty categoryName;
    private final SimpleStringProperty serviceName;
    private final SimpleDoubleProperty price;
    private final SimpleStringProperty status;

    // CONSTRUCTOR 1

    public Service(int id, String serviceName, double price) {
        this.id = new SimpleIntegerProperty(id);
        this.categoryId = new SimpleIntegerProperty(0);
        this.categoryName = new SimpleStringProperty("");
        this.serviceName = new SimpleStringProperty(serviceName);
        this.price = new SimpleDoubleProperty(price);
        this.status = new SimpleStringProperty("ACTIVE");
    }

    // CONSTRUCTOR 2

    public Service(int id, int categoryId, String categoryName,
                   String serviceName, double price, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.categoryId = new SimpleIntegerProperty(categoryId);
        this.categoryName = new SimpleStringProperty(categoryName);
        this.serviceName = new SimpleStringProperty(serviceName);
        this.price = new SimpleDoubleProperty(price);
        this.status = new SimpleStringProperty(status);
    }

    // STANDARD GETTERS (old controllers still work!)

    public int getId() {
        return id.get();
    }

    public int getCategoryId() {
        return categoryId.get();
    }

    public String getCategoryName() {
        return categoryName.get();
    }

    public String getServiceName() {
        return serviceName.get();
    }

    public double getPrice() {
        return price.get();
    }

    public String getStatus() {
        return status.get();
    }

    // STANDARD SETTERS

    public void setId(int id) {
        this.id.set(id);
    }

    public void setCategoryId(int categoryId) {
        this.categoryId.set(categoryId);
    }

    public void setCategoryName(String categoryName) {
        this.categoryName.set(categoryName);
    }

    public void setServiceName(String serviceName) {
        this.serviceName.set(serviceName);
    }

    public void setPrice(double price) {
        this.price.set(price);
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    // PROPERTY GETTERS (for TableView binding)
    // ManageServiceController uses these!

    public SimpleIntegerProperty idProperty() {
        return id;
    }

    public SimpleIntegerProperty categoryIdProperty() {
        return categoryId;
    }

    public SimpleStringProperty categoryNameProperty() {
        return categoryName;
    }

    public SimpleStringProperty serviceNameProperty() {
        return serviceName;
    }

    public SimpleDoubleProperty priceProperty() {
        return price;
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    // toString()

    @Override
    public String toString() {
        return serviceName.get() + " - Rs." +
                String.format("%.2f", price.get());
    }
}