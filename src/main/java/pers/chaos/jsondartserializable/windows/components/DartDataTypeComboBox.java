package pers.chaos.jsondartserializable.windows.components;

import com.intellij.openapi.ui.ComboBox;
import pers.chaos.jsondartserializable.core.json.enums.DartDataTypeEnum;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public interface DartDataTypeComboBox {
    class DartDataTypeComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private static final long serialVersionUID = 1L;

        protected DartDataTypeEnum dartDataTypeEnum;

        public DartDataTypeComboBoxCellEditor() {
        }

        @Override
        public Object getCellEditorValue() {
            return this.dartDataTypeEnum;
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {
            if (value instanceof DartDataTypeEnum) {
                this.dartDataTypeEnum = (DartDataTypeEnum) value;
            }

            JComboBox<DartDataTypeEnum> dartDataTypeEnumComboBox = new ComboBox<>();

            for (DartDataTypeEnum typeEnum : DartDataTypeEnum.values()) {
                if (DartDataTypeEnum.OBJECT != typeEnum) {
                    dartDataTypeEnumComboBox.addItem(typeEnum);
                }
            }

            dartDataTypeEnumComboBox.setSelectedItem(this.dartDataTypeEnum);
            dartDataTypeEnumComboBox.addActionListener(this);

            return dartDataTypeEnumComboBox;

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JComboBox<DartDataTypeEnum> comboCountry = (JComboBox<DartDataTypeEnum>) e.getSource();
            this.dartDataTypeEnum = (DartDataTypeEnum) comboCountry.getSelectedItem();
        }
    }

    class DartDataTypeComboBoxRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        public DartDataTypeComboBoxRenderer() {
            super();
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {

            if (value instanceof DartDataTypeEnum) {
                DartDataTypeEnum typeEnum = (DartDataTypeEnum) value;
                setText(typeEnum.name());
            }

            setForeground(table.getForeground());
            setBackground(table.getBackground());

            return this;
        }
    }
}
