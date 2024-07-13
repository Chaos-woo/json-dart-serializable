package pers.chaos.jsondartserializable.domain.models.node;

public interface ModelConst {
    /**
     * 魔法常量
     */
    interface MagicKey {
        /**
         * 对象扩展
         */
        String objectExt = "@oext";
        /**
         * 基本类型扩展
         */
        String basisExt = "@bext";
        /**
         * 数组对象扩展
         */
        String arrayExt = "@arroext";
        /**
         * 数组基本类型扩展
         */
        String arrayBasicExt = "@arrbext";
    }
}
