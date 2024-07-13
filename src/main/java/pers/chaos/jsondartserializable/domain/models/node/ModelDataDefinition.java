package pers.chaos.jsondartserializable.domain.models.node;

import lombok.Data;
import pers.chaos.jsondartserializable.domain.enums.DartDataType;

/**
 * 模型数据定义
 */
@Data
public class ModelDataDefinition {
    private String name;
    private DartDataType type;
    private Object defaultVal;
    private boolean nullable = false;
    private String remark;
}
