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

    public static JsonDartAnalysis analysis(String className, String jsonString,
                                            UserAdvanceConfiguration userAdvanceConfiguration)
            throws JsonProcessingException {
        JsonNode node = ANALYSER.readTree(jsonString);

        if (!node.isObject()) {
            throw new RuntimeException("Only support Object JSON tree analysis");
        }

        return new JsonDartAnalysis(className, node, userAdvanceConfiguration);
    }

    public static class AnalysisRebuildData {
        private final String className;
        private final JsonNode jsonNode;
        private final UserAdvanceConfiguration userAdvanceConfiguration;

        public AnalysisRebuildData(String className, String jsonString, UserAdvanceConfiguration userAdvanceConfiguration) throws JsonProcessingException {
            this.className = className;
            this.userAdvanceConfiguration = userAdvanceConfiguration;
            this.jsonNode = ANALYSER.readTree(jsonString);

            if (!this.jsonNode.isObject()) {
                throw new RuntimeException("Only support Object JSON tree analysis");
            }
        }

        public String getClassName() {
            return className;
        }

        public JsonNode getJsonNode() {
            return jsonNode;
        }

        public UserAdvanceConfiguration getUserAdvanceConfiguration() {
            return userAdvanceConfiguration;
        }
    }
}
