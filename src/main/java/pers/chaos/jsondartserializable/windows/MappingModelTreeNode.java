package pers.chaos.jsondartserializable.windows;

import pers.chaos.jsondartserializable.core.json.MappingModelNode;

public class MappingModelTreeNode {

    private final MappingModelNode model;

    public MappingModelTreeNode(MappingModelNode model) {
        this.model = model;
    }

    public MappingModelNode getModel() {
        return model;
    }

    @Override
    public String toString() {
        return this.model.getClassName();
    }
}
