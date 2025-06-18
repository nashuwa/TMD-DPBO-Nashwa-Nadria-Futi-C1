package model;

public class Ikan {
    private int posX;
    private int posY;
    private int width;
    private int height;
    private int fishType; // 0, 1, or 2 representing different fish types
    private boolean isBeingCaught = false;
    private final double attractSpeed = 3.0;
    private boolean beingCarried = false;
    private boolean beingDelivered = false; // Ikan sedang dibawa ke tempat makan
    private int velocityX;
    private boolean movingRight;
    private int moveSpeed;

    // constructor
    public Ikan(int posX, int posY, int width, int height, int fishType) {
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.fishType = fishType;
        this.isBeingCaught = false;
        this.beingCarried = false;
        this.beingDelivered = false;
        this.moveSpeed = 2;
        this.movingRight = true;
        this.velocityX = moveSpeed;
    } // Getter dan Setter

    public int getPosX() {
        return posX;
    }

    public void setPosX(int posX) {
        this.posX = posX;
    }

    public int getPosY() {
        return posY;
    }

    public void setPosY(int posY) {
        this.posY = posY;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getFishType() {
        return fishType;
    }

    public int getCenterX() {
        return posX + width / 2;
    }

    public int getCenterY() {
        return posY + height / 2;
    }

    public void setBeingCaught(boolean beingCaught) {
        this.isBeingCaught = beingCaught;
    }

    public void setBeingCarried(boolean beingCarried) {
        this.beingCarried = beingCarried;
    }

    public void setBeingDelivered(boolean beingDelivered) {
        this.beingDelivered = beingDelivered;
    }

    public boolean isBeingCarried() {
        return beingCarried;
    }

    public boolean isBeingDelivered() {
        return beingDelivered;
    }

    public int getVelocityX() {
        return velocityX;
    }

    public boolean isMovingRight() {
        return movingRight;
    }

    public int getMoveSpeed() {
        return moveSpeed;
    }

    public void setVelocityX(int velocityX) {
        this.velocityX = velocityX;
    }

    public void setMoveSpeed(int moveSpeed) {
        this.moveSpeed = moveSpeed;
        this.velocityX = movingRight ? moveSpeed : -moveSpeed;
    }

    public boolean isBeingCaught() {
        return isBeingCaught;
    }

    public void moveTowardsCat(int catCenterX, int catCenterY) {
        if (!isBeingCaught)
            return;

        double dx = catCenterX - getCenterX();
        double dy = catCenterY - getCenterY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > attractSpeed) {
            posX += (int) (dx / distance * attractSpeed);
            posY += (int) (dy / distance * attractSpeed);
        } else {
            // Ikan sampai di kucing
            posX = catCenterX - width / 2;
            posY = catCenterY - height / 2;
        }
    }

    public boolean hasReachedCat(int catCenterX, int catCenterY) {
        double distance = Math.sqrt(Math.pow(getCenterX() - catCenterX, 2) + Math.pow(getCenterY() - catCenterY, 2));
        return distance < 20; // Jika jarak kurang dari 20 pixel
    }

    // Method untuk mengecek apakah ikan tertangkap oleh tangan kucing
    public boolean isCollidingWith(int x, int y, int handWidth, int handHeight) {
        return x < posX + width && x + handWidth > posX && y < posY + height && y + handHeight > posY;
    }

    // Method untuk mengecek apakah ikan bisa ditangkap
    public boolean isAvailableForCatch() {
        return !isBeingCaught && !beingCarried && !beingDelivered;
    }

    public void freezeMovement() {
        this.velocityX = 0;
    }

    public void resumeMovement() {
        this.velocityX = movingRight ? moveSpeed : -moveSpeed;
    }

    public void updatePosition(int panelWidth) {
        if (isBeingCaught || beingCarried || beingDelivered) {
            return;
        }

        // gerak horizontal
        posX += velocityX;

        // Cek apakah ikan sudah keluar dari layar
        if (velocityX > 0 && posX > panelWidth + 100) {
            // kalau ikan bergerak ke kanan dan udah keluar - RESPAWN DI KIRI
            respawnFromLeft();
        } else if (velocityX < 0 && posX < -100) {
            // kalau ikan bergerak ke kiri dan udah keluar - RESPAWN DI KANAN
            respawnFromRight();
        }
    }

    // Method untuk mendapatkan skor berdasarkan jenis ikan
    public int getScore() {
        switch (fishType) {
            case 0:
                return 10; // Ikan jenis 1 = 10 poin
            case 1:
                return 20; // Ikan jenis 2 = 20 poin
            case 2:
                return 30; // Ikan jenis 3 = 30 poin
            default:
                return 10; // Default = 10 poin
        }
    }

    private void respawnFromLeft() {
        posX = -width - 50; // Start dari luar layar kiri
    }

    private void respawnFromRight() {
        posX = 850; // Start dari luar layar kanan
    }
}