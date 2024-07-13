package pers.chaos.jsondartserializable.domain.models.node;

import lombok.Getter;
import pers.chaos.jsondartserializable.domain.models.nodedata.ModelNodeMeta;
import pers.chaos.jsondartserializable.domain.models.nodedata.ModelOutputMeta;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;

/**
 * 节点反射器
 */
public interface ModelNodeReflector {
    /**
     * 模型属性编辑Key
     */
    @Getter
    enum Key {
        M_JSON_FIELD_NAME("jsonFieldName", 0, "Json field"),
        M_JSON_DATA_TYPE("modelNodeDataType", 7, "Json node type"),

        TM_DART_PROPERTY_NAME("propertyName", 1, "Dart field"),
        TM_DART_DATA_TYPE("dataType", 2, "Dart basis type"),
        TM_DART_PROPERTY_REQUIRED("isRequired", 3, "Required?"),
        TM_DART_PROPERTY_DEFAULT_VALUE("defaultValue", 4, "Default value"),
        TM_DART_FILE_NAME("filename", 5, "Dart filename"),
        TM_CLASS_NAME("classname", 6, "Dart class name"),
        TM_REMARK("remark", 8, "Remark"),
        ;

        private final String property;
        private final int columnIndex;
        private final String columnName;

        Key(String property, int columnIndex, String columnName) {
            this.property = property;
            this.columnIndex = columnIndex;
            this.columnName = columnName;
        }

        public static Key[] sortedByColumnIndexKeys() {
            Key[] keys = Key.values();
            Arrays.sort(keys, Comparator.comparingInt(Key::getColumnIndex));
            return keys;
        }

        public boolean equalsColumn(int column) {
            return columnIndex == column;
        }

        /**
         * ModelNode元数据反射读
         */
        public Object reflectRead(ModelNode node) {
            try {
                boolean isMetaClass = this == M_JSON_DATA_TYPE || this == M_JSON_FIELD_NAME;
                PropertyDescriptor descriptor = new PropertyDescriptor(property, isMetaClass ? ModelNodeMeta.class : ModelOutputMeta.class);
                Method readMethod = descriptor.getReadMethod();
                Object o = readMethod.invoke(isMetaClass ? node.getNodeMeta() : node.getOutputMeta());

                if (this == TM_DART_PROPERTY_REQUIRED) {
                    return ((boolean) o) ? Boolean.TRUE : Boolean.FALSE;
                } else {
                    return o;
                }
            } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }

        /**
         * ModelNode元数据反射写
         */
        public void reflectWrite(ModelNode node, Object value) {
            try {
                boolean isMetaClass = this == M_JSON_DATA_TYPE || this == M_JSON_FIELD_NAME;
                PropertyDescriptor descriptor = new PropertyDescriptor(property, isMetaClass ? ModelNodeMeta.class : ModelOutputMeta.class);
                Method writeMethod = descriptor.getWriteMethod();
                writeMethod.invoke(isMetaClass ? node.getNodeMeta() : node.getOutputMeta(), value);
            } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
