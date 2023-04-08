package pers.chaos.jsondartserializable.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pers.chaos.jsondartserializable.windows.UserAdvanceConfiguration;

public final class JsonAnalyser {
    private static final ObjectMapper ANALYSER = new ObjectMapper();
    private static final ObjectMapper PRETTY_PRINTER = new ObjectMapper();

    public static String getPrettyString(String jsonString) throws JsonProcessingException {
        JsonNode node = PRETTY_PRINTER.readTree(jsonString);
        return PRETTY_PRINTER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    public static JsonDartAnalysisMapping analysis(String className, String jsonString,
                                                   UserAdvanceConfiguration userAdvanceConfiguration)
            throws JsonProcessingException {
        JsonNode node = ANALYSER.readTree(jsonString);

        if (!node.isObject()) {
            throw new RuntimeException("Only support Object JSON tree analysis");
        }

        // start with a JsonDartAnalysisMapping holder
        return new JsonDartAnalysisMapping(className, node, userAdvanceConfiguration);
    }
}
