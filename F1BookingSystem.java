import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// =================================================================================
// 1. Main Application Runner
// =================================================================================
public class F1BookingSystem {
    public static void main(String[] args) {
        DataManager.initializeDatabase();
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.out.println("System L&F not found.");
            }
            new AuthFrame().setVisible(true);
        });
    }
}

// =================================================================================
// 2. Data Models
// =================================================================================
class User {
    private String name, email, password;
    private double walletBalanceUSD;
    public User(String name, String email, String password, double initialWallet) {
        this.name = name; this.email = email; this.password = password; this.walletBalanceUSD = initialWallet;
    }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public double getWalletBalanceUSD() { return walletBalanceUSD; }
    public void setWalletBalanceUSD(double balance) { this.walletBalanceUSD = balance; }
    public String getPassword() { return password; }
}

class Ticket {
    private String ticketId, userEmail, grandPrixName, seatingAreaName, raceDate;
    private int ticketCount;
    private double totalPriceUSD;
    private Date bookingDate;
    public Ticket(String id, String email, String gpName, String areaName, int count, double price, Date date, String raceDate) {
        this.ticketId = id; this.userEmail = email; this.grandPrixName = gpName; this.seatingAreaName = areaName;
        this.ticketCount = count; this.totalPriceUSD = price; this.bookingDate = date; this.raceDate = raceDate;
    }
    public String getGrandPrixName() { return grandPrixName; }
    public String getSeatingAreaName() { return seatingAreaName; }
    public int getTicketCount() { return ticketCount; }
    public double getTotalPriceUSD() { return totalPriceUSD; }
    public Date getBookingDate() { return bookingDate; }
    public String getTicketId() { return ticketId; }
    public String getRaceDate() { return raceDate; }
    @Override public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        return String.format("<html><b>%s</b><br>%d x %s<br>Booked on: %s - Price: %s</html>",
                grandPrixName, ticketCount, seatingAreaName, sdf.format(bookingDate),
                NumberFormat.getCurrencyInstance(Locale.US).format(totalPriceUSD));
    }
}

class SeatingArea {
    private String uniqueId, gpName, name;
    private double priceINR;
    private int capacity, soldTickets;
    public SeatingArea(String id, String gp, String n, double p, int cap, int sold) {
        this.uniqueId = id; this.gpName = gp; this.name = n; this.priceINR = p; this.capacity = cap; this.soldTickets = sold;
    }
    public String getUniqueId() { return uniqueId; }
    public String getGpName() { return gpName; }
    public String getName() { return name; }
    public double getPriceINR() { return priceINR; }
    public int getTicketsLeft() { return capacity - soldTickets; }
    public boolean isSoldOut() { return getTicketsLeft() <= 0; }
    @Override public String toString() {
        double priceUSD = priceINR * 0.012; // Using the project's conversion rate
        if (isSoldOut()) return String.format("%s - (SOLD OUT)", name);
        return String.format("%s - %s (%d left)", name, NumberFormat.getCurrencyInstance(Locale.US).format(priceUSD), getTicketsLeft());
    }
}

class GrandPrix {
    private String name, country, imagePath, date;
    public GrandPrix(String n, String c, String path, String date) { 
        this.name = n; this.country = c; this.imagePath = path; this.date = date; 
    }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getImagePath() { return imagePath; }
    public String getDate() { return date; }
    @Override public String toString() { return name; }
}

// =================================================================================
// 3. Data Manager (for SQLite Database)
// =================================================================================
class DataManager {
    private static final String DB_URL = "jdbc:sqlite:f1_booking.db";

    public static Connection connect() {
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(DB_URL);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return conn;
    }

    public static void initializeDatabase() {
        String createUserTable = "CREATE TABLE IF NOT EXISTS users (email TEXT PRIMARY KEY, name TEXT NOT NULL, password TEXT NOT NULL, wallet_balance REAL NOT NULL);";
        String createSeatingAreaTable = "CREATE TABLE IF NOT EXISTS seating_areas (unique_id TEXT PRIMARY KEY, gp_name TEXT NOT NULL, area_name TEXT NOT NULL, price_inr REAL NOT NULL, capacity INTEGER NOT NULL, sold_tickets INTEGER NOT NULL);";
        String createTicketsTable = "CREATE TABLE IF NOT EXISTS tickets (ticket_id TEXT PRIMARY KEY, user_email TEXT NOT NULL, gp_name TEXT NOT NULL, seating_area TEXT NOT NULL, race_date TEXT NOT NULL, ticket_count INTEGER NOT NULL, total_price_usd REAL NOT NULL, booking_date INTEGER NOT NULL, FOREIGN KEY (user_email) REFERENCES users (email));";

        try (Connection conn = connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(createUserTable);
            stmt.execute(createSeatingAreaTable);
            stmt.execute(createTicketsTable);
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM seating_areas");
            if (rs.getInt(1) == 0) {
                System.out.println("Database empty. Populating initial data...");
                populateInitialData(conn);
                System.out.println("Data populated.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void populateInitialData(Connection conn) throws SQLException {
        // --- COMPREHENSIVELY UPDATED WITH ALL STANDS ---
        addSeatingArea(conn, "Abu Dhabi Grand Prix", "Main Grandstand", 350000, 5000);
        addSeatingArea(conn, "Abu Dhabi Grand Prix", "North Straight", 180000, 3000);
        addSeatingArea(conn, "Abu Dhabi Grand Prix", "North Grandstand", 185000, 3500);
        addSeatingArea(conn, "Abu Dhabi Grand Prix", "West Straight", 195000, 2500);
        addSeatingArea(conn, "Abu Dhabi Grand Prix", "West Grandstand", 205000, 4000);
        addSeatingArea(conn, "Abu Dhabi Grand Prix", "Marina Grandstand", 250000, 3000);
        addSeatingArea(conn, "Abu Dhabi Grand Prix", "South Grandstand", 210000, 4500);
        addSeatingArea(conn, "Abu Dhabi Grand Prix", "General Admission", 80000, 10000);

        addSeatingArea(conn, "Australian Grand Prix", "Stewart Grandstand", 115000, 1500);
        addSeatingArea(conn, "Australian Grand Prix", "Hill Grandstand", 85000, 2000);
        addSeatingArea(conn, "Australian Grand Prix", "Ricciardo Grandstand", 95000, 1800);
        addSeatingArea(conn, "Australian Grand Prix", "Jones Grandstand", 90000, 1200);
        addSeatingArea(conn, "Australian Grand Prix", "Moss Grandstand", 90000, 1200);
        addSeatingArea(conn, "Australian Grand Prix", "Fangio Grandstand", 120000, 2500);
        addSeatingArea(conn, "Australian Grand Prix", "Senna Grandstand", 110000, 1000);
        addSeatingArea(conn, "Australian Grand Prix", "Prost Grandstand", 112000, 1000);
        addSeatingArea(conn, "Australian Grand Prix", "Lauda Grandstand", 98000, 1300);
        addSeatingArea(conn, "Australian Grand Prix", "Schumacher Grandstand", 92000, 1400);
        addSeatingArea(conn, "Australian Grand Prix", "Webber Grandstand", 88000, 1600);
        addSeatingArea(conn, "Australian Grand Prix", "Vettel Grandstand", 88000, 1600);
        addSeatingArea(conn, "Australian Grand Prix", "Waite Grandstand", 82000, 1700);
        addSeatingArea(conn, "Australian Grand Prix", "Clark Grandstand", 83000, 1700);
        addSeatingArea(conn, "Australian Grand Prix", "Button Grandstand", 84000, 1700);
        
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Absheron (Main)", 280000, 4000);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Champions Club", 250000, 500);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Zafar Grandstand", 160000, 1000);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Khazar Grandstand", 155000, 1200);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Icheri Sheher", 150000, 800);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Sahil Grandstand", 145000, 1500);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Bulvar Grandstand", 130000, 1500);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Mugham Grandstand", 125000, 1000);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Giz Galasi", 120000, 1000);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Marine Grandstand", 115000, 1000);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Azneft Grandstand", 110000, 1000);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "Philarmoniya", 100000, 900);
        addSeatingArea(conn, "Azerbaijan Grand Prix", "General Admission", 60000, 8000);
        
        addSeatingArea(conn, "Dutch Grand Prix", "Pit Grandstand", 450000, 3000);
        addSeatingArea(conn, "Dutch Grand Prix", "Paddock Club", 1200000, 200);
        addSeatingArea(conn, "Dutch Grand Prix", "Hairpin Grandstand 1 & 2", 210000, 4000);
        addSeatingArea(conn, "Dutch Grand Prix", "Arena Grandstand 1", 250000, 5000);
        addSeatingArea(conn, "Dutch Grand Prix", "Champions Club", 950000, 400);

        addSeatingArea(conn, "Italian Grand Prix", "Main Straight (1)", 420000, 3000);
        addSeatingArea(conn, "Italian Grand Prix", "Laterale Destra (4)", 380000, 2500);
        addSeatingArea(conn, "Italian Grand Prix", "Piscina (5)", 210000, 1500);
        addSeatingArea(conn, "Italian Grand Prix", "Alta Velocita (6a-c)", 250000, 2000);
        addSeatingArea(conn, "Italian Grand Prix", "Prima Variante (8a-b)", 220000, 2200);
        addSeatingArea(conn, "Italian Grand Prix", "Seconda Variante (9-10)", 195000, 2800);
        addSeatingArea(conn, "Italian Grand Prix", "Variante Ascari (16-19)", 175000, 3000);
        addSeatingArea(conn, "Italian Grand Prix", "Parabolica (22-23b)", 190000, 3500);
        addSeatingArea(conn, "Italian Grand Prix", "General Admission", 90000, 15000);

        addSeatingArea(conn, "Las Vegas Grand Prix", "Heineken Silver (Main)", 800000, 6000);
        addSeatingArea(conn, "Las Vegas Grand Prix", "Sphere Zone (SG1-8)", 650000, 8000);
        addSeatingArea(conn, "Las Vegas Grand Prix", "T-Mobile Zone", 600000, 7000);
        addSeatingArea(conn, "Las Vegas Grand Prix", "West Harmon Zone", 500000, 4000);
        addSeatingArea(conn, "Las Vegas Grand Prix", "Caesar's Palace Experience", 950000, 1000);
        addSeatingArea(conn, "Las Vegas Grand Prix", "Flamingo General Admission", 250000, 5000);
        
        addSeatingArea(conn, "Qatar Grand Prix", "Main Grandstand", 300000, 6000);
        addSeatingArea(conn, "Qatar Grand Prix", "North Grandstand", 220000, 4000);
        addSeatingArea(conn, "Qatar Grand Prix", "T2 Grandstand", 190000, 2000);
        addSeatingArea(conn, "Qatar Grand Prix", "T3 Grandstand", 195000, 2000);
        addSeatingArea(conn, "Qatar Grand Prix", "T16 Grandstand", 180000, 2500);
        addSeatingArea(conn, "Qatar Grand Prix", "General Admission", 95000, 12000);
        
        addSeatingArea(conn, "British Grand Prix", "Hamilton Straight A/B", 550000, 7000);
        addSeatingArea(conn, "British Grand Prix", "Abbey A/B", 380000, 4000);
        addSeatingArea(conn, "British Grand Prix", "Farm Curve", 370000, 3000);
        addSeatingArea(conn, "British Grand Prix", "Village A/B", 360000, 4500);
        addSeatingArea(conn, "British Grand Prix", "The Loop", 355000, 2500);
        addSeatingArea(conn, "British Grand Prix", "Aintree", 350000, 2500);
        addSeatingArea(conn, "British Grand Prix", "Wellington Straight", 340000, 3000);
        addSeatingArea(conn, "British Grand Prix", "Luffield", 325000, 3200);
        addSeatingArea(conn, "British Grand Prix", "Woodcote A/B", 320000, 3500);
        addSeatingArea(conn, "British Grand Prix", "National Pits Straight", 480000, 2000);
        addSeatingArea(conn, "British Grand Prix", "Copse A/B/C", 310000, 4000);
        addSeatingArea(conn, "British Grand Prix", "Becketts", 350000, 3800);
        addSeatingArea(conn, "British Grand Prix", "Stowe A/B/C", 290000, 5000);
        addSeatingArea(conn, "British Grand Prix", "Vale / Club", 420000, 4200);
        addSeatingArea(conn, "British Grand Prix", "General Admission", 150000, 20000);
        
        addSeatingArea(conn, "Singapore Grand Prix", "Super Pit Grandstand", 650000, 2000);
        addSeatingArea(conn, "Singapore Grand Prix", "Pit Grandstand", 450000, 4000);
        addSeatingArea(conn, "Singapore Grand Prix", "Turn 1 Grandstand", 250000, 3000);
        addSeatingArea(conn, "Singapore Grand Prix", "Turn 2 Grandstand", 240000, 3000);
        addSeatingArea(conn, "Singapore Grand Prix", "Republic Grandstand", 220000, 2500);
        addSeatingArea(conn, "Singapore Grand Prix", "Raffles Grandstand", 210000, 2500);
        addSeatingArea(conn, "Singapore Grand Prix", "Bayfront Grandstand", 190000, 3500);
        addSeatingArea(conn, "Singapore Grand Prix", "Padang Grandstand", 180000, 4000);
        addSeatingArea(conn, "Singapore Grand Prix", "Connaught Grandstand", 160000, 3200);
        addSeatingArea(conn, "Singapore Grand Prix", "Orange @ Empress", 150000, 2800);
        addSeatingArea(conn, "Singapore Grand Prix", "Promenade Grandstand", 170000, 2000);
        
        addSeatingArea(conn, "United States Grand Prix", "Main Grandstand", 480000, 6000);
        addSeatingArea(conn, "United States Grand Prix", "Turn 1 Grandstand", 350000, 4000);
        addSeatingArea(conn, "United States Grand Prix", "Turn 4 Grandstand", 290000, 3500);
        addSeatingArea(conn, "United States Grand Prix", "Turn 9 Grandstand", 285000, 3000);
        addSeatingArea(conn, "United States Grand Prix", "Turn 12 Grandstand", 280000, 3200);
        addSeatingArea(conn, "United States Grand Prix", "Turn 13 Grandstand", 270000, 2000);
        addSeatingArea(conn, "United States Grand Prix", "Turn 15 Grandstand", 320000, 4500);
        addSeatingArea(conn, "United States Grand Prix", "Turn 19 Grandstand", 310000, 3800);
        addSeatingArea(conn, "United States Grand Prix", "Turn 19B Grandstand", 300000, 1500);
        addSeatingArea(conn, "United States Grand Prix", "General Admission", 160000, 18000);
    }
    
    private static void addSeatingArea(Connection conn, String gpName, String areaName, double price, int capacity) throws SQLException {
        String sql = "INSERT INTO seating_areas(unique_id, gp_name, area_name, price_inr, capacity, sold_tickets) VALUES(?,?,?,?,?,?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, gpName + "|" + areaName);
            pstmt.setString(2, gpName);
            pstmt.setString(3, areaName);
            pstmt.setDouble(4, price);
            pstmt.setInt(5, capacity);
            pstmt.setInt(6, 0);
            pstmt.executeUpdate();
        }
    }

    public static User authenticateUser(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return new User(rs.getString("name"), rs.getString("email"), rs.getString("password"), rs.getDouble("wallet_balance"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean registerUser(String name, String email, String password) {
        String sql = "INSERT INTO users(name, email, password, wallet_balance) VALUES(?,?,?,?)";
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setDouble(4, 1000000.00);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    public static List<GrandPrix> getAllGrandPrix() {
        List<GrandPrix> gpList = new ArrayList<>();
        gpList.add(new GrandPrix("Abu Dhabi Grand Prix", "UAE", "tracks/abu dhabi track.jpg", "Dec 06-08"));
        gpList.add(new GrandPrix("Australian Grand Prix", "Australia", "tracks/australia track.jpg", "Mar 21-23"));
        gpList.add(new GrandPrix("Azerbaijan Grand Prix", "Azerbaijan", "tracks/azerbaijan track.jpg", "Sep 13-15"));
        gpList.add(new GrandPrix("Dutch Grand Prix", "Netherlands", "tracks/dutch track.jpg", "Aug 29-31"));
        gpList.add(new GrandPrix("Italian Grand Prix", "Italy", "tracks/italy track.jpg", "Sep 05-07"));
        gpList.add(new GrandPrix("Las Vegas Grand Prix", "USA", "tracks/las vegas track.jpg", "Nov 20-22"));
        gpList.add(new GrandPrix("Qatar Grand Prix", "Qatar", "tracks/qatar track.jpg", "Nov 28-30"));
        gpList.add(new GrandPrix("British Grand Prix", "UK", "tracks/silverstone track.jpg", "Jul 04-06"));
        gpList.add(new GrandPrix("Singapore Grand Prix", "Singapore", "tracks/singapore track.jpg", "Oct 03-05"));
        gpList.add(new GrandPrix("United States Grand Prix", "USA", "tracks/us track.jpg", "Oct 17-19"));
        return gpList;
    }

    public static List<SeatingArea> getSeatingAreasForGP(String gpName) {
        String sql = "SELECT * FROM seating_areas WHERE gp_name = ?";
        List<SeatingArea> areas = new ArrayList<>();
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, gpName);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                areas.add(new SeatingArea(rs.getString("unique_id"), rs.getString("gp_name"), rs.getString("area_name"), rs.getDouble("price_inr"), rs.getInt("capacity"), rs.getInt("sold_tickets")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return areas;
    }

    public static List<Ticket> getTicketsForUser(String email) {
        String sql = "SELECT * FROM tickets WHERE user_email = ?";
        List<Ticket> tickets = new ArrayList<>();
        try (Connection conn = connect(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                tickets.add(new Ticket(rs.getString("ticket_id"), rs.getString("user_email"), rs.getString("gp_name"), rs.getString("seating_area"), rs.getInt("ticket_count"), rs.getDouble("total_price_usd"), new Date(rs.getLong("booking_date")), rs.getString("race_date")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tickets;
    }

    public static boolean bookTicket(User user, SeatingArea area, int count, double totalUsd, String raceDate) {
        String insertTicketSQL = "INSERT INTO tickets(ticket_id, user_email, gp_name, seating_area, ticket_count, total_price_usd, booking_date, race_date) VALUES(?,?,?,?,?,?,?,?)";
        String updateUserSQL = "UPDATE users SET wallet_balance = ? WHERE email = ?";
        String updateAreaSQL = "UPDATE seating_areas SET sold_tickets = sold_tickets + ? WHERE unique_id = ?";
        Connection conn = null;
        try {
            conn = connect();
            conn.setAutoCommit(false);
            try (PreparedStatement pstmt = conn.prepareStatement(insertTicketSQL)) {
                pstmt.setString(1, "F1TKT-" + System.currentTimeMillis());
                pstmt.setString(2, user.getEmail());
                pstmt.setString(3, area.getGpName());
                pstmt.setString(4, area.getName());
                pstmt.setInt(5, count);
                pstmt.setDouble(6, totalUsd);
                pstmt.setLong(7, new Date().getTime());
                pstmt.setString(8, raceDate);
                pstmt.executeUpdate();
            }
            try (PreparedStatement pstmt = conn.prepareStatement(updateUserSQL)) {
                double newBalance = user.getWalletBalanceUSD() - totalUsd;
                pstmt.setDouble(1, newBalance);
                pstmt.setString(2, user.getEmail());
                pstmt.executeUpdate();
                user.setWalletBalanceUSD(newBalance);
            }
            try (PreparedStatement pstmt = conn.prepareStatement(updateAreaSQL)) {
                pstmt.setInt(1, count);
                pstmt.setString(2, area.getUniqueId());
                pstmt.executeUpdate();
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) { try { conn.rollback(); } catch (SQLException ex) { ex.printStackTrace(); } }
            e.printStackTrace();
            return false;
        } finally {
            if (conn != null) { try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ex) { ex.printStackTrace(); } }
        }
    }
}

// =================================================================================
// 4. GUI Frames
// =================================================================================
class AuthFrame extends JFrame {
    private CardLayout cardLayout = new CardLayout();
    private JPanel mainPanel = new JPanel(cardLayout);
    private JTextField loginEmailField, suNameField, suEmailField;
    private JPasswordField loginPasswordField, suPasswordField;
    private final Color F1_RED = new Color(225, 6, 0);
    private final Color DARK_BG = new Color(20, 20, 20);
    private final Color TEXT_COLOR = Color.WHITE;
    private final Font LABEL_FONT = new Font("SansSerif", Font.BOLD, 14);

    public AuthFrame() {
        setTitle("F1 Booking System - Welcome");
        setSize(450, 550);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        mainPanel.add(createLoginPanel(), "LOGIN");
        mainPanel.add(createSignupPanel(), "SIGNUP");
        add(mainPanel);
    }

    private JPanel createLoginPanel() {
        JPanel panel = createBasePanel("Login");
        panel.add(createLabel("Email Address:"));
        loginEmailField = createTextField();
        addPlaceholder(loginEmailField, "your.email@example.com");
        panel.add(loginEmailField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(createLabel("Password:"));
        loginPasswordField = createPasswordField();
        addPlaceholder(loginPasswordField, "Password");
        panel.add(loginPasswordField);
        panel.add(Box.createVerticalStrut(30));
        JButton loginButton = createButton("LOGIN", e -> performLogin());
        panel.add(loginButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(createLink("Don't have an account? Sign Up", "SIGNUP"));
        return panel;
    }
    
    private JPanel createSignupPanel() {
        JPanel panel = createBasePanel("Signup");
        panel.add(createLabel("Full Name:"));
        suNameField = createTextField();
        addPlaceholder(suNameField, "Your Name");
        panel.add(suNameField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(createLabel("Email Address:"));
        suEmailField = createTextField();
        addPlaceholder(suEmailField, "your.email@example.com");
        panel.add(suEmailField);
        panel.add(Box.createVerticalStrut(15));
        panel.add(createLabel("Password:"));
        suPasswordField = createPasswordField();
        addPlaceholder(suPasswordField, "Password");
        panel.add(suPasswordField);
        panel.add(Box.createVerticalStrut(30));
        JButton signupButton = createButton("SIGN UP", e -> performSignup());
        panel.add(signupButton);
        panel.add(Box.createVerticalStrut(15));
        panel.add(createLink("Already have an account? Login", "LOGIN"));
        return panel;
    }

    private JPanel createBasePanel(String subTitle) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(DARK_BG);
        panel.setBorder(new EmptyBorder(40, 40, 40, 40));
        JLabel header = new JLabel("F1 2025 Tickets", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 32));
        header.setForeground(Color.WHITE);
        header.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(header);
        JLabel subHeader = new JLabel(subTitle, SwingConstants.CENTER);
        subHeader.setFont(new Font("Arial", Font.PLAIN, 18));
        subHeader.setForeground(Color.LIGHT_GRAY);
        subHeader.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(subHeader);
        panel.add(Box.createVerticalStrut(30));
        return panel;
    }

    private void performLogin() { 
        String email = loginEmailField.getText();
        String password = new String(loginPasswordField.getPassword());
        if (email.equals("your.email@example.com") || password.equals("Password")) {
              JOptionPane.showMessageDialog(this, "Please enter your credentials.", "Login Error", JOptionPane.ERROR_MESSAGE);
              return;
        }
        User user = DataManager.authenticateUser(email, password);
        if (user != null) {
            dispose();
            new CalendarFrame(user).setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Invalid credentials.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void performSignup() {
        String name = suNameField.getText();
        String email = suEmailField.getText();
        String password = new String(suPasswordField.getPassword());
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || 
            name.equals("Your Name") || email.equals("your.email@example.com") || password.equals("Password")) {
              JOptionPane.showMessageDialog(this, "All fields are required.", "Signup Error", JOptionPane.ERROR_MESSAGE);
              return;
        }
        if (DataManager.registerUser(name, email, password)) {
            JOptionPane.showMessageDialog(this, "Registration successful! Please login.", "Success", JOptionPane.INFORMATION_MESSAGE);
            cardLayout.show(mainPanel, "LOGIN");
        } else {
            JOptionPane.showMessageDialog(this, "This email is already registered.", "Signup Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private JLabel createLabel(String text) { 
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_COLOR);
        l.setFont(LABEL_FONT);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }
    private JTextField createTextField() { 
        JTextField f = new JTextField(20);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }
    private JPasswordField createPasswordField() { 
        JPasswordField f = new JPasswordField(20);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        return f;
    }
    private JButton createButton(String text, java.awt.event.ActionListener listener) { 
        JButton b = new JButton(text);
        b.setBackground(F1_RED);
        b.setForeground(Color.BLACK);
        b.setFont(new Font("SansSerif", Font.BOLD, 16));
        b.setFocusPainted(false);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(listener);
        return b;
    }
    private JLabel createLink(String text, String cardName) { 
        JLabel l = new JLabel("<html><a href='' style='color: #AAAAAA;'>"+text+"</a></html>");
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.setAlignmentX(Component.CENTER_ALIGNMENT);
        l.addMouseListener(new MouseAdapter(){
            public void mouseClicked(MouseEvent e){
                cardLayout.show(mainPanel,cardName);
            }
        });
        return l;
    }

    public static void addPlaceholder(JTextField field, String placeholder) {
        field.setText(placeholder);
        field.setForeground(Color.GRAY);
        if (field instanceof JPasswordField) { ((JPasswordField) field).setEchoChar((char) 0); }
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                    if (field instanceof JPasswordField) { ((JPasswordField) field).setEchoChar('*'); }
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                    if (field instanceof JPasswordField) { ((JPasswordField) field).setEchoChar((char) 0); }
                }
            }
        });
    }
}

class CalendarFrame extends JFrame {
    private User currentUser;
    public CalendarFrame(User user) {
        this.currentUser = user;
        setTitle("F1 2025 Season Calendar");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        JLabel header = new JLabel("Select a Grand Prix", SwingConstants.CENTER);
        header.setFont(new Font("Arial", Font.BOLD, 32));
        mainPanel.add(header, BorderLayout.NORTH);
        
        JPanel calendarGrid = new JPanel();
        calendarGrid.setLayout(new BoxLayout(calendarGrid, BoxLayout.Y_AXIS));

        List<GrandPrix> allGPs = DataManager.getAllGrandPrix();
        for (GrandPrix gp : allGPs) {
            JButton gpButton = new JButton();
            gpButton.setLayout(new BorderLayout(10,0));
            gpButton.setFocusPainted(false);
            gpButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            gpButton.setPreferredSize(new Dimension(100, 70));
            gpButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));
            gpButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
            ));

            JPanel textPanel = new JPanel(new GridLayout(2,1));
            textPanel.setOpaque(false);
            JLabel nameLabel = new JLabel(gp.getName());
            nameLabel.setFont(new Font("Arial", Font.BOLD, 18));
            JLabel infoLabel = new JLabel(gp.getCountry() + "  |  " + gp.getDate());
            infoLabel.setForeground(Color.DARK_GRAY);
            infoLabel.setFont(new Font("Arial", Font.PLAIN, 14));
            textPanel.add(nameLabel);
            textPanel.add(infoLabel);
            gpButton.add(textPanel, BorderLayout.CENTER);

            JLabel availableLabel = new JLabel(" SEATS AVAILABLE ");
            availableLabel.setOpaque(true);
            availableLabel.setBackground(new Color(46, 204, 113));
            availableLabel.setForeground(Color.WHITE);
            availableLabel.setFont(new Font("Arial", Font.BOLD, 12));
            gpButton.add(availableLabel, BorderLayout.EAST);
            
            gpButton.addActionListener(e -> {
                new BookingFrame(currentUser, gp).setVisible(true);
            });
            calendarGrid.add(gpButton);
            calendarGrid.add(Box.createVerticalStrut(10));
        }
        
        JScrollPane scrollPane = new JScrollPane(calendarGrid);
        scrollPane.setBorder(null);
        mainPanel.add(scrollPane, BorderLayout.CENTER);
        add(mainPanel);
    }
}

class BookingFrame extends JFrame {
    private User currentUser;
    private GrandPrix currentGP;
    private final double INR_TO_USD_RATE = 0.012;
    private JLabel walletLabel, trackImageLabel, priceLabel;
    private JComboBox<SeatingArea> areaSelector;
    private JSpinner ticketSpinner;
    private JTabbedPane tabbedPane;
    private DefaultListModel<Ticket> ticketListModel;
    private JList<Ticket> ticketList;

    public BookingFrame(User user, GrandPrix gp) {
        this.currentUser = user;
        this.currentGP = gp;
        setTitle("Booking: " + currentGP.getName());
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        createHeaderPanel();
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabbedPane.addTab("  Book Tickets  ", createBookingPanel());
        tabbedPane.addTab("  My Bookings  ", createMyBookingsPanel());
        add(tabbedPane, BorderLayout.CENTER);
        updateUI();
        updateMyBookingsTab();
    }

    private void createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(20, 0));
        headerPanel.setBackground(new Color(30, 30, 30));
        headerPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        JLabel welcomeLabel = new JLabel("Welcome, " + currentUser.getName() + "!");
        welcomeLabel.setForeground(Color.WHITE);
        welcomeLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        headerPanel.add(welcomeLabel, BorderLayout.WEST);
        walletLabel = new JLabel();
        updateWalletLabel();
        walletLabel.setForeground(new Color(10, 200, 10));
        walletLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        headerPanel.add(walletLabel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);
    }

    private JComponent createBookingPanel() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.65);
        splitPane.setBorder(null);
        splitPane.setDividerSize(5);
        trackImageLabel = new JLabel();
        trackImageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        trackImageLabel.setBackground(Color.DARK_GRAY);
        trackImageLabel.setOpaque(true);
        JScrollPane imageScrollPane = new JScrollPane(trackImageLabel);
        imageScrollPane.setBorder(null);
        splitPane.setLeftComponent(imageScrollPane);
        JPanel controlPanel = new JPanel(new GridBagLayout());
        controlPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.gridx = 0; gbc.gridwidth = 2;
        
        gbc.gridy = 0; controlPanel.add(createStyledLabel("1. Select Seating Area"), gbc);
        gbc.gridy = 1;
        areaSelector = new JComboBox<>();
        areaSelector.setFont(new Font("SansSerif", Font.PLAIN, 18));
        areaSelector.setRenderer(new SeatingAreaRenderer());
        areaSelector.addActionListener(e -> updatePrice());
        controlPanel.add(areaSelector, gbc);
        
        gbc.gridy = 2; controlPanel.add(Box.createVerticalStrut(20), gbc);
        
        gbc.gridy = 3; controlPanel.add(createStyledLabel("2. Number of Tickets"), gbc);
        gbc.gridy = 4;
        ticketSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        ticketSpinner.setFont(new Font("SansSerif", Font.PLAIN, 18));
        ticketSpinner.addChangeListener(e -> updatePrice());
        controlPanel.add(ticketSpinner, gbc);

        gbc.gridy = 5; controlPanel.add(Box.createVerticalStrut(40), gbc);

        gbc.gridy = 6; gbc.gridwidth = 1;
        priceLabel = createStyledLabel("Total: $0.00");
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 22));
        controlPanel.add(priceLabel, gbc);
        
        gbc.gridx = 1;
        JButton bookButton = new JButton("Book Now");
        bookButton.setBackground(new Color(225, 6, 0));
        bookButton.setForeground(Color.BLACK);
        bookButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        bookButton.setPreferredSize(new Dimension(150, 50));
        bookButton.addActionListener(e -> processBooking());
        controlPanel.add(bookButton, gbc);

        splitPane.setRightComponent(controlPanel);
        return splitPane;
    }

    private JComponent createMyBookingsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        ticketListModel = new DefaultListModel<>();
        ticketList = new JList<>(ticketListModel);
        ticketList.setCellRenderer(new TicketListRenderer());
        panel.add(new JScrollPane(ticketList), BorderLayout.CENTER);
        JButton viewTicketButton = new JButton("View Selected E-Ticket");
        viewTicketButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        viewTicketButton.addActionListener(e -> viewTicket());
        panel.add(viewTicketButton, BorderLayout.SOUTH);
        return panel;
    }

    private void updateUI() {
        try {
            BufferedImage img = ImageIO.read(new File(currentGP.getImagePath()));
            Image scaledImg = img.getScaledInstance(800, -1, Image.SCALE_SMOOTH);
            trackImageLabel.setIcon(new ImageIcon(scaledImg));
        } catch (IOException e) {
            trackImageLabel.setIcon(null);
            trackImageLabel.setText("Image not found: " + currentGP.getImagePath());
        }
        areaSelector.removeAllItems();
        List<SeatingArea> areas = DataManager.getSeatingAreasForGP(currentGP.getName());
        for (SeatingArea area : areas) {
            areaSelector.addItem(area);
        }
        updatePrice();
    }

    private void updateMyBookingsTab() {
        ticketListModel.clear();
        List<Ticket> tickets = DataManager.getTicketsForUser(currentUser.getEmail());
        for (Ticket t : tickets) {
            ticketListModel.addElement(t);
        }
    }

    private void updatePrice() {
        SeatingArea selectedArea = (SeatingArea) areaSelector.getSelectedItem();
        int ticketCount = (int) ticketSpinner.getValue();
        if (selectedArea == null) {
            priceLabel.setText("Total Price: $0.00");
            return;
        }
        double totalInr = selectedArea.getPriceINR() * ticketCount;
        double totalUsd = totalInr * INR_TO_USD_RATE;
        priceLabel.setText("Total: " + NumberFormat.getCurrencyInstance(Locale.US).format(totalUsd));
    }

    private void updateWalletLabel() {
        walletLabel.setText("Wallet: " + NumberFormat.getCurrencyInstance(Locale.US).format(currentUser.getWalletBalanceUSD()));
    }
    
    private void viewTicket() {
        Ticket selected = ticketList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a ticket from the list first.", "No Ticket Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        // To get the GrandPrix object for the selected ticket
        GrandPrix selectedGP = null;
        for (GrandPrix gp : DataManager.getAllGrandPrix()) {
            if (gp.getName().equals(selected.getGrandPrixName())) {
                selectedGP = gp;
                break;
            }
        }
        new TicketFrame(currentUser, selected, selectedGP).setVisible(true);
    }

    private void processBooking() {
        SeatingArea area = (SeatingArea) areaSelector.getSelectedItem();
        int count = (int) ticketSpinner.getValue();
        if (area == null || area.isSoldOut()) {
            JOptionPane.showMessageDialog(this, "This seating area is sold out.", "Booking Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (count > area.getTicketsLeft()) {
            JOptionPane.showMessageDialog(this, "Not enough tickets available. Only " + area.getTicketsLeft() + " left.", "Booking Error", JOptionPane.WARNING_MESSAGE);
            return;
        }
        double totalUsd = area.getPriceINR() * count * INR_TO_USD_RATE;
        if (currentUser.getWalletBalanceUSD() < totalUsd) {
            JOptionPane.showMessageDialog(this, "Insufficient funds.", "Payment Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Confirm booking?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            if (DataManager.bookTicket(currentUser, area, count, totalUsd, currentGP.getDate())) {
                updateWalletLabel();
                updateMyBookingsTab();
                updateUI();
                JOptionPane.showMessageDialog(this, "Booking successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
                tabbedPane.setSelectedIndex(1);
            } else {
                JOptionPane.showMessageDialog(this, "Booking failed due to a database error.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 18));
        return label;
    }
}

// =================================================================================
// 5. Other GUI Classes
// =================================================================================
class TicketFrame extends JFrame {
    private final JPanel ticketContentPanel; // Panel that holds the ticket content
    private final Ticket ticket;
    public TicketFrame(User user, Ticket ticket, GrandPrix gp) {
        this.ticket = ticket;
        setTitle("Your E-Ticket: " + ticket.getGrandPrixName());
        setSize(500, 750);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JPanel mainPanel = new JPanel(new BorderLayout()); 
        
        ticketContentPanel = new JPanel(new BorderLayout());
        ticketContentPanel.setBackground(Color.WHITE);
        
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(20, 20, 40));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel headerLabel = new JLabel("TICKETS");
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        ticketContentPanel.add(headerPanel, BorderLayout.NORTH);
        
        JPanel detailsPanel = createTicketDetailsPanel(user, ticket, gp);
        ticketContentPanel.add(detailsPanel, BorderLayout.CENTER);
        
        JPanel footerPanel = createFooterPanel();
        ticketContentPanel.add(footerPanel, BorderLayout.SOUTH);
        
        mainPanel.add(ticketContentPanel, BorderLayout.CENTER);

        JButton saveButton = new JButton("Save as Picture");
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        saveButton.addActionListener(e -> saveTicketAsImage());
        
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBorder(new EmptyBorder(10,10,10,10));
        buttonPanel.add(saveButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }

    private JPanel createTicketDetailsPanel(User user, Ticket ticket, GrandPrix gp) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);

        // Grid for details
        JPanel infoGrid = new JPanel(new GridLayout(4, 2, 10, 5));
        infoGrid.setOpaque(false);
        infoGrid.add(createDetailRow("Purchaser:", user.getName()));
        infoGrid.add(createDetailRow("Event:", ticket.getGrandPrixName()));
        infoGrid.add(createDetailRow("Venue:", gp.getCountry()));
        infoGrid.add(createDetailRow("Date:", ticket.getRaceDate()));
        infoGrid.add(createDetailRow("Seat:", ticket.getSeatingAreaName()));
        infoGrid.add(createDetailRow("Quantity:", String.valueOf(ticket.getTicketCount())));
        infoGrid.add(createDetailRow("Total Price:", NumberFormat.getCurrencyInstance(Locale.US).format(ticket.getTotalPriceUSD())));
        
        // Serial number panel below the grid
        JPanel serialPanel = new JPanel(new BorderLayout());
        serialPanel.setBorder(BorderFactory.createTitledBorder("Serial Number"));
        JLabel serialLabel = new JLabel(ticket.getTicketId(), SwingConstants.CENTER);
        serialLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        serialPanel.add(serialLabel, BorderLayout.CENTER);
        
        // Main details panel with the grid and serial number
        JPanel mainDetailsPanel = new JPanel();
        mainDetailsPanel.setLayout(new BoxLayout(mainDetailsPanel, BoxLayout.Y_AXIS));
        mainDetailsPanel.add(infoGrid);
        mainDetailsPanel.add(Box.createVerticalStrut(20)); // Spacing
        mainDetailsPanel.add(serialPanel);
        
        panel.add(mainDetailsPanel, BorderLayout.NORTH);
        
        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel contentPanel = new JPanel(new BorderLayout(15, 0));
        contentPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        contentPanel.setBackground(new Color(240, 240, 240));
        
        JPanel termsPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        termsPanel.setOpaque(false);
        JLabel termsTitle = new JLabel("IMPORTANT INFORMATION:");
        termsTitle.setFont(new Font("SansSerif", Font.BOLD, 12));
        termsTitle.setForeground(new Color(20, 20, 40));
        termsPanel.add(termsTitle);
        
        JLabel terms1 = new JLabel("<html>1. This ticket is non-transferable.</html>");
        terms1.setFont(new Font("SansSerif", Font.PLAIN, 12));
        termsPanel.add(terms1);
        
        JLabel terms2 = new JLabel("<html>2. All sales are final. No refunds.</html>");
        terms2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        termsPanel.add(terms2);
        
        contentPanel.add(termsPanel, BorderLayout.CENTER);
        
        JLabel imageLabel = new JLabel();
        try {
            BufferedImage img = ImageIO.read(new File("assets/cover image.jpg"));
            Image scaledImg = img.getScaledInstance(150, -1, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImg));
        } catch (IOException e) {
            imageLabel.setText("Image not found");
        }
        contentPanel.add(imageLabel, BorderLayout.EAST);
        return contentPanel;
    }

    private JPanel createDetailRow(String title, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        titleLabel.setForeground(Color.GRAY);
        JLabel valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        row.add(titleLabel, BorderLayout.NORTH);
        row.add(valueLabel, BorderLayout.CENTER);
        return row;
    }

    private void saveTicketAsImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Ticket as PNG");
        fileChooser.setSelectedFile(new File(ticket.getGrandPrixName().replace(" ", "_") + "_Ticket.png"));
        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            BufferedImage image = new BufferedImage(ticketContentPanel.getWidth(), ticketContentPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            ticketContentPanel.paint(g2d);
            g2d.dispose();
            try {
                ImageIO.write(image, "png", fileToSave);
                JOptionPane.showMessageDialog(this, "Ticket saved successfully as an image!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

class TicketListRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        label.setBorder(new EmptyBorder(10, 15, 10, 15));
        label.setVerticalAlignment(SwingConstants.TOP);
        if(!isSelected) {
            label.setBackground(index % 2 == 0 ? new Color(240, 240, 240) : Color.WHITE);
        }
        return label;
    }
}

class SeatingAreaRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof SeatingArea) {
            SeatingArea area = (SeatingArea) value;
            if (area.isSoldOut()) {
                setEnabled(false);
                setForeground(Color.GRAY);
            } else {
                setEnabled(true);
                setForeground(Color.BLACK);
            }
        }
        return this;
    }
}