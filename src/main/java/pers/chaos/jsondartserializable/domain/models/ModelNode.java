package pers.chaos.jsondartserializable.domain.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.time.DateUtils;
import pers.chaos.jsondartserializable.common.models.StringConst;
import pers.chaos.jsondartserializable.domain.enums.DartConst;
import pers.chaos.jsondartserializable.domain.enums.DartDataType;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeDataType;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeType;
import pers.chaos.jsondartserializable.domain.template.DartClassTemplate;
import pers.chaos.jsondartserializable.domain.util.DartUtil;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Data
public class ModelNode {
    /**
     * JsonNode节点
     */
    private JsonNode jsonNode;
    /**
     * 子节点
     */
    private List<ModelNode> childNodes;
    /**
     * 父节点
     */
    private ModelNode parentNode;
    /**
     * 元数据
     */
    private ModelNodeMeta meta;
    /**
     * 目标属性
     */
    private ModelTargetMeta targetMeta;

    /**
     * 管理器
     */
    private ModelNodeMgr mgr;

    public ModelNode(JsonNode jsonNode, ModelNode parentNode) {
        this.jsonNode = jsonNode;
        this.parentNode = parentNode;
        this.childNodes = new ArrayList<>();
    }

    public static ModelNode createRoot(JsonNode jsonNode, String rootClassName, String remark) {
        ModelNode node = new ModelNode(jsonNode, null);

        ModelNodeMeta meta = new ModelNodeMeta();
        meta.setNodeType(ModelNodeType.ROOT);
        meta.setModelNodeDataType(ModelNodeDataType.OBJECT);

        ModelTargetMeta targetMeta = new ModelTargetMeta();
        targetMeta.setDataType(DartDataType.OBJECT);
        targetMeta.setIsRequired(DartConst.Required.yes);
        targetMeta.setDefaultValue(null);
        targetMeta.setRemark(remark);
        targetMeta.setMarkJsonKeyAnno(DartConst.UseJsonKey.no);
        targetMeta.setClassName(DartUtil.toClassName(rootClassName));
        targetMeta.setFilename(DartUtil.toFileName(rootClassName));
        targetMeta.setPropertyName(null);

        node.setMeta(meta);
        node.setTargetMeta(targetMeta);
        return node;
    }

    public List<ModelNode> createChildNodes() {
        List<ModelNode> childNodes = new ArrayList<>();
        ModelNodeDataType nodeDataType = meta.getModelNodeDataType();
        if (ModelNodeDataType.OBJECT_ARRAY == nodeDataType) {
            JsonNode firstJsonNode = jsonNode.get(0);
            ModelNode childNode = createChildNode(firstJsonNode, this, meta.getJsonFieldName());
            childNodes.add(childNode);
        } else if (ModelNodeDataType.BASIS_DATA_ARRAY == nodeDataType) {
            JsonNode firstJsonNode = jsonNode.get(0);
            ModelNode childNode = createChildNode(firstJsonNode, this, "NORMAL_FIELD_ARRAY");
            childNodes.add(childNode);
        } else {
            jsonNode.fieldNames().forEachRemaining(name -> {
                JsonNode childJsonNode = jsonNode.get(name);
                ModelNode childNode = createChildNode(childJsonNode, this, name);
                childNodes.add(childNode);
            });
        }
        return childNodes;
    }

    private ModelNode createChildNode(JsonNode jsonNode, ModelNode parentNode, String jsonFieldName) {
        ModelNode node = new ModelNode(jsonNode, parentNode);
        ModelNodeMeta meta = createModelMeta(node, jsonFieldName);
        ModelTargetMeta targetMeta = createModelTargetMeta(node, meta);
        node.setMeta(meta);
        node.setTargetMeta(targetMeta);

        // 初始化子节点的子节点
        List<ModelNode> childNodes = node.createChildNodes();
        node.setChildNodes(childNodes);

        return node;
    }

    public ModelNode findRootNode() {
        if (meta.getNodeType() != ModelNodeType.ROOT && Objects.nonNull(parentNode)) {
            return parentNode.findRootNode();
        } else {
            return this;
        }
    }

    /**
     * 创建模型目标元数据
     */
    private ModelTargetMeta createModelTargetMeta(ModelNode node, ModelNodeMeta meta) {
        ModelTargetMeta targetMeta = new ModelTargetMeta();
        ModelNodeDataType modelNodeDataType = meta.getModelNodeDataType();
        JsonNode jsonNode = node.getJsonNode();
        DartDataType dataType = DartDataType.OBJECT;
        if (meta.isBasisModelNodeDataType()) {
            if (modelNodeDataType == ModelNodeDataType.BASIS_DATA) {
                dataType = getDartDataType(jsonNode, meta);
            } else if (modelNodeDataType == ModelNodeDataType.BASIS_DATA_ARRAY) {
                if (!jsonNode.isEmpty()) {
                    JsonNode firstNode = jsonNode.get(0);
                    if (firstNode.isObject()) {
                        // 注释 dataType = DartDataType.OBJECT;
                    } else if (firstNode.isArray()) {
                        throw new RuntimeException("Not support analysis array nesting");
                    } else {
                        dataType = getDartDataType(firstNode, meta);
                    }
                } else {
                    // 原始JSON字符串是个空数组，则默认将数组的基本类型设置为字符串数组
                    dataType = DartDataType.STRING;
                }
            }
        }
        targetMeta.setDataType(dataType);

        if (ModelNodeDataType.OBJECT == modelNodeDataType
                || ModelNodeDataType.OBJECT_ARRAY == modelNodeDataType) {
            targetMeta.setClassName(DartUtil.toClassName(meta.getJsonFieldName()));
            targetMeta.setFilename(DartUtil.toFileName(meta.getJsonFieldName()));
        } else {
            targetMeta.setClassName(null);
            targetMeta.setFilename(null);
        }
        String propertyName = DartUtil.toPropertyName(meta.getJsonFieldName());
        targetMeta.setPropertyName(propertyName);
        targetMeta.setMarkJsonKeyAnno(!Objects.equals(propertyName, meta.getJsonFieldName()));
        targetMeta.setIsRequired(DartConst.Required.yes);
        return targetMeta;
    }

    /**
     * 获取Dart数据类型
     */
    private DartDataType getDartDataType(JsonNode jsonNode, ModelNodeMeta meta) {
        DartDataType dartDataType = DartDataType.OBJECT;
        if (jsonNode.isBoolean()) {
            dartDataType = DartDataType.BOOLEAN;
        } else if (jsonNode.isTextual()) {
            boolean isDateString = tryParseTextDate(jsonNode.asText(), meta.getJsonFieldName());
            dartDataType = isDateString ? DartDataType.DATE_TIME : DartDataType.STRING;
        } else if (jsonNode.isInt() || jsonNode.isLong() || jsonNode.isBigInteger()) {
            dartDataType = DartDataType.INT;
        } else if (jsonNode.isDouble() || jsonNode.isFloat()) {
            dartDataType = DartDataType.DOUBLE;
        }
        return dartDataType;
    }

    final static String[] DATE_FORMATS = new String[]{
            "HH:mm:ss",
            "hh:mm:ss a",
            "YYYY-MM-DD HH:mm:ss",
            "YYYY-MM-DD HH:mm:ss Z",
            "YYYY年MM月DD日",
            "MM/DD/YYYY",
            "DD/MM/YYYY",
            "YY-MM-DD",
            "MM/DD",
            "HH:mm"
    };

    /**
     * 尝试解析字符串是否为时间字符串
     */
    private boolean tryParseTextDate(String text, String jsonFieldName) {
        if (jsonFieldName.toLowerCase().contains("time")) {
            return true;
        }

        try {
            DateUtils.parseDate(text, DATE_FORMATS);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    /**
     * 创建模型元数据
     */
    private ModelNodeMeta createModelMeta(ModelNode node, String jsonFieldName) {
        ModelNodeMeta meta = new ModelNodeMeta();
        ModelNodeDataType modelNodeDataType = getModelNodeDataTypeByJsonNode(node.getJsonNode());

        meta.setModelNodeDataType(modelNodeDataType);
        meta.setNodeType(ModelNodeType.NORMAL);
        meta.setJsonFieldName(jsonFieldName);
        return meta;
    }

    private ModelNodeDataType getModelNodeDataTypeByJsonNode(JsonNode node) {
        ModelNodeDataType dataType;
        if (node.isObject()) {
            dataType = ModelNodeDataType.OBJECT;
        } else if (node.isArray()) {
            if (!node.isEmpty()) {
                JsonNode firstChildNode = node.get(0);
                if (firstChildNode.isObject()) {
                    dataType = ModelNodeDataType.OBJECT_ARRAY;
                } else if (firstChildNode.isArray()) {
                    throw new RuntimeException("Not support analysis array nesting");
                } else {
                    dataType = ModelNodeDataType.BASIS_DATA_ARRAY;
                }
            } else {
                dataType = ModelNodeDataType.BASIS_DATA_ARRAY;
            }
        } else {
            dataType = ModelNodeDataType.BASIS_DATA;
        }
        return dataType;
    }

    public ModelNode findChildByPath(String path) {
        return findChildByPathRecursive(this, path.split("\\."), 0);
    }

    private ModelNode findChildByPathRecursive(ModelNode node, String[] path, int index) {
        if (index >= path.length) {
            return node;
        }

        String nodeName = path[index];
        for (ModelNode childNode : node.getChildNodes()) {
            if (childNode.getMeta().getJsonFieldName().equals(nodeName)) {
                return findChildByPathRecursive(childNode, path, index + 1);
            }
        }

        return null;
    }

    /**
     * 处理普通节点是否需要使用JsonKey注解
     */
    public void handleMarkJsonKeyAnno() {
        if (ModelNodeType.NORMAL == meta.getNodeType() && !Objects.equals(meta.getJsonFieldName(), targetMeta.getPropertyName())) {
            targetMeta.setMarkJsonKeyAnno(DartConst.UseJsonKey.yes);
        }
    }

    /**
     * 生成多Dart文件
     */
    public List<MultiDartFile> getMultiDartFileRecursively() {
        List<ModelNode> importModels = new ArrayList<>();
        List<MultiDartFile> output = new ArrayList<>();
        if (ModelNodeDataType.OBJECT == meta.getModelNodeDataType()) {
            // 对象类型将会优先生成它的内部子对象类型
            // 例如，对象中包含另一个对象
            // {"anotherObj":{"name":"Leo"}}
            for (ModelNode childNode : childNodes) {
                if (!childNode.getMeta().isBasisModelNodeDataType()) {
                    // 添加该节点需要引用的其他节点，将在生成文件中使用如下格式：
                    // 'import xx'
                    importModels.add(childNode);

                    // 其子节点继续生成
                    List<MultiDartFile> files = childNode.getMultiDartFileRecursively();
                    output.addAll(files);
                }
            }
        } else if (ModelNodeDataType.OBJECT_ARRAY == meta.getModelNodeDataType()) {
            // 对象数组类型，将会取第1个对象生成新的对象节点，并继续处理这个新节点的子节点
            ModelNode firstChild = childNodes.get(0);
            importModels.add(firstChild);
            List<MultiDartFile> files = firstChild.getMultiDartFileRecursively();
            output.addAll(files);
            return output;
        } else {
            // dart基础数据类型不再处理
            return output;
        }

        for (ModelNode importModel : importModels) {
            VirtualFile dartFile = findRootNode().getMgr().getFileRepo().findDartFile(importModel.getTargetMeta().getFilename());
            if (Objects.nonNull(dartFile)) {
                importModel.getTargetMeta().setFilename(importModel.getTargetMeta().getFilename() + StringConst.underline + RandomStringUtils.randomAlphanumeric(4));
            }
        }
        List<MultiDartFile> files = outputMultiDartFile(importModels);
        output.addAll(files);
        return output;
    }

    private List<MultiDartFile> outputMultiDartFile(List<ModelNode> importModels) {
        StringBuilder importClassSb = new StringBuilder();
        for (ModelNode importModel : importModels) {
            importClassSb.append(DartClassTemplate.formatImportClass(importModel.getTargetMeta().getFilename()));
        }
        importClassSb.append("\n");

        StringBuilder fieldSb = new StringBuilder();
        StringBuilder constructorParamSb = new StringBuilder();
        for (ModelNode childNode : childNodes) {
            boolean isRequired = childNode.getTargetMeta().getIsRequired();
            Object defaultValue = childNode.getTargetMeta().getDefaultValue();
            boolean nullable = !isRequired && Objects.isNull(defaultValue);
            // 添加字段注释
            fieldSb.append(DartClassTemplate.formatFiledRemark(childNode.getTargetMeta().getRemark()));
            if (childNode.getTargetMeta().getMarkJsonKeyAnno()) {
                fieldSb.append(DartClassTemplate.formatJsonKeyAnno(childNode.getMeta().getJsonFieldName()));
            }
            // 字段
            fieldSb.append(DartClassTemplate.formatField(toDartDataType(childNode, nullable), childNode.getTargetMeta().getPropertyName()));

            // 处理初始化函数参数
            if (isRequired && Objects.nonNull(defaultValue)) {
                constructorParamSb.append(DartClassTemplate.formatRequiredConstructorWithDefaultVal(childNode.getTargetMeta().getPropertyName(), getDefaultValueStr(childNode, defaultValue)));
            } else if (isRequired) {
                constructorParamSb.append(DartClassTemplate.formatRequiredConstructor(childNode.getTargetMeta().getPropertyName()));
            } else if (Objects.nonNull(defaultValue)) {
                constructorParamSb.append(DartClassTemplate.formatConstructorOnlyDefaultValue(childNode.getTargetMeta().getPropertyName(), getDefaultValueStr(childNode, defaultValue)));
            } else {
                constructorParamSb.append(DartClassTemplate.formatConstructorNullable(childNode.getTargetMeta().getPropertyName()));
            }
        }

        StringBuilder wholeSb = new StringBuilder(DartClassTemplate.formatFileHeader());
        wholeSb.append(importClassSb);
        wholeSb.append(DartClassTemplate.formatImportDartPart(targetMeta.getFilename()))
                .append(DartClassTemplate.formatHeaderRemark(targetMeta.getRemark()))
                .append(DartClassTemplate.formatJsonSerializablePackAnno())
                .append(DartClassTemplate.formatClassName(targetMeta.getClassName()));
        wholeSb.append(fieldSb).append("\n");
        wholeSb.append(DartClassTemplate.formatConstructor(targetMeta.getClassName(), CollectionUtils.isNotEmpty(childNodes) ? "{" + constructorParamSb + "}" : ""))
                .append(DartClassTemplate.formatFromJson(targetMeta.getClassName()))
                .append(DartClassTemplate.formatToJson(targetMeta.getClassName()))
                .append(DartClassTemplate.formatEOF());

        return Collections.singletonList(new MultiDartFile(this, wholeSb.toString()));
    }

    private String toDartDataType(ModelNode node, boolean nullable) {
        ModelNodeDataType nodeDataType = node.getMeta().getModelNodeDataType();
        if (nodeDataType == ModelNodeDataType.OBJECT) {
            return nullable ? node.getTargetMeta().getClassName() + "?" : node.getTargetMeta().getClassName();
        } else if (nodeDataType == ModelNodeDataType.OBJECT_ARRAY) {
            ModelNode firstModel = node.getChildNodes().get(0);
            return nullable ? "List<" + firstModel.getTargetMeta().getClassName() + "?>" : "List<" + firstModel.getTargetMeta().getClassName() + ">";
        } else if (nodeDataType == ModelNodeDataType.BASIS_DATA_ARRAY) {
            ModelNode firstModel = node.getChildNodes().get(0);
            return nullable ? "List<" + firstModel.getTargetMeta().getDataType().toDataStr() + "?>"
                    : "List<" + firstModel.getTargetMeta().getDataType().toDataStr() + ">";
        } else {
            return nullable ? node.getTargetMeta().getDataType().toDataStr() + "?"
                    : node.getTargetMeta().getDataType().toDataStr();
        }
    }

    private Object getDefaultValueStr(ModelNode node, Object defVal) {
        String defaultValueStr = String.valueOf(defVal);
        switch (node.getTargetMeta().getDataType()) {
            case INT:
                if (defaultValueStr.contains(".")) {
                    return Long.parseLong(defaultValueStr.split("\\.")[0]);
                } else {
                    try {
                        return Long.parseLong(defaultValueStr);
                    } catch (Exception e) {
                        return 0;
                    }
                }
            case DOUBLE:
                if (defaultValueStr.contains(".")) {
                    return defVal;
                } else {
                    try {
                        return Double.parseDouble(Long.parseLong(defaultValueStr) + ".0");
                    } catch (Exception e) {
                        return Double.parseDouble("0.0");
                    }
                }
            case BOOLEAN:
                if ("true".equalsIgnoreCase(defaultValueStr) || "1".equalsIgnoreCase(defaultValueStr)) {
                    return true;
                } else if ("false".equalsIgnoreCase(defaultValueStr) || "0".equalsIgnoreCase(defaultValueStr)) {
                    return false;
                }

                return true;
            case DATE_TIME:
                return "";
            default:
            case STRING:
                return "'" + defVal + "'";
        }
    }

    /**
     * 生成单Dart文件
     */
    public SingleDartFile outputSingleDartFile() {
        List<ModelNode> allObjectNodes = new ArrayList<>();
        allObjectNodes.add(this);
        collectAllChildObjectNodes(allObjectNodes);

        StringBuilder headerSb = new StringBuilder();
        headerSb.append(DartClassTemplate.formatFileHeader())
                .append("\n")
                .append(DartClassTemplate.formatImportDartPart(targetMeta.getFilename()));

        StringBuilder classSb = getAllDartClassFormatContent(allObjectNodes);
        StringBuilder wholeSb = new StringBuilder(headerSb);
        wholeSb.append(classSb);
        VirtualFile dartFile = findRootNode().getMgr().getFileRepo().findDartFile(targetMeta.getFilename());
        if (Objects.nonNull(dartFile)) {
            targetMeta.setFilename(targetMeta.getFilename() + StringConst.underline + RandomStringUtils.randomAlphanumeric(4));
        }
        return new SingleDartFile(this, wholeSb.toString());
    }

    private void collectAllChildObjectNodes(List<ModelNode> allObjectNodes) {
        for (ModelNode childNode : getChildNodes()) {
            if (!childNode.getMeta().isBasisModelNodeDataType()) {
                // 非基础数据类型的节点将会被单独生成一个dart文件中
                allObjectNodes.add(childNode);

                // 继续处理子节点
                childNode.collectAllChildObjectNodes(allObjectNodes);
            }
        }
    }

    private StringBuilder getAllDartClassFormatContent(List<ModelNode> allObjectNodes) {
        StringBuilder sb = new StringBuilder();

        for (ModelNode node : allObjectNodes) {
            StringBuilder fieldSb = new StringBuilder();
            StringBuilder constructorParamSb = new StringBuilder();
            for (ModelNode childNode : node.getChildNodes()) {
                boolean isRequired = childNode.getTargetMeta().getIsRequired();
                Object defaultValue = childNode.getTargetMeta().getDefaultValue();
                boolean nullable = !isRequired && Objects.isNull(defaultValue);
                // 添加字段注释
                fieldSb.append(DartClassTemplate.formatFiledRemark(childNode.getTargetMeta().getRemark()));
                if (childNode.getTargetMeta().getMarkJsonKeyAnno()) {
                    fieldSb.append(DartClassTemplate.formatJsonKeyAnno(childNode.getMeta().getJsonFieldName()));
                }
                // 字段
                fieldSb.append(DartClassTemplate.formatField(toDartDataType(childNode, nullable), childNode.getTargetMeta().getPropertyName()));

                // 处理初始化函数参数
                if (isRequired && Objects.nonNull(defaultValue)) {
                    constructorParamSb.append(DartClassTemplate.formatRequiredConstructorWithDefaultVal(childNode.getTargetMeta().getPropertyName(), getDefaultValueStr(childNode, defaultValue)));
                } else if (isRequired) {
                    constructorParamSb.append(DartClassTemplate.formatRequiredConstructor(childNode.getTargetMeta().getPropertyName()));
                } else if (Objects.nonNull(defaultValue)) {
                    constructorParamSb.append(DartClassTemplate.formatConstructorOnlyDefaultValue(childNode.getTargetMeta().getPropertyName(), getDefaultValueStr(childNode, defaultValue)));
                } else {
                    constructorParamSb.append(DartClassTemplate.formatConstructorNullable(childNode.getTargetMeta().getPropertyName()));
                }
            }

            StringBuilder wholeSb = new StringBuilder();
            wholeSb.append(DartClassTemplate.formatHeaderRemark(node.getTargetMeta().getRemark()))
                    .append(DartClassTemplate.formatJsonSerializablePackAnno())
                    .append(DartClassTemplate.formatClassName(node.getTargetMeta().getClassName()));
            wholeSb.append(fieldSb).append("\n");
            wholeSb.append(DartClassTemplate.formatConstructor(node.getTargetMeta().getClassName(), CollectionUtils.isNotEmpty(childNodes) ? "{" + constructorParamSb + "}" : ""))
                    .append(DartClassTemplate.formatFromJson(node.getTargetMeta().getClassName()))
                    .append(DartClassTemplate.formatToJson(node.getTargetMeta().getClassName()))
                    .append(DartClassTemplate.formatEOF());

            sb.append("\n\n").append(wholeSb);
        }

        return sb;
    }
}
