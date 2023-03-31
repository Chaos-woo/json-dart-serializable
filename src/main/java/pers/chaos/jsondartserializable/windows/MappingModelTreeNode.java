package pers.chaos.jsondartserializable.windows;

import pers.chaos.jsondartserializable.core.json.MappingModel;

public class MappingModelTreeNode {

    private final MappingModel model;

    public MappingModelTreeNode(MappingModel model) {
        this.model = model;
    }

    public MappingModel getModel() {
        return model;
    }

    @Override
    public String toString() {
        return this.model.getClassName();
    }
}
