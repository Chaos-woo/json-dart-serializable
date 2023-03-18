package pers.chaos.jsondartserializable.utils;

import com.google.common.base.CaseFormat;

public class DartClassFileUtils {
    public static String getDartFileNameByClassName(String jsonField) {
        if (jsonField.contains("_")) {
            return jsonField;
        } else if (jsonField.contains("-")) {
            return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_UNDERSCORE, jsonField);
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, jsonField);
    }

    public static String getDartClassName(String jsonField) {
        if (jsonField.contains("_")) {
            return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, jsonField);
        } else if (jsonField.contains("-")) {
            return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, jsonField);
        }
        return jsonField.substring(0, 1).toUpperCase() + jsonField.substring(1);
    }
}
