package pers.chaos.jsondartserializable.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import pers.chaos.jsondartserializable.core.json.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.utils.DartClassFileUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JsonDartAnalysisMapping {
    private final String dartFileName;
    private final String className;

    private final MappingModel rootMappingModel;

    public JsonDartAnalysisMapping(String className, JsonNode node) {
        this.className = DartClassFileUtils.getDartClassName(className);
        this.dartFileName = DartClassFileUtils.getDartFileNameByClassName(className);

        // init root mapping model
        rootMappingModel = new MappingModel(className, node, true);

        // root mapping model really analysis in there,
        // MappingModel constructor must judge `isRoot` and
        // currently only support root mapping model is
        // an OBJECT
        analysisMapping(rootMappingModel, node);
    }

    private void analysisMapping(MappingModel mappingModel, JsonNode node) {
        // process the fields under the root mapping model in turn
        node.fieldNames().forEachRemaining(fieldName -> {
            JsonNode childNode = node.get(fieldName);
            MappingModel mm = new MappingModel(fieldName, childNode, false);

            mappingModel.getInnerMappingModels().add(mm);
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
