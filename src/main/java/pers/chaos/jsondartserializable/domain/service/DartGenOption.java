package pers.chaos.jsondartserializable.domain.service;

import com.google.common.base.CaseFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import pers.chaos.jsondartserializable.domain.util.StringConst;

import java.util.function.Function;

/**
 * Dart生成的常量选项
 */
public interface DartGenOption {
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
     * Dart完整文件名生成器
     */
    @Getter
    @AllArgsConstructor
    enum FileSuffix {
        DART(".dart"),
        ;

        private final String suffix;

        public String filename(String name) {
            return name + suffix;
        }
    }

    /**
     * Dart相关数据名字生成器
     */
    @Getter
    @AllArgsConstructor
    enum NameGen {
        /**
         * 文件名
         */
        FILE(input -> {
            if (input.contains(StringConst.underline)) {
                return input;
            } else if (input.contains(StringConst.strikethrough)) {
                return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_UNDERSCORE, input);
            }
            return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, input);
        }),
        /**
         * 类名
         */
        CLASS(input -> {
            if (input.contains(StringConst.underline)) {
                return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, input);
            } else if (input.contains(StringConst.strikethrough)) {
                return CaseFormat.LOWER_HYPHEN.to(CaseFormat.UPPER_CAMEL, input);
            }
            return input.substring(0, 1).toUpperCase() + input.substring(1);
        }),
        /**
         * 属性名
         */
        PROPERTY(input -> {
            if (input.contains(StringConst.underline)) {
                return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, input);
            } else if (input.contains(StringConst.strikethrough)) {
                return CaseFormat.LOWER_HYPHEN.to(CaseFormat.LOWER_CAMEL, input);
            }
            return input.substring(0, 1).toLowerCase() + input.substring(1);
        }),
        ;

        private final Function<String, String> convert;

        public String gen(String input) {
            return convert.apply(input);
        }
    }
}
