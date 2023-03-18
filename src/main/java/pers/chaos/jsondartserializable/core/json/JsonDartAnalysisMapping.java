package pers.chaos.jsondartserializable.core.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import pers.chaos.jsondartserializable.utils.DartClassFileUtils;

public class JsonDartAnalysisMapping {
    private final String dartFileName;
    private final String className;

    private MappingModel rootMappingModel;

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
        // start generate file by root mapping model
        rootMappingModel.cycleGeneratedDartFile(parent, project);
    }

    public String getDartFileName() {
        return dartFileName + ".dart";
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
