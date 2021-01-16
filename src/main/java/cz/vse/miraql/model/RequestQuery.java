package cz.vse.miraql.model;


import lombok.Data;

@Data
public class RequestQuery {
    private String sparql;
    private String targetOntologyIri;
}