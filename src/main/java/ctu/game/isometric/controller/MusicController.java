package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import ctu.game.isometric.model.game.GameState;

import java.util.HashMap;
import java.util.Map;

public class MusicController {
    private final Map<String, Music> musicTracks;
    private String currentTrackId;
    private float volume = 1.0f;
    private boolean enabled = true;

    public MusicController() {
        this.musicTracks = new HashMap<>();
        this.currentTrackId = null;
    }

    public void initialize() {
        loadMusic("exploring_theme", "audio/musics/exploring_theme.mp3");
        loadMusic("main_theme", "audio/musics/main_theme.mp3");
        loadMusic("menu_theme", "audio/musics/menu_theme.mp3");
        loadMusic("setting_theme", "audio/musics/menu_theme.mp3");
        loadMusic("dialog_theme", "audio/musics/dialog_theme.mp3");
    }

    private void loadMusic(String id, String path) {
        try {
            Music music = Gdx.audio.newMusic(Gdx.files.internal(path));
            music.setLooping(true);
            music.setVolume(volume);
            musicTracks.put(id, music);
        } catch (Exception e) {
            System.err.println("Error loading music track: " + path);
        }
    }

    public void playMusic(String musicId) {
        if (!enabled || (currentTrackId != null && currentTrackId.equals(musicId))) {
            return;
        }

        // Stop current track if any
        stopCurrentTrack();

        // Play new track
        Music track = musicTracks.get(musicId);
        if (track != null) {
            track.setVolume(volume);
            track.play();
            currentTrackId = musicId;
        }
    }

    public void stopCurrentTrack() {
        if (currentTrackId != null) {
            Music currentTrack = musicTracks.get(currentTrackId);
            if (currentTrack != null) {
                currentTrack.stop();
            }
        }
    }

    public void setVolume(float volume) {
        this.volume = Math.max(0, Math.min(1, volume));

        if (currentTrackId != null) {
            Music currentTrack = musicTracks.get(currentTrackId);
            if (currentTrack != null) {
                currentTrack.setVolume(this.volume);
            }
        }
    }

    public void playMusicForState(GameState state) {
        switch (state) {
            case EXPLORING:
                playMusic("exploring_theme");
                break;
            case DIALOG:
                playMusic("dialog_theme");
                break;
            case MAIN_MENU:
                playMusic("main_theme");
                break;
            case MENU:
                playMusic("menu_theme");
                break;
            case CUTSCENE:
                playMusic("cutscene_theme");
                break;
        }
    }

    public void dispose() {
        for (Music track : musicTracks.values()) {
            track.dispose();
        }
        musicTracks.clear();
    }

    // Getters and setters
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        // neu trang thai dang tat va dang bat lai
        boolean wasDisabled = !this.enabled && enabled;
        this.enabled = enabled;

        if (!enabled) {
            stopCurrentTrack();
        } else if (wasDisabled && currentTrackId != null) {
            // Resume playback when re-enabled
            Music track = musicTracks.get(currentTrackId);
            if (track != null) {
                track.setVolume(volume);
                track.play();
            }
        }
    }

    public float getVolume() {
        return volume;
    }

    public String getCurrentTrackId() {
        return currentTrackId;
    }
}