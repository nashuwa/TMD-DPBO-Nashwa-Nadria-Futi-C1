package viewmodel;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.awt.event.KeyEvent;
import javax.swing.Timer;
import model.DatabaseManager;

/**
 * GameViewModel - Pengendali utama logika permainan kucing menangkap ikan
 * 
 * ALUR PERMAINAN:
 * 1. Pemain memilih nama dan memulai game dengan timer 60 detik
 * 2. Kucing digerakkan dengan WASD/Arrow keys untuk menangkap ikan
 * 3. Klik ikan untuk membuat kucing mengantarkannya ke tempat makan
 * 4. Setiap ikan memberikan poin berbeda (10-30) berdasarkan jenisnya
 * 5. Game berakhir saat waktu habis, skor disimpan ke database
 * 6. Tekan SPACE untuk pause/resume kapan saja
 * 
 * FUNGSI UTAMA:
 * - Mengatur koordinasi antar ViewModel (Kucing, Ikan, TempatMakan)
 * - Mengelola timer game dan hand tracking untuk animasi
 * - Menangani input keyboard/mouse dan meneruskannya ke ViewModel yang tepat
 * - Mengelola sistem scoring dan high score dengan database
 * - Mengatur pause/resume dan reset game state
 */
public class GameViewModel { // Sistem PropertyChangeSupport untuk komunikasi dengan View
    private PropertyChangeSupport support;

    // ViewModel untuk mengelola kucing dan interaksinya
    private KucingViewModelNew kucingViewModelNew;
    // ViewModel untuk mengelola tempat makan kucing
    private TempatMakanViewModel tempatMakanViewModel;
    // ViewModel untuk mengelola ikan-ikan dalam game
    private IkanViewModel ikanViewModel;

    // Timer untuk update hand tracking kucing (30 FPS)
    private Timer handTrackingTimer;
    // Timer untuk countdown waktu permainan (1 detik)
    private Timer gameTimer;

    // Konfigurasi ukuran panel game
    private int panelWidth = 800;
    private int panelHeight = 600;

    // Status permainan utama
    private boolean isGameRunning = false;
    private boolean isPaused = false;
    private boolean spaceKeyPressed = false; // Mencegah pause berulang saat spasi ditahan

    // Sistem scoring permainan
    private int score = 0; // Total poin dari nilai ikan yang ditangkap
    private int fishCount = 0; // Jumlah ikan yang berhasil ditangkap // Konfigurasi timer permainan
    private int gameTimeLimit = 60; // Durasi permainan dalam detik (1 menit)
    private int remainingTime = gameTimeLimit; // Waktu tersisa
    private boolean isTimeUp = false; // Flag apakah waktu sudah habis
    private int highScore = 0; // Skor tertinggi pemain saat ini

    // Manajemen pemain dan database
    private String currentPlayerName = ""; // Nama pemain yang sedang bermain
    private DatabaseManager databaseManager; // Koneksi ke database untuk menyimpan skor

    // Constructor utama - inisialisasi semua komponen game
    public GameViewModel() {
        // Setup sistem notifikasi perubahan
        support = new PropertyChangeSupport(this);
        // Inisialisasi database manager
        databaseManager = DatabaseManager.getInstance();

        // Inisialisasi semua ViewModel dan setup koneksi antar mereka
        initializeViewModels();
        // Setup timer untuk hand tracking animasi
        setupHandTracking();
        // Setup timer untuk countdown permainan
        setupGameTimer();
        // Load skor tertinggi dari database
        loadHighScore();
    } // Setup timer untuk hand tracking animasi kucing

    private void setupHandTracking() {
        // Timer berjalan 30 kali per detik untuk animasi yang smooth
        handTrackingTimer = new Timer(1000 / 30, e -> updateHandTracking());
    }

    // State untuk proses pengantaran ikan ke tempat makan
    private model.Ikan carriedFish = null;

    // Update hand tracking dan proses pengantaran ikan setiap frame
    private void updateHandTracking() {
        // Hanya update jika game sedang berjalan dan tidak di-pause
        if (!isGameRunning || isPaused) {
            return;
        }

        // Proses pengantaran ikan yang sedang dibawa kucing
        updateFishDelivery();
    } // Proses pengantaran ikan ke tempat makan

    private void updateFishDelivery() {
        // Pastikan ada ikan yang sedang dibawa dan semua komponen tersedia
        if (carriedFish != null && kucingViewModelNew != null && tempatMakanViewModel != null) {
            // 1. Buat ikan mengikuti posisi tangan kucing
            KucingViewModelNew.KucingViewData kucingData = kucingViewModelNew.getKucingViewData();
            if (kucingData != null) {
                // Posisikan ikan di tengah tangan kucing
                carriedFish.setPosX(kucingData.handX - carriedFish.getWidth() / 2);
                carriedFish.setPosY(kucingData.handY - carriedFish.getHeight() / 2);
            }

            // 2. Cek apakah tangan kucing sudah sampai di tempat makan
            model.TempatMakan tmModel = tempatMakanViewModel.getModel();
            if (tmModel != null) {
                // Hitung jarak antara tangan dan pusat tempat makan
                int handX = kucingViewModelNew.getHandX();
                int handY = kucingViewModelNew.getHandY();
                double distance = Math
                        .sqrt(Math.pow(handX - tmModel.getCenterX(), 2) + Math.pow(handY - tmModel.getCenterY(), 2));

                // Jika jarak cukup dekat, selesaikan pengantaran
                if (distance < 50) { // Threshold 50 pixel untuk deteksi
                    handleFishDelivered(carriedFish);
                    // Reset semua state pengantaran
                    this.carriedFish = null;
                    kucingViewModelNew.getModel().setHandDelivering(false);
                    kucingViewModelNew.setHandActive(false); // Tangan akan kembali ke kucing
                }
            }
        }
    } // Menangani ikan yang berhasil diantarkan ke tempat makan

    private void handleFishDelivered(model.Ikan fish) {
        if (fish != null) {
            // Hapus ikan dari daftar ikan yang tersedia
            ikanViewModel.removeIkan(fish);
            // Tambahkan ikan ke tempat makan
            if (tempatMakanViewModel != null) {
                tempatMakanViewModel.addFish();
            }

            // Hitung dan tambahkan skor berdasarkan jenis ikan
            int oldScore = this.score;
            int fishScore = fish.getScore(); // Dapatkan nilai poin ikan (10, 20, atau 30)
            this.score += fishScore;
            // Beritahu UI bahwa skor berubah
            support.firePropertyChange("scoreChanged", oldScore, this.score);

            // Tambah counter jumlah ikan yang ditangkap
            int oldFishCount = this.fishCount;
            this.fishCount += 1;
            // Beritahu UI bahwa jumlah ikan berubah
            support.firePropertyChange("fishCountChanged", oldFishCount, this.fishCount);

            // Update high score secara real-time tanpa pop-up
            if (this.score > this.highScore) {
                this.highScore = this.score;
            }
        }
    } // Inisialisasi semua ViewModel dan setup komunikasi antar mereka

    private void initializeViewModels() {
        // Inisialisasi ViewModel kucing dengan semua logika game
        kucingViewModelNew = new KucingViewModelNew();

        // Inisialisasi ViewModel ikan
        ikanViewModel = new IkanViewModel();

        // TempatMakanViewModel akan dibuat setelah setPanelDimensions dipanggil
        // karena memerlukan koordinat yang tepat

        // Setup forwarding event dari KucingViewModelNew ke GameViewModel
        kucingViewModelNew.addPropertyChangeListener(evt -> {
            // Teruskan semua event kucing ke listener GameViewModel
            support.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());

            // Handle event khusus untuk pembuatan tempat makan
            if ("tempatMakanCreated".equals(evt.getPropertyName())) {
                // Buat TempatMakanViewModel ketika tempat makan sudah dibuat
                if (evt.getNewValue() instanceof model.TempatMakan) {
                    tempatMakanViewModel = new TempatMakanViewModel((model.TempatMakan) evt.getNewValue());
                    tempatMakanViewModel.setVisible(false); // Mulai dengan tersembunyi

                    // Setup forwarding event dari TempatMakanViewModel
                    tempatMakanViewModel.addPropertyChangeListener(tempatMakanEvt -> {
                        support.firePropertyChange(tempatMakanEvt.getPropertyName(),
                                tempatMakanEvt.getOldValue(),
                                tempatMakanEvt.getNewValue());
                    });
                }
            }
        });

        // Setup forwarding event dari KucingViewModelNew (backup listener)
        if (kucingViewModelNew != null) {
            kucingViewModelNew.addPropertyChangeListener(evt -> {
                support.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
            });
        }

        // Setup forwarding event dari IkanViewModel
        if (ikanViewModel != null) {
            ikanViewModel.addPropertyChangeListener(evt -> {
                support.firePropertyChange(evt.getPropertyName(), evt.getOldValue(), evt.getNewValue());
                // Scoring untuk delivery ikan sudah dihandle di handleFishDelivered()
            });
        }
    } // Method konfigurasi game

    // Mengatur ukuran panel game dan update semua ViewModel terkait
    public void setPanelDimensions(int width, int height) {
        int oldWidth = this.panelWidth;
        int oldHeight = this.panelHeight;

        // Update ukuran panel lokal
        this.panelWidth = width;
        this.panelHeight = height;

        // Update ukuran panel di KucingViewModel
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setPanelDimensions(width, height);
        }

        // Beritahu UI bahwa dimensi panel berubah
        support.firePropertyChange("panelDimensions",
                new java.awt.Dimension(oldWidth, oldHeight),
                new java.awt.Dimension(width, height));
    }

    // Memulai permainan baru
    public void startGame() {
        if (!isGameRunning) {
            // SELALU reset semua game state sebelum memulai game baru
            resetGameState();

            // Set flag game sedang berjalan
            isGameRunning = true;

            // Mulai loop game dan animasi kucing
            if (kucingViewModelNew != null) {
                kucingViewModelNew.startGameLoop();
                kucingViewModelNew.startAnimation();
            }
            // Mulai pergerakan ikan
            if (ikanViewModel != null) {
                ikanViewModel.startMovement();
            }

            // Mulai timer hand tracking jika belum berjalan
            if (handTrackingTimer != null && !handTrackingTimer.isRunning()) {
                handTrackingTimer.start();
            }

            // Mulai timer countdown game
            if (gameTimer != null && !gameTimer.isRunning()) {
                remainingTime = gameTimeLimit; // Reset waktu tersisa
                gameTimer.start();
            }

            // Beritahu UI bahwa game sudah dimulai
            support.firePropertyChange("gameRunning", false, true);
        }
    } // Menghentikan permainan

    public void stopGame() {
        if (isGameRunning) {
            // Set flag game tidak berjalan
            isGameRunning = false;

            // Hentikan timer hand tracking
            if (handTrackingTimer != null && handTrackingTimer.isRunning()) {
                handTrackingTimer.stop();
            }

            // Hentikan timer countdown game
            if (gameTimer != null && gameTimer.isRunning()) {
                gameTimer.stop();
            }

            // Beritahu UI bahwa game sudah dihentikan
            support.firePropertyChange("gameRunning", true, false);
        }
    }

    // Method delegasi untuk mengontrol ViewModel anak

    // Mengatur kecepatan pergerakan kucing
    public void setKucingVelocity(int vx, int vy) {
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setKucingVelocity(vx, vy);
        }
    }

    // Mengaktifkan/menonaktifkan tangan kucing
    public void setHandActive(boolean active) {
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setHandActive(active);
        }
    }

    // Mengatur target posisi tangan kucing
    public void setHandTarget(int x, int y) {
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setHandTarget(x, y);
        }
    }

    // Mengatur hover effect di tempat makan
    public void setTempatMakanHover(int mouseX, int mouseY) {
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setTempatMakanHover(mouseX, mouseY);
        }
    } // Method akses data untuk Views

    // Getter untuk ViewModel kucing (versi utama)
    public KucingViewModelNew getKucingViewModel() {
        return kucingViewModelNew;
    }

    // Getter untuk ViewModel kucing (versi baru)
    public KucingViewModelNew getKucingViewModelNew() {
        return kucingViewModelNew;
    }

    // Getter untuk backward compatibility - mengembalikan KucingViewModelNew
    // sebagai main
    public KucingViewModelNew getKucingViewModel2() {
        return kucingViewModelNew;
    }

    // Getter untuk ViewModel tempat makan
    public TempatMakanViewModel getTempatMakanViewModel() {
        return tempatMakanViewModel;
    }

    // Getter untuk ViewModel ikan
    public IkanViewModel getIkanViewModel() {
        return ikanViewModel;
    } // Mengambil statistik game untuk ditampilkan di panel status

    public GameStats getGameStats() {
        GameStats stats = new GameStats();

        // Ambil data dari KucingViewModel jika tersedia
        if (kucingViewModelNew != null) {
            stats.fishDelivered = kucingViewModelNew.getFishDelivered();
            stats.isCarrying = kucingViewModelNew.isCarryingFish();
            stats.availableFish = kucingViewModelNew.getIkanViewData().size();
        }

        // Ambil data dari TempatMakanViewModel jika tersedia
        if (tempatMakanViewModel != null) {
            stats.fishInBowl = tempatMakanViewModel.getTempatMakanViewData().fishCount;
        }

        // Update dengan sistem scoring yang baru
        stats.fishDelivered = this.fishCount; // Jumlah ikan yang ditangkap (bukan total skor)

        // Ambil jumlah ikan yang tersedia dari IkanViewModel
        if (ikanViewModel != null) {
            stats.availableFish = ikanViewModel.getAvailableIkanCount();
        }

        // Update status game
        stats.isGameRunning = isGameRunning;

        // Tambahkan informasi timer dan high score
        stats.remainingTime = this.remainingTime;
        stats.formattedTime = getFormattedTime();
        stats.isTimeUp = this.isTimeUp;

        // Ambil high score terkini dari database untuk memastikan akurasi
        if (!currentPlayerName.isEmpty()) {
            stats.highScore = databaseManager.getHighScore(currentPlayerName);
            this.highScore = stats.highScore; // Update high score lokal juga
        } else {
            stats.highScore = this.highScore;
        }

        // Cek apakah ini adalah high score baru
        stats.isNewHighScore = isNewHighScore();

        return stats;
    } // Method getter untuk UI

    // Mendapatkan skor saat ini
    public int getScore() {
        return score;
    }

    // Mendapatkan jumlah ikan yang ditangkap
    public int getFishCount() {
        return fishCount;
    }

    // Mendapatkan waktu tersisa dalam detik
    public int getRemainingTime() {
        return remainingTime;
    }

    // Mendapatkan high score saat ini
    public int getHighScore() {
        return highScore;
    }

    // Mendapatkan waktu tersisa dalam format MM:SS
    public String getFormattedTime() {
        int minutes = remainingTime / 60;
        int seconds = remainingTime % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // Mengecek apakah waktu sudah habis
    public boolean isTimeUp() {
        return isTimeUp;
    }

    // Mengecek apakah skor saat ini adalah high score baru
    public boolean isNewHighScore() {
        return score > 0 && score == highScore;
    }

    // Method reset untuk permainan

    // Reset skor ke nol
    public void resetScore() {
        int oldScore = this.score;
        this.score = 0;
        support.firePropertyChange("scoreChanged", oldScore, this.score);
    }

    // Reset jumlah ikan yang ditangkap ke nol
    public void resetFishCount() {
        int oldFishCount = this.fishCount;
        this.fishCount = 0;
        support.firePropertyChange("fishCountChanged", oldFishCount, this.fishCount);
    } // Method untuk mereset semua state permainan ke kondisi awal

    public void resetGameState() {
        // Reset skor dan jumlah ikan ke nol
        this.score = 0;
        this.fishCount = 0;

        // Reset timer ke waktu awal
        this.remainingTime = gameTimeLimit;
        this.isTimeUp = false;

        // Reset state ikan yang sedang dibawa
        this.carriedFish = null;

        // Reset state kucing ke kondisi awal
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setHandActive(false);
            kucingViewModelNew.getModel().setHandDelivering(false);
        }

        // Beritahu UI tentang perubahan state
        support.firePropertyChange("scoreChanged", -1, this.score);
        support.firePropertyChange("fishCountChanged", -1, this.fishCount);
        support.firePropertyChange("remainingTime", -1, this.remainingTime);
        support.firePropertyChange("gameReset", false, true);
    }

    // Method reset game (alternative untuk backward compatibility)
    public void resetGame() {
        // Reset semua komponen score dan timer
        resetScore();
        resetFishCount();
        remainingTime = gameTimeLimit;
        isTimeUp = false;

        // Beritahu UI tentang reset
        support.firePropertyChange("gameReset", false, true);
        support.firePropertyChange("remainingTime", 0, remainingTime);
    }

    // Method getter untuk state permainan

    // Mengecek apakah game sedang berjalan
    public boolean isGameRunning() {
        return isGameRunning;
    }

    // Mengecek apakah game sedang di-pause
    public boolean isPaused() {
        return isPaused;
    }

    // Mendapatkan lebar panel game
    public int getPanelWidth() {
        return panelWidth;
    } // Mendapatkan tinggi panel game

    public int getPanelHeight() {
        return panelHeight;
    }

    // Support untuk PropertyChange listener // Menambahkan listener untuk
    // mendengarkan perubahan property
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    // Menghapus listener yang tidak diperlukan lagi
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    // Method penanganan event input dari UI

    // Menangani tombol keyboard yang ditekan
    public void handleKeyPressed(int keyCode) {
        // Handle pause/resume dengan tombol SPACE - hanya sekali per key press
        if (keyCode == KeyEvent.VK_SPACE) {
            if (!spaceKeyPressed) { // Hanya toggle jika space belum ditekan sebelumnya
                spaceKeyPressed = true;
                togglePause(); // Toggle status pause/resume
            }
            return; // Keluar dari method setelah handle pause
        }

        // Jika game sedang pause, jangan proses input movement
        if (isPaused) {
            return;
        }

        // Proses input pergerakan kucing menggunakan KucingViewModelNew
        if (kucingViewModelNew != null) {
            switch (keyCode) {
                case KeyEvent.VK_W:
                case KeyEvent.VK_UP:
                    // Gerak ke atas
                    kucingViewModelNew.setKucingVelocity(0, -5);
                    break;
                case KeyEvent.VK_S:
                case KeyEvent.VK_DOWN:
                    // Gerak ke bawah
                    kucingViewModelNew.setKucingVelocity(0, 5);
                    break;
                case KeyEvent.VK_A:
                case KeyEvent.VK_LEFT:
                    // Gerak ke kiri
                    kucingViewModelNew.setKucingVelocity(-5, 0);
                    break;
                case KeyEvent.VK_D:
                case KeyEvent.VK_RIGHT:
                    // Gerak ke kanan
                    kucingViewModelNew.setKucingVelocity(5, 0);
                    break;
            }
        }
    }

    // Menangani tombol keyboard yang dilepas
    public void handleKeyReleased(int keyCode) {
        // Reset flag space key ketika tombol space dilepas
        if (keyCode == KeyEvent.VK_SPACE) {
            spaceKeyPressed = false;
            return;
        }

        // Hentikan pergerakan kucing ketika tombol movement dilepas
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setKucingVelocity(0, 0);
        }
    } // Menangani klik mouse pada game panel

    public void handleMousePressed(int x, int y) {
        // Hanya bisa beraksi jika kucing tidak sedang membawa ikan
        if (kucingViewModelNew != null && carriedFish == null) {
            // Cek apakah pengguna mengklik ikan yang tersedia
            if (ikanViewModel != null) {
                model.Ikan clickedFish = ikanViewModel.findClickedFish(x, y);
                if (clickedFish != null) {
                    // Ikan diklik, mulai proses pengantaran
                    this.carriedFish = clickedFish;
                    // Beri tahu IkanViewModel agar ikan ini tidak bergerak sendiri
                    ikanViewModel.setIkanBeingCarried(this.carriedFish, true);

                    // Aktifkan tangan kucing dan arahkan ke posisi ikan
                    kucingViewModelNew.setHandActive(true);
                    kucingViewModelNew.setHandTarget(clickedFish.getCenterX(), clickedFish.getCenterY());

                    // Dapatkan posisi tempat makan dan suruh kucing mengantar ke sana
                    if (tempatMakanViewModel != null) {
                        model.TempatMakan tmModel = tempatMakanViewModel.getModel();
                        if (tmModel != null) {
                            // Mulai animasi pengantaran ke tempat makan
                            kucingViewModelNew.getModel().startDeliveryToFoodBowl(tmModel.getCenterX(),
                                    tmModel.getCenterY());
                        }
                    }
                    return; // Selesai menangani klik pada ikan
                }
            }

            // Jika tidak ada ikan yang diklik, aktifkan tangan di posisi mouse
            // (perilaku normal untuk interaksi umum)
            kucingViewModelNew.setHandActive(true);
            kucingViewModelNew.setHandTarget(x, y);
        }
    }

    // Menangani mouse button yang dilepas
    public void handleMouseReleased() {
        // Hanya deactivate tangan jika tidak sedang membawa ikan
        if (kucingViewModelNew != null && carriedFish == null) {
            // Nonaktifkan tangan kucing
            kucingViewModelNew.setHandActive(false);
        }
    }

    // Menangani pergerakan mouse di atas game panel
    public void handleMouseMoved(int x, int y) {
        if (kucingViewModelNew != null) {
            // Update target tangan jika tangan sedang aktif
            if (kucingViewModelNew.isHandActive()) {
                kucingViewModelNew.setHandTarget(x, y);
            }
        } // Cek hover effect di tempat makan menggunakan KucingViewModelNew
        if (kucingViewModelNew != null) {
            kucingViewModelNew.setTempatMakanHover(x, y);
        }
    }

    // Setup timer untuk countdown permainan
    private void setupGameTimer() {// Timer untuk countdown game (update setiap 1 detik) // Timer berjalan setiap 1
                                   // detik untuk mengurangi waktu tersisa
        gameTimer = new Timer(1000, e -> {
            // Hanya countdown jika game berjalan dan tidak di-pause
            if (isGameRunning && !isPaused) {
                remainingTime--;

                // Beritahu UI untuk update tampilan waktu
                support.firePropertyChange("remainingTime", remainingTime + 1, remainingTime);

                // Cek apakah waktu sudah habis
                if (remainingTime <= 0) {
                    gameOver(); // Akhiri permainan
                }
            }
        });
    }

    // Menangani akhir permainan ketika waktu habis
    private void gameOver() {
        // Set flag bahwa waktu sudah habis
        isTimeUp = true;
        isGameRunning = false;

        // Simpan skor ke database
        saveHighScore();

        // Hentikan semua timer
        stopGame();

        // Beritahu UI bahwa game berakhir
        support.firePropertyChange("gameOver", false, true);
        support.firePropertyChange("gameEnded", false, true);
    } // Manajemen nama pemain dan database

    // Mengatur nama pemain yang sedang bermain
    public void setCurrentPlayerName(String playerName) {
        if (playerName != null && !playerName.trim().isEmpty()) {
            this.currentPlayerName = playerName.trim();

            // Tambahkan pemain ke database jika belum ada
            if (!databaseManager.playerExists(this.currentPlayerName)) {
                databaseManager.addPlayer(this.currentPlayerName);
            }

            // Reset game state untuk pemain baru
            resetGameForNewPlayer();

            // Load high score pemain dari database
            loadHighScore();

            // Beritahu UI bahwa nama pemain berubah
            support.firePropertyChange("currentPlayerName", null, this.currentPlayerName);
        }
    }

    // Mendapatkan nama pemain saat ini
    public String getCurrentPlayerName() {
        return currentPlayerName;
    }

    // Load high score pemain dari database
    private void loadHighScore() {
        if (!currentPlayerName.isEmpty()) {
            highScore = databaseManager.getHighScore(currentPlayerName);
        }
    }

    // Simpan skor permainan ke database
    private void saveHighScore() {
        if (!currentPlayerName.isEmpty()) {
            // Catat skor dan jumlah ikan ke database (akan update high score jika perlu)
            boolean recorded = databaseManager.recordGameScore(currentPlayerName, score, fishCount);
            if (recorded) {
                // Reload high score untuk mendapatkan nilai terbaru
                int newHighScore = databaseManager.getHighScore(currentPlayerName);
                highScore = newHighScore;

                // Beritahu UI untuk refresh leaderboard
                support.firePropertyChange("highScore", 0, newHighScore);
            }
        }
    }

    // Inner class untuk statistik permainan yang akan ditampilkan di UI
    public static class GameStats {
        public int fishDelivered = 0; // Jumlah ikan yang berhasil diantarkan
        public int fishInBowl = 0; // Jumlah ikan di tempat makan
        public int availableFish = 0; // Jumlah ikan yang tersedia untuk ditangkap
        public boolean isCarrying = false; // Apakah kucing sedang membawa ikan
        public boolean isGameRunning = false; // Status apakah game sedang berjalan
        public int remainingTime = 60; // Waktu tersisa dalam detik
        public String formattedTime = "01:00"; // Waktu tersisa dalam format MM:SS
        public boolean isTimeUp = false; // Apakah waktu sudah habis
        public int highScore = 0; // Skor tertinggi pemain
        public boolean isNewHighScore = false; // Apakah ini adalah high score baru
    }

    // Reset game state untuk pemain baru
    private void resetGameForNewPlayer() {
        // Hentikan game jika sedang berjalan
        if (isGameRunning) {
            stopGame();
        } // Gunakan resetGameState() untuk konsistensi
        resetGameState();
    }

    // Method untuk toggle pause/resume permainan
    public void togglePause() {
        // Tidak bisa pause jika game tidak sedang berjalan
        if (!isGameRunning) {
            return;
        }

        // Toggle status pause
        isPaused = !isPaused;

        if (isPaused) {
            // PAUSE GAME - hentikan semua timer dan aktivitas
            if (gameTimer != null && gameTimer.isRunning()) {
                gameTimer.stop();
            }
            if (handTrackingTimer != null && handTrackingTimer.isRunning()) {
                handTrackingTimer.stop();
            }
            // Hentikan pergerakan ikan
            if (ikanViewModel != null) {
                ikanViewModel.stopMovement();
            }

            // Beritahu UI untuk menampilkan overlay pause
            support.firePropertyChange("gamePaused", false, true);
        } else {
            // RESUME GAME - jalankan kembali semua timer dan aktivitas
            if (gameTimer != null && !gameTimer.isRunning()) {
                gameTimer.start();
            }
            if (handTrackingTimer != null && !handTrackingTimer.isRunning()) {
                handTrackingTimer.start();
            }
            // Lanjutkan pergerakan ikan
            if (ikanViewModel != null) {
                ikanViewModel.startMovement();
            }

            // Beritahu UI untuk menyembunyikan overlay pause
            support.firePropertyChange("gameResumed", false, true);
        }
    }
}