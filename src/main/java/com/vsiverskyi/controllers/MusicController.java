package com.vsiverskyi.controllers;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
@Getter
public class MusicController {

    private List<Media> playlist;
    private MediaPlayer mediaPlayer;
    private boolean wasStarted;
    private int currentTrackIndex = 0;

    public MusicController() {
        playlist = new ArrayList<>();
    }

    // Load all MP3 files from the specified folder into the playlist
    public void loadMusicFilesFromFolder(String folderPath) {
        File folder = new File(folderPath);
        File[] files = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".mp3"));

        if (files != null) {
            for (File file : files) {
                Media media = new Media(file.toURI().toString());
                playlist.add(media);
            }
        }
    }


    // Play the track at the current index
    public void playTrack(int index) {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
        wasStarted = true;

        currentTrackIndex = index;
        mediaPlayer = new MediaPlayer(playlist.get(currentTrackIndex));
        mediaPlayer.play();


        // Set listener to play the next track when the current one finishes
        mediaPlayer.setOnEndOfMedia(this::playNextTrack);
    }

    // Play the next track in the playlist
    public void playNextTrack() {
        currentTrackIndex = (currentTrackIndex + 1) % playlist.size();
        playTrack(currentTrackIndex);
    }

    // Play the previous track in the playlist
    public void playPreviousTrack() {
        currentTrackIndex = (currentTrackIndex - 1 + playlist.size()) % playlist.size();
        playTrack(currentTrackIndex);
    }

    // Stop playback
    public void stopPlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
        }
    }

    // Pause playback
    public void pausePlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.pause();
        }
    }

    // Resume playback
    public void resumePlayback() {
        if (mediaPlayer != null) {
            mediaPlayer.play();
        }
    }
}
