package dev.turtywurty.turtyapi.games;

import lombok.Data;

@Data
public class GamePlatform {
    private String abbreviation;
    private String alternative_name;
    private int category = -1;
    private String checksum;
    private long created_at = -1;
    private int generation = -1;
    private String name;
    private int platform_family = -1;
    private int platform_logo = -1;
    private String slug;
    private String summary;
    private long updated_at = -1;
    private String url;
    private int[] versions;
    private int[] websites;
}
