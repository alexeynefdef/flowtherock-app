package org.anefdev.flowtherockapp.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MusicPlaylist {

     String id;
     String title;
     Integer count;
     String imageUrl;

}
