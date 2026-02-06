package app.model;

public class Patient {

    private int id;
    private String patientCode;
    private String fullName;
    private String gender;
    private int age;
    private String phone;
    private String address;

    public Patient(String patientCode, String fullName, String gender,
                   int age, String phone, String address) {
        this.patientCode = patientCode;
        this.fullName = fullName;
        this.gender = gender;
        this.age = age;
        this.phone = phone;
        this.address = address;
    }

    // getters only for now
    public String getPatientCode() { return patientCode; }
    public String getFullName() { return fullName; }
    public String getGender() { return gender; }
    public int getAge() { return age; }
    public String getPhone() { return phone; }
    public String getAddress() { return address; }
}
