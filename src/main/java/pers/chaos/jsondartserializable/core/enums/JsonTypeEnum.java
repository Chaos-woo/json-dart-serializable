package pers.chaos.jsondartserializable.core.enums;

/**
 * Json类型枚举
 */
public enum JsonTypeEnum {
    BASIS_TYPE, // 例如，数字、字符串
    OBJECT,
    OBJECT_ARRAY,
    BASIS_TYPE_ARRAY, // 例如，数组或字符串的集合. [1,2] or ["val0", "val1"]
}
