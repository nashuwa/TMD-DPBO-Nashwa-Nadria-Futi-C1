package model;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:gamedata.db";
    private static DatabaseManager instance;

    private DatabaseManager() {
        try {
            // Load SQLite JDBC driver
            Class.forName("org.sqlite.JDBC");
            System.out.println("SQLite JDBC driver loaded successfully");
        } catch (ClassNotFoundException e) {
            System.err.println("Error loading SQLite JDBC driver: " + e.getMessage());
        }
        initializeDatabase();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    private void initializeDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            String createTableSQL = """
                        CREATE TABLE IF NOT EXISTS players (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            name TEXT NOT NULL UNIQUE,
                            high_score INTEGER DEFAULT 0,
                            high_fish_count INTEGER DEFAULT 0,
                            games_played INTEGER DEFAULT 0,
                            last_played TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                        )
                    """;

            Statement stmt = conn.createStatement();
            stmt.execute(createTableSQL);

            // Add new column if it doesn't exist (for existing databases)
            try {
                stmt.execute("ALTER TABLE players ADD COLUMN high_fish_count INTEGER DEFAULT 0");
            } catch (SQLException e) {
                // Column already exists, which is fine
            }

            System.out.println("Database initialized successfully");
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
        }
    }

    public boolean addPlayer(String name) {
        String sql = "INSERT INTO players (name) VALUES (?)";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding player: " + e.getMessage());
            return false;
        }
    }

    public boolean updateHighScore(String playerName, int score) {
        // First, update games_played counter
        String updateGamesSQL = "UPDATE players SET games_played = games_played + 1, last_played = CURRENT_TIMESTAMP WHERE name = ?";

        // Then, update high score only if the new score is higher
        String updateScoreSQL = "UPDATE players SET high_score = ? WHERE name = ? AND high_score < ?";

        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            // Update games played count
            try (PreparedStatement pstmt1 = conn.prepareStatement(updateGamesSQL)) {
                pstmt1.setString(1, playerName);
                pstmt1.executeUpdate();
            }

            try (PreparedStatement pstmt2 = conn.prepareStatement(updateScoreSQL)) {
                pstmt2.setInt(1, score);
                pstmt2.setString(2, playerName);
                pstmt2.setInt(3, score);

                int rowsAffected = pstmt2.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error updating high score: " + e.getMessage());
            return false;
        }
    }

    public int getHighScore(String playerName) {
        String sql = "SELECT high_score FROM players WHERE name = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("high_score");
            }
        } catch (SQLException e) {
            System.err.println("Error getting high score: " + e.getMessage());
        }
        return 0;
    }

    public int getHighFishCount(String playerName) {
        String sql = "SELECT high_fish_count FROM players WHERE name = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("high_fish_count");
            }
        } catch (SQLException e) {
            System.err.println("Error getting high fish count: " + e.getMessage());
        }
        return 0;
    }

    public List<Player> getTopPlayers(int limit) {
        List<Player> topPlayers = new ArrayList<>();
        String sql = "SELECT name, high_score, high_fish_count FROM players ORDER BY high_score DESC LIMIT ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                topPlayers.add(new Player(
                        rs.getString("name"),
                        rs.getInt("high_score"),
                        rs.getInt("high_fish_count")));
            }
        } catch (SQLException e) {
            System.err.println("Error getting top players: " + e.getMessage());
        }
        return topPlayers;
    }

    public boolean playerExists(String name) {
        String sql = "SELECT COUNT(*) FROM players WHERE name = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking player existence: " + e.getMessage());
        }
        return false;
    }

    public boolean recordGameScore(String playerName, int score, int fishCount) {
        // This method records any score and fish count, regardless of whether they're
        // high scores
        String sql = "UPDATE players SET games_played = games_played + 1, last_played = CURRENT_TIMESTAMP WHERE name = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, playerName);
            int rowsAffected = pstmt.executeUpdate(); // Now check if this is a new high score and update if necessary
            if (score > getHighScore(playerName)) {
                updateHighScore(playerName, score);
            }

            // Check if this is a new high fish count and update if necessary
            if (fishCount > getHighFishCount(playerName)) {
                updateHighFishCount(playerName, fishCount);
            }

            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error recording game score: " + e.getMessage());
            return false;
        }
    }

    // Backward compatibility method
    public boolean recordGameScore(String playerName, int score) {
        return recordGameScore(playerName, score, 0);
    }

    public boolean updateHighFishCount(String playerName, int fishCount) {
        // Update high fish count only if the new count is higher
        String updateFishCountSQL = "UPDATE players SET high_fish_count = ? WHERE name = ? AND high_fish_count < ?";

        try (Connection conn = DriverManager.getConnection(DB_URL);
                PreparedStatement pstmt = conn.prepareStatement(updateFishCountSQL)) {

            pstmt.setInt(1, fishCount);
            pstmt.setString(2, playerName);
            pstmt.setInt(3, fishCount);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("Error updating high fish count: " + e.getMessage());
            return false;
        }
    }

    // Inner class data player
    public static class Player {
        private String name;
        private int highScore;
        private int highFishCount;

        public Player(String name, int highScore) {
            this.name = name;
            this.highScore = highScore;
            this.highFishCount = 0;
        }

        public Player(String name, int highScore, int highFishCount) {
            this.name = name;
            this.highScore = highScore;
            this.highFishCount = highFishCount;
        }

        public String getName() {
            return name;
        }

        public int getHighScore() {
            return highScore;
        }

        public int getHighFishCount() {
            return highFishCount;
        }

        @Override
        public String toString() {
            return name + " - " + highScore + " pts (" + highFishCount + " fish)";
        }
    }
}
