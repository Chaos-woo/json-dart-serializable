package pers.chaos.jsondartserializable.domain.models.node;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections.CollectionUtils;
import pers.chaos.jsondartserializable.domain.enums.DartDataType;
import pers.chaos.jsondartserializable.domain.util.StringConst;

import java.util.*;

/**
 * 模型数据定义
 */
public final class ModelDataDefinition {
    /**
     * 魔法常量
     */
    public interface MagicKey {
        /**
         * 对象扩展
         */
        String objectExt = "@oext";
        /**
         * 基本类型扩展
         */
        String basisExt = "@bext";
        /**
         * 数组对象扩展
         */
        String arrayExt = "@arroext";
        /**
         * 数组基本类型扩展
         */
        String arrayBasicExt = "@arrbext";

        static boolean isMagicKey(final String key) {
            List<String> keys = Arrays.asList(objectExt, basisExt, arrayExt, arrayBasicExt);
            return keys.contains(key);
        }

        static boolean hasAnyMagicKey(final Set<String> targetKeys) {
            List<String> keys = Arrays.asList(objectExt, basisExt, arrayExt, arrayBasicExt);
            return CollectionUtils.containsAny(targetKeys, keys);
        }
    }

    interface SyntaxKey {
        String name = "@name";
        String type = "@type";
        String val = "@val";
        String remark = "@remark";
        String nullable = "@nullable";
    }

    public enum DefinitionPosition {
        SELF,
        CHILD,
        ;

        public boolean is(DefinitionPosition position) {
            return this == position;
        }
    }

    public static Object findDataDefinitionDefaultVal(JsonNode node) {
        if (node.isNull()) {
            return null;
        }

        String text = node.asText();
        Map<String, Object> syntaxMap = parseSyntax(text);
        if (!syntaxMap.containsKey(MagicKey.basisExt)) {
            return null;
        }

        return syntaxMap.get(SyntaxKey.val);
    }

    public static String findDataDefinitionName(JsonNode node, DefinitionPosition definitionPosition) {
        if (node.isNull()) {
            return null;
        }

        String text = "";
        if (DefinitionPosition.CHILD.is(definitionPosition)) {
            if (node.has(MagicKey.objectExt)) {
                text = node.get(MagicKey.objectExt).asText();
            } else if (node.has(MagicKey.arrayExt)) {
                text = node.get(MagicKey.arrayExt).asText();
            }
        } else {
            text = node.asText();
        }
        Map<String, Object> syntaxMap = parseSyntax(text);
        if (!MagicKey.hasAnyMagicKey(syntaxMap.keySet())) {
            return null;
        }

        Object o = syntaxMap.get(SyntaxKey.name);
        return Objects.isNull(o) ? null : String.valueOf(o);
    }

    public static boolean findDataDefinitionNullable(JsonNode node, DefinitionPosition definitionPosition) {
        if (node.isNull()) {
            return true;
        }

        String text = "";
        if (DefinitionPosition.CHILD.is(definitionPosition)) {
            if (node.has(MagicKey.objectExt)) {
                text = node.get(MagicKey.objectExt).asText();
            } else if (node.has(MagicKey.arrayExt)) {
                text = node.get(MagicKey.arrayExt).asText();
            }
        } else {
            text = node.asText();
        }
        Map<String, Object> syntaxMap = parseSyntax(text);
        if (!MagicKey.hasAnyMagicKey(syntaxMap.keySet())) {
            return false;
        }
        return syntaxMap.containsKey(SyntaxKey.nullable);
    }

    public static String findDataDefinitionRemark(JsonNode node, DefinitionPosition definitionPosition) {
        if (node.isNull()) {
            return null;
        }

        String text = "";
        if (DefinitionPosition.CHILD.is(definitionPosition)) {
            if (node.has(MagicKey.objectExt)) {
                text = node.get(MagicKey.objectExt).asText();
            } else if (node.has(MagicKey.arrayExt)) {
                text = node.get(MagicKey.arrayExt).asText();
            }
        } else {
            text = node.asText();
        }
        Map<String, Object> syntaxMap = parseSyntax(text);
        if (!MagicKey.hasAnyMagicKey(syntaxMap.keySet())) {
            return null;
        }
        Object o = syntaxMap.get(SyntaxKey.remark);
        return Objects.isNull(o) ? null : String.valueOf(o);
    }

    public static DartDataType findBasisDataType(JsonNode node) {
        if (node.isNull()) {
            return DartDataType.UNKNOWN;
        }

        String text = node.asText();
        Map<String, Object> syntaxMap = parseSyntax(text);
        if (!syntaxMap.containsKey(MagicKey.basisExt)) {
            return DartDataType.UNKNOWN;
        }

        return DartDataType.fromDartType(String.valueOf(syntaxMap.getOrDefault(SyntaxKey.type, "ANY")));
    }

    public static DartDataType findBasisArrayDataType(JsonNode node) {
        if (node.isNull()) {
            return DartDataType.UNKNOWN;
        }

        String text = node.asText();
        Map<String, Object> syntaxMap = parseSyntax(text);
        if (!syntaxMap.containsKey(MagicKey.arrayBasicExt)) {
            return DartDataType.UNKNOWN;
        }

        return DartDataType.fromDartType(String.valueOf(syntaxMap.getOrDefault(SyntaxKey.type, "ANY")));
    }

    private static Map<String, Object> parseSyntax(String syntax) {
        Map<String, Object> result = new HashMap<>();
        for (String s : syntax.split(StringConst.semicolon)) {
            String[] colon = s.trim().split(StringConst.colon);
            if (colon.length == 0) {
                continue;
            }

            if (colon.length == 1) {
                result.put(colon[0].trim(), null);
                continue;
            }

            result.put(colon[0].trim(), colon[1].trim());
        }
        return result;
    }
}
