package pers.chaos.jsondartserializable.core.constants;

public interface JsonAnalysisTableKeys {

    enum MappingModelTableReflectable {
        JSON_FIELD_NAME("jsonFieldName", 0, "JSON field"),
        DART_PROPERTY_NAME("dartPropertyName", 1, "Dart field"),
        DART_DATA_TYPE("dartDataTypeEnum", 2, "Dart basis type"),
        DART_PROPERTY_REQUIRED("dartPropertyRequired", 3, "Required?"),
        DART_PROPERTY_DEFAULT_VALUE("dartPropertyDefaultValue", 4, "Default value"),
        DART_FILE_NAME("dartFileName", 5, "Dart file name"),
        INNER_OBJECT_CLASS_NAME("className", 6, "Dart class name"),
        JSON_DATA_TYPE("jsonTypeEnum", 7, "JSON type"),
        DESCRIPTION("description", 8, "Field note"),

        ;

        private final String propertyName;
        private final int tableColumnIndex;
        private final String columnName;

        MappingModelTableReflectable(String propertyName, int tableColumnIndex, String columnName) {
            this.propertyName = propertyName;
            this.tableColumnIndex = tableColumnIndex;
            this.columnName = columnName;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public int getTableColumnIndex() {
            return tableColumnIndex;
        }

        public String getColumnName() {
            return columnName;
        }
    }

    interface ObjectPropertyTable {
        String TABLE_TITLE_TEMPLATE = "Confirm 『%s』 JSON string analysis result";

        static String formatTableTitle(String object) {
            return String.format(TABLE_TITLE_TEMPLATE, object);
        }
    }

}
