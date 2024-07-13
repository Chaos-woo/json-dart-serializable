package pers.chaos.jsondartserializable.domain.ui.views;

import pers.chaos.jsondartserializable.domain.enums.DartDataType;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeDataType;
import pers.chaos.jsondartserializable.domain.models.node.ModelNodeReflector;
import pers.chaos.jsondartserializable.domain.models.node.ModelNode;
import pers.chaos.jsondartserializable.domain.ui.components.DartDataTypeComboBox;
import pers.chaos.jsondartserializable.domain.ui.components.DartPropertyRequiredCheckBox;
import pers.chaos.jsondartserializable.domain.ui.models.UiConst;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ModelNodeTableDialog extends JDialog {
    private final ModelNode node;
    private final ModelNodeReflector.Key[] tablesColumnKeys;

    private JPanel contentPane;
    private JButton buttonConfirm;
    private JTable modelNodeTable;
    private JLabel labelClassTitle;

    private boolean[][] editableCellRecords;

    public ModelNodeTableDialog(ModelNode node) {
        this.node = node;

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

        this.tablesColumnKeys = ModelNodeReflector.Key.sortedByColumnIndexKeys();

        // 提前处理表格模型展示数据
        final Object[][] tableModelData = getModelNodeTableModelData();
        // 获取表格的数据模型及可编辑规则
        DefaultTableModel tableModel = new DefaultTableModel() {
            public boolean isCellEditable(int row, int column) {
                return handleTableCellEditableByRule(tableModelData, row, column);
            }

        };

        modelNodeTable.setModel(tableModel);
        tableModel = (DefaultTableModel) modelNodeTable.getModel();
        // 设置列标题
        Object[] columnTitle = Arrays.stream(this.tablesColumnKeys)
                .map(ModelNodeReflector.Key::getColumnName)
                .toArray();
        tableModel.setColumnIdentifiers(columnTitle);

        // 设置Dart数据类型ComboBox渲染类
        modelNodeTable.getColumnModel()
                .getColumn(ModelNodeReflector.Key.TM_DART_DATA_TYPE.getColumnIndex())
                .setCellEditor(new DartDataTypeComboBox.DartDataTypeComboBoxCellEditor());
        modelNodeTable.getColumnModel()
                .getColumn(ModelNodeReflector.Key.TM_DART_DATA_TYPE.getColumnIndex())
                .setCellRenderer(new DartDataTypeComboBox.DartDataTypeComboBoxRenderer());

        // 设置Dart是否为必填字段CheckBox渲染类
        modelNodeTable.getColumnModel()
                .getColumn(ModelNodeReflector.Key.TM_DART_PROPERTY_REQUIRED.getColumnIndex())
                .setCellEditor(new DartPropertyRequiredCheckBox.DartPropertyRequiredCheckBoxCellEditor());
        modelNodeTable.getColumnModel()
                .getColumn(ModelNodeReflector.Key.TM_DART_PROPERTY_REQUIRED.getColumnIndex())
                .setCellRenderer(new DartPropertyRequiredCheckBox.DartPropertyRequiredCheckBoxRenderer());

        // 设置表格数据
        for (Object[] data : tableModelData) {
            tableModel.addRow(data);
        }

        labelClassTitle.setText(String.format("Confirm 『%s』 Model Analysis", node.getOutputMeta().getClassname()));

        modelNodeTable.addMouseListener(new MouseAdapter() {
            private long lastClickTime = 0;

            @Override
            public void mouseClicked(MouseEvent e) {
                long currentTime = System.currentTimeMillis();
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    if (currentTime - lastClickTime < 500) { // 500 毫秒内的两次点击被视为双击
                        // 双击打开属性表格弹窗
                        int row = modelNodeTable.rowAtPoint(e.getPoint());
                        int column = modelNodeTable.columnAtPoint(e.getPoint());

                        if (ModelNodeReflector.Key.M_JSON_FIELD_NAME.equalsColumn(column)) {
                            DartDataType dartDataType = (DartDataType) tableModelData[row][ModelNodeReflector.Key.TM_DART_DATA_TYPE.getColumnIndex()];
                            if (DartDataType.OBJECT == dartDataType) {
                                String jsonFieldName = (String) modelNodeTable.getValueAt(row, column);
                                ModelNode filteredModelNode = node.getChildNodes().stream()
                                        .filter(modelNode -> Objects.equals(modelNode.getNodeMeta().getJsonFieldName(), jsonFieldName))
                                        .findFirst()
                                        .orElse(null);
                                if (Objects.isNull(filteredModelNode)) {
                                    super.mouseClicked(e);
                                } else {
                                    openModelNodePropertiesTableDialog(filteredModelNode);
                                }
                            }
                        } else {
                            super.mouseClicked(e);
                        }
                    }
                } else {
                    super.mouseClicked(e);
                }
                lastClickTime = currentTime;
            }
        });
    }

    private void openModelNodePropertiesTableDialog(ModelNode modelNode) {
        ModelNode realModelNode = modelNode;
        if (ModelNodeDataType.OBJECT_ARRAY == modelNode.getNodeMeta().getModelNodeDataType()) {
            realModelNode = modelNode.getChildNodes().get(0);
        }

        ModelNodeTableDialog dialog = new ModelNodeTableDialog(realModelNode);
        dialog.pack();
        dialog.setTitle("Child Model-Dart Property Table");
        Point location = this.getLocation();
        Dimension size = getSize();
        double movingX = location.getX() + (size.getWidth() / 2) - ((double) UiConst.AnalysisDialog.width / 2);
        double movingY = location.getY() + (size.getHeight() / 2) - ((double) UiConst.AnalysisDialog.height / 2);
        if (movingX < 0 || movingY < 0) {
            dialog.setLocation(location);
        } else {
            dialog.setLocation((int) movingX, (int) movingY);
        }
        dialog.setMinimumSize(new Dimension(UiConst.AnalysisDialog.width, UiConst.AnalysisDialog.height));
        dialog.setVisible(true);
    }

    private Object[][] getModelNodeTableModelData() {
        List<ModelNode> childNodes = node.getChildNodes();
        Object[][] tableData = new Object[childNodes.size()][tablesColumnKeys.length];

        // 初始化可编辑的数据记录
        editableCellRecords = new boolean[childNodes.size()][tablesColumnKeys.length];
        for (int i = 0; i < childNodes.size(); i++) {
            ModelNode childNode = childNodes.get(i);
            for (int j = 0; j < tablesColumnKeys.length; j++) {
                // 初始化时所有格子都可编辑
                this.editableCellRecords[i][j] = true;
                ModelNodeReflector.Key key = tablesColumnKeys[j];
                tableData[i][j] = key.reflectRead(childNode);
            }
        }
        return tableData;
    }

    private void onConfirm() {
        // 表数据转换为模型节点数据
        List<ModelNode> childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.size(); i++) {
            ModelNode childChildNode = childNodes.get(i);
            for (int j = 0; j < tablesColumnKeys.length; j++) {
                if (!this.editableCellRecords[i][j]) {
                    continue;
                }

                ModelNodeReflector.Key key = tablesColumnKeys[j];
                Object value = modelNodeTable.getModel().getValueAt(i, j);
                key.reflectWrite(childChildNode, value);
                childChildNode.handleMarkJsonKeyAnno();
            }
        }

        dispose();
    }

    /**
     * 根据规则处理单元格是否可编辑
     */
    private boolean handleTableCellEditableByRule(final Object[][] tableModelData, int row, int column) {
        // 可被编辑的JSON字段
        if (ModelNodeReflector.Key.M_JSON_FIELD_NAME.equalsColumn(column)) {
            this.editableCellRecords[row][column] = false;
            return false;
        }

        // 不可被编辑的JSON字段
        if (ModelNodeReflector.Key.M_JSON_DATA_TYPE.equalsColumn(column)) {
            this.editableCellRecords[row][column] = false;
            return false;
        }

        // dart对象类型不可被编辑
        if (ModelNodeReflector.Key.TM_DART_DATA_TYPE.equalsColumn(column)) {
            DartDataType dartDataType = (DartDataType) tableModelData[row][column];
            if (DartDataType.OBJECT == dartDataType) {
                this.editableCellRecords[row][column] = false;
                return false;
            }
        }

        // dart基础数据类型不可被编辑为对象类型
        if (ModelNodeReflector.Key.TM_DART_FILE_NAME.equalsColumn(column)) {
            DartDataType dartDataType = (DartDataType) tableModelData[row][ModelNodeReflector.Key.TM_DART_FILE_NAME.getColumnIndex()];
            if (DartDataType.OBJECT != dartDataType) {
                this.editableCellRecords[row][column] = false;
                return false;
            }
        }

        // 仅有对象类型的才可被编辑类名
        if (ModelNodeReflector.Key.TM_CLASS_NAME.equalsColumn(column)) {
            DartDataType dartDataType = (DartDataType) tableModelData[row][ModelNodeReflector.Key.TM_CLASS_NAME.getColumnIndex()];
            if (DartDataType.OBJECT != dartDataType) {
                this.editableCellRecords[row][column] = false;
                return false;
            }
        }

        // 如果不是dart基本数据类型，那不可编辑默认值数据
        if (ModelNodeReflector.Key.TM_DART_PROPERTY_DEFAULT_VALUE.equalsColumn(column)) {
            ModelNodeDataType modelNodeDataType = (ModelNodeDataType) tableModelData[row][ModelNodeReflector.Key.M_JSON_DATA_TYPE.getColumnIndex()];
            if (ModelNodeDataType.BASIS_DATA != modelNodeDataType) {
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
