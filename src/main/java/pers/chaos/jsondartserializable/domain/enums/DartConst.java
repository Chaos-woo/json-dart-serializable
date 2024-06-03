package pers.chaos.jsondartserializable.domain.enums;

/**
 * Dart常量
 */
public interface DartConst {
    /**
     * 字段再dart中是否是必填字段
     */
    interface Required {
        Boolean yes = Boolean.TRUE;
        Boolean no = Boolean.FALSE;
    }

    /**
     * 标识是否使用 @JsonKey 注解修饰字段与JSON节点中名字的映射关系
     */
    interface UseJsonKey {
        Boolean yes = Boolean.TRUE;
        Boolean no = Boolean.FALSE;
    }

    /**
     * 文件相关
     */
    interface File {
        String suffix = ".dart";
    }
}
