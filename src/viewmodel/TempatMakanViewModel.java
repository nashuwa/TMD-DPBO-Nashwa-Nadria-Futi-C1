package viewmodel;

import model.TempatMakan;
import java.awt.Image;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

public class TempatMakanViewModel {
    private TempatMakan tempatMakan;
    private PropertyChangeSupport support;

    public TempatMakanViewModel(TempatMakan tempatMakan) {
        this.tempatMakan = tempatMakan;
        this.support = new PropertyChangeSupport(this);
    }

    // Inner class untuk data yang diperlukan oleh View
    public static class TempatMakanViewData {
        public final int posX;
        public final int posY;
        public final int width;
        public final int height;
        public final Image image;
        public final boolean isVisible;
        public final int fishCount;
        public final int centerX;
        public final int centerY;

        public TempatMakanViewData(int posX, int posY, int width, int height,
                Image image, boolean isVisible, int fishCount,
                int centerX, int centerY) {
            this.posX = posX;
            this.posY = posY;
            this.width = width;
            this.height = height;
            this.image = image;
            this.isVisible = isVisible;
            this.fishCount = fishCount;
            this.centerX = centerX;
            this.centerY = centerY;
        }
    }

    // Method untuk mendapatkan data view
    public TempatMakanViewData getTempatMakanViewData() {
        return new TempatMakanViewData(
                tempatMakan.getPosX(),
                tempatMakan.getPosY(),
                tempatMakan.getWidth(),
                tempatMakan.getHeight(),
                tempatMakan.getImage(),
                tempatMakan.isVisible(),
                tempatMakan.getFishCount(),
                tempatMakan.getCenterX(),
                tempatMakan.getCenterY());
    }

    // Method untuk menambah ikan
    public void addFish() {
        int oldCount = tempatMakan.getFishCount();
        tempatMakan.addFish();
        support.firePropertyChange("fishCount", oldCount, tempatMakan.getFishCount());
        support.firePropertyChange("tempatMakanUpdate", false, true);
    }

    // Method untuk mengatur visibility
    public void setVisible(boolean visible) {
        boolean oldVisible = tempatMakan.isVisible();
        tempatMakan.setVisible(visible);
        support.firePropertyChange("visibility", oldVisible, visible);
        support.firePropertyChange("tempatMakanUpdate", false, true);
    }

    // Method untuk mengatur posisi
    public void setPosition(int x, int y) {
        tempatMakan.setPosX(x);
        tempatMakan.setPosY(y);
        support.firePropertyChange("position", null, null);
        support.firePropertyChange("tempatMakanUpdate", false, true);
    }

    // Method untuk reset fish count
    public void resetFishCount() {
        int oldCount = tempatMakan.getFishCount();
        tempatMakan.resetFishCount();
        support.firePropertyChange("fishCount", oldCount, 0);
        support.firePropertyChange("tempatMakanUpdate", false, true);
    }

    // Property change listener support
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        support.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        support.removePropertyChangeListener(listener);
    }

    // Getter untuk model (jika diperlukan)
    public TempatMakan getModel() {
        return tempatMakan;
    }
}