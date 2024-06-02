package pers.chaos.jsondartserializable.domain.ui.components;

import com.intellij.openapi.ui.ComboBox;
import pers.chaos.jsondartserializable.domain.enums.DartDataType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public interface DartDataTypeComboBox {
    class DartDataTypeComboBoxCellEditor extends AbstractCellEditor implements TableCellEditor, ActionListener {
        private static final long serialVersionUID = 1L;

        protected DartDataType dartDataType;

        public DartDataTypeComboBoxCellEditor() {
        }

        @Override
        public Object getCellEditorValue() {
            return this.dartDataType;
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {
            if (value instanceof DartDataType) {
                this.dartDataType = (DartDataType) value;
            }

            JComboBox<DartDataType> dartDataTypeEnumComboBox = new ComboBox<>();

            for (DartDataType typeEnum : DartDataType.values()) {
                if (DartDataType.OBJECT != typeEnum) {
                    dartDataTypeEnumComboBox.addItem(typeEnum);
                }
            }

            dartDataTypeEnumComboBox.setSelectedItem(this.dartDataType);
            dartDataTypeEnumComboBox.addActionListener(this);

            return dartDataTypeEnumComboBox;

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            JComboBox<DartDataType> comboCountry = (JComboBox<DartDataType>) e.getSource();
            this.dartDataType = (DartDataType) comboCountry.getSelectedItem();
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

            if (value instanceof DartDataType) {
                DartDataType type = (DartDataType) value;
                setText(type.name());
            }

            setForeground(table.getForeground());
            setBackground(table.getBackground());

            return this;
        }
    }
}
