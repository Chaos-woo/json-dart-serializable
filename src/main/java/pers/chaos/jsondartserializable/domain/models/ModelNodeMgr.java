package pers.chaos.jsondartserializable.domain.models;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.rits.cloning.Cloner;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import pers.chaos.jsondartserializable.domain.enums.DartConst;
import pers.chaos.jsondartserializable.domain.enums.DartDataType;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeDataType;
import pers.chaos.jsondartserializable.domain.repository.FileRepo;
import pers.chaos.jsondartserializable.domain.service.JsonNodeAnalyser;
import pers.chaos.jsondartserializable.domain.ui.models.InputDataVO;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型节点管理
 */
@Getter
public class ModelNodeMgr {
    @Setter
    private String jsonString;
    @Setter
    private ModelGenUserConfig userConfig;
    @Setter
    private String rootClassName;
    @Setter
    private String rootClassRemark;

    private ModelNode rootNode;
    private ModelNode preRootNode;

    private FileRepo fileRepo;

    @Builder
    public ModelNodeMgr(String jsonString, ModelGenUserConfig userConfig, String rootClassName, String rootClassRemark) {
        this.jsonString = jsonString;
        this.userConfig = userConfig;
        this.rootClassName = rootClassName;
        this.rootClassRemark = rootClassRemark;
    }

    /**
     * 分析JSON string并构建模型节点
     */
    public void analysis() throws JsonProcessingException {
        JsonNode jsonNode = JsonNodeAnalyser.readJsonObjectNode(jsonString);
        if (!jsonNode.isObject()) {
            throw new RuntimeException("Only support analysis object type json node");
        }

        ModelNode rootNode = ModelNode.createRoot(jsonNode, rootClassName, rootClassRemark);
        List<ModelNode> childNodes = rootNode.createChildNodes();
        rootNode.setChildNodes(childNodes);
        rootNode.setMgr(this);
        this.rootNode = rootNode;

        handleUserConfigAfterRootCreated();
    }

    /**
     * 节点初始化后，自动进行用户预选配置处理
     */
    private void handleUserConfigAfterRootCreated() {
        if (userConfig.isEnableRealtimeJsonDefaultValueAnalysis()) {
            // 根据用户配置，使用导入的Json字段值设置模型的默认数据
            handleRealtimeJsonDefaultValue(rootNode);
        }
    }

    /**
     * 处理预置默认值
     */
    private void handleRealtimeJsonDefaultValue(ModelNode modelNode) {
        // 根据JSON节点值设置dart中基本类型的默认值
        if (DartDataType.OBJECT != modelNode.getTargetMeta().getDataType()) {
            JsonNode node = modelNode.getJsonNode();
            Object value = null;
            if (node.isBoolean()) {
                value = node.asBoolean();
            } else if (node.isTextual()) {
                value = node.asText();
            } else if (node.isInt()) {
                value = node.asInt();
            } else if (node.isLong()) {
                value = node.asLong();
            } else if (node.isBigInteger()) {
                value = node.bigIntegerValue();
            } else if (node.isDouble() || node.isFloat()) {
                value = node.asDouble();
            } else if (node.isFloat()) {
                value = node.floatValue();
            }
            modelNode.getTargetMeta().setDefaultValue(value);
        }

        for (ModelNode childNode : modelNode.getChildNodes()) {
            handleRealtimeJsonDefaultValue(childNode);
        }
    }

    /**
     * 使用用户数据重新构建根节点
     */
    public void rebuildRootModelNode(InputDataVO inputData) throws JsonProcessingException {
        jsonString = inputData.getJsonString();
        userConfig = inputData.getUserConfig();
        rootClassName = inputData.getRootClassName();
        rootClassRemark = inputData.getRootClassRemark();

        Cloner cloner = new Cloner();
        // 临时保存当前的节点信息
        preRootNode = cloner.deepClone(rootNode);
        analysis();

        copyModelNodeData(rootNode, "");

        preRootNode = null;
    }

    private void copyModelNodeData(ModelNode node, String parentJsonFieldName) {
        for (ModelNode childNode : node.getChildNodes()) {
            // 获取新的子节点的JSON路径,用于在旧的根节点下查找对应的子节点
            ModelNodeMeta meta = childNode.getMeta();
            String jsonFieldName = meta.getJsonFieldName();
            if (!meta.isBasisModelNodeDataType()) {
                String jsonPath = StringUtils.isBlank(parentJsonFieldName) ? jsonFieldName : parentJsonFieldName + "." + jsonFieldName;
                copyModelNodeData(childNode, jsonPath);
            }

            // 根据JSON路径在旧的根节点下查找对应的子节点
            String jsonPath = StringUtils.isBlank(parentJsonFieldName) ? jsonFieldName : parentJsonFieldName + "." + jsonFieldName;
            ModelNode oldChildNode = preRootNode.findChildByPath(jsonPath);
            if (Objects.nonNull(oldChildNode)) {
                ModelTargetMeta oldTargetMeta = oldChildNode.getTargetMeta();
                ModelTargetMeta targetMeta = childNode.getTargetMeta();
                targetMeta.setRemark(oldTargetMeta.getRemark());
                if (oldChildNode.getMeta().isBasisModelNodeDataType()) {
                    targetMeta.setDataType(oldTargetMeta.getDataType());
                    targetMeta.setDefaultValue(oldTargetMeta.getDefaultValue());
                }
                targetMeta.setIsRequired(oldTargetMeta.getIsRequired());
                targetMeta.setPropertyName(oldTargetMeta.getPropertyName());
                if (oldChildNode.getMeta().getModelNodeDataType() == ModelNodeDataType.OBJECT
                        || oldChildNode.getMeta().getModelNodeDataType() == ModelNodeDataType.OBJECT_ARRAY) {
                    targetMeta.setFilename(oldTargetMeta.getFilename());
                    targetMeta.setClassName(oldTargetMeta.getClassName());
                }
            }
        }
    }

    public void output(VirtualFile parent, Project project) {
        this.fileRepo = new FileRepo(parent, project);

        rebuildModelNodeRemark(rootNode.getChildNodes());
        rebuildObjectTypeModelTargetNames(rootNode);

        // 检验是否存在重名的dart文件
        final Set<String> targetFolderFileNames = new HashSet<>();
        resolveDuplicateNameFiles(targetFolderFileNames, rootNode);

        if (userConfig.isEnableAllClassGeneratedIntoSingleFile()) {
            // 将多个对象类生成在同一个dart文件中
            SingleDartFile single = rootNode.outputSingleDartFile();
            fileRepo.createDartFile(single.getNode().getTargetMeta().getFilename(), single.getContent());
        } else {
            // 将多个对象类生成在单独的dart文件中
            List<MultiDartFile> multis = rootNode.getMultiDartFileRecursively();
            for (MultiDartFile multi : multis) {
                fileRepo.createDartFile(multi.getNode().getTargetMeta().getFilename(), multi.getContent());
            }
        }
    }

    private void rebuildObjectTypeModelTargetNames(ModelNode node) {
        if (ModelNodeDataType.OBJECT == node.getMeta().getModelNodeDataType()) {
            for (ModelNode childNode : node.getChildNodes()) {
                rebuildObjectTypeModelTargetNames(childNode);
            }
        }

        if (ModelNodeDataType.OBJECT_ARRAY == node.getMeta().getModelNodeDataType()
                && CollectionUtils.isNotEmpty(node.getChildNodes())) {
            ModelNode firstChild = node.getChildNodes().get(0);
            firstChild.getTargetMeta().setClassName(node.getTargetMeta().getClassName());
            firstChild.getTargetMeta().setFilename(node.getTargetMeta().getFilename());
            for (ModelNode childChildNode : firstChild.getChildNodes()) {
                rebuildObjectTypeModelTargetNames(childChildNode);
            }
        }
    }

    private void rebuildModelNodeRemark(List<ModelNode> nodes) {
        for (ModelNode node : nodes) {
            if (StringUtils.isBlank(node.getTargetMeta().getRemark())) {
                node.getTargetMeta().setRemark(node.getMeta().getJsonFieldName());
            }

            rebuildModelNodeRemark(node.getChildNodes());
        }
    }

    private void resolveDuplicateNameFiles(final Set<String> existDartFilenames, ModelNode node) {
        if (node.getMeta().isBasisModelNodeDataType()) {
            return;
        }

        if (ModelNodeDataType.OBJECT == node.getMeta().getModelNodeDataType()) {
            String dartFileName = node.getTargetMeta().getFilename();
            if (existDartFilenames.contains(dartFileName)) {
                String antiDuplicateDartFileNameEOF = RandomStringUtils.randomAlphabetic(4);
                node.getTargetMeta().setFilename(dartFileName + "_" + antiDuplicateDartFileNameEOF);
            }

            existDartFilenames.add(node.getTargetMeta().getFilename());
        }

        for (ModelNode childNode : node.getChildNodes()) {
            resolveDuplicateNameFiles(existDartFilenames, childNode);
        }
    }
}
