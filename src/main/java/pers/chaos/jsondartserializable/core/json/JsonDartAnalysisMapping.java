package pers.chaos.jsondartserializable.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import pers.chaos.jsondartserializable.core.json.enums.DartDataTypeEnum;
import pers.chaos.jsondartserializable.core.json.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.utils.DartClassFileUtils;
import pers.chaos.jsondartserializable.windows.UserAdvanceConfiguration;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonDartAnalysisMapping {
    private final String dartFileName;
    private final String className;

    private final MappingModel rootMappingModel;

    private final UserAdvanceConfiguration userAdvanceConfiguration;

    public JsonDartAnalysisMapping(String className, JsonNode node, UserAdvanceConfiguration userAdvanceConfiguration) {
        this.className = DartClassFileUtils.getDartClassName(className);
        this.dartFileName = DartClassFileUtils.getDartFileNameByClassName(className);

        this.userAdvanceConfiguration = userAdvanceConfiguration;

        // init root mapping model
        rootMappingModel = new MappingModel(className, node, true);

        // root mapping model really analysis in there,
        // MappingModel constructor must judge `isRoot` and
        // currently only support root mapping model is
        // an OBJECT
        analysisRootMappingModelNode(rootMappingModel, node);

        if (userAdvanceConfiguration.isEnableRealtimeJsonDefaultValueAnalysis()) {
            // enable realtime json default value option,
            // will set default value for all dart basis type
            // according to JSON string node value
            processRealtimeJsonDefaultValue(rootMappingModel);
        }
    }

    private void processRealtimeJsonDefaultValue(MappingModel mappingModel) {
        // set default value for all dart basis type
        // according to JSON string node value
        if (DartDataTypeEnum.OBJECT != mappingModel.getDartDataTypeEnum()) {
            JsonNode node = mappingModel.getNode();
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
            mappingModel.setDartPropertyDefaultValue(defaultValue);
        }

        if (CollectionUtils.isNotEmpty(mappingModel.getInnerMappingModels())) {
            for (MappingModel innerMappingModel : mappingModel.getInnerMappingModels()) {
                processRealtimeJsonDefaultValue(innerMappingModel);
            }
        }
    }

    private void analysisRootMappingModelNode(MappingModel rootMappingModel, JsonNode node) {
        // process the fields under the root mapping model in turn
        node.fieldNames().forEachRemaining(fieldName -> {
            JsonNode childNode = node.get(fieldName);
            MappingModel mm = new MappingModel(fieldName, childNode, false);

            rootMappingModel.getInnerMappingModels().add(mm);
        });
    }

    public void generated(VirtualFile parent, Project project) {

        // set default description of all mapping model
        this.innerMappingModelRebuildDescription(rootMappingModel.getInnerMappingModels());

        // OBJECT array's first child class name rebuild
        this.objectArrayInnerFirstMappingModelRebuildClassNameAndDartFileName(rootMappingModel);

        // check whether exist dart file of the same name
        final Set<String> existDartFileNames = new HashSet<>();
        this.checkAllMappingModelExistSameNameDartFileName(existDartFileNames, rootMappingModel);

        // start generate file by root mapping model
        rootMappingModel.cycleGeneratedDartFile(parent, project);
    }

    private void objectArrayInnerFirstMappingModelRebuildClassNameAndDartFileName(MappingModel mappingModel) {
        if (mappingModel.isBasisJsonType()) {
            return;
        }

        if (JsonTypeEnum.OBJECT == mappingModel.getJsonTypeEnum()
            && CollectionUtils.isNotEmpty(mappingModel.getInnerMappingModels())) {

            for (MappingModel innerMappingModel : mappingModel.getInnerMappingModels()) {
                objectArrayInnerFirstMappingModelRebuildClassNameAndDartFileName(innerMappingModel);
            }
        }

        if (JsonTypeEnum.OBJECT_ARRAY == mappingModel.getJsonTypeEnum()
                && CollectionUtils.isNotEmpty(mappingModel.getInnerMappingModels())) {

            MappingModel firstChild = mappingModel.getInnerMappingModels().get(0);
            firstChild.setClassName(mappingModel.getClassName());
            firstChild.setDartFileName(mappingModel.getDartFileName());

            if (CollectionUtils.isNotEmpty(firstChild.getInnerMappingModels())) {
                for (MappingModel innerMappingModel : firstChild.getInnerMappingModels()) {
                    objectArrayInnerFirstMappingModelRebuildClassNameAndDartFileName(innerMappingModel);
                }
            }
        }
    }

    private void checkAllMappingModelExistSameNameDartFileName(final Set<String> existDartFileNames, MappingModel mappingModel) {
        if (mappingModel.isBasisJsonType()) {
            return;
        }

        if (JsonTypeEnum.OBJECT == mappingModel.getJsonTypeEnum()) {
            String dartFileName = mappingModel.getDartFileName();
            if (existDartFileNames.contains(dartFileName)) {
                String antiDuplicateDartFileNameEOF = RandomStringUtils.randomAlphabetic(4);
                mappingModel.setDartFileName(dartFileName + "_" + antiDuplicateDartFileNameEOF);
            }

            existDartFileNames.add(mappingModel.getDartFileName());
        }

        if (CollectionUtils.isNotEmpty(mappingModel.getInnerMappingModels())) {
            for (MappingModel innerMappingModel : mappingModel.getInnerMappingModels()) {
                checkAllMappingModelExistSameNameDartFileName(existDartFileNames, innerMappingModel);
            }
        }
    }

    private void innerMappingModelRebuildDescription(List<MappingModel> innerMappingModels) {
        for (MappingModel innerMappingModel : innerMappingModels) {
            if (StringUtils.isBlank(innerMappingModel.getDescription())) {
                innerMappingModel.setDescription(innerMappingModel.getJsonFieldName());
            }

            if (CollectionUtils.isNotEmpty(innerMappingModel.getInnerMappingModels())) {
                innerMappingModelRebuildDescription(innerMappingModel.getInnerMappingModels());
            }
        }
    }

    public MappingModel getRootMappingModel() {
        return rootMappingModel;
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
