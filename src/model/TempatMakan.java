package model;

import java.awt.Image;

public class TempatMakan {
    private int posX;
    private int posY;
    private int width;
    private int height;
    private Image image;
    private boolean isVisible;
    private int fishCount; // Jumlah ikan yang sudah dimakan

    public TempatMakan(int posX, int posY, int width, int height, Image image) {
        this.posX = posX;
        this.posY = posY;
        this.width = width;
        this.height = height;
        this.image = image;
        this.isVisible = false;
        this.fishCount = 0;
    }

    // Getter dan Setter
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

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Image getImage() {
        return image;
    }

    public boolean isVisible() {
        return isVisible;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    public int getFishCount() {
        return fishCount;
    }

    public void addFish() {
        this.fishCount++;
    }

    public void resetFishCount() {
        this.fishCount = 0;
    }

    // Method untuk mengecek apakah mouse berada di atas tempat makan
    public boolean isMouseOver(int mouseX, int mouseY) {
        return mouseX >= posX && mouseX <= posX + width &&
                mouseY >= posY && mouseY <= posY + height;
    }

    // Method untuk mendapatkan posisi center tempat makan
    public int getCenterX() {
        return posX + width / 2;
    }

    public int getCenterY() {
        return posY + height / 2;
    }
}