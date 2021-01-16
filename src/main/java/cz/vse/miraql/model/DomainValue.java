package cz.vse.miraql.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class DomainValue {
    @JsonProperty("type")
    private final String type = "string";

    @JsonProperty("enum")
    private List<String> enumList = new ArrayList<>();

    public DomainValue(String value) {
        enumList.add(value);
    }

    public void addValue(String value) {
        enumList.add(value);
    }
}
