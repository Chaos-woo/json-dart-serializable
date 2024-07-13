package pers.chaos.jsondartserializable.domain.enums;

/**
 * Dart数据类型枚举
 */
public enum DartDataType {
    BOOLEAN,
    INT,
    DOUBLE,
    STRING,
    OBJECT,
    DATE_TIME,
    // 分析异常使用
    UNKNOWN,
    ;

    /**
     * 转换为数据字符串
     */
    public String toDartDefinition() {
        switch (this) {
            case INT:
                return "int";
            case DOUBLE:
                return "double";
            case STRING:
                return "String";
            case BOOLEAN:
                return "bool";
            case DATE_TIME:
                return "DateTime";
            case OBJECT:
            default:
                return "Unknown";
        }
    }
}
