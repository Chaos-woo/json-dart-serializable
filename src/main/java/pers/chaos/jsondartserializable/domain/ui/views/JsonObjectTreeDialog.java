package pers.chaos.jsondartserializable.domain.ui.views;

import org.apache.commons.collections.CollectionUtils;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeDataType;
import pers.chaos.jsondartserializable.domain.models.node.ModelNode;
import pers.chaos.jsondartserializable.domain.service.ModelNodesMgr;
import pers.chaos.jsondartserializable.domain.ui.components.TreeNodeCellRenderer;
import pers.chaos.jsondartserializable.domain.ui.models.ModelTreeNode;
import pers.chaos.jsondartserializable.domain.ui.models.UiConst;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;

public class JsonObjectTreeDialog extends JDialog {
    private final ModelNodesMgr mgr;

    private JPanel contentPane;
    private JButton buttonOK;
    private JTree modelNodeTree;

    public JsonObjectTreeDialog(ModelNodesMgr mgr) {
        this.mgr = mgr;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

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

        // 构建Json对象树
        DefaultMutableTreeNode treeRootNode = buildJsonObjectTreeNode();
        modelNodeTree.setModel(new DefaultTreeModel(treeRootNode));

        // 关闭双击展开或收缩节点
        modelNodeTree.setToggleClickCount(0);

        // 设置节点展示文案和图标
        modelNodeTree.setCellRenderer(new TreeNodeCellRenderer());

        modelNodeTree.addMouseListener(new MouseAdapter() {
            private long lastClickTime = 0;

            public void mouseClicked(MouseEvent e) {
                long currentTime = System.currentTimeMillis();
                if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
                    if (currentTime - lastClickTime < 500) { // 500 毫秒内的两次点击被视为双击
                        // 双击打开属性表格弹窗
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) modelNodeTree.getLastSelectedPathComponent();
                        if (node == null) {
                            return;
                        }

                        Object userObject = node.getUserObject();
                        openModelNodePropertiesTableDialog(((ModelTreeNode) userObject).getNode());
                    }
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
        dialog.setTitle("Json Model-Dart Property Table");
        Point location = this.getLocation();
        double movingX = location.getX() - ((double) UiConst.AnalysisDialog.width / 4);
        if (movingX < 0) {
            dialog.setLocation(location);
        } else {
            dialog.setLocation((int) movingX, (int) location.getLocation().getY());
        }
        dialog.setMinimumSize(new Dimension(UiConst.AnalysisDialog.width, UiConst.AnalysisDialog.height));
        dialog.setVisible(true);
    }

    private DefaultMutableTreeNode buildJsonObjectTreeNode() {
        DefaultMutableTreeNode rootTreeNode = new DefaultMutableTreeNode();
        ModelTreeNode treeVO = new ModelTreeNode(mgr.getRootNode());
        rootTreeNode.setUserObject(treeVO);

        recursiveBuildJsonChildTreeNode(rootTreeNode, mgr.getRootNode().getChildNodes());

        return rootTreeNode;
    }

    private void recursiveBuildJsonChildTreeNode(DefaultMutableTreeNode parent, List<ModelNode> childNodes) {
        if (CollectionUtils.isEmpty(childNodes)) {
            return;
        }

        List<ModelNode> objectChildNodes = childNodes.stream()
                .filter(node -> !node.getNodeMeta().isBasisModelNodeDataType())
                .collect(Collectors.toList());

        for (ModelNode objectNode : objectChildNodes) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode();
            ModelTreeNode treeVO = new ModelTreeNode(objectNode);
            node.setUserObject(treeVO);

            parent.add(node);
            recursiveBuildJsonChildTreeNode(node, objectNode.getChildNodes());
        }
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
