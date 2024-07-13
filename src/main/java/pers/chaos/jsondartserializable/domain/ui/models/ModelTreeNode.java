package pers.chaos.jsondartserializable.domain.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import pers.chaos.jsondartserializable.domain.models.node.ModelNode;

@Data
@AllArgsConstructor
public class ModelTreeNode {
    private final ModelNode node;

    @Override
    public String toString() {
        return node.getOutputMeta().getClassname();
    }
}
