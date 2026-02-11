package app.model;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;


public class User {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty counterNo;
    private final SimpleStringProperty username;
    private final SimpleStringProperty password;
    private final SimpleStringProperty role;
    private final SimpleIntegerProperty status;

    public User(int id, String counterNo, String username, String password, String role, int status) {
        this.id = new SimpleIntegerProperty(id);
        this.counterNo = new SimpleStringProperty(counterNo != null ? counterNo : "N/A");
        this.username = new SimpleStringProperty(username);
        this.password = new SimpleStringProperty(password);
        this.role = new SimpleStringProperty(role);
        this.status = new SimpleIntegerProperty(status);
    }

    // Getters for properties
    public int getId() {
        return id.get();
    }
    public String getCounterNo() {
        String val = counterNo.get();
        return val.equals("N/A") ? null : val;
    }
    public String getUsername() {
        return username.get();
    }
    public String getPassword() {
        return password.get();
    }
    public String getRole() {
        return role.get();
    }
    public int getStatus() {
        return status.get();
    }

    // Masked password (for display)
    public String getMaskedPassword() {
        return "••••••••";
    }

    // Status as text
    public String getStatusText() {
        return status.get() == 1 ? "Active" : "Disabled";
    }

    // Property getters (for TableView binding)
    public SimpleIntegerProperty idProperty() { return id; }
    public SimpleStringProperty counterNoProperty() { return counterNo; }
    public SimpleStringProperty usernameProperty() { return username; }
    public SimpleStringProperty roleProperty() { return role; }
}
