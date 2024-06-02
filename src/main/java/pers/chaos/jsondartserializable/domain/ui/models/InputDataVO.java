package pers.chaos.jsondartserializable.domain.ui.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import pers.chaos.jsondartserializable.domain.models.ModelGenUserConfig;
import pers.chaos.jsondartserializable.domain.service.JsonNodeAnalyser;

@Data
public class InputDataVO {
    private final String rootClassName;
    private final String rootClassRemark;
    private final String jsonString;
    private final ModelGenUserConfig userConfig;

    @Builder
    public InputDataVO(String className, String remark, String jsonString, ModelGenUserConfig userConfig) {
        this.rootClassName = className;
        this.rootClassRemark = remark;
        this.userConfig = userConfig;
        this.jsonString = jsonString;
    }

    public JsonNode getJsonNode() throws JsonProcessingException {
        return JsonNodeAnalyser.readJsonObjectNode(jsonString);
    }
}
