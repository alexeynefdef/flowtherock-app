package org.anefdev.flowtherockapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MusicTrack implements Serializable {

    private String id;
    private String title;
    private String artist;
    private String album;
    private Integer duration;
    private String bpm;
    private String key;
    private String mode;
    private String camelot;
    private boolean matched;
    private String previewUrl;

}
