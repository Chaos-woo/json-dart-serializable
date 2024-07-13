package pers.chaos.jsondartserializable.domain.enums;

import org.apache.commons.lang3.StringUtils;

/**
 * Dart数据类型枚举
 */
public enum DartDataType {
    BOOLEAN("bool"),
    INT("int"),
    DOUBLE("double"),
    STRING("String"),
    OBJECT("UNKNOWN"),
    DATE_TIME("DateTime"),
    // 分析异常使用
    UNKNOWN("UNKNOWN"),
    ;

    private final String dartType;

    DartDataType(String dartType) {
        this.dartType = dartType;
    }

    public static DartDataType fromDartType(String dartType) {
        for (DartDataType dartDataType : DartDataType.values()) {
            if (StringUtils.equalsIgnoreCase(dartDataType.dartType, dartType)) {
                return dartDataType;
            }
        }
        return UNKNOWN;
    }

    public boolean is(DartDataType dartDataType) {
        return this == dartDataType;
    }

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
