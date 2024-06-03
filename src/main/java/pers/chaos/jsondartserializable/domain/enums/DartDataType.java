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
    ;

    public String toDataStr() {
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
                return "none";
        }
    }
}
