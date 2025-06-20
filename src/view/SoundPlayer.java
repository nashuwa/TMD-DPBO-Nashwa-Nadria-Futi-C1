package view;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class SoundPlayer {
    private Clip clip;
    private URL soundURL;
    private long pausePosition = 0;
    private boolean isPaused = false;

    public SoundPlayer(String filePath) {
        try {
            soundURL = getClass().getResource(filePath);
            System.out.println("Sound file found at: " + soundURL);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundURL);
            clip = AudioSystem.getClip();
            clip.open(audioStream);
            System.out.println("Sound loaded successfully: " + filePath);
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            System.err.println("Error loading sound: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void play() {
        if (clip != null) {
            clip.stop(); // Stop if already playing
            clip.setFramePosition(0); // Rewind to the beginning
            clip.start();
        }
    }

    public void loop() {
        if (clip != null) {
            System.out.println("Starting background music loop...");
            clip.stop(); 
            clip.setFramePosition(0); 
            clip.loop(Clip.LOOP_CONTINUOUSLY); 
        } else {
            System.out.println("Cannot loop - clip is null!");
        }
    }

    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
        }
    }

    public void close() {
        if (clip != null) {
            clip.close();
        }
    }

    public void restart() {
        if (clip != null) {
            System.out.println("Restarting sound...");
            stop();
            loop();
        } else {
            System.out.println("Cannot restart - clip is null!");
        }
    }

    public void pause() {
        if (clip != null && clip.isRunning()) {
            pausePosition = clip.getMicrosecondPosition();
            clip.stop();
            isPaused = true;
            System.out.println("Music paused at position: " + pausePosition);
        }
    }

    public void resume() {
        if (clip != null && isPaused) {
            clip.setMicrosecondPosition(pausePosition);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            isPaused = false;
            System.out.println("Music resumed from position: " + pausePosition);
        }
    }

    public boolean isPaused() {
        return isPaused;
    }
}