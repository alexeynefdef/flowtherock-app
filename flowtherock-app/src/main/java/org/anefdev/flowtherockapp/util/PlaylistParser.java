package org.anefdev.flowtherockapp.util;

import org.anefdev.flowtherockapp.model.MusicPlaylist;
import org.anefdev.flowtherockapp.model.MusicTrack;
import se.michaelthelin.spotify.model_objects.miscellaneous.AudioAnalysis;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PlaylistParser {


    public static List<MusicTrack> getTrackListNoMetaData(List<PlaylistTrack> playlist) {
        return playlist.stream().map(
                playlistTrack -> {
                    var musicTrack = new MusicTrack();
                    var track = playlistTrack.getTrack();
                    musicTrack.setId(track.getId());
                    return musicTrack;
                }
        ).toList();
    }

    public static List<MusicPlaylist> getAllPlaylists(List<PlaylistSimplified> playlists) {
        return playlists.stream().map(
                playlist -> {
                    var musicPlaylist = new MusicPlaylist();
                    musicPlaylist.setId(playlist.getId());
                    musicPlaylist.setTitle(playlist.getName());
                    musicPlaylist.setCount(playlist.getTracks().getTotal());
                    musicPlaylist.setImageUrl(playlist.getImages()[0].getUrl());
                    return musicPlaylist;
                }
        ).toList();
    }

    public static MusicTrack convertTrackAnalysisToTrack(AudioAnalysis analysis) {

        MusicTrack track = new MusicTrack();
        var trackAnalysis = analysis.getTrack();
        track.setBpm(String.valueOf(trackAnalysis.getTempo()));
        track.setKey(String.valueOf(trackAnalysis.getKey()));
        track.setMode(String.valueOf(trackAnalysis.getMode()));

        return track;
    }

    public static MusicTrack parseTrackAnalysisKey(MusicTrack track) {

        final Map<Integer, String> keys = new HashMap<>();
        keys.put(-1, "NO_KEY");
        keys.put(0, "C");
        keys.put(1, "D♭");
        keys.put(2, "D");
        keys.put(3, "E♭");
        keys.put(4, "E");
        keys.put(5, "F");
        keys.put(6, "F♯");
        keys.put(7, "G");
        keys.put(8, "A♭");
        keys.put(9, "A");
        keys.put(10, "B♭");
        keys.put(11, "B");

        final Map<String, String> camelot = new HashMap<>();
        camelot.put("A♭-Min", "a1");
        camelot.put("E♭-Min", "a2");
        camelot.put("B♭-Min", "a3");
        camelot.put("F-Min", "a4");
        camelot.put("C-Min", "a5");
        camelot.put("G-Min", "a6");
        camelot.put("D-Min", "a7");
        camelot.put("A-Min", "a8");
        camelot.put("E-Min", "a9");
        camelot.put("B-Min", "a10");
        camelot.put("F♯-Min", "a11");
        camelot.put("D♭-Min", "a12");
        camelot.put("B-Maj", "b1");
        camelot.put("F♯-Maj", "b2");
        camelot.put("D♭-Maj", "b3");
        camelot.put("A♭-Maj", "b4");
        camelot.put("E♭-Maj", "b5");
        camelot.put("B♭-Maj", "b6");
        camelot.put("F-Maj", "b7");
        camelot.put("C-Maj", "b8");
        camelot.put("G-Maj", "b9");
        camelot.put("D-Maj", "b10");
        camelot.put("A-Maj", "b11");
        camelot.put("E-Maj", "b12");

        track.setKey(keys.get(Integer.parseInt(track.getKey()))
                + "-"
                + (track.getMode().equals("MAJOR") ? "Maj" : "Min"));
        track.setCamelot(camelot.get(track.getKey()));
        return track;
    }

    public static MusicTrack parseTrackInfo(MusicTrack track, Track trackInfo) {
        track.setId(trackInfo.getId());
        track.setTitle(trackInfo.getName());
        track.setArtist(trackInfo.getArtists()[0].getName());
        track.setAlbum(trackInfo.getAlbum().getName());
        track.setDuration(trackInfo.getDurationMs());
        track.setPreviewUrl(trackInfo.getPreviewUrl());
        return track;
    }

    public static List<MusicTrack> sortPlaylist(List<MusicTrack> playlist, String trackId) {

        Map<String, List<String>> harmonies = new HashMap<>();
        harmonies.put("a1", List.of("a12", "a2", "b1"));
        harmonies.put("a2", List.of("a1", "a3", "b2"));
        harmonies.put("a3", List.of("a2", "a4", "b3"));
        harmonies.put("a4", List.of("a3", "a5", "b4"));
        harmonies.put("a5", List.of("a4", "a6", "b5"));
        harmonies.put("a6", List.of("a5", "a7", "b6"));
        harmonies.put("a7", List.of("a6", "a8", "b7"));
        harmonies.put("a8", List.of("a7", "a9", "b8"));
        harmonies.put("a9", List.of("a8", "a10", "b9"));
        harmonies.put("a10", List.of("a9", "a11", "b10"));
        harmonies.put("a11", List.of("a10", "a12", "b11"));
        harmonies.put("a12", List.of("a11", "a1", "b12"));
        harmonies.put("b1", List.of("b12", "b2", "a1"));
        harmonies.put("b2", List.of("b1", "b2", "a2"));
        harmonies.put("b3", List.of("b2", "b4", "a3"));
        harmonies.put("b4", List.of("b3", "b5", "a4"));
        harmonies.put("b5", List.of("b4", "b6", "a5"));
        harmonies.put("b6", List.of("b5", "b7", "a6"));
        harmonies.put("b7", List.of("b6", "b8", "a7"));
        harmonies.put("b8", List.of("b7", "b9", "a8"));
        harmonies.put("b9", List.of("b8", "b10", "a9"));
        harmonies.put("b10", List.of("b9", "b11", "a10"));
        harmonies.put("b11", List.of("b10", "b12", "a11"));
        harmonies.put("b12", List.of("b11", "b1", "a12"));

        MusicTrack trackFind = null;
        for (MusicTrack track : playlist) {
            if (track.getId().equals(trackId)) {
                trackFind = track;
            }
        }
        assert trackFind != null;
        List<String> currentHarmonies = harmonies.get(trackFind.getCamelot());
        List<MusicTrack> matchedTracks = new ArrayList<>();

        for (MusicTrack track : playlist) {
            if (currentHarmonies.contains(track.getCamelot()) || trackFind.getCamelot().equals(track.getCamelot())) {
                track.setMatched(true);
                matchedTracks.add(track);
            }
        }

        matchedTracks.sort((t1, t2) -> {

            var diff = Math.abs(Float.parseFloat(t1.getBpm()) - Float.parseFloat(t2.getBpm()));

            if (diff > 0) {
                return 1;
            } else if (diff < 0) {
                return -1;
            }
            return 0;
        });

        List<MusicTrack> playlistFinal = new ArrayList<>(matchedTracks);

        for (MusicTrack track: playlist) {
            if (!playlistFinal.contains(track) && !trackFind.getId().equals(track.getId())) {
                playlistFinal.add(track);
            }
        }

        return playlistFinal;

    }

}
