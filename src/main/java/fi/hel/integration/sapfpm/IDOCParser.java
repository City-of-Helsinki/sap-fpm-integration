package fi.hel.integration.sapfpm;

import org.apache.camel.Exchange;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Stream;

public class IDOCParser {

    public static List<LinkedHashMap<String, Object>> extractValuesFromIDOC(Exchange e, String valuesKey, Function<Map<String, Object>, LinkedHashMap<String, Object>> parseValues) {
        Map<String, Map<String, Object>> xmlRoot = e.getIn().getBody(Map.class);
        Map<String, Object> IDOC = xmlRoot.get("IDOC");
        return extractValuesFromValueOrList(IDOC.get(valuesKey), parseValues);
    }

    // no IDOC structure, no common values
    public static List<LinkedHashMap<String, Object>> extractValuesDirectlyFromXML(Exchange e, String valuesKey, Function<Map<String, Object>, LinkedHashMap<String, Object>> parseValues) {
        Map<String, Object> xmlRoot = e.getIn().getBody(Map.class);
        return extractValuesFromValueOrList(xmlRoot.get(valuesKey), parseValues);
    }

    public static List<LinkedHashMap<String, Object>> extractValuesFromValueOrList(Object valuesObj, Function<Map<String, Object>, LinkedHashMap<String, Object>> parseValues) {
        if (valuesObj instanceof List valList) {
            List<Map<String, Object>> vals = valList;
            return vals.stream().map(v -> parseValues.apply(v)).toList();
        } else {
            Map<String, Object> v = (LinkedHashMap<String, Object>) valuesObj;
            return List.of(parseValues.apply(v));
        }
    }
}
