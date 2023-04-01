package pers.chaos.jsondartserializable.windows;

import org.apache.commons.collections.CollectionUtils;
import pers.chaos.jsondartserializable.core.json.JsonDartAnalysisMapping;
import pers.chaos.jsondartserializable.core.json.MappingModel;
import pers.chaos.jsondartserializable.core.json.enums.JsonTypeEnum;
import pers.chaos.jsondartserializable.windows.components.TreeNodeCellRenderer;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;

public class JsonObjectTreeDialog extends JDialog {

    private final JsonDartAnalysisMapping analysisMapping;

    private JPanel contentPane;
    private JButton buttonOK;
    private JTree jsonTree;

    public JsonObjectTreeDialog(JsonDartAnalysisMapping analysisMapping) {
        this.analysisMapping = analysisMapping;

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

        // build JSON object tree
        DefaultMutableTreeNode root = buildJsonObjectTreeNode();
        jsonTree.setModel(new DefaultTreeModel(root));

        // close JTree double click expand or shrink node
        jsonTree.setToggleClickCount(0);

        // set node icon by diff json type
        jsonTree.setCellRenderer(new TreeNodeCellRenderer());

        jsonTree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode) jsonTree.getLastSelectedPathComponent();
                    if (node == null) {
                        return;
                    }

                    Object userObject = node.getUserObject();
                    confirmObjectPropertiesInNewDialog(((MappingModelTreeNode) userObject).getModel());
                }
            }
        });
    }

    private void confirmObjectPropertiesInNewDialog(MappingModel mappingModel) {
        MappingModel effectiveModel = mappingModel;
        if (JsonTypeEnum.OBJECT_ARRAY == mappingModel.getJsonTypeEnum()) {
            effectiveModel = mappingModel.getInnerMappingModels().get(0);
        }

        AnalysisJsonDartMappingTableDialog dialog = new AnalysisJsonDartMappingTableDialog(effectiveModel);
        dialog.pack();
        dialog.setTitle("『OBJECT』's Mapping Model Table");
        dialog.setLocation(this.getLocation());
        dialog.setMinimumSize(new Dimension(1200, 500));
        dialog.setVisible(true);
    }

    private DefaultMutableTreeNode buildJsonObjectTreeNode() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        MappingModelTreeNode mappingModelTreeNode = new MappingModelTreeNode(this.analysisMapping.getRootMappingModel());
        root.setUserObject(mappingModelTreeNode);

        recursiveBuildJsonChildTreeNode(root, this.analysisMapping.getRootMappingModel().getInnerMappingModels());

        return root;
    }

    private void recursiveBuildJsonChildTreeNode(DefaultMutableTreeNode parent, List<MappingModel> childMappingModels) {
        if (CollectionUtils.isEmpty(childMappingModels)) {
            return;
        }

        List<MappingModel> objectChildModels = childMappingModels.stream()
                .filter(model -> !model.isBasisJsonType())
                .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(objectChildModels)) {
            return;
        }

        for (MappingModel objectChildModel : objectChildModels) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode();
            MappingModelTreeNode mappingModelTreeNode = new MappingModelTreeNode(objectChildModel);
            node.setUserObject(mappingModelTreeNode);

            parent.add(node);
            recursiveBuildJsonChildTreeNode(node, objectChildModel.getInnerMappingModels());
        }
    }

    private void onOK() {
        dispose();
    }

    private void onCancel() {
        dispose();
    }
}
