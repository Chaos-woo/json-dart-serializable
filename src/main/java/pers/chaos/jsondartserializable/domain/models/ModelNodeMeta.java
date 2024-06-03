package pers.chaos.jsondartserializable.domain.models;

import lombok.Data;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeDataType;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeType;

/**
 * 模型节点元数据
 */
@Data
public class ModelNodeMeta {
    /**
     * 原始JSON字段名
     */
    private String jsonFieldName;
    /**
     * 自定义分析后的JSON类型枚举
     */
    private ModelNodeDataType modelNodeDataType;
    /**
     * 节点类型
     */
    private ModelNodeType nodeType;

    /**
     * 是否是基础数据类型，包含基本数据类型或由基本数据类型构成的数组
     */
    public boolean isBasisModelNodeDataType() {
        return modelNodeDataType == ModelNodeDataType.BASIS_DATA || modelNodeDataType == ModelNodeDataType.BASIS_DATA_ARRAY;
    }
}
