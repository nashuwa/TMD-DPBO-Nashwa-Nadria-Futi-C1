package view;

import viewmodel.GameViewModel;
import viewmodel.TempatMakanViewModel;
import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class TempatMakanPanel extends JPanel implements PropertyChangeListener 
{
    private GameViewModel gameViewModel;

    public TempatMakanPanel(GameViewModel gameViewModel) {
        this.gameViewModel = gameViewModel;
        setOpaque(false);
        gameViewModel.addPropertyChangeListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawTempatMakan(g2d);

        g2d.dispose();
    }

    private void drawTempatMakan(Graphics2D g2d) {
        // Check if tempat makan exists
        if (gameViewModel.getTempatMakanViewModel() == null) {
            // Draw a simple visual tempat makan jika TempatMakanViewModel belum dibuat
            int x = 650; // Same as TEMPAT_MAKAN_X in IkanViewModel
            int y = 280; // Same as TEMPAT_MAKAN_Y in IkanViewModel
            int width = 60;
            int height = 60;

            // Draw simple bowl shape
            g2d.setColor(new Color(139, 69, 19)); // Brown color
            g2d.fillOval(x, y, width, height);
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(3));
            g2d.drawOval(x, y, width, height);
            return;
        }

        TempatMakanViewModel.TempatMakanViewData data = gameViewModel.getTempatMakanViewModel().getTempatMakanViewData();
        // Draw tempat makan image
        g2d.drawImage(data.image, data.posX, data.posY, data.width, data.height, this);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        repaint();
    }
}