package dev.turtywurty.turtyapi.games;

import lombok.Data;

@Data
public class Artwork {
    private boolean alpha_channel;
    private boolean animated;
    private String checksum;
    private int game;
    private int height;
    private String image_id;
    private String url;
    private int width;
}
