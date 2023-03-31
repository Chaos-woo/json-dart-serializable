package pers.chaos.jsondartserializable.windows;

import pers.chaos.jsondartserializable.core.json.MappingModel;
import pers.chaos.jsondartserializable.core.json.constants.JsonAnalysisTableKeys;
import pers.chaos.jsondartserializable.core.json.enums.DartDataTypeEnum;
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
    private final MappingModel mappingModel;
    private final JsonAnalysisTableKeys.MappingModelTableReflectable[] tablesColumnReflectable;

    private JPanel contentPane;
    private JButton buttonConfirm;
    private JTable jsonAnalysisTable;
    private JLabel labelClassTitle;

    private boolean[][] editableCellRecords;

    public AnalysisJsonDartMappingTableDialog(MappingModel mappingModel) {
        this.mappingModel = mappingModel;

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

        // early get JSON model mapping table model data
        final Object[][] tableModelData = this.getMappingModelTableModelData(this.tablesColumnReflectable);
        DefaultTableModel tableModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) {
                return tableCellEditableRuleVerify(tableModelData, row, column);
            }

        };

        jsonAnalysisTable.setModel(tableModel);
        tableModel = (DefaultTableModel) jsonAnalysisTable.getModel();
        // set table column title
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

        // set table data
        for (Object[] data : tableModelData) {
            tableModel.addRow(data);
        }

        labelClassTitle.setText(JsonAnalysisTableKeys.ObjectPropertyTable.formatTableTitle(this.mappingModel.getClassName()));
    }

    private Object[][] getMappingModelTableModelData(JsonAnalysisTableKeys.MappingModelTableReflectable[] tablesColumnReflectable) {
        List<MappingModel> innerMappingModels = this.mappingModel.getInnerMappingModels();
        Object[][] tableData = new Object[innerMappingModels.size()][tablesColumnReflectable.length];

        // initial editable cell records
        this.editableCellRecords = new boolean[innerMappingModels.size()][tablesColumnReflectable.length];
        for (int i = 0; i < innerMappingModels.size(); i++) {
            MappingModel innerProperty = innerMappingModels.get(i);
            for (int j = 0; j < tablesColumnReflectable.length; j++) {
                // firstly all cell editable
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
        // table data convert to MappingModel
        List<MappingModel> innerMappingModels = this.mappingModel.getInnerMappingModels();
        for (int i = 0; i < innerMappingModels.size(); i++) {
            MappingModel innerProperty = innerMappingModels.get(i);
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
        // any false result will be set editable records
        if (column == 0) {
            this.editableCellRecords[row][column] = false;
            return false;
        }

        if (column == 2) {
            DartDataTypeEnum dartDataTypeEnum = (DartDataTypeEnum) tableModelData[row][column];
            if (DartDataTypeEnum.OBJECT == dartDataTypeEnum) {
                this.editableCellRecords[row][column] = false;
                return false;
            }
        }

        if (column == 5) {
            DartDataTypeEnum dartDataTypeEnum = (DartDataTypeEnum) tableModelData[row][2];
            if (DartDataTypeEnum.OBJECT != dartDataTypeEnum) {
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