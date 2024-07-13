package pers.chaos.jsondartserializable.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.rits.cloning.Cloner;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeDataType;
import pers.chaos.jsondartserializable.domain.models.forgenerated.DartMultiFile;
import pers.chaos.jsondartserializable.domain.models.forgenerated.DartSingleFile;
import pers.chaos.jsondartserializable.domain.models.forgenerated.ModelGenUserOption;
import pers.chaos.jsondartserializable.domain.models.node.ModelDataDefinition;
import pers.chaos.jsondartserializable.domain.models.node.ModelNode;
import pers.chaos.jsondartserializable.domain.models.nodedata.ModelNodeMeta;
import pers.chaos.jsondartserializable.domain.models.nodedata.ModelOutputMeta;
import pers.chaos.jsondartserializable.domain.repository.FileRepository;
import pers.chaos.jsondartserializable.domain.ui.models.UserInputData;
import pers.chaos.jsondartserializable.domain.util.JsonNodeUtil;

import java.util.*;

/**
 * 模型节点管理器
 */
@Getter
public class ModelNodesMgr {
    /**
     * 原始输入的JSON字符串
     */
    @Setter
    private String originalUserInputJsonString;
    /**
     * 用户输入的可选项配置
     */
    @Setter
    private ModelGenUserOption userOption;
    /**
     * 记录用户输入的根节点类名
     */
    @Setter
    private String rootClassName;
    /**
     * 记录用户输入的根节点类的备注，描述
     */
    @Setter
    private String userInputRootClassRemark;

    /**
     * 模型根节点
     */
    private ModelNode rootNode;
    /**
     * 前一模型根节点，用于生成新的模型根节点时数据复制，复制完成后，该变量将被置为NULL
     */
    private ModelNode preRootNode;

    /**
     * 文件仓库，用于文件生成
     */
    private FileRepository fileRepository;

    @Builder
    public ModelNodesMgr(String jsonString, ModelGenUserOption userOption, String rootClassName, String rootClassRemark) {
        this.originalUserInputJsonString = jsonString;
        this.userOption = userOption;
        this.rootClassName = rootClassName;
        this.userInputRootClassRemark = rootClassRemark;
    }

    /**
     * 分析JSON string并构建模型节点
     */
    public void startAnalysisDataAndBuildModelNode() throws JsonProcessingException {
        JsonNode jsonNode = JsonNodeUtil.readJsonObjectNode(originalUserInputJsonString);
        if (!jsonNode.isObject()) {
            throw new RuntimeException("Current version only support analysis object type json string");
        }

        if (userOption.useCustomJsonSyntaxForAllClassGenerated()) {
            // 预处理移除根节点中的Json扩展语法
            Iterator<Map.Entry<String, JsonNode>> fieldsIter = jsonNode.fields();
            while (fieldsIter.hasNext()) {
                Map.Entry<String, JsonNode> field = fieldsIter.next();
                if (StringUtils.equals(ModelDataDefinition.MagicKey.objectExt, field.getKey())) {
                    fieldsIter.remove();
                }
            }
        }
        ModelNode rootNode = ModelNode.createRoot(this, jsonNode, rootClassName, userInputRootClassRemark);
        List<ModelNode> childNodes = rootNode.createChildNodes();
        rootNode.setChildNodes(childNodes);
        this.rootNode = rootNode;
    }

    /**
     * 使用用户数据重新构建根节点
     */
    public void rebuildRootModelNode(UserInputData inputData) throws JsonProcessingException {
        originalUserInputJsonString = inputData.getJsonString();
        userOption = inputData.getUserOption();
        rootClassName = inputData.getRootClassName();
        userInputRootClassRemark = inputData.getRootClassRemark();

        Cloner cloner = new Cloner();
        // 临时保存当前的节点信息
        preRootNode = cloner.deepClone(rootNode);
        startAnalysisDataAndBuildModelNode();

        copyModelNodeData(rootNode, "");

        preRootNode = null;
    }

    private void copyModelNodeData(ModelNode node, String parentJsonFieldName) {
        for (ModelNode childNode : node.getChildNodes()) {
            // 获取新的子节点的JSON路径,用于在旧的根节点下查找对应的子节点
            ModelNodeMeta meta = childNode.getNodeMeta();
            String jsonFieldName = meta.getJsonFieldName();
            if (!meta.isBasisModelNodeDataType()) {
                String jsonPath = StringUtils.isBlank(parentJsonFieldName) ? jsonFieldName : parentJsonFieldName + "." + jsonFieldName;
                copyModelNodeData(childNode, jsonPath);
            }

            // 根据JSON路径在旧的根节点下查找对应的子节点
            String jsonPath = StringUtils.isBlank(parentJsonFieldName) ? jsonFieldName : parentJsonFieldName + "." + jsonFieldName;
            ModelNode oldChildNode = preRootNode.findChildByPath(jsonPath);
            if (Objects.nonNull(oldChildNode)) {
                ModelOutputMeta oldTargetMeta = oldChildNode.getOutputMeta();
                ModelOutputMeta targetMeta = childNode.getOutputMeta();
                targetMeta.setRemark(oldTargetMeta.getRemark());
                if (oldChildNode.getNodeMeta().isBasisModelNodeDataType()) {
                    targetMeta.setDataType(oldTargetMeta.getDataType());
                    targetMeta.setDefaultValue(oldTargetMeta.getDefaultValue());
                }
                targetMeta.setIsRequired(oldTargetMeta.getIsRequired());
                targetMeta.setPropertyName(oldTargetMeta.getPropertyName());
                if (oldChildNode.getNodeMeta().getModelNodeDataType() == ModelNodeDataType.OBJECT
                        || oldChildNode.getNodeMeta().getModelNodeDataType() == ModelNodeDataType.OBJECT_ARRAY) {
                    targetMeta.setFilename(oldTargetMeta.getFilename());
                    targetMeta.setClassname(oldTargetMeta.getClassname());
                }
            }
        }
    }

    public void output(VirtualFile parent, Project project) {
        this.fileRepository = new FileRepository(parent, project);

        rebuildModelNodeRemark(rootNode.getChildNodes());
        rebuildObjectTypeModelTargetNames(rootNode);

        // 检验是否存在重名的dart文件
        final Set<String> targetFolderFileNames = new HashSet<>();
        resolveDuplicateNameFiles(targetFolderFileNames, rootNode);

        if (userOption.useSingleFileForAllClassGenerated()) {
            // 将多个对象类生成在同一个dart文件中
            DartSingleFile single = rootNode.outputSingleDartFile();
            fileRepository.createDartFile(single.getNode().getOutputMeta().getFilename(), single.getContent());
        } else {
            // 将多个对象类生成在单独的dart文件中
            List<DartMultiFile> multis = rootNode.getMultiDartFileRecursively();
            for (DartMultiFile multi : multis) {
                fileRepository.createDartFile(multi.getNode().getOutputMeta().getFilename(), multi.getContent());
            }
        }
    }

    private void rebuildObjectTypeModelTargetNames(ModelNode node) {
        if (ModelNodeDataType.OBJECT == node.getNodeMeta().getModelNodeDataType()) {
            for (ModelNode childNode : node.getChildNodes()) {
                rebuildObjectTypeModelTargetNames(childNode);
            }
        }

        if (ModelNodeDataType.OBJECT_ARRAY == node.getNodeMeta().getModelNodeDataType()
                && CollectionUtils.isNotEmpty(node.getChildNodes())) {
            ModelNode firstChild = node.getChildNodes().get(0);
            firstChild.getOutputMeta().setClassname(node.getOutputMeta().getClassname());
            firstChild.getOutputMeta().setFilename(node.getOutputMeta().getFilename());
            for (ModelNode childChildNode : firstChild.getChildNodes()) {
                rebuildObjectTypeModelTargetNames(childChildNode);
            }
        }
    }

    private void rebuildModelNodeRemark(List<ModelNode> nodes) {
        for (ModelNode node : nodes) {
            if (StringUtils.isBlank(node.getOutputMeta().getRemark())) {
                node.getOutputMeta().setRemark(node.getNodeMeta().getJsonFieldName());
            }

            rebuildModelNodeRemark(node.getChildNodes());
        }
    }

    private void resolveDuplicateNameFiles(final Set<String> existDartFilenames, ModelNode node) {
        if (node.getNodeMeta().isBasisModelNodeDataType()) {
            return;
        }

        if (ModelNodeDataType.OBJECT == node.getNodeMeta().getModelNodeDataType()) {
            String dartFileName = node.getOutputMeta().getFilename();
            if (existDartFilenames.contains(dartFileName)) {
                String antiDuplicateDartFileNameEOF = RandomStringUtils.randomAlphabetic(4);
                node.getOutputMeta().setFilename(dartFileName + "_" + antiDuplicateDartFileNameEOF);
            }

            existDartFilenames.add(node.getOutputMeta().getFilename());
        }

        for (ModelNode childNode : node.getChildNodes()) {
            resolveDuplicateNameFiles(existDartFilenames, childNode);
        }
    }
}
