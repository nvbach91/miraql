package cz.vse.miraql.model;


import lombok.Data;

@Data
public class OntologyDependencyDependantCount implements Comparable<OntologyDependencyDependantCount> {
    private String iri;
    private Number count;
    public OntologyDependencyDependantCount(String iri, Number count) {
        this.iri = iri;
        this.count = count;
    }
    @Override
    public int compareTo(OntologyDependencyDependantCount o) {
        return o.getCount().intValue() - this.getCount().intValue();
    }
}
