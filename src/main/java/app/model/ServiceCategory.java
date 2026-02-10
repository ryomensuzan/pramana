package app.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class ServiceCategory {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty categoryName;
    private final SimpleStringProperty description;
    private final SimpleStringProperty status;

    // CONSTRUCTOR 1
    // Used by existing controllers (ManageCategoryController)

    public ServiceCategory(int id, String categoryName,
                           String description, String status) {
        this.id = new SimpleIntegerProperty(id);
        this.categoryName = new SimpleStringProperty(categoryName);
        this.description = new SimpleStringProperty(
                description != null ? description : "");
        this.status = new SimpleStringProperty(status);
    }

    // CONSTRUCTOR 2 ( for ManageServiceController)
    // Only needs id and categoryName for ComboBox
    // description and status get default values

    public ServiceCategory(int id, String categoryName) {
        this.id = new SimpleIntegerProperty(id);
        this.categoryName = new SimpleStringProperty(categoryName);
        this.description = new SimpleStringProperty("");  // default
        this.status = new SimpleStringProperty("ACTIVE"); // default
    }

    // Standard GETTERS

    public int getId() {
        return id.get();
    }

    public String getCategoryName() {
        return categoryName.get();
    }

    public String getDescription() {
        String desc = description.get();
        return desc.isEmpty() ? null : desc;
    }

    public String getStatus() {
        return status.get();
    }

    //Property GETTERS

    public SimpleIntegerProperty idProperty() {
        return id;
    }

    public SimpleStringProperty categoryNameProperty() {
        return categoryName;
    }

    public SimpleStringProperty descriptionProperty() {
        return description;
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    // toString() ManageServiceController ComboBox needs this!

    @Override
    public String toString() {
        return categoryName.get();
    }
}