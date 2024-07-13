package pers.chaos.jsondartserializable.domain.ui.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pers.chaos.jsondartserializable.domain.models.forgenerated.ModelGenUserOption;
import pers.chaos.jsondartserializable.domain.models.nodedata.ModelNodeMeta;
import pers.chaos.jsondartserializable.domain.models.nodedata.ModelOutputMeta;
import pers.chaos.jsondartserializable.domain.models.node.ModelNode;
import pers.chaos.jsondartserializable.domain.service.DartGenOption;
import pers.chaos.jsondartserializable.domain.util.JsonNodeUtil;
import pers.chaos.jsondartserializable.domain.service.ModelNodesMgr;
import pers.chaos.jsondartserializable.domain.ui.models.UserInputData;
import pers.chaos.jsondartserializable.domain.ui.models.UiConst;
import pers.chaos.jsondartserializable.domain.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.net.URI;
import java.util.Objects;

public class JsonStringInputDialog extends JDialog {
    private static final Logger log = LoggerFactory.getLogger(JsonStringInputDialog.class);
    private final AnActionEvent anActionEvent;

    private ModelNodesMgr mgr;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonFormat;
    private JScrollPane scrollPane;
    private JLabel labelError;
    private JTextField textFieldClassName;
    private JTextArea textAreaJsonString;
    private JButton buttonPreviewEdit;
    private JButton buttonClearEdited;
    private JTextField textFieldClassDescription;
    private JCheckBox realtimeDefaultValCheckBox;
    private JCheckBox allClassIntoSingleCheckBox;
    private JCheckBox extensionJsonSyntaxcheckBox;
    private JLabel syntaxFaq;

    public JsonStringInputDialog(AnActionEvent anActionEvent) {
        this.anActionEvent = anActionEvent;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> {
            try {
                onGenerate();
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        });

        buttonCancel.addActionListener(e -> onCancel());

        buttonFormat.addActionListener(e -> onFormat());

        buttonPreviewEdit.addActionListener(e -> {
            try {
                onPreviewEdit();
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        });

        buttonClearEdited.addActionListener(e -> {
            mgr = null;

            Messages.showInfoMessage("Cleared all edited data", "Clear Success");
        });

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

        textFieldClassName.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (Objects.nonNull(mgr)) {
                    String rootClassName = ((JTextField) e.getSource()).getText();
                    mgr.setRootClassName(rootClassName);
                    ModelNode rootNode = mgr.getRootNode();
                    ModelNodeMeta meta = rootNode.getNodeMeta();
                    meta.setJsonFieldName(rootClassName);
                    ModelOutputMeta targetMeta = rootNode.getOutputMeta();
                    targetMeta.setClassname(DartGenOption.NameGen.CLASS.gen(rootClassName));
                    targetMeta.setFilename(DartGenOption.NameGen.FILE.gen(rootClassName));
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });

        textFieldClassDescription.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (Objects.nonNull(mgr)) {
                    String rootClassRemark = ((JTextField) e.getSource()).getText();
                    mgr.setUserInputRootClassRemark(rootClassRemark);
                    mgr.getRootNode().getOutputMeta().setRemark(rootClassRemark);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });

        syntaxFaq.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                openFaqDialog();
            }
        });
    }

    private void openFaqDialog() {
        String link = "https://github.com/Chaos-woo/json-dart-serializable";
        CommonDialog alertDialog = new CommonDialog(
                "Please copy the URI: " + link + " and open it or click OK button to skip github to read the JSON extension syntax guide, " +
                        "but be aware that the attempt to open it may fail due to security reasons.", () -> {
            try {
                // 检查Desktop类是否支持打开URI
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    // 将链接转换为URI
                    URI uri = new URI(link);
                    // 打开URI
                    desktop.browse(uri);
                }
            } catch (Exception ex) {
                // do nothing
            }
        });
        alertDialog.setTitle("JSON extension syntax guide");
        Point location = getLocation();
        double movingX = location.getX() + ((double) UiConst.AnalysisDialog.width / 4);
        if (movingX < 0) {
            alertDialog.setLocation(location);
        } else {
            alertDialog.setLocation((int) movingX, (int) location.getLocation().getY());
        }
        alertDialog.setMinimumSize(new Dimension(400, 500));
        alertDialog.pack();
        alertDialog.setVisible(true);
    }

    private void onPreviewEdit() throws JsonProcessingException {
        if (Objects.isNull(mgr)) {
            ModelNodesMgr nodeMgr = getModelNodeMgrByUserJsonString();
            if (Objects.isNull(nodeMgr)) {
                return;
            } else {
                nodeMgr.startAnalysisDataAndBuildModelNode();
                mgr = nodeMgr;
            }
        }

        // 存在已经解析过的根节点时，保留用户已填写的数据，
        // 删除Json中不存在的字段，增加Json中新增的字段解析模型
        UserInputData inputData = getUserInputData();
        if (Objects.isNull(inputData)) {
            return;
        }
        mgr.rebuildRootModelNode(inputData);

        // 根节点的子节点是否不存在对象型子节点
        boolean isRootNodeNotHasObjectChildNode = mgr.getRootNode().getChildNodes()
                .stream()
                .allMatch(modelNode -> modelNode.getNodeMeta().isBasisModelNodeDataType());
        if (isRootNodeNotHasObjectChildNode) {
            // 根节点无对象/对象数组子节点时直接展示表格形式的属性表
            ModelNodeTableDialog dialog = new ModelNodeTableDialog(mgr.getRootNode());
            dialog.pack();
            dialog.setTitle("Json Model-Dart Property Table");
            Point location = getLocation();
            double movingX = location.getX() - ((double) UiConst.AnalysisDialog.width / 4);
            if (movingX < 0) {
                dialog.setLocation(location);
            } else {
                dialog.setLocation((int) movingX, (int) location.getLocation().getY());
            }
            dialog.setMinimumSize(new Dimension(UiConst.AnalysisDialog.width, UiConst.AnalysisDialog.height));
            dialog.setVisible(true);
        } else {
            // 根节点存在对象/对象数组子节点时展示根节点下的对象树
            JsonObjectTreeDialog dialog = new JsonObjectTreeDialog(mgr);
            dialog.pack();
            dialog.setTitle("Root Model Tree");
            Point location = getLocation();
            double movingX = location.getX() + ((double) UiConst.AnalysisDialog.width / 4);
            if (movingX < 0) {
                dialog.setLocation(location);
            } else {
                dialog.setLocation((int) movingX, (int) location.getLocation().getY());
            }
            dialog.setMinimumSize(new Dimension(400, 500));
            dialog.setVisible(true);
        }
    }

    private UserInputData getUserInputData() {
        String rootClassName = textFieldClassName.getText().trim();
        String rootClassRemark = textFieldClassDescription.getText();
        String jsonString = textAreaJsonString.getText().trim();
        if (StringUtil.isEmpty(rootClassName) || StringUtil.isEmpty(jsonString)) {
            showErrorLabel("Empty root class name or invalid Json string!!");
            return null;
        }

        ModelGenUserOption userOption = new ModelGenUserOption();
        userOption.setEnableRealtimeJsonDefaultValueAnalysis(realtimeDefaultValCheckBox.isSelected());
        userOption.setEnableAllClassGeneratedIntoSingleFile(allClassIntoSingleCheckBox.isSelected());
        userOption.setEnableCustomJsonSyntax(extensionJsonSyntaxcheckBox.isSelected());

        return UserInputData.builder()
                .className(rootClassName)
                .remark(rootClassRemark)
                .jsonString(jsonString)
                .userOption(userOption)
                .build();
    }

    private ModelNodesMgr getModelNodeMgrByUserJsonString() {
        hideErrorLabel();
        UserInputData inputData = getUserInputData();

        try {
            return ModelNodesMgr.builder()
                    .jsonString(inputData.getJsonString())
                    .rootClassName(inputData.getRootClassName())
                    .rootClassRemark(inputData.getRootClassRemark())
                    .userOption(inputData.getUserOption())
                    .build();
        } catch (Exception e) {
            log.error("Build model node error", e);
            showJsonFormatErrorLabel();
            return null;
        }
    }

    private void onFormat() {
        hideErrorLabel();
        try {
            // JSON字符串美化
            String prettyJsonString = JsonNodeUtil.getPrettyString(textAreaJsonString.getText().trim());
            textAreaJsonString.setText(prettyJsonString);
        } catch (Exception e) {
            showJsonFormatErrorLabel();
        }
    }

    private void onCancel() {
        dispose();
    }

    private void showJsonFormatErrorLabel() {
        labelError.setText("Invalid Json string!!");
        labelError.setForeground(JBColor.red);
    }

    private void showErrorLabel(String tip) {
        labelError.setText(tip);
        labelError.setForeground(JBColor.blue);
    }

    private void hideErrorLabel() {
        labelError.setText(null);
    }

    private void onGenerate() throws JsonProcessingException {
        if (Objects.isNull(mgr)) {
            ModelNodesMgr nodeMgr = getModelNodeMgrByUserJsonString();
            if (Objects.isNull(nodeMgr)) {
                return;
            } else {
                nodeMgr.startAnalysisDataAndBuildModelNode();
                mgr = nodeMgr;
            }
        }

        UserInputData inputData = getUserInputData();
        if (Objects.isNull(inputData)) {
            return;
        }
        mgr.rebuildRootModelNode(inputData);

        Project project = anActionEvent.getProject();
        if (Objects.isNull(project)) {
            return;
        }

        // 获取用户当前focus的文件
        VirtualFile parent = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
        if (parent != null && parent.isDirectory()) {
            ModelNode rootNode = mgr.getRootNode();
            VirtualFile child = parent.findChild(rootNode.getOutputMeta().getFilename() + ".dart");
            if (Objects.nonNull(child)) {
                Messages.showErrorDialog("Exist " + rootNode.getOutputMeta().getFilename() + ".dart file. Please edit root dart class name 『 "
                        + rootNode.getOutputMeta().getClassname() + "』", "Json to Dart Convert Fail!!");
                return;
            }
        }

        try {
            // 生成dart文件
            output(parent, project);
        } catch (IOException e) {
            showErrorLabel("Json to Dart convert fail!!");
            NotificationUtil.show(this.anActionEvent.getProject(), "Json to Dart convert fail.", "Check JSON data string, please.", NotificationType.WARNING);
            return;
        }
        dispose();
    }

    private void output(VirtualFile parent, Project project) throws IOException {
        if (parent != null && parent.isDirectory()) {
            // 开始生成文件
            mgr.output(parent, project);

            // 刷新IntelliJ文件系统
            parent.refresh(false, true);
            NotificationUtil.show(project,
                    "Json to Dart convert success!!",
                    mgr.getRootNode().getOutputMeta().getFilename() + ".dart generated.",
                    NotificationType.INFORMATION);
        }
    }
}
