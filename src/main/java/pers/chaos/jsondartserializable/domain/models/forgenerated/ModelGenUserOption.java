package pers.chaos.jsondartserializable.domain.models.forgenerated;

import lombok.Setter;

/**
 * 用户可选操作
 */
@Setter
public class ModelGenUserOption {
    /**
     * 是否开启使用JSON字段值作为生成文件的属性默认值
     */
    private boolean enableRealtimeJsonDefaultValueAnalysis;
    /**
     * 是否开启所有对象生成在一个dart文件中
     */
    private boolean enableAllClassGeneratedIntoSingleFile;
    /**
     * 是否使用自定义JSON扩展语法处理模型属性元数据，开启此模式情况下，实时默认值不生效
     */
    private boolean enableCustomJsonSyntax;

    /**
     * 是否开启使用JSON字段值作为生成文件的属性默认值
     */
    public boolean useRealtimeJsonValForDefaultVal() {
        return enableRealtimeJsonDefaultValueAnalysis;
    }

    /**
     * 是否开启所有对象生成在一个dart文件中
     */
    public boolean useSingleFileForAllClassGenerated() {
        return enableAllClassGeneratedIntoSingleFile;
    }

    /**
     * 是否开启使用JSON字段值作为生成文件的属性默认值
     */
    public boolean useCustomJsonSyntaxForAllClassGenerated() {
        return enableCustomJsonSyntax;
    }
}
