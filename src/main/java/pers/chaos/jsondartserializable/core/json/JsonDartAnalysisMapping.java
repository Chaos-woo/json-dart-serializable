package pers.chaos.jsondartserializable.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import pers.chaos.jsondartserializable.core.enums.DartDataTypeEnum;
import pers.chaos.jsondartserializable.core.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.utils.DartClassFileUtils;
import pers.chaos.jsondartserializable.windows.UserAdvanceConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonDartAnalysisMapping {
    private final String dartFileName;
    private final String className;

    private final MappingModelNode rootMappingModelNode;

    private final UserAdvanceConfiguration userAdvanceConfiguration;

    public JsonDartAnalysisMapping(String className, JsonNode node, UserAdvanceConfiguration userAdvanceConfiguration) {
        this.className = DartClassFileUtils.getDartClassName(className);
        this.dartFileName = DartClassFileUtils.getDartFileNameByClassName(className);
        this.userAdvanceConfiguration = userAdvanceConfiguration;

        // 初始化根模型结点
        rootMappingModelNode = new MappingModelNode(className, node, true);
        // 分析根模型的JSON数据及根模型下的子节点
        analysisRootMappingModelNode(rootMappingModelNode, node);

        if (userAdvanceConfiguration.isEnableRealtimeJsonDefaultValueAnalysis()) {
            // 根据用户配置，使用导入的Json字段值设置模型数据默认值
            processRealtimeJsonDefaultValue(rootMappingModelNode);
        }
    }

    private void processRealtimeJsonDefaultValue(MappingModelNode mappingModelNode) {
        // 根据JSON节点值设置dart中基本类型的默认值
        if (DartDataTypeEnum.OBJECT != mappingModelNode.getDartDataTypeEnum()) {
            JsonNode node = mappingModelNode.getNode();
            Object defaultValue = null;
            if (node.isBoolean()) {
                defaultValue = node.asBoolean();
            } else if (node.isTextual()) {
                defaultValue = node.asText();
            } else if (node.isInt()) {
                defaultValue = node.asInt();
            } else if (node.isLong()) {
                defaultValue = node.asLong();
            } else if (node.isBigInteger()) {
                defaultValue = node.bigIntegerValue();
            } else if (node.isDouble() || node.isFloat()) {
                defaultValue = node.asDouble();
            } else if (node.isFloat()) {
                defaultValue = node.floatValue();
            }
            mappingModelNode.setDartPropertyDefaultValue(defaultValue);
        }

        if (CollectionUtils.isNotEmpty(mappingModelNode.getChildModelNodes())) {
            for (MappingModelNode innerMappingModelNode : mappingModelNode.getChildModelNodes()) {
                processRealtimeJsonDefaultValue(innerMappingModelNode);
            }
        }
    }

    private void analysisRootMappingModelNode(MappingModelNode rootMappingModelNode, JsonNode node) {
        node.fieldNames().forEachRemaining(fieldName -> {
            JsonNode childNode = node.get(fieldName);
            MappingModelNode childMappingModelNode = new MappingModelNode(fieldName, childNode, false);

            rootMappingModelNode.getChildModelNodes().add(childMappingModelNode);
        });
    }

    public void generated(VirtualFile parent, Project project) {

        // 设置默认描述
        innerMappingModelRebuildDescription(rootMappingModelNode.getChildModelNodes());

        // 重构对象数组dart文件名和类名
        objectArrayInnerFirstMappingModelRebuildClassNameAndDartFileName(rootMappingModelNode);

        // 检验是否存在重名的dart文件
        final Set<String> existDartFileNames = new HashSet<>();
        checkAllMappingModelExistSameNameDartFileName(existDartFileNames, rootMappingModelNode);

        if (userAdvanceConfiguration.isEnableAllClassGeneratedIntoSingleFile()) {
            // 将多个对象类生成在同一个dart文件中
            rootMappingModelNode.generateSingleDartFileWithAllClasses(parent, project);
        } else {
            // 将多个对象类生成在单独的dart文件中
            rootMappingModelNode.generateMultiDartFilesRecursively(parent, project);
        }
    }

    private void objectArrayInnerFirstMappingModelRebuildClassNameAndDartFileName(MappingModelNode mappingModelNode) {
        if (mappingModelNode.isBasisJsonType()) {
            return;
        }

        if (JsonTypeEnum.OBJECT == mappingModelNode.getJsonTypeEnum()
            && CollectionUtils.isNotEmpty(mappingModelNode.getChildModelNodes())) {

            for (MappingModelNode innerMappingModelNode : mappingModelNode.getChildModelNodes()) {
                objectArrayInnerFirstMappingModelRebuildClassNameAndDartFileName(innerMappingModelNode);
            }
        }

        if (JsonTypeEnum.OBJECT_ARRAY == mappingModelNode.getJsonTypeEnum()
                && CollectionUtils.isNotEmpty(mappingModelNode.getChildModelNodes())) {

            MappingModelNode firstChild = mappingModelNode.getChildModelNodes().get(0);
            firstChild.setClassName(mappingModelNode.getClassName());
            firstChild.setDartFileName(mappingModelNode.getDartFileName());

            if (CollectionUtils.isNotEmpty(firstChild.getChildModelNodes())) {
                for (MappingModelNode innerMappingModelNode : firstChild.getChildModelNodes()) {
                    objectArrayInnerFirstMappingModelRebuildClassNameAndDartFileName(innerMappingModelNode);
                }
            }
        }
    }

    private void checkAllMappingModelExistSameNameDartFileName(final Set<String> existDartFileNames, MappingModelNode mappingModelNode) {
        if (mappingModelNode.isBasisJsonType()) {
            return;
        }

        if (JsonTypeEnum.OBJECT == mappingModelNode.getJsonTypeEnum()) {
            String dartFileName = mappingModelNode.getDartFileName();
            if (existDartFileNames.contains(dartFileName)) {
                String antiDuplicateDartFileNameEOF = RandomStringUtils.randomAlphabetic(4);
                mappingModelNode.setDartFileName(dartFileName + "_" + antiDuplicateDartFileNameEOF);
            }

            existDartFileNames.add(mappingModelNode.getDartFileName());
        }

        if (CollectionUtils.isNotEmpty(mappingModelNode.getChildModelNodes())) {
            for (MappingModelNode innerMappingModelNode : mappingModelNode.getChildModelNodes()) {
                checkAllMappingModelExistSameNameDartFileName(existDartFileNames, innerMappingModelNode);
            }
        }
    }

    private void innerMappingModelRebuildDescription(List<MappingModelNode> innerMappingModelNodes) {
        for (MappingModelNode innerMappingModelNode : innerMappingModelNodes) {
            if (StringUtils.isBlank(innerMappingModelNode.getDescription())) {
                innerMappingModelNode.setDescription(innerMappingModelNode.getJsonFieldName());
            }

            if (CollectionUtils.isNotEmpty(innerMappingModelNode.getChildModelNodes())) {
                innerMappingModelRebuildDescription(innerMappingModelNode.getChildModelNodes());
            }
        }
    }

    public MappingModelNode getRootMappingModel() {
        return rootMappingModelNode;
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
