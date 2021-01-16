package cz.vse.miraql.model;


import lombok.Data;


@Data
public class BitbucketUserInfo {
    private String name;
    private String emailAddress;
    private int id;
    private String displayName;
    private boolean active;
    private String slug;
    private String type;
}