package pers.chaos.jsondartserializable.domain.models.nodedata;

import lombok.Data;
import pers.chaos.jsondartserializable.domain.enums.DartDataType;

/**
 * 模型节点生成目标元数据
 */
@Data
public class ModelOutputMeta {
    /**
     * 生成的dart属性名
     */
    private String propertyName;
    /**
     * 当前结点类型为Object或数组中的Object时，需要生成对应的dart文件名
     */
    private String filename;
    /**
     * 当前结点类型为Object或数组中的Object时，需要生成对应的dart类名
     */
    private String classname;
    /**
     * dart数据类型，例如， String，int
     */
    private DartDataType dataType;
    /**
     * 当前dart字段是否是必填，必填类型在dart空安全版本初始化函数中需要使用required关键字
     */
    private Boolean isRequired;
    /**
     * dart属性默认值
     */
    private Object defaultValue;
    /**
     * dart属性描述，用于注释描述字段的作用
     */
    private String remark;
    /**
     * 标识是否使用 @JsonKey 注解修饰字段与JSON节点中名字的映射关系
     */
    private Boolean markJsonKeyAnno;
}
