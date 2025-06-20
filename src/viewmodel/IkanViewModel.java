package viewmodel;

import model.Ikan;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * IkanViewModel - ViewModel untuk mengelola semua ikan dalam game
 * 
 * Class ini bertanggung jawab untuk:
 * - Spawn dan menghapus ikan secara otomatis
 * - Mengatur pergerakan dan animasi ikan
 * - Mendeteksi tabrakan dan collision avoidance
 * - Memberikan data ikan ke View layer
 * - Menangani interaksi dengan ikan (tangkap, bawa, antarkan)
 * 
 * Pattern: MVVM (Model-View-ViewModel)
 * 
 */

// Class untuk mengelola semua ikan dalam game
// Mengatur spawn, movement, dan interaksi ikan
public class IkanViewModel implements ActionListener {
    // Daftar semua ikan yang ada di game saat ini
    private List<Ikan> listIkan;
    // Support untuk memberitahu komponen lain tentang perubahan
    private final PropertyChangeSupport propertyChangeSupport; // Generator angka random untuk spawn dan movement
    private final Random random;
    // Timer untuk mengontrol pergerakan ikan secara otomatis
    private Timer movementTimer;
    // Lebar panel game untuk boundary checking
    private static final int PANEL_WIDTH = 800;

    /**
     * Constructor untuk inisialisasi IkanViewModel
     * Mengatur semua komponen yang diperlukan untuk sistem ikan
     */
    public IkanViewModel() {
        // Membuat list kosong untuk menyimpan ikan-ikan
        this.listIkan = new ArrayList<>();
        // Setup property change support untuk notifikasi ke View
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        // Inisialisasi random generator untuk spawn dan movement
        this.random = new Random();
        // Setup timer dengan frame rate 60 FPS untuk smooth movement
        this.movementTimer = new Timer(1000 / 60, this);

        // Spawn ikan-ikan awal ketika game dimulai
        spawnInitialFish();
        // Mulai timer untuk pergerakan ikan otomatis
        startMovement();
    }// Method untuk membuat ikan baru secara random

    public void spawnNewFish() {
        // Generate tipe ikan secara random (0=ikan1, 1=ikan2, 2=ikan3)
        int fishType = random.nextInt(3);

        // Variabel untuk posisi spawn ikan (inisialisasi dengan nilai default)
        int fishX = 0, fishY = 0;
        boolean isTopZone = false;
        int attempts = 0;
        int maxAttempts = 15;

        // Loop untuk mencari posisi spawn yang tidak terlalu dekat dengan ikan lain
        do {
            if (random.nextBoolean()) {
                // ZONA ATAS (0-150) - Ikan bergerak dari kanan ke kiri
                fishY = random.nextInt(120) + 15; // Posisi Y random dalam zona atas: 15-135
                isTopZone = true;
                // Spawn dari sebelah kanan layar
                fishX = PANEL_WIDTH + random.nextInt(300) + 50; // Area spawn: 850-1150
            } else {
                // ZONA BAWAH (450-600) - Ikan bergerak dari kiri ke kanan
                fishY = random.nextInt(120) + 465; // Posisi Y random dalam zona bawah: 465-585
                isTopZone = false;
                // Spawn dari sebelah kiri layar
                fishX = -random.nextInt(300) - 50; // Area spawn: -350 sampai -50
            }
            attempts++;
        } while (isTooCloseToOtherFish(fishX, fishY, 60, 50) && attempts < maxAttempts);

        // Jika setelah 15 percobaan masih terlalu dekat, skip spawn untuk sementara
        if (attempts >= maxAttempts) {
            return;
        }

        // Membuat objek ikan baru dengan informasi tipe (View akan load gambar yang
        // sesuai)
        Ikan newFish = new Ikan(fishX, fishY, 60, 50, fishType);

        // Mengatur arah pergerakan sesuai zona (kecepatan sama untuk semua)
        int baseSpeed = 5; // Kecepatan tetap untuk semua jenis ikan

        if (isTopZone) {
            // Zona atas: bergerak ke kiri dengan velocity negatif
            newFish.setVelocityX(-baseSpeed);
        } else {
            // Zona bawah: bergerak ke kanan dengan velocity positif
            newFish.setVelocityX(baseSpeed);
        }

        // Menambahkan ikan ke list dan memberitahu listener
        listIkan.add(newFish);
        firePropertyChange("fishSpawned", null, newFish);
    }

    // Method untuk memperbarui posisi semua ikan dalam game
    public void updateAllFish(int panelWidth) {
        // Update posisi setiap ikan berdasarkan velocity mereka
        for (Ikan ikan : listIkan) {
            ikan.updatePosition(panelWidth);
        }
        // Memberitahu UI bahwa ikan sudah diupdate agar melakukan repaint
        firePropertyChange("fishUpdated", null, null);
    }

    // Method untuk spawn ikan-ikan awal ketika game dimulai
    private void spawnInitialFish() {
        // Spawn ikan awal dengan jumlah yang lebih moderat
        int initialFishCount = 8 + random.nextInt(3); // 8-10 ikan awal
        for (int i = 0; i < initialFishCount; i++) {
            spawnNewFish();
        }
    }

    // Method untuk mencari ikan yang tersedia di posisi tertentu
    public Ikan findAvailableIkanAt(int x, int y, int width, int height) {
        // Loop semua ikan untuk mencari yang bisa ditangkap
        for (Ikan ikan : listIkan) {
            // Cek apakah ikan tersedia dan berada di area yang diklik
            if (ikan.isAvailableForCatch() && ikan.isCollidingWith(x, y, width, height)) {
                return ikan;
            }
        }
        // Tidak ditemukan ikan yang sesuai
        return null;
    }

    // Method untuk mengatur status ikan yang sedang dibawa
    public void setIkanBeingCarried(Ikan ikan, boolean beingCarried) {
        if (ikan != null) {
            // Set status ikan sedang ditangkap
            ikan.setBeingCaught(beingCarried);
            if (beingCarried) {
                // Hentikan pergerakan normal ikan
                ikan.setVelocityX(0);
            }
        }
    }

    // Method untuk menghapus ikan dari daftar (misalnya setelah dimakan)
    public void removeIkan(Ikan ikan) {
        if (ikan != null && listIkan.contains(ikan)) {
            // Hapus ikan dari list
            listIkan.remove(ikan);
            // Beritahu listener bahwa ikan telah dihapus
            firePropertyChange("fishRemoved", ikan, null);
        }
    } // Method untuk mengatur status ikan yang sedang ditangkap

    public void setIkanBeingCaught(Ikan ikan, boolean beingCaught) {
        if (ikan != null) {
            // Set flag bahwa ikan sedang dalam proses penangkapan
            ikan.setBeingCaught(beingCaught);
        }
    }

    // Method untuk mengatur status ikan yang sedang diantarkan
    public void setIkanBeingDelivered(Ikan ikan, boolean beingDelivered) {
        if (ikan != null) {
            // Set flag bahwa ikan sedang dalam proses pengiriman ke tempat makan
            ikan.setBeingDelivered(beingDelivered);
        }
    }

    // Method untuk memindahkan posisi ikan ke koordinat tertentu
    public void moveIkanToPosition(Ikan ikan, int x, int y) {
        if (ikan != null) {
            // Update posisi ikan langsung tanpa animasi
            ikan.setPosX(x);
            ikan.setPosY(y);
        }
    } // Inner class untuk menyimpan data ikan yang akan ditampilkan di UI

    public static class IkanViewData {
        // Data posisi dan ukuran ikan
        public final int posX, posY, width, height;
        // Tipe ikan untuk menentukan gambar yang digunakan
        public final int fishType;
        // Status apakah ikan sedang ditangkap
        public final boolean isBeingCaught;
        // Kecepatan horizontal ikan
        public final int velocityX;
        // Flag arah pergerakan ikan
        public final boolean isMovingRight;// Constructor untuk membuat data view dari objek Ikan

        public IkanViewData(Ikan ikan) {
            // Menyalin semua data yang diperlukan untuk tampilan
            this.posX = ikan.getPosX();
            this.posY = ikan.getPosY();
            this.width = ikan.getWidth();
            this.height = ikan.getHeight();
            this.fishType = ikan.getFishType();
            this.isBeingCaught = ikan.isBeingCaught();
            this.velocityX = ikan.getVelocityX();
            this.isMovingRight = ikan.isMovingRight();
        }
    }

    // Method untuk mendapatkan daftar data ikan untuk ditampilkan di UI
    public List<IkanViewData> getIkanViewDataList() {
        List<IkanViewData> viewDataList = new ArrayList<>();
        // Konversi setiap ikan menjadi data view
        for (Ikan ikan : listIkan) {
            viewDataList.add(new IkanViewData(ikan));
        }
        return viewDataList;
    }

    // Method untuk mendapatkan jumlah ikan yang tersedia
    public int getAvailableIkanCount() {
        return listIkan.size();
    }

    // Method untuk mendapatkan total jumlah ikan
    public int getTotalIkanCount() {
        return listIkan.size();
    } // Method untuk menambahkan listener yang ingin mendengar perubahan

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    // Method untuk mengirim notifikasi perubahan ke semua listener
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    // Method getter untuk mengakses list ikan (protected untuk inheritance)
    protected List<Ikan> getListIkan() {
        return listIkan;
    }

    // Method untuk menghapus listener yang tidak diperlukan lagi
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    } // ===== BAGIAN KONTROL PERGERAKAN IKAN =====

    // Method untuk memulai pergerakan ikan
    public void startMovement() {
        // Mulai timer jika belum berjalan
        if (movementTimer != null && !movementTimer.isRunning()) {
            movementTimer.start();
        }
    }

    // Method untuk menghentikan pergerakan ikan (saat pause)
    public void stopMovement() {
        // Hentikan timer jika sedang berjalan
        if (movementTimer != null && movementTimer.isRunning()) {
            movementTimer.stop();
        }
    }

    // Method untuk mengecek apakah posisi spawn terlalu dekat dengan ikan lain
    private boolean isTooCloseToOtherFish(int newX, int newY, int newWidth, int newHeight) {
        // Jarak minimum antar ikan untuk menghindari spawn yang terlalu berdekatan
        final int MIN_DISTANCE = 120;

        // Periksa jarak dengan semua ikan yang sudah ada
        for (Ikan existingFish : listIkan) {
            // Skip jika ikan sedang dalam interaksi (ditangkap, dibawa, dll)
            if (existingFish.isBeingCaught() || existingFish.isBeingCarried() ||
                    existingFish.isBeingDelivered()) {
                continue;
            }

            // Hitung jarak antara pusat ikan yang sudah ada dengan posisi baru
            int existingCenterX = existingFish.getPosX() + existingFish.getWidth() / 2;
            int existingCenterY = existingFish.getPosY() + existingFish.getHeight() / 2;
            int newCenterX = newX + newWidth / 2;
            int newCenterY = newY + newHeight / 2;
            double distance = calculateDistance(existingCenterX, existingCenterY, newCenterX, newCenterY);

            // Jika jarak kurang dari minimum, berarti terlalu dekat
            if (distance < MIN_DISTANCE) {
                return true;
            }
        }
        // Posisi aman, tidak terlalu dekat dengan ikan lain
        return false;
    }

    // Implementasi ActionListener untuk pergerakan ikan otomatis
    @Override
    public void actionPerformed(ActionEvent e) {
        // Update pergerakan semua ikan setiap frame
        updateAllFishMovement();
        // updateAttractedFish(); // Disabled - sekarang tangan yang membawa ikan
    } // Method untuk memperbarui pergerakan semua ikan

    private void updateAllFishMovement() {
        // List untuk menyimpan ikan yang perlu dihapus (keluar dari layar)
        List<Ikan> fishToRemove = new ArrayList<>();

        // Iterasi semua ikan untuk update posisi mereka
        for (Ikan ikan : listIkan) {
            // Skip pergerakan jika ikan sedang ditangkap, dibawa, atau diantarkan
            if (ikan.isBeingCaught() || ikan.isBeingCarried() || ikan.isBeingDelivered()) {
                continue;
            }

            // Hitung posisi baru berdasarkan velocity
            int newX = ikan.getPosX() + ikan.getVelocityX();
            int newY = ikan.getPosY();

            // Sistem deteksi tabrakan yang lebih canggih dengan ikan lain
            boolean canMove = true;
            Ikan closestFish = null;
            double minDistance = Double.MAX_VALUE;

            // Cek jarak dengan semua ikan lain untuk menghindari tabrakan
            for (Ikan otherFish : listIkan) {
                if (otherFish != ikan && !otherFish.isBeingCaught() &&
                        !otherFish.isBeingCarried() && !otherFish.isBeingDelivered()) {

                    // Hitung jarak antara pusat kedua ikan
                    double distance = calculateDistance(
                            newX + ikan.getWidth() / 2, newY + ikan.getHeight() / 2,
                            otherFish.getPosX() + otherFish.getWidth() / 2,
                            otherFish.getPosY() + otherFish.getHeight() / 2);

                    // Tracking ikan terdekat untuk collision avoidance
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestFish = otherFish;
                    }

                    // Cek apakah terlalu dekat (threshold tabrakan)
                    if (distance < 85) { // Threshold ditingkatkan untuk spacing yang lebih baik
                        canMove = false;
                        break;
                    }
                }
            }

            if (canMove) {
                // Aman untuk bergerak, update posisi
                ikan.setPosX(newX);
            } else {
                // Lakukan manuver menghindari tabrakan
                avoidCollision(ikan, closestFish);
            }

            // Hapus ikan yang sudah keluar dari layar
            if (ikan.getVelocityX() < 0 && ikan.getPosX() < -120) {
                // Ikan bergerak ke kiri dan sudah melewati batas kiri layar
                fishToRemove.add(ikan);
            } else if (ikan.getVelocityX() > 0 && ikan.getPosX() > PANEL_WIDTH + 120) {
                // Ikan bergerak ke kanan dan sudah melewati batas kanan layar
                fishToRemove.add(ikan);
            }
        }

        // Hapus ikan-ikan yang sudah keluar dari layar
        for (Ikan fish : fishToRemove) {
            listIkan.remove(fish);
        }

        // Spawn ikan baru jika diperlukan (maintain jumlah optimal)
        while (listIkan.size() < 6) { // Pertahankan minimal 6 ikan
            spawnNewFish();
        }

        // Spawn ikan tambahan secara random untuk variasi
        if (random.nextInt(250) < 2) { // Reduced spawn rate (0.8% chance per frame)
            if (listIkan.size() < 9) { // Maksimal 9 ikan (dikurangi dari 10)
                spawnNewFish();
            }
        }

        // Kirim notifikasi ke UI bahwa ada pergerakan ikan
        firePropertyChange("fishMovement", null, System.currentTimeMillis());
    } // Method untuk menghitung jarak antara dua titik menggunakan rumus Euclidean

    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    // Method untuk menghindari tabrakan dengan ikan lain
    private void avoidCollision(Ikan ikan, Ikan closestFish) {
        if (closestFish == null)
            return;

        // Dapatkan posisi saat ini dari kedua ikan
        int fishX = ikan.getPosX();
        int fishY = ikan.getPosY();
        int otherY = closestFish.getPosY();

        // Hitung manuver penghindaran berdasarkan arah gerakan
        if (ikan.getVelocityX() > 0) { // Bergerak ke kanan
            if (fishY < otherY) {
                // Bergerak ke atas untuk menghindari tabrakan
                int newY = Math.max(fishY - 15, getMinYForZone(fishY));
                ikan.setPosY(newY);
            } else {
                // Bergerak ke bawah untuk menghindari tabrakan
                int newY = Math.min(fishY + 15, getMaxYForZone(fishY));
                ikan.setPosY(newY);
            }
        } else { // Bergerak ke kiri
            if (fishY < otherY) {
                // Bergerak ke atas untuk menghindari tabrakan
                int newY = Math.max(fishY - 15, getMinYForZone(fishY));
                ikan.setPosY(newY);
            } else {
                // Bergerak ke bawah untuk menghindari tabrakan
                int newY = Math.min(fishY + 15, getMaxYForZone(fishY));
                ikan.setPosY(newY);
            }
        }

        // Tambahkan penyesuaian horizontal kecil untuk menghindari stuck
        int horizontalAdjust = random.nextInt(10) - 5; // Range -5 sampai +5
        ikan.setPosX(fishX + horizontalAdjust);
    }

    // Method untuk mendapatkan batas minimum Y berdasarkan zona pergerakan
    private int getMinYForZone(int currentY) {
        if (currentY < 150) {
            return 15; // Batas minimum zona atas
        } else {
            return 465; // Batas minimum zona bawah
        }
    }

    // Method untuk mendapatkan batas maksimum Y berdasarkan zona pergerakan
    private int getMaxYForZone(int currentY) {
        if (currentY < 150) {
            return 135; // Batas maksimum zona atas
        } else {
            return 585; // Batas maksimum zona bawah
        }
    } // Method untuk mendeteksi ikan yang diklik berdasarkan koordinat mouse

    public Ikan findClickedFish(int mouseX, int mouseY) {
        for (Ikan ikan : listIkan) {
            if (ikan.isAvailableForCatch()) {
                // Cek apakah klik mouse berada dalam batas area ikan
                if (mouseX >= ikan.getPosX() && mouseX <= ikan.getPosX() + ikan.getWidth() &&
                        mouseY >= ikan.getPosY() && mouseY <= ikan.getPosY() + ikan.getHeight()) {
                    return ikan;
                }
            }
        }
        // Tidak ada ikan yang terklik
        return null;
    }

    // Method untuk mendapatkan ikan yang sedang tertarik ke tempat makan
    public Ikan getAttractedFish() {
        for (Ikan ikan : listIkan) {
            if (ikan.isBeingCaught()) { // Menggunakan isBeingCaught sebagai flag attracted
                return ikan;
            }
        }
        // Tidak ada ikan yang sedang tertarik
        return null;
    }
}