package pers.chaos.jsondartserializable.domain.models;

import lombok.Data;

@Data
public class ModelGenUserConfig {
    /**
     * 是否开启试试JSON字段值作为生成文件的属性默认值
     */
    private boolean enableRealtimeJsonDefaultValueAnalysis;
    /**
     * 是否开启所有对象生成在一个dart文件中
     */
    private boolean enableAllClassGeneratedIntoSingleFile;
}
