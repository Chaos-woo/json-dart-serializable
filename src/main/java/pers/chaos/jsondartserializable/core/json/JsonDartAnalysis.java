package pers.chaos.jsondartserializable.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.rits.cloning.Cloner;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import pers.chaos.jsondartserializable.core.enums.DartDataTypeEnum;
import pers.chaos.jsondartserializable.core.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.utils.DartClassFileUtils;
import pers.chaos.jsondartserializable.windows.UserAdvanceConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class JsonDartAnalysis {
    private String dartFileName;
    private String className;

    private MappingModelNode rootModelNode;
    private MappingModelNode preRootModelNode;

    private UserAdvanceConfiguration userAdvanceConfiguration;

    public JsonDartAnalysis(String className, JsonNode node, UserAdvanceConfiguration userAdvanceConfiguration) {
        setJsonAnalysisConfig(className, userAdvanceConfiguration);
        this.rootModelNode = initNewRoomModelNode(className, node, userAdvanceConfiguration);
    }

    // 初始化根模型结点
    private MappingModelNode initNewRoomModelNode(String className, JsonNode node, UserAdvanceConfiguration userAdvanceConfiguration) {
        MappingModelNode rootModelNode = new MappingModelNode(className, node, true);
        // 分析根模型的JSON数据及根模型下的子节点
        analysisRootMappingModelNode(rootModelNode, node);

        if (userAdvanceConfiguration.isEnableRealtimeJsonDefaultValueAnalysis()) {
            // 根据用户配置，使用导入的Json字段值设置模型数据默认值
            processRealtimeJsonDefaultValue(rootModelNode);
        }

        return rootModelNode;
    }

    // 设置JSON分析配置
    private void setJsonAnalysisConfig(String className, UserAdvanceConfiguration userAdvanceConfiguration) {
        this.className = DartClassFileUtils.getDartClassName(className);
        this.dartFileName = DartClassFileUtils.getDartFileNameByClassName(className);
        this.userAdvanceConfiguration = userAdvanceConfiguration;
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
        innerMappingModelRebuildDescription(rootModelNode.getChildModelNodes());

        // 重构对象数组dart文件名和类名
        objectArrayInnerFirstMappingModelRebuildClassNameAndDartFileName(rootModelNode);

        // 检验是否存在重名的dart文件
        final Set<String> existDartFileNames = new HashSet<>();
        checkAllMappingModelExistSameNameDartFileName(existDartFileNames, rootModelNode);

        if (userAdvanceConfiguration.isEnableAllClassGeneratedIntoSingleFile()) {
            // 将多个对象类生成在同一个dart文件中
            rootModelNode.generateSingleDartFileWithAllClasses(parent, project);
        } else {
            // 将多个对象类生成在单独的dart文件中
            rootModelNode.generateMultiDartFilesRecursively(parent, project);
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
        return rootModelNode;
    }


    @Override
    public String toString() {
        try {
            return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(this);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void rebuildRootMappingModel(JsonAnalyser.AnalysisRebuildData simpleAnalysisData) {
        // 设置新的分析配置
        setJsonAnalysisConfig(simpleAnalysisData.getClassName(), simpleAnalysisData.getUserAdvanceConfiguration());
        // 保存历史根节点数据
        Cloner cloner = new Cloner();
        this.preRootModelNode = cloner.deepClone(this.rootModelNode);
        // 初始化新的根节点
        this.rootModelNode = initNewRoomModelNode(simpleAnalysisData.getClassName(), simpleAnalysisData.getJsonNode(), simpleAnalysisData.getUserAdvanceConfiguration());

        // 拷贝已存在节点用户已编辑的数据
        copyExistMappingModelNodeData(this.rootModelNode, "");

        this.preRootModelNode = null;
    }

    private void copyExistMappingModelNodeData(MappingModelNode node, String parentJsonFieldName) {
        // 遍历新的根节点下的所有子节点
        for (MappingModelNode childNode : node.getChildModelNodes()) {
            // 获取新的子节点的JSON路径,用于在旧的根节点下查找对应的子节点
            String jsonFieldName = childNode.getJsonFieldName();

            if (!childNode.isBasisJsonType()) {
                String jsonPath = StringUtils.isBlank(parentJsonFieldName) ? jsonFieldName : parentJsonFieldName + "." + jsonFieldName;
                copyExistMappingModelNodeData(childNode, jsonPath);
            }

            // 根据JSON路径在旧的根节点下查找对应的子节点
            String jsonPath = StringUtils.isBlank(parentJsonFieldName) ? jsonFieldName : parentJsonFieldName + "." + jsonFieldName;
            MappingModelNode oldChildModelNode = this.preRootModelNode.findChildByPath(jsonPath);
            if (Objects.nonNull(oldChildModelNode)) {
                // 找到对应的旧子节点,则将旧子节点中用户已编辑的数据复制到新子节点中
                childNode.setDescription(oldChildModelNode.getDescription());

                if (oldChildModelNode.isBasisJsonType()) {
                    childNode.setDartDataTypeEnum(oldChildModelNode.getDartDataTypeEnum());
                    childNode.setDartPropertyDefaultValue(oldChildModelNode.getDartPropertyDefaultValue());
                }

                childNode.setDartPropertyRequired(oldChildModelNode.isDartPropertyRequired());
                childNode.setDartPropertyName(oldChildModelNode.getDartPropertyName());

                if (oldChildModelNode.getJsonTypeEnum() == JsonTypeEnum.OBJECT
                        || oldChildModelNode.getJsonTypeEnum() == JsonTypeEnum.OBJECT_ARRAY) {
                    childNode.setDartFileName(oldChildModelNode.getDartFileName());
                    childNode.setClassName(oldChildModelNode.getClassName());
                }
            }
        }
    }
}
