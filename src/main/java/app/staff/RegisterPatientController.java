package app.staff;

import app.db.DBConnection;
import app.model.Patient;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.UUID;

public class RegisterPatientController {

    @FXML private TextField nameField;
    @FXML private ComboBox<String> genderBox;
    @FXML private TextField ageField;
    @FXML private TextField phoneField;
    @FXML private TextArea addressArea;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        genderBox.getItems().addAll("Male", "Female", "Other");
    }

    @FXML
    private void handleRegister() {

        String name = nameField.getText();
        String gender = genderBox.getValue();
        String ageText = ageField.getText();
        String phone = phoneField.getText();
        String address = addressArea.getText();

        if (name.isEmpty() || gender == null || ageText.isEmpty()) {
            statusLabel.setText("Required fields missing");
            return;
        }

        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            statusLabel.setText("Invalid age");
            return;
        }

        // Generate readable patient code
        String patientCode = "PAT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();

        Patient patient = new Patient(
                patientCode, name, gender, age, phone, address
        );

        insertPatient(patient);
    }

    private void insertPatient(Patient patient) {

        String sql = """
            INSERT INTO patients
            (patient_code, full_name, gender, age, phone, address)
            VALUES (?, ?, ?, ?, ?, ?)
        """;

        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, patient.getPatientCode());
            ps.setString(2, patient.getFullName());
            ps.setString(3, patient.getGender());
            ps.setInt(4, patient.getAge());
            ps.setString(5, patient.getPhone());
            ps.setString(6, patient.getAddress());

            ps.executeUpdate();

            statusLabel.setText("Patient registered: " + patient.getPatientCode());
            clearForm();

        } catch (Exception e) {
            e.printStackTrace();
            statusLabel.setText("Database error");
        }
    }

    private void clearForm() {
        nameField.clear();
        genderBox.setValue(null);
        ageField.clear();
        phoneField.clear();
        addressArea.clear();
    }
}
