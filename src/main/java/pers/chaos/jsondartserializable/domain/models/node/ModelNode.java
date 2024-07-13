package pers.chaos.jsondartserializable.domain.models.node;

import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Data;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.time.DateUtils;
import pers.chaos.jsondartserializable.domain.enums.DartDataType;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeDataType;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeType;
import pers.chaos.jsondartserializable.domain.models.forgenerated.DartMultiFile;
import pers.chaos.jsondartserializable.domain.models.forgenerated.DartSingleFile;
import pers.chaos.jsondartserializable.domain.models.nodedata.ModelNodeMeta;
import pers.chaos.jsondartserializable.domain.models.nodedata.ModelOutputMeta;
import pers.chaos.jsondartserializable.domain.service.DartGenOption;
import pers.chaos.jsondartserializable.domain.service.ModelNodesMgr;
import pers.chaos.jsondartserializable.domain.service.template.DartGenTemplateMgr;
import pers.chaos.jsondartserializable.domain.util.StringConst;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 模型节点
 */
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
     * 节点数据
     */
    private ModelNodeMeta nodeMeta;
    /**
     * 输出元数据
     */
    private ModelOutputMeta outputMeta;

    /**
     * 根节点-节点管理器
     */
    private ModelNodesMgr rootNodeMgr;

    private ModelNode() {
    }

    private ModelNode(JsonNode jsonNode, ModelNode parentNode) {
        this.jsonNode = jsonNode;
        this.parentNode = parentNode;
        this.childNodes = new ArrayList<>();
    }

    /**
     * 构造根节点
     *
     * @param mgr                      节点管理器对象
     * @param rootJsonNode             JSON对象
     * @param userInputRootClassname   用户输入的根对象类名
     * @param userInputRootClassRemark 用户输入的根对象注释
     * @return 仅有根对象数据的模型节点
     */
    public static ModelNode createRoot(ModelNodesMgr mgr, JsonNode rootJsonNode, String userInputRootClassname, String userInputRootClassRemark) {
        ModelNode node = new ModelNode(rootJsonNode, null);
        node.setRootNodeMgr(mgr);

        // 设置节点元数据
        ModelNodeMeta nodeMeta = new ModelNodeMeta();
        nodeMeta.setNodeType(ModelNodeType.ROOT);
        nodeMeta.setModelNodeDataType(ModelNodeDataType.OBJECT);
        // 设置节点目标数据元数据
        ModelOutputMeta outputMeta = new ModelOutputMeta();
        outputMeta.setDataType(DartDataType.OBJECT);
        outputMeta.setIsRequired(DartGenOption.Required.yes);
        outputMeta.setDefaultValue(null);
        outputMeta.setRemark(userInputRootClassRemark);
        outputMeta.setMarkJsonKeyAnno(DartGenOption.UseJsonKey.no);
        outputMeta.setClassname(DartGenOption.NameGen.CLASS.gen(userInputRootClassname));
        outputMeta.setFilename(DartGenOption.NameGen.FILE.gen(userInputRootClassname));
        outputMeta.setPropertyName(null);

        node.setNodeMeta(nodeMeta);
        node.setOutputMeta(outputMeta);
        return node;
    }

    /**
     * 创建子模型节点
     */
    public List<ModelNode> createChildNodes() {
        List<ModelNode> childNodes = new ArrayList<>();
        ModelNodeDataType nodeDataType = nodeMeta.getModelNodeDataType();
        if (ModelNodeDataType.OBJECT_ARRAY == nodeDataType) {
            JsonNode firstJsonNode = jsonNode.get(0);
            ModelNode childNode = createChildNode(firstJsonNode, this, nodeMeta.getJsonFieldName());
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
        ModelOutputMeta targetMeta = createModelTargetMeta(node, meta);
        node.setNodeMeta(meta);
        node.setOutputMeta(targetMeta);

        // 初始化子节点的子节点
        List<ModelNode> childNodes = node.createChildNodes();
        node.setChildNodes(childNodes);

        return node;
    }

    /**
     * 创建模型目标元数据
     */
    private ModelOutputMeta createModelTargetMeta(ModelNode node, ModelNodeMeta meta) {
        ModelOutputMeta targetMeta = new ModelOutputMeta();
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
            targetMeta.setClassname(DartGenOption.NameGen.CLASS.gen(meta.getJsonFieldName()));
            targetMeta.setFilename(DartGenOption.NameGen.FILE.gen(meta.getJsonFieldName()));
        } else {
            targetMeta.setClassname(null);
            targetMeta.setFilename(null);
        }
        String propertyName = DartGenOption.NameGen.PROPERTY.gen(meta.getJsonFieldName());
        targetMeta.setPropertyName(propertyName);
        targetMeta.setMarkJsonKeyAnno(!Objects.equals(propertyName, meta.getJsonFieldName()));
        targetMeta.setIsRequired(DartGenOption.Required.yes);
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
            "YYYY-MM-DD'T'HH:mm:ssZ",
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
            if (childNode.getNodeMeta().getJsonFieldName().equals(nodeName)) {
                return findChildByPathRecursive(childNode, path, index + 1);
            }
        }

        return null;
    }

    /**
     * 处理普通节点是否需要使用JsonKey注解
     */
    public void handleMarkJsonKeyAnno() {
        if (ModelNodeType.NORMAL == nodeMeta.getNodeType() && !Objects.equals(nodeMeta.getJsonFieldName(), outputMeta.getPropertyName())) {
            outputMeta.setMarkJsonKeyAnno(DartGenOption.UseJsonKey.yes);
        }
    }


    public ModelNode findRootNode() {
        if (nodeMeta.getNodeType() != ModelNodeType.ROOT && Objects.nonNull(parentNode)) {
            return parentNode.findRootNode();
        } else {
            return this;
        }
    }

    /**
     * 生成多Dart文件
     */
    public List<DartMultiFile> getMultiDartFileRecursively() {
        List<ModelNode> importModels = new ArrayList<>();
        List<DartMultiFile> output = new ArrayList<>();
        if (ModelNodeDataType.OBJECT == nodeMeta.getModelNodeDataType()) {
            // 对象类型将会优先生成它的内部子对象类型
            // 例如，对象中包含另一个对象
            // {"anotherObj":{"name":"Leo"}}
            for (ModelNode childNode : childNodes) {
                if (!childNode.getNodeMeta().isBasisModelNodeDataType()) {
                    // 添加该节点需要引用的其他节点，将在生成文件中使用如下格式：
                    // 'import xx'
                    importModels.add(childNode);

                    // 其子节点继续生成
                    List<DartMultiFile> files = childNode.getMultiDartFileRecursively();
                    output.addAll(files);
                }
            }
        } else if (ModelNodeDataType.OBJECT_ARRAY == nodeMeta.getModelNodeDataType()) {
            // 对象数组类型，将会取第1个对象生成新的对象节点，并继续处理这个新节点的子节点
            ModelNode firstChild = childNodes.get(0);
            importModels.add(firstChild);
            List<DartMultiFile> files = firstChild.getMultiDartFileRecursively();
            output.addAll(files);
            return output;
        } else {
            // dart基础数据类型不再处理
            return output;
        }

        for (ModelNode importModel : importModels) {
            VirtualFile dartFile = findRootNode().getRootNodeMgr().getFileRepository().findDartFile(importModel.getOutputMeta().getFilename());
            if (Objects.nonNull(dartFile)) {
                importModel.getOutputMeta().setFilename(importModel.getOutputMeta().getFilename() + StringConst.underline + RandomStringUtils.randomAlphanumeric(4));
            }
        }
        List<DartMultiFile> files = outputMultiDartFile(importModels);
        output.addAll(files);
        return output;
    }

    private String toDartDataType(ModelNode node, boolean nullable) {
        ModelNodeDataType nodeDataType = node.getNodeMeta().getModelNodeDataType();
        if (nodeDataType == ModelNodeDataType.OBJECT) {
            return nullable ? node.getOutputMeta().getClassname() + "?" : node.getOutputMeta().getClassname();
        } else if (nodeDataType == ModelNodeDataType.OBJECT_ARRAY) {
            ModelNode firstModel = node.getChildNodes().get(0);
            return nullable ? "List<" + firstModel.getOutputMeta().getClassname() + "?>" : "List<" + firstModel.getOutputMeta().getClassname() + ">";
        } else if (nodeDataType == ModelNodeDataType.BASIS_DATA_ARRAY) {
            ModelNode firstModel = node.getChildNodes().get(0);
            return nullable ? "List<" + firstModel.getOutputMeta().getDataType().toDartDefinition() + "?>"
                    : "List<" + firstModel.getOutputMeta().getDataType().toDartDefinition() + ">";
        } else {
            return nullable ? node.getOutputMeta().getDataType().toDartDefinition() + "?"
                    : node.getOutputMeta().getDataType().toDartDefinition();
        }
    }

    private Object getDefaultValueStr(ModelNode node, Object defVal) {
        String defaultValueStr = String.valueOf(defVal);
        switch (node.getOutputMeta().getDataType()) {
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

    private List<DartMultiFile> outputMultiDartFile(List<ModelNode> importModels) {
        StringBuilder importClassSb = new StringBuilder();
        for (ModelNode importModel : importModels) {
            importClassSb.append(DartGenTemplateMgr.formatImportClass(importModel.getOutputMeta().getFilename()));
        }
        importClassSb.append("\n");

        StringBuilder fieldSb = new StringBuilder();
        StringBuilder constructorParamSb = new StringBuilder();
        for (ModelNode childNode : childNodes) {
            boolean isRequired = childNode.getOutputMeta().getIsRequired();
            Object defaultValue = childNode.getOutputMeta().getDefaultValue();
            boolean nullable = !isRequired && Objects.isNull(defaultValue);
            // 添加字段注释
            fieldSb.append(DartGenTemplateMgr.formatFiledRemark(childNode.getOutputMeta().getRemark()));
            if (childNode.getOutputMeta().getMarkJsonKeyAnno()) {
                fieldSb.append(DartGenTemplateMgr.formatJsonKeyAnno(childNode.getNodeMeta().getJsonFieldName()));
            }
            // 字段
            fieldSb.append(DartGenTemplateMgr.formatField(toDartDataType(childNode, nullable), childNode.getOutputMeta().getPropertyName()));

            // 处理初始化函数参数
            if (isRequired && Objects.nonNull(defaultValue)) {
                constructorParamSb.append(DartGenTemplateMgr.formatRequiredConstructorWithDefaultVal(childNode.getOutputMeta().getPropertyName(), getDefaultValueStr(childNode, defaultValue)));
            } else if (isRequired) {
                constructorParamSb.append(DartGenTemplateMgr.formatRequiredConstructor(childNode.getOutputMeta().getPropertyName()));
            } else if (Objects.nonNull(defaultValue)) {
                constructorParamSb.append(DartGenTemplateMgr.formatConstructorOnlyDefaultValue(childNode.getOutputMeta().getPropertyName(), getDefaultValueStr(childNode, defaultValue)));
            } else {
                constructorParamSb.append(DartGenTemplateMgr.formatConstructorNullable(childNode.getOutputMeta().getPropertyName()));
            }
        }

        StringBuilder wholeSb = new StringBuilder(DartGenTemplateMgr.formatFileHeader());
        wholeSb.append(importClassSb);
        wholeSb.append(DartGenTemplateMgr.formatImportDartPart(outputMeta.getFilename()))
                .append(DartGenTemplateMgr.formatHeaderRemark(outputMeta.getRemark()))
                .append(DartGenTemplateMgr.formatJsonSerializablePackAnno())
                .append(DartGenTemplateMgr.formatClassName(outputMeta.getClassname()));
        wholeSb.append(fieldSb).append("\n");
        wholeSb.append(DartGenTemplateMgr.formatConstructor(outputMeta.getClassname(), CollectionUtils.isNotEmpty(childNodes) ? "{" + constructorParamSb + "}" : ""))
                .append(DartGenTemplateMgr.formatFromJson(outputMeta.getClassname()))
                .append(DartGenTemplateMgr.formatToJson(outputMeta.getClassname()))
                .append(DartGenTemplateMgr.formatEOF());

        return Collections.singletonList(new DartMultiFile(this, wholeSb.toString()));
    }

    /**
     * 生成单Dart文件
     */
    public DartSingleFile outputSingleDartFile() {
        List<ModelNode> allObjectNodes = new ArrayList<>();
        allObjectNodes.add(this);
        collectAllChildObjectNodes(allObjectNodes);

        StringBuilder headerSb = new StringBuilder();
        headerSb.append(DartGenTemplateMgr.formatFileHeader())
                .append("\n")
                .append(DartGenTemplateMgr.formatImportDartPart(outputMeta.getFilename()));

        StringBuilder classSb = getAllDartClassFormatContent(allObjectNodes);
        StringBuilder wholeSb = new StringBuilder(headerSb);
        wholeSb.append(classSb);
        VirtualFile dartFile = findRootNode().getRootNodeMgr().getFileRepository().findDartFile(outputMeta.getFilename());
        if (Objects.nonNull(dartFile)) {
            outputMeta.setFilename(outputMeta.getFilename() + StringConst.underline + RandomStringUtils.randomAlphanumeric(4));
        }
        return new DartSingleFile(this, wholeSb.toString());
    }

    private void collectAllChildObjectNodes(List<ModelNode> allObjectNodes) {
        for (ModelNode childNode : getChildNodes()) {
            if (!childNode.getNodeMeta().isBasisModelNodeDataType()) {
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
                boolean isRequired = childNode.getOutputMeta().getIsRequired();
                Object defaultValue = childNode.getOutputMeta().getDefaultValue();
                boolean nullable = !isRequired && Objects.isNull(defaultValue);
                // 添加字段注释
                fieldSb.append(DartGenTemplateMgr.formatFiledRemark(childNode.getOutputMeta().getRemark()));
                if (childNode.getOutputMeta().getMarkJsonKeyAnno()) {
                    fieldSb.append(DartGenTemplateMgr.formatJsonKeyAnno(childNode.getNodeMeta().getJsonFieldName()));
                }
                // 字段
                fieldSb.append(DartGenTemplateMgr.formatField(toDartDataType(childNode, nullable), childNode.getOutputMeta().getPropertyName()));

                // 处理初始化函数参数
                if (isRequired && Objects.nonNull(defaultValue)) {
                    constructorParamSb.append(DartGenTemplateMgr.formatRequiredConstructorWithDefaultVal(childNode.getOutputMeta().getPropertyName(), getDefaultValueStr(childNode, defaultValue)));
                } else if (isRequired) {
                    constructorParamSb.append(DartGenTemplateMgr.formatRequiredConstructor(childNode.getOutputMeta().getPropertyName()));
                } else if (Objects.nonNull(defaultValue)) {
                    constructorParamSb.append(DartGenTemplateMgr.formatConstructorOnlyDefaultValue(childNode.getOutputMeta().getPropertyName(), getDefaultValueStr(childNode, defaultValue)));
                } else {
                    constructorParamSb.append(DartGenTemplateMgr.formatConstructorNullable(childNode.getOutputMeta().getPropertyName()));
                }
            }

            StringBuilder wholeSb = new StringBuilder();
            wholeSb.append(DartGenTemplateMgr.formatHeaderRemark(node.getOutputMeta().getRemark()))
                    .append(DartGenTemplateMgr.formatJsonSerializablePackAnno())
                    .append(DartGenTemplateMgr.formatClassName(node.getOutputMeta().getClassname()));
            wholeSb.append(fieldSb).append("\n");
            wholeSb.append(DartGenTemplateMgr.formatConstructor(node.getOutputMeta().getClassname(), CollectionUtils.isNotEmpty(childNodes) ? "{" + constructorParamSb + "}" : ""))
                    .append(DartGenTemplateMgr.formatFromJson(node.getOutputMeta().getClassname()))
                    .append(DartGenTemplateMgr.formatToJson(node.getOutputMeta().getClassname()))
                    .append(DartGenTemplateMgr.formatEOF());

            sb.append("\n\n").append(wholeSb);
        }

        return sb;
    }
}
