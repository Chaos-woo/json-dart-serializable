package pers.chaos.jsondartserializable.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.lang3.RandomStringUtils;
import pers.chaos.jsondartserializable.core.constants.GeneratedTemplate;
import pers.chaos.jsondartserializable.core.enums.DartDataTypeEnum;
import pers.chaos.jsondartserializable.core.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.utils.DartClassFileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class MappingModelNode {
    // 原始JSON字段名
    private String jsonFieldName;
    // 生成的dart字段名
    private String dartPropertyName;
    // dart基本数据类型，例如， String，int
    private DartDataTypeEnum dartDataTypeEnum = DartDataTypeEnum.OBJECT;
    // 当前dart字段是否是必填，必填类型在dart空安全版本初始化函数中需要使用required关键字
    private boolean dartPropertyRequired = true;
    // 当前dart属性默认值
    private Object dartPropertyDefaultValue;
    // 生成后dart属性描述，用于注释描述字段的作用
    private String description;


    // 当前结点类型为Object或数组中的Object时，需要生成对应的dart文件名
    private String dartFileName;
    // 当前结点类型为Object或数组中的Object时，需要生成对应的dart类名
    private String className;

    // JSON结点
    private final JsonNode node;
    // 自定义分析后的JSON类型枚举
    private JsonTypeEnum jsonTypeEnum;
    // 当前结点是否是根节点
    private final boolean isRoot;
    // mark whether generated need using @JsonKey annotation convert original json filed name
    private boolean markJsonKeyAnno = false;
    // 当前结点下的子节点
    private final List<MappingModelNode> childModelNodes;

    public MappingModelNode(String jsonFieldName, JsonNode node, boolean isRoot) {
        this.jsonFieldName = jsonFieldName;
        this.node = node;
        this.childModelNodes = new ArrayList<>();
        this.isRoot = isRoot;

        /**
         * 映射为自定义Json类型枚举
         * @see JsonTypeEnum
         */
        mappingCustomJsonTypeEnum();
        /**
         * 映射为自定义Dart数据类型枚举
         * @see DartDataTypeEnum
         */
        mappingCustomDartTypeEnum();
        // 处理Dart文件和Dart类名
        mappingCustomDartClassName();
        // 处理Dart类中的属性名
        mappingCustomDartPropertyName();
    }

    public boolean isBasisJsonType() {
        return jsonTypeEnum == JsonTypeEnum.BASIS_TYPE || jsonTypeEnum == JsonTypeEnum.BASIS_TYPE_ARRAY;
    }

    public void mappingCustomJsonTypeEnum() {
        if (node.isObject()) {
            jsonTypeEnum = JsonTypeEnum.OBJECT;

            if (!isRoot) {
                node.fieldNames().forEachRemaining(fieldName -> {
                    JsonNode childNode = node.get(fieldName);
                    MappingModelNode mm = new MappingModelNode(fieldName, childNode, false);
                    childModelNodes.add(mm);
                });
            }

        } else if (node.isArray()) {
            if (!node.isEmpty()) {
                JsonNode firstChildNode = node.get(0);
                if (firstChildNode.isObject()) {
                    jsonTypeEnum = JsonTypeEnum.OBJECT_ARRAY;
                    // JSON中对象数组字段名将被用于其生成类的dart类名和dart文件名，
                    // 该JSON的对象数组字段名将被用于其父节点的属性名
                    childModelNodes.add(new MappingModelNode(this.jsonFieldName, firstChildNode, false));
                } else if (firstChildNode.isArray()) {
                    throw new RuntimeException("Not support analysis array nesting");
                } else {
                    jsonTypeEnum = JsonTypeEnum.BASIS_TYPE_ARRAY;
                    childModelNodes.add(new MappingModelNode("NORMAL_FIELD_ARRAY", firstChildNode, false));
                }
            }

            // do nothing
        } else {
            jsonTypeEnum = JsonTypeEnum.BASIS_TYPE;
        }
    }

    public void mappingCustomDartClassName() {
        if (jsonTypeEnum == JsonTypeEnum.OBJECT) {
            this.className = DartClassFileUtils.getDartClassName(this.jsonFieldName);
            this.dartFileName = DartClassFileUtils.getDartFileNameByClassName(this.jsonFieldName);
        } else if (jsonTypeEnum == JsonTypeEnum.OBJECT_ARRAY) {
            this.className = DartClassFileUtils.getDartClassName(this.jsonFieldName);
            this.dartFileName = DartClassFileUtils.getDartFileNameByClassName(this.jsonFieldName);
        }
    }

    public void mappingCustomDartTypeEnum() {
        if (isBasisJsonType()) {
            if (jsonTypeEnum == JsonTypeEnum.BASIS_TYPE) {
                this.dartDataTypeEnum = getCustomDartDataType(node);
            } else if (jsonTypeEnum == JsonTypeEnum.BASIS_TYPE_ARRAY) {
                if (!node.isEmpty()) {
                    JsonNode firstChildNode = node.get(0);
                    if (firstChildNode.isObject()) {
                        this.dartDataTypeEnum = DartDataTypeEnum.OBJECT;
                    } else if (firstChildNode.isArray()) {
                        throw new RuntimeException("Not support analysis array nesting");
                    } else {
                        this.dartDataTypeEnum = getCustomDartDataType(firstChildNode);
                    }
                } else {
                    // 原始JSON字符串是个空数组，则默认将数组的基本类型设置为字符串数组
                    this.dartDataTypeEnum = DartDataTypeEnum.STRING;
                }
            }
        }
    }

    private void mappingCustomDartPropertyName() {
        this.dartPropertyName = DartClassFileUtils.getDartPropertyName(this.jsonFieldName);
        if (!this.jsonFieldName.equals(this.dartPropertyName)) {
            this.markJsonKeyAnno = true;
        }
    }

    private DartDataTypeEnum getCustomDartDataType(JsonNode node) {
        DartDataTypeEnum dartDataTypeEnum = DartDataTypeEnum.OBJECT;
        if (node.isBoolean()) {
            dartDataTypeEnum = DartDataTypeEnum.BOOLEAN;
        } else if (node.isTextual()) {
            boolean isDateString = tryParseTextDate(node.asText());
            dartDataTypeEnum = isDateString ? DartDataTypeEnum.DATETIME : DartDataTypeEnum.STRING;
        } else if (node.isInt() || node.isLong() || node.isBigInteger()) {
            dartDataTypeEnum = DartDataTypeEnum.INT;
        } else if (node.isDouble() || node.isFloat()) {
            dartDataTypeEnum = DartDataTypeEnum.DOUBLE;
        }
        return dartDataTypeEnum;
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

    private boolean tryParseTextDate(String text) {
        try {
            DateUtils.parseDate(text,  DATE_FORMATS);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public void generateMultiDartFilesRecursively(final VirtualFile parent, final Project project) {
        List<MappingModelNode> importModels = new ArrayList<>();

        if (jsonTypeEnum == JsonTypeEnum.OBJECT) {
            if (CollectionUtils.isNotEmpty(childModelNodes)) {
                // 对象类型将会优先生成它的内部子对象类型
                // 例如，对象中包含另一个对象
                // {"anotherObj":{"name":"Leo"}}
                for (MappingModelNode childNode : childModelNodes) {
                    if (!childNode.isBasisJsonType()) {
                        // 添加该节点需要引用的其他节点，将在生成文件中使用如下格式：
                        // 'import xx'
                        importModels.add(childNode);

                        // 其子节点继续生成
                        childNode.generateMultiDartFilesRecursively(parent, project);
                    }
                }
            }
        } else if (jsonTypeEnum == JsonTypeEnum.OBJECT_ARRAY) {
            // 对象数组类型，将会取第1个对象生成新的对象节点，并继续处理这个新节点的子节点
            MappingModelNode mappingModelNode = childModelNodes.get(0);
            importModels.add(mappingModelNode);
            mappingModelNode.generateMultiDartFilesRecursively(parent, project);
            return;
        } else {
            // dart基础数据类型不再处理
            return;
        }

        // 最后依据处理后的MappingModel节点进行文件生成
        generatedDartFileOfCurrentModel(childModelNodes, importModels, parent, project);
    }

    // 参考: https://pub.dev/packages/json_serializable
    // format refer [Example]
    private void generatedDartFileOfCurrentModel(List<MappingModelNode> currentNodeInnerMappingModelNodes,
                                                 List<MappingModelNode> importModels,
                                                 VirtualFile parent, Project project) {

        AtomicReference<VirtualFile> fileRefer = createVirtualFileAndReturnFileHandle(parent);

        StringBuilder importFileStringBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(importModels)) {
            for (MappingModelNode importModel : importModels) {
                importFileStringBuilder.append(String.format(GeneratedTemplate.Header.otherFileImport, importModel.getDartFileName() + ".dart"));
            }
            importFileStringBuilder.append("\n");
        }

        StringBuilder fieldsStringBuilder = new StringBuilder();
        StringBuilder constructorParametersStringBuilder = new StringBuilder();
        if (CollectionUtils.isNotEmpty(currentNodeInnerMappingModelNodes)) {
            for (MappingModelNode childNode : currentNodeInnerMappingModelNodes) {
                boolean isRequired = childNode.isDartPropertyRequired();
                Object defaultValue = childNode.getDartPropertyDefaultValue();
                boolean nullable = !isRequired && Objects.isNull(defaultValue);

                // 添加字段注释
                fieldsStringBuilder.append(String.format(GeneratedTemplate.ClassBody.fieldDescription, childNode.getDescription()));

                if (childNode.isMarkJsonKeyAnno()) {
                    fieldsStringBuilder.append(String.format(GeneratedTemplate.ClassBody.fieldJsonKey, childNode.getJsonFieldName()));
                }

                // 字段
                fieldsStringBuilder.append(
                        String.format(GeneratedTemplate.ClassBody.field,
                                this.convertDartFieldType(childNode, nullable),
                                childNode.getDartPropertyName())
                );

                // 处理初始化函数参数
                if (isRequired && Objects.nonNull(defaultValue)) {
                    constructorParametersStringBuilder.append(
                            String.format(GeneratedTemplate.ClassBody.requiredConstructorWithDefaultValPart,
                                    childNode.getDartPropertyName(),
                                    getMappingModelDefaultValueString(childNode, defaultValue))
                    );
                } else if (isRequired) {
                    constructorParametersStringBuilder.append(
                            String.format(GeneratedTemplate.ClassBody.requiredConstructorPart,
                                    childNode.getDartPropertyName())
                    );
                } else if (Objects.nonNull(defaultValue)) {
                    constructorParametersStringBuilder.append(
                            String.format(GeneratedTemplate.ClassBody.constructorOnlyDefaultValPart,
                                    childNode.getDartPropertyName(),
                                    getMappingModelDefaultValueString(childNode, defaultValue))
                    );
                } else {
                    constructorParametersStringBuilder.append(
                            String.format(GeneratedTemplate.ClassBody.constructorNullablePart,
                                    childNode.getDartPropertyName())
                    );
                }
            }
        }

        StringBuilder completeStringBuilder = new StringBuilder();
        completeStringBuilder.append(GeneratedTemplate.Header.generatedFileHeaders.get(0))
                .append(GeneratedTemplate.Header.generatedFileHeaders.get(1))
                .append(GeneratedTemplate.Header.generatedFileHeaders.get(2))
                .append(GeneratedTemplate.Header.jsonSerializablePack);
        if (CollectionUtils.isNotEmpty(importModels)) {
            completeStringBuilder.append(importFileStringBuilder);
        } else {
            completeStringBuilder.append("\n");
        }

        completeStringBuilder.append(String.format(GeneratedTemplate.Header.gFileImport, this.getDartFileName()))
                .append(String.format(GeneratedTemplate.Header.description, this.getDescription()))
                .append(GeneratedTemplate.Header.jsonSerializablePackAnno)
                .append(String.format(GeneratedTemplate.ClassBody.className, this.getClassName()));

        if (CollectionUtils.isNotEmpty(currentNodeInnerMappingModelNodes)) {
            completeStringBuilder.append(fieldsStringBuilder);
            completeStringBuilder.append("\n");
        }

        completeStringBuilder.append(
                        String.format(GeneratedTemplate.ClassBody.constructor,
                                this.getClassName(),
                                CollectionUtils.isNotEmpty(currentNodeInnerMappingModelNodes) ? "{" + constructorParametersStringBuilder.toString() + "}" : ""))
                .append(String.format(GeneratedTemplate.ClassBody.fromJson, this.getClassName(), this.getClassName()))
                .append(String.format(GeneratedTemplate.ClassBody.toJson, this.getClassName()))
                .append(GeneratedTemplate.ClassBody.classEOF);

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                fileRefer.get().setBinaryContent(completeStringBuilder.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private Object getMappingModelDefaultValueString(MappingModelNode model, Object defVal) {
        String defValString = String.valueOf(defVal);
        switch (model.getDartDataTypeEnum()) {
            case INT:
                if (defValString.contains(".")) {
                    return Long.parseLong(defValString.split("\\.")[0]);
                } else {
                    try {
                        return Long.parseLong(defValString);
                    } catch (Exception e) {
                        return 0;
                    }
                }
            case DOUBLE:
                if (defValString.contains(".")) {
                    return defVal;
                } else {
                    try {
                        return Double.parseDouble(Long.parseLong(defValString) + ".0");
                    } catch (Exception e) {
                        return Double.parseDouble("0.0");
                    }
                }
            case BOOLEAN:
                if ("true".equalsIgnoreCase(defValString) || "1".equalsIgnoreCase(defValString)) {
                    return true;
                } else if ("false".equalsIgnoreCase(defValString) || "0".equalsIgnoreCase(defValString)) {
                    return false;
                }

                return true;
            case DATETIME:
                return "";
            default:
            case STRING:
                return "'" + defVal + "'";
        }
    }

    private String convertDartFieldType(MappingModelNode mappingModelNode, boolean nullable) {
        if (mappingModelNode.getJsonTypeEnum() == JsonTypeEnum.OBJECT) {
            return nullable ? mappingModelNode.getClassName() + "?" : mappingModelNode.getClassName();
        } else if (mappingModelNode.getJsonTypeEnum() == JsonTypeEnum.OBJECT_ARRAY) {
            MappingModelNode firstModel = mappingModelNode.getChildModelNodes().get(0);
            return nullable ? "List<" + firstModel.getClassName() + "?>" : "List<" + firstModel.getClassName() + ">";
        } else if (mappingModelNode.getJsonTypeEnum() == JsonTypeEnum.BASIS_TYPE_ARRAY) {
            MappingModelNode firstModel = mappingModelNode.getChildModelNodes().get(0);
            return nullable ? "List<" + convertDartBasisType(firstModel.getDartDataTypeEnum()) + "?>"
                    : "List<" + convertDartBasisType(firstModel.getDartDataTypeEnum()) + ">";
        } else {
            return nullable ? convertDartBasisType(mappingModelNode.getDartDataTypeEnum()) + "?"
                    : convertDartBasisType(mappingModelNode.getDartDataTypeEnum());
        }
    }

    public void generateSingleDartFileWithAllClasses(VirtualFile parent, Project project) {
        AtomicReference<VirtualFile> fileRefer = createVirtualFileAndReturnFileHandle(parent);

        List<MappingModelNode> allObjectMappingModelNodes = new ArrayList<>();
        allObjectMappingModelNodes.add(this);
        collectAllChildObjectMappingModels(allObjectMappingModelNodes);

        StringBuilder generalHeaderStringBuilder = new StringBuilder();
        generalHeaderStringBuilder.append(GeneratedTemplate.Header.generatedFileHeaders.get(0))
                .append(GeneratedTemplate.Header.generatedFileHeaders.get(1))
                .append(GeneratedTemplate.Header.generatedFileHeaders.get(2))
                .append(GeneratedTemplate.Header.jsonSerializablePack)
                .append("\n")
                .append(String.format(GeneratedTemplate.Header.gFileImport, this.getDartFileName()));

        StringBuilder allDartClassStringBuilder = generateAllDartClassStringBuilder(allObjectMappingModelNodes);

        StringBuilder completeStringBuilder = new StringBuilder(generalHeaderStringBuilder);
        completeStringBuilder.append(allDartClassStringBuilder);

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                fileRefer.get().setBinaryContent(completeStringBuilder.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private StringBuilder generateAllDartClassStringBuilder(List<MappingModelNode> allObjectMappingModelNodes) {
        StringBuilder allDartClassStringBuilder = new StringBuilder();

        for (MappingModelNode mappingModelNode : allObjectMappingModelNodes) {
            // 单个dart文件字段
            StringBuilder fieldsStringBuilder = new StringBuilder();
            // dart类初始化函数参数
            StringBuilder constructorParametersStringBuilder = new StringBuilder();
            if (CollectionUtils.isNotEmpty(mappingModelNode.getChildModelNodes())) {
                for (MappingModelNode childNode : mappingModelNode.getChildModelNodes()) {
                    boolean isRequired = childNode.isDartPropertyRequired();
                    Object defaultValue = childNode.getDartPropertyDefaultValue();
                    boolean nullable = !isRequired && Objects.isNull(defaultValue);

                    // 字段注释处理
                    fieldsStringBuilder.append(String.format(GeneratedTemplate.ClassBody.fieldDescription, childNode.getDescription()));

                    if (childNode.isMarkJsonKeyAnno()) {
                        fieldsStringBuilder.append(String.format(GeneratedTemplate.ClassBody.fieldJsonKey, childNode.getJsonFieldName()));
                    }

                    // 字段处理
                    fieldsStringBuilder.append(
                            String.format(GeneratedTemplate.ClassBody.field,
                                    this.convertDartFieldType(childNode, nullable),
                                    childNode.getDartPropertyName())
                    );

                    // 处理构造函数
                    if (isRequired && Objects.nonNull(defaultValue)) {
                        constructorParametersStringBuilder.append(
                                String.format(GeneratedTemplate.ClassBody.requiredConstructorWithDefaultValPart,
                                        childNode.getDartPropertyName(),
                                        getMappingModelDefaultValueString(childNode, defaultValue))
                        );
                    } else if (isRequired) {
                        constructorParametersStringBuilder.append(
                                String.format(GeneratedTemplate.ClassBody.requiredConstructorPart,
                                        childNode.getDartPropertyName())
                        );
                    } else if (Objects.nonNull(defaultValue)) {
                        constructorParametersStringBuilder.append(
                                String.format(GeneratedTemplate.ClassBody.constructorOnlyDefaultValPart,
                                        childNode.getDartPropertyName(),
                                        getMappingModelDefaultValueString(childNode, defaultValue))
                        );
                    } else {
                        constructorParametersStringBuilder.append(
                                String.format(GeneratedTemplate.ClassBody.constructorNullablePart,
                                        childNode.getDartPropertyName())
                        );
                    }
                }
            }

            // 添加其他json_serializable生成的数据
            StringBuilder singleDartClassStringBuilder = new StringBuilder();
            singleDartClassStringBuilder.append(String.format(GeneratedTemplate.Header.description, mappingModelNode.getDescription()))
                    .append(GeneratedTemplate.Header.jsonSerializablePackAnno)
                    .append(String.format(GeneratedTemplate.ClassBody.className, mappingModelNode.getClassName()));

            if (CollectionUtils.isNotEmpty(mappingModelNode.getChildModelNodes())) {
                singleDartClassStringBuilder.append(fieldsStringBuilder);
                singleDartClassStringBuilder.append("\n");
            }

            // 添加dart方法
            singleDartClassStringBuilder.append(
                            String.format(GeneratedTemplate.ClassBody.constructor,
                                    mappingModelNode.getClassName(),
                                    CollectionUtils.isNotEmpty(mappingModelNode.getChildModelNodes()) ? "{" + constructorParametersStringBuilder + "}" : ""))
                    .append(String.format(GeneratedTemplate.ClassBody.fromJson, mappingModelNode.getClassName(), mappingModelNode.getClassName()))
                    .append(String.format(GeneratedTemplate.ClassBody.toJson, mappingModelNode.getClassName()))
                    .append(GeneratedTemplate.ClassBody.classEOF);

            allDartClassStringBuilder.append("\n\n")
                    .append(singleDartClassStringBuilder);
        }

        return allDartClassStringBuilder;
    }

    private void collectAllChildObjectMappingModels(List<MappingModelNode> mappingModelNodeCollection) {
        if (CollectionUtils.isEmpty(getChildModelNodes())) {
            return;
        }

        for (MappingModelNode childNode : getChildModelNodes()) {
            if (!childNode.isBasisJsonType()) {
                // 非基础数据类型的节点将会被单独生成一个dart文件中
                mappingModelNodeCollection.add(childNode);

                // 继续处理子节点
                childNode.collectAllChildObjectMappingModels(mappingModelNodeCollection);
            }
        }
    }

    // 生成新的文件并返回虚拟文件节点
    private AtomicReference<VirtualFile> createVirtualFileAndReturnFileHandle(VirtualFile parent) {
        AtomicReference<VirtualFile> fileRefer = new AtomicReference<>();

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile childFile = parent.findChild(this.getDartFileName() + ".dart");
                if (Objects.nonNull(childFile)) {
                    this.setDartFileName(this.getDartFileName() + "_" + RandomStringUtils.randomAlphabetic(4));
                }

                VirtualFile fileHandle = parent.createChildData(null, this.getDartFileName() + ".dart");
                fileRefer.set(fileHandle);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return fileRefer;
    }

    public static String convertDartBasisType(DartDataTypeEnum dartDataTypeEnum) {
        switch (dartDataTypeEnum) {
            case INT:
                return "int";
            case DOUBLE:
                return "double";
            case STRING:
                return "String";
            case BOOLEAN:
                return "bool";
            case DATETIME:
                return "DateTime";
            case OBJECT:
            default:
                return "none";
        }
    }

    public void innerLocalPropertyRebuild() {
        if (isRoot) {
            return;
        }

        if (!this.jsonFieldName.equals(this.dartPropertyName)) {
            this.markJsonKeyAnno = true;
        }
    }


    public String getJsonFieldName() {
        return jsonFieldName;
    }

    public String getDartPropertyName() {
        return dartPropertyName;
    }

    public boolean isDartPropertyRequired() {
        return dartPropertyRequired;
    }

    public Object getDartPropertyDefaultValue() {
        return dartPropertyDefaultValue;
    }

    public String getDartFileName() {
        return dartFileName;
    }

    public String getClassName() {
        return className;
    }

    public JsonNode getNode() {
        return node;
    }

    public JsonTypeEnum getJsonTypeEnum() {
        return jsonTypeEnum;
    }

    public DartDataTypeEnum getDartDataTypeEnum() {
        return dartDataTypeEnum;
    }

    public boolean isRoot() {
        return isRoot;
    }

    public List<MappingModelNode> getChildModelNodes() {
        return childModelNodes;
    }

    public void setDartPropertyName(String dartPropertyName) {
        this.dartPropertyName = dartPropertyName;
    }

    public void setDartDataTypeEnum(DartDataTypeEnum dartDataTypeEnum) {
        this.dartDataTypeEnum = dartDataTypeEnum;
    }

    public void setDartPropertyRequired(boolean dartPropertyRequired) {
        this.dartPropertyRequired = dartPropertyRequired;
    }

    public void setDartPropertyDefaultValue(Object dartPropertyDefaultValue) {
        this.dartPropertyDefaultValue = dartPropertyDefaultValue;
    }

    public void setDartFileName(String dartFileName) {
        this.dartFileName = dartFileName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isMarkJsonKeyAnno() {
        return markJsonKeyAnno;
    }

    public void setJsonFieldName(String jsonFieldName) {
        this.jsonFieldName = jsonFieldName;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public void setJsonTypeEnum(JsonTypeEnum jsonTypeEnum) {
        this.jsonTypeEnum = jsonTypeEnum;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
