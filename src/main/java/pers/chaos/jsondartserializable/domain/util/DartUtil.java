package pers.chaos.jsondartserializable.domain.util;

import com.google.common.base.CaseFormat;
import pers.chaos.jsondartserializable.common.models.StringConst;

/**
 * Dart工具
 */
public class DartUtil {
    public static String toFileName(String fieldName) {
        if (fieldName.contains(StringConst.underline)) {
            return fieldName;
        } else if (fieldName.contains(StringConst.strikethrough)) {
            return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
        }
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, fieldName);
    }

    public static String toClassName(String field) {
        if (field.contains(StringConst.underline)) {
            return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, field);
        } else if (field.contains(StringConst.strikethrough)) {
            return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, field);
        }
        return field.substring(0, 1).toUpperCase() + field.substring(1);
    }

    public static String toPropertyName(String field) {
        if (field.contains(StringConst.underline)) {
            return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, field);
        } else if (field.contains(StringConst.strikethrough)) {
            return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, field);
        }
        return field.substring(0, 1).toLowerCase() + field.substring(1);
    }
}
