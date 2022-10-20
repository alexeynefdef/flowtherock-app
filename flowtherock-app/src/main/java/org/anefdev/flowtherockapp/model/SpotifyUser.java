package org.anefdev.flowtherockapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SpotifyUser {
    private String id;
    private String email;
    private String name;
    private String img;
    private String url;
}
