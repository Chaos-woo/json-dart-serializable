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
import pers.chaos.jsondartserializable.domain.models.*;
import pers.chaos.jsondartserializable.domain.service.JsonNodeAnalyser;
import pers.chaos.jsondartserializable.domain.ui.models.InputDataVO;
import pers.chaos.jsondartserializable.domain.ui.models.UiConst;
import pers.chaos.jsondartserializable.domain.util.DartUtil;
import pers.chaos.jsondartserializable.domain.util.NotificationUtil;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Objects;

public class JsonStringInputDialog extends JDialog {
    private static final Logger log = LoggerFactory.getLogger(JsonStringInputDialog.class);
    private final AnActionEvent anActionEvent;

    private ModelNodeMgr mgr;

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

            Messages.showInfoMessage("Your input JSON analysis mapping model cleared", "Clear Success");
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
                    ModelNodeMeta meta = rootNode.getMeta();
                    meta.setJsonFieldName(rootClassName);
                    ModelTargetMeta targetMeta = rootNode.getTargetMeta();
                    targetMeta.setClassName(DartUtil.toClassName(rootClassName));
                    targetMeta.setFilename(DartUtil.toFileName(rootClassName));
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
                    mgr.setRootClassRemark(rootClassRemark);
                    mgr.getRootNode().getTargetMeta().setRemark(rootClassRemark);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
    }

    private void onPreviewEdit() throws JsonProcessingException {
        if (Objects.isNull(mgr)) {
            ModelNodeMgr nodeMgr = getModelNodeMgrByUserJsonString();
            if (Objects.isNull(nodeMgr)) {
                return;
            } else {
                nodeMgr.analysis();
                mgr = nodeMgr;
            }
        }

        // 存在已经解析过的根节点时，保留用户已填写的数据，
        // 删除Json中不存在的字段，增加Json中新增的字段解析模型
        InputDataVO inputData = getUserInputData();
        if (Objects.isNull(inputData)) {
            return;
        }
        mgr.rebuildRootModelNode(inputData);

        // 根节点的子节点是否不存在对象型子节点
        boolean isRootNodeNotHasObjectChildNode = mgr.getRootNode().getChildNodes()
                .stream()
                .allMatch(modelNode -> modelNode.getMeta().isBasisModelNodeDataType());
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

    private InputDataVO getUserInputData() {
        String rootClassName = textFieldClassName.getText().trim();
        String rootClassRemark = textFieldClassDescription.getText();
        String jsonString = textAreaJsonString.getText().trim();
        if (StringUtil.isEmpty(rootClassName) || StringUtil.isEmpty(jsonString)) {
            showErrorLabel("Empty root class name or invalid Json string!!");
            return null;
        }

        ModelGenUserConfig userConfig = new ModelGenUserConfig();
        userConfig.setEnableRealtimeJsonDefaultValueAnalysis(realtimeDefaultValCheckBox.isSelected());
        userConfig.setEnableAllClassGeneratedIntoSingleFile(allClassIntoSingleCheckBox.isSelected());

        return InputDataVO.builder()
                .className(rootClassName)
                .remark(rootClassRemark)
                .jsonString(jsonString)
                .userConfig(userConfig)
                .build();
    }

    private ModelNodeMgr getModelNodeMgrByUserJsonString() {
        hideErrorLabel();
        InputDataVO inputData = getUserInputData();

        try {
            return ModelNodeMgr.builder()
                    .jsonString(inputData.getJsonString())
                    .rootClassName(inputData.getRootClassName())
                    .rootClassRemark(inputData.getRootClassRemark())
                    .userConfig(inputData.getUserConfig())
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
            String prettyJsonString = JsonNodeAnalyser.getPrettyString(textAreaJsonString.getText().trim());
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
            ModelNodeMgr nodeMgr = getModelNodeMgrByUserJsonString();
            if (Objects.isNull(nodeMgr)) {
                return;
            } else {
                nodeMgr.analysis();
                mgr = nodeMgr;
            }
        }

        InputDataVO inputData = getUserInputData();
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
            VirtualFile child = parent.findChild(rootNode.getTargetMeta().getFilename() + ".dart");
            if (Objects.nonNull(child)) {
                Messages.showErrorDialog("Exist " + rootNode.getTargetMeta().getFilename() + ".dart file. Please edit root dart class name 『 "
                        + rootNode.getTargetMeta().getClassName() + "』", "Json to Dart Convert Fail!!");
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
                    mgr.getRootNode().getTargetMeta().getFilename() + ".dart generated.",
                    NotificationType.INFORMATION);
        }
    }
}
