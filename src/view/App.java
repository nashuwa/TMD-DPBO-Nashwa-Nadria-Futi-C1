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

        setupMainMenu();

        // Add panels to card layout - hanya main menu dulu
        mainContainer.add(mainMenuPanel, "MENU");

        // Show menu first
        cardLayout.show(mainContainer, "MENU");

        frame.add(mainContainer);
        frame.setVisible(true);
    }

    // Method untuk setup MainMenuPanel
    // Ini akan menginisialisasi MainMenuPanel dan menambahkan listener untuk tombol
    private static void setupMainMenu() {
        mainMenuPanel = new MainMenuPanel(); // Set event listeners
        mainMenuPanel.setStartGameListener(e -> {
            // Get player name dan set ke GameViewModel
            // Jika nama kosong, tampilkan pesan peringatan
            String playerName = mainMenuPanel.getCurrentPlayerName();
            if (playerName == null || playerName.trim().isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Mohon masukkan nama pemain!", "Nama Diperlukan",
                JOptionPane.WARNING_MESSAGE);
                // pop up dialog untuk meminta nama pemain
                return;
            }

            createFreshGamePanel(playerName);

            cardLayout.show(mainContainer, "GAME"); // menampilkan GamePanel
        }); // Set listener untuk tombol mulai

        mainMenuPanel.setExitGameListener(e -> {
            int result = JOptionPane.showConfirmDialog(
                    frame,
                    "Apakah Anda yakin ingin keluar?",
                    "Konfirmasi",
                    JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                System.exit(0);
            }
        }); // Set listener untuk tombol keluar
    } 

    // Method untuk membuat GamePanel baru
    private static void createFreshGamePanel(String playerName) {
        
        // Cleanup existing game panel jika ada
        if (gamePanel != null) {
            gamePanel.cleanup(); // Bersihkan resources yang digunakan, seperti musik, skor, poin, dll
            mainContainer.remove(gamePanel); // Hapus dari container
            gamePanel = null;
        }

        // Buat GameViewModel baru
        gameViewModel = new GameViewModel(); // Inisialisasi GameViewModel baru
        gameViewModel.setPanelDimensions(800, 600); // Set ukuran panel game
        gameViewModel.setCurrentPlayerName(playerName); // Set nama pemain
        
        // Mengupdate leaderboard di menu utama secara otomatis ketika game selesai, waktu habis atau player stop. Mari saya jelaskan detail
        gameViewModel.addPropertyChangeListener(evt -> {
            if ("gameEnded".equals(evt.getPropertyName()) || "highScore".equals(evt.getPropertyName())) {
                SwingUtilities.invokeLater(() -> {
                    if (mainMenuPanel != null) {
                        mainMenuPanel.refreshLeaderboard(); // Refresh leaderboard di MainMenuPanel
                    }
                }); 
            }
        });

        // Buat game panel baru dengan GameViewModel
        gamePanel = new GamePanel(gameViewModel);
        gamePanel.setPreferredSize(new Dimension(800, 600));

        // Add to container
        mainContainer.add(gamePanel, "GAME");

        // Refresh container layout
        mainContainer.revalidate();
        mainContainer.repaint();

        // Memulai game
        gamePanel.startGame();

    }

    // Method untuk membuat GamePanel baru saat restart
    public static void showMainMenu() {
        
        if (gamePanel != null) {
            gamePanel.stopGame();
            gamePanel.cleanup();
            mainContainer.remove(gamePanel);
            gamePanel = null; // reset game
        }

        // Reset game state ketika kembali ke menu
        if (gameViewModel != null) {
            gameViewModel.resetGameState();
            gameViewModel = null;
        }

        if (mainMenuPanel != null) {
            mainMenuPanel.refreshLeaderboard();
        }
        cardLayout.show(mainContainer, "MENU"); // Tampilkan MainMenuPanel
    } 

    public static void restartGame() {
        
        // Get current player name if exists
        String currentPlayer = "";
        if (mainMenuPanel != null) {
            currentPlayer = mainMenuPanel.getCurrentPlayerName();
        }
        if (currentPlayer == null || currentPlayer.trim().isEmpty()) {
            currentPlayer = "Player"; // Fallback name
        }

        // Create fresh game panel
        createFreshGamePanel(currentPlayer);

        // Show game panel
        cardLayout.show(mainContainer, "GAME");

    }
}