package dev.turtywurty.turtyapi.nsfw;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Pornstar {
    private String career_status;
    private String country;
    private int day_of_birth;
    private String month_of_birth;
    private int year_of_birth;
    private String gender;
    private String id;
    private String name;
    private String profession;
    private List<String> nicknames = new ArrayList<>();
    private List<Photo> photos = new ArrayList<>();

    public boolean hasPhoto() {
        return !this.photos.isEmpty() &&
                this.photos.get(0) != null &&
                this.photos.get(0).getFull() != null &&
                !this.photos.get(0).getFull().isBlank();
    }
}
