package pers.chaos.jsondartserializable.windows;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import pers.chaos.jsondartserializable.core.json.JsonAnalyser;
import pers.chaos.jsondartserializable.core.json.JsonDartAnalysis;
import pers.chaos.jsondartserializable.core.json.MappingModelNode;
import pers.chaos.jsondartserializable.utils.DartClassFileUtils;
import pers.chaos.jsondartserializable.utils.NotificationUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Objects;

public class JsonStringInputDialog extends JDialog {
    private final AnActionEvent anActionEvent;

    private JsonDartAnalysis jsonAnalysis;

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

        buttonOK.addActionListener(e -> onGenerate());

        buttonCancel.addActionListener(e -> onCancel());

        buttonFormat.addActionListener(e -> onFormat());

        buttonPreviewEdit.addActionListener(e -> onPreviewEdit());

        buttonClearEdited.addActionListener(e -> {
            this.jsonAnalysis = null;

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
                if (Objects.nonNull(jsonAnalysis)) {
                    JTextField textField = (JTextField) e.getSource();
                    String newRootClassName = textField.getText();
                    MappingModelNode rootMappingModelNode = jsonAnalysis.getRootMappingModel();
                    rootMappingModelNode.setClassName(newRootClassName);
                    rootMappingModelNode.setJsonFieldName(newRootClassName);
                    rootMappingModelNode.setDartFileName(DartClassFileUtils.getDartFileNameByClassName(newRootClassName));
                }
            }
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}
        });

        textFieldClassDescription.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (Objects.nonNull(jsonAnalysis)) {
                    JTextField textField = (JTextField) e.getSource();
                    String newRootModelDescription = textField.getText();
                    jsonAnalysis.getRootMappingModel().setDescription(newRootModelDescription);
                }
            }

            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {}
        });
    }

    private void onPreviewEdit() {
        if (Objects.isNull(this.jsonAnalysis)) {
            JsonDartAnalysis jsonAnalysis = inputJsonStringAnalysis();
            if (Objects.isNull(jsonAnalysis)) {
                return;
            } else {
                this.jsonAnalysis = jsonAnalysis;
            }
        }

        // 已经存在解析过的根节点时，保留一些用户之前已填写的数据，
        // 删除JSON中不存在的字段，增加JSON中新增的字段解析模型
        JsonAnalyser.AnalysisRebuildData simpleAnalysisData = getSimpleInputJsonAnalysisData();
        if (Objects.isNull(simpleAnalysisData)) {
            return;
        }
        this.jsonAnalysis.rebuildRootMappingModel(simpleAnalysisData);

        boolean isRootObjectNodeHasNoChildNodes = this.jsonAnalysis.getRootMappingModel().getChildModelNodes().stream()
                .allMatch(MappingModelNode::isBasisJsonType);

        if (isRootObjectNodeHasNoChildNodes) {
            // 根节点无对象/对象数组子节点时直接展示表格形式的属性表
            AnalysisJsonDartMappingTableDialog dialog =
                    new AnalysisJsonDartMappingTableDialog(this.jsonAnalysis.getRootMappingModel());
            dialog.pack();
            dialog.setTitle("JSON mapping Dart objects table");
            Point location = this.getLocation();
            double movingX = location.getX() - ((double) Consts.AnalysisJsonDialog.WIDTH_WINDOW / 4);
            if (movingX < 0) {
                dialog.setLocation(location);
            } else {
                dialog.setLocation((int) movingX, (int) location.getLocation().getY());
            }
            dialog.setMinimumSize(new Dimension(Consts.AnalysisJsonDialog.WIDTH_WINDOW, Consts.AnalysisJsonDialog.HEIGHT_WINDOW));
            dialog.setVisible(true);
        } else {
            // 根节点存在对象/对象数组子节点时展示根节点下的对象树
            JsonObjectTreeDialog dialog = new JsonObjectTreeDialog(this.jsonAnalysis);
            dialog.pack();
            dialog.setTitle("Root mapping objects tree");
            dialog.setLocation(this.getLocation());
            dialog.setMinimumSize(new Dimension(400, 500));
            dialog.setVisible(true);
        }
    }

    private JsonAnalyser.AnalysisRebuildData  getSimpleInputJsonAnalysisData() {
        String userInputRootClassName = textFieldClassName.getText().trim();
        String jsonString = textAreaJsonString.getText().trim();
        if (StringUtil.isEmpty(userInputRootClassName) || StringUtil.isEmpty(jsonString)) {
            showAnalysisErrorTip("Empty root class name or invalid JSON string!!");
        }

        UserAdvanceConfiguration userAdvanceConfiguration = new UserAdvanceConfiguration();
        userAdvanceConfiguration.setEnableRealtimeJsonDefaultValueAnalysis(realtimeDefaultValCheckBox.isSelected());
        userAdvanceConfiguration.setEnableAllClassGeneratedIntoSingleFile(allClassIntoSingleCheckBox.isSelected());

        try {
            return new JsonAnalyser.AnalysisRebuildData(userInputRootClassName, jsonString, userAdvanceConfiguration);
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            showJsonFormatErrorTip();
            return null;
        }
    }

    private JsonDartAnalysis inputJsonStringAnalysis() {
        hideJsonAnalysisErrorTip();

        String userInputRootClassName = textFieldClassName.getText().trim();
        String jsonString = textAreaJsonString.getText().trim();
        if (StringUtil.isEmpty(userInputRootClassName) || StringUtil.isEmpty(jsonString)) {
            showAnalysisErrorTip("Empty root class name or invalid JSON string!!");
        }

        UserAdvanceConfiguration userAdvanceConfiguration = new UserAdvanceConfiguration();
        userAdvanceConfiguration.setEnableRealtimeJsonDefaultValueAnalysis(realtimeDefaultValCheckBox.isSelected());
        userAdvanceConfiguration.setEnableAllClassGeneratedIntoSingleFile(allClassIntoSingleCheckBox.isSelected());

        JsonDartAnalysis jsonAnalysis;
        try {
            // 分析JSON并生成模型
            jsonAnalysis = JsonAnalyser.analysis(userInputRootClassName, jsonString, userAdvanceConfiguration);
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            showJsonFormatErrorTip();
            return null;
        }
        return jsonAnalysis;
    }

    private void onFormat() {
        hideJsonAnalysisErrorTip();
        try {
            // JSON字符串美化
            String prettyJson = JsonAnalyser.getPrettyString(textAreaJsonString.getText().trim());
            textAreaJsonString.setText(prettyJson);
        } catch (Exception e) {
            showJsonFormatErrorTip();
        }
    }

    private void onCancel() {
        dispose();
    }

    private void showJsonFormatErrorTip() {
        labelError.setText("Invalid JSON string!!");
        labelError.setForeground(JBColor.red);
    }

    private void showAnalysisErrorTip(String tip) {
        labelError.setText(tip);
        labelError.setForeground(JBColor.blue);
    }

    private void hideJsonAnalysisErrorTip() {
        labelError.setText(null);
    }

    private void onGenerate() {
        if (Objects.isNull(this.jsonAnalysis)) {
            JsonDartAnalysis jsonAnalysis = inputJsonStringAnalysis();
            if (Objects.isNull(jsonAnalysis)) {
                return;
            } else {
                this.jsonAnalysis = jsonAnalysis;
            }
        }

        // 已经存在解析过的根节点时，保留一些用户之前已填写的数据，
        // 删除JSON中不存在的字段，增加JSON中新增的字段解析模型
        JsonAnalyser.AnalysisRebuildData simpleAnalysisData = getSimpleInputJsonAnalysisData();
        if (Objects.isNull(simpleAnalysisData)) {
            return;
        }
        this.jsonAnalysis.rebuildRootMappingModel(simpleAnalysisData);

        String classDesc = textFieldClassDescription.getText();
        MappingModelNode rootMappingModelNode = this.jsonAnalysis.getRootMappingModel();
        if (StringUtils.isNoneBlank(classDesc)) {
            rootMappingModelNode.setDescription(classDesc);
        }

        Project project = anActionEvent.getProject();
        if (Objects.isNull(project)) {
            return;
        }

        // 获取用户当前focus的文件
        VirtualFile parent = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
        if (parent != null && parent.isDirectory()) {
            VirtualFile child = parent.findChild(rootMappingModelNode.getDartFileName() + ".dart");
            if (Objects.nonNull(child)) {
                Messages.showErrorDialog("Exist " +
                        rootMappingModelNode.getDartFileName()
                                + ".dart file. Please edit root dart class name 『 "
                                + rootMappingModelNode.getClassName() + " 』",
                        "Json to Dart convert fail!!");
                return;
            }
        }

        try {
            // 生成dart文件
            outputDartClassModelFile(parent, project, this.jsonAnalysis);
        } catch (IOException e) {
            showAnalysisErrorTip("Json to Dart convert fail!!");
            NotificationUtils.show(this.anActionEvent.getProject(), "Json to Dart convert fail.", "Check JSON data string, please.", NotificationType.WARNING);
            return;
        }
        dispose();
    }

    private void outputDartClassModelFile(VirtualFile parent, Project project, JsonDartAnalysis analysisMapping) throws IOException {
        if (parent != null && parent.isDirectory()) {
            // 开始生成文件
            analysisMapping.generated(parent, project);

            // 刷新IntelliJ文件系统
            parent.refresh(false, true);
            NotificationUtils.show(project,
                    "Json to Dart convert success!!",
                    analysisMapping.getRootMappingModel().getDartFileName() + ".dart generated.",
                    NotificationType.INFORMATION);
        }
    }
}
