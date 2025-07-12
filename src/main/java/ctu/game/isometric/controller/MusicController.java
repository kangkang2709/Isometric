package ctu.game.isometric.controller;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.Music;
import ctu.game.isometric.model.game.GameState;

import java.util.HashMap;
import java.util.Map;

public class MusicController {
    private final Map<String, Music> musicTracks;
    private String currentTrackId;
    private float volume = 0.5f;
    private boolean enabled = true;

    public MusicController() {
        this.musicTracks = new HashMap<>();
        this.currentTrackId = null;
    }

    public void initialize() {
        loadMusic("exploring_theme", "audio/musics/village.mp3");
        loadMusic("main_theme", "audio/musics/Menu.mp3");
        loadMusic("menu_theme", "audio/musics/menu.mp3");
        loadMusic("setting_theme", "audio/musics/setting_theme.mp3");
        loadMusic("combat_theme", "audio/musics/combat_theme.mp3");
        loadMusic("victory", "audio/musics/Victory.mp3");
        loadMusic("defeat", "audio/musics/Defeat.mp3");
        loadMusic("boss", "audio/musics/BOSS.mp3");
        loadMusic("lord", "audio/musics/LORD.mp3");
        loadMusic("dungeon", "audio/musics/dungeon1.mp3");
    }

    private void loadMusic(String id, String path) {
        try {
            Music music = Gdx.audio.newMusic(Gdx.files.internal(path));
            music.setLooping(true);
            music.setVolume(volume);
            musicTracks.put(id, music);
        } catch (Exception e) {
            System.err.println("Error loading music track: " + path + " - " + e.getMessage());
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
            System.out.println("Playing music: " + musicId);
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


    public void playDungeonMusic() {
        if (!enabled) {
            return;
        }
        playMusic("dungeon");
    }


    public void playMusicForState(GameState state) {
        if (state == null) {
            return;
        }
        if (!enabled) {
            return;
        }
        switch (state) {
            case EXPLORING:
                playMusic("exploring_theme");
                break;
            case LOAD_GAME:
                break;
            case GAMEPLAY:
                break;
            case MAIN_MENU:
                playMusic("main_theme");
                break;
            case MENU:
                playMusic("main_theme");
                System.out.println("Playing menu theme music");
                break;
            case SETTINGS:
                break;
            case CHARACTER_CREATION:
                break;
            default:
                stopCurrentTrack();
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
        boolean wasDisabled = !this.enabled && enabled;
        this.enabled = enabled;

        if (!enabled) {
            stopCurrentTrack();
        } else if (wasDisabled && currentTrackId != null) {
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


}