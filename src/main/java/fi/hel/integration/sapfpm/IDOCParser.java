package fi.hel.integration.sapfpm;

import io.smallrye.mutiny.tuples.Tuple2;
import org.apache.camel.Exchange;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

public class IDOCParser {

    public static Tuple2<Map<String, Object>, List<LinkedHashMap<String, Object>>> extractValuesFromXML(Exchange e, String xmlRootName, String commonValueKey, String valuesKey,
                                                                                                         BiFunction<Map<String, Object>, Map<String, Object>, LinkedHashMap<String, Object>> parseValues) {
        Map<String, Map<String, Object>> xmlRoot = e.getIn().getBody(Map.class);
        Map<String, Object> IDOC = xmlRoot.get(xmlRootName);
        Map<String, Object> commonValues;
        if (commonValueKey != null) {
            commonValues = (Map) IDOC.get(commonValueKey);
        } else {
            commonValues = null;
        }
        Object valuesObj = IDOC.get(valuesKey);

        if (valuesObj instanceof List valList) {
            List<Map<String, Object>> vals = valList;
            return Tuple2.of(commonValues, vals.stream().map(v -> parseValues.apply(commonValues, v)).toList());
        } else {
            Map<String, Object> v = (LinkedHashMap<String, Object>) valuesObj;
            return Tuple2.of(commonValues, List.of(parseValues.apply(commonValues, v)));
        }
    }

    public static Tuple2<Map<String, Object>, List<LinkedHashMap<String, Object>>> extractValuesFromIDOC(Exchange e, String commonValueKey, String valuesKey,
                                                                                                         BiFunction<Map<String, Object>, Map<String, Object>, LinkedHashMap<String, Object>> parseValues) {
        Map<String, Map<String, Object>> xmlRoot = e.getIn().getBody(Map.class);
        Map<String, Object> IDOC = xmlRoot.get("IDOC");
        Map<String, Object> commonValues;
        if (commonValueKey != null) {
            commonValues = (Map) IDOC.get(commonValueKey);
        } else {
            commonValues = null;
        }
        Object valuesObj = IDOC.get(valuesKey);

        if (valuesObj instanceof List valList) {
            List<Map<String, Object>> vals = valList;
           return Tuple2.of(commonValues, vals.stream().map(v -> parseValues.apply(commonValues, v)).toList());
        } else {
            Map<String, Object> v = (LinkedHashMap<String, Object>) valuesObj;
            return Tuple2.of(commonValues, List.of(parseValues.apply(commonValues, v)));
        }
    }

    public static List<LinkedHashMap<String, Object>> extractValuesDirectlyFromXML(Exchange e, String valuesKey, Function<Map<String, Object>, LinkedHashMap<String, Object>> parseValues) {
        Map<String, Object> xmlRoot = e.getIn().getBody(Map.class);
        Object valuesObj = xmlRoot.get(valuesKey);

        if (valuesObj instanceof List valList) {
            List<Map<String, Object>> vals = valList;
            return vals.stream().map(v -> parseValues.apply(v)).toList();
        } else {
            Map<String, Object> v = (LinkedHashMap<String, Object>) valuesObj;
            return List.of(parseValues.apply(v));
        }
    }

    public static Map<String, List<LinkedHashMap<String, Object>>> addToByYearAndMonthIfExistsOrCreate(Map<String, List<LinkedHashMap<String, Object>>> byYearAndMonth, String yearAndMonth, List<LinkedHashMap<String, Object>> valueLines) {
        Map<String, List<LinkedHashMap<String, Object>>> existing = byYearAndMonth;
        if (existing == null) existing = new HashMap<>(1);

        List<LinkedHashMap<String, Object>> prevLines = existing.get(yearAndMonth);
        if (prevLines == null) {
            existing.put(yearAndMonth, valueLines);
        } else {
            existing.put(yearAndMonth, concatNewLinesToOld(prevLines, valueLines));
        }
        return existing;
    }

    public static <T> List<T> concatNewLinesToOld(List<T> prevLines, List<T> workBreakDownLines) {
        return Stream.concat(prevLines.stream(), workBreakDownLines.stream()).toList();
    }

}
