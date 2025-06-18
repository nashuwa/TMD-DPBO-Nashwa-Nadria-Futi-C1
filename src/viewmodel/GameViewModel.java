package viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.awt.event.KeyEvent;
import javax.swing.Timer;
import model.DatabaseManager;

public class GameViewModel {
    private PropertyChangeSupport support;
    private KucingViewModelNew kucingViewModelNew;
    private TempatMakanViewModel tempatMakanViewModel;
    private IkanViewModel ikanViewModel;
    private Timer handTrackingTimer; // Timer untuk update hand tracking
    private Timer gameTimer; // Timer untuk game countdown

    // Game configuration
    private int panelWidth = 800;
    private int panelHeight = 600;
    private boolean isGameRunning = false;
    private int score = 0; // Score tracking (total points from fish values)
    private int fishCount = 0; // Fish count tracking (total number of fish caught)

    // Game timer configuration
    private int gameTimeLimit = 60; // 1 menit dalam detik
    private int remainingTime = gameTimeLimit;
    private boolean isTimeUp = false;
    private int highScore = 0; // High score tracking

    // Player management
    private String currentPlayerName = "";
    private DatabaseManager databaseManager;

    public GameViewModel() {
        support = new PropertyChangeSupport(this);
        databaseManager = DatabaseManager.getInstance();
        initializeViewModels();
        setupHandTracking();
        setupGameTimer();
        loadHighScore();
    }

    private void setupHandTracking() {
        // Timer untuk update hand tracking (30 FPS)
        handTrackingTimer = new Timer(1000 / 30, e -> updateHandTracking());
    } // State untuk pengantaran ikan

    private model.Ikan carriedFish = null;

    private void updateHandTracking() {
        if (!isGameRunning) {
            return;
        }

        // Update delivery process
        updateFishDelivery();
    }

    private void updateFishDelivery() {
        if (carriedFish != null && kucingViewModelNew != null && tempatMakanViewModel != null) {
            // 1. Buat ikan mengikuti posisi tangan kucing
            KucingViewModelNew.KucingViewData kucingData = kucingViewModelNew.getKucingViewData();
            if (kucingData != null) {
                carriedFish.setPosX(kucingData.handX - carriedFish.getWidth() / 2);
                carriedFish.setPosY(kucingData.handY - carriedFish.getHeight() / 2);
            }

            // 2. Cek apakah tangan sudah sampai di tempat makan
            model.TempatMakan tmModel = tempatMakanViewModel.getModel();
            if (tmModel != null) {
                int handX = kucingViewModelNew.getHandX();
                int handY = kucingViewModelNew.getHandY();
                double distance = Math
                        .sqrt(Math.pow(handX - tmModel.getCenterX(), 2) + Math.pow(handY - tmModel.getCenterY(), 2));

                // Jika jarak cukup dekat, selesaikan pengantaran
                if (distance < 50) { // Increase threshold
                    handleFishDelivered(carriedFish); // Reset state pengantaran
                    this.carriedFish = null;
                    kucingViewModelNew.getModel().setHandDelivering(false);
                    kucingViewModelNew.setHandActive(false); // Tangan akan kembali ke kucing
                }
            }
        }
    }

    private void handleFishDelivered(model.Ikan fish) {
        if (fish != null) {
            ikanViewModel.removeIkan(fish);
            if (tempatMakanViewModel != null) {
                tempatMakanViewModel.addFish();
            }

            // Tambah skor berdasarkan jenis ikan
            int oldScore = this.score;
            int fishScore = fish.getScore(); // Dapatkan skor dari ikan (10, 20, atau 30)
            this.score += fishScore;
            support.firePropertyChange("scoreChanged", oldScore, this.score);

            // Tambah jumlah ikan yang ditangkap
            int oldFishCount = this.fishCount;
            this.fishCount += 1;
            support.firePropertyChange("fishCountChanged", oldFishCount, this.fishCount);

            // Update high score tanpa pop-up
            if (this.score > this.highScore) {
                this.highScore = this.score;
            }
        }
    }

    private void initializeViewModels() {
        // Initialize KucingViewModelNew with all game logic
        kucingViewModelNew = new KucingViewModelNew();

        // Initialize IkanViewModel
        ikanViewModel = new IkanViewModel();

        // Initialize other ViewModels (if needed separately)
        // tempatMakanViewModel akan dibuat setelah setPanelDimensions dipanggil

        // Forward property changes from child ViewModels
        kucingViewModelNew.addPropertyChangeListener(evt -> {
            // Forward all kucing events to our listeners
            support.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());

            // Handle special events
            if ("tempatMakanCreated".equals(evt.getPropertyName())) {
                // Create TempatMakanViewModel when tempat makan is created
                if (evt.getNewValue() instanceof model.TempatMakan) {
                    tempatMakanViewModel = new TempatMakanViewModel((model.TempatMakan) evt.getNewValue());
                    tempatMakanViewModel.setVisible(false); // Start hidden

                    // Forward tempat makan events
                    tempatMakanViewModel.addPropertyChangeListener(tempatMakanEvt -> {
                        support.firePropertyChange(tempatMakanEvt.getPropertyName(),
                                tempatMakanEvt.getOldValue(),
                                tempatMakanEvt.getNewValue());
                    });
                }
            }
        });

        // Forward KucingViewModelNew events
        if (kucingViewModelNew != null) {
            kucingViewModelNew.addPropertyChangeListener(evt -> {
                support.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
            });
        } // Forward IkanViewModel events
        if (ikanViewModel != null) {
            ikanViewModel.addPropertyChangeListener(evt -> {
                support.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
                // Fish delivery scoring sudah dihandle di handleFishDelivered()
            });
        }
    } // Game configuration methods

    public void setPanelDimensions(int width, int height) {
        int oldWidth = this.panelWidth;
        int oldHeight = this.panelHeight;

        this.panelWidth = width;
        this.panelHeight = height;
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setPanelDimensions(width, height);
        }

        support.firePropertyChange("panelDimensions",
                new java.awt.Dimension(oldWidth, oldHeight),
                new java.awt.Dimension(width, height));
    }

    public void startGame() {
        if (!isGameRunning) {
            // SELALU reset semua game state sebelum memulai game baru
            resetGameState();

            isGameRunning = true;

            if (kucingViewModelNew != null) {
                kucingViewModelNew.startGameLoop();
                kucingViewModelNew.startAnimation();
            }
            if (ikanViewModel != null) {
                ikanViewModel.startMovement();
            }

            // Start hand tracking timer
            if (handTrackingTimer != null && !handTrackingTimer.isRunning()) {
                handTrackingTimer.start();
            }

            // Start game timer
            if (gameTimer != null && !gameTimer.isRunning()) {
                remainingTime = gameTimeLimit; // Reset remaining time
                gameTimer.start();
            }

            support.firePropertyChange("gameRunning", false, true);
        }
    }

    public void stopGame() {
        if (isGameRunning) {
            isGameRunning = false;

            // Stop hand tracking timer
            if (handTrackingTimer != null && handTrackingTimer.isRunning()) {
                handTrackingTimer.stop();
            }

            // Stop game timer
            if (gameTimer != null && gameTimer.isRunning()) {
                gameTimer.stop();
            }

            support.firePropertyChange("gameRunning", true, false);
        }
    }

    // Delegate methods to child ViewModels
    public void setKucingVelocity(int vx, int vy) {
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setKucingVelocity(vx, vy);
        }
    }

    public void setHandActive(boolean active) {
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setHandActive(active);
        }
    }

    public void setHandTarget(int x, int y) {
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setHandTarget(x, y);
        }
    }

    public void setTempatMakanHover(int mouseX, int mouseY) {
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setTempatMakanHover(mouseX, mouseY);
        }
    }

    // Data access methods for Views
    public KucingViewModelNew getKucingViewModel() {
        return kucingViewModelNew;
    }

    public KucingViewModelNew getKucingViewModelNew() {
        return kucingViewModelNew;
    }

    // For backward compatibility - return KucingViewModelNew as main kucing
    // viewmodel
    public KucingViewModelNew getKucingViewModel2() {
        return kucingViewModelNew;
    }

    public TempatMakanViewModel getTempatMakanViewModel() {
        return tempatMakanViewModel;
    }

    public IkanViewModel getIkanViewModel() {
        return ikanViewModel;
    } // Game stats for status panel

    public GameStats getGameStats() {
        GameStats stats = new GameStats();

        if (kucingViewModelNew != null) {
            stats.fishDelivered = kucingViewModelNew.getFishDelivered();
            stats.isCarrying = kucingViewModelNew.isCarryingFish();
            stats.availableFish = kucingViewModelNew.getIkanViewData().size();
        }

        if (tempatMakanViewModel != null) {
            stats.fishInBowl = tempatMakanViewModel.getTempatMakanViewData().fishCount;
        } // Update with our new score system
        stats.fishDelivered = this.fishCount; // Jumlah ikan yang ditangkap (count)
        // Note: stats.fishDelivered akan menunjukkan jumlah ikan, bukan total skor
        if (ikanViewModel != null) {
            stats.availableFish = ikanViewModel.getAvailableIkanCount();
        }
        stats.isGameRunning = isGameRunning;

        // Add timer and high score information
        stats.remainingTime = this.remainingTime;
        stats.formattedTime = getFormattedTime();
        stats.isTimeUp = this.isTimeUp;

        // Get current high score from database to ensure it's up-to-date
        if (!currentPlayerName.isEmpty()) {
            stats.highScore = databaseManager.getHighScore(currentPlayerName);
            this.highScore = stats.highScore; // Update local highScore as well
        } else {
            stats.highScore = this.highScore;
        }

        stats.isNewHighScore = isNewHighScore();

        return stats;
    }

    // Score getter
    public int getScore() {
        return score;
    }

    // Fish count getter
    public int getFishCount() {
        return fishCount;
    }

    // Reset score
    public void resetScore() {
        int oldScore = this.score;
        this.score = 0;
        support.firePropertyChange("scoreChanged", oldScore, this.score);
    }

    // Reset fish count
    public void resetFishCount() {
        int oldFishCount = this.fishCount;
        this.fishCount = 0;
        support.firePropertyChange("fishCountChanged", oldFishCount, this.fishCount);
    }

    // Timer getters
    public int getRemainingTime() {
        return remainingTime;
    }

    public String getFormattedTime() {
        int minutes = remainingTime / 60;
        int seconds = remainingTime % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    public boolean isTimeUp() {
        return isTimeUp;
    }

    // High Score getters
    public int getHighScore() {
        return highScore;
    }

    public boolean isNewHighScore() {
        return score > 0 && score == highScore;
    } // Method untuk mereset semua game state

    public void resetGameState() {
        // Reset score dan fish count
        this.score = 0;
        this.fishCount = 0;

        // Reset timer
        this.remainingTime = gameTimeLimit;
        this.isTimeUp = false;

        // Reset carried fish state
        this.carriedFish = null;

        // Reset kucing state
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setHandActive(false);
            kucingViewModelNew.getModel().setHandDelivering(false);
        }

        // Fire property changes untuk update UI
        support.firePropertyChange("scoreChanged", -1, this.score);
        support.firePropertyChange("fishCountChanged", -1, this.fishCount);
        support.firePropertyChange("remainingTime", -1, this.remainingTime);
        support.firePropertyChange("gameReset", false, true);
    }

    // Reset game method
    public void resetGame() {
        resetScore();
        resetFishCount();
        remainingTime = gameTimeLimit;
        isTimeUp = false;
        support.firePropertyChange("gameReset", false, true);
        support.firePropertyChange("remainingTime", 0, remainingTime);
    }

    // Game state getters
    public boolean isGameRunning() {
        return isGameRunning;
    }

    public boolean isPaused() {
        return false; // Game tidak punya pause lagi
    }

    public int getPanelWidth() {
        return panelWidth;
    }

    public int getPanelHeight() {
        return panelHeight;
    }

    // Property change listener support
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    // Event handling methods for KucingPanel
    public void handleKeyPressed(int keyCode) {
        if (kucingViewModelNew != null) {
            // Handle movement keys using KucingViewModelNew
            switch (keyCode) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    kucingViewModelNew.setKucingVelocity(0, -5);
                    break;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    kucingViewModelNew.setKucingVelocity(0, 5);
                    break;
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    kucingViewModelNew.setKucingVelocity(-5, 0);
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    kucingViewModelNew.setKucingVelocity(5, 0);
                    break;
                // SPACE key tidak lagi digunakan untuk pause
            }
        }
    }

    public void handleKeyReleased() {
        if (kucingViewModelNew != null) {
            // Stop movement when key is released
            kucingViewModelNew.setKucingVelocity(0, 0);
        }
    }

    public void handleMousePressed(int x, int y) {
        if (kucingViewModelNew != null && carriedFish == null) { // Hanya bisa beraksi jika tidak sedang membawa ikan
            // Cek apakah pengguna mengklik ikan
            if (ikanViewModel != null) {
                model.Ikan clickedFish = ikanViewModel.findClickedFish(x, y);
                if (clickedFish != null) {
                    // Ikan diklik, mulai proses pengantaran
                    this.carriedFish = clickedFish;
                    ikanViewModel.setIkanBeingCarried(this.carriedFish, true); // Beri tahu IkanViewModel agar ikan ini
                                                                               // tidak bergerak sendiri

                    // Aktifkan tangan dan arahkan ke ikan dulu
                    kucingViewModelNew.setHandActive(true);
                    kucingViewModelNew.setHandTarget(clickedFish.getCenterX(), clickedFish.getCenterY());

                    // Dapatkan posisi tempat makan dan suruh kucing mengantar ke sana
                    if (tempatMakanViewModel != null) {
                        model.TempatMakan tmModel = tempatMakanViewModel.getModel();
                        if (tmModel != null) {
                            // Gunakan metode di model Kucing untuk memulai animasi pengantaran
                            kucingViewModelNew.getModel().startDeliveryToFoodBowl(tmModel.getCenterX(),
                                    tmModel.getCenterY());
                        }
                    }
                    return; // Aksi selesai untuk klik ini
                }
            }

            // Jika tidak ada ikan yang diklik, aktifkan tangan di posisi mouse (perilaku
            // normal)
            kucingViewModelNew.setHandActive(true);
            kucingViewModelNew.setHandTarget(x, y);
        }
    }

    public void handleMouseReleased() {
        if (kucingViewModelNew != null && carriedFish == null) { // Hanya deactivate jika tidak sedang membawa ikan
            // Deactivate hand
            kucingViewModelNew.setHandActive(false);
        }
    }

    public void handleMouseMoved(int x, int y) {
        if (kucingViewModelNew != null) {
            // Update hand target if hand is active
            if (kucingViewModelNew.isHandActive()) {
                kucingViewModelNew.setHandTarget(x, y);
            }
        }

        // Also check hover over tempat makan using KucingViewModelNew
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setTempatMakanHover(x, y);
        }
    }

    private void setupGameTimer() { // Timer untuk countdown game (update setiap 1 detik)
        gameTimer = new Timer(1000, e -> {
            if (isGameRunning) {
                remainingTime--;

                // Notify UI untuk update display
                support.firePropertyChange("remainingTime", remainingTime + 1, remainingTime);

                // Check game over
                if (remainingTime <= 0) {
                    gameOver();
                }
            }
        });
    }

    private void gameOver() {
        isTimeUp = true;
        isGameRunning = false;

        // Save score regardless of whether it's a high score
        saveHighScore();

        // Stop all timers
        stopGame();

        // Notify UI game over and that game has ended
        support.firePropertyChange("gameOver", false, true);
        support.firePropertyChange("gameEnded", false, true);
    } // Player name management

    public void setCurrentPlayerName(String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            this.currentPlayerName = playerName.trim();

            // Add player to database if doesn't exist
            if (!databaseManager.playerExists(this.currentPlayerName)) {
                databaseManager.addPlayer(this.currentPlayerName);
            }

            // Reset game state untuk player baru
            resetGameForNewPlayer();

            // Load player's high score
            loadHighScore();

            support.firePropertyChange("currentPlayerName", null, this.currentPlayerName);
        }
    }

    public String getCurrentPlayerName() {
        return currentPlayerName;
    }

    private void loadHighScore() {
        if (!currentPlayerName.isEmpty()) {
            highScore = databaseManager.getHighScore(currentPlayerName);
        }
    }

    private void saveHighScore() {
        if (!currentPlayerName.isEmpty()) {
            // Record the game score and fish count (this will update high scores if
            // necessary)
            boolean recorded = databaseManager.recordGameScore(currentPlayerName, score, fishCount);

            if (recorded) {
                // Reload the high score to get the latest value
                int newHighScore = databaseManager.getHighScore(currentPlayerName);
                highScore = newHighScore;
            }
        }
    } // Inner class for game statistics

    public static class GameStats {
        public int fishDelivered = 0;
        public int fishInBowl = 0;
        public int availableFish = 0;
        public boolean isCarrying = false;
        public boolean isGameRunning = false;
        public int remainingTime = 60;
        public String formattedTime = "01:00";
        public boolean isTimeUp = false;
        public int highScore = 0;
        public boolean isNewHighScore = false;
    } // Reset game state untuk player baru

    private void resetGameForNewPlayer() {
        // Stop game jika sedang berjalan
        if (isGameRunning) {
            stopGame();
        }

        // Gunakan resetGameState() untuk konsistensi
        resetGameState();
    }
}