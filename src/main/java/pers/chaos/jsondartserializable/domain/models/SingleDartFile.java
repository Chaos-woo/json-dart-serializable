package pers.chaos.jsondartserializable.domain.models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SingleDartFile {
    private ModelNode node;
    private String content;
}
