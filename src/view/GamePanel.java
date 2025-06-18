package view;

import viewmodel.GameViewModel;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import view.SoundPlayer;

public class GamePanel extends JPanel
        implements PropertyChangeListener, KeyListener, MouseListener, MouseMotionListener {
    private GameViewModel gameViewModel;
    private KucingPanel kucingPanel;
    private IkanPanel ikanPanel;
    private TempatMakanPanel tempatMakanPanel;
    private Timer uiUpdateTimer;
    private Image backgroundImage; // Background image // UI Components
    private JLabel scoreLabel;
    private JLabel fishCountLabel;
    private JLabel timerLabel;
    private JLabel highScoreLabel;
    private JButton backToMenuButton;
    private SoundPlayer backgroundMusicPlayer;

    public GamePanel(GameViewModel gameViewModel) {
        System.out.println("=== CREATING NEW GAMEPANEL INSTANCE ===");
        this.gameViewModel = gameViewModel;
        setLayout(new BorderLayout());
        setFocusable(true);

        initializeComponents();
        setupUI();
        setupTimers();
        loadBackgroundImage(); // Load background image
        loadBackgroundMusic(); // Load background music

        // Add listeners
        addKeyListener(this);
        addMouseListener(this);
        addMouseMotionListener(this);

        // Add property change listener
        gameViewModel.addPropertyChangeListener(this);

        // JANGAN AUTO START GAME - akan dipanggil manual setelah GamePanel dibuat
        System.out.println("=== GAMEPANEL CREATION COMPLETE (NOT STARTED YET) ===");
    }

    private void initializeComponents() {
        // Create kucing panel
        kucingPanel = new KucingPanel(gameViewModel);

        // Create ikan panel
        ikanPanel = new IkanPanel(gameViewModel.getIkanViewModel());

        // Create tempat makan panel
        tempatMakanPanel = new TempatMakanPanel(gameViewModel); // Create UI labels
        scoreLabel = new JLabel("Score: 0");
        fishCountLabel = new JLabel("Fish: 0");
        timerLabel = new JLabel("Time: 01:00");
        highScoreLabel = new JLabel("High Score: 0");// Create buttons
        backToMenuButton = new JButton("Menu");

        // Style UI components
        setupUIStyles();
    }

    private void setupUIStyles() {
        Font gameFont = new Font("Arial", Font.BOLD, 16);
        Color textColor = Color.WHITE;
        scoreLabel.setFont(gameFont);
        scoreLabel.setForeground(textColor);

        fishCountLabel.setFont(gameFont);
        fishCountLabel.setForeground(textColor);

        timerLabel.setFont(gameFont);
        timerLabel.setForeground(textColor);
        highScoreLabel.setFont(gameFont);
        highScoreLabel.setForeground(textColor);

        backToMenuButton.setFont(new Font("Arial", Font.BOLD, 14));
    }

    private void setupUI() { // Main game area - use custom paintComponent
        JPanel gameArea = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                // Draw background
                if (backgroundImage != null) {
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                }

                // Draw additional game elements
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw fish using IkanPanel
                if (ikanPanel != null) {
                    ikanPanel.drawAllIkan(g2d);
                }

                // Draw tempat makan
                if (tempatMakanPanel != null) {
                    tempatMakanPanel.repaint(); // Make sure it's painted
                }

                g2d.dispose();
            }
        };
        gameArea.setLayout(new OverlayLayout(gameArea));
        gameArea.setOpaque(false); // Add panels to game area
        kucingPanel.setOpaque(false);
        ikanPanel.setOpaque(false);
        tempatMakanPanel.setOpaque(false);

        gameArea.add(kucingPanel);
        gameArea.add(ikanPanel);
        gameArea.add(tempatMakanPanel);

        // Create UI overlay panel
        JPanel uiOverlay = createUIOverlay();
        gameArea.add(uiOverlay);

        add(gameArea, BorderLayout.CENTER);

        // Add event listeners
        setupEventListeners();
    }

    private JPanel createUIOverlay() {
        JPanel overlay = new JPanel(null); // Absolute positioning
        overlay.setOpaque(false); // Position UI elements
        scoreLabel.setBounds(10, 10, 150, 30);
        fishCountLabel.setBounds(10, 50, 150, 30);
        timerLabel.setBounds(10, 90, 150, 30);
        highScoreLabel.setBounds(10, 130, 150, 30);
        backToMenuButton.setBounds(10, 170, 80, 30);

        overlay.add(scoreLabel);
        overlay.add(fishCountLabel);
        overlay.add(timerLabel);
        overlay.add(highScoreLabel);
        overlay.add(backToMenuButton);

        return overlay;
    }

    private void setupEventListeners() { // Back to menu button
        backToMenuButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Kembali ke menu utama?\nProgress game akan hilang.",
                    "Konfirmasi",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                gameViewModel.stopGame();
                if (backgroundMusicPlayer != null) {
                    System.out.println("Stopping music - returning to menu");
                    backgroundMusicPlayer.stop(); // Stop music when returning to menu
                }
                App.showMainMenu();
            }
        });

        // Key listeners for game controls
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                gameViewModel.handleKeyPressed(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
                gameViewModel.handleKeyReleased();
            }
        });

        // Mouse listeners for game interaction
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                gameViewModel.handleMousePressed(e.getX(), e.getY());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                gameViewModel.handleMouseReleased();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                gameViewModel.handleMouseMoved(e.getX(), e.getY());
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                gameViewModel.handleMouseMoved(e.getX(), e.getY());
            }
        });
    }

    private void setupTimers() {
        // UI update timer (30 FPS) - BUAT tapi jangan start dulu
        uiUpdateTimer = new Timer(1000 / 30, e -> updateGameUI());
        // Timer akan di-start saat startGame() dipanggil
    }

    private void updateGameUI() {
        if (gameViewModel != null) {
            GameViewModel.GameStats stats = gameViewModel.getGameStats(); // Update labels
            scoreLabel.setText("Score: " + gameViewModel.getScore());
            fishCountLabel.setText("Fish: " + gameViewModel.getFishCount());
            timerLabel.setText("Time: " + stats.formattedTime);
            highScoreLabel.setText("High Score: " + stats.highScore);

            // Debug: Print remaining time
            if (stats.remainingTime <= 5) {
                System.out.println("=== TIME RUNNING OUT: " + stats.remainingTime + " seconds ===");
            }

            // Update timer color based on remaining time
            if (stats.remainingTime <= 10) {
                timerLabel.setForeground(Color.RED);
            } else if (stats.remainingTime <= 30) {
                timerLabel.setForeground(Color.ORANGE);
            } else {
                timerLabel.setForeground(Color.WHITE);
            }
        }

        // Repaint panels
        if (kucingPanel != null) {
            kucingPanel.repaint();
        }
        if (ikanPanel != null) {
            ikanPanel.repaint();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        System.out.println("=== PROPERTY CHANGE: " + propertyName + " = " + evt.getNewValue() + " ===");

        switch (propertyName) {
            case "gameOver":
                System.out.println("=== GAME OVER EVENT RECEIVED ===");
                SwingUtilities.invokeLater(this::showGameOverDialog);
                break;
            case "newHighScore":
                System.out.println("=== NEW HIGH SCORE EVENT RECEIVED ===");
                SwingUtilities.invokeLater(this::showNewHighScoreDialog);
                break;
            case "scoreChanged":
            case "remainingTime":
                // Force immediate UI update when score or time changes
                SwingUtilities.invokeLater(this::updateGameUI);
                break;
        }
    }

    private Rectangle getBackToMenuButtonBounds() {
        return new Rectangle(10, 10, 120, 35);
    }

    private void showGameOverDialog() {
        System.out.println("=== SHOWING GAME OVER DIALOG ===");
        GameViewModel.GameStats stats = gameViewModel.getGameStats();
        String message;

        if (stats.isNewHighScore) {
            message = String.format(
                    "TIME'S UP!\n\n" +
                            "NEW HIGH SCORE!\n" +
                            "Total Score: %d points\n" +
                            "Fish Caught: %d fish\n\n" +
                            "Congratulations!",
                    gameViewModel.getScore(),
                    gameViewModel.getFishCount());
        } else {
            message = String.format(
                    "TIME'S UP!\n\n" +
                            "Final Score: %d points\n" +
                            "Fish Caught: %d fish\n" +
                            "High Score: %d points",
                    gameViewModel.getScore(),
                    gameViewModel.getFishCount(),
                    stats.highScore);
        }

        String[] options = { "Play Again", "Back to Menu", "Exit" };
        int result = JOptionPane.showOptionDialog(
                this,
                message,
                "Game Over",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.INFORMATION_MESSAGE,
                null,
                options,
                options[0]);
        switch (result) {
            case 0: // Play Again
                System.out.println("=== PLAY AGAIN CLICKED - USING FORCE RESTART ===");
                forceRestartGame();
                break;
            case 1: // Back to Menu
                System.out.println("=== BACK TO MENU CLICKED ===");
                if (backgroundMusicPlayer != null) {
                    backgroundMusicPlayer.stop(); // Stop music when going to menu
                }
                // Use App.showMainMenu() if available
                try {
                    Class<?> appClass = Class.forName("view.App");
                    java.lang.reflect.Method showMainMenuMethod = appClass.getMethod("showMainMenu");
                    showMainMenuMethod.invoke(null);
                } catch (Exception e) {
                    System.err.println("Could not return to main menu: " + e.getMessage());
                    System.exit(0);
                }
                break;
            case 2: // Exit
            default:
                System.out.println("=== EXIT CLICKED ===");
                if (backgroundMusicPlayer != null) {
                    backgroundMusicPlayer.stop();
                }
                System.exit(0);
                break;
        }
    }

    private void showNewHighScoreDialog() {
        JOptionPane.showMessageDialog(
                this,
                "NEW HIGH SCORE!\n\nAmazing performance!",
                "New Record!",
                JOptionPane.INFORMATION_MESSAGE);
    }

    // Cleanup method
    public void cleanup() {
        if (uiUpdateTimer != null && uiUpdateTimer.isRunning()) {
            uiUpdateTimer.stop();
        }

        if (gameViewModel != null) {
            gameViewModel.removePropertyChangeListener(this);
        }

        if (kucingPanel != null) {
            kucingPanel.cleanup();
        }

        if (ikanPanel != null) {
            ikanPanel.cleanup();
        }

        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.close(); // Close the sound clip
        }
    }

    // Public methods for external access
    public void startGame() {
        System.out.println("=== GAMEPANEL.STARTGAME CALLED ===");
        gameViewModel.startGame();

        // START UI UPDATE TIMER - INI YANG HILANG!
        if (uiUpdateTimer != null && !uiUpdateTimer.isRunning()) {
            uiUpdateTimer.start();
            System.out.println("UI Update timer started");
        }

        // Initial UI update
        updateGameUI();

        System.out.println("Starting game - attempting to start background music...");
        if (backgroundMusicPlayer != null) {
            System.out.println("Background music player is not null, starting loop...");
            backgroundMusicPlayer.loop(); // Start looping music when game starts
        } else {
            System.out.println("Background music player is null!");
        }
        requestFocusInWindow();
        System.out.println("=== GAMEPANEL.STARTGAME COMPLETE ===");
    }

    public void stopGame() {
        gameViewModel.stopGame();
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop(); // Stop music when game stops
        }
    }

    public void setPlayerName(String playerName) {
        gameViewModel.setCurrentPlayerName(playerName);
    }

    private void loadBackgroundImage() {
        try {
            backgroundImage = new ImageIcon(getClass().getResource("/assets/backgroundd.png")).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load background image: " + e.getMessage());
        }
    } // New method to load background music

    private void loadBackgroundMusic() {
        System.out.println("Attempting to load background music...");
        backgroundMusicPlayer = new SoundPlayer("/assets/backgroundmusic.wav");
        if (backgroundMusicPlayer != null) {
            System.out.println("Background music player created successfully");
        } else {
            System.out.println("Failed to create background music player");
        }
    }

    // Method untuk restart background music
    public void restartBackgroundMusic() {
        System.out.println("Restarting background music...");
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.stop();
            backgroundMusicPlayer.loop();
        } else {
            System.out.println("Background music player is null, reloading...");
            loadBackgroundMusic();
            if (backgroundMusicPlayer != null) {
                backgroundMusicPlayer.loop();
            }
        }
    }

    // Method untuk memulai musik paksa (jika dibutuhkan dari luar)
    public void forceStartBackgroundMusic() {
        System.out.println("Force starting background music...");
        if (backgroundMusicPlayer == null) {
            loadBackgroundMusic();
        }
        if (backgroundMusicPlayer != null) {
            backgroundMusicPlayer.loop();
        } else {
            System.out.println("Failed to force start background music - player is null");
        }
    }

    // Method static untuk dipanggil dari game over dialog
    public static void forceRestartGame() {
        System.out.println("=== FORCE RESTART GAME CALLED ===");
        try {
            Class<?> appClass = Class.forName("view.App");
            java.lang.reflect.Method restartGameMethod = appClass.getMethod("restartGame");
            restartGameMethod.invoke(null);
            System.out.println("=== FORCE RESTART SUCCESS ===");
        } catch (Exception e) {
            System.err.println("FAILED to force restart: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Event handling methods
    @Override
    public void keyPressed(KeyEvent e) {
        gameViewModel.handleKeyPressed(e.getKeyCode());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        gameViewModel.handleKeyReleased();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // Not used
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // Request focus untuk keyboard input
        requestFocusInWindow();

        // Check if back to menu button was clicked
        Rectangle backButtonBounds = getBackToMenuButtonBounds();
        if (backButtonBounds.contains(e.getX(), e.getY())) {
            // Show confirmation dialog
            int result = JOptionPane.showConfirmDialog(
                    this,
                    "Kembali ke menu utama?\nProgress game akan hilang.",
                    "Konfirmasi",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                if (backgroundMusicPlayer != null) {
                    System.out.println("Stopping music - returning to menu via mouse click");
                    backgroundMusicPlayer.stop(); // Stop music when returning to menu
                }
                App.showMainMenu();
            }
            return;
        }

        // Handle normal game mouse press
        gameViewModel.handleMousePressed(e.getX(), e.getY());
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        gameViewModel.handleMouseReleased();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        gameViewModel.handleMouseMoved(e.getX(), e.getY());
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // Not used
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Not used
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Not used
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        gameViewModel.handleMouseMoved(e.getX(), e.getY());
    }
}