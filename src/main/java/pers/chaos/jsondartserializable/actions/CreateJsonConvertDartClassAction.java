package pers.chaos.jsondartserializable.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.wm.WindowManager;
import pers.chaos.jsondartserializable.windows.Consts;
import pers.chaos.jsondartserializable.windows.JsonStringInputDialog;

import java.awt.*;

public class CreateJsonConvertDartClassAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        Window window = WindowManager.getInstance().getFrame(e.getProject());
        assert window != null;

        Point windowLocation = window.getLocationOnScreen();
        Dimension windowSize = window.getSize();

        JsonStringInputDialog dialog = new JsonStringInputDialog(e);
        dialog.pack();
        dialog.setTitle("JSON to Dart Convertor");
        dialog.setLocation(
                windowLocation.x + (windowSize.width - dialog.getWidth()) / 2,
                (int)(windowLocation.y + (windowSize.getHeight() - dialog.getHeight()) / 2)
        );
        dialog.setMinimumSize(new Dimension(Consts.JsonStringInputDialog.WIDTH_WINDOW, Consts.JsonStringInputDialog.HEIGHT_WINDOW));
        dialog.setVisible(true);
    }
}
