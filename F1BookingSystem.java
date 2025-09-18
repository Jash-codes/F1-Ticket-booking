import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

// =================================================================================
// 1. Main Application Runner
// =================================================================================
public class F1BookingSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                System.out.println("System L&F not found, using default.");
            }
            new AuthFrame().setVisible(true);
        });
    }
}

// =================================================================================
// 2. Data Models
// =================================================================================
class User {
    private final String name;
    private final String email;
    private final String password;
    private double walletBalanceUSD;
    private final List<Ticket> bookedTickets = new ArrayList<>();

    public User(String name, String email, String password, double initialWallet) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.walletBalanceUSD = initialWallet;
    }

    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public double getWalletBalanceUSD() { return walletBalanceUSD; }
    public void deductFromWallet(double amountUSD) { this.walletBalanceUSD -= amountUSD; }
    public void addBookedTicket(Ticket ticket) { bookedTickets.add(ticket); }
    public List<Ticket> getBookedTickets() { return bookedTickets; }
}

class Ticket {
    private final String grandPrixName;
    private final String seatingAreaName;
    private final int ticketCount;
    private final double totalPriceUSD;
    private final Date bookingDate;
    private final String ticketId;

    public Ticket(String gpName, String areaName, int count, double price) {
        this.grandPrixName = gpName;
        this.seatingAreaName = areaName;
        this.ticketCount = count;
        this.totalPriceUSD = price;
        this.bookingDate = new Date();
        this.ticketId = "F1TKT-" + System.currentTimeMillis();
    }
    
    public String getGrandPrixName() { return grandPrixName; }
    public String getSeatingAreaName() { return seatingAreaName; }
    public int getTicketCount() { return ticketCount; }
    public double getTotalPriceUSD() { return totalPriceUSD; }
    public Date getBookingDate() { return bookingDate; }
    public String getTicketId() { return ticketId; }

    @Override
    public String toString() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy");
        return String.format("<html><b>%s</b><br>%d x %s<br>Booked on: %s - Price: %s</html>",
                grandPrixName, ticketCount, seatingAreaName, sdf.format(bookingDate),
                NumberFormat.getCurrencyInstance(Locale.US).format(totalPriceUSD));
    }
}

class SeatingArea {
    private final String name;
    private final double priceINR;

    public SeatingArea(String name, double priceINR) { this.name = name; this.priceINR = priceINR; }
    public String getName() { return name; }
    public double getPriceINR() { return priceINR; }
    @Override public String toString() { return String.format("%s - ₹%,.0f", name, priceINR); }
}

class GrandPrix {
    private final String name;
    private final String imagePath;
    private final List<SeatingArea> seatingAreas = new ArrayList<>();

    public GrandPrix(String name, String imagePath) { this.name = name; this.imagePath = imagePath; }
    public void addArea(String name, double price) { this.seatingAreas.add(new SeatingArea(name, price)); }
    public String getName() { return name; }
    public String getImagePath() { return imagePath; }
    public List<SeatingArea> getSeatingAreas() { return seatingAreas; }
    @Override public String toString() { return name; }
}

// =================================================================================
// 3. Data Manager (COMPREHENSIVELY UPDATED)
// =================================================================================
class DataManager {
    private static final Map<String, User> users = new HashMap<>();
    private static final List<GrandPrix> grandPrixList = new ArrayList<>();

    static {
        loadUsers();
        loadGrandPrixData();
    }

    private static void loadUsers() {
        users.put("lewis@f1.com", new User("Lewis H.", "lewis@f1.com", "44", 1000000));
    }

    private static void loadGrandPrixData() {
        GrandPrix abuDhabi = new GrandPrix("Abu Dhabi Grand Prix", "tracks/abu dhabi track.jpg");
        abuDhabi.addArea("Main Grandstand", 350000);
        abuDhabi.addArea("North Straight", 180000);
        abuDhabi.addArea("North Grandstand", 185000);
        abuDhabi.addArea("West Straight", 195000);
        abuDhabi.addArea("West Grandstand", 205000);
        abuDhabi.addArea("Marina Grandstand", 250000);
        abuDhabi.addArea("South Grandstand", 210000);
        abuDhabi.addArea("General Admission", 80000);
        grandPrixList.add(abuDhabi);

        GrandPrix australia = new GrandPrix("Australian Grand Prix", "tracks/australia track.jpg");
        australia.addArea("Stewart Grandstand", 115000);
        australia.addArea("Hill Grandstand", 85000);
        australia.addArea("Ricciardo Grandstand", 95000);
        australia.addArea("Jones Grandstand", 90000);
        australia.addArea("Moss Grandstand", 90000);
        australia.addArea("Fangio Grandstand", 120000);
        australia.addArea("Senna Grandstand", 110000);
        australia.addArea("Prost Grandstand", 112000);
        australia.addArea("Lauda Grandstand", 98000);
        australia.addArea("Schumacher Grandstand", 92000);
        australia.addArea("Webber Grandstand", 88000);
        australia.addArea("Vettel Grandstand", 88000);
        australia.addArea("Waite Grandstand", 82000);
        australia.addArea("Clark Grandstand", 83000);
        australia.addArea("Button Grandstand", 84000);
        grandPrixList.add(australia);
        
        GrandPrix azerbaijan = new GrandPrix("Azerbaijan Grand Prix", "tracks/azerbaijan track.jpg");
        azerbaijan.addArea("Absheron (Main)", 280000);
        azerbaijan.addArea("Champions Club", 250000);
        azerbaijan.addArea("Zafar Grandstand", 160000);
        azerbaijan.addArea("Khazar Grandstand", 155000);
        azerbaijan.addArea("Icheri Sheher", 150000);
        azerbaijan.addArea("Sahil Grandstand", 145000);
        azerbaijan.addArea("Bulvar Grandstand", 130000);
        azerbaijan.addArea("Mugham Grandstand", 125000);
        azerbaijan.addArea("Giz Galasi", 120000);
        azerbaijan.addArea("Marine Grandstand", 115000);
        azerbaijan.addArea("Azneft Grandstand", 110000);
        azerbaijan.addArea("General Admission", 60000);
        grandPrixList.add(azerbaijan);
        
        GrandPrix dutch = new GrandPrix("Dutch Grand Prix", "tracks/dutch track.jpg");
        dutch.addArea("Pit Grandstand", 450000);
        dutch.addArea("Paddock Club", 1200000);
        dutch.addArea("Hairpin Grandstand 1 & 2", 210000);
        dutch.addArea("Arena Grandstand 1", 250000);
        dutch.addArea("Champions Club", 950000);
        grandPrixList.add(dutch);

        GrandPrix italy = new GrandPrix("Italian Grand Prix (Monza)", "tracks/italy track.jpg");
        italy.addArea("Main Straight (1)", 420000);
        italy.addArea("Laterale Destra (4)", 380000);
        italy.addArea("Piscina (5)", 210000);
        italy.addArea("Alta Velocita (6a, 6b, 6c)", 250000);
        italy.addArea("Prima Variante (8a, 8b)", 220000);
        italy.addArea("Seconda Variante (9, 10)", 195000);
        italy.addArea("Variante Ascari (16, 19)", 175000);
        italy.addArea("Parabolica (22, 23a, 23b)", 190000);
        italy.addArea("General Admission", 90000);
        grandPrixList.add(italy);

        GrandPrix lasVegas = new GrandPrix("Las Vegas Grand Prix", "tracks/las vegas track.jpg");
        lasVegas.addArea("Heineken Silver (Main)", 800000);
        lasVegas.addArea("Sphere Zone (SG1-8)", 650000);
        lasVegas.addArea("T-Mobile Zone", 600000);
        lasVegas.addArea("West Harmon Zone", 500000);
        lasVegas.addArea("Caesar's Palace Experience", 950000);
        lasVegas.addArea("Flamingo General Admission", 250000);
        grandPrixList.add(lasVegas);
        
        GrandPrix qatar = new GrandPrix("Qatar Grand Prix", "tracks/qatar track.jpg");
        qatar.addArea("Main Grandstand", 300000);
        qatar.addArea("North Grandstand", 220000);
        qatar.addArea("T2 Grandstand", 190000);
        qatar.addArea("T3 Grandstand", 195000);
        qatar.addArea("T16 Grandstand", 180000);
        qatar.addArea("General Admission", 95000);
        grandPrixList.add(qatar);
        
        GrandPrix silverstone = new GrandPrix("British Grand Prix (Silverstone)", "tracks/silverstone track.jpg");
        silverstone.addArea("Hamilton Straight A/B", 550000);
        silverstone.addArea("Abbey A/B", 380000);
        silverstone.addArea("Farm Curve", 370000);
        silverstone.addArea("Village A/B", 360000);
        silverstone.addArea("The Loop", 355000);
        silverstone.addArea("Aintree", 350000);
        silverstone.addArea("Wellington Straight", 340000);
        silverstone.addArea("Brooklands", 330000);
        silverstone.addArea("Luffield", 325000);
        silverstone.addArea("Woodcote A/B", 320000);
        silverstone.addArea("National Pits Straight", 480000);
        silverstone.addArea("Copse A/B/C", 310000);
        silverstone.addArea("Becketts", 350000);
        silverstone.addArea("Chapel", 345000);
        silverstone.addArea("Stowe A/B/C", 290000);
        silverstone.addArea("Vale / Club", 420000);
        silverstone.addArea("General Admission", 150000);
        grandPrixList.add(silverstone);
        
        GrandPrix singapore = new GrandPrix("Singapore Grand Prix", "tracks/singapore track.jpg");
        singapore.addArea("Super Pit Grandstand", 650000);
        singapore.addArea("Pit Grandstand", 450000);
        singapore.addArea("Turn 1 Grandstand", 250000);
        singapore.addArea("Turn 2 Grandstand", 240000);
        singapore.addArea("Republic Grandstand", 220000);
        singapore.addArea("Raffles Grandstand", 210000);
        singapore.addArea("Bayfront Grandstand", 190000);
        singapore.addArea("Padang Grandstand", 180000);
        singapore.addArea("Connaught Grandstand", 160000);
        singapore.addArea("Orange @ Empress", 150000);
        grandPrixList.add(singapore);
        
        GrandPrix usa = new GrandPrix("United States Grand Prix", "tracks/us track.jpg");
        usa.addArea("Main Grandstand", 480000);
        usa.addArea("Turn 1 Grandstand", 350000);
        usa.addArea("Turn 4 Grandstand", 290000);
        usa.addArea("Turn 9 Grandstand", 285000);
        usa.addArea("Turn 12 Grandstand", 280000);
        usa.addArea("Turn 15 Grandstand", 320000);
        usa.addArea("Turn 19 Grandstand", 310000);
        usa.addArea("General Admission", 160000);
        grandPrixList.add(usa);
    }

    public static User authenticateUser(String email, String password) {
        User user = users.get(email.toLowerCase());
        return (user != null && user.getPassword().equals(password)) ? user : null;
    }
    public static boolean isEmailTaken(String email) { return users.containsKey(email.toLowerCase()); }
    public static User registerUser(String name, String email, String password) { 
        if(isEmailTaken(email)) return null;
        User newUser = new User(name, email, password, 1000000);
        users.put(email.toLowerCase(), newUser);
        return newUser;
    }
    public static List<GrandPrix> getAllGrandPrix() { return grandPrixList; }
}

// =================================================================================
// 4. GUI - Authentication Frame (Button Color UPDATED)
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
            new MainFrame(user).setVisible(true);
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
        
        User newUser = DataManager.registerUser(name, email, password);
        if (newUser != null) {
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
        b.setForeground(Color.BLACK); // <-- UPDATED
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

        if (field instanceof JPasswordField) {
            ((JPasswordField) field).setEchoChar((char) 0);
        }

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(Color.BLACK);
                    if (field instanceof JPasswordField) {
                        ((JPasswordField) field).setEchoChar('•');
                    }
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setForeground(Color.GRAY);
                    field.setText(placeholder);
                    if (field instanceof JPasswordField) {
                        ((JPasswordField) field).setEchoChar((char) 0);
                    }
                }
            }
        });
    }
}

// =================================================================================
// 5. GUI - Main Application Frame (Button Color UPDATED)
// =================================================================================
class MainFrame extends JFrame {
    private final User currentUser;
    private final double INR_TO_USD_RATE = 0.012;
    private JLabel walletLabel, trackImageLabel, priceLabel;
    private JComboBox<GrandPrix> gpSelector;
    private JComboBox<SeatingArea> areaSelector;
    private JSpinner ticketSpinner;
    private JTabbedPane tabbedPane;
    private DefaultListModel<Ticket> ticketListModel;
    private JList<Ticket> ticketList;

    public MainFrame(User user) { 
        this.currentUser = user;
        setTitle("F1 Grand Prix Booking");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        createHeaderPanel();
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.BOLD, 14));
        tabbedPane.addTab("  Book Tickets  ", createBookingPanel());
        tabbedPane.addTab("  My Bookings  ", createMyBookingsPanel());
        add(tabbedPane, BorderLayout.CENTER);
        gpSelector.setSelectedIndex(0);
        updateUIForSelectedGP();
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
        splitPane.setResizeWeight(0.7);
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
        gbc.insets = new Insets(8, 5, 8, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.gridx = 0; gbc.gridwidth = 2;
        gbc.gridy = 0; controlPanel.add(createStyledLabel("1. Select Grand Prix"), gbc);
        gbc.gridy = 1; 
        gpSelector = new JComboBox<>(DataManager.getAllGrandPrix().toArray(new GrandPrix[0]));
        gpSelector.addActionListener(e -> updateUIForSelectedGP());
        controlPanel.add(gpSelector, gbc);
        gbc.gridy = 2; controlPanel.add(Box.createVerticalStrut(20), gbc);
        gbc.gridy = 3; controlPanel.add(createStyledLabel("2. Select Seating Area"), gbc);
        gbc.gridy = 4;
        areaSelector = new JComboBox<>();
        areaSelector.addActionListener(e -> updatePrice());
        controlPanel.add(areaSelector, gbc);
        gbc.gridy = 5; controlPanel.add(Box.createVerticalStrut(20), gbc);
        gbc.gridy = 6; controlPanel.add(createStyledLabel("3. Number of Tickets"), gbc);
        gbc.gridy = 7;
        ticketSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        ticketSpinner.addChangeListener(e -> updatePrice());
        controlPanel.add(ticketSpinner, gbc);
        gbc.gridy = 8; gbc.weighty = 1.0; controlPanel.add(new JLabel(), gbc);
        gbc.gridy = 9; gbc.weighty = 0; gbc.gridwidth = 1;
        priceLabel = createStyledLabel("Total: $0.00");
        priceLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        controlPanel.add(priceLabel, gbc);
        gbc.gridx = 1;
        JButton bookButton = new JButton("Book Now");
        bookButton.setBackground(new Color(225, 6, 0));
        bookButton.setForeground(Color.BLACK); // <-- UPDATED
        bookButton.setFont(new Font("SansSerif", Font.BOLD, 14));
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
    private void updateWalletLabel() { 
        walletLabel.setText("Wallet: " + NumberFormat.getCurrencyInstance(Locale.US).format(currentUser.getWalletBalanceUSD()));
    }
    private void updateUIForSelectedGP() {
        GrandPrix selectedGP = (GrandPrix) gpSelector.getSelectedItem();
        if (selectedGP == null) return;
        try {
            BufferedImage img = ImageIO.read(new File(selectedGP.getImagePath()));
            Image scaledImg = img.getScaledInstance(800, -1, Image.SCALE_SMOOTH);
            trackImageLabel.setIcon(new ImageIcon(scaledImg));
        } catch (IOException e) {
            trackImageLabel.setIcon(null);
            trackImageLabel.setText("Image not found: " + selectedGP.getImagePath());
            e.printStackTrace();
        }
        areaSelector.removeAllItems();
        selectedGP.getSeatingAreas().forEach(areaSelector::addItem);
        updatePrice();
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
    private void processBooking() {
        SeatingArea area = (SeatingArea) areaSelector.getSelectedItem();
        int count = (int) ticketSpinner.getValue();
        if (area == null) return;
        double totalUsd = area.getPriceINR() * count * INR_TO_USD_RATE;
        if (currentUser.getWalletBalanceUSD() < totalUsd) {
            JOptionPane.showMessageDialog(this, "Insufficient funds.", "Payment Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        int choice = JOptionPane.showConfirmDialog(this, "Confirm booking for " + NumberFormat.getCurrencyInstance(Locale.US).format(totalUsd) + "?", "Confirm", JOptionPane.YES_NO_OPTION);
        if (choice == JOptionPane.YES_OPTION) {
            currentUser.deductFromWallet(totalUsd);
            Ticket newTicket = new Ticket(((GrandPrix)gpSelector.getSelectedItem()).getName(), area.getName(), count, totalUsd);
            currentUser.addBookedTicket(newTicket);
            updateWalletLabel();
            updateMyBookingsTab();
            JOptionPane.showMessageDialog(this, "Booking successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            tabbedPane.setSelectedIndex(1);
        }
    }
    private void updateMyBookingsTab() {
        ticketListModel.clear();
        currentUser.getBookedTickets().forEach(ticketListModel::addElement);
    }
    private void viewTicket() {
        Ticket selected = ticketList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Please select a ticket from the list first.", "No Ticket Selected", JOptionPane.WARNING_MESSAGE);
            return;
        }
        new TicketFrame(currentUser, selected).setVisible(true);
    }
    private JLabel createStyledLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 16));
        return label;
    }
}

// =================================================================================
// 6. GUI - E-Ticket Frame (UPDATED)
// =================================================================================
class TicketFrame extends JFrame {
    private final JPanel mainPanel;
    private final Ticket ticket;

    public TicketFrame(User user, Ticket ticket) {
        this.ticket = ticket;
        setTitle("Your E-Ticket: " + ticket.getGrandPrixName());
        setSize(500, 750);
        setLocationRelativeTo(null);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.WHITE);
        
        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(20, 20, 40));
        headerPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel headerLabel = new JLabel("TICKETS"); // <-- UPDATED
        headerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        headerLabel.setForeground(Color.WHITE);
        headerPanel.add(headerLabel);
        mainPanel.add(headerPanel, BorderLayout.NORTH);

        JPanel detailsPanel = createTicketDetailsPanel(user, ticket);
        mainPanel.add(detailsPanel, BorderLayout.CENTER);

        JPanel footerPanel = createFooterPanel();
        mainPanel.add(footerPanel, BorderLayout.SOUTH);

        add(mainPanel);
    }

    private JPanel createTicketDetailsPanel(User user, Ticket ticket) {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBackground(Color.WHITE);
        
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        infoPanel.add(createDetailRow("Purchaser:", user.getName()));
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(createDetailRow("Event:", ticket.getGrandPrixName()));
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(createDetailRow("Seat:", ticket.getSeatingAreaName()));
        infoPanel.add(Box.createVerticalStrut(15));
        infoPanel.add(createDetailRow("Quantity:", String.valueOf(ticket.getTicketCount())));
        infoPanel.add(Box.createVerticalStrut(15));
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy");
        infoPanel.add(createDetailRow("Date:", sdf.format(ticket.getBookingDate())));

        JPanel serialPanel = new JPanel(new BorderLayout());
        serialPanel.setOpaque(false);
        serialPanel.setBorder(BorderFactory.createTitledBorder("Serial Number"));

        JLabel serialLabel = new JLabel(ticket.getTicketId(), SwingConstants.CENTER);
        serialLabel.setFont(new Font("Monospaced", Font.BOLD, 18));
        serialPanel.add(serialLabel, BorderLayout.CENTER);

        panel.add(infoPanel, BorderLayout.CENTER);
        panel.add(serialPanel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createFooterPanel() {
        JPanel outerPanel = new JPanel();
        outerPanel.setLayout(new BoxLayout(outerPanel, BoxLayout.Y_AXIS));

        JPanel contentPanel = new JPanel(new BorderLayout(15, 0));
        contentPanel.setBorder(new EmptyBorder(10, 20, 10, 20));
        contentPanel.setBackground(new Color(240, 240, 240));

        JPanel infoPanel = new JPanel();
        infoPanel.setBackground(new Color(20, 20, 40));
        infoPanel.setBorder(new EmptyBorder(10,10,10,10));
        JTextArea termsText = new JTextArea(
            "IMPORTANT INFORMATION:\n\n" +
            "1. This ticket is non-transferable.\n" +
            "2. Gates open 2 hours before the race.\n" +
            "3. No outside food or beverages allowed.\n" +
            "4. All sales are final. No refunds.\n" +
            "5. Entry subject to security screening."
        );
        termsText.setEditable(false);
        termsText.setLineWrap(true);
        termsText.setWrapStyleWord(true);
        termsText.setBackground(new Color(20, 20, 40));
        termsText.setForeground(Color.WHITE);
        infoPanel.add(termsText);

        JLabel imageLabel = new JLabel();
        try {
            BufferedImage img = ImageIO.read(new File("assets/cover image.jpg")); // <-- UPDATED
            Image scaledImg = img.getScaledInstance(150, -1, Image.SCALE_SMOOTH);
            imageLabel.setIcon(new ImageIcon(scaledImg));
        } catch (IOException e) {
            imageLabel.setText("Image not found");
        }

        contentPanel.add(infoPanel, BorderLayout.CENTER);
        contentPanel.add(imageLabel, BorderLayout.EAST);

        JButton saveButton = new JButton("Save as Picture");
        saveButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        saveButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        saveButton.addActionListener(e -> saveTicketAsImage());

        outerPanel.add(contentPanel);
        outerPanel.add(Box.createVerticalStrut(10));
        outerPanel.add(saveButton);
        outerPanel.add(Box.createVerticalStrut(10));

        return outerPanel;
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
            
            BufferedImage image = new BufferedImage(mainPanel.getWidth(), mainPanel.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = image.createGraphics();
            mainPanel.paint(g2d);
            g2d.dispose();

            try {
                ImageIO.write(image, "png", fileToSave);
                JOptionPane.showMessageDialog(this, "Ticket saved successfully as an image!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
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

