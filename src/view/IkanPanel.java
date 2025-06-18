package view;

import viewmodel.IkanViewModel;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

public class IkanPanel extends JPanel implements PropertyChangeListener {
    private IkanViewModel ikanViewModel;
    private Image[] fishImages; // Fish images for rendering

    public IkanPanel(IkanViewModel ikanViewModel) {
        this.ikanViewModel = ikanViewModel;
        setOpaque(false); // Transparent background

        // Load fish images
        loadFishImages();

        // Add property change listener
        ikanViewModel.addPropertyChangeListener(this);
    }

    private void loadFishImages() {
        try {
            fishImages = new Image[3];
            fishImages[0] = new ImageIcon(getClass().getResource("/assets/ikan1.png")).getImage();
            fishImages[1] = new ImageIcon(getClass().getResource("/assets/ikan2.png")).getImage();
            fishImages[2] = new ImageIcon(getClass().getResource("/assets/ikan3.png")).getImage();
        } catch (Exception e) {
            System.err.println("Error loading fish images: " + e.getMessage());
        }
    }

    public void drawAllIkan(Graphics2D g2d) {
        if (ikanViewModel != null && fishImages != null) {
            List<IkanViewModel.IkanViewData> ikanList = ikanViewModel.getIkanViewDataList();

            for (IkanViewModel.IkanViewData ikan : ikanList) {
                if (ikan != null && ikan.fishType >= 0 && ikan.fishType < fishImages.length) {
                    Image fishImage = fishImages[ikan.fishType];

                    if (fishImage != null) {
                        // HIGHLIGHT kalau lagi ditangkap
                        if (ikan.isBeingCaught) {
                            g2d.setColor(new Color(255, 255, 0, 100));
                            g2d.fillOval(ikan.posX - 5, ikan.posY - 5, ikan.width + 10, ikan.height + 10);
                        }

                        // Draw fish with proper orientation
                        if (ikan.velocityX < 0) {
                            // Moving left - normal orientation
                            g2d.drawImage(fishImage,
                                    ikan.posX, ikan.posY,
                                    ikan.width, ikan.height,
                                    this);
                        } else {
                            // Moving right - flip horizontally
                            g2d.drawImage(fishImage,
                                    ikan.posX + ikan.width, ikan.posY,
                                    -ikan.width, ikan.height, this);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // React to fish-related property changes
        if ("fishSpawned".equals(evt.getPropertyName()) ||
                "fishUpdated".equals(evt.getPropertyName()) ||
                "fishRemoved".equals(evt.getPropertyName()) ||
                "fishMovement".equals(evt.getPropertyName())) {
            repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawAllIkan(g2d);

        g2d.dispose();
    }

    // Method to cleanup listeners when panel is disposed
    public void cleanup() {
        if (ikanViewModel != null) {
            ikanViewModel.removePropertyChangeListener(this);
        }
    }
}
