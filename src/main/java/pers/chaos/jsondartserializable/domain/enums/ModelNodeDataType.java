package pers.chaos.jsondartserializable.domain.enums;

/**
 * 模型节点类型枚举
 */
public enum ModelNodeDataType {
    /**
     * 基本数据类型
     * 例如，数字、字符串
     */
    BASIS_DATA,
    /**
     * 对象类型
     */
    OBJECT,
    /**
     * 对象数组
     */
    OBJECT_ARRAY,
    /**
     * 基本数据类型数组
     * 例如，数组或字符串的集合. [1,2] or ["val0", "val1"]
     */
    BASIS_DATA_ARRAY,
    ;
}
