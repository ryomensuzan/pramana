package app.model;

public class Patient {
    private int id;
    private String patientCode;
    private String fullName;
    private String gender;
    private int age;
    private String phone;
    private String address;

    // Constructor with ID (for existing patients from database)
    public Patient(int id, String patientCode, String fullName, String gender,
                   int age, String phone, String address) {
        this.id = id;
        this.patientCode = patientCode;
        this.fullName = fullName;
        this.gender = gender;
        this.age = age;
        this.phone = phone;
        this.address = address;
    }

    // Constructor without ID (for new patient registration)
    public Patient(String patientCode, String fullName, String gender,
                   int age, String phone, String address) {
        this.patientCode = patientCode;
        this.fullName = fullName;
        this.gender = gender;
        this.age = age;
        this.phone = phone;
        this.address = address;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPatientCode() {
        return patientCode;
    }

    public void setPatientCode(String patientCode) {
        this.patientCode = patientCode;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    @Override
    public String toString() {
        return "Patient{" +
                "id=" + id +
                ", patientCode='" + patientCode + '\'' +
                ", fullName='" + fullName + '\'' +
                ", gender='" + gender + '\'' +
                ", age=" + age +
                ", phone='" + phone + '\'' +
                ", address='" + address + '\'' +
                '}';
    }
}