package pers.chaos.jsondartserializable.domain.ui.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import pers.chaos.jsondartserializable.domain.models.ModelNode;

@Data
@AllArgsConstructor
public class ModelNodeTreeVO {
    private final ModelNode node;

    @Override
    public String toString() {
        return node.getTargetMeta().getClassName();
    }
}
