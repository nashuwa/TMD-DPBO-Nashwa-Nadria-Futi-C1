package view;

import viewmodel.GameViewModel;
import viewmodel.KucingViewModelNew;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.awt.geom.AffineTransform;

public class KucingPanel extends JPanel implements PropertyChangeListener 
{
    private GameViewModel gameViewModel; 
    private IkanPanel ikanPanel;

    public KucingPanel(GameViewModel gameViewModel) {
        this.gameViewModel = gameViewModel;
        setFocusable(false); // GamePanel sekarang yang handle focus
        setOpaque(false); // Transparent background

        // Create fish panel untuk compatibility
        ikanPanel = new IkanPanel(gameViewModel.getIkanViewModel()); // Add property change listener
        gameViewModel.addPropertyChangeListener(this);
    }

    private void drawKucing(Graphics2D g2d) {
        KucingViewModelNew kucingViewModelNew = gameViewModel.getKucingViewModelNew();
        if (kucingViewModelNew != null) {
            KucingViewModelNew.KucingViewData kucingData = kucingViewModelNew.getKucingViewData();

            if (kucingData != null && kucingData.image != null) {
                g2d.drawImage(kucingData.image,
                        kucingData.posX, kucingData.posY,
                        kucingData.width, kucingData.height, this);
            }
        }
    }

    private void drawHand(Graphics2D g) {
        KucingViewModelNew kucingViewModelNew = gameViewModel.getKucingViewModelNew();
        if (kucingViewModelNew != null) {
            KucingViewModelNew.KucingViewData kucingData = kucingViewModelNew.getKucingViewData();
            if (kucingData != null && kucingData.isHandActive && kucingData.handImage != null) {
                // POSISI BADAN KUCING (CENTER)
                int x1 = kucingData.posX + kucingData.width / 2;
                int y1 = kucingData.posY + kucingData.height / 2;

                // POSISI TARGET TANGAN
                int x2 = kucingData.handX;
                int y2 = kucingData.handY;

                // HITUNG JARAK DAN SUDUT
                double dx = x2 - x1;
                double dy = y2 - y1;
                double length = Math.sqrt(dx * dx + dy * dy);
                double angle = Math.atan2(dy, dx);

                // UKURAN TANGAN
                int handW = 30; // Lebar tangan
                int handH = (int) length; // TINGGI = JARAK KE TARGET (STRETCH!)

                // GAMBAR TANGAN YANG DI-STRETCH
                Graphics2D g2d = (Graphics2D) g.create();
                AffineTransform old = g2d.getTransform();

                // PINDAH KE POSISI KUCING CENTER
                g2d.translate(x1, y1);

                // ROTATE SESUAI ARAH TARGET
                g2d.rotate(angle - Math.PI / 2); // -PI/2 karena PNG vertikal // GAMBAR TANGAN DARI (0,0) SAMPAI
                                                 // (0,handH) - STRETCHED!
                g2d.drawImage(kucingData.handImage, -handW / 2, 0, handW, handH, null);

                // RESTORE TRANSFORM
                g2d.setTransform(old);
                g2d.dispose();
            }
        }
    } // === PROPERTY CHANGE HANDLING ===

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        // KucingPanel sekarang hanya repaint untuk kucing-related changes
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        // KucingPanel sekarang hanya menggambar kucing dan tangan
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw tangan (hand)
        drawHand(g2d);
        // Draw kucing (cat body)
        drawKucing(g2d);

        g2d.dispose();
    }

    // Method to cleanup resources when panel is disposed
    public void cleanup() {
        if (ikanPanel != null) {
            ikanPanel.cleanup();
        }
        gameViewModel.removePropertyChangeListener(this);
    }
}