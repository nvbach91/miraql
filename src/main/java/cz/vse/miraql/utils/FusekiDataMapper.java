package cz.vse.miraql.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import javax.annotation.Nonnull;
import java.util.*;

@Slf4j
public final class FusekiDataMapper {

    private FusekiDataMapper() {
        throw new IllegalAccessError("Utility class");
    }

    /**
     * Binding fields of the result Map Object as the following:
     * 
     * "head": { "vars": [ ] } , // in "vars" goes ResultSet headers
     * "results": { 
     *   "bindings": []
     * }
     */
    public static Map<String, Object> composeResult(final ResultSet rs, final List<Object> bindings) {
        Map<String, Object> head = new HashMap<>();
        head.put("vars", rs.getResultVars());

        Map<String, Object> results = new HashMap<>();
        results.put("bindings", bindings);

        Map<String, Object> map = new HashMap<>();
        map.put("head", head);
        map.put("results", results);

        return map;
    }
    
    private static void bindContentToBinding(final Map<String, Object> binding, 
                                            final String valueName, 
                                            final String valueType, 
                                            final String value) {
        /**
         * Binding fields of binding objects as the following:
         * "bindings": [
         *   {
         *     "___": { "type": "___" , "value": "___", "datatype": "___" } ,
         *   }
         * ]
         * Note that datatype might be included in the value so we must split it
         * Only bind fields which have some value
         */
        if (!value.isEmpty()) {
            Map<String, String> valueObject = new HashMap<>();
            valueObject.put("type", valueType);
            if (value.contains("^^")) {
                String[] valueParts = value.split("\\^\\^");
                valueObject.put("value", valueParts[0]);
                valueObject.put("datatype", valueParts[1]);
            } else {
                valueObject.put("value", value);
            }
            binding.put(valueName, valueObject);
        }
    }

    private static String detectTermType(final String value) {
        if (value.matches("^http(s)?://.*$")) {
            return "uri";
        }
        return "literal";
    }

    public static Map<String, Object> composeBinding(final List<String> valueNames, final QuerySolution qs) {
        Map<String, Object> binding = new HashMap<>();
        for (String valueName : valueNames) {
            String value = getStringValue(valueName, qs).replaceAll("\\\\", "");
            String valueType = detectTermType(value);
            bindContentToBinding(binding, valueName, valueType, value);
        }
        return binding;
    }


    @Nonnull
    private static String getStringValue(@Nonnull final String varName, @Nonnull final QuerySolution qs) {
        final RDFNode rdfNode = qs.get(varName);

        return rdfNode != null ? rdfNode.toString() : "";
    }

    /**
     * Manual serialization of SPARQL query results to Java Map Objects and later to JSON
     * See: https://www.w3.org/TR/rdf-sparql-json-res/
     * This serialization should be changed in the future using our own serialization logic
     */
    @Nonnull
    public static Map<String, Object> mapToCommonJson(@Nonnull final ResultSet rs) {
        List<Object> bindings = new ArrayList<>();
        List<String> vars = rs.getResultVars();
        while (rs.hasNext()) {
            QuerySolution qs = rs.next();
            Map<String, Object> binding = composeBinding(vars, qs);
            bindings.add(binding);
        }

        return composeResult(rs, bindings);
    }

}