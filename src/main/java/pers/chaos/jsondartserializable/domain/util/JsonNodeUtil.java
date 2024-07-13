package pers.chaos.jsondartserializable.domain.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * JSON分析器
 */
public class JsonNodeUtil {
    private static final ObjectMapper ANALYSER = new ObjectMapper();
    private static final ObjectMapper PRETTY_PRINTER = new ObjectMapper();

    /**
     * 展示格式化的JSON string
     */
    public static String getPrettyString(String jsonString) throws JsonProcessingException {
        JsonNode node = PRETTY_PRINTER.readTree(jsonString);
        return PRETTY_PRINTER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
    }

    /**
     * 解析Json字符串为Json节点
     */
    public static JsonNode readJsonObjectNode(String jsonString) throws JsonProcessingException {
        JsonNode jsonNode = ANALYSER.readTree(jsonString);
        if (Objects.isNull(jsonNode) || !jsonNode.isObject()) {
            throw new RuntimeException("Only support analysis object Json string, please check your Json content");
        }

        return jsonNode;
    }
}
