package pers.chaos.jsondartserializable.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import pers.chaos.jsondartserializable.core.json.enums.DartDataTypeEnum;
import pers.chaos.jsondartserializable.core.json.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.utils.DartClassFileUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class MappingModel {
    private final String jsonFieldName;
    private JsonTypeEnum jsonTypeEnum;
    private DartDataTypeEnum dartDataTypeEnum;
    private String dartFileName;
    private String className;
    private JsonNode node;
    private List<MappingModel> innerMappingModels;

    private boolean isRoot;

    public MappingModel(String jsonFieldName, JsonNode node, boolean isRoot) {
        this.jsonFieldName = jsonFieldName;
        this.node = node;
        this.innerMappingModels = new ArrayList<>();
        this.isRoot = isRoot;

        mappingCustomJsonTypeEnum();
        mappingCustomDartTypeEnum();
        mappingCustomDartClassName();
    }

    public boolean isBasisJsonType() {
        return jsonTypeEnum == JsonTypeEnum.NORMAL_FIELD || jsonTypeEnum == JsonTypeEnum.NORMAL_FIELD_ARRAY;
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
                    this.innerMappingModels.add(new MappingModel(this.jsonFieldName, firstChildNode, false));
                } else if (firstChildNode.isArray()) {
                    throw new RuntimeException("Not support analysis array nesting");
                } else {
                    jsonTypeEnum = JsonTypeEnum.NORMAL_FIELD_ARRAY;
                    this.innerMappingModels.add(new MappingModel("NORMAL_FIELD_ARRAY", firstChildNode, false));
                }
            }

            // do nothing
        } else {
            jsonTypeEnum = JsonTypeEnum.NORMAL_FIELD;
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
            if (jsonTypeEnum == JsonTypeEnum.NORMAL_FIELD) {
                dartDataTypeEnum = getCustomDartDataType(node);
            } else if (jsonTypeEnum == JsonTypeEnum.NORMAL_FIELD_ARRAY) {
                if (!node.isEmpty()) {
                    JsonNode firstChildNode = node.get(0);
                    if (firstChildNode.isObject()) {
                        dartDataTypeEnum = null;
                    } else if (firstChildNode.isArray()) {
                        throw new RuntimeException("Not support analysis array nesting");
                    } else {
                        dartDataTypeEnum = getCustomDartDataType(firstChildNode);
                    }
                }
            }
        }
    }

    private DartDataTypeEnum getCustomDartDataType(JsonNode node) {
        DartDataTypeEnum dartDataTypeEnum = null;
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
            // normal basis dart data type field will be generated by it's parent object
            return;
        }

        // final generated dart file of current mapping model
        generatedDartFileOfCurrentModel(innerMappingModels, importModels, parent, project);
    }

    interface CommonPath {
        List<String> generatedFileHeaders = Arrays.asList(
                "/// Generated by json-dart-serializable plugin.\n",
                "/// When some incorrect data generated, please fix it.\n",
                "/// Finally, run flutter build_runner auto generate *.g.dart file.\n"
        );
        String gFileDir = "gen-serializable";
        String jsonSerializablePackAnno = "@JsonSerializable()\n";
        String jsonSerializablePack = "import 'package:json_annotation/json_annotation.dart';\n";
        String otherFileImport = "import '%s';\n";
        String gFileImport = "part '%s.g.dart';\n\n";
    }

    interface GeneratedToken {
        String className = "class %s {\n";
        String field = "    final %s %s;\n";
        String constructor = "    %s(%s);\n\n";
        String constructorPart = "required this.%s, ";
        String fromJson = "    factory %s.fromJson(Map<String, dynamic> json) => _$%sFromJson(json);\n\n";
        String toJson = "    Map<String, dynamic> toJson() => _$%sToJson(this);\n";
        String classEOF = "}";
    }


    // reference: https://pub.dev/packages/json_serializable
    // format refer [Example]
    private void generatedDartFileOfCurrentModel(List<MappingModel> currentNodeInnerMappingModels,
                                                 List<MappingModel> importModels,
                                                 VirtualFile parent, Project project) {

        String projectName = project.getName();
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
                importFile.append(String.format(CommonPath.otherFileImport, importModel.getDartFileName() + ".dart"));
            }
            importFile.append("\n");
        }

        StringBuilder fields = new StringBuilder();
        StringBuilder constructorArguments = new StringBuilder();
        if (CollectionUtils.isNotEmpty(currentNodeInnerMappingModels)) {
            for (MappingModel innerMappingModel : currentNodeInnerMappingModels) {
                fields.append(
                        String.format(GeneratedToken.field, convertDartFieldType(innerMappingModel), innerMappingModel.getJsonFieldName())
                );
                constructorArguments.append(String.format(GeneratedToken.constructorPart, innerMappingModel.getJsonFieldName()));
            }
        }

        StringBuilder completeFile = new StringBuilder();
        completeFile.append(CommonPath.generatedFileHeaders.get(0))
                .append(CommonPath.generatedFileHeaders.get(1))
                .append(CommonPath.generatedFileHeaders.get(2))
                .append(CommonPath.jsonSerializablePack);
        if (CollectionUtils.isNotEmpty(importModels)) {
            completeFile.append(importFile);
        } else {
            completeFile.append("\n");
        }

        completeFile.append(String.format(CommonPath.gFileImport, this.getDartFileName()))
                .append(CommonPath.jsonSerializablePackAnno)
                .append(String.format(GeneratedToken.className, this.getClassName()));

        if (CollectionUtils.isNotEmpty(currentNodeInnerMappingModels)) {
            completeFile.append(fields);
            completeFile.append("\n");
        }

        completeFile.append(
                        String.format(GeneratedToken.constructor,
                                this.getClassName(),
                                CollectionUtils.isNotEmpty(currentNodeInnerMappingModels) ? "{" + constructorArguments.toString() + "}" : ""))
                .append(String.format(GeneratedToken.fromJson, this.getClassName(), this.getClassName()))
                .append(String.format(GeneratedToken.toJson, this.getClassName()))
                .append(GeneratedToken.classEOF);

        ApplicationManager.getApplication().runWriteAction(() -> {
            try {
                fileRefer.get().setBinaryContent(completeFile.toString().getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private String convertDartFieldType(MappingModel mappingModel) {
        if (mappingModel.getJsonTypeEnum() == JsonTypeEnum.OBJECT) {
            return mappingModel.getClassName();
        } else if (mappingModel.getJsonTypeEnum() == JsonTypeEnum.OBJECT_ARRAY) {
            MappingModel firstModel = mappingModel.getInnerMappingModels().get(0);
            return "List<" + firstModel.getClassName() + ">";
        } else if (mappingModel.getJsonTypeEnum() == JsonTypeEnum.NORMAL_FIELD_ARRAY) {
            MappingModel firstModel = mappingModel.getInnerMappingModels().get(0);
            return "List<" + convertDartBasisType(firstModel.getDartDataType()) + ">";
        } else {
            return convertDartBasisType(mappingModel.getDartDataType());
        }
    }

    private String convertDartBasisType(DartDataTypeEnum dartDataTypeEnum) {
        switch (dartDataTypeEnum) {
            case INT:
                return "int";
            case DOUBLE:
                return "double";
            case STRING:
                return "String";
            case BOOLEAN:
                return "bool";
            default:
                return "";
        }
    }

    public String getJsonFieldName() {
        return jsonFieldName;
    }

    public JsonTypeEnum getJsonTypeEnum() {
        return jsonTypeEnum;
    }

    public List<MappingModel> getInnerMappingModels() {
        return innerMappingModels;
    }

    public DartDataTypeEnum getDartDataType() {
        return dartDataTypeEnum;
    }

    public String getDartFileName() {
        return dartFileName;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
