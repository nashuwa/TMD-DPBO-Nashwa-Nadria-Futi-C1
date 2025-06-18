package view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import model.DatabaseManager;
import java.util.List;

public class MainMenuPanel extends JPanel {
    private ActionListener startGameListener;
    private ActionListener exitGameListener;
    private Image backgroundImage;
    private Font titleFont;
    private Font buttonFont;

    // Colors
    private final Color TITLE_COLOR = new Color(255, 215, 0); // Gold
    private final Color BUTTON_COLOR = new Color(70, 130, 180); // Steel Blue
    private final Color BUTTON_HOVER_COLOR = new Color(100, 149, 237); // Cornflower Blue
    private final Color BUTTON_TEXT_COLOR = Color.WHITE;
    private final Color BACKGROUND_COLOR = new Color(135, 206, 235); // Sky Blue

    // Button states
    private boolean startButtonHovered = false;
    private boolean exitButtonHovered = false;
    private int hoveredLeaderboardIndex = -1; // Index of hovered leaderboard entry

    // Button dimensions
    private final int BUTTON_WIDTH = 200;
    private final int BUTTON_HEIGHT = 60;
    private JTextField playerNameField;
    private List<DatabaseManager.Player> leaderboard;

    public MainMenuPanel() {
        setPreferredSize(new Dimension(800, 600));
        setBackground(BACKGROUND_COLOR);
        loadAssets();
        setupMouseListener();
        setupPlayerNameInput();
        loadLeaderboard();
        setFocusable(true);
    }

    private void loadAssets() {
        // Coba beberapa variasi path
        try {
            // Variasi 1: Tanpa leading slash
            backgroundImage = new ImageIcon(getClass().getResource("assets/backgroundd.png")).getImage();
        } catch (Exception e1) {
            try {
                // Variasi 2: Dengan leading slash
                backgroundImage = new ImageIcon(getClass().getResource("/assets/backgroundd.png")).getImage();
            } catch (Exception e2) {
                try {
                    // Variasi 3: Relative ke src
                    backgroundImage = new ImageIcon("assets/backgroundd.png").getImage();
                } catch (Exception e3) {
                    System.out.println("All paths failed, using default");
                }
            }
        }

        // Setup fonts
        try {
            titleFont = new Font("Arial", Font.BOLD, 48);
            buttonFont = new Font("Arial", Font.BOLD, 18);
        } catch (Exception e) {
            titleFont = new Font(Font.SERIF, Font.BOLD, 48);
            buttonFont = new Font(Font.SERIF, Font.BOLD, 18);
        }
    }

    private void setupMouseListener() {
        addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                handleMouseClick(e.getX(), e.getY());
            }
        });

        addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                handleMouseMove(e.getX(), e.getY());
            }
        });
    }

    private void handleMouseClick(int mouseX, int mouseY) {
        Rectangle startButtonBounds = getStartButtonBounds();
        Rectangle exitButtonBounds = getExitButtonBounds();

        if (startButtonBounds.contains(mouseX, mouseY)) {
            if (startGameListener != null) {
                startGameListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "START_GAME"));
            }
        } else if (exitButtonBounds.contains(mouseX, mouseY)) {
            if (exitGameListener != null) {
                exitGameListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "EXIT_GAME"));
            }
        } else {
            // Check leaderboard clicks
            if (leaderboard != null && !leaderboard.isEmpty()) {
                for (int i = 0; i < Math.min(leaderboard.size(), 5); i++) {
                    Rectangle playerBounds = getLeaderboardEntryBounds(i);
                    if (playerBounds.contains(mouseX, mouseY)) {
                        DatabaseManager.Player selectedPlayer = leaderboard.get(i);
                        selectPlayerFromLeaderboard(selectedPlayer);
                        break;
                    }
                }
            }
        }
    }

    private void handleMouseMove(int mouseX, int mouseY) {
        Rectangle startButtonBounds = getStartButtonBounds();
        Rectangle exitButtonBounds = getExitButtonBounds();

        boolean oldStartHovered = startButtonHovered;
        boolean oldExitHovered = exitButtonHovered;
        int oldLeaderboardHover = hoveredLeaderboardIndex;

        startButtonHovered = startButtonBounds.contains(mouseX, mouseY);
        exitButtonHovered = exitButtonBounds.contains(mouseX, mouseY);

        // Check leaderboard hover
        hoveredLeaderboardIndex = -1;
        if (leaderboard != null && !leaderboard.isEmpty()) {
            for (int i = 0; i < Math.min(leaderboard.size(), 5); i++) {
                Rectangle playerBounds = getLeaderboardEntryBounds(i);
                if (playerBounds.contains(mouseX, mouseY)) {
                    hoveredLeaderboardIndex = i;
                    break;
                }
            }
        }

        if (oldStartHovered != startButtonHovered || oldExitHovered != exitButtonHovered ||
                oldLeaderboardHover != hoveredLeaderboardIndex) {
            repaint();
        }

        if (startButtonHovered || exitButtonHovered || hoveredLeaderboardIndex != -1) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        } else {
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        drawBackground(g2d);
        drawTitle(g2d);
        drawMainUI(g2d); // Method baru yang menggabungkan buttons dan game info

        g2d.dispose();
    }

    private void drawBackground(Graphics2D g2d) {
        // Draw background image if available, otherwise use gradient
        if (backgroundImage != null) {
            g2d.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        } else {
            // Create gradient background
            GradientPaint gradient = new GradientPaint(
                    0, 0, new Color(135, 206, 250), // Light Sky Blue
                    0, getHeight(), new Color(70, 130, 180) // Steel Blue
            );
            g2d.setPaint(gradient);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    private void drawTitle(Graphics2D g2d) {
        // Draw game title
        String title = "LILO SI KUCING RAKUS";
        g2d.setFont(titleFont);
        FontMetrics fm = g2d.getFontMetrics();
        int titleWidth = fm.stringWidth(title);
        int titleX = (getWidth() - titleWidth) / 2;
        int titleY = getHeight() / 4;

        // Draw title shadow
        g2d.setColor(new Color(0, 0, 0, 100)); // Semi-transparent black
        g2d.drawString(title, titleX + 3, titleY + 3);

        // Draw title
        g2d.setColor(TITLE_COLOR);
        g2d.drawString(title, titleX, titleY);

        // Draw subtitle
        String subtitle = "~ Lilo sang Pemancing Handal ~";
        Font subtitleFont = new Font("Arial", Font.ITALIC, 20);
        g2d.setFont(subtitleFont);
        FontMetrics subtitleFm = g2d.getFontMetrics();
        int subtitleWidth = subtitleFm.stringWidth(subtitle);
        int subtitleX = (getWidth() - subtitleWidth) / 2;
        int subtitleY = titleY + 50;

        g2d.setColor(new Color(255, 255, 255, 200)); // Semi-transparent white
        g2d.drawString(subtitle, subtitleX, subtitleY);
    }

    private void drawButton(Graphics2D g2d, String text, Rectangle bounds, boolean hovered) {
        // Button colors
        Color bgColor = hovered ? BUTTON_HOVER_COLOR : BUTTON_COLOR;
        Color textColor = BUTTON_TEXT_COLOR;

        // Draw button shadow
        g2d.setColor(new Color(0, 0, 0, 50));
        RoundRectangle2D shadowButton = new RoundRectangle2D.Float(
                bounds.x + 3, bounds.y + 3, bounds.width, bounds.height, 15, 15);
        g2d.fill(shadowButton);

        // Draw button background
        g2d.setColor(bgColor);
        RoundRectangle2D button = new RoundRectangle2D.Float(
                bounds.x, bounds.y, bounds.width, bounds.height, 15, 15);
        g2d.fill(button);

        // Draw button border
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.draw(button);

        // Draw button text
        g2d.setFont(buttonFont);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int textHeight = fm.getHeight();
        int textX = bounds.x + (bounds.width - textWidth) / 2;
        int textY = bounds.y + (bounds.height - textHeight) / 2 + fm.getAscent();

        g2d.setColor(textColor);
        g2d.drawString(text, textX, textY);
    }

    private void drawMainUI(Graphics2D g2d) {
        // Gambar tombol di tengah kiri
        drawButtonsLeftCenter(g2d);

        // Gambar leaderboard di kanan atas (tidak bertabrakan dengan tombol)
        drawLeaderboardRightSide(g2d);

        // Gambar player input di bawah tombol
        drawPlayerInputSection(g2d);
    }

    private void drawButtonsLeftCenter(Graphics2D g2d) {
        // Posisi tombol di sebelah kiri tengah
        int buttonStartX = 150; // Geser ke kiri
        int centerY = getHeight() / 2;

        // Draw Start Game button
        Rectangle startButtonBounds = new Rectangle(buttonStartX, centerY - BUTTON_HEIGHT - 10, BUTTON_WIDTH,
                BUTTON_HEIGHT);
        drawButton(g2d, "START GAME", startButtonBounds, startButtonHovered);

        // Draw Exit button
        Rectangle exitButtonBounds = new Rectangle(buttonStartX, centerY + 10, BUTTON_WIDTH, BUTTON_HEIGHT);
        drawButton(g2d, "EXIT", exitButtonBounds, exitButtonHovered);
    }

    private void drawLeaderboardRightSide(Graphics2D g2d) {
        // Draw leaderboard di kanan atas (tidak bertabrakan dengan tombol)
        if (leaderboard != null && !leaderboard.isEmpty()) {
            // Draw leaderboard background
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRoundRect(480, 230, 250, 350, 15, 15); // Lebih besar dan lebih ke bawah

            // Draw leaderboard border
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(480, 230, 250, 350, 15, 15);

            // Draw leaderboard title
            g2d.setColor(new Color(255, 215, 0)); // Gold color
            g2d.setFont(new Font("Arial", Font.BOLD, 20));
            FontMetrics titleFm = g2d.getFontMetrics();
            String title = "LEADERBOARD";
            int titleX = 480 + (250 - titleFm.stringWidth(title)) / 2;
            g2d.drawString(title, titleX, 265);

            // Draw leaderboard entries
            g2d.setFont(new Font("Arial", Font.PLAIN, 16));
            int y = 295;
            int rank = 1;
            for (DatabaseManager.Player player : leaderboard) {
                if (rank > 5)
                    break; // Show only top 5

                // Highlight hovered entry
                if (hoveredLeaderboardIndex == rank - 1) {
                    g2d.setColor(new Color(255, 255, 255, 50));
                    g2d.fillRoundRect(485, y - 15, 240, 22, 5, 5);
                    g2d.setColor(new Color(255, 215, 0)); // Gold for hovered text
                } else {
                    g2d.setColor(Color.WHITE);
                }
                String entry = rank + ". " + player.getName() + " - " + player.getHighScore() + "pts/"
                        + player.getHighFishCount() + "fish";
                g2d.drawString(entry, 490, y);

                // Add "Click to play" hint for hovered entry
                if (hoveredLeaderboardIndex == rank - 1) {
                    g2d.setFont(new Font("Arial", Font.ITALIC, 12));
                    g2d.setColor(new Color(200, 200, 200));
                    g2d.drawString("(Click to play)", 650, y);
                    g2d.setFont(new Font("Arial", Font.PLAIN, 16));
                }

                y += 30;
                rank++;
            }

            // If no players yet
            if (leaderboard.isEmpty()) {
                g2d.setColor(new Color(200, 200, 200));
                g2d.drawString("No scores yet", 530, 180);
            }
        }
    }

    private void drawPlayerInputSection(Graphics2D g2d) {
        // Draw player name input area di bawah tombol
        int inputY = 460;

        // Draw label
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        FontMetrics fm = g2d.getFontMetrics();
        String label = "Enter Your Name:";
        int labelX = 150 + (200 - fm.stringWidth(label)) / 2;
        g2d.drawString(label, labelX, inputY);

        // Draw input box outline (the actual input field is drawn by Swing)
        g2d.setColor(new Color(255, 255, 255, 100));
        g2d.setStroke(new BasicStroke(2));
        g2d.drawRoundRect(149, inputY + 9, 202, 32, 8, 8);
    }

    private Rectangle getStartButtonBounds() {
        int buttonStartX = 150; // Sesuai dengan drawButtonsLeftCenter
        int centerY = getHeight() / 2;
        return new Rectangle(buttonStartX, centerY - BUTTON_HEIGHT - 10, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private Rectangle getExitButtonBounds() {
        int buttonStartX = 150; // Sesuai dengan drawButtonsLeftCenter
        int centerY = getHeight() / 2;
        return new Rectangle(buttonStartX, centerY + 10, BUTTON_WIDTH, BUTTON_HEIGHT);
    }

    private void setupPlayerNameInput() {
        setLayout(null); // Use absolute positioning

        playerNameField = new JTextField("User");

        int inputY = 470; // Posisi absolut, bukan relatif
        playerNameField.setBounds(150, inputY, 200, 30);
        playerNameField.setFont(new Font("Arial", Font.PLAIN, 16));
        playerNameField.setHorizontalAlignment(JTextField.CENTER);
        playerNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 2),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        add(playerNameField);
    }

    private void loadLeaderboard() {
        try {
            DatabaseManager dbManager = DatabaseManager.getInstance();
            leaderboard = dbManager.getTopPlayers(5);
        } catch (Exception e) {
            System.err.println("Error loading leaderboard: " + e.getMessage());
        }
    }

    private Rectangle getLeaderboardEntryBounds(int index) {
        int entryY = 295 + (index * 30);
        return new Rectangle(480, entryY - 15, 250, 30);
    }

    private void selectPlayerFromLeaderboard(DatabaseManager.Player player) {
        // Set the player name in the text field
        playerNameField.setText(player.getName()); // Show confirmation dialog
        String message = String.format(
                "Play as '%s'?\n\nHigh Score: %d points\nBest Fish Count: %d fish",
                player.getName(),
                player.getHighScore(),
                player.getHighFishCount());

        int result = JOptionPane.showConfirmDialog(
                this,
                message,
                "Select Player",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            // Start game with selected player
            if (startGameListener != null) {
                startGameListener.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "START_GAME"));
            }
        }
    }

    public String getCurrentPlayerName() {
        String name = playerNameField.getText().trim();
        return name.isEmpty() ? "Player" : name;
    }

    public void refreshLeaderboard() {
        loadLeaderboard();
        repaint();
    }

    // Event listener setters
    public void setStartGameListener(ActionListener listener) {
        this.startGameListener = listener;
    }

    public void setExitGameListener(ActionListener listener) {
        this.exitGameListener = listener;
    }
}