package app.model;

public class ServiceCategoryModel {

    private int id;
    private String categoryName;
    private String description;
    private String status;

    public ServiceCategoryModel(int id, String categoryName,
                                String description, String status) {
        this.id = id;
        this.categoryName = categoryName;
        this.description = description;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getDescription() {
        return description;
    }

    public String getStatus() {
        return status;
    }
}

