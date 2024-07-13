package pers.chaos.jsondartserializable.domain.ui.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;
import pers.chaos.jsondartserializable.domain.models.forgenerated.ModelGenUserOption;
import pers.chaos.jsondartserializable.domain.util.JsonNodeUtil;

@Data
public class UserInputData {
    private final String rootClassName;
    private final String rootClassRemark;
    private final String jsonString;
    private final ModelGenUserOption userOption;

    @Builder
    public UserInputData(String className, String remark, String jsonString, ModelGenUserOption userOption) {
        this.rootClassName = className;
        this.rootClassRemark = remark;
        this.userOption = userOption;
        this.jsonString = jsonString;
    }

    public JsonNode getJsonNode() throws JsonProcessingException {
        return JsonNodeUtil.readJsonObjectNode(jsonString);
    }
}
