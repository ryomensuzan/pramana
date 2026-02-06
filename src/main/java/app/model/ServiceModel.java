package app.model;

public class ServiceModel {

    private int id;
    private String categoryName;
    private String serviceName;
    private double price;
    private String status;

    public ServiceModel(int id, String categoryName,
                        String serviceName, double price, String status) {
        this.id = id;
        this.categoryName = categoryName;
        this.serviceName = serviceName;
        this.price = price;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public double getPrice() {
        return price;
    }

    public String getStatus() {
        return status;
    }
}
