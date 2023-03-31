package pers.chaos.jsondartserializable.windows.components;

import pers.chaos.jsondartserializable.core.json.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.windows.MappingModelTreeNode;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.IOException;
import java.util.Objects;

public class TreeNodeCellRenderer extends DefaultTreeCellRenderer {
    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        String normalImage = "assets/object.png";
        int scale = 20;
        if (JsonTypeEnum.OBJECT_ARRAY == ((MappingModelTreeNode) node.getUserObject()).getModel().getJsonTypeEnum()) {
            normalImage = "assets/object_array.png";
            scale = 18;
        }

        JLabel label = new JLabel();

        try {
            Image img = ImageIO.read(Objects.requireNonNull(TreeNodeCellRenderer.class.getClassLoader().getResource(normalImage)));
            img = img.getScaledInstance(scale, scale , Image.SCALE_SMOOTH);
            ImageIcon icon = new ImageIcon(img);
            label.setIcon(icon);
            label.setDisabledIcon(icon);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        label.setText(node.getUserObject().toString());
        return label;
    }
}
