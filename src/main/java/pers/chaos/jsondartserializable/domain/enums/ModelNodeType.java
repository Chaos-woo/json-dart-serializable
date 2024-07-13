package pers.chaos.jsondartserializable.domain.enums;

public enum ModelNodeType {
    /**
     * 根节点
     */
    ROOT,
    /**
     * 普通节点
     */
    NORMAL,
    ;

    public boolean is(ModelNodeType type) {
        return this == type;
    }
}
