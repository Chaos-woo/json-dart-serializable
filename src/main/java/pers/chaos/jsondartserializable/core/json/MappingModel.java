package pers.chaos.jsondartserializable.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.collections.CollectionUtils;
import pers.chaos.jsondartserializable.core.json.constants.GeneratedTemplate;
import pers.chaos.jsondartserializable.core.json.enums.DartDataTypeEnum;
import pers.chaos.jsondartserializable.core.json.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.utils.DartClassFileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class MappingModel {
    // original JSON field name
    private String jsonFieldName;
    // generated dart property name
    private String dartPropertyName;
    // dart basis data type, e.g. String or int
    private DartDataTypeEnum dartDataTypeEnum = DartDataTypeEnum.OBJECT;
    // current model mapping property is required
    private boolean dartPropertyRequired = true;
    // current model mapping property's default value,
    // but only support set dart basis data type
    private Object dartPropertyDefaultValue;
    // node description, generated will be set default value of json field name
    private String description;


    // will be set when current model is OBJECT or OBJECT in JSON array
    private String dartFileName;
    // will be set when current model is OBJECT or OBJECT in JSON array
    private String className;


    private final JsonNode node;
    private JsonTypeEnum jsonTypeEnum;
    private final boolean isRoot;
    // mark whether generated need using @JsonKey annotation convert original json filed name
    private boolean markJsonKeyAnno = false;
    // inner mapping models in JSON tree
    private final List<MappingModel> innerMappingModels;

    public MappingModel(String jsonFieldName, JsonNode node, boolean isRoot) {
        this.jsonFieldName = jsonFieldName;
        this.node = node;
        this.innerMappingModels = new ArrayList<>();
        this.isRoot = isRoot;

        /**
         * mapping custom json type enum
         * @see JsonTypeEnum
         */
        this.mappingCustomJsonTypeEnum();
        /**
         * mapping custom dart basis data type enum
         * @see DartDataTypeEnum
         */
        this.mappingCustomDartTypeEnum();
        // process dart file name, class name and class file name
        this.mappingCustomDartClassName();
        // process dart property name
        this.mappingCustomDartPropertyName();
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
                    MappingModel mm = new MappingModel(fieldName, childNode, false);
                    this.innerMappingModels.add(mm);
                });
            }

        } else if (node.isArray()) {
            if (!node.isEmpty()) {
                JsonNode firstChildNode = node.get(0);
                if (firstChildNode.isObject()) {
                    jsonTypeEnum = JsonTypeEnum.OBJECT_ARRAY;
                    // OBJECT ARRAY's json field name will be used to generated
                    // its inner object's class name, but the json field name will
                    // be used to set this OBJECT ARRAY's dart property name
                    this.innerMappingModels.add(new MappingModel(this.jsonFieldName, firstChildNode, false));
                } else if (firstChildNode.isArray()) {
                    throw new RuntimeException("Not support analysis array nesting");
                } else {
                    jsonTypeEnum = JsonTypeEnum.BASIS_TYPE_ARRAY;
                    this.innerMappingModels.add(new MappingModel("NORMAL_FIELD_ARRAY", firstChildNode, false));
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
                this.dartDataTypeEnum = this.getCustomDartDataType(node);
            } else if (jsonTypeEnum == JsonTypeEnum.BASIS_TYPE_ARRAY) {
                if (!node.isEmpty()) {
                    JsonNode firstChildNode = node.get(0);
                    if (firstChildNode.isObject()) {
                        this.dartDataTypeEnum = DartDataTypeEnum.OBJECT;
                    } else if (firstChildNode.isArray()) {
                        throw new RuntimeException("Not support analysis array nesting");
                    } else {
                        this.dartDataTypeEnum = this.getCustomDartDataType(firstChildNode);
                    }
                } else {
                    // original JSON string array is empty,
                    // can not judge dart type, so, default set
                    // it is String array
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
            dartDataTypeEnum = DartDataTypeEnum.STRING;
        } else if (node.isInt() || node.isLong() || node.isBigInteger()) {
            dartDataTypeEnum = DartDataTypeEnum.INT;
        } else if (node.isDouble() || node.isFloat()) {
            dartDataTypeEnum = DartDataTypeEnum.DOUBLE;
        }
        return dartDataTypeEnum;
    }

    public void cycleGeneratedDartFile(final VirtualFile parent, final Project project) {
        List<MappingModel> importModels = new ArrayList<>();

        if (jsonTypeEnum == JsonTypeEnum.OBJECT) {
            if (CollectionUtils.isNotEmpty(innerMappingModels)) {
                // OBJECT type mapping model will preferentially
                // generate it's inner mapping models
                // eg. OBJECT hold another OBJECT
                // {"anotherObj":{"name":"Leo"}}
                for (MappingModel innerMappingModel : innerMappingModels) {
                    if (!innerMappingModel.isBasisJsonType()) {
                        // not basis type mapping model will need 'import xx',
                        // so it will be added for current mapping model using
                        // in subsequent builds
                        importModels.add(innerMappingModel);

                        // inner mapping model generate
                        innerMappingModel.cycleGeneratedDartFile(parent, project);
                    }
                }
            }
        } else if (jsonTypeEnum == JsonTypeEnum.OBJECT_ARRAY) {
            // if current mapping model is an OBJECT ARRAY,
            // not only it's parent object need import OBJECT ARRAY's
            // inner OBJECT, but also the inner OBJECT also
            // need keeping on preferentially generate dart file
            MappingModel mappingModel = innerMappingModels.get(0);
            importModels.add(mappingModel);
            mappingModel.cycleGeneratedDartFile(parent, project);
            return;
        } else {
            // normal basis dart data type field will be generated by its parent object
            return;
        }

        // final generated dart file of current mapping model
        // this method only call by OBJECT mapping model
        generatedDartFileOfCurrentModel(innerMappingModels, importModels, parent, project);
    }

    // reference: https://pub.dev/packages/json_serializable
    // format refer [Example]
    private void generatedDartFileOfCurrentModel(List<MappingModel> currentNodeInnerMappingModels,
                                                 List<MappingModel> importModels,
                                                 VirtualFile parent, Project project) {

        AtomicReference<VirtualFile> fileRefer = new AtomicReference<>();

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                VirtualFile fileHandle = parent.createChildData(null, this.getDartFileName() + ".dart");
                fileRefer.set(fileHandle);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        StringBuilder importFile = new StringBuilder();
        if (CollectionUtils.isNotEmpty(importModels)) {
            for (MappingModel importModel : importModels) {
                importFile.append(String.format(GeneratedTemplate.Header.otherFileImport, importModel.getDartFileName() + ".dart"));
            }
            importFile.append("\n");
        }

        StringBuilder fields = new StringBuilder();
        StringBuilder constructorParameters = new StringBuilder();
        if (CollectionUtils.isNotEmpty(currentNodeInnerMappingModels)) {
            for (MappingModel innerMappingModel : currentNodeInnerMappingModels) {
                boolean isRequired = innerMappingModel.isDartPropertyRequired();
                Object defaultValue = innerMappingModel.getDartPropertyDefaultValue();
                boolean nullable = !isRequired && Objects.isNull(defaultValue);

                // field description
                fields.append(String.format(GeneratedTemplate.ClassBody.fieldDescription, innerMappingModel.getDescription()));

                if (innerMappingModel.isMarkJsonKeyAnno()) {
                    fields.append(String.format(GeneratedTemplate.ClassBody.fieldJsonKey, innerMappingModel.getJsonFieldName()));
                }

                // really field
                fields.append(
                        String.format(GeneratedTemplate.ClassBody.field,
                                this.convertDartFieldType(innerMappingModel, nullable),
                                innerMappingModel.getDartPropertyName())
                );

                // process constructor parameters
                if (isRequired && Objects.nonNull(defaultValue)) {
                    constructorParameters.append(
                            String.format(GeneratedTemplate.ClassBody.requiredConstructorWithDefaultValPart,
                                    innerMappingModel.getDartPropertyName(),
                                    getMappingModelDefaultValueString(innerMappingModel, defaultValue))
                    );
                } else if (isRequired) {
                    constructorParameters.append(
                            String.format(GeneratedTemplate.ClassBody.requiredConstructorPart,
                                    innerMappingModel.getDartPropertyName())
                    );
                } else if (Objects.nonNull(defaultValue)) {
                    constructorParameters.append(
                            String.format(GeneratedTemplate.ClassBody.constructorOnlyDefaultValPart,
                                    innerMappingModel.getDartPropertyName(),
                                    getMappingModelDefaultValueString(innerMappingModel, defaultValue))
                    );
                } else {
                    constructorParameters.append(
                            String.format(GeneratedTemplate.ClassBody.constructorNullablePart,
                                    innerMappingModel.getDartPropertyName())
                    );
                }
            }
        }

        StringBuilder completeFile = new StringBuilder();
        completeFile.append(GeneratedTemplate.Header.generatedFileHeaders.get(0))
                .append(GeneratedTemplate.Header.generatedFileHeaders.get(1))
                .append(GeneratedTemplate.Header.generatedFileHeaders.get(2))
                .append(GeneratedTemplate.Header.jsonSerializablePack);
        if (CollectionUtils.isNotEmpty(importModels)) {
            completeFile.append(importFile);
        } else {
            completeFile.append("\n");
        }

        completeFile.append(String.format(GeneratedTemplate.Header.gFileImport, this.getDartFileName()))
                .append(String.format(GeneratedTemplate.Header.description, this.getDescription()))
                .append(GeneratedTemplate.Header.jsonSerializablePackAnno)
                .append(String.format(GeneratedTemplate.ClassBody.className, this.getClassName()));

        if (CollectionUtils.isNotEmpty(currentNodeInnerMappingModels)) {
            completeFile.append(fields);
            completeFile.append("\n");
        }

        completeFile.append(
                        String.format(GeneratedTemplate.ClassBody.constructor,
                                this.getClassName(),
                                CollectionUtils.isNotEmpty(currentNodeInnerMappingModels) ? "{" + constructorParameters.toString() + "}" : ""))
                .append(String.format(GeneratedTemplate.ClassBody.fromJson, this.getClassName(), this.getClassName()))
                .append(String.format(GeneratedTemplate.ClassBody.toJson, this.getClassName()))
                .append(GeneratedTemplate.ClassBody.classEOF);

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                fileRefer.get().setBinaryContent(completeFile.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String getMappingModelDefaultValueString(MappingModel model, Object defVal) {
        String defValString = (String) defVal;
        switch (model.getDartDataTypeEnum()) {
            case INT:
                if (defValString.contains(".")) {
                    return defValString.split("\\.")[0];
                } else {
                    try {
                        return String.valueOf(Long.parseLong(defValString));
                    } catch (Exception e) {
                        return "0";
                    }
                }
            case DOUBLE:
                if (defValString.contains(".")) {
                    return defValString;
                } else {
                    try {
                        return Long.parseLong(defValString) + ".0";
                    } catch (Exception e) {
                        return "0.0";
                    }
                }
            case BOOLEAN:
                if ("true".equalsIgnoreCase(defValString) || "1".equalsIgnoreCase(defValString)) {
                    return "true";
                } else if ("false".equalsIgnoreCase(defValString) || "0".equalsIgnoreCase(defValString)) {
                    return "false";
                }

                return "true";
            default:
            case STRING:
                return "'" + defVal + "'";
        }
    }

    private String convertDartFieldType(MappingModel mappingModel, boolean nullable) {
        if (mappingModel.getJsonTypeEnum() == JsonTypeEnum.OBJECT) {
            return nullable ? mappingModel.getClassName() + "?" : mappingModel.getClassName();
        } else if (mappingModel.getJsonTypeEnum() == JsonTypeEnum.OBJECT_ARRAY) {
            MappingModel firstModel = mappingModel.getInnerMappingModels().get(0);
            return nullable ? "List<" + firstModel.getClassName() + "?>" : "List<" + firstModel.getClassName() + ">";
        } else if (mappingModel.getJsonTypeEnum() == JsonTypeEnum.BASIS_TYPE_ARRAY) {
            MappingModel firstModel = mappingModel.getInnerMappingModels().get(0);
            return nullable ? "List<" + convertDartBasisType(firstModel.getDartDataTypeEnum()) + "?>"
                    : "List<" + convertDartBasisType(firstModel.getDartDataTypeEnum()) + ">";
        } else {
            return nullable ? convertDartBasisType(mappingModel.getDartDataTypeEnum()) + "?"
                    : convertDartBasisType(mappingModel.getDartDataTypeEnum());
        }
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

    public List<MappingModel> getInnerMappingModels() {
        return innerMappingModels;
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


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
