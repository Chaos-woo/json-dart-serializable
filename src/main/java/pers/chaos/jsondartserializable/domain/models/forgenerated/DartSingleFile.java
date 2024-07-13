package pers.chaos.jsondartserializable.domain.models.forgenerated;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pers.chaos.jsondartserializable.domain.models.node.ModelNode;

/**
 * 单dart文件数据承载对象
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DartSingleFile {
    private ModelNode node;
    private String content;
}
