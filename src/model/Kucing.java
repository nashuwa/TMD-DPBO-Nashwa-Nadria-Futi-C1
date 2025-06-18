package model;

import java.awt.Image;

public class Kucing {
    private int posX;
    private int posY;
    private int width;
    private int height;
    private Image imageRight;
    private Image imageLeft;
    private Image imageUp; // Gambar kucing menghadap atas
    private Image imageDown; // Gambar kucing menghadap bawah
    private Image imageX; // Gambar kucing saat aksi 'X'
    private Image currentImage; // Gambar kucing yang sedang ditampilkan

    private int velocityX;
    private int velocityY;

    // --- Tambahan untuk animasi tangan ---
    private boolean isHandActive = false;
    private boolean isHandReturning = false;
    private boolean isHandDelivering = false; // Tangan sedang mengantarkan ikan ke tempat makan
    private int handTargetX, handTargetY; // Titik target tangan (ke kursor)
    private int handCurrentX, handCurrentY; // Posisi tangan saat ini (untuk animasi halus)
    private final double handSpeed = 8.0; // Kecepatan gerakan tangan
    private final double handDeliverySpeed = 18.0; // Kecepatan saat mengantarkan ikan
    private Image imageHand; // Gambar tangan // Konstruktor baru dengan gambar atas/bawah dan imageX

    public Kucing(int posX, int posY, int width, int height, Image imageRight, Image imageLeft, Image imageUp, Image imageDown, Image imageX, Image imageHand) {
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.imageRight = imageRight;
        this.imageLeft = imageLeft;
        this.imageUp = imageUp;
        this.imageDown = imageDown;
        this.imageX = imageX; // Inisialisasi imageX
        this.currentImage = imageRight; // Default
        this.velocityX = 0;
        this.velocityY = 0;
        this.imageHand = imageHand;

        // Inisialisasi posisi tangan di tengah kucing
        this.handCurrentX = getCenterX();
        this.handCurrentY = getCenterY();
    }

    // --- Getter dan Setter untuk semua atribut ---

    public int getPosX() { return posX; }
    public void setPosX(int posX) { this.posX = posX; }
    public int getPosY() { return posY; }
    public void setPosY(int posY) { this.posY = posY; }
    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }
    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }
    public Image getImageRight() { return imageRight; }
    public void setImageRight(Image imageRight) { this.imageRight = imageRight; }
    public Image getImageLeft() { return imageLeft;}
    public void setImageLeft(Image imageLeft) { this.imageLeft = imageLeft; }
    public Image getImageUp() { return imageUp; }
    public void setImageUp(Image imageUp) { this.imageUp = imageUp; }
    public Image getImageDown() { return imageDown; }
    public void setImageDown(Image imageDown) { this.imageDown = imageDown; }
    public Image getImageX() { return imageX; }
    public void setImageX(Image imageX) { this.imageX = imageX; }

    public Image getCurrentImage() {
        if (isHandActive()) {
            return getImageX(); // kucingambil.png
        }
        if (velocityX > 0) {
            return getImageRight();
        } else if (velocityX < 0) {
            return getImageLeft();
        } else if (velocityY > 0) {
            return getImageDown();
        } else if (velocityY < 0) {
            return getImageUp();
        }
        return currentImage;
    }

    public void setCurrentImage(Image currentImage) { this.currentImage = currentImage; }

    public int getVelocityX() {  return velocityX; }

    public void setVelocityX(int velocityX) {
        this.velocityX = velocityX;
        // Atur currentImage berdasarkan arah, utamakan aksi 'X' jika aktif
        updateCurrentImageBasedOnVelocity();
    }

    public int getVelocityY() { return velocityY; }

    public void setVelocityY(int velocityY) {
        this.velocityY = velocityY;
        // Atur currentImage berdasarkan arah, utamakan aksi 'X' jika aktif
        updateCurrentImageBasedOnVelocity();
    }

    // Metode internal untuk mengupdate gambar berdasarkan kecepatan atau aksi 'X'
    private void updateCurrentImageBasedOnVelocity() {
        if (velocityY < 0) { // Bergerak ke atas
            setCurrentImage(imageUp);
        } else if (velocityY > 0) { // Bergerak ke bawah
            setCurrentImage(imageDown);
        } else if (velocityX > 0) { // Bergerak ke kanan (jika tidak ada gerakan vertikal)
            setCurrentImage(imageRight);
        } else if (velocityX < 0) { // Bergerak ke kiri (jika tidak ada gerakan vertikal)
            setCurrentImage(imageLeft);
        }
        // Jika kedua kecepatan 0, gambar tetap pada arah terakhir
    } // --- Getter untuk tangan ---

    public boolean isHandActive() { return isHandActive; }

    public boolean isHandDelivering() { return isHandDelivering; }

    public void setHandDelivering(boolean handDelivering) { this.isHandDelivering = handDelivering; }

    // Method untuk memulai delivery ke tempat makan
    public void startDeliveryToFoodBowl(int targetX, int targetY) {
        this.isHandDelivering = true;
        this.isHandActive = true;
        this.handTargetX = targetX;
        this.handTargetY = targetY;
    }

    public int getHandTargetX() { return handTargetX;}
    public int getHandTargetY() { return handTargetY; }
    public int getCenterX() { return posX + width / 2; }
    public int getCenterY() { return posY + height / 2; }
    public Image getImageHand() { return imageHand; }
    // Getter untuk posisi animasi tangan (untuk gerakan halus)
    public int getHandAnimX() { return handCurrentX; }
    public int getHandAnimY() { return handCurrentY; }

    public void setHandActive(boolean handActive) {
        if (!handActive && this.isHandActive && !isHandDelivering) {
            // Mulai animasi kembali ke kucing hanya jika tidak sedang delivery
            isHandReturning = true;
            handTargetX = getCenterX();
            handTargetY = getCenterY();
        } else if (handActive) {
            isHandReturning = false;
        }
        this.isHandActive = handActive;
    }

    public void setHandTarget(int targetX, int targetY) {
        if (isHandActive && !isHandDelivering) {
            // Hanya set target jika tidak sedang delivery, untuk gerakan instan ke kursor
            this.handTargetX = targetX;
            this.handTargetY = targetY;
            // Set posisi tangan langsung ke target untuk gerakan instan
            this.handCurrentX = targetX;
            this.handCurrentY = targetY;
        } else if (isHandDelivering) {
            // Saat delivery, target sudah diset oleh startDeliveryToFoodBowl
            this.handTargetX = targetX;
            this.handTargetY = targetY;
        }
    }

    public void updateHandAnimation() {
        if (isHandActive || isHandReturning) {
            if (isHandDelivering) {
                // Gerakan smooth untuk delivery ke tempat makan
                double dx = handTargetX - handCurrentX;
                double dy = handTargetY - handCurrentY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance > handDeliverySpeed) {
                    handCurrentX += (int) (dx / distance * handDeliverySpeed);
                    handCurrentY += (int) (dy / distance * handDeliverySpeed);
                } else {
                    handCurrentX = handTargetX;
                    handCurrentY = handTargetY;
                    // Delivery sampai di tempat makan - akan dihandle oleh ViewModel
                }
            } else {
                // Gerakan normal atau returning (smooth)
                double dx = handTargetX - handCurrentX;
                double dy = handTargetY - handCurrentY;
                double distance = Math.sqrt(dx * dx + dy * dy);

                if (distance > handSpeed && (isHandReturning || !isHandActive)) {
                    handCurrentX += (int) (dx / distance * handSpeed);
                    handCurrentY += (int) (dy / distance * handSpeed);
                } else {
                    handCurrentX = handTargetX;
                    handCurrentY = handTargetY;

                    // Jika sampai di kucing, hentikan animasi kembali
                    if (isHandReturning) {
                        isHandReturning = false;
                    }
                }
            }
        }
    }

    public boolean isHandReturning() {
        return isHandReturning;
    }

    public void updatePosition(int panelWidth, int panelHeight) {
        // Tentukan margin dari tepi panel (kotak transparan)
        int margin = 100; // 220 pixel dari tepi

        // Update posisi berdasarkan kecepatan
        posX += velocityX;
        posY += velocityY;

        // Batasi gerakan dengan margin
        if (posX < margin) {
            posX = margin;
        } else if (posX + width > panelWidth - margin) {
            posX = panelWidth - margin - width;
        }

        if (posY < margin) {
            posY = margin;
        } else if (posY + height > panelHeight - margin) {
            posY = panelHeight - margin - height;
        }

        // Update arah gambar berdasarkan kecepatan
        updateCurrentImageBasedOnVelocity();

        // Update animasi tangan jika aktif
        if (isHandActive) {
            updateHandAnimation();
        }
    }
}
