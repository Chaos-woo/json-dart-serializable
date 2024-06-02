package pers.chaos.jsondartserializable.domain.ui.views;

import org.apache.commons.collections.CollectionUtils;
import pers.chaos.jsondartserializable.domain.enums.ModelNodeDataType;
import pers.chaos.jsondartserializable.domain.models.ModelNode;
import pers.chaos.jsondartserializable.domain.models.ModelNodeMgr;
import pers.chaos.jsondartserializable.domain.ui.components.TreeNodeCellRenderer;
import pers.chaos.jsondartserializable.domain.ui.models.ModelNodeTreeVO;
import pers.chaos.jsondartserializable.domain.ui.models.UiConst;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;

public class JsonObjectTreeDialog extends JDialog {
    private final ModelNodeMgr mgr;

    private JPanel contentPane;
    private JButton buttonOK;
    private JTree jsonTree;

    public JsonObjectTreeDialog(ModelNodeMgr mgr) {
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
        DefaultMutableTreeNode root = buildJsonObjectTreeNode();
        jsonTree.setModel(new DefaultTreeModel(root));

        // 关闭双击展开或收缩节点
        jsonTree.setToggleClickCount(0);

        // 设置节点展示文案和图标
        jsonTree.setCellRenderer(new TreeNodeCellRenderer());

        jsonTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    // 双击打开属性表格弹窗
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) jsonTree.getLastSelectedPathComponent();
                    if (node == null) {
                        return;
                    }

                    Object userObject = node.getUserObject();
                    openModelNodePropertiesTableDialog(((ModelNodeTreeVO) userObject).getNode());
                }
            }
        });
    }

    private void openModelNodePropertiesTableDialog(ModelNode modelNode) {
        ModelNode effectiveModel = modelNode;
        if (ModelNodeDataType.OBJECT_ARRAY == modelNode.getMeta().getModelNodeDataType()) {
            effectiveModel = modelNode.getChildNodes().get(0);
        }

        ModelNodeTableDialog dialog = new ModelNodeTableDialog(effectiveModel);
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
        ModelNodeTreeVO treeVO = new ModelNodeTreeVO(mgr.getRootNode());
        rootTreeNode.setUserObject(treeVO);

        recursiveBuildJsonChildTreeNode(rootTreeNode, mgr.getRootNode().getChildNodes());

        return rootTreeNode;
    }

    private void recursiveBuildJsonChildTreeNode(DefaultMutableTreeNode parent, List<ModelNode> childNodes) {
        if (CollectionUtils.isEmpty(childNodes)) {
            return;
        }

        List<ModelNode> objectChildNodes = childNodes.stream()
                .filter(node -> !node.getMeta().isBasisModelNodeDataType())
                .collect(Collectors.toList());

        for (ModelNode objectNode : objectChildNodes) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode();
            ModelNodeTreeVO treeVO = new ModelNodeTreeVO(objectNode);
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
