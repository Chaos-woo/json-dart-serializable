package pers.chaos.jsondartserializable.domain.ui.components;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

public interface DartPropertyRequiredCheckBox {
    class DartPropertyRequiredCheckBoxCellEditor extends AbstractCellEditor implements TableCellEditor {
        private static final long serialVersionUID = 1L;
        protected JCheckBox checkBox;

        public DartPropertyRequiredCheckBoxCellEditor() {
            checkBox = new JCheckBox();
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Object getCellEditorValue() {
            return checkBox.isSelected();
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table,
                Object value,
                boolean isSelected,
                int row,
                int column) {
            checkBox.setSelected((Boolean) value);

            return checkBox;
        }
    }

    class DartPropertyRequiredCheckBoxRenderer extends JCheckBox implements TableCellRenderer {
        private static final long serialVersionUID = 1L;

        public DartPropertyRequiredCheckBoxRenderer() {
            super();
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column) {
            if (value instanceof Boolean) {
                setSelected((Boolean) value);
                setForeground(table.getForeground());
                setBackground(table.getBackground());

            }

            return this;
        }
    }
}
