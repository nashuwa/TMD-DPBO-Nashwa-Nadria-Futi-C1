/*
    Saya Nashwa Nadria Futi dengan NIM 2308130 mengerjakan evaluasi 
    Tugas Masa Depan dalam mata kuliah Desain dan Pemrograman Berorientasi 
    Objek untuk keberkahanNya maka saya tidak melakukan kecurangan seperti 
    yang telah dispesifikasikan. Aamiin.
 */

package view;

import viewmodel.GameViewModel;
import javax.swing.*;
import java.awt.*;

public class App {
    private static JFrame frame;
    private static CardLayout cardLayout;
    private static JPanel mainContainer;
    private static GameViewModel gameViewModel;
    private static MainMenuPanel mainMenuPanel;
    private static GamePanel gamePanel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            createAndShowGUI();
        });
    }

    private static void createAndShowGUI() {
        frame = new JFrame("Lilo si Kucing Rakus");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(820, 650); // Window size (800 + 20 border, 600 + 50 status bar)
        frame.setLocationRelativeTo(null);
        frame.setResizable(false);

        // Setup card layout for navigation
        cardLayout = new CardLayout();
        mainContainer = new JPanel(cardLayout);

        // Setup panels
        setupMainMenu();
        setupGamePanel();

        // Add panels to card layout
        mainContainer.add(mainMenuPanel, "MENU");
        mainContainer.add(gamePanel, "GAME");

        // Show menu first
        cardLayout.show(mainContainer, "MENU");

        frame.add(mainContainer);
        frame.setVisible(true);
    }

    private static void setupMainMenu() {
        mainMenuPanel = new MainMenuPanel();

        // Set event listeners
        mainMenuPanel.setStartGameListener(e -> {
            // Get player name and set it in GameViewModel
            String playerName = mainMenuPanel.getCurrentPlayerName();
            if (playerName == null || playerName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Mohon masukkan nama pemain!", "Nama Diperlukan",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (gameViewModel != null) {
                gameViewModel.setCurrentPlayerName(playerName);
            }

            startGame();
            cardLayout.show(mainContainer, "GAME");
        });

        mainMenuPanel.setExitGameListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    frame,
                    "Apakah Anda yakin ingin keluar?",
                    "Konfirmasi",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        });
    }

    private static void setupGamePanel() {
        gameViewModel = new GameViewModel();
        gameViewModel.setPanelDimensions(800, 600); // GAME AREA 800x600

        // Add property change listener to refresh leaderboard when game ends
        gameViewModel.addPropertyChangeListener(evt -> {
            if ("gameEnded".equals(evt.getPropertyName()) || "highScore".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    if (mainMenuPanel != null) {
                        mainMenuPanel.refreshLeaderboard();
                    }
                });
            }
        });

        // Create main game panel using the new GamePanel
        gamePanel = new GamePanel(gameViewModel);
        gamePanel.setPreferredSize(new Dimension(800, 600));
    }

    private static void startGame() {
        if (gamePanel != null) {
            gamePanel.startGame();
        }
    } // Method untuk kembali ke menu (bisa dipanggil dari game)

    public static void showMainMenu() {
        if (gamePanel != null) {
            gamePanel.stopGame();
            gamePanel.cleanup();
        }

        // Reset game state ketika kembali ke menu
        if (gameViewModel != null) {
            gameViewModel.resetGameState();
        }

        if (mainMenuPanel != null) {
            mainMenuPanel.refreshLeaderboard();
        }
        cardLayout.show(mainContainer, "MENU");
    }
}