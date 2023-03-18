package pers.chaos.jsondartserializable.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBColor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import pers.chaos.jsondartserializable.core.json.JsonAnalyser;
import pers.chaos.jsondartserializable.core.json.JsonDartAnalysisMapping;
import pers.chaos.jsondartserializable.utils.NotificationUtils;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.Objects;

public class JsonStringInputDialog extends JDialog {
    private final AnActionEvent anActionEvent;

    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JButton buttonChecker;
    private JScrollPane scrollPane;
    private JLabel labelError;
    private JTextField textFieldClassName;
    private JTextArea textAreaJsonString;

    public JsonStringInputDialog(AnActionEvent anActionEvent) {
        this.anActionEvent = anActionEvent;

        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        buttonChecker.addActionListener(e -> onFormat());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onFormat() {
        hideJsonAnalysisErrorTip();
        try {
            // output pretty json display for user
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
        labelError.setText("Invalid JSON string, please check it");
        labelError.setForeground(JBColor.red);
    }

    private void showAnalysisErrorTip(String tip) {
        labelError.setText(tip);
        labelError.setForeground(JBColor.blue);
    }

    private void hideJsonAnalysisErrorTip() {
        labelError.setText(null);
    }

    private void onOK() {
        hideJsonAnalysisErrorTip();

        // support user input custom OBJECT dart class name
        String className = textFieldClassName.getText().trim();
        // user input JSON string
        String jsonString = textAreaJsonString.getText().trim();
        if (StringUtil.isEmpty(className) || StringUtil.isEmpty(jsonString)) {
            showAnalysisErrorTip("Empty class name or JSON string");
        }

        JsonDartAnalysisMapping analysisMapping;
        try {
            // check and analysis mapping model
            analysisMapping = JsonAnalyser.analysis(className, jsonString);
        } catch (Exception e) {
            System.out.println(ExceptionUtils.getStackTrace(e));
            showJsonFormatErrorTip();
            return;
        }

        try {
            // output dart class from mapping model
            outputDartClassModelFile(analysisMapping);
        } catch (IOException e) {
            showAnalysisErrorTip("Generate dart file fail");
            NotificationUtils.showNotification(this.anActionEvent.getProject(), "Generated error", "Check it", NotificationType.WARNING);
            return;
        }
        dispose();
    }

    private void outputDartClassModelFile(JsonDartAnalysisMapping analysisMapping) throws IOException {
        Project project = anActionEvent.getProject();
        if (Objects.isNull(project)) {
            return;
        }

        // get user current focus virtual file's handle
        VirtualFile parent = anActionEvent.getData(CommonDataKeys.VIRTUAL_FILE);
        if (parent != null && parent.isDirectory()) {
            VirtualFile child = parent.findChild(analysisMapping.getDartFileName());
            if (Objects.nonNull(child)) {
                NotificationUtils.showNotification(project, "Convert error", analysisMapping.getDartFileName() + ".dart exist", NotificationType.WARNING);
                return;
            }

            // start generate dart class
            analysisMapping.generated(parent, project);

            // refresh  IntelliJ IDEA file system
            parent.refresh(false, true);
            NotificationUtils.showNotification(project, "Convert success", analysisMapping.getDartFileName() + ".dart generated", NotificationType.INFORMATION);
        }
    }
}
