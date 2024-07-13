package pers.chaos.jsondartserializable.domain.ui.components;

import pers.chaos.jsondartserializable.domain.enums.ModelNodeDataType;
import pers.chaos.jsondartserializable.domain.ui.models.ModelTreeNode;

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
        String imgPath = "assets/object.png";
        int scale = 20;
        if (ModelNodeDataType.OBJECT_ARRAY == ((ModelTreeNode) node.getUserObject()).getNode().getNodeMeta().getModelNodeDataType()) {
            imgPath = "assets/object_array.png";
        }

        JLabel label = new JLabel();

        try {
            Image img = ImageIO.read(Objects.requireNonNull(TreeNodeCellRenderer.class.getClassLoader().getResource(imgPath)));
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
