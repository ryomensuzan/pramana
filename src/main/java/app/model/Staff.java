package app.model;

public class Staff {

    private int id;
    private String counterNo;
    private String username;

    public Staff(int id, String counterNo, String username) {
        this.id = id;
        this.counterNo = counterNo;
        this.username = username;
    }

    public int getId() {
        return id;
    }

    public String getCounterNo() {
        return counterNo;
    }

    public String getUsername() {
        return username;
    }
}
