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

public class IkanViewModel implements ActionListener {
    private List<Ikan> listIkan;
    private final PropertyChangeSupport propertyChangeSupport;
    private final Random random;
    private Timer movementTimer;
    private static final int PANEL_WIDTH = 800;

    public IkanViewModel() {
        this.listIkan = new ArrayList<>();
        this.propertyChangeSupport = new PropertyChangeSupport(this);
        this.random = new Random();
        this.movementTimer = new Timer(1000 / 60, this);

        spawnInitialFish();
        startMovement();
    }

    public void spawnNewFish() {
        // Generate random fish type (0-2)
        int fishType = random.nextInt(3);

        int fishX, fishY;
        boolean isTopZone;
        int attempts = 0;
        int maxAttempts = 15;

        do {
            if (random.nextBoolean()) {
                // ZONE ATAS (0-150) - BERGERAK KANAN KE KIRI
                fishY = random.nextInt(120) + 15; // Random dalam zona atas: 15-135
                isTopZone = true;
                // Spawn dari kanan layar
                fishX = PANEL_WIDTH + random.nextInt(300) + 50; // Expanded spawn area: 850-1150
            } else {
                // ZONE BAWAH (450-600) - BERGERAK KIRI KE KANAN
                fishY = random.nextInt(120) + 465; // Random dalam zona bawah: 465-585
                isTopZone = false;
                // Spawn dari kiri layar
                fishX = -random.nextInt(300) - 50; // Expanded spawn area: -350 sampai -50
            }
            attempts++;
        } while (isTooCloseToOtherFish(fishX, fishY, 60, 50) && attempts < maxAttempts);

        // Jika setelah 15 percobaan masih terlalu dekat, skip spawn kali ini
        if (attempts >= maxAttempts) {
            return;
        }

        // Create fish with type information (View will load appropriate image)
        Ikan newFish = new Ikan(fishX, fishY, 60, 50, fishType);

        // SET ARAH BERGERAK SESUAI ZONE (KECEPATAN SAMA UNTUK SEMUA)
        int baseSpeed = 5; // Kecepatan tetap untuk semua ikan

        if (isTopZone) {
            // ATAS: Gerak ke kiri (kecepatan sama)
            newFish.setVelocityX(-baseSpeed);
        } else {
            // BAWAH: Gerak ke kanan (kecepatan sama)
            newFish.setVelocityX(baseSpeed);
        }

        listIkan.add(newFish);
        firePropertyChange("fishSpawned", null, newFish);
    }

    public void updateAllFish(int panelWidth) {
        for (Ikan ikan : listIkan) {
            ikan.updatePosition(panelWidth);
        }
        // Fire property change untuk trigger repaint
        firePropertyChange("fishUpdated", null, null);
    }

    private void spawnInitialFish() {
        // SPAWN IKAN AWAL DENGAN JUMLAH LEBIH SEDIKIT
        int initialFishCount = 8 + random.nextInt(3); // 4-6 ikan (dikurangi dari 8-12)
        for (int i = 0; i < initialFishCount; i++) {
            spawnNewFish();
        }
    }

    public Ikan findAvailableIkanAt(int x, int y, int width, int height) {
        for (Ikan ikan : listIkan) {
            if (ikan.isAvailableForCatch() && ikan.isCollidingWith(x, y, width, height)) {
                return ikan;
            }
        }
        return null;
    } // Method untuk mengatur ikan yang sedang dibawa

    public void setIkanBeingCarried(Ikan ikan, boolean beingCarried) {
        if (ikan != null) {
            ikan.setBeingCaught(beingCarried);
            if (beingCarried) {
                ikan.setVelocityX(0); // Stop normal movement
            }
        }
    }

    // Method untuk menghapus ikan dari list
    public void removeIkan(Ikan ikan) {
        if (ikan != null && listIkan.contains(ikan)) {
            listIkan.remove(ikan);
            firePropertyChange("fishRemoved", ikan, null);
        }
    }

    public void setIkanBeingCaught(Ikan ikan, boolean beingCaught) {
        if (ikan != null) {
            ikan.setBeingCaught(beingCaught);
        }
    }

    public void setIkanBeingDelivered(Ikan ikan, boolean beingDelivered) {
        if (ikan != null) {
            ikan.setBeingDelivered(beingDelivered);
        }
    }

    public void moveIkanToPosition(Ikan ikan, int x, int y) {
        if (ikan != null) {
            ikan.setPosX(x);
            ikan.setPosY(y);
        }
    }

    public static class IkanViewData {
        public final int posX, posY, width, height;
        public final int fishType;
        public final boolean isBeingCaught;
        public final int velocityX;
        public final boolean isMovingRight;

        public IkanViewData(Ikan ikan) {
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

    public List<IkanViewData> getIkanViewDataList() {
        List<IkanViewData> viewDataList = new ArrayList<>();
        for (Ikan ikan : listIkan) {
            viewDataList.add(new IkanViewData(ikan));
        }
        return viewDataList;
    }

    public int getAvailableIkanCount() {
        return listIkan.size();
    }

    public int getTotalIkanCount() {
        return listIkan.size();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }

    protected List<Ikan> getListIkan() {
        return listIkan;
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.removePropertyChangeListener(listener);
    }

    // Movement control methods
    public void startMovement() {
        if (movementTimer != null && !movementTimer.isRunning()) {
            movementTimer.start();
        }
    }

    public void stopMovement() {
        if (movementTimer != null && movementTimer.isRunning()) {
            movementTimer.stop();
        }
    } // Method untuk cek apakah posisi terlalu dekat dengan ikan lain

    private boolean isTooCloseToOtherFish(int newX, int newY, int newWidth, int newHeight) {
        final int MIN_DISTANCE = 120; // Increased minimum distance between fish

        for (Ikan existingFish : listIkan) {
            // Skip if existing fish is being interacted with
            if (existingFish.isBeingCaught() || existingFish.isBeingCarried() ||
                    existingFish.isBeingDelivered()) {
                continue;
            }

            // Calculate distance between fish centers
            int existingCenterX = existingFish.getPosX() + existingFish.getWidth() / 2;
            int existingCenterY = existingFish.getPosY() + existingFish.getHeight() / 2;
            int newCenterX = newX + newWidth / 2;
            int newCenterY = newY + newHeight / 2;

            double distance = calculateDistance(existingCenterX, existingCenterY, newCenterX, newCenterY);

            // If distance is less than minimum, return true (too close)
            if (distance < MIN_DISTANCE) {
                return true;
            }
        }
        return false; // Safe, not too close
    } // ActionListener implementation for automatic fish movement @Override

    public void actionPerformed(ActionEvent e) {
        updateAllFishMovement();
        // updateAttractedFish(); // Disabled - sekarang tangan yang membawa ikan
    }

    private void updateAllFishMovement() {
        List<Ikan> fishToRemove = new ArrayList<>();

        for (Ikan ikan : listIkan) {
            // Skip movement if fish is being caught, carried, or delivered
            if (ikan.isBeingCaught() || ikan.isBeingCarried() || ikan.isBeingDelivered()) {
                continue;
            }

            // Calculate new position
            int newX = ikan.getPosX() + ikan.getVelocityX();
            int newY = ikan.getPosY();

            // Enhanced collision detection with other fish
            boolean canMove = true;
            Ikan closestFish = null;
            double minDistance = Double.MAX_VALUE;

            for (Ikan otherFish : listIkan) {
                if (otherFish != ikan && !otherFish.isBeingCaught() &&
                        !otherFish.isBeingCarried() && !otherFish.isBeingDelivered()) {

                    // Calculate distance between fish centers
                    double distance = calculateDistance(
                            newX + ikan.getWidth() / 2, newY + ikan.getHeight() / 2,
                            otherFish.getPosX() + otherFish.getWidth() / 2,
                            otherFish.getPosY() + otherFish.getHeight() / 2);

                    // Track closest fish
                    if (distance < minDistance) {
                        minDistance = distance;
                        closestFish = otherFish;
                    }

                    // Check if too close (collision threshold)
                    if (distance < 85) { // Increased threshold for better spacing
                        canMove = false;
                        break;
                    }
                }
            }

            if (canMove) {
                // Safe to move
                ikan.setPosX(newX);
            } else {
                // Collision avoidance behavior
                avoidCollision(ikan, closestFish);
            }

            // Remove fish that have moved off screen
            if (ikan.getVelocityX() < 0 && ikan.getPosX() < -120) {
                // Fish moving left and off left edge
                fishToRemove.add(ikan);
            } else if (ikan.getVelocityX() > 0 && ikan.getPosX() > PANEL_WIDTH + 120) {
                // Fish moving right and off right edge
                fishToRemove.add(ikan);
            }
        }

        // Remove off-screen fish
        for (Ikan fish : fishToRemove) {
            listIkan.remove(fish);
        }

        // Spawn new fish if needed (maintain optimal count)
        while (listIkan.size() < 6) { // Maintain minimal 6 ikan
            spawnNewFish();
        }

        // Randomly spawn additional fish untuk variasi
        if (random.nextInt(250) < 2) { // Reduced spawn rate (0.8% chance)
            if (listIkan.size() < 9) { // Max 9 ikan (reduced from 10)
                spawnNewFish();
            }
        }

        // Fire property change to trigger UI update
        firePropertyChange("fishMovement", null, System.currentTimeMillis());
    }

    private double calculateDistance(int x1, int y1, int x2, int y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    }

    private void avoidCollision(Ikan ikan, Ikan closestFish) {
        if (closestFish == null)
            return;

        // Get current positions
        int fishX = ikan.getPosX();
        int fishY = ikan.getPosY();
        int otherY = closestFish.getPosY();

        // Calculate avoidance maneuver
        if (ikan.getVelocityX() > 0) { // Moving right
            if (fishY < otherY) {
                // Move up to avoid collision
                int newY = Math.max(fishY - 15, getMinYForZone(fishY));
                ikan.setPosY(newY);
            } else {
                // Move down to avoid collision
                int newY = Math.min(fishY + 15, getMaxYForZone(fishY));
                ikan.setPosY(newY);
            }
        } else { // Moving left
            if (fishY < otherY) {
                // Move up to avoid collision
                int newY = Math.max(fishY - 15, getMinYForZone(fishY));
                ikan.setPosY(newY);
            } else {
                // Move down to avoid collision
                int newY = Math.min(fishY + 15, getMaxYForZone(fishY));
                ikan.setPosY(newY);
            }
        }

        // Add small horizontal adjustment to avoid getting stuck
        int horizontalAdjust = random.nextInt(10) - 5; // -5 to +5
        ikan.setPosX(fishX + horizontalAdjust);
    }

    private int getMinYForZone(int currentY) {
        if (currentY < 150) {
            return 15; // Top zone minimum
        } else {
            return 465; // Bottom zone minimum
        }
    }

    private int getMaxYForZone(int currentY) {
        if (currentY < 150) {
            return 135; // Top zone maximum
        } else {
            return 585; // Bottom zone maximum
        }
    }

    // Method untuk mendeteksi ikan yang diklik
    public Ikan findClickedFish(int mouseX, int mouseY) {
        for (Ikan ikan : listIkan) {
            if (ikan.isAvailableForCatch()) {
                // Check if mouse click is within fish bounds
                if (mouseX >= ikan.getPosX() && mouseX <= ikan.getPosX() + ikan.getWidth() &&
                        mouseY >= ikan.getPosY() && mouseY <= ikan.getPosY() + ikan.getHeight()) {
                    return ikan;
                }
            }
        }
        return null;
    } // Method untuk mendapatkan ikan yang sedang tertarik ke tempat makan

    public Ikan getAttractedFish() {
        for (Ikan ikan : listIkan) {
            if (ikan.isBeingCaught()) { // Menggunakan isBeingCaught sebagai flag attracted
                return ikan;
            }
        }
        return null;
    }
}