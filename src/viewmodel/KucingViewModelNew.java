package viewmodel;

// Import semua class yang diperlukan untuk viewmodel kucing
import model.Kucing;
import javax.swing.ImageIcon;
import javax.swing.Timer;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import model.Ikan;
import model.TempatMakan;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * KucingViewModelNew - ViewModel Unified untuk Kucing dengan Logika Game Lengkap
=============================================================================
 * ALUR PERMAINAN LENGKAP:
 * =============================================================================
 * 
 * 1. INISIALISASI GAME:
 *    - Load semua asset gambar (kucing berbagai arah, ikan, tempat makan, tangan)
 *    - Spawn kucing di zona hijau (area permainan) dengan posisi awal yang aman
 *    - Buat tempat makan di sisi kanan zona hijau sebagai target delivery
 *    - Spawn ikan-ikan awal (10-20 ikan) secara random di zona hijau
 *    - Setup timer animasi 60 FPS untuk smooth gameplay
 * 
 * 2. KONTROL KUCING:
 *    - Player menggerakkan kucing dengan WASD/Arrow keys di zona hijau saja
 *    - Kucing tidak bisa keluar dari zona hijau (boundary detection)
 *    - Gambar kucing berubah sesuai arah pergerakan (kiri/kanan/atas/bawah)
 *    - Sistem velocity-based movement untuk pergerakan yang smooth
 * 
 * 3. SISTEM TANGAN KUCING:
 *    - Player klik mouse untuk mengaktifkan tangan kucing
 *    - Tangan bergerak ke posisi mouse dengan animasi smooth
 *    - Saat tangan aktif, kucing tampil dalam mode "mengambil" (catImageX)
 *    - Tangan bisa menangkap ikan yang berada dalam jangkauan collision
 * 
 * 4. MENANGKAP IKAN:
 *    - Collision detection antara tangan kucing dan ikan menggunakan AABB
 *    - Ikan yang tertangkap akan "menempel" pada kucing (carried state)
 *    - Hanya bisa membawa satu ikan dalam satu waktu
 *    - Ikan yang dibawa mengikuti posisi kucing dengan smooth animation
 * 
 * 5. DELIVERY SYSTEM (SISTEM PENGANTARAN):
 *    - Setelah menangkap ikan, kucing otomatis masuk mode "delivery"
 *    - Tangan kucing bergerak otomatis ke arah tempat makan
 *    - Player tidak bisa kontrol tangan manual selama delivery berlangsung
 *    - Ikan mengikuti pergerakan tangan menuju tempat makan
 * 
 * 6. SCORING & RESPAWN:
 *    - Saat ikan sampai di tempat makan (jarak < 30 pixel), ikan "dimakan"
 *    - Score bertambah sesuai jenis ikan (ikan1=10pts, ikan2=20pts, ikan3=30pts)
 *    - Ikan yang sudah dimakan hilang dari game
 *    - Sistem otomatis spawn ikan baru untuk mengganti yang sudah dimakan
 *    - Jumlah ikan di game selalu dijaga antara 10-20 ikan
 * 
 * 7. GAME LOOP CONTINUOUS:
 *    - Game berjalan terus dengan timer 60 FPS
 *    - Spawn ikan baru setiap 1 detik jika jumlah ikan < minimum
 *    - Update animasi tangan, pergerakan kucing, dan collision detection
 *    - Real-time property change notification ke UI layer
 * 
 */
public class KucingViewModelNew implements ActionListener { // Konstanta untuk zona kucing - ZONA HIJAU (area permainan kucing)
    private static final int ZONE_TOP_LIMIT = 200; // Batas atas zona hijau - sedikit di bawah jembatan
    private static final int ZONE_BOTTOM_LIMIT = 360; // Batas bawah zona hijau - sebelum area air

    // Object model kucing utama
    private Kucing kucing;
    // Support untuk property change listener (observer pattern)
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    // Timer untuk animasi kucing
    private Timer animationTimer;
    // Timer untuk game loop utama
    private Timer gameLoopTimer;

    // Gambar-gambar untuk berbagai arah dan state kucing
    private Image catImageRight; // Gambar kucing menghadap kanan
    private Image catImageLeft; // Gambar kucing menghadap kiri
    private Image catImageUp; // Gambar kucing menghadap atas
    private Image catImageDown; // Gambar kucing menghadap bawah
    private Image catImageX; // Gambar kucing saat mengambil ikan
    private Image catImageHand; // Gambar tangan kucing

    // Dimensi panel game
    private int panelWidth = 800; // Lebar panel default
    private int panelHeight = 600; // Tinggi panel default (full height) // State untuk animasi tangan
    private int handTargetX = 0; // Target posisi X tangan
    private int handTargetY = 0; // Target posisi Y tangan

    // Manajemen ikan dari KucingViewModel
    private List<Ikan> listIkan; // Daftar semua ikan di game
    private Image[] fishImages; // Array gambar-gambar ikan
    private Image tempatMakanImage; // Gambar tempat makan
    private Random random; // Generator angka random
    private final int MAX_FISH = 20; // Maksimal jumlah ikan
    private final int MIN_FISH = 10; // Minimal jumlah ikan
    private int fishSpawnTimer = 0; // Timer untuk spawn ikan baru
    private final int FISH_SPAWN_INTERVAL = 60; // Interval spawn ikan (60 FPS, jadi 1 detik)
    private TempatMakan tempatMakan; // Object tempat makan
    private Ikan carriedFish = null; // Ikan yang sedang dibawa kucing
    private boolean isCarryingFish = false; // Status apakah kucing sedang membawa ikan
    private int fishDelivered = 0; // Jumlah ikan yang sudah diantarkan

    // Constructor - inisialisasi semua komponen
    public KucingViewModelNew() {
        loadCatImages(); // Load semua gambar kucing
        initializeKucing(); // Inisialisasi object kucing
        setupAnimationTimer(); // Setup timer animasi
        gameLoopTimer = new Timer(1000 / 60, this); // Timer game loop 60 FPS

        // Inisialisasi manajemen ikan
        listIkan = new ArrayList<>();
        random = new Random();

        // Spawn ikan awal saat game dimulai
        spawnInitialFish();
    } // Method untuk memuat semua gambar kucing dan asset lainnya

    private void loadCatImages() {
        try {
            // Load gambar kucing untuk berbagai arah
            catImageRight = new ImageIcon(getClass().getResource("/assets/kucingkanan.png")).getImage();
            catImageLeft = new ImageIcon(getClass().getResource("/assets/kucingkiri.png")).getImage();
            catImageUp = new ImageIcon(getClass().getResource("/assets/kucingbelakang.png")).getImage();
            catImageDown = new ImageIcon(getClass().getResource("/assets/kucingdepan.png")).getImage();
            catImageX = new ImageIcon(getClass().getResource("/assets/kucingambil.png")).getImage();
            catImageHand = new ImageIcon(getClass().getResource("/assets/tangan.png")).getImage();

            // Load gambar-gambar ikan (3 jenis)
            fishImages = new Image[3];
            fishImages[0] = new ImageIcon(getClass().getResource("/assets/ikan1.png")).getImage();
            fishImages[1] = new ImageIcon(getClass().getResource("/assets/ikan2.png")).getImage();
            fishImages[2] = new ImageIcon(getClass().getResource("/assets/ikan3.png")).getImage();

            // Load gambar tempat makan
            tempatMakanImage = new ImageIcon(getClass().getResource("/assets/tempatmakan.png")).getImage();
        } catch (Exception e) {
            // Tangani error jika gambar gagal dimuat
            System.err.println("Failed to load cat images: " + e.getMessage());
        }
    } // Method untuk memulai game loop

    public void startGameLoop() {
        gameLoopTimer.start(); // Mulai timer game loop
    }

    // Method untuk inisialisasi object kucing
    private void initializeKucing() {
        // Spawn kucing di zona hijau (area bawah - area permainan kucing)
        int startX = 100; // Posisi awal X kucing
        int startY = ZONE_TOP_LIMIT + 30; // Posisi awal Y - di zona hijau, agak ke bawah dari batas atas

        // Buat object kucing dengan gambar-gambar yang sudah dimuat
        kucing = new Kucing(startX, startY, 70, 60,
                catImageRight, catImageLeft, catImageUp, catImageDown,
                catImageX, catImageHand);
    }

    // Method untuk setup timer animasi
    private void setupAnimationTimer() {
        animationTimer = new Timer(1000 / 60, this); // Timer animasi 60 FPS
    }

    // Method untuk memulai animasi
    public void startAnimation() {
        if (!animationTimer.isRunning()) {
            animationTimer.start(); // Mulai timer animasi jika belum berjalan
        }
    }

    // Method untuk menghentikan animasi
    public void stopAnimation() {
        if (animationTimer.isRunning()) {
            animationTimer.stop(); // Hentikan timer animasi jika sedang berjalan
        }
    } // Method untuk set dimensi panel game

    public void setPanelDimensions(int width, int height) {
        // Simpan nilai lama untuk property change
        int oldWidth = this.panelWidth;
        int oldHeight = this.panelHeight;

        // Update dimensi panel
        this.panelWidth = width;
        this.panelHeight = height;

        // Pastikan kucing tidak keluar dari boundaries dan tetap di zona hijau
        if (kucing != null) {
            int kucingX = kucing.getPosX(); // Posisi X kucing saat ini
            int kucingY = kucing.getPosY(); // Posisi Y kucing saat ini

            // Batasi pergerakan horizontal agar tidak keluar panel
            if (kucingX + kucing.getWidth() > this.panelWidth) {
                kucing.setPosX(this.panelWidth - kucing.getWidth());
            }

            // Batasi pergerakan vertikal agar tetap di zona hijau (area bawah)
            int minY = ZONE_TOP_LIMIT; // Batas atas zona hijau
            int maxY = ZONE_BOTTOM_LIMIT - kucing.getHeight(); // Batas bawah zona hijau

            if (kucingY < minY) {
                kucing.setPosY(minY); // Jika terlalu ke atas, pindah ke batas atas
            } else if (kucingY > maxY) {
                kucing.setPosY(maxY); // Jika terlalu ke bawah, pindah ke batas bawah
            }
        }

        // Buat tempat makan jika dimensi panel valid
        if (this.panelWidth > 0 && this.panelHeight > 0) {
            // Ukuran business logic tempat makan (untuk collision detection)
            int tempatMakanWidth = 100; // Lebar business size
            int tempatMakanHeight = 80; // Tinggi business size

            // Posisi tempat makan di zona hijau (kanan-tengah)
            int greenZoneTop = ZONE_TOP_LIMIT; // Batas atas zona hijau
            int greenZoneBottom = ZONE_BOTTOM_LIMIT; // Batas bawah zona hijau
            int greenZoneHeight = greenZoneBottom - greenZoneTop; // Tinggi zona hijau (160px)

            // Letakkan tempat makan di kanan zona hijau, vertikal di tengah
            int tempatMakanX = this.panelWidth - tempatMakanWidth - 50; // 50px dari tepi kanan
            int tempatMakanY = greenZoneTop + (greenZoneHeight - tempatMakanHeight) / 2; // Tengah vertikal zona hijau
                                                                                         // // Buat object tempat makan
                                                                                         // baru atau update yang sudah
                                                                                         // ada
            tempatMakan = new TempatMakan(tempatMakanX, tempatMakanY, tempatMakanWidth, tempatMakanHeight,
                    tempatMakanImage);

            // Fire property change agar UI update
            support.firePropertyChange("tempatMakanCreated", null, tempatMakan);
        }

        // Fire property change untuk notifikasi perubahan dimensi panel
        support.firePropertyChange("panelDimensions",
                new java.awt.Dimension(oldWidth, oldHeight),
                new java.awt.Dimension(this.panelWidth, this.panelHeight));
    }

    // Method untuk kontrol pergerakan kucing
    public void setKucingVelocity(int vx, int vy) {
        if (kucing != null) {
            kucing.setVelocityX(vx); // Set velocity horizontal
            kucing.setVelocityY(vy); // Set velocity vertikal
            // Fire property change untuk notifikasi perubahan velocity
            support.firePropertyChange("kucingVelocity", null, new int[] { vx, vy });
        }
    }

    // Method untuk menggerakkan kucing secara langsung
    public void moveKucing(int deltaX, int deltaY) {
        if (kucing != null) {
            int oldX = kucing.getPosX(); // Posisi X lama
            int oldY = kucing.getPosY(); // Posisi Y lama

            // Hitung posisi baru dengan batasan horizontal
            int newX = Math.max(0, Math.min(panelWidth - kucing.getWidth(), oldX + deltaX));
            int newY = oldY + deltaY; // Posisi Y baru

            // Batasi pergerakan vertikal agar kucing tidak keluar dari zona hijau
            int minY = ZONE_TOP_LIMIT; // Batas atas zona
            int maxY = ZONE_BOTTOM_LIMIT - kucing.getHeight(); // Batas bawah zona

            newY = Math.max(minY, Math.min(maxY, newY)); // Terapkan batasan vertikal

            // Set posisi baru kucing
            kucing.setPosX(newX);
            kucing.setPosY(newY); // Update gambar kucing berdasarkan arah pergerakan
            updateKucingDirection(deltaX, deltaY);

            // Fire property change untuk notifikasi perubahan posisi
            support.firePropertyChange("kucingPosition",
                    new java.awt.Point(oldX, oldY),
                    new java.awt.Point(newX, newY));
        }
    }

    // Method private untuk update arah kucing berdasarkan pergerakan
    private void updateKucingDirection(int deltaX, int deltaY) {
        if (kucing == null)
            return; // Keluar jika kucing null

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // Pergerakan horizontal lebih dominan
            if (deltaX > 0) {
                kucing.setCurrentImage(catImageRight); // Gerak ke kanan
            } else if (deltaX < 0) {
                kucing.setCurrentImage(catImageLeft); // Gerak ke kiri
            }
        } else if (deltaY != 0) {
            // Pergerakan vertikal lebih dominan
            if (deltaY > 0) {
                kucing.setCurrentImage(catImageDown); // Gerak ke bawah
            } else {
                kucing.setCurrentImage(catImageUp); // Gerak ke atas
            }
        }
    }

    // Method untuk mengaktifkan/menonaktifkan tangan kucing
    public void setHandActive(boolean active) {
        // Hanya izinkan kontrol manual jika tidak sedang delivery
        if (kucing != null && !kucing.isHandDelivering()) {
            kucing.setHandActive(active); // Set status tangan aktif
            // Fire property change untuk notifikasi perubahan status tangan
            support.firePropertyChange("handActive", !active, active);
        }
    }

    // Method untuk set target posisi tangan kucing
    public void setHandTarget(int x, int y) {
        this.handTargetX = x; // Set target X tangan
        this.handTargetY = y; // Set target Y tangan

        // Hanya izinkan kontrol manual jika tidak sedang delivery
        if (kucing != null && !kucing.isHandDelivering()) {
            kucing.setHandTarget(x, y); // Set target tangan di model
            // Fire property change untuk notifikasi perubahan target tangan
            support.firePropertyChange("handTarget", null, new java.awt.Point(handTargetX, handTargetY));
        }
    }

    // Method untuk melakukan aksi tangan (mengambil)
    public void performHandAction() {
        if (kucing != null && kucing.isHandActive()) {
            kucing.setCurrentImage(catImageX); // Tampilkan animasi mengambil
            // Fire property change untuk notifikasi aksi tangan
            support.firePropertyChange("handAction", false, true);
        }
    }

    // Method untuk cek collision kucing dengan area tertentu
    public boolean isKucingCollidingWith(int x, int y, int width, int height) {
        if (kucing == null)
            return false; // Return false jika kucing null // Cek collision menggunakan AABB (Axis-Aligned
                          // Bounding Box)
        return kucing.getPosX() < x + width &&
                kucing.getPosX() + kucing.getWidth() > x &&
                kucing.getPosY() < y + height &&
                kucing.getPosY() + kucing.getHeight() > y;
    }

    // Method utama untuk update game (dipanggil oleh timer)
    @Override
    public void actionPerformed(ActionEvent e) {
        // Update pergerakan kucing berdasarkan velocity
        if (kucing != null && panelWidth > 0 && panelHeight > 0) {
            // Ambil posisi kucing saat ini
            int oldX = kucing.getPosX();
            int oldY = kucing.getPosY();

            // Ambil velocity kucing saat ini
            int vx = kucing.getVelocityX(); // Velocity horizontal
            int vy = kucing.getVelocityY(); // Velocity vertikal

            if (vx != 0 || vy != 0) {
                // Hitung posisi baru berdasarkan velocity
                int newX = oldX + vx;
                int newY = oldY + vy;

                // Batasi pergerakan horizontal agar tidak keluar panel
                newX = Math.max(0, Math.min(panelWidth - kucing.getWidth(), newX));

                // Batasi pergerakan vertikal agar tetap di zona hijau
                int minY = ZONE_TOP_LIMIT; // Batas atas zona hijau
                int maxY = ZONE_BOTTOM_LIMIT - kucing.getHeight(); // Batas bawah zona hijau
                newY = Math.max(minY, Math.min(maxY, newY));

                // Terapkan posisi baru ke kucing
                kucing.setPosX(newX);
                kucing.setPosY(newY);

                // Update gambar kucing berdasarkan arah pergerakan
                updateKucingDirection(vx, vy);
            }

            // Update animasi tangan kucing
            kucing.updateHandAnimation();

            // Update sistem ikan
            checkFishCollision(); // Cek apakah kucing menangkap ikan
            updateCarriedFish(); // Update posisi ikan yang dibawa
            checkTempatMakanInteraction(); // Cek interaksi dengan tempat makan

            // Spawn ikan baru jika jumlah ikan kurang dari minimum
            fishSpawnTimer++; // Increment timer spawn ikan
            if (fishSpawnTimer >= FISH_SPAWN_INTERVAL && listIkan.size() < MIN_FISH) {
                spawnRandomFish(); // Spawn ikan baru
                fishSpawnTimer = 0; // Reset timer spawn
            }

            // Fire property change untuk notifikasi perubahan posisi kucing
            support.firePropertyChange("kucingPosition", null, kucing);
        }

        // Notify semua view untuk repaint
        support.firePropertyChange("animationUpdate", false, true);
    }

    // Method untuk mendapatkan data kucing untuk view
    public KucingViewData getKucingViewData() {
        if (kucing == null) {
            return null; // Return null jika kucing tidak ada
        } // Return data kucing dalam bentuk immutable object
        return new KucingViewData(
                kucing.getPosX(), // Posisi X kucing
                kucing.getPosY(), // Posisi Y kucing
                kucing.getWidth(), // Lebar kucing
                kucing.getHeight(), // Tinggi kucing
                kucing.getCurrentImage(), // Gambar kucing saat ini
                kucing.isHandActive(), // Status apakah tangan aktif
                kucing.getHandAnimX(), // Posisi X animasi tangan
                kucing.getHandAnimY(), // Posisi Y animasi tangan
                kucing.getImageHand(), // Gambar tangan
                kucing.getVelocityX(), // Velocity horizontal
                kucing.getVelocityY()); // Velocity vertikal
    }

    // Getter methods untuk properti individual kucing
    public int getKucingX() {
        return kucing != null ? kucing.getPosX() : 0; // Return posisi X atau 0 jika null
    }

    public int getKucingY() {
        return kucing != null ? kucing.getPosY() : 0; // Return posisi Y atau 0 jika null
    }

    public int getKucingWidth() {
        return kucing != null ? kucing.getWidth() : 0; // Return lebar atau 0 jika null
    }

    public int getKucingHeight() {
        return kucing != null ? kucing.getHeight() : 0; // Return tinggi atau 0 jika null
    }

    // Method untuk mendapatkan gambar kucing saat ini
    public Image getKucingImage() {
        if (kucing != null) {
            if (kucing.isHandActive()) {
                return catImageX; // Tampilkan gambar mengambil saat tangan aktif
            } else {
                return kucing.getCurrentImage(); // Tampilkan gambar sesuai arah
            }
        }
        return catImageRight; // Default gambar menghadap kanan
    }

    // Method untuk cek apakah tangan kucing aktif
    public boolean isHandActive() {
        return kucing != null && kucing.isHandActive();
    } // Method untuk mendapatkan posisi X tangan

    public int getHandX() {
        return kucing != null ? kucing.getHandAnimX() : 0; // Return posisi X tangan atau 0
    }

    // Method untuk mendapatkan posisi Y tangan
    public int getHandY() {
        return kucing != null ? kucing.getHandAnimY() : 0; // Return posisi Y tangan atau 0
    }

    // Method untuk mendapatkan gambar tangan
    public Image getHandImage() {
        return kucing != null ? kucing.getImageHand() : catImageHand; // Return gambar tangan
    }

    // Getter methods tambahan untuk kompatibilitas dengan View
    public int getKucingPosX() {
        return kucing != null ? kucing.getPosX() : 0; // Alternative getter untuk posisi X
    }

    public int getKucingPosY() {
        return kucing != null ? kucing.getPosY() : 0; // Alternative getter untuk posisi Y
    }

    public int getKucingHandAnimX() {
        return kucing != null ? kucing.getHandAnimX() : 0; // Getter posisi X animasi tangan
    }

    public int getKucingHandAnimY() {
        return kucing != null ? kucing.getHandAnimY() : 0; // Getter posisi Y animasi tangan
    }

    public Image getKucingHandImage() {
        return kucing != null ? kucing.getImageHand() : null; // Getter gambar tangan kucing
    }

    public int getKucingVelocityX() {
        return kucing != null ? kucing.getVelocityX() : 0; // Getter velocity horizontal
    }

    public int getKucingVelocityY() {
        return kucing != null ? kucing.getVelocityY() : 0; // Getter velocity vertikal
    }

    public boolean isKucingHandActive() {
        return kucing != null && kucing.isHandActive(); // Cek apakah tangan kucing aktif
    }

    public boolean isKucingHandDelivering() {
        return kucing != null && kucing.isHandDelivering(); // Cek apakah kucing sedang delivery
    } // Method untuk mendapatkan data ikan tanpa akses langsung ke model

    public java.util.List<IkanViewData> getIkanViewData() {
        java.util.List<IkanViewData> ikanData = new java.util.ArrayList<>(); // List data ikan untuk view
        for (Ikan ikan : listIkan) {
            // Semua ikan di listIkan adalah ikan yang "hidup" (bisa ditampilkan)
            // Ambil gambar ikan berdasarkan tipe ikan
            Image fishImage = null;
            if (ikan.getFishType() >= 0 && ikan.getFishType() < fishImages.length) {
                fishImage = fishImages[ikan.getFishType()]; // Ambil gambar sesuai tipe
            }

            // Tambahkan data ikan ke list
            ikanData.add(new IkanViewData(
                    ikan.getPosX(), // Posisi X ikan
                    ikan.getPosY(), // Posisi Y ikan
                    ikan.getWidth(), // Lebar ikan
                    ikan.getHeight(), // Tinggi ikan
                    fishImage)); // Gambar ikan
        }
        return ikanData; // Return list data ikan untuk view
    }

    // Method untuk mendapatkan data tempat makan
    public TempatMakanViewData getTempatMakanViewData() {
        if (tempatMakan != null) {
            // Return data tempat makan untuk view
            return new TempatMakanViewData(
                    tempatMakan.getPosX(), // Posisi X tempat makan
                    tempatMakan.getPosY(), // Posisi Y tempat makan
                    tempatMakan.getWidth(), // Lebar tempat makan (business size)
                    tempatMakan.getHeight(), // Tinggi tempat makan (business size)
                    tempatMakan.getImage(), // Gambar tempat makan
                    tempatMakan.isVisible()); // Status visibility tempat makan
        }
        return null; // Return null jika tempat makan tidak ada
    }

    // Support untuk property change listener (observer pattern)
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener); // Tambah listener
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener); // Hapus listener
    }

    // Method untuk akses ke model kucing (untuk keperluan GameViewModel)
    public Kucing getModel() {
        return kucing; // Return object kucing
    } // Method untuk cek apakah kucing sedang melakukan delivery

    public boolean isHandDelivering() {
        return kucing != null && kucing.isHandDelivering(); // Return status delivery
    }

    // Inner class untuk transfer data kucing ke View (immutable data object)
    public static class KucingViewData {
        public final int posX; // Posisi X kucing
        public final int posY; // Posisi Y kucing
        public final int width; // Lebar kucing
        public final int height; // Tinggi kucing
        public final Image image; // Gambar kucing saat ini
        public final boolean isHandActive; // Status apakah tangan aktif
        public final int handX; // Posisi X tangan
        public final int handY; // Posisi Y tangan
        public final Image handImage; // Gambar tangan
        public final int velocityX; // Velocity horizontal
        public final int velocityY; // Velocity vertikal

        // Constructor untuk inisialisasi semua data kucing
        public KucingViewData(int posX, int posY, int width, int height, Image image,
                boolean isHandActive, int handX, int handY, Image handImage,
                int velocityX, int velocityY) {
            this.posX = posX;
            this.posY = posY;
            this.width = width;
            this.height = height;
            this.image = image;
            this.isHandActive = isHandActive;
            this.handX = handX;
            this.handY = handY;
            this.handImage = handImage;
            this.velocityX = velocityX;
            this.velocityY = velocityY;
        }
    }

    // Method manajemen ikan (Fish management methods)

    // Method untuk spawn ikan awal saat game dimulai
    private void spawnInitialFish() {
        for (int i = 0; i < MIN_FISH; i++) {
            spawnRandomFish(); // Spawn ikan secara random
        }
    }

    // Method untuk spawn ikan baru secara random
    private void spawnRandomFish() {
        // Cek apakah sudah mencapai maksimal ikan atau panel belum valid
        if (listIkan.size() >= MAX_FISH || panelWidth <= 0 || panelHeight <= 0) {
            return; // Keluar jika kondisi tidak memenuhi
        }
        int fishWidth = 60; // Lebar ikan default
        int fishHeight = 50; // Tinggi ikan default
        int margin = 50; // Margin dari tepi panel

        // Spawn ikan hanya di zona hijau (area kucing bermain)
        int minX = margin; // Batas kiri spawn
        int maxX = panelWidth - fishWidth - margin; // Batas kanan spawn
        int minY = ZONE_TOP_LIMIT + margin; // Batas atas spawn (zona hijau)
        int maxY = ZONE_BOTTOM_LIMIT - fishHeight - margin; // Batas bawah spawn (zona hijau)

        // Generate posisi random dalam zona hijau
        int x = random.nextInt(maxX - minX) + minX; // Posisi X random
        int y = random.nextInt(maxY - minY) + minY; // Posisi Y random

        // Pastikan ikan tidak spawn terlalu dekat dengan kucing
        if (kucing != null) {
            while (Math.abs(x - kucing.getPosX()) < 100 && Math.abs(y - kucing.getPosY()) < 100) {
                x = random.nextInt(maxX - minX) + minX; // Generate ulang posisi X
                y = random.nextInt(maxY - minY) + minY; // Generate ulang posisi Y
            }
        }

        // Pilih tipe ikan secara random dari 3 pilihan (0, 1, 2)
        int randomFishType = random.nextInt(3);

        // Buat object ikan baru dan tambahkan ke list
        Ikan ikan = new Ikan(x, y, fishWidth, fishHeight, randomFishType);
        listIkan.add(ikan);
    }

    // Method untuk mengecek collision tangan kucing dengan ikan
    private void checkFishCollision() {
        // Keluar jika kucing null, tangan tidak aktif, atau sudah membawa ikan
        if (kucing == null || !kucing.isHandActive() || isCarryingFish) {
            return;
        }

        // Ambil posisi tangan kucing
        int handX = kucing.getHandAnimX();
        int handY = kucing.getHandAnimY();

        // Cek collision dengan setiap ikan
        for (Ikan ikan : listIkan) {
            // Cek apakah ikan bisa ditangkap dan terjadi collision dengan tangan
            if (ikan.isAvailableForCatch() &&
                    ikan.isCollidingWith(handX - 15, handY - 15, 30, 30)) {
                // Tangkap ikan pertama yang ditemukan
                ikan.setBeingCarried(true); // Set ikan sedang dibawa
                carriedFish = ikan; // Simpan referensi ikan yang dibawa
                isCarryingFish = true; // Set status membawa ikan // Mulai proses delivery otomatis ke tempat makan
                if (tempatMakan != null) {
                    kucing.startDeliveryToFoodBowl(tempatMakan.getCenterX(), tempatMakan.getCenterY());
                    kucing.setHandActive(true); // Pastikan tangan tetap aktif selama delivery
                }
                break; // Hanya tangkap satu ikan saja
            }
        }
    }

    // Method untuk update posisi ikan yang sedang dibawa kucing
    private void updateCarriedFish() {
        if (carriedFish != null && isCarryingFish) {
            if (kucing.isHandDelivering()) {
                // Saat delivery, ikan mengikuti posisi tangan
                carriedFish.setPosX(kucing.getHandAnimX() - carriedFish.getWidth() / 2);
                carriedFish.setPosY(kucing.getHandAnimY() - carriedFish.getHeight() / 2);
            } else {
                // Saat ikan dibawa tapi belum dalam animasi delivery
                // Hitung posisi target ikan (tengah kucing)
                int targetX = kucing.getPosX() + kucing.getWidth() / 2 - carriedFish.getWidth() / 2;
                int targetY = kucing.getPosY() + kucing.getHeight() / 2 - carriedFish.getHeight() / 2;

                // Ambil posisi ikan saat ini
                int currentX = carriedFish.getPosX();
                int currentY = carriedFish.getPosY();

                // Hitung delta untuk smooth movement
                int deltaX = targetX - currentX;
                int deltaY = targetY - currentY;

                // Terapkan kecepatan pergerakan yang smooth
                double speed = 0.3; // Kecepatan follow kucing
                int newX = currentX + (int) (deltaX * speed);
                int newY = currentY + (int) (deltaY * speed);

                // Set posisi baru ikan
                carriedFish.setPosX(newX);
                carriedFish.setPosY(newY);
            }
        }
    }

    // Method untuk mengecek interaksi dengan tempat makan
    private void checkTempatMakanInteraction() {
        // Cek jika sedang membawa ikan, ada tempat makan, dan sedang delivery
        if (carriedFish != null && isCarryingFish && tempatMakan != null && kucing.isHandDelivering()) {
            // Hitung jarak antara tangan kucing dan pusat tempat makan
            int handX = kucing.getHandAnimX(); // Posisi X tangan
            int handY = kucing.getHandAnimY(); // Posisi Y tangan

            int tempatMakanCenterX = tempatMakan.getCenterX(); // Pusat X tempat makan
            int tempatMakanCenterY = tempatMakan.getCenterY(); // Pusat Y tempat makan

            // Hitung jarak euclidean
            double distance = Math
                    .sqrt(Math.pow(handX - tempatMakanCenterX, 2) + Math.pow(handY - tempatMakanCenterY, 2)); // Jika
                                                                                                              // tangan
                                                                                                              // cukup
                                                                                                              // dekat
                                                                                                              // dengan
                                                                                                              // tempat
                                                                                                              // makan
                                                                                                              // (dalam
                                                                                                              // radius
                                                                                                              // 30
                                                                                                              // pixel)
            if (distance < 30) {
                // Letakkan ikan di tempat makan
                listIkan.remove(carriedFish); // Hapus ikan dari list
                carriedFish = null; // Reset referensi ikan yang dibawa
                isCarryingFish = false; // Reset status membawa ikan
                fishDelivered++; // Increment skor ikan yang diantarkan

                // Reset mode delivery - penting untuk mengizinkan kontrol tangan lagi
                kucing.setHandDelivering(false); // Matikan mode delivery
                kucing.setHandActive(false); // Matikan tangan setelah delivery selesai

                // Fire property change untuk update UI
                support.firePropertyChange("fishDelivered", fishDelivered - 1, fishDelivered);

                // Spawn ikan baru untuk mengganti yang sudah diantarkan
                spawnRandomFish();
            }
        }
    }

    // Method untuk mendapatkan status apakah kucing sedang membawa ikan
    public boolean isCarryingFish() {
        return isCarryingFish; // Return status carrying fish
    }

    // Method untuk mendapatkan referensi ikan yang sedang dibawa
    public Ikan getCarriedFish() {
        return carriedFish; // Return ikan yang sedang dibawa
    }

    // Method untuk mengatur hover state tempat makan
    public void setTempatMakanHover(int mouseX, int mouseY) {
        if (tempatMakan != null) {
            boolean wasVisible = tempatMakan.isVisible(); // Status visibility sebelumnya
            boolean isHovering = tempatMakan.isMouseOver(mouseX, mouseY); // Cek apakah mouse hover

            // Jika status hover berubah, update visibility
            if (isHovering != wasVisible) {
                tempatMakan.setVisible(isHovering); // Update visibility
                // Fire property change untuk notifikasi perubahan hover
                support.firePropertyChange("tempatMakanHover", wasVisible, isHovering);
            }
        }
    } // Method untuk mendapatkan jumlah ikan yang sudah diantarkan

    public int getFishDelivered() {
        return fishDelivered; // Return jumlah ikan yang sudah diantarkan
    }

    // Inner classes untuk transfer data dari viewmodel ke view

    // Inner class untuk data ikan yang akan ditampilkan di view
    public static class IkanViewData {
        public final int posX, posY, width, height; // Posisi dan dimensi ikan
        public final Image image; // Gambar ikan

        // Constructor untuk inisialisasi data ikan
        public IkanViewData(int posX, int posY, int width, int height, Image image) {
            this.posX = posX; // Posisi X ikan
            this.posY = posY; // Posisi Y ikan
            this.width = width; // Lebar ikan
            this.height = height; // Tinggi ikan
            this.image = image; // Gambar ikan
        }
    }

    // Inner class untuk data tempat makan yang akan ditampilkan di view
    public static class TempatMakanViewData {
        public final int posX, posY, width, height; // Posisi dan dimensi tempat makan
        public final Image image; // Gambar tempat makan
        public final boolean isVisible; // Status visibility tempat makan

        // Constructor untuk inisialisasi data tempat makan
        public TempatMakanViewData(int posX, int posY, int width, int height, Image image, boolean isVisible) {
            this.posX = posX; // Posisi X tempat makan
            this.posY = posY; // Posisi Y tempat makan
            this.width = width; // Lebar tempat makan
            this.height = height; // Tinggi tempat makan
            this.image = image; // Gambar tempat makan
            this.isVisible = isVisible; // Status apakah tempat makan terlihat
        }
    }
}