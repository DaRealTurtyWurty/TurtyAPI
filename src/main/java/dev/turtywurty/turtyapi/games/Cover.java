package dev.turtywurty.turtyapi.games;

import lombok.Data;

@Data
public class Cover {
    private boolean alpha_channel;
    private boolean animated;
    private String checksum;
    private int game = -1;
    private int game_localization = -1;
    private int height = -1;
    private String image_id;
    private String url;
    private int width = -1;
}
