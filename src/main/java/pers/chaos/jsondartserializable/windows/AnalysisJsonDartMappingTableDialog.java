package pers.chaos.jsondartserializable.windows;

import pers.chaos.jsondartserializable.core.json.MappingModelNode;
import pers.chaos.jsondartserializable.core.constants.JsonAnalysisTableKeys;
import pers.chaos.jsondartserializable.core.enums.DartDataTypeEnum;
import pers.chaos.jsondartserializable.core.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.windows.components.DartDataTypeComboBox;
import pers.chaos.jsondartserializable.windows.components.DartPropertyRequiredCheckBox;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AnalysisJsonDartMappingTableDialog extends JDialog {
    private final MappingModelNode mappingModelNode;
    private final JsonAnalysisTableKeys.MappingModelTableReflectable[] tablesColumnReflectable;

    private JPanel contentPane;
    private JButton buttonConfirm;
    private JTable jsonAnalysisTable;
    private JLabel labelClassTitle;

    private boolean[][] editableCellRecords;

    public AnalysisJsonDartMappingTableDialog(MappingModelNode mappingModelNode) {
        this.mappingModelNode = mappingModelNode;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonConfirm);

        buttonConfirm.addActionListener(e -> onConfirm());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(
                e -> onCancel(),
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
        );

        this.tablesColumnReflectable = JsonAnalysisTableKeys.MappingModelTableReflectable.values();
        Arrays.sort(this.tablesColumnReflectable, Comparator.comparingInt(JsonAnalysisTableKeys.MappingModelTableReflectable::getTableColumnIndex));

        // 提前处理表格模型展示数据
        final Object[][] tableModelData = getMappingModelTableModelData(this.tablesColumnReflectable);
        // 获取表格的数据模型及可编辑规则
        DefaultTableModel tableModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) {
                return tableCellEditableRuleVerify(tableModelData, row, column);
            }

        };

        jsonAnalysisTable.setModel(tableModel);
        tableModel = (DefaultTableModel) jsonAnalysisTable.getModel();
        // 设置列标题
        Object[] columnTitle = Arrays.stream(this.tablesColumnReflectable)
                .map(JsonAnalysisTableKeys.MappingModelTableReflectable::getColumnName)
                .toArray();
        tableModel.setColumnIdentifiers(columnTitle);

        // set dart data basis type ComboBox cell
        jsonAnalysisTable.getColumnModel()
                .getColumn(JsonAnalysisTableKeys.MappingModelTableReflectable.DART_DATA_TYPE.getTableColumnIndex())
                .setCellEditor(new DartDataTypeComboBox.DartDataTypeComboBoxCellEditor());
        jsonAnalysisTable.getColumnModel()
                .getColumn(JsonAnalysisTableKeys.MappingModelTableReflectable.DART_DATA_TYPE.getTableColumnIndex())
                .setCellRenderer(new DartDataTypeComboBox.DartDataTypeComboBoxRenderer());

        // set dart property [required] decorate CheckBox cell
        jsonAnalysisTable.getColumnModel()
                .getColumn(JsonAnalysisTableKeys.MappingModelTableReflectable.DART_PROPERTY_REQUIRED.getTableColumnIndex())
                .setCellEditor(new DartPropertyRequiredCheckBox.DartPropertyRequiredCheckBoxCellEditor());
        jsonAnalysisTable.getColumnModel()
                .getColumn(JsonAnalysisTableKeys.MappingModelTableReflectable.DART_PROPERTY_REQUIRED.getTableColumnIndex())
                .setCellRenderer(new DartPropertyRequiredCheckBox.DartPropertyRequiredCheckBoxRenderer());

        // 设置表格数据
        for (Object[] data : tableModelData) {
            tableModel.addRow(data);
        }

        labelClassTitle.setText(JsonAnalysisTableKeys.ObjectPropertyTable.formatTableTitle(this.mappingModelNode.getClassName()));
    }

    private Object[][] getMappingModelTableModelData(JsonAnalysisTableKeys.MappingModelTableReflectable[] tablesColumnReflectable) {
        List<MappingModelNode> innerMappingModelNodes = this.mappingModelNode.getChildModelNodes();
        Object[][] tableData = new Object[innerMappingModelNodes.size()][tablesColumnReflectable.length];

        // 初始化可编辑的数据记录
        this.editableCellRecords = new boolean[innerMappingModelNodes.size()][tablesColumnReflectable.length];
        for (int i = 0; i < innerMappingModelNodes.size(); i++) {
            MappingModelNode innerProperty = innerMappingModelNodes.get(i);
            for (int j = 0; j < tablesColumnReflectable.length; j++) {
                // 初始化时所有格子都可编辑
                this.editableCellRecords[i][j] = true;

                JsonAnalysisTableKeys.MappingModelTableReflectable ref = tablesColumnReflectable[j];
                try {
                    PropertyDescriptor descriptor = new PropertyDescriptor(ref.getPropertyName(), innerProperty.getClass());
                    Method readMethod = descriptor.getReadMethod();
                    Object o = readMethod.invoke(innerProperty);

                    if (ref == JsonAnalysisTableKeys.MappingModelTableReflectable.DART_PROPERTY_REQUIRED) {
                        tableData[i][j] = ((boolean) o) ? Boolean.TRUE : Boolean.FALSE;
                    } else {
                        tableData[i][j] = o;
                    }
                } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return tableData;
    }

    private void onConfirm() {
        // 表数据转换为模型节点数据
        List<MappingModelNode> innerMappingModelNodes = this.mappingModelNode.getChildModelNodes();
        for (int i = 0; i < innerMappingModelNodes.size(); i++) {
            MappingModelNode innerProperty = innerMappingModelNodes.get(i);
            for (int j = 0; j < this.tablesColumnReflectable.length; j++) {
                if (!this.editableCellRecords[i][j]) {
                    continue;
                }

                JsonAnalysisTableKeys.MappingModelTableReflectable ref = tablesColumnReflectable[j];
                try {
                    PropertyDescriptor descriptor = new PropertyDescriptor(ref.getPropertyName(), innerProperty.getClass());
                    Method writeMethod = descriptor.getWriteMethod();
                    Object userInput = jsonAnalysisTable.getModel().getValueAt(i, j);
                    writeMethod.invoke(innerProperty, userInput);
                } catch (IntrospectionException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }

                innerProperty.innerLocalPropertyRebuild();
            }
        }

        dispose();
    }

    private boolean tableCellEditableRuleVerify(final Object[][] tableModelData, int row, int column) {
        // 可被编辑的JSON字段
        if (column == JsonAnalysisTableKeys.MappingModelTableReflectable.JSON_FIELD_NAME.getTableColumnIndex()) {
            this.editableCellRecords[row][column] = false;
            return false;
        }

        // 不可被编辑的JSON字段
        if (column == JsonAnalysisTableKeys.MappingModelTableReflectable.JSON_DATA_TYPE.getTableColumnIndex()) {
            this.editableCellRecords[row][column] = false;
            return false;
        }

        // dart对象类型不可被编辑
        if (column == JsonAnalysisTableKeys.MappingModelTableReflectable.DART_DATA_TYPE.getTableColumnIndex()) {
            DartDataTypeEnum dartDataTypeEnum = (DartDataTypeEnum) tableModelData[row][column];
            if (DartDataTypeEnum.OBJECT == dartDataTypeEnum) {
                this.editableCellRecords[row][column] = false;
                return false;
            }
        }

        // dart基础数据类型不可被编辑为对象类型
        if (column == JsonAnalysisTableKeys.MappingModelTableReflectable.DART_FILE_NAME.getTableColumnIndex()) {
            DartDataTypeEnum dartDataTypeEnum = (DartDataTypeEnum) tableModelData[row][JsonAnalysisTableKeys.MappingModelTableReflectable.DART_DATA_TYPE.getTableColumnIndex()];
            if (DartDataTypeEnum.OBJECT != dartDataTypeEnum) {
                this.editableCellRecords[row][column] = false;
                return false;
            }
        }

        // 仅有对象类型的才可被编辑类名
        if (column == JsonAnalysisTableKeys.MappingModelTableReflectable.INNER_OBJECT_CLASS_NAME.getTableColumnIndex()) {
            DartDataTypeEnum dartDataTypeEnum = (DartDataTypeEnum) tableModelData[row][JsonAnalysisTableKeys.MappingModelTableReflectable.DART_DATA_TYPE.getTableColumnIndex()];
            if (DartDataTypeEnum.OBJECT != dartDataTypeEnum) {
                this.editableCellRecords[row][column] = false;
                return false;
            }
        }

        // 如果不是dart基本数据类型，那不可编辑默认值数据
        if (column == JsonAnalysisTableKeys.MappingModelTableReflectable.DART_PROPERTY_DEFAULT_VALUE.getTableColumnIndex()) {
            JsonTypeEnum jsonTypeEnum = (JsonTypeEnum) tableModelData[row][JsonAnalysisTableKeys.MappingModelTableReflectable.JSON_DATA_TYPE.getTableColumnIndex()];
            if (JsonTypeEnum.BASIS_TYPE != jsonTypeEnum) {
                this.editableCellRecords[row][column] = false;
                return false;
            }
        }

        return true;
    }

    private void onCancel() {
        dispose();
    }
}
