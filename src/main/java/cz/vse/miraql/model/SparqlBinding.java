package cz.vse.miraql.model;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class SparqlBinding {
    private Map<String, String> ontologyLabel;
    @JsonProperty("import")
    private Map<String, String> imports;
    @JsonProperty("importLabel")
    private Map<String, String> importsLabel;
    private Map<String, String> ontology;
}
