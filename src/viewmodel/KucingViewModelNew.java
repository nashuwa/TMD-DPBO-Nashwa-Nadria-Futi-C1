package viewmodel;

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
 * KucingViewModelNew - Unified cat view model with complete game logic
 * Combines cat movement, fish management, and delivery system
 */
public class KucingViewModelNew implements ActionListener {
    // Konstanta untuk zona kucing - ZONA HIJAU (area permainan kucing)
    private static final int ZONE_TOP_LIMIT = 200; // Sedikit di bawah jembatan
    private static final int ZONE_BOTTOM_LIMIT = 360; // Sampai batas bawah zona hijau, sebelum air

    private Kucing kucing;
    private final PropertyChangeSupport support = new PropertyChangeSupport(this);
    private Timer animationTimer;
    private Timer gameLoopTimer;

    // Images untuk kucing
    private Image catImageRight;
    private Image catImageLeft;
    private Image catImageUp;
    private Image catImageDown;
    private Image catImageX;
    private Image catImageHand;

    // Panel dimensions
    private int panelWidth = 800;
    private int panelHeight = 600; // Full panel height

    // Animation states
    private int handTargetX = 0;
    private int handTargetY = 0;

    // Fish management from KucingViewModel
    private List<Ikan> listIkan;
    private Image[] fishImages;
    private Image tempatMakanImage;
    private Random random;
    private final int MAX_FISH = 10;
    private final int MIN_FISH = 8;
    private int fishSpawnTimer = 0;
    private final int FISH_SPAWN_INTERVAL = 120;
    private TempatMakan tempatMakan;
    private Ikan carriedFish = null; // Ikan yang sedang dibawa kucing
    private boolean isCarryingFish = false;
    private int fishDelivered = 0;

    public KucingViewModelNew() {
        loadCatImages();
        initializeKucing();
        setupAnimationTimer();
        gameLoopTimer = new Timer(1000 / 60, this);

        // Initialize fish management
        listIkan = new ArrayList<>();
        random = new Random();

        // Spawn ikan awal
        spawnInitialFish();
    }

    private void loadCatImages() {
        try {
            catImageRight = new ImageIcon(getClass().getResource("/assets/kucingkanan.png")).getImage();
            catImageLeft = new ImageIcon(getClass().getResource("/assets/kucingkiri.png")).getImage();
            catImageUp = new ImageIcon(getClass().getResource("/assets/kucingbelakang.png")).getImage();
            catImageDown = new ImageIcon(getClass().getResource("/assets/kucingdepan.png")).getImage();
            catImageX = new ImageIcon(getClass().getResource("/assets/kucingambil.png")).getImage();
            catImageHand = new ImageIcon(getClass().getResource("/assets/tangan.png")).getImage();

            // Load fish images
            fishImages = new Image[3];
            fishImages[0] = new ImageIcon(getClass().getResource("/assets/ikan1.png")).getImage();
            fishImages[1] = new ImageIcon(getClass().getResource("/assets/ikan2.png")).getImage();
            fishImages[2] = new ImageIcon(getClass().getResource("/assets/ikan3.png")).getImage();

            // Load tempat makan image
            tempatMakanImage = new ImageIcon(getClass().getResource("/assets/tempatmakan.png")).getImage();
        } catch (Exception e) {
            System.err.println("Failed to load cat images: " + e.getMessage());
        }
    }

    public void startGameLoop() {
        gameLoopTimer.start();
    }

    private void initializeKucing() {
        // Spawn kucing di zona hijau (area bawah - area permainan kucing)
        int startX = 100; // Posisi awal X
        int startY = ZONE_TOP_LIMIT + 30; // Di zona hijau, agak ke bawah dari batas atas

        kucing = new Kucing(startX, startY, 70, 60,
                catImageRight, catImageLeft, catImageUp, catImageDown,
                catImageX, catImageHand);
    }

    private void setupAnimationTimer() {
        animationTimer = new Timer(1000 / 60, this); // 60 FPS
    }

    public void startAnimation() {
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    public void stopAnimation() {
        if (animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }

    public void setPanelDimensions(int width, int height) {
        int oldWidth = this.panelWidth;
        int oldHeight = this.panelHeight;

        // Gunakan ukuran panel yang sebenarnya, jangan dipaksa
        this.panelWidth = width;
        this.panelHeight = height;

        // Pastikan kucing tidak keluar dari boundaries dan tetap di zona hijau
        if (kucing != null) {
            int kucingX = kucing.getPosX();
            int kucingY = kucing.getPosY();

            // Batasi horizontal
            if (kucingX + kucing.getWidth() > this.panelWidth) {
                kucing.setPosX(this.panelWidth - kucing.getWidth());
            }

            // Batasi vertikal agar tetap di zona hijau (area bawah)
            int minY = ZONE_TOP_LIMIT;
            int maxY = ZONE_BOTTOM_LIMIT - kucing.getHeight();

            if (kucingY < minY) {
                kucing.setPosY(minY);
            } else if (kucingY > maxY) {
                kucing.setPosY(maxY);
            }
        }

        // Create tempat makan if panel dimensions are valid
        if (this.panelWidth > 0 && this.panelHeight > 0) {
            // Ukuran business logic tempat makan (untuk collision detection)
            int tempatMakanWidth = 100; // Business size
            int tempatMakanHeight = 80; // Business size

            // Posisi tempat makan di zona hijau (kanan-tengah)
            // Zona hijau: Y = 300-500
            int greenZoneTop = ZONE_TOP_LIMIT;
            int greenZoneBottom = ZONE_BOTTOM_LIMIT;
            int greenZoneHeight = greenZoneBottom - greenZoneTop; // 200px

            // Letakkan tempat makan di kanan zona hijau, vertikal di tengah
            int tempatMakanX = this.panelWidth - tempatMakanWidth - 50; // 50px dari kanan
            int tempatMakanY = greenZoneTop + (greenZoneHeight - tempatMakanHeight) / 2; // Tengah vertikal zona hijau

            // Buat tempat makan baru atau update yang sudah ada
            tempatMakan = new TempatMakan(tempatMakanX, tempatMakanY, tempatMakanWidth, tempatMakanHeight,
                    tempatMakanImage);

            // Fire property change agar UI update
            support.firePropertyChange("tempatMakanCreated", null, tempatMakan);
        }

        support.firePropertyChange("panelDimensions",
                new java.awt.Dimension(oldWidth, oldHeight),
                new java.awt.Dimension(this.panelWidth, this.panelHeight));
    }

    // Movement control methods
    public void setKucingVelocity(int vx, int vy) {
        if (kucing != null) {
            kucing.setVelocityX(vx);
            kucing.setVelocityY(vy);
            support.firePropertyChange("kucingVelocity", null, new int[] { vx, vy });
        }
    }

    public void moveKucing(int deltaX, int deltaY) {
        if (kucing != null) {
            int oldX = kucing.getPosX();
            int oldY = kucing.getPosY();

            // Hitung posisi baru
            int newX = Math.max(0, Math.min(panelWidth - kucing.getWidth(), oldX + deltaX));
            int newY = oldY + deltaY;

            // Batasi pergerakan vertikal agar kucing tidak keluar dari zona tengah
            int minY = ZONE_TOP_LIMIT;
            int maxY = ZONE_BOTTOM_LIMIT - kucing.getHeight();

            newY = Math.max(minY, Math.min(maxY, newY));

            kucing.setPosX(newX);
            kucing.setPosY(newY);

            // Update direction image based on movement
            updateKucingDirection(deltaX, deltaY);

            support.firePropertyChange("kucingPosition",
                    new java.awt.Point(oldX, oldY),
                    new java.awt.Point(newX, newY));
        }
    }

    private void updateKucingDirection(int deltaX, int deltaY) {
        if (kucing == null)
            return;

        if (Math.abs(deltaX) > Math.abs(deltaY)) {
            // Horizontal movement dominates
            if (deltaX > 0) {
                kucing.setCurrentImage(catImageRight);
            } else if (deltaX < 0) {
                kucing.setCurrentImage(catImageLeft);
            }
        } else if (deltaY != 0) {
            // Vertical movement dominates
            if (deltaY > 0) {
                kucing.setCurrentImage(catImageDown);
            } else {
                kucing.setCurrentImage(catImageUp);
            }
        }
    } // Hand animation methods

    public void setHandActive(boolean active) {
        if (kucing != null && !kucing.isHandDelivering()) { // Only allow manual control if not delivering
            kucing.setHandActive(active);
            support.firePropertyChange("handActive", !active, active);
        }
    }

    public void setHandTarget(int x, int y) {
        this.handTargetX = x;
        this.handTargetY = y;

        if (kucing != null && !kucing.isHandDelivering()) { // Only allow manual control if not delivering
            kucing.setHandTarget(x, y);
            support.firePropertyChange("handTarget", null, new java.awt.Point(handTargetX, handTargetY));
        }
    }

    public void performHandAction() {
        if (kucing != null && kucing.isHandActive()) {
            kucing.setCurrentImage(catImageX); // Show grabbing animation
            support.firePropertyChange("handAction", false, true);
        }
    }

    // Collision detection helper
    public boolean isKucingCollidingWith(int x, int y, int width, int height) {
        if (kucing == null)
            return false;

        return kucing.getPosX() < x + width &&
                kucing.getPosX() + kucing.getWidth() > x &&
                kucing.getPosY() < y + height &&
                kucing.getPosY() + kucing.getHeight() > y;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Update kucing movement based on velocity
        if (kucing != null && panelWidth > 0 && panelHeight > 0) {
            // Gunakan updatePosition dari model Kucing, tapi dengan boundaries yang benar
            int oldX = kucing.getPosX();
            int oldY = kucing.getPosY();

            // Update posisi berdasarkan velocity
            int vx = kucing.getVelocityX();
            int vy = kucing.getVelocityY();

            if (vx != 0 || vy != 0) {
                // Hitung posisi baru
                int newX = oldX + vx;
                int newY = oldY + vy;

                // Batasi horizontal
                newX = Math.max(0, Math.min(panelWidth - kucing.getWidth(), newX));

                // Batasi vertikal agar tetap di zona hijau
                int minY = ZONE_TOP_LIMIT;
                int maxY = ZONE_BOTTOM_LIMIT - kucing.getHeight();
                newY = Math.max(minY, Math.min(maxY, newY));

                // Set posisi baru
                kucing.setPosX(newX);
                kucing.setPosY(newY);

                // Update direction image based on movement
                updateKucingDirection(vx, vy);
            }

            // Update hand animation
            kucing.updateHandAnimation();

            // Update fish system
            checkFishCollision();
            updateCarriedFish();
            checkTempatMakanInteraction();

            // Spawn ikan baru jika kurang dari minimum
            fishSpawnTimer++;
            if (fishSpawnTimer >= FISH_SPAWN_INTERVAL && listIkan.size() < MIN_FISH) {
                spawnRandomFish();
                fishSpawnTimer = 0;
            }

            support.firePropertyChange("kucingPosition", null, kucing);
        }

        // Notify views to repaint
        support.firePropertyChange("animationUpdate", false, true);
    }

    // Data access methods for View (MVVM compliant)
    public KucingViewData getKucingViewData() {
        if (kucing == null) {
            return null;
        }
        return new KucingViewData(
                kucing.getPosX(),
                kucing.getPosY(),
                kucing.getWidth(),
                kucing.getHeight(),
                kucing.getCurrentImage(),
                kucing.isHandActive(),
                kucing.getHandAnimX(),
                kucing.getHandAnimY(),
                kucing.getImageHand(),
                kucing.getVelocityX(),
                kucing.getVelocityY());
    }

    // Getters for individual properties
    public int getKucingX() {
        return kucing != null ? kucing.getPosX() : 0;
    }

    public int getKucingY() {
        return kucing != null ? kucing.getPosY() : 0;
    }

    public int getKucingWidth() {
        return kucing != null ? kucing.getWidth() : 0;
    }

    public int getKucingHeight() {
        return kucing != null ? kucing.getHeight() : 0;
    }

    public Image getKucingImage() {
        if (kucing != null) {
            if (kucing.isHandActive()) {
                return catImageX; // kucingambil.png saat tangan aktif
            } else {
                return kucing.getCurrentImage();
            }
        }
        return catImageRight;
    }

    public boolean isHandActive() {
        return kucing != null && kucing.isHandActive();
    }

    public int getHandX() {
        return kucing != null ? kucing.getHandAnimX() : 0;
    }

    public int getHandY() {
        return kucing != null ? kucing.getHandAnimY() : 0;
    }

    public Image getHandImage() {
        return kucing != null ? kucing.getImageHand() : catImageHand;
    } // Additional getter methods for View compatibility

    public int getKucingPosX() {
        return kucing != null ? kucing.getPosX() : 0;
    }

    public int getKucingPosY() {
        return kucing != null ? kucing.getPosY() : 0;
    }

    public int getKucingHandAnimX() {
        return kucing != null ? kucing.getHandAnimX() : 0;
    }

    public int getKucingHandAnimY() {
        return kucing != null ? kucing.getHandAnimY() : 0;
    }

    public Image getKucingHandImage() {
        return kucing != null ? kucing.getImageHand() : null;
    }

    public int getKucingVelocityX() {
        return kucing != null ? kucing.getVelocityX() : 0;
    }

    public int getKucingVelocityY() {
        return kucing != null ? kucing.getVelocityY() : 0;
    }

    public boolean isKucingHandActive() {
        return kucing != null && kucing.isHandActive();
    }

    public boolean isKucingHandDelivering() {
        return kucing != null && kucing.isHandDelivering();
    } // Method untuk mendapatkan data ikan tanpa akses langsung ke model

    public java.util.List<IkanViewData> getIkanViewData() {
        java.util.List<IkanViewData> ikanData = new java.util.ArrayList<>();
        for (Ikan ikan : listIkan) {
            // Semua ikan di listIkan adalah ikan yang "hidup" (bisa ditampilkan)
            // Get fish image based on fish type
            Image fishImage = null;
            if (ikan.getFishType() >= 0 && ikan.getFishType() < fishImages.length) {
                fishImage = fishImages[ikan.getFishType()];
            }

            ikanData.add(new IkanViewData(
                    ikan.getPosX(),
                    ikan.getPosY(),
                    ikan.getWidth(),
                    ikan.getHeight(),
                    fishImage));
        }
        return ikanData;
    }

    // Method untuk mendapatkan data tempat makan (MVVM compliant)
    public TempatMakanViewData getTempatMakanViewData() {
        if (tempatMakan != null) {
            return new TempatMakanViewData(
                    tempatMakan.getPosX(),
                    tempatMakan.getPosY(),
                    tempatMakan.getWidth(), // Gunakan business size dari Model
                    tempatMakan.getHeight(), // Gunakan business size dari Model
                    tempatMakan.getImage(),
                    tempatMakan.isVisible());
        }
        return null;
    }

    // Property change listener support
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    // Method untuk akses ke model (untuk keperluan GameViewModel)
    public Kucing getModel() {
        return kucing;
    }

    public boolean isHandDelivering() {
        return kucing != null && kucing.isHandDelivering();
    }

    // Inner class for data transfer to View
    public static class KucingViewData {
        public final int posX;
        public final int posY;
        public final int width;
        public final int height;
        public final Image image;
        public final boolean isHandActive;
        public final int handX;
        public final int handY;
        public final Image handImage;
        public final int velocityX;
        public final int velocityY;

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

    // Fish management methods from KucingViewModel

    // Method untuk spawn ikan awal
    private void spawnInitialFish() {
        for (int i = 0; i < MIN_FISH; i++) {
            spawnRandomFish();
        }
    }

    private void spawnRandomFish() {
        if (listIkan.size() >= MAX_FISH || panelWidth <= 0 || panelHeight <= 0) {
            return;
        }

        int fishWidth = 60;
        int fishHeight = 50;
        int margin = 50;

        // Spawn ikan hanya di zona hijau (area kucing)
        int minX = margin;
        int maxX = panelWidth - fishWidth - margin;
        int minY = ZONE_TOP_LIMIT + margin;
        int maxY = ZONE_BOTTOM_LIMIT - fishHeight - margin;

        int x = random.nextInt(maxX - minX) + minX;
        int y = random.nextInt(maxY - minY) + minY;

        // Pastikan tidak spawn dekat kucing
        if (kucing != null) {
            while (Math.abs(x - kucing.getPosX()) < 100 && Math.abs(y - kucing.getPosY()) < 100) {
                x = random.nextInt(maxX - minX) + minX;
                y = random.nextInt(maxY - minY) + minY;
            }
        } // Pilih tipe ikan secara random dari 3 pilihan
        int randomFishType = random.nextInt(3);

        Ikan ikan = new Ikan(x, y, fishWidth, fishHeight, randomFishType);
        listIkan.add(ikan);
    }

    // Method untuk mengecek collision dan menghapus ikan yang tertangkap
    private void checkFishCollision() {
        if (kucing == null || !kucing.isHandActive() || isCarryingFish) {
            return;
        }

        int handX = kucing.getHandAnimX();
        int handY = kucing.getHandAnimY();
        for (Ikan ikan : listIkan) {
            if (ikan.isAvailableForCatch() &&
                    ikan.isCollidingWith(handX - 15, handY - 15, 30, 30)) {
                // Tangkap ikan pertama yang ditemukan
                ikan.setBeingCarried(true);
                carriedFish = ikan;
                isCarryingFish = true;

                // Mulai proses delivery otomatis ke tempat makan
                if (tempatMakan != null) {
                    kucing.startDeliveryToFoodBowl(tempatMakan.getCenterX(), tempatMakan.getCenterY());
                    // Keep the hand active during delivery
                    kucing.setHandActive(true); // Ensure hand remains active
                }
                break; // Hanya tangkap satu ikan
            }
        }
    }

    private void updateCarriedFish() {
        if (carriedFish != null && isCarryingFish) {
            if (kucing.isHandDelivering()) {
                // Saat delivery, ikan mengikuti tangan
                carriedFish.setPosX(kucing.getHandAnimX() - carriedFish.getWidth() / 2);
                carriedFish.setPosY(kucing.getHandAnimY() - carriedFish.getHeight() / 2);
            } else {
                // This part is for when the fish is carried but not yet in delivery animation
                int targetX = kucing.getPosX() + kucing.getWidth() / 2 - carriedFish.getWidth() / 2;
                int targetY = kucing.getPosY() + kucing.getHeight() / 2 - carriedFish.getHeight() / 2;

                int currentX = carriedFish.getPosX();
                int currentY = carriedFish.getPosY();

                int deltaX = targetX - currentX;
                int deltaY = targetY - currentY;

                double speed = 0.3;
                int newX = currentX + (int) (deltaX * speed);
                int newY = currentY + (int) (deltaY * speed);

                carriedFish.setPosX(newX);
                carriedFish.setPosY(newY);
            }
        }
    }

    private void checkTempatMakanInteraction() {
        if (carriedFish != null && isCarryingFish && tempatMakan != null && kucing.isHandDelivering()) {
            // Calculate the distance between the cat's hand and the food bowl
            int handX = kucing.getHandAnimX();
            int handY = kucing.getHandAnimY();

            int tempatMakanCenterX = tempatMakan.getCenterX();
            int tempatMakanCenterY = tempatMakan.getCenterY();

            double distance = Math
                    .sqrt(Math.pow(handX - tempatMakanCenterX, 2) + Math.pow(handY - tempatMakanCenterY, 2));

            // If the hand is close enough to the food bowl (within 30 pixels radius)
            if (distance < 30) {
                // Place the fish in the food bowl
                listIkan.remove(carriedFish);
                carriedFish = null;
                isCarryingFish = false;
                fishDelivered++; // Increase score

                // Reset delivery mode - important to allow hand control again
                kucing.setHandDelivering(false);
                kucing.setHandActive(false); // Hand goes inactive AFTER delivery is DONE

                // Fire property change to update the UI
                support.firePropertyChange("fishDelivered", fishDelivered - 1, fishDelivered);

                // Spawn a new fish to replace the one delivered
                spawnRandomFish();
            }
        }
    }

    // Method untuk mendapatkan status apakah kucing sedang membawa ikan
    public boolean isCarryingFish() {
        return isCarryingFish;
    }

    // Method untuk mendapatkan ikan yang sedang dibawa
    public Ikan getCarriedFish() {
        return carriedFish;
    }

    // Method untuk mengatur hover state tempat makan
    public void setTempatMakanHover(int mouseX, int mouseY) {
        if (tempatMakan != null) {
            boolean wasVisible = tempatMakan.isVisible();
            boolean isHovering = tempatMakan.isMouseOver(mouseX, mouseY);

            if (isHovering != wasVisible) {
                tempatMakan.setVisible(isHovering);
                support.firePropertyChange("tempatMakanHover", wasVisible, isHovering);
            }
        }
    }

    public int getFishDelivered() {
        return fishDelivered;
    }

    // Inner classes for data transfer from KucingViewModel
    public static class IkanViewData {
        public final int posX, posY, width, height;
        public final Image image;

        public IkanViewData(int posX, int posY, int width, int height, Image image) {
            this.posX = posX;
            this.posY = posY;
            this.width = width;
            this.height = height;
            this.image = image;
        }
    }

    public static class TempatMakanViewData {
        public final int posX, posY, width, height;
        public final Image image;
        public final boolean isVisible;

        public TempatMakanViewData(int posX, int posY, int width, int height, Image image, boolean isVisible) {
            this.posX = posX;
            this.posY = posY;
            this.width = width;
            this.height = height;
            this.image = image;
            this.isVisible = isVisible;
        }
    }
}