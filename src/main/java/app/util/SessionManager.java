package app.util;

/**
 * Singleton class to manage user session data
 */
public class SessionManager {
    private static SessionManager instance;

    private int userId;
    private String username;
    private String role;
    private String counterNo;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /**
     * Set session data for logged-in user
     */
    public static void setSession(int userId, String username, String role, String counterNo) {
        SessionManager session = getInstance();
        session.userId = userId;
        session.username = username;
        session.role = role;
        session.counterNo = counterNo;

        System.out.println("✅ Session set: User=" + username + ", Counter=" + counterNo + ", Role=" + role);
    }

    /**
     * Clear session on logout
     */
    public static void clearSession() {
        SessionManager session = getInstance();
        session.userId = 0;
        session.username = null;
        session.role = null;
        session.counterNo = null;

        System.out.println("✅ Session cleared");
    }

    /**
     * Get logged-in username (for ProfileController compatibility)
     */
    public static String getLoggedInUser() {
        return getInstance().username;
    }

    /**
     * Print session info for debugging
     */
    public static void printSessionInfo() {
        SessionManager session = getInstance();
        System.out.println("=== SESSION INFO ===");
        System.out.println("User ID: " + session.userId);
        System.out.println("Username: " + session.username);
        System.out.println("Role: " + session.role);
        System.out.println("Counter No: " + session.counterNo);
        System.out.println("Is Logged In: " + session.isLoggedIn());
        System.out.println("===================");
    }

    // Instance getters
    public int getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getCounterNo() {
        return counterNo;
    }

    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return username != null && !username.isEmpty();
    }

    /**
     * Check if current user is admin
     */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(role);
    }

    /**
     * Check if current user is staff
     */
    public boolean isStaff() {
        return "STAFF".equalsIgnoreCase(role);
    }
}