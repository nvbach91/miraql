package cz.vse.miraql.model;


import lombok.Data;

import java.util.List;

@Data
public class SparqlHead {
    private List<String> vars;
}