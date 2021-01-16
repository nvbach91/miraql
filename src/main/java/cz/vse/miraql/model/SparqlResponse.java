package cz.vse.miraql.model;

import lombok.Data;

@Data
public class SparqlResponse {
    private SparqlHead head;
    private SparqlResults results;
}