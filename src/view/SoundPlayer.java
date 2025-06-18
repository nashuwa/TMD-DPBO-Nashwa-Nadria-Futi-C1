package view;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

public class SoundPlayer {
    private Clip clip;
    private URL soundURL;

    public SoundPlayer(String filePath) {
        try {
            System.out.println("Attempting to load sound file: " + filePath);
            // Get the URL for the sound file from the classpath
            soundURL = getClass().getResource(filePath);
            if (soundURL == null) {
                System.err.println("Sound file not found: " + filePath);
                System.err.println("Make sure the file exists in the resources folder");
                return;
            }
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
            clip.stop(); // Stop if already playing
            clip.setFramePosition(0); // Rewind to the beginning
            clip.loop(Clip.LOOP_CONTINUOUSLY); // Loop indefinitely
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
}